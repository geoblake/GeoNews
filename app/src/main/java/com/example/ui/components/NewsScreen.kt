package com.example.ui.components

import android.Manifest
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
                                text = "GeoNews Pro",
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

            // Horizontal Category Filter Layout
            SectionSelectors(
                selectedSection = selectedSection,
                onSectionSelected = { viewModel.setSection(it) },
                accentPurple = accentPurple,
                textSecondary = textSecondary
            )

            // Breaking News Alert banner at top of feed
            val topHighImpactArticle = newsList.firstOrNull { it.impactLevel == "HIGH" }
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
                            onClick = { viewModel.selectArticle(article) }
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
        }
    }

    // Floating Details Reader Dialog
    selectedArticle?.let { article ->
        NewsDetailsDialog(
            article = article,
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
            bgPrimary = bgPrimary,
            bgCard = bgCard,
            borderColor = borderColor,
            accentPurple = accentPurple,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            onFrequencyChange = { viewModel.setUpdateFrequency(it) },
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
        "Todos" to "WORLD",
        "Mundo" to "GERMANY",
        "Alemania" to "COLOMBIA",
        "Colombia" to "LOCAL"
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
fun NewsArticleCard(
    article: NewsArticle,
    bgCardColor: Color,
    accentPurple: Color,
    bgPurpleContainer: Color,
    textPrimary: Color,
    textSecondary: Color,
    onClick: () -> Unit
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
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // Card Header: Source, Section & Time
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

                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                Text(
                    text = timeFormat.format(Date(article.timestamp)),
                    fontSize = 10.sp,
                    color = textSecondary
                )
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
    }
}

@Composable
fun NewsDetailsDialog(
    article: NewsArticle,
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

                    // Acknowledge Button
                    Button(
                        onClick = { onClose() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF381E72)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Entendido",
                            color = textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
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
    bgPrimary: Color,
    bgCard: Color,
    borderColor: Color,
    accentPurple: Color,
    textPrimary: Color,
    textSecondary: Color,
    onFrequencyChange: (Int) -> Unit,
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

