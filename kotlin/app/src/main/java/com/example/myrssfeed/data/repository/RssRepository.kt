package com.example.myrssfeed.data.repository

import com.example.myrssfeed.data.local.dao.RssFeedDao
import com.example.myrssfeed.data.local.dao.RssArticleDao
import com.example.myrssfeed.data.local.dao.WidgetSettingsDao
import com.example.myrssfeed.data.local.entity.RssFeed
import com.example.myrssfeed.data.local.entity.RssArticle
import com.example.myrssfeed.data.local.entity.WidgetSettings
import com.example.myrssfeed.data.remote.api.RssApiService
import com.example.myrssfeed.data.remote.dto.RssResponse
import com.example.myrssfeed.data.remote.dto.RssResponseRdf
import org.simpleframework.xml.core.Persister
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*

class RssRepository(
    private val rssFeedDao: RssFeedDao,
    private val rssArticleDao: RssArticleDao,
    private val widgetSettingsDao: WidgetSettingsDao,
    private val rssApiService: RssApiService
) {
    // RSSフィード関連
    fun getAllEnabledFeeds(): Flow<List<RssFeed>> = rssFeedDao.getAllEnabledFeeds()
    
    suspend fun getAllEnabledFeedsSync(): List<RssFeed> = rssFeedDao.getAllEnabledFeedsSync()
    
    suspend fun getFeedById(feedId: String): RssFeed? = rssFeedDao.getFeedById(feedId)
    
    suspend fun insertFeed(feed: RssFeed) = rssFeedDao.insertFeed(feed)
    
    suspend fun updateFeed(feed: RssFeed) = rssFeedDao.updateFeed(feed)
    
    suspend fun deleteFeed(feed: RssFeed) {
        rssFeedDao.deleteFeed(feed)
        rssArticleDao.deleteArticlesByFeed(feed.id)
    }
    
    // RSS記事関連
    fun getLatestArticles(limit: Int): Flow<List<RssArticle>> = rssArticleDao.getLatestArticles(limit)
    
    fun getLatestArticlesByFeed(feedId: String, limit: Int): Flow<List<RssArticle>> = 
        rssArticleDao.getLatestArticlesByFeed(feedId, limit)
    
    suspend fun getLatestArticlesSync(limit: Int): List<RssArticle> = 
        rssArticleDao.getLatestArticlesSync(limit)
    
    suspend fun getLatestArticlesByFeedSync(feedId: String, limit: Int): List<RssArticle> = 
        rssArticleDao.getLatestArticlesByFeedSync(feedId, limit)
    
    suspend fun markAsRead(articleId: String) = rssArticleDao.markAsRead(articleId)
    
    suspend fun updateBookmark(articleId: String, isBookmarked: Boolean) = 
        rssArticleDao.updateBookmark(articleId, isBookmarked)
    
    // ウィジェット設定関連
    suspend fun getWidgetSettings(widgetId: Int): WidgetSettings? = 
        widgetSettingsDao.getWidgetSettings(widgetId)
    
    fun getWidgetSettingsFlow(widgetId: Int): Flow<WidgetSettings?> = 
        widgetSettingsDao.getWidgetSettingsFlow(widgetId)
    
    suspend fun insertWidgetSettings(settings: WidgetSettings) = 
        widgetSettingsDao.insertWidgetSettings(settings)
    
    suspend fun updateSelectedFeed(widgetId: Int, feedId: String?) = 
        widgetSettingsDao.updateSelectedFeed(widgetId, feedId)
    
    suspend fun updateDisplayCount(widgetId: Int, count: Int) = 
        widgetSettingsDao.updateDisplayCount(widgetId, count)
    
    suspend fun updateShowImages(widgetId: Int, showImages: Boolean) = 
        widgetSettingsDao.updateShowImages(widgetId, showImages)
    
    // RSSフィード更新
    suspend fun updateRssFeed(feed: RssFeed) {
        try {
            val responseBody = rssApiService.getRssFeed(feed.url)
            val xml = responseBody.string()
            
            if (xml.isBlank()) {
                throw Exception("空のレスポンスを受信しました")
            }
            
            val serializer = Persister()
            var articles: List<RssArticle> = emptyList()
            var parsed = false
            var parseError = ""
            
            // RSS2.0パース
            try {
                val rss = serializer.read(RssResponse::class.java, xml)
                val channel = rss.channel
                if (channel.items.isNotEmpty()) {
                    articles = channel.items.mapNotNull { item ->
                        if (item.title.isNotEmpty() && item.link.isNotEmpty()) {
                            RssArticle(
                                id = generateArticleId(feed.id, item.link),
                                feedId = feed.id,
                                title = item.title,
                                description = item.description,
                                content = item.description,
                                link = item.link,
                                publishedAt = parseDate(item.pubDate),
                                imageUrl = item.enclosure?.url
                            )
                        } else null
                    }
                    parsed = true
                }
            } catch (e: Exception) {
                parseError += "RSS2.0パース失敗: ${e.message}; "
            }
            
            // RDF(RSS1.0)パース
            if (!parsed) {
                try {
                    val rdf = serializer.read(RssResponseRdf::class.java, xml)
                    if (rdf.items.isNotEmpty()) {
                        articles = rdf.items.mapNotNull { item ->
                            if (item.title.isNotEmpty() && item.link.isNotEmpty()) {
                                RssArticle(
                                    id = generateArticleId(feed.id, item.link),
                                    feedId = feed.id,
                                    title = item.title,
                                    description = item.description,
                                    content = item.description,
                                    link = item.link,
                                    publishedAt = parseDate(item.pubDate),
                                    imageUrl = null
                                )
                            } else null
                        }
                        parsed = true
                    } else {
                        parseError += "RDFパース成功したが記事が見つかりません; "
                    }
                } catch (e: Exception) {
                    parseError += "RDFパース失敗: ${e.message} (${e.javaClass.simpleName}); "
                }
            }
            
            if (!parsed) {
                throw Exception("RSSフィードの解析に失敗しました: $parseError")
            }
            
            if (articles.isNotEmpty()) {
                rssArticleDao.insertArticles(articles)
            } else {
                throw Exception("記事が見つかりませんでした")
            }
            
            rssFeedDao.updateLastUpdated(feed.id, System.currentTimeMillis())
        } catch (e: Exception) {
            // エラーハンドリング
            throw e
        }
    }
    
    // すべての有効なフィードを更新
    suspend fun refreshAllFeeds() {
        val feeds = getAllEnabledFeedsSync()
        feeds.forEach { feed ->
            try {
                updateRssFeed(feed)
            } catch (e: Exception) {
                // 個別のフィードでエラーが発生しても他のフィードの更新は続行
                // ログ出力などでエラーを記録
            }
        }
    }
    
    private fun generateArticleId(feedId: String, link: String): String {
        return "${feedId}_${link.hashCode()}"
    }
    
    private fun parseDate(dateString: String?): Long {
        if (dateString.isNullOrEmpty()) return System.currentTimeMillis()
        
        return try {
            val formatter = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US)
            formatter.parse(dateString)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
} 