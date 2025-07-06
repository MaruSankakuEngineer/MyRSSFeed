package com.example.myrssfeed.data.local.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun rssFeedDao(): RssFeedDao
    abstract fun rssArticleDao(): RssArticleDao
    abstract fun widgetSettingsDao(): WidgetSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // フィード名と更新時刻の色設定フィールドを追加（古いバージョン）
                database.execSQL("ALTER TABLE rss_feeds ADD COLUMN feedNameColor TEXT")
                database.execSQL("ALTER TABLE rss_feeds ADD COLUMN timestampColor TEXT")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 一時的なテーブルを作成
                database.execSQL("""
                    CREATE TABLE rss_feeds_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        url TEXT NOT NULL,
                        description TEXT,
                        lastUpdated INTEGER NOT NULL,
                        updateInterval INTEGER NOT NULL,
                        isEnabled INTEGER NOT NULL,
                        sortOrder INTEGER NOT NULL,
                        category TEXT,
                        feedColor TEXT
                    )
                """.trimIndent())
                
                // データを移行（feedNameColorを優先）
                database.execSQL("""
                    INSERT INTO rss_feeds_new (
                        id, title, url, description, lastUpdated, updateInterval, 
                        isEnabled, sortOrder, category, feedColor
                    )
                    SELECT 
                        id, title, url, description, lastUpdated, updateInterval,
                        isEnabled, sortOrder, category,
                        CASE 
                            WHEN feedNameColor IS NOT NULL THEN feedNameColor 
                            WHEN timestampColor IS NOT NULL THEN timestampColor 
                            ELSE NULL 
                        END as feedColor
                    FROM rss_feeds
                """.trimIndent())
                
                // 古いテーブルを削除して新しいテーブルに名前を変更
                database.execSQL("DROP TABLE rss_feeds")
                database.execSQL("ALTER TABLE rss_feeds_new RENAME TO rss_feeds")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rss_feed_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
} 