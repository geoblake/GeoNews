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
    val impactLevel: String = "MEDIUM", // "HIGH", "MEDIUM", "LOW"
    val imageUrl: String? = null,
    val videoUrl: String? = null,
    val webUrl: String? = null,
    val isFavorite: Boolean = false
) : Serializable {

    fun getOriginalNewsUrl(): String {
        if (!webUrl.isNullOrBlank() && (webUrl.startsWith("http://") || webUrl.startsWith("https://"))) {
            return webUrl
        }
        return when (source.lowercase().trim()) {
            "reuters" -> "https://www.reuters.com/world/"
            "bbc news", "bbc" -> "https://www.bbc.com/news"
            "associated press", "ap" -> "https://apnews.com/"
            "al jazeera" -> "https://www.aljazeera.com/"
            "cnn" -> "https://edition.cnn.com/"
            "tagesschau" -> "https://www.tagesschau.de/"
            "deutsche welle", "dw", "deutsche welle (dw)" -> "https://www.dw.com/es/noticias/s-30684"
            "der spiegel" -> "https://www.spiegel.de/"
            "süddeutsche zeitung" -> "https://www.sueddeutsche.de/"
            "el tiempo" -> "https://www.eltiempo.com/colombia"
            "el espectador" -> "https://www.elespectador.com/"
            "caracol radio" -> "https://caracol.com.co/"
            "revista semana", "semana" -> "https://www.semana.com/"
            else -> when (section) {
                "Colombia" -> "https://www.eltiempo.com/colombia"
                "Alemania" -> "https://www.tagesschau.de/"
                else -> "https://www.reuters.com/world/"
            }
        }
    }
}

