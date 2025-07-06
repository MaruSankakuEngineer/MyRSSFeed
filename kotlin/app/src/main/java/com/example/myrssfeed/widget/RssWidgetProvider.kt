package com.example.myrssfeed.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.widget.RemoteViews
import com.example.myrssfeed.R
import com.example.myrssfeed.data.local.database.AppDatabase
import com.example.myrssfeed.data.local.entity.WidgetSettings
import com.example.myrssfeed.data.repository.RssRepository
import com.example.myrssfeed.data.remote.api.RssApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.simplexml.SimpleXmlConverterFactory

class RssWidgetProvider : AppWidgetProvider() {
    
    private fun formatRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < 60 * 1000 -> "今" // 1分未満
            diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}分前" // 1時間未満
            diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}時間前" // 1日未満
            diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)}日前" // 1週間未満
            else -> java.text.SimpleDateFormat("MM/dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(timestamp)) // 1週間以上前は日付表示
        }
    }
    
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }
    
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        android.util.Log.d("RssWidget", "Widget enabled")
    }
    
    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        android.util.Log.d("RssWidget", "Widget options changed for widget $appWidgetId")
        
        // サイズ変更時は少し遅延してから更新
        Handler(android.os.Looper.getMainLooper()).postDelayed({
            updateWidget(context, appWidgetManager, appWidgetId)
        }, 100)
    }
    
    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        android.util.Log.d("RssWidget", "updateWidget called for widget $appWidgetId")
        
        val views = RemoteViews(context.packageName, R.layout.widget_rss_feed)
        
        // 記事を直接配置
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getDatabase(context)
                val repository = RssRepository(
                    database.rssFeedDao(),
                    database.rssArticleDao(),
                    database.widgetSettingsDao(),
                    createRssApiService()
                )
                
                val settings = repository.getWidgetSettings(appWidgetId)
                android.util.Log.d("RssWidget", "Current widget settings: $settings")
                
                if (settings == null) {
                    android.util.Log.d("RssWidget", "Creating default settings for widget $appWidgetId")
                    repository.insertWidgetSettings(
                        WidgetSettings(
                            widgetId = appWidgetId,
                            selectedFeedId = null,
                            displayCount = 5,
                            showImages = false
                        )
                    )
                }
                
                val articles = if (settings?.selectedFeedId != null) {
                    repository.getLatestArticlesByFeedSync(settings.selectedFeedId, settings.displayCount)
                } else {
                    repository.getLatestArticlesSync(settings?.displayCount ?: 5)
                }
                
                android.util.Log.d("RssWidget", "Retrieved ${articles.size} articles for widget $appWidgetId")
                
                // 既存の記事をクリア
                views.removeAllViews(R.id.list_view)
                
                // 記事を直接配置
                for (i in 0 until minOf(articles.size, 5)) {
                    val article = articles[i]
                    val articleViews = RemoteViews(context.packageName, R.layout.widget_item_article)
                    
                    // 記事タイトルを設定
                    val title = article.title ?: "タイトルなし"
                    articleViews.setTextViewText(R.id.article_title, title)
                    
                    // フィード名と日時を設定
                    val feed = repository.getFeedByIdSync(article.feedId)
                    val feedTitle = feed?.title ?: "Unknown"
                    val dateStr = formatRelativeTime(article.publishedAt)
                    val feedInfo = "$feedTitle • $dateStr"
                    articleViews.setTextViewText(R.id.feed_info, feedInfo)
                    
                    // 背景を明示的に設定
                    articleViews.setInt(R.id.article_container, "setBackgroundResource", R.drawable.article_item_background)
                    
                    // クリックリスナーを設定
                    if (!article.link.isNullOrEmpty()) {
                        try {
                            val clickIntent = Intent(context, RssWidgetProvider::class.java).apply {
                                action = "com.example.myrssfeed.ARTICLE_CLICKED"
                                putExtra("widget_id", appWidgetId)
                                putExtra("position", i)
                                putExtra("article_id", article.id)
                                putExtra("article_link", article.link)
                                putExtra("article_title", article.title)
                            }
                            
                            val pendingIntent = PendingIntent.getBroadcast(
                                context,
                                appWidgetId * 1000 + i,
                                clickIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                            
                            articleViews.setOnClickPendingIntent(R.id.article_container, pendingIntent)
                            articleViews.setOnClickPendingIntent(R.id.article_title, pendingIntent)
                            articleViews.setOnClickPendingIntent(R.id.feed_info, pendingIntent)
                            
                            android.util.Log.d("RssWidget", "Set pending intent for article: ${article.title} (position=$i, link=${article.link})")
                        } catch (e: Exception) {
                            android.util.Log.e("RssWidget", "Error creating pending intent for article: ${article.title}", e)
                        }
                    }
                    
                    // 記事をリストに追加
                    views.addView(R.id.list_view, articleViews)
                }
                
                // 記事が表示された場合は空の状態表示を非表示にする
                if (articles.isNotEmpty()) {
                    views.setViewVisibility(R.id.empty_view, android.view.View.GONE)
                } else {
                    views.setViewVisibility(R.id.empty_view, android.view.View.VISIBLE)
                }
                
                // ウィジェットを更新
                appWidgetManager.updateAppWidget(appWidgetId, views)
                android.util.Log.d("RssWidget", "Widget updated with ${articles.size} articles")
                
            } catch (e: Exception) {
                android.util.Log.e("RssWidget", "Error updating widget", e)
            }
        }
        
        // ウィジェットの背景を明示的に設定
        views.setInt(R.id.widget_container, "setBackgroundResource", R.drawable.widget_background)
        
        // フィルターボタンのクリックリスナーを設定
        val filterIntent = Intent(context, RssWidgetProvider::class.java).apply {
            action = "com.example.myrssfeed.FILTER_CLICKED"
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        val filterPendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId,
            filterIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
                views.setOnClickPendingIntent(R.id.filter_button, filterPendingIntent)
 

        

    }
    
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        android.util.Log.d("RssWidget", "onReceive called with action: ${intent.action}")
        
        when (intent.action) {
            "com.example.myrssfeed.FILTER_CLICKED" -> {
                android.util.Log.d("RssWidget", "Filter button clicked")
                val appWidgetId = intent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID
                )
                showFilterDialog(context, appWidgetId)
            }
            "com.example.myrssfeed.ARTICLE_CLICKED" -> {
                android.util.Log.d("RssWidget", "Article clicked event received")
                
                // setOnClickFillInIntentから送信された情報を取得
                val widgetId = intent.getIntExtra("widget_id", -1)
                val position = intent.getIntExtra("position", -1)
                val articleId = intent.getStringExtra("article_id")
                val articleLink = intent.getStringExtra("article_link")
                val articleTitle = intent.getStringExtra("article_title")
                
                android.util.Log.d("RssWidget", "Article clicked: widgetId=$widgetId, position=$position, articleId=$articleId, articleLink=$articleLink, title=$articleTitle")
                
                // デバッグ用：全てのインテントエクストラをログ出力
                val extras = intent.extras
                if (extras != null) {
                    android.util.Log.d("RssWidget", "All intent extras:")
                    for (key in extras.keySet()) {
                        android.util.Log.d("RssWidget", "  $key = ${extras.get(key)}")
                    }
                }
                
                if (!articleLink.isNullOrEmpty()) {
                    try {
                        val browserIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(articleLink)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        }
                        context.startActivity(browserIntent)
                        android.util.Log.d("RssWidget", "Successfully opened article in browser: $articleLink")
                    } catch (e: Exception) {
                        android.util.Log.e("RssWidget", "Error opening article in browser: $articleLink", e)
                    }
                } else {
                    android.util.Log.e("RssWidget", "Article link is null or empty")
                }
            }
            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                android.util.Log.d("RssWidget", "Widget update requested")
                val appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                if (appWidgetIds != null) {
                    for (appWidgetId in appWidgetIds) {
                        android.util.Log.d("RssWidget", "Updating widget $appWidgetId")
                        updateWidget(context, AppWidgetManager.getInstance(context), appWidgetId)
                    }
                }
            }
            else -> {
                android.util.Log.d("RssWidget", "Unknown action received: ${intent.action}")
            }
        }
    }
    
    private fun showFilterDialog(context: Context, appWidgetId: Int) {
        // フィルターダイアログを表示するアクティビティを起動
        val intent = Intent(context, com.example.myrssfeed.presentation.ui.filter.FilterActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        context.startActivity(intent)
    }
    

    
    private fun createRssApiService(): RssApiService {
        return Retrofit.Builder()
            .baseUrl("https://example.com/") // ダミーURL
            .addConverterFactory(SimpleXmlConverterFactory.create())
            .build()
            .create(RssApiService::class.java)
    }
    
    companion object {
        private const val TAG = "RssWidget"
    }
} 