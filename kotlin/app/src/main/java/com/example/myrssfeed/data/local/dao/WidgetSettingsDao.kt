package com.example.myrssfeed.data.local.dao

import androidx.room.*
import com.example.myrssfeed.data.local.entity.WidgetSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface WidgetSettingsDao {
    @Query("SELECT * FROM widget_settings WHERE widgetId = :widgetId")
    suspend fun getWidgetSettings(widgetId: Int): WidgetSettings?
    
    @Query("SELECT * FROM widget_settings WHERE widgetId = :widgetId")
    fun getWidgetSettingsFlow(widgetId: Int): Flow<WidgetSettings?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWidgetSettings(settings: WidgetSettings)
    
    @Update
    suspend fun updateWidgetSettings(settings: WidgetSettings)
    
    @Delete
    suspend fun deleteWidgetSettings(settings: WidgetSettings)
    
    @Query("DELETE FROM widget_settings WHERE widgetId = :widgetId")
    suspend fun deleteWidgetSettingsById(widgetId: Int)
    
    @Query("UPDATE widget_settings SET selectedFeedId = :feedId WHERE widgetId = :widgetId")
    suspend fun updateSelectedFeed(widgetId: Int, feedId: String?)
    
    @Query("UPDATE widget_settings SET displayCount = :count WHERE widgetId = :widgetId")
    suspend fun updateDisplayCount(widgetId: Int, count: Int)
    
    @Query("UPDATE widget_settings SET showImages = :showImages WHERE widgetId = :widgetId")
    suspend fun updateShowImages(widgetId: Int, showImages: Boolean)
    
    @Query("UPDATE widget_settings SET theme = :theme WHERE widgetId = :widgetId")
    suspend fun updateTheme(widgetId: Int, theme: String)
} 