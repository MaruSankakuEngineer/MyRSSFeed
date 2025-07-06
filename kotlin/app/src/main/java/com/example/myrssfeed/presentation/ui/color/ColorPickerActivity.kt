package com.example.myrssfeed.presentation.ui.color

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.myrssfeed.R

class ColorPickerActivity : AppCompatActivity() {
    
    companion object {
        const val EXTRA_FEED_ID = "feed_id"
        const val EXTRA_FEED_TITLE = "feed_title"
        const val EXTRA_CURRENT_COLOR = "current_color"
        const val RESULT_COLOR = "result_color"
    }
    
    private lateinit var gridLayout: GridLayout
    private var selectedColor: String? = null
    
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
        setContentView(R.layout.activity_color_picker)
        
        val feedId = intent.getStringExtra(EXTRA_FEED_ID) ?: ""
        val feedTitle = intent.getStringExtra(EXTRA_FEED_TITLE) ?: ""
        val currentColor = intent.getStringExtra(EXTRA_CURRENT_COLOR)
        
        title = "$feedTitle の色を選択"
        
        gridLayout = findViewById(R.id.color_grid)
        selectedColor = currentColor
        
        setupColorGrid()
        setupButtons()
    }
    
    private fun setupColorGrid() {
        val columns = 5
        val rows = (presetColors.size + columns - 1) / columns
        
        gridLayout.columnCount = columns
        gridLayout.rowCount = rows
        
        presetColors.forEachIndexed { index, color ->
            val colorButton = createColorButton(color, index)
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
    
    private fun createColorButton(color: String, index: Int): Button {
        return Button(this).apply {
            text = ""
            setBackgroundColor(Color.parseColor(color))
            isSelected = color == selectedColor
            
            setOnClickListener {
                selectedColor = color
                updateButtonSelection()
            }
        }
    }
    
    private fun updateButtonSelection() {
        for (i in 0 until gridLayout.childCount) {
            val child = gridLayout.getChildAt(i)
            if (child is Button) {
                val color = presetColors[i]
                child.isSelected = color == selectedColor
                child.alpha = if (child.isSelected) 0.7f else 1.0f
            }
        }
    }
    
    private fun setupButtons() {
        findViewById<Button>(R.id.btn_apply).setOnClickListener {
            val resultIntent = Intent().apply {
                putExtra(RESULT_COLOR, selectedColor)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
        
        findViewById<Button>(R.id.btn_cancel).setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
        
        findViewById<Button>(R.id.btn_clear).setOnClickListener {
            selectedColor = null
            updateButtonSelection()
        }
    }
} 