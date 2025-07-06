package com.example.myrssfeed.widget

import android.content.Intent
import android.widget.RemoteViewsService
import com.example.myrssfeed.data.local.database.AppDatabase
import com.example.myrssfeed.data.repository.RssRepository
import com.example.myrssfeed.data.remote.api.RssApiService
import retrofit2.Retrofit
import retrofit2.converter.simplexml.SimpleXmlConverterFactory

class RssWidgetService : RemoteViewsService() {
    
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val appWidgetId = intent.getIntExtra(
            android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID,
            android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID
        )
        
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = RssRepository(
            database.rssFeedDao(),
            database.rssArticleDao(),
            database.widgetSettingsDao(),
            createRssApiService()
        )
        
        return com.example.myrssfeed.widget.RssWidgetFactory(applicationContext, appWidgetId, repository)
    }
    
    private fun createRssApiService(): RssApiService {
        return Retrofit.Builder()
            .baseUrl("https://example.com/") // ダミーURL
            .addConverterFactory(SimpleXmlConverterFactory.create())
            .build()
            .create(RssApiService::class.java)
    }
} 