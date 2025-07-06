package com.example.myrssfeed.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rss_feeds")
data class RssFeed(
    @PrimaryKey val id: String,
    val title: String,
    val url: String,
    val description: String?,
    val lastUpdated: Long,
    val updateInterval: Int,
    val isEnabled: Boolean = true,
    val sortOrder: Int = 0,
    val category: String? = null // フィルター用カテゴリ
) 