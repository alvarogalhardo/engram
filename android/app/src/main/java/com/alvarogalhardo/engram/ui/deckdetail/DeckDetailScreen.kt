package com.alvarogalhardo.engram.ui.deckdetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.alvarogalhardo.engram.AppContainer
import com.alvarogalhardo.engram.data.db.entity.Card
import com.alvarogalhardo.engram.ui.components.ConfirmDialog
import com.alvarogalhardo.engram.ui.nav.appContainer
import com.alvarogalhardo.engram.util.HtmlUtil
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DeckDetailViewModel(private val c: AppContainer, private val deckId: Long) : ViewModel() {
    val deck = c.deckRepo.observeDeck(deckId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val cards = c.deckRepo.observeCardsInDeck(deckId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val counts = c.deckRepo.observeDecksWithCounts()
        .map { list -> list.find { it.deckId == deckId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun rename(name: String) {
        viewModelScope.launch { c.deckRepo.renameDeck(deckId, name) }
    }

    fun deleteDeck(onDone: () -> Unit) {
        viewModelScope.launch {
            c.deckRepo.deleteDeck(deckId)
            onDone()
        }
    }

    fun deleteCard(cardId: Long) {
        viewModelScope.launch { c.deckRepo.deleteCard(cardId) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckDetailScreen(
    deckId: Long,
    onBack: () -> Unit,
    onStudy: () -> Unit,
    onAddCard: () -> Unit,
    onEditCard: (Long) -> Unit,
) {
    val container = appContainer()
    val vm: DeckDetailViewModel = viewModel(
        key = "deck-$deckId",
        factory = viewModelFactory { initializer { DeckDetailViewModel(container, deckId) } },
    )
    val deck by vm.deck.collectAsStateWithLifecycle()
    val cards by vm.cards.collectAsStateWithLifecycle()
    val counts by vm.counts.collectAsStateWithLifecycle()

    var menuOpen by remember { mutableStateOf(false) }
    var renameDialog by remember { mutableStateOf(false) }
    var deleteDeckDialog by remember { mutableStateOf(false) }
    var cardToDelete by remember { mutableStateOf<Card?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(deck?.name ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Renomear") },
                            onClick = {
                                menuOpen = false
                                renameDialog = true
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Excluir baralho") },
                            onClick = {
                                menuOpen = false
                                deleteDeckDialog = true
                            },
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddCard) {
                Icon(Icons.Default.Add, contentDescription = "Nova carta")
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            val c = counts
            val dueTotal = (c?.learningCount ?: 0) + (c?.reviewCount ?: 0) + (c?.newCount ?: 0)
            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "${c?.newCount ?: 0} novas",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "${c?.learningCount ?: 0} aprendendo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                Text(
                    "${c?.reviewCount ?: 0} a revisar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            Button(
                onClick = onStudy,
                modifier = Modifier.fillMaxWidth(),
                enabled = dueTotal > 0,
            ) {
                Text(if (dueTotal > 0) "Estudar" else "Nada para estudar agora")
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(cards, key = { it.id }) { card ->
                    ElevatedCard(Modifier.fillMaxWidth().clickable { onEditCard(card.id) }) {
                        Row(
                            Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                HtmlUtil.stripHtml(card.front).ifBlank { "(sem texto)" },
                                modifier = Modifier.weight(1f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            IconButton(onClick = { cardToDelete = card }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Excluir carta",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (renameDialog) {
        var name by remember(deck) { mutableStateOf(deck?.name ?: "") }
        AlertDialog(
            onDismissRequest = { renameDialog = false },
            title = { Text("Renomear baralho") },
            text = {
                OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.rename(name)
                        renameDialog = false
                    },
                    enabled = name.isNotBlank(),
                ) { Text("Salvar") }
            },
            dismissButton = {
                TextButton(onClick = { renameDialog = false }) { Text("Cancelar") }
            },
        )
    }

    if (deleteDeckDialog) {
        ConfirmDialog(
            title = "Excluir baralho?",
            text = "Todas as cartas e o histórico de revisões deste baralho serão apagados.",
            onConfirm = { vm.deleteDeck(onBack) },
            onDismiss = { deleteDeckDialog = false },
        )
    }

    cardToDelete?.let { card ->
        ConfirmDialog(
            title = "Excluir carta?",
            text = HtmlUtil.stripHtml(card.front).take(120),
            onConfirm = {
                vm.deleteCard(card.id)
                cardToDelete = null
            },
            onDismiss = { cardToDelete = null },
        )
    }
}
