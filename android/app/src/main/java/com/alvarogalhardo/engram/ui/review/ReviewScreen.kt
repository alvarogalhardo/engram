package com.alvarogalhardo.engram.ui.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.alvarogalhardo.engram.AppContainer
import com.alvarogalhardo.engram.data.db.entity.Card
import com.alvarogalhardo.engram.domain.scheduler.CardState
import com.alvarogalhardo.engram.domain.scheduler.Grade
import com.alvarogalhardo.engram.ui.components.CardRenderer
import com.alvarogalhardo.engram.ui.nav.appContainer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ReviewViewModel(private val c: AppContainer, private val deckId: Long) : ViewModel() {
    var queue by mutableStateOf<List<Card>>(emptyList())
        private set
    var current by mutableStateOf<Card?>(null)
        private set
    var revealed by mutableStateOf(false)
        private set
    var previews by mutableStateOf<Map<Grade, String>>(emptyMap())
        private set
    var waitingUntil by mutableStateOf<Long?>(null)
        private set
    var finished by mutableStateOf(false)
        private set
    var loading by mutableStateOf(true)
        private set

    val newLeft: Int get() = queue.count { it.state == CardState.NEW }
    val learningLeft: Int
        get() = queue.count { it.state == CardState.LEARNING || it.state == CardState.RELEARNING }
    val reviewLeft: Int get() = queue.count { it.state == CardState.REVIEW }

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            queue = c.studyRepo.buildQueue(deckId)
            loading = false
            advance()
        }
    }

    /** Re-evaluates the queue (learning cards may have come due while we waited). */
    fun tick() = advance()

    fun reveal() {
        revealed = true
    }

    fun answer(grade: Grade) {
        val card = current ?: return
        viewModelScope.launch {
            val updated = c.studyRepo.answer(card, grade)
            val rest = queue.filter { it.id != card.id }
            queue = if (updated.state == CardState.LEARNING || updated.state == CardState.RELEARNING) {
                rest + updated // back into the session; advance() picks by due time
            } else {
                rest
            }
            advance()
        }
    }

    private fun advance() {
        val now = c.clock.nowMillis()
        // Due learning cards come first, then the queue order (reviews → new).
        val dueLearning = queue
            .filter { (it.state == CardState.LEARNING || it.state == CardState.RELEARNING) && it.dueAt <= now }
            .minByOrNull { it.dueAt }
        val next = dueLearning
            ?: queue.firstOrNull { it.state == CardState.NEW || it.state == CardState.REVIEW }
        when {
            next != null -> {
                current = next
                revealed = false
                previews = c.studyRepo.previews(next)
                waitingUntil = null
                finished = false
            }
            queue.isNotEmpty() -> {
                current = null
                waitingUntil = queue.minOf { it.dueAt }
                finished = false
            }
            else -> {
                current = null
                waitingUntil = null
                finished = true
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(deckId: Long, onBack: () -> Unit) {
    val container = appContainer()
    val vm: ReviewViewModel = viewModel(
        key = "review-$deckId",
        factory = viewModelFactory { initializer { ReviewViewModel(container, deckId) } },
    )

    // While waiting for a learning card to come due, re-check every 15 s.
    LaunchedEffect(vm.waitingUntil) {
        while (vm.waitingUntil != null) {
            delay(15_000)
            vm.tick()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Revisão") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "Novas: ${vm.newLeft}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "Aprendendo: ${vm.learningLeft}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                Text(
                    "Revisar: ${vm.reviewLeft}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }

            Box(Modifier.fillMaxWidth().weight(1f)) {
                val card = vm.current
                when {
                    vm.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    card != null -> {
                        val html = if (vm.revealed) "${card.front}<hr>${card.back}" else card.front
                        CardRenderer(html = html, modifier = Modifier.fillMaxSize())
                    }
                    vm.finished -> Text(
                        "Nada para revisar agora. 🎉",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                    )
                    vm.waitingUntil != null -> {
                        val minutes =
                            ((vm.waitingUntil!! - container.clock.nowMillis()) / 60_000L + 1)
                                .coerceAtLeast(1)
                        Text(
                            "Próximo cartão em ~$minutes min.\nPode aguardar ou voltar depois.",
                            modifier = Modifier.align(Alignment.Center),
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            val card = vm.current
            if (card != null) {
                if (!vm.revealed) {
                    Button(onClick = { vm.reveal() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Mostrar resposta")
                    }
                } else {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        GradeButton(
                            label = "De novo",
                            interval = vm.previews[Grade.AGAIN] ?: "",
                            container = MaterialTheme.colorScheme.errorContainer,
                            content = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f),
                        ) { vm.answer(Grade.AGAIN) }
                        GradeButton(
                            label = "Difícil",
                            interval = vm.previews[Grade.HARD] ?: "",
                            container = MaterialTheme.colorScheme.tertiaryContainer,
                            content = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.weight(1f),
                        ) { vm.answer(Grade.HARD) }
                        GradeButton(
                            label = "Bom",
                            interval = vm.previews[Grade.GOOD] ?: "",
                            container = MaterialTheme.colorScheme.primary,
                            content = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.weight(1f),
                        ) { vm.answer(Grade.GOOD) }
                        GradeButton(
                            label = "Fácil",
                            interval = vm.previews[Grade.EASY] ?: "",
                            container = MaterialTheme.colorScheme.secondaryContainer,
                            content = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.weight(1f),
                        ) { vm.answer(Grade.EASY) }
                    }
                }
            }
        }
    }
}

@Composable
private fun GradeButton(
    label: String,
    interval: String,
    container: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 10.dp),
        ) {
            Text(label, maxLines = 1, style = MaterialTheme.typography.labelLarge)
        }
        Text(
            interval,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
