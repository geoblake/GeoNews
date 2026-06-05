package com.example.ui.components

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.NewsArticle
import com.example.ui.NewsViewModel
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.compose.ui.viewinterop.AndroidView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsScreen(
    viewModel: NewsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val newsList by viewModel.filteredNewsList.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val selectedSection by viewModel.selectedSection.collectAsStateWithLifecycle()
    val lastUpdate by viewModel.lastUpdate.collectAsStateWithLifecycle()
    val updateFrequencyMinutes by viewModel.updateFrequencyMinutes.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val selectedArticle by viewModel.selectedArticle.collectAsStateWithLifecycle()
    val localArticleCount by viewModel.localArticleCount.collectAsStateWithLifecycle()
    val dismissedAlerts by viewModel.dismissedAlertIds.collectAsStateWithLifecycle()
    val favoriteNewsList by viewModel.favoriteNewsList.collectAsStateWithLifecycle()
    val isApiKeyMissing by viewModel.isApiKeyMissing.collectAsStateWithLifecycle()
    val customApiKey by viewModel.customApiKey.collectAsStateWithLifecycle()
    var activeTab by remember { mutableStateOf("feed") } // "feed" or "favorites"

    var showSettingsSheet by remember { mutableStateOf(false) }
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    // Permission launcher for Android 13+ notifications
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    // Trigger permission dialog on mount if needed
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Core High Density Theme colors
    val bgPrimary = Color(0xFF1C1B1F)
    val bgCard = Color(0xFF2B2930)
    val borderColor = Color(0xFF49454F)
    val accentPurple = Color(0xFFD0BCFF)
    val bgPurpleContainer = Color(0xFF381E72)
    val textPrimary = Color(0xFFE6E1E5)
    val textSecondary = Color(0xFFCAC4D0)

    Scaffold(
        modifier = modifier.background(bgPrimary),
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = if (activeTab == "feed") "GeoNews Pro" else "Favoritas Guardadas",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary,
                                letterSpacing = 1.sp
                            )
                            val lastUpdateText = if (lastUpdate > 0L) {
                                val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                                "Actualizado: ${sdf.format(Date(lastUpdate))}"
                            } else {
                                "Sin actualizar"
                            }
                            Text(
                                text = lastUpdateText,
                                fontSize = 11.sp,
                                color = textSecondary
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.triggerNewsRefresh(isManual = true) },
                            modifier = Modifier.testTag("refresh_button")
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = accentPurple,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Actualizar Noticias",
                                    tint = textPrimary
                                )
                            }
                        }
                        IconButton(
                            onClick = { showSettingsSheet = true },
                            modifier = Modifier.testTag("settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Limpieza y Configuración",
                                tint = textPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = bgPrimary,
                        titleContentColor = textPrimary,
                        actionIconContentColor = textPrimary
                    )
                )
                HorizontalDivider(color = borderColor, thickness = 1.dp)
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = bgCard,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = activeTab == "feed",
                    onClick = { activeTab = "feed" },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Feed") },
                    label = { Text("Noticias") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = bgPrimary,
                        selectedTextColor = accentPurple,
                        indicatorColor = accentPurple,
                        unselectedIconColor = textSecondary,
                        unselectedTextColor = textSecondary
                    ),
                    modifier = Modifier.testTag("tab_noticias")
                )
                NavigationBarItem(
                    selected = activeTab == "favorites",
                    onClick = { activeTab = "favorites" },
                    icon = { Icon(Icons.Default.Favorite, contentDescription = "Favoritos") },
                    label = { Text("Favoritos") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = bgPrimary,
                        selectedTextColor = accentPurple,
                        indicatorColor = accentPurple,
                        unselectedIconColor = textSecondary,
                        unselectedTextColor = textSecondary
                    ),
                    modifier = Modifier.testTag("tab_favoritos")
                )
            }
        },
        containerColor = bgPrimary
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Error Snackbar
            errorMessage?.let { errorMsg ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF31111D)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF8C1D18))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Error",
                                tint = Color(0xFFF2B8B5),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMsg,
                                color = Color(0xFFF2B8B5),
                                fontSize = 13.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = { viewModel.clearErrorMessage() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Gemini API Key Warning Banner
            if (isApiKeyMissing) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2216)), // Warm amber/orange warning
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFD3A13B))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(Color(0xFFD3A13B), RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Simulaciones Activas",
                                    tint = Color(0xFF1C1B1F),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = "Modo de Simulaciones Activo",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF0E1B9),
                                fontSize = 13.sp
                            )
                        }
                        Text(
                            text = "Para disfrutar de noticias reales del mundo real en tiempo real (de 2026), por favor configure una API Key válida de Gemini en la barra de Ajustes (icono de engranaje arriba a la derecha).",
                            color = Color(0xFFCAC4D0),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                        Button(
                            onClick = { showSettingsSheet = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD3A13B)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(28.dp).align(Alignment.End)
                        ) {
                            Text(
                                "Configurar Key",
                                color = Color(0xFF1C1B1F),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Notification permission warning banner for SDK 33+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF31111D)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF8C1D18))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color(0xFF8C1D18), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Habilitar Notificaciones",
                                tint = Color(0xFFF2B8B5),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Notificaciones deshabilitadas",
                                fontWeight = FontWeight.SemiBold,
                                color = textPrimary,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Haz clic para dar permisos y recibir titulares de alto impacto.",
                                color = Color(0xFFF2B8B5),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            if (activeTab == "feed") {
                // Horizontal Category Filter Layout
                SectionSelectors(
                    selectedSection = selectedSection,
                    onSectionSelected = { viewModel.setSection(it) },
                    accentPurple = accentPurple,
                    textSecondary = textSecondary
                )

                // Breaking News Alert banner at top of feed
                val topHighImpactArticle = newsList.firstOrNull { it.impactLevel == "HIGH" && it.id !in dismissedAlerts }
                if (topHighImpactArticle != null) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF31111D))
                            .border(1.dp, Color(0xFF8C1D18), RoundedCornerShape(16.dp))
                            .clickable { viewModel.selectArticle(topHighImpactArticle) }
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFF8C1D18), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Alerta",
                                    tint = Color(0xFFF2B8B5),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "PUSH ALERT: BREAKING",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF2B8B5),
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = topHighImpactArticle.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { viewModel.dismissAlert(topHighImpactArticle.id) },
                                modifier = Modifier.size(28.dp).testTag("dismiss_push_alert_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cerrar Alerta",
                                    tint = Color(0xFFF2B8B5),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // News scroll layout
                if (newsList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Sin Noticias",
                                tint = textSecondary,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No hay noticias disponibles",
                                fontWeight = FontWeight.Bold,
                                color = textPrimary,
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Presiona 'Actualizar' arriba para consultar los titulares usando Gemini AI.",
                                color = textSecondary,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("news_list"),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(newsList) { article ->
                            NewsArticleCard(
                                article = article,
                                bgCardColor = bgCard,
                                accentPurple = accentPurple,
                                bgPurpleContainer = bgPurpleContainer,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                onClick = { viewModel.selectArticle(article) },
                                showFavoriteButton = true,
                                onFavoriteToggle = { viewModel.toggleFavorite(article) }
                            )
                        }

                        item {
                            // Quick informative bottom disclaimer
                            Text(
                                text = "Fuentes analizadas: Tagesschau, DW, Spiegel, El Tiempo, El Espectador, Reuters, BBC.\nTodos los datos son gestionados de forma local.",
                                fontSize = 11.sp,
                                color = textSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 24.dp, bottom = 12.dp)
                            )
                        }
                    }
                }
            } else {
                // Header Area for Favorites Tab
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Favoritas Guardadas",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentPurple
                    )
                    Box(
                        modifier = Modifier
                            .background(bgPurpleContainer, RoundedCornerShape(100.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        val favCount = favoriteNewsList.size
                        Text(
                            text = if (favCount == 1) "1 FAVORITA" else "$favCount FAVORITAS",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentPurple
                        )
                    }
                }

                // Favorites scroll layout
                if (favoriteNewsList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FavoriteBorder,
                                contentDescription = "Sin Favoritos",
                                tint = textSecondary,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Sin favoritas guardadas",
                                fontWeight = FontWeight.Bold,
                                color = textPrimary,
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Los artículos que guardes haciendo clic en el corazón se mostrarán aquí de forma persistente.",
                                color = textSecondary,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("favorites_list"),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(favoriteNewsList) { article ->
                            NewsArticleCard(
                                article = article,
                                bgCardColor = bgCard,
                                accentPurple = accentPurple,
                                bgPurpleContainer = bgPurpleContainer,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                onClick = { viewModel.selectArticle(article) },
                                showFavoriteButton = true,
                                onFavoriteToggle = { viewModel.toggleFavorite(article) }
                            )
                        }

                        item {
                            Text(
                                text = "Presione el icono del corazón para eliminar elementos de esta pestaña.",
                                fontSize = 11.sp,
                                color = textSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 24.dp, bottom = 12.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Floating Details Reader Dialog
    selectedArticle?.let { article ->
        NewsDetailsDialog(
            article = article,
            isFavorite = article.isFavorite,
            onToggleFavorite = { viewModel.toggleFavorite(article) },
            bgPrimary = bgPrimary,
            bgCard = bgCard,
            accentPurple = accentPurple,
            bgPurpleContainer = bgPurpleContainer,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            onClose = { viewModel.selectArticle(null) }
        )
    }

    // Dynamic Storage and Refresh Interval Sheet
    if (showSettingsSheet) {
        SettingsDialog(
            currentFrequency = updateFrequencyMinutes,
            totalCached = localArticleCount,
            customApiKey = customApiKey,
            bgPrimary = bgPrimary,
            bgCard = bgCard,
            borderColor = borderColor,
            accentPurple = accentPurple,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            onFrequencyChange = { viewModel.setUpdateFrequency(it) },
            onSaveApiKey = { viewModel.saveCustomApiKey(it) },
            onPurge = { periodDays ->
                viewModel.deleteHistoryByPeriod(periodDays)
            },
            onDismiss = { showSettingsSheet = false }
        )
    }
}

@Composable
fun SectionSelectors(
    selectedSection: String,
    onSectionSelected: (String) -> Unit,
    accentPurple: Color,
    textSecondary: Color
) {
    val sections = listOf(
        "Todos" to "TODAS",
        "Mundo" to "WORLD",
        "Alemania" to "GERMANY",
        "Colombia" to "COLOMBIA"
    )

    Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF1C1B1F))) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            sections.forEach { (id, label) ->
                val isSelected = selectedSection == id
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSectionSelected(id) }
                        .padding(vertical = 12.dp)
                        .testTag("section_chip_$id"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) accentPurple else textSecondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    // Bottom indicator line precisely as High Density theme
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(2.5.dp)
                            .background(if (isSelected) accentPurple else Color.Transparent)
                    )
                }
            }
        }
    }
}

@Composable
fun EmbeddedVideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val exoPlayer = remember(videoUrl) {
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .setAllowCrossProtocolRedirects(true)

        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(dataSourceFactory)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build().apply {
                addListener(object : androidx.media3.common.Player.Listener {
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        hasError = true
                        val errorCause = error.cause?.localizedMessage ?: error.message ?: ""
                        errorMessage = "Código: ${error.errorCodeName} (${error.errorCode})\nDetalle: $errorCause"
                    }

                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == androidx.media3.common.Player.STATE_READY) {
                            hasError = false
                        }
                    }
                })
                val mediaItem = MediaItem.fromUri(videoUrl)
                setMediaItem(mediaItem)
                prepare()
                playWhenReady = true
                repeatMode = ExoPlayer.REPEAT_MODE_ONE
            }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF49454F).copy(alpha = 0.5f))
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (hasError) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error de video",
                        tint = Color(0xFFF2B8B5),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No se pudo cargar el video",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = errorMessage,
                        color = Color.Gray,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            hasError = false
                            exoPlayer.prepare()
                            exoPlayer.play()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF49454F)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Reintentar", fontSize = 11.sp, color = Color.White)
                    }
                }
            } else {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = true
                            layoutParams = android.view.ViewGroup.LayoutParams(
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun NewsArticleCard(
    article: NewsArticle,
    bgCardColor: Color,
    accentPurple: Color,
    bgPurpleContainer: Color,
    textPrimary: Color,
    textSecondary: Color,
    onClick: () -> Unit,
    showFavoriteButton: Boolean = true,
    onFavoriteToggle: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("news_article_card_${article.title.replace(" ", "_")}"),
        colors = CardDefaults.cardColors(containerColor = bgCardColor),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF49454F).copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Card Header: Source, Section & Time with inline Favoritar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(bgPurpleContainer, RoundedCornerShape(100.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = article.source.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentPurple
                            )
                        }
                        Text(
                            text = article.section.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = textSecondary
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                        Text(
                            text = timeFormat.format(Date(article.timestamp)),
                            fontSize = 10.sp,
                            color = textSecondary
                        )

                        if (showFavoriteButton && onFavoriteToggle != null) {
                            IconButton(
                                onClick = { onFavoriteToggle() },
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("fav_toggle_${article.id}")
                            ) {
                                Icon(
                                    imageVector = if (article.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Favoritar",
                                    tint = if (article.isFavorite) Color(0xFFF2B8B5) else textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Body Headline title
                Text(
                    text = article.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Short summary snippet
                Text(
                    text = article.description,
                    fontSize = 12.sp,
                    color = textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Card Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (article.impactLevel == "HIGH") {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF31111D), RoundedCornerShape(100.dp))
                                .border(1.dp, Color(0xFF8C1D18), RoundedCornerShape(100.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .background(Color(0xFFF2B8B5), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "BLITZ / BREAKING",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFF2B8B5)
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF1C1B1F), RoundedCornerShape(100.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "REGULAR",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = textSecondary
                            )
                        }
                    }

                    Text(
                        text = "Leer Más →",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentPurple
                    )
                }
            }

            // Thumbnail on the right hand side
            if (!article.imageUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF100F11)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = article.imageUrl,
                        contentDescription = "Imagen de la noticia",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    if (!article.videoUrl.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Noticia con Video",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NewsDetailsDialog(
    article: NewsArticle,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    bgPrimary: Color,
    bgCard: Color,
    accentPurple: Color,
    bgPurpleContainer: Color,
    textPrimary: Color,
    textSecondary: Color,
    onClose: () -> Unit
) {
    Dialog(onDismissRequest = { onClose() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("news_detail_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = bgCard),
            border = BorderStroke(1.dp, Color(0xFF49454F))
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                item {
                    // Header Area
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val sectionEmoji = when (article.section) {
                            "Mundo" -> "🌍"
                            "Alemania" -> "🇩🇪"
                            "Colombia" -> "🇨🇴"
                            else -> "📰"
                        }
                        Box(
                            modifier = Modifier
                                .background(bgPurpleContainer, RoundedCornerShape(100.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "$sectionEmoji ${article.section.uppercase()}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentPurple
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Heart Favorite/Bookmark Button
                            IconButton(
                                onClick = onToggleFavorite,
                                modifier = Modifier.size(32.dp).testTag("dialog_favorite_btn")
                            ) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Favoritar",
                                    tint = if (isFavorite) Color(0xFFF2B8B5) else textSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            IconButton(
                                onClick = { onClose() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cerrar Diálogo",
                                    tint = textSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Media Source details and date
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = article.source,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentPurple
                        )
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .background(textSecondary, CircleShape)
                        )
                        val fullDateFormat = SimpleDateFormat("dd MMM yyyy - HH:mm", Locale("es", "ES"))
                        Text(
                            text = fullDateFormat.format(Date(article.timestamp)),
                            fontSize = 12.sp,
                            color = textSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Direct Headline
                    Text(
                        text = article.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textPrimary,
                        lineHeight = 26.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Dynamic News Cover Image (AsyncImage)
                    if (!article.imageUrl.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1C1B1F))
                        ) {
                            AsyncImage(
                                model = article.imageUrl,
                                contentDescription = "Imagen de portada",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Alert impact tags
                    if (article.impactLevel == "HIGH") {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF31111D), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF8C1D18), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color(0xFFF2B8B5), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ALERTA URGENTE: NOTICIA DE ALTO IMPACTO",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFF2B8B5)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Embedded Video Player
                    if (!article.videoUrl.isNullOrBlank()) {
                        Text(
                            text = "📺 REPORTE EN VIDEO Y MULTIMEDIA",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentPurple,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        EmbeddedVideoPlayer(
                            videoUrl = article.videoUrl,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Separation line
                    HorizontalDivider(color = Color(0xFF49454F))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Summary intro
                    Text(
                        text = article.description,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = textPrimary,
                        lineHeight = 20.sp,
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Complete core paragraphs
                    Text(
                        text = article.content,
                        fontSize = 13.sp,
                        color = textSecondary,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Justify,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    val context = LocalContext.current
                    val originalUrl = remember(article) { article.getOriginalNewsUrl() }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Action button to open URL
                        Button(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(originalUrl))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentPurple,
                                contentColor = bgCard
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("open_original_news_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Ver en navegador",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Leer noticia completa en ${article.source}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        // Close dialog button
                        OutlinedButton(
                            onClick = { onClose() },
                            border = BorderStroke(1.dp, Color(0xFF49454F)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = textPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("dismiss_dialog_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "Volver",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsDialog(
    currentFrequency: Int,
    totalCached: Int,
    customApiKey: String?,
    bgPrimary: Color,
    bgCard: Color,
    borderColor: Color,
    accentPurple: Color,
    textPrimary: Color,
    textSecondary: Color,
    onFrequencyChange: (Int) -> Unit,
    onSaveApiKey: (String?) -> Unit,
    onPurge: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var expandedDropdown by remember { mutableStateOf(false) }
    val frequencies = listOf(
        15 to "Cada 15 minutos",
        30 to "Cada 30 minutos",
        60 to "Cada hora (Recomendado)",
        180 to "Cada 3 horas",
        1440 to "Diario (Cada 24 horas)"
    )
    val curFreqLabel = frequencies.firstOrNull { it.first == currentFrequency }?.second ?: "Cada hora (Recomendado)"

    Dialog(onDismissRequest = { onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("settings_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF211F26)),
            border = BorderStroke(1.dp, borderColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header settings
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configuraciones",
                            tint = accentPurple,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ajustes y Almacenamiento",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                    }
                    IconButton(
                        onClick = { onDismiss() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar Ajustes",
                            tint = textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // API Key section
                Text(
                    text = "GEMINI API KEY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentPurple,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                
                var tempApiKey by remember { mutableStateOf(customApiKey ?: "") }
                
                OutlinedTextField(
                    value = tempApiKey,
                    onValueChange = { tempApiKey = it },
                    placeholder = { Text("Ingrese su API Key de Gemini...", color = textSecondary, fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth().testTag("api_key_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary,
                        focusedBorderColor = accentPurple,
                        unfocusedBorderColor = borderColor,
                        cursorColor = accentPurple,
                        focusedPlaceholderColor = textSecondary,
                        unfocusedPlaceholderColor = textSecondary
                    )
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { 
                            onSaveApiKey(tempApiKey)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentPurple),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("save_key_btn")
                    ) {
                        Text("Guardar Key", color = Color(0xFF1C1B1F), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    if (customApiKey != null) {
                        Button(
                            onClick = { 
                                tempApiKey = ""
                                onSaveApiKey(null)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF31111D)),
                            border = BorderStroke(1.dp, Color(0xFF8C1D18)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("delete_key_btn")
                        ) {
                            Text("Borrar", color = Color(0xFFF2B8B5), fontSize = 12.sp)
                        }
                    }
                }
                
                Text(
                    text = "Al guardar se intentará descargar noticias reales globales en tiempo real.",
                    fontSize = 10.sp,
                    color = textSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Frecuencia de actualización
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "UPDATE FREQUENCY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentPurple,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Every 1 hour (Default)",
                            fontSize = 13.sp,
                            color = textSecondary
                        )
                    }
                    
                    Box {
                        Button(
                            onClick = { expandedDropdown = true },
                            colors = ButtonDefaults.buttonColors(containerColor = borderColor),
                            shape = RoundedCornerShape(100.dp),
                            modifier = Modifier.testTag("frequency_dropdown_button")
                        ) {
                            Text(text = "Change", fontSize = 11.sp, color = textPrimary)
                        }

                        DropdownMenu(
                            expanded = expandedDropdown,
                            onDismissRequest = { expandedDropdown = false },
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .background(bgCard)
                                .border(1.dp, borderColor)
                        ) {
                            frequencies.forEach { (minutes, label) ->
                                DropdownMenuItem(
                                    text = { Text(text = label, color = textPrimary, fontSize = 13.sp) },
                                    onClick = {
                                        onFrequencyChange(minutes)
                                        expandedDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // History Management header
                Text(
                    text = "HISTORY MANAGEMENT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentPurple,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // 3 Column Grid for History Purge action items
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Past 24h Purge button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(bgCard)
                            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                            .clickable {
                                onPurge(1)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Past 24h",
                                tint = textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Past 24h",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = textPrimary
                            )
                        }
                    }

                    // Past Week Purge button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(bgCard)
                            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                            .clickable {
                                onPurge(7)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Past Week",
                                tint = textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Past Week",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = textPrimary
                            )
                        }
                    }

                    // Clear All Purge button (Crimson Red background)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF8C1D18))
                            .clickable {
                                onPurge(0)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear All",
                                tint = Color(0xFFF2B8B5),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Clear All",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF2B8B5)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { onDismiss() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = borderColor),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text(text = "Cerrar", color = textPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

