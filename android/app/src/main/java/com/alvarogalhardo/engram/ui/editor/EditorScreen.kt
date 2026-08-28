package com.alvarogalhardo.engram.ui.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.alvarogalhardo.engram.AppContainer
import com.alvarogalhardo.engram.data.db.entity.Card
import com.alvarogalhardo.engram.domain.markdown.MarkdownConverter
import com.alvarogalhardo.engram.ui.components.CardRenderer
import com.alvarogalhardo.engram.ui.nav.appContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditorViewModel(
    private val c: AppContainer,
    private val deckId: Long,
    private val cardId: Long,
) : ViewModel() {
    var card by mutableStateOf<Card?>(null)
        private set
    var front by mutableStateOf("")
    var back by mutableStateOf("")
    var loaded by mutableStateOf(false)
        private set

    /** Cartas criadas no app são editadas em Markdown; importadas, em HTML cru. */
    val isMarkdown: Boolean
        get() = card?.let { it.frontSrc != null } ?: true

    init {
        viewModelScope.launch {
            if (cardId > 0) {
                card = c.deckRepo.cardById(cardId)
                front = card?.frontSrc ?: card?.front ?: ""
                back = card?.backSrc ?: card?.back ?: ""
            }
            loaded = true
        }
    }

    fun attachImage(uri: Uri?, toFront: Boolean) {
        if (uri == null) return
        viewModelScope.launch {
            val name = withContext(Dispatchers.IO) { c.mediaFiles.saveFromUri(uri) }
            val ref = if (isMarkdown) "\n\n![]($name)" else "<img src=\"$name\">"
            if (toFront) front += ref else back += ref
        }
    }

    fun previewHtml(): String {
        val f = if (isMarkdown) MarkdownConverter.toHtml(front) else front
        val b = if (isMarkdown) MarkdownConverter.toHtml(back) else back
        return "$f<hr>$b"
    }

    fun save(onDone: () -> Unit) {
        if (front.isBlank()) return
        viewModelScope.launch {
            val existing = card
            if (existing == null) {
                c.deckRepo.addCard(deckId, front, back)
            } else {
                c.deckRepo.updateCardContent(existing, front, back)
            }
            onDone()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(deckId: Long, cardId: Long, onDone: () -> Unit) {
    val container = appContainer()
    val vm: EditorViewModel = viewModel(
        key = "editor-$deckId-$cardId",
        factory = viewModelFactory { initializer { EditorViewModel(container, deckId, cardId) } },
    )

    var pickTargetFront by remember { mutableStateOf(true) }
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> vm.attachImage(uri, pickTargetFront) }

    var showPreview by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (cardId > 0) "Editar carta" else "Nova carta") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.save(onDone) }, enabled = vm.front.isNotBlank()) {
                        Icon(Icons.Default.Check, contentDescription = "Salvar")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            if (!vm.loaded) return@Column

            if (!vm.isMarkdown) {
                Text(
                    "Carta importada do Anki — editando o HTML diretamente.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            } else {
                Text(
                    "Escreva em Markdown: **negrito**, `código`, blocos com ``` e imagens.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }

            OutlinedTextField(
                value = vm.front,
                onValueChange = { vm.front = it },
                label = { Text("Frente") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
            )
            TextButton(onClick = {
                pickTargetFront = true
                imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }) { Text("Adicionar imagem à frente") }

            OutlinedTextField(
                value = vm.back,
                onValueChange = { vm.back = it },
                label = { Text("Verso") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
            )
            TextButton(onClick = {
                pickTargetFront = false
                imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }) { Text("Adicionar imagem ao verso") }

            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Switch(checked = showPreview, onCheckedChange = { showPreview = it })
                Text("  Visualizar", style = MaterialTheme.typography.bodyMedium)
            }
            if (showPreview) {
                CardRenderer(
                    html = vm.previewHtml(),
                    modifier = Modifier.fillMaxWidth().height(320.dp).padding(bottom = 16.dp),
                )
            }
        }
    }
}
