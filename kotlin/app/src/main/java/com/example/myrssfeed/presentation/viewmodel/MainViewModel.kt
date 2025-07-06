package com.example.myrssfeed.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myrssfeed.data.local.entity.RssFeed
import com.example.myrssfeed.data.repository.RssRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.*

class MainViewModel(
    private val repository: RssRepository
) : ViewModel() {
    
    private val _feeds = MutableStateFlow<List<RssFeed>>(emptyList())
    val feeds: StateFlow<List<RssFeed>> = _feeds
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage
    
    fun loadFeeds() {
        viewModelScope.launch {
            repository.getAllEnabledFeeds().collect { feeds ->
                _feeds.value = feeds
            }
        }
    }
    
    fun addFeed(title: String, url: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                
                val feed = RssFeed(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    url = url,
                    description = null,
                    lastUpdated = 0L,
                    updateInterval = 30, // 30分
                    isEnabled = true,
                    sortOrder = _feeds.value.size
                )
                repository.insertFeed(feed)
                
                // フィード追加後に記事を取得
                try {
                    repository.updateRssFeed(feed)
                } catch (e: Exception) {
                    _errorMessage.value = "フィードの追加は成功しましたが、記事の取得に失敗しました: ${e.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "フィードの追加に失敗しました: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun deleteFeed(feed: RssFeed) {
        viewModelScope.launch {
            repository.deleteFeed(feed)
        }
    }
    
    fun refreshFeed(feed: RssFeed) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                repository.updateRssFeed(feed)
            } catch (e: Exception) {
                _errorMessage.value = "フィードの更新に失敗しました: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun refreshAllFeeds() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                repository.refreshAllFeeds()
            } catch (e: Exception) {
                _errorMessage.value = "フィードの更新に失敗しました: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
} 