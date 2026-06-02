package com.example.ui

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.NewsArticle
import com.example.data.repository.NewsRepository
import com.example.util.NotificationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class NewsViewModel(
    application: Application,
    private val repository: NewsRepository
) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences("news_prefs", Context.MODE_PRIVATE)
    private val notificationHelper = NotificationHelper(application)

    // Current filter section ("Todos", "Mundo", "Alemania", "Colombia")
    private val _selectedSection = MutableStateFlow("Todos")
    val selectedSection: StateFlow<String> = _selectedSection.asStateFlow()

    // Refreshing loading state
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // Last update helper
    private val _lastUpdate = MutableStateFlow(sharedPrefs.getLong("last_update", 0L))
    val lastUpdate: StateFlow<Long> = _lastUpdate.asStateFlow()

    // Current update frequency in minutes (15, 30, 60, 180, 1440)
    private val _updateFrequencyMinutes = MutableStateFlow(sharedPrefs.getInt("update_frequency", 60))
    val updateFrequencyMinutes: StateFlow<Int> = _updateFrequencyMinutes.asStateFlow()

    // Error events
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Active detail views
    private val _selectedArticle = MutableStateFlow<NewsArticle?>(null)
    val selectedArticle: StateFlow<NewsArticle?> = _selectedArticle.asStateFlow()

    // Collect list of news in real-time, filtered by section
    val filteredNewsList: StateFlow<List<NewsArticle>> = combine(
        repository.allNews,
        _selectedSection
    ) { allNews, section ->
        if (section == "Todos") {
            allNews
        } else {
            allNews.filter { it.section.equals(section, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Number of articles currently stored locally
    private val _localArticleCount = MutableStateFlow(0)
    val localArticleCount: StateFlow<Int> = _localArticleCount.asStateFlow()

    private var autoUpdateJob: Job? = null

    init {
        updateLocalCount()
        startAutomaticTicker()
        
        // Initial fetch if database is empty
        viewModelScope.launch {
            repository.allNews.first().let { currentList ->
                if (currentList.isEmpty()) {
                    triggerNewsRefresh(isManual = false)
                }
            }
        }
    }

    private fun updateLocalCount() {
        viewModelScope.launch {
            repository.allNews.collect { list ->
                _localArticleCount.value = list.size
            }
        }
    }

    fun setSection(section: String) {
        _selectedSection.value = section
    }

    fun selectArticle(article: NewsArticle?) {
        _selectedArticle.value = article
    }

    fun setUpdateFrequency(minutes: Int) {
        _updateFrequencyMinutes.value = minutes
        sharedPrefs.edit().putInt("update_frequency", minutes).apply()
        // Restart ticker with the updated schedule
        startAutomaticTicker()
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    // Trigger full manual or automatic updates
    fun triggerNewsRefresh(isManual: Boolean) {
        if (_isRefreshing.value) return
        
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                // Fetch new articles (returns any newly fetched articles classified as HIGH impact)
                val highImpactArticles = repository.refreshNews()
                
                // Track update timestamp
                val now = System.currentTimeMillis()
                _lastUpdate.value = now
                sharedPrefs.edit().putLong("last_update", now).apply()

                // Dispatch system notification for high impact news
                if (highImpactArticles.isNotEmpty()) {
                    // Send notification for the most interest item
                    val topAlert = highImpactArticles.first()
                    notificationHelper.sendNewsNotification(
                        title = topAlert.title,
                        description = topAlert.description,
                        section = topAlert.section
                    )
                } else if (isManual && filteredNewsList.value.isNotEmpty()) {
                    // Quick alert simulation for high impact if manually refreshed and requested (optional)
                    val sampleHigh = filteredNewsList.value.firstOrNull { it.impactLevel == "HIGH" }
                    if (sampleHigh != null) {
                        notificationHelper.sendNewsNotification(
                            title = sampleHigh.title,
                            description = sampleHigh.description,
                            section = sampleHigh.section
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("NewsViewModel", "Error refreshing news", e)
                _errorMessage.value = "Error de red al actualizar noticias: ${e.localizedMessage ?: "Consulte su conexión"}"
            } finally {
                _isRefreshing.value = false
                updateLocalCount()
            }
        }
    }

    // Perform selective database deletion
    fun deleteHistoryByPeriod(days: Int) {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                if (days == 0) {
                    repository.clearAll()
                } else {
                    repository.deleteNewsOlderThan(days)
                }
                updateLocalCount()
            } catch (e: Exception) {
                Log.e("NewsViewModel", "Error clearing history", e)
                _errorMessage.value = "Error al limpiar el historial"
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    // Dynamic auto background update ticker when app session is active
    private fun startAutomaticTicker() {
        autoUpdateJob?.cancel()
        autoUpdateJob = viewModelScope.launch {
            while (isActive) {
                val lastTime = _lastUpdate.value
                val frequencyMs = _updateFrequencyMinutes.value.toLong() * 60L * 1000L
                val timePassed = System.currentTimeMillis() - lastTime
                
                if (lastTime > 0L && timePassed >= frequencyMs) {
                    Log.i("NewsViewModel", "Interval elapsed! Automatically refreshing news background.")
                    triggerNewsRefresh(isManual = false)
                }
                // Check once every 10 seconds
                delay(10000L)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        autoUpdateJob?.cancel()
    }
}
