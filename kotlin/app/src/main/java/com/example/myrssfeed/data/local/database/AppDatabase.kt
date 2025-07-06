package com.example.myrssfeed.data.local.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.example.myrssfeed.data.local.dao.RssFeedDao
import com.example.myrssfeed.data.local.dao.RssArticleDao
import com.example.myrssfeed.data.local.dao.WidgetSettingsDao
import com.example.myrssfeed.data.local.entity.RssFeed
import com.example.myrssfeed.data.local.entity.RssArticle
import com.example.myrssfeed.data.local.entity.WidgetSettings

@Database(
    entities = [
        RssFeed::class,
        RssArticle::class,
        WidgetSettings::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun rssFeedDao(): RssFeedDao
    abstract fun rssArticleDao(): RssArticleDao
    abstract fun widgetSettingsDao(): WidgetSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rss_feed_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
} 