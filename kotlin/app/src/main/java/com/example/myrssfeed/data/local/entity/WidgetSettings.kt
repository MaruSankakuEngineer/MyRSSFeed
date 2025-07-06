package com.example.myrssfeed.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "widget_settings")
data class WidgetSettings(
    @PrimaryKey val widgetId: Int,
    val selectedFeedId: String? = null, // 選択されたフィードID（null=すべて）
    val displayCount: Int = 5,
    val showImages: Boolean = true,
    val theme: String = "system" // light, dark, system
) 