package com.example.myrssfeed.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myrssfeed.data.local.entity.RssFeed
import com.example.myrssfeed.data.local.entity.WidgetSettings
import com.example.myrssfeed.data.repository.RssRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FilterViewModel(
    private val repository: RssRepository,
    private val appWidgetId: Int
) : ViewModel() {
    
    private val _feeds = MutableStateFlow<List<RssFeed>>(emptyList())
    val feeds: StateFlow<List<RssFeed>> = _feeds
    
    private val _selectedFeedId = MutableStateFlow<String?>(null)
    val selectedFeedId: StateFlow<String?> = _selectedFeedId
    
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing
    
    private val _onWidgetUpdate = MutableStateFlow<(() -> Unit)?>(null)
    val onWidgetUpdate: StateFlow<(() -> Unit)?> = _onWidgetUpdate
    
    fun setWidgetUpdateCallback(callback: () -> Unit) {
        _onWidgetUpdate.value = callback
    }
    
    fun loadFeeds() {
        viewModelScope.launch {
            repository.getAllEnabledFeeds().collect { feeds ->
                _feeds.value = feeds
            }
        }
    }
    
    fun loadCurrentSettings() {
        viewModelScope.launch {
            val settings = repository.getWidgetSettings(appWidgetId)
            _selectedFeedId.value = settings?.selectedFeedId
        }
    }
    
    fun updateSelectedFeed(feedId: String?) {
        viewModelScope.launch {
            android.util.Log.d("FilterViewModel", "Updating selected feed to: $feedId")
            repository.updateSelectedFeed(appWidgetId, feedId)
            _selectedFeedId.value = feedId
            
            // 設定更新後にウィジェットを更新
            _onWidgetUpdate.value?.invoke()
            
            android.util.Log.d("FilterViewModel", "Widget update callback invoked")
        }
    }
    
    fun refreshFeeds() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                repository.refreshAllFeeds()
                _onWidgetUpdate.value?.invoke()
            } finally {
                _isRefreshing.value = false
            }
        }
    }
} 