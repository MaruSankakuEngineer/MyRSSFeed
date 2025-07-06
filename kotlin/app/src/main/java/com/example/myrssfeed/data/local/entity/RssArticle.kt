package com.example.myrssfeed.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "rss_articles",
    indices = [Index("feedId"), Index("publishedAt")]
)
data class RssArticle(
    @PrimaryKey val id: String,
    val feedId: String,
    val title: String,
    val description: String?,
    val content: String?,
    val link: String,
    val publishedAt: Long,
    val imageUrl: String?,
    val isRead: Boolean = false,
    val isBookmarked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) 