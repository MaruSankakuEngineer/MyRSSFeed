package com.example.myrssfeed.presentation.ui.color

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.myrssfeed.R
import com.example.myrssfeed.data.local.database.AppDatabase
import com.example.myrssfeed.data.repository.RssRepository
import com.example.myrssfeed.data.remote.api.RssApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.simplexml.SimpleXmlConverterFactory

class FeedColorSettingsActivity : AppCompatActivity() {
    
    companion object {
        const val EXTRA_FEED_ID = "feed_id"
        const val EXTRA_FEED_TITLE = "feed_title"
        const val EXTRA_CURRENT_FEED_COLOR = "current_feed_color"
        const val RESULT_FEED_COLOR = "result_feed_color"
    }
    
    private lateinit var colorGrid: GridLayout
    private lateinit var preview: TextView
    private lateinit var saveButton: Button
    private lateinit var resetButton: Button
    
    private var selectedColor: String? = null
    private var feedId: String = ""
    private var feedTitle: String = ""
    
    // プリセットカラーのリスト
    private val presetColors = listOf(
        "#FF6B6B", // 赤
        "#4ECDC4", // シアン
        "#45B7D1", // 青
        "#96CEB4", // 緑
        "#FFEAA7", // 黄色
        "#DDA0DD", // プラム
        "#98D8C8", // ミント
        "#F7DC6F", // ゴールド
        "#BB8FCE", // ラベンダー
        "#85C1E9", // スカイブルー
        "#F8C471", // オレンジ
        "#82E0AA", // ライトグリーン
        "#F1948A", // ピンク
        "#85C1E9", // ライトブルー
        "#FAD7A0", // ピーチ
        "#D7BDE2", // ライトパープル
        "#A9CCE3", // ベビーブルー
        "#F9E79F", // ライトイエロー
        "#D5A6BD", // ローズ
        "#A2D9CE", // アクア
        "#F5B7B1", // サルモン
        "#D2B4DE", // ライトラベンダー
        "#AED6F1", // パウダーブルー
        "#FADBD8", // ライトピンク
        "#D5F4E6"  // ライトミント
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feed_color_settings)
        
        feedId = intent.getStringExtra(EXTRA_FEED_ID) ?: ""
        feedTitle = intent.getStringExtra(EXTRA_FEED_TITLE) ?: ""
        selectedColor = intent.getStringExtra(EXTRA_CURRENT_FEED_COLOR)
        
        title = "$feedTitle の色設定"
        
        initializeViews()
        setupColorGrid()
        setupButtons()
        updatePreview()
    }
    
    private fun initializeViews() {
        colorGrid = findViewById(R.id.color_grid)
        preview = findViewById(R.id.preview)
        saveButton = findViewById(R.id.save_button)
        resetButton = findViewById(R.id.reset_button)
    }
    
    private fun setupColorGrid() {
        setupColorGrid(colorGrid, selectedColor) { color ->
            selectedColor = color
            updatePreview()
        }
    }
    
    private fun setupColorGrid(gridLayout: GridLayout, selectedColor: String?, onColorSelected: (String) -> Unit) {
        val columns = 5
        val rows = (presetColors.size + columns - 1) / columns
        
        gridLayout.columnCount = columns
        gridLayout.rowCount = rows
        
        presetColors.forEachIndexed { index, color ->
            val colorButton = createColorButton(color, color == selectedColor) {
                onColorSelected(color)
                updateButtonSelection(gridLayout, color)
            }
            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = resources.getDimensionPixelSize(R.dimen.color_button_size)
                columnSpec = GridLayout.spec(index % columns, 1f)
                rowSpec = GridLayout.spec(index / columns)
                setMargins(8, 8, 8, 8)
            }
            gridLayout.addView(colorButton, params)
        }
    }
    
    private fun createColorButton(color: String, isSelected: Boolean, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = ""
            setBackgroundColor(Color.parseColor(color))
            tag = color // 色をタグとして保存
            
            setOnClickListener {
                onClick()
            }
        }
    }
    
    private fun updateButtonSelection(gridLayout: GridLayout, selectedColor: String) {
        for (i in 0 until gridLayout.childCount) {
            val child = gridLayout.getChildAt(i) as? Button
            child?.let {
                val buttonColor = it.tag as? String
                it.isSelected = buttonColor == selectedColor
            }
        }
    }
    
    private fun setupButtons() {
        saveButton.setOnClickListener {
            saveColorSettings()
        }
        
        resetButton.setOnClickListener {
            selectedColor = null
            updatePreview()
            // リセット時は選択状態をクリア
            for (i in 0 until colorGrid.childCount) {
                (colorGrid.getChildAt(i) as? Button)?.isSelected = false
            }
        }
    }
    
    private fun updatePreview() {
        // プレビュー（フィード名 + 更新時刻）
        preview.text = "$feedTitle • 1時間前"
        preview.setTextColor(
            if (selectedColor != null) Color.parseColor(selectedColor) 
            else Color.WHITE
        )
    }
    
    private fun saveColorSettings() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getDatabase(this@FeedColorSettingsActivity)
                val repository = RssRepository(
                    database.rssFeedDao(),
                    database.rssArticleDao(),
                    database.widgetSettingsDao(),
                    createRssApiService()
                )
                
                repository.updateFeedColor(feedId, selectedColor)
                
                // すべてのウィジェットを更新
                updateAllWidgets()
                
                withContext(Dispatchers.Main) {
                    val resultIntent = Intent().apply {
                        putExtra(RESULT_FEED_COLOR, selectedColor)
                    }
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        this@FeedColorSettingsActivity,
                        "色設定の保存に失敗しました: ${e.message}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    private fun updateAllWidgets() {
        try {
            val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(this)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                android.content.ComponentName(this, com.example.myrssfeed.widget.RssWidgetProvider::class.java)
            )
            
            if (appWidgetIds.isNotEmpty()) {
                android.util.Log.d("FeedColorSettings", "Updating ${appWidgetIds.size} widgets")
                
                // 各ウィジェットのデータを更新
                appWidgetIds.forEach { widgetId ->
                    appWidgetManager.notifyAppWidgetViewDataChanged(widgetId, R.id.list_view)
                }
                
                // ウィジェットを更新
                val updateIntent = android.content.Intent(this, com.example.myrssfeed.widget.RssWidgetProvider::class.java).apply {
                    action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                sendBroadcast(updateIntent)
                
                // 少し遅延して再度更新（確実に反映されるように）
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    appWidgetIds.forEach { widgetId ->
                        appWidgetManager.notifyAppWidgetViewDataChanged(widgetId, R.id.list_view)
                    }
                    val updateIntent2 = android.content.Intent(this, com.example.myrssfeed.widget.RssWidgetProvider::class.java).apply {
                        action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                    }
                    sendBroadcast(updateIntent2)
                }, 1000)
            }
        } catch (e: Exception) {
            android.util.Log.e("FeedColorSettings", "Error updating widgets", e)
        }
    }
    
    private fun createRssApiService(): RssApiService {
        return Retrofit.Builder()
            .baseUrl("https://example.com/") // ダミーURL
            .addConverterFactory(SimpleXmlConverterFactory.create())
            .build()
            .create(RssApiService::class.java)
    }
} 