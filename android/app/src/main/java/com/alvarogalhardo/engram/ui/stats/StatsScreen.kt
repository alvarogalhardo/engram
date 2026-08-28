package com.alvarogalhardo.engram.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.alvarogalhardo.engram.AppContainer
import com.alvarogalhardo.engram.domain.stats.DayCount
import com.alvarogalhardo.engram.domain.stats.StatsResult
import com.alvarogalhardo.engram.ui.nav.appContainer
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

class StatsViewModel(private val c: AppContainer) : ViewModel() {
    var stats by mutableStateOf<StatsResult?>(null)
        private set

    init {
        viewModelScope.launch { stats = c.statsRepo.stats() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(onBack: () -> Unit) {
    val container = appContainer()
    val vm: StatsViewModel =
        viewModel(factory = viewModelFactory { initializer { StatsViewModel(container) } })
    val stats = vm.stats

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Estatísticas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (stats == null) return@Column

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(
                    title = "Sequência",
                    value = "${stats.streakDays} dia(s)",
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    title = "Revisões (ano)",
                    value = "${stats.totalReviews}",
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(
                    title = "Acerto (30 dias)",
                    value = stats.accuracy30?.let { "${(it * 100).toInt()}%" } ?: "—",
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    title = "Acerto (geral)",
                    value = stats.accuracyAll?.let { "${(it * 100).toInt()}%" } ?: "—",
                    modifier = Modifier.weight(1f),
                )
            }

            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Revisões nos últimos 30 dias",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    val max = stats.last30Days.maxOf { it.count }
                    Text(
                        if (max > 0) "pico: $max/dia" else "nenhuma revisão ainda",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    ReviewsBarChart(
                        data = stats.last30Days,
                        modifier = Modifier.fillMaxWidth().height(160.dp).padding(top = 12.dp),
                    )
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                        val fmt = DateTimeFormatter.ofPattern("dd/MM")
                        Text(
                            stats.last30Days.first().date.format(fmt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "hoje",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(title: String, value: String, modifier: Modifier = Modifier) {
    ElevatedCard(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(value, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

/**
 * Barra por dia, um matiz só (primary); hoje em opacidade cheia, dias anteriores
 * atenuados; trilho neutro marca os dias vazios. Gap de 2dp entre barras.
 */
@Composable
private fun ReviewsBarChart(data: List<DayCount>, modifier: Modifier = Modifier) {
    val barColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    Canvas(modifier) {
        if (data.isEmpty()) return@Canvas
        val max = data.maxOf { it.count }.coerceAtLeast(1)
        val gap = 2.dp.toPx()
        val barWidth = (size.width - gap * (data.size - 1)) / data.size
        val corner = CornerRadius(barWidth / 3f)
        data.forEachIndexed { i, day ->
            val x = i * (barWidth + gap)
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(x, size.height - 3.dp.toPx()),
                size = Size(barWidth, 3.dp.toPx()),
                cornerRadius = corner,
            )
            if (day.count > 0) {
                val h = size.height * day.count / max
                drawRoundRect(
                    color = if (i == data.lastIndex) barColor else barColor.copy(alpha = 0.55f),
                    topLeft = Offset(x, size.height - h),
                    size = Size(barWidth, h),
                    cornerRadius = corner,
                )
            }
        }
    }
}
