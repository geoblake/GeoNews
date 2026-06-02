package com.example.data.repository

import com.example.data.local.NewsDao
import com.example.data.model.NewsArticle
import com.example.data.remote.GeminiNewsService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NewsRepository(
    private val newsDao: NewsDao,
    private val geminiService: GeminiNewsService
) {
    val allNews: Flow<List<NewsArticle>> = newsDao.getAllNews()

    fun getNewsBySection(section: String): Flow<List<NewsArticle>> =
        newsDao.getNewsBySection(section)

    suspend fun refreshNews(): List<NewsArticle> = withContext(Dispatchers.IO) {
        val dateFormat = SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", Locale("es", "ES"))
        val currentDateStr = dateFormat.format(Date())
        
        // Fetch new articles from Gemini service
        val remoteArticles = geminiService.fetchLatestNews(currentDateStr)
        
        if (remoteArticles.isNotEmpty()) {
            // Save them to database
            newsDao.insertArticles(remoteArticles)
        }
        
        // Return only high impact articles that we just fetched to enable notifications
        remoteArticles.filter { it.impactLevel == "HIGH" }
    }

    suspend fun deleteNewsOlderThan(days: Int) = withContext(Dispatchers.IO) {
        val cutoffTime = System.currentTimeMillis() - (days.toLong() * 24L * 60L * 60L * 1000L)
        newsDao.deleteNewsOlderThan(cutoffTime)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        newsDao.clearAllNews()
    }
}
