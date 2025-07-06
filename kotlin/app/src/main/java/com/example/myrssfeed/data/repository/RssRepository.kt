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
    
    suspend fun getFeedByIdSync(feedId: String): RssFeed? = rssFeedDao.getFeedById(feedId)
    
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
            android.util.Log.d("RssRepository", "Updating RSS feed: ${feed.title} (${feed.url})")
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
                    android.util.Log.d("RssRepository", "RSS2.0 parsing successful, found ${channel.items.size} items")
                    articles = channel.items.mapNotNull { item ->
                        if (item.title.isNotEmpty() && item.link.isNotEmpty()) {
                            val publishedAt = parseDate(item.pubDate)
                            android.util.Log.d("RssRepository", "Article: ${item.title}, pubDate: ${item.pubDate}, parsed: ${Date(publishedAt)}")
                            RssArticle(
                                id = generateArticleId(feed.id, item.link),
                                feedId = feed.id,
                                title = item.title,
                                description = item.description,
                                content = item.description,
                                link = item.link,
                                publishedAt = publishedAt,
                                imageUrl = item.enclosure?.url
                            )
                        } else {
                            android.util.Log.w("RssRepository", "Skipping article with empty title or link")
                            null
                        }
                    }
                    parsed = true
                } else {
                    parseError += "RSS2.0パース成功したが記事が見つかりません; "
                }
            } catch (e: Exception) {
                parseError += "RSS2.0パース失敗: ${e.message}; "
            }
            
            // RDF(RSS1.0)パース
            if (!parsed) {
                try {
                    val rdf = serializer.read(RssResponseRdf::class.java, xml)
                    if (rdf.items.isNotEmpty()) {
                        android.util.Log.d("RssRepository", "RDF parsing successful, found ${rdf.items.size} items")
                        articles = rdf.items.mapNotNull { item ->
                            if (item.title.isNotEmpty() && item.link.isNotEmpty()) {
                                val publishedAt = parseDate(item.getPublicationDate())
                                android.util.Log.d("RssRepository", "Article: ${item.title}, pubDate: ${item.getPublicationDate()}, parsed: ${Date(publishedAt)}")
                                RssArticle(
                                    id = generateArticleId(feed.id, item.link),
                                    feedId = feed.id,
                                    title = item.title,
                                    description = item.description,
                                    content = item.description,
                                    link = item.link,
                                    publishedAt = publishedAt,
                                    imageUrl = null
                                )
                            } else {
                                android.util.Log.w("RssRepository", "Skipping article with empty title or link")
                                null
                            }
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
                android.util.Log.d("RssRepository", "Inserting ${articles.size} articles for feed: ${feed.title}")
                rssArticleDao.insertArticles(articles)
            } else {
                throw Exception("記事が見つかりませんでした")
            }
            
            rssFeedDao.updateLastUpdated(feed.id, System.currentTimeMillis())
            android.util.Log.d("RssRepository", "Successfully updated feed: ${feed.title}")
        } catch (e: Exception) {
            android.util.Log.e("RssRepository", "Error updating RSS feed: ${feed.title}", e)
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
        if (dateString.isNullOrEmpty()) {
            android.util.Log.w("RssRepository", "Empty date string, using current time")
            return System.currentTimeMillis()
        }
        
        // 複数の日時フォーマットを試行
        val dateFormats = listOf(
            "EEE, dd MMM yyyy HH:mm:ss Z",  // RFC 822 (標準)
            "EEE, dd MMM yyyy HH:mm:ss z",  // タイムゾーン小文字
            "EEE, dd MMM yyyy HH:mm Z",     // 秒なし
            "EEE, dd MMM yyyy HH:mm z",     // 秒なし、タイムゾーン小文字
            "yyyy-MM-dd'T'HH:mm:ss'Z'",     // ISO 8601
            "yyyy-MM-dd'T'HH:mm:ssZ",       // ISO 8601 with timezone
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", // ISO 8601 with milliseconds
            "yyyy-MM-dd HH:mm:ss",          // シンプルな形式
            "dd MMM yyyy HH:mm:ss Z",       // 曜日なし
            "dd MMM yyyy HH:mm Z"           // 曜日なし、秒なし
        )
        
        for (format in dateFormats) {
            try {
                val formatter = SimpleDateFormat(format, Locale.US)
                formatter.isLenient = false
                val date = formatter.parse(dateString)
                if (date != null) {
                    android.util.Log.d("RssRepository", "Successfully parsed date: $dateString with format: $format -> ${Date(date.time)}")
                    return date.time
                }
            } catch (e: Exception) {
                // このフォーマットではパースできなかった場合、次のフォーマットを試行
                continue
            }
        }
        
        // すべてのフォーマットでパースに失敗した場合
        android.util.Log.w("RssRepository", "Failed to parse date: $dateString, using current time")
        return System.currentTimeMillis()
    }
} 