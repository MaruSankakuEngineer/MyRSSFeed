package com.example.myrssfeed.presentation.ui.filter

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myrssfeed.data.local.database.AppDatabase
import com.example.myrssfeed.data.repository.RssRepository
import com.example.myrssfeed.data.remote.api.RssApiService
import com.example.myrssfeed.presentation.viewmodel.FilterViewModel
import com.example.myrssfeed.widget.RssWidgetProvider
import retrofit2.Retrofit
import retrofit2.converter.simplexml.SimpleXmlConverterFactory

class FilterActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        
        val database = AppDatabase.getDatabase(this)
        val repository = RssRepository(
            database.rssFeedDao(),
            database.rssArticleDao(),
            database.widgetSettingsDao(),
            createRssApiService()
        )
        
        setContent {
            MaterialTheme {
                FilterScreen(
                    appWidgetId = appWidgetId,
                    repository = repository,
                    onFilterSelected = { selectedFeedId ->
                        // フィルター設定を保存
                        // ウィジェットを更新
                        val appWidgetManager = AppWidgetManager.getInstance(this)
                        val intent = Intent(this, RssWidgetProvider::class.java).apply {
                            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
                        }
                        sendBroadcast(intent)
                        
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                )
            }
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

@Composable
fun FilterScreen(
    appWidgetId: Int,
    repository: RssRepository,
    onFilterSelected: (String?) -> Unit
) {
    val factory = remember(appWidgetId, repository) {
        object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return FilterViewModel(repository, appWidgetId) as T
            }
        }
    }
    val viewModel: FilterViewModel = viewModel(factory = factory)
    
    val feeds by viewModel.feeds.collectAsState(initial = emptyList())
    val selectedFeedId by viewModel.selectedFeedId.collectAsState(initial = null)
    val isRefreshing by viewModel.isRefreshing.collectAsState(initial = false)
    
    // ウィジェット更新のコールバックを設定
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.setWidgetUpdateCallback {
            android.util.Log.d("FilterActivity", "Updating widget $appWidgetId")
            val intent = android.content.Intent(context, RssWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
            }
            context.sendBroadcast(intent)
            
            // 少し遅延して再度更新（確実に反映されるように）
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                val intent2 = android.content.Intent(context, RssWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
                }
                context.sendBroadcast(intent2)
            }, 1000)
        }
        viewModel.loadFeeds()
        viewModel.loadCurrentSettings()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // ヘッダー部分（タイトルと手動更新ボタン）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "フィルター選択",
                style = MaterialTheme.typography.headlineSmall
            )
            
            IconButton(
                onClick = { 
                    viewModel.refreshFeeds()
                },
                enabled = !isRefreshing
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "手動更新"
                    )
                }
            }
        }
        
        LazyColumn {
            // すべての記事
            item {
                FilterItem(
                    title = "すべての記事",
                    subtitle = "すべてのフィードの記事を表示",
                    isSelected = selectedFeedId == null,
                    onClick = {
                        android.util.Log.d("FilterActivity", "All articles selected")
                        viewModel.updateSelectedFeed(null)
                        onFilterSelected(null)
                    }
                )
            }
            
            // 各フィード
            items(feeds) { feed ->
                FilterItem(
                    title = feed.title ?: "",
                    subtitle = feed.description ?: "",
                    isSelected = selectedFeedId == feed.id,
                    onClick = {
                        android.util.Log.d("FilterActivity", "Feed selected: ${feed.id}")
                        viewModel.updateSelectedFeed(feed.id)
                        onFilterSelected(feed.id)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterItem(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
} 