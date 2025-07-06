package com.example.myrssfeed.data.local.dao

import androidx.room.*
import com.example.myrssfeed.data.local.entity.RssArticle
import kotlinx.coroutines.flow.Flow

@Dao
interface RssArticleDao {
    @Query("SELECT * FROM rss_articles ORDER BY publishedAt DESC LIMIT :limit")
    fun getLatestArticles(limit: Int): Flow<List<RssArticle>>
    
    @Query("SELECT * FROM rss_articles WHERE feedId = :feedId ORDER BY publishedAt DESC LIMIT :limit")
    fun getLatestArticlesByFeed(feedId: String, limit: Int): Flow<List<RssArticle>>
    
    @Query("SELECT * FROM rss_articles ORDER BY publishedAt DESC LIMIT :limit")
    suspend fun getLatestArticlesSync(limit: Int): List<RssArticle>
    
    @Query("SELECT * FROM rss_articles WHERE feedId = :feedId ORDER BY publishedAt DESC LIMIT :limit")
    suspend fun getLatestArticlesByFeedSync(feedId: String, limit: Int): List<RssArticle>
    
    @Query("SELECT * FROM rss_articles WHERE id = :articleId")
    suspend fun getArticleById(articleId: String): RssArticle?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticle(article: RssArticle)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticles(articles: List<RssArticle>)
    
    @Update
    suspend fun updateArticle(article: RssArticle)
    
    @Delete
    suspend fun deleteArticle(article: RssArticle)
    
    @Query("DELETE FROM rss_articles WHERE feedId = :feedId")
    suspend fun deleteArticlesByFeed(feedId: String)
    
    @Query("UPDATE rss_articles SET isRead = 1 WHERE id = :articleId")
    suspend fun markAsRead(articleId: String)
    
    @Query("UPDATE rss_articles SET isBookmarked = :isBookmarked WHERE id = :articleId")
    suspend fun updateBookmark(articleId: String, isBookmarked: Boolean)
    
    @Query("DELETE FROM rss_articles WHERE createdAt < :timestamp")
    suspend fun deleteOldArticles(timestamp: Long)
} 