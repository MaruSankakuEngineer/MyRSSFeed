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
import retrofit2.Retrofit
import retrofit2.converter.simplexml.SimpleXmlConverterFactory

class RssWidgetProvider : AppWidgetProvider() {
    
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
        
        // ウィジェットサービスを設定
        val intent = Intent(context, RssWidgetService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            data = Uri.fromParts("content", appWidgetId.toString(), null)
        }
        views.setRemoteAdapter(R.id.list_view, intent)
        
        // 空のビューを設定
        views.setEmptyView(R.id.list_view, R.id.empty_view)
        
        // ウィジェットの背景を明示的に設定
        views.setInt(R.id.widget_container, "setBackgroundResource", R.drawable.widget_background)
        
        // デフォルト設定を作成
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
                
                // 設定作成後、即座にウィジェットを更新
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.list_view)
                
                // データが確実に読み込まれるように、少し遅延して再度更新
                Handler(android.os.Looper.getMainLooper()).postDelayed({
                    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.list_view)
                }, 1000)
            } catch (e: Exception) {
                android.util.Log.e("RssWidget", "Error creating default settings", e)
            }
        }
        
        // フィルターボタンのクリックリスナーを設定
        val filterIntent = Intent(context, RssWidgetProvider::class.java).apply {
            action = ACTION_FILTER_CLICKED
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        val filterPendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId,
            filterIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.filter_button, filterPendingIntent)
        
        // リストアイテムのクリックリスナーを設定
        val listItemIntent = Intent(context, RssWidgetProvider::class.java).apply {
            action = ACTION_ITEM_CLICKED
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        val listItemPendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId,
            listItemIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setPendingIntentTemplate(R.id.list_view, listItemPendingIntent)
        
        // ウィジェットを更新
        appWidgetManager.updateAppWidget(appWidgetId, views)
        
        // データ変更を通知（即座に実行）
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.list_view)
        
        // 少し遅延して再度更新（確実に表示されるように）
        Handler(android.os.Looper.getMainLooper()).postDelayed({
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.list_view)
        }, 500)
        
        // さらに遅延して最終更新
        Handler(android.os.Looper.getMainLooper()).postDelayed({
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.list_view)
        }, 2000)
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        when (intent.action) {
            ACTION_FILTER_CLICKED -> {
                val appWidgetId = intent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID
                )
                showFilterDialog(context, appWidgetId)
            }
            ACTION_ITEM_CLICKED -> {
                val appWidgetId = intent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID
                )
                val position = intent.getIntExtra(EXTRA_ITEM_POSITION, -1)
                android.util.Log.d("RssWidget", "Item clicked: widgetId=$appWidgetId, position=$position")
                openArticle(context, appWidgetId, position)
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
    
    private fun openArticle(context: Context, appWidgetId: Int, position: Int) {
        try {
            // メインスレッドで即座に実行
            val database = AppDatabase.getDatabase(context)
            val repository = RssRepository(
                database.rssFeedDao(),
                database.rssArticleDao(),
                database.widgetSettingsDao(),
                createRssApiService()
            )
            
            // 非同期で記事を取得してブラウザで開く
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val settings = repository.getWidgetSettings(appWidgetId)
                    val articles = if (settings?.selectedFeedId != null) {
                        repository.getLatestArticlesByFeedSync(settings.selectedFeedId, settings.displayCount)
                    } else {
                        repository.getLatestArticlesSync(settings?.displayCount ?: 5)
                    }
                    
                    if (position < articles.size) {
                        val article = articles[position]
                        android.util.Log.d("RssWidget", "Opening article: ${article.title} at ${article.link}")
                        
                        // 既読マーク
                        repository.markAsRead(article.id)
                        
                        // ブラウザで記事を開く
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(article.link))
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(intent)
                    } else {
                        android.util.Log.w("RssWidget", "Invalid position: $position, articles size: ${articles.size}")
                    }
                } catch (e: Exception) {
                    // エラーハンドリング
                }
            }
        } catch (e: Exception) {
            // エラーハンドリング
        }
    }
    
    private fun createRssApiService(): RssApiService {
        return Retrofit.Builder()
            .baseUrl("https://example.com/") // ダミーURL
            .addConverterFactory(SimpleXmlConverterFactory.create())
            .build()
            .create(RssApiService::class.java)
    }
    
    companion object {
        const val ACTION_FILTER_CLICKED = "com.example.myrssfeed.FILTER_CLICKED"
        const val ACTION_ITEM_CLICKED = "com.example.myrssfeed.ITEM_CLICKED"
        const val EXTRA_ITEM_POSITION = "item_position"
    }
} 