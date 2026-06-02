package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.data.local.NewsDatabase
import com.example.data.remote.GeminiNewsService
import com.example.data.repository.NewsRepository
import com.example.ui.NewsViewModel
import com.example.ui.components.NewsScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize dependencies (Constructor Injection)
        val database = NewsDatabase.getDatabase(applicationContext)
        val repository = NewsRepository(database.newsDao, GeminiNewsService())
        val viewModel = NewsViewModel(application, repository)

        setContent {
            MyApplicationTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NewsScreen(viewModel = viewModel)
                }
            }
        }
    }
}
