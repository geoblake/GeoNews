package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "news_articles")
data class NewsArticle(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val content: String,
    val section: String, // "Mundo", "Alemania", "Colombia"
    val source: String, // e.g. BBC, Tagesschau, El Tiempo
    val timestamp: Long = System.currentTimeMillis(),
    val impactLevel: String = "MEDIUM" // "HIGH", "MEDIUM", "LOW"
) : Serializable
