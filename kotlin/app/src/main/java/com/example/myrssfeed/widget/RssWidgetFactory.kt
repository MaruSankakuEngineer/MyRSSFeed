package com.example.myrssfeed.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.myrssfeed.R
import com.example.myrssfeed.data.local.entity.RssArticle
import com.example.myrssfeed.data.local.entity.RssFeed
import com.example.myrssfeed.data.repository.RssRepository

import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*

class RssWidgetFactory(
    private val context: Context,
    private val appWidgetId: Int,
    private val repository: RssRepository
) : RemoteViewsService.RemoteViewsFactory {
    
    private var articles: List<RssArticle> = emptyList()
    private var feeds: List<RssFeed> = emptyList()
    private val dateFormat = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
    
    private fun formatRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < 60 * 1000 -> "今" // 1分未満
            diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}分前" // 1時間未満
            diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}時間前" // 1日未満
            diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)}日前" // 1週間未満
            else -> dateFormat.format(Date(timestamp)) // 1週間以上前は日付表示
        }
    }
    
    override fun onCreate() {
        // 初期化処理
        android.util.Log.d("RssWidget", "Widget factory created for widget $appWidgetId")
        onDataSetChanged()
    }
    
    override fun onDataSetChanged() {
        runBlocking {
            try {
                android.util.Log.d("RssWidget", "onDataSetChanged called for widget $appWidgetId")
                val settings = repository.getWidgetSettings(appWidgetId)
                android.util.Log.d("RssWidget", "Widget settings: $settings")
                android.util.Log.d("RssWidget", "Selected feed ID: ${settings?.selectedFeedId}")
                
                feeds = repository.getAllEnabledFeedsSync()
                android.util.Log.d("RssWidget", "Found ${feeds.size} feeds")
                
                // フィルター設定に基づいて記事を取得（最大100件）
                val displayCount = settings?.displayCount ?: 100
                articles = if (settings?.selectedFeedId != null) {
                    android.util.Log.d("RssWidget", "Getting articles for specific feed: ${settings.selectedFeedId}")
                    repository.getLatestArticlesByFeedSync(
                        settings.selectedFeedId,
                        displayCount
                    )
                } else {
                    android.util.Log.d("RssWidget", "Getting articles from all feeds")
                    repository.getLatestArticlesSync(displayCount)
                }
                android.util.Log.d("RssWidget", "Found ${articles.size} articles")
                
                // 記事の詳細ログを出力
                articles.forEachIndexed { index, article ->
                    android.util.Log.d("RssWidget", "Article $index: ${article.title} (${article.feedId}) - ${Date(article.publishedAt)}")
                }
                
                // 記事が見つからない場合は、すべてのフィードから最新記事を取得
                if (articles.isEmpty() && feeds.isNotEmpty()) {
                    android.util.Log.d("RssWidget", "No articles found, trying to get from all feeds")
                    articles = repository.getLatestArticlesSync(100)
                    android.util.Log.d("RssWidget", "Retrieved ${articles.size} articles from all feeds")
                }
                
                // データ変更後の確認ログ
                android.util.Log.d("RssWidget", "Data change completed. Articles: ${articles.size}, Feeds: ${feeds.size}")
            } catch (e: Exception) {
                android.util.Log.e("RssWidget", "Error in onDataSetChanged", e)
                articles = emptyList()
                feeds = emptyList()
            }
        }
    }
    
    override fun onDestroy() {
        articles = emptyList()
        feeds = emptyList()
    }
    
    override fun getCount(): Int = articles.size
    
    override fun getViewAt(position: Int): RemoteViews {
        android.util.Log.d("RssWidget", "getViewAt called for position $position, articles size: ${articles.size}")
        
        if (position >= articles.size) {
            android.util.Log.w("RssWidget", "Position $position is out of bounds")
            // ダミーの記事ビューを返す
            val views = RemoteViews(context.packageName, R.layout.widget_item_article)
            views.setTextViewText(R.id.article_title, "記事を読み込み中...")
            views.setTextViewText(R.id.feed_info, "")
            views.setInt(R.id.article_container, "setBackgroundResource", R.drawable.article_item_background)
            return views
        }
        
        val article = articles[position]
        val feed = feeds.find { it.id == article.feedId }
        
        android.util.Log.d("RssWidget", "Creating view for article: ${article.title}")
        
        val views = RemoteViews(context.packageName, R.layout.widget_item_article)
        
        try {
            // 記事タイトルを設定
            val title = article.title ?: "タイトルなし"
            views.setTextViewText(R.id.article_title, title)
            
            // フィード名と日時を設定
            val feedTitle = feed?.title ?: "Unknown"
            val dateStr = formatRelativeTime(article.publishedAt)
            val feedInfo = "$feedTitle • $dateStr"
            views.setTextViewText(R.id.feed_info, feedInfo)
            
            // フィードの色を適用
            android.util.Log.d("RssWidget", "Feed: ${feed?.title}, feedColor: ${feed?.feedColor}")
            if (feed != null && !feed.feedColor.isNullOrEmpty()) {
                try {
                    val feedColor = android.graphics.Color.parseColor(feed.feedColor)
                    views.setTextColor(R.id.feed_info, feedColor)
                    android.util.Log.d("RssWidget", "Applied color: ${feed.feedColor} to feed: ${feed.title}")
                } catch (e: Exception) {
                    android.util.Log.e("RssWidget", "Error parsing feed color: ${feed.feedColor}", e)
                }
            } else {
                android.util.Log.d("RssWidget", "No color set for feed: ${feed?.title}")
            }
            
            // 背景を明示的に設定
            views.setInt(R.id.article_container, "setBackgroundResource", R.drawable.article_item_background)
            
            // 記事リンクが存在する場合のみクリックリスナーを設定
            if (!article.link.isNullOrEmpty()) {
                try {
                    // setOnClickFillInIntentを使用してクリックイベントを設定
                    val fillInIntent = Intent().apply {
                        putExtra("widget_id", appWidgetId)
                        putExtra("position", position)
                        putExtra("article_id", article.id)
                        putExtra("article_link", article.link)
                        putExtra("article_title", article.title)
                    }
                    
                    views.setOnClickFillInIntent(R.id.article_container, fillInIntent)
                    views.setOnClickFillInIntent(R.id.article_title, fillInIntent)
                    views.setOnClickFillInIntent(R.id.feed_info, fillInIntent)
                    
                    android.util.Log.d("RssWidget", "Set fill-in intent for article: ${article.title} (position=$position, link=${article.link})")
                } catch (e: Exception) {
                    android.util.Log.e("RssWidget", "Error creating fill-in intent for article: ${article.title}", e)
                }
            } else {
                android.util.Log.w("RssWidget", "Article link is null or empty: ${article.title}")
            }
        } catch (e: Exception) {
            android.util.Log.e("RssWidget", "Error creating view for article", e)
            views.setTextViewText(R.id.article_title, "エラーが発生しました")
            views.setTextViewText(R.id.feed_info, "")
            views.setInt(R.id.article_container, "setBackgroundResource", R.drawable.article_item_background)
        }
        
        return views
    }
    
    override fun getLoadingView(): RemoteViews? = null
    
    override fun getViewTypeCount(): Int = 1
    
    override fun getItemId(position: Int): Long = position.toLong()
    
    override fun hasStableIds(): Boolean = true
} 