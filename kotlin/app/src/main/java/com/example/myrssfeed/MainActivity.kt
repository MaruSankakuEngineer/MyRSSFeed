package com.example.myrssfeed

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myrssfeed.data.local.database.AppDatabase
import com.example.myrssfeed.data.repository.RssRepository
import com.example.myrssfeed.data.remote.api.RssApiService
import com.example.myrssfeed.presentation.viewmodel.MainViewModel
import retrofit2.Retrofit
import retrofit2.converter.simplexml.SimpleXmlConverterFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val database = AppDatabase.getDatabase(this)
        val repository = RssRepository(
            database.rssFeedDao(),
            database.rssArticleDao(),
            database.widgetSettingsDao(),
            createRssApiService()
        )
        
        setContent {
            MaterialTheme {
                MainScreen(repository = repository)
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
fun MainScreen(repository: RssRepository) {
    val viewModel: MainViewModel = viewModel {
        MainViewModel(repository)
    }
    
    val feeds by viewModel.feeds.collectAsState(initial = emptyList())
    val isLoading by viewModel.isLoading.collectAsState(initial = false)
    val errorMessage by viewModel.errorMessage.collectAsState(initial = null)
    var showAddDialog by remember { mutableStateOf(false) }
    var newFeedUrl by remember { mutableStateOf("") }
    var newFeedTitle by remember { mutableStateOf("") }
    
    LaunchedEffect(Unit) {
        viewModel.loadFeeds()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "RSSフィード設定",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // エラーメッセージ表示
        errorMessage?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("×", color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }
        
        // 全フィード更新ボタン
        Button(
            onClick = { viewModel.refreshAllFeeds() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(if (isLoading) "更新中..." else "全フィード更新")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(feeds) { feed ->
                FeedItem(
                    feed = feed,
                    onDelete = { viewModel.deleteFeed(feed) },
                    onRefresh = { viewModel.refreshFeed(feed) },
                    isLoading = isLoading
                )
            }
        }
        
        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text("フィードを追加")
        }
    }
    
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("フィードを追加") },
            text = {
                Column {
                    TextField(
                        value = newFeedTitle,
                        onValueChange = { newFeedTitle = it },
                        label = { Text("タイトル") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = newFeedUrl,
                        onValueChange = { newFeedUrl = it },
                        label = { Text("URL") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newFeedTitle.isNotEmpty() && newFeedUrl.isNotEmpty()) {
                            viewModel.addFeed(newFeedTitle, newFeedUrl)
                            newFeedTitle = ""
                            newFeedUrl = ""
                            showAddDialog = false
                        }
                    },
                    enabled = !isLoading
                ) {
                    Text("追加")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }
}

@Composable
fun FeedItem(
    feed: com.example.myrssfeed.data.local.entity.RssFeed,
    onDelete: () -> Unit,
    onRefresh: () -> Unit,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = feed.title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = feed.url,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row {
                IconButton(
                    onClick = onRefresh,
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icons.Default.Refresh
                    }
                }
                TextButton(onClick = onDelete) {
                    Text("削除")
                }
            }
        }
    }
}