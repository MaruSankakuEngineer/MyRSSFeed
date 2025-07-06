package com.example.myrssfeed.data.local.dao

import androidx.room.*
import com.example.myrssfeed.data.local.entity.RssFeed
import kotlinx.coroutines.flow.Flow

@Dao
interface RssFeedDao {
    @Query("SELECT * FROM rss_feeds WHERE isEnabled = 1 ORDER BY sortOrder ASC")
    fun getAllEnabledFeeds(): Flow<List<RssFeed>>
    
    @Query("SELECT * FROM rss_feeds WHERE isEnabled = 1 ORDER BY sortOrder ASC")
    suspend fun getAllEnabledFeedsSync(): List<RssFeed>
    
    @Query("SELECT * FROM rss_feeds WHERE id = :feedId")
    suspend fun getFeedById(feedId: String): RssFeed?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeed(feed: RssFeed)
    
    @Update
    suspend fun updateFeed(feed: RssFeed)
    
    @Delete
    suspend fun deleteFeed(feed: RssFeed)
    
    @Query("DELETE FROM rss_feeds WHERE id = :feedId")
    suspend fun deleteFeedById(feedId: String)
    
    @Query("UPDATE rss_feeds SET lastUpdated = :timestamp WHERE id = :feedId")
    suspend fun updateLastUpdated(feedId: String, timestamp: Long)
} 