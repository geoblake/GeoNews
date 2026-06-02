package com.example.data.local

import androidx.room.*
import com.example.data.model.NewsArticle
import kotlinx.coroutines.flow.Flow

@Dao
interface NewsDao {
    @Query("SELECT * FROM news_articles ORDER BY timestamp DESC")
    fun getAllNews(): Flow<List<NewsArticle>>

    @Query("SELECT * FROM news_articles")
    suspend fun getNewsListSync(): List<NewsArticle>

    @Query("SELECT * FROM news_articles WHERE section = :section ORDER BY timestamp DESC")
    fun getNewsBySection(section: String): Flow<List<NewsArticle>>

    @Query("SELECT * FROM news_articles WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteNews(): Flow<List<NewsArticle>>

    @Query("UPDATE news_articles SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Int, isFavorite: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticles(articles: List<NewsArticle>)

    @Query("DELETE FROM news_articles WHERE timestamp < :cutoffTime")
    suspend fun deleteNewsOlderThan(cutoffTime: Long)

    @Query("DELETE FROM news_articles")
    suspend fun clearAllNews()

    @Query("SELECT COUNT(*) FROM news_articles")
    suspend fun getNewsCount(): Int
}
