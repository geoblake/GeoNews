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

    val favoriteNews: Flow<List<NewsArticle>> = newsDao.getFavoriteNews()

    suspend fun updateFavoriteStatus(id: Int, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        newsDao.updateFavoriteStatus(id, isFavorite)
    }

    fun getNewsBySection(section: String): Flow<List<NewsArticle>> =
        newsDao.getNewsBySection(section)

    suspend fun refreshNews(): List<NewsArticle> = withContext(Dispatchers.IO) {
        val dateFormat = SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", Locale("es", "ES"))
        val currentDateStr = dateFormat.format(Date())
        
        // Fetch new articles from Gemini service
        val remoteArticles = geminiService.fetchLatestNews(currentDateStr)
        
        if (remoteArticles.isNotEmpty()) {
            val existingArticles = newsDao.getNewsListSync()
            val filteredArticles = mutableListOf<NewsArticle>()
            
            for (remote in remoteArticles) {
                // Check if this article is similar to any existing article in DB,
                // or if we already added a similar remote article in this batch
                val isDuplicate = existingArticles.any { areArticlesSimilar(it.title, remote.title) } ||
                        filteredArticles.any { areArticlesSimilar(it.title, remote.title) }
                
                if (!isDuplicate) {
                    filteredArticles.add(remote)
                }
            }
            
            if (filteredArticles.isNotEmpty()) {
                // Save non-duplicate articles to database
                newsDao.insertArticles(filteredArticles)
            }
            
            // Return only high impact articles that we actually inserted to enable notifications
            filteredArticles.filter { it.impactLevel == "HIGH" }
        } else {
            emptyList()
        }
    }

    private fun areArticlesSimilar(title1: String, title2: String): Boolean {
        val t1 = title1.lowercase(Locale.ROOT).trim().replace(Regex("[^a-z0-9áéíóúüñ\\s]"), "")
        val t2 = title2.lowercase(Locale.ROOT).trim().replace(Regex("[^a-z0-9áéíóúüñ\\s]"), "")
        if (t1 == t2) return true

        val stopWords = setOf(
            "el", "la", "los", "las", "un", "una", "unos", "unas",
            "de", "del", "en", "para", "por", "con", "sin", "sobre",
            "y", "o", "u", "e", "a", "al", "que", "se", "su", "sus",
            "este", "esta", "estos", "estas", "este", "es", "son", "con"
        )
        val tokens1 = t1.split(Regex("\\s+")).filter { it.length > 2 && it !in stopWords }.toSet()
        val tokens2 = t2.split(Regex("\\s+")).filter { it.length > 2 && it !in stopWords }.toSet()

        if (tokens1.isEmpty() || tokens2.isEmpty()) return false

        val commonTokens = tokens1.intersect(tokens2)
        val smallerSize = minOf(tokens1.size, tokens2.size)
        if (smallerSize == 0) return false

        val similarity = commonTokens.size.toDouble() / smallerSize
        return similarity >= 0.55
    }

    suspend fun deleteNewsOlderThan(days: Int) = withContext(Dispatchers.IO) {
        val cutoffTime = System.currentTimeMillis() - (days.toLong() * 24L * 60L * 60L * 1000L)
        newsDao.deleteNewsOlderThan(cutoffTime)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        newsDao.clearAllNews()
    }
}
