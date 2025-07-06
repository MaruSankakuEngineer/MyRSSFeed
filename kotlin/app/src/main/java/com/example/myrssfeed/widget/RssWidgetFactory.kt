package com.example.myrssfeed.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.myrssfeed.R
import com.example.myrssfeed.data.local.entity.RssArticle
import com.example.myrssfeed.data.local.entity.RssFeed
import com.example.myrssfeed.data.repository.RssRepository
import com.example.myrssfeed.widget.RssWidgetProvider.Companion.EXTRA_ITEM_POSITION
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
                
                feeds = repository.getAllEnabledFeedsSync()
                android.util.Log.d("RssWidget", "Found ${feeds.size} feeds")
                
                articles = if (settings?.selectedFeedId != null) {
                    repository.getLatestArticlesByFeedSync(
                        settings.selectedFeedId,
                        settings.displayCount
                    )
                } else {
                    repository.getLatestArticlesSync(settings?.displayCount ?: 5)
                }
                android.util.Log.d("RssWidget", "Found ${articles.size} articles")
                
                // 記事が見つからない場合は、すべてのフィードから最新記事を取得
                if (articles.isEmpty() && feeds.isNotEmpty()) {
                    android.util.Log.d("RssWidget", "No articles found, trying to get from all feeds")
                    articles = repository.getLatestArticlesSync(5)
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
            val dateStr = try {
                dateFormat.format(Date(article.publishedAt))
            } catch (e: Exception) {
                "日時不明"
            }
            val feedInfo = "$feedTitle • $dateStr"
            views.setTextViewText(R.id.feed_info, feedInfo)
            
            // 背景を明示的に設定
            views.setInt(R.id.article_container, "setBackgroundResource", R.drawable.article_item_background)
            
            // クリックリスナーを設定
            val intent = Intent().apply {
                putExtra(EXTRA_ITEM_POSITION, position)
            }
            views.setOnClickFillInIntent(R.id.article_container, intent)
            
            android.util.Log.d("RssWidget", "Successfully created view for article: ${article.title}")
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