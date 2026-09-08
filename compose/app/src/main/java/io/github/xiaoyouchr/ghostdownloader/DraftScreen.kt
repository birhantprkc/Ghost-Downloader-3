package io.github.xiaoyouchr.ghostdownloader

import android.content.Context
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import io.github.xiaoyouchr.ghostdownloader.i18n.engineText

@Serializable
data class DraftFile(
    val index: Int = 0,
    val path: String = "",
    val size: Long = 0,
    val isSelected: Boolean = true,
)

@Serializable
data class DraftItem(
    val url: String = "",
    val isParsing: Boolean = true,
    val name: String = "",
    val fileSize: Long = 0,
    val files: List<DraftFile> = emptyList(),
    val error: TaskError? = null,
    val videoTiers: List<DraftOption> = emptyList(),
    val audioTiers: List<DraftOption> = emptyList(),
    val subtitles: List<DraftOption> = emptyList(),
    val videoTier: String = "",
    val audioTier: String = "",
    val subtitleLanguages: List<String> = emptyList(),
    val isVideoEnabled: Boolean = false,
    val isAudioEnabled: Boolean = false,
    val isCoverEnabled: Boolean = false,
    val hasCover: Boolean = false,
    val canRenameFiles: Boolean = false,
    val duration: Int = 0,
    val startTime: Int = 0,
    val endTime: Int = 0,
)

class DraftViewModel : ViewModel() {

    private val _items = MutableStateFlow<List<DraftItem>>(emptyList())
    val items: StateFlow<List<DraftItem>> = _items.asStateFlow()

    private var pollJob: Job? = null

    fun parse(urls: String) {
        viewModelScope.launch {
            EngineRepository.request("parse", urls)
            pollJob?.cancel()
            pollJob = viewModelScope.launch {
                while (isActive) {
                    val list = runCatching { EngineRepository.fetch<List<DraftItem>>("draft") }
                        .getOrNull() ?: continue
                    _items.value = list
                    if (list.none { it.isParsing }) break
                    delay(400)
                }
            }
        }
    }

    fun setName(url: String, name: String) {
        viewModelScope.launch { EngineRepository.request("setDraftName", url, name) }
    }

    fun setSelection(url: String, indexes: List<Int>) =
        write { EngineRepository.request("setDraftSelection", url, indexes.joinToString(",")) }

    fun setTrack(url: String, track: String, isEnabled: Boolean) =
        write { EngineRepository.request("setDraft", url, "setTrack", track, isEnabled) }

    fun setQuality(url: String, track: String, key: String) =
        write { EngineRepository.request("setDraft", url, "setQuality", track, key) }

    fun setSubtitles(url: String, languages: List<String>) =
        write { EngineRepository.request("setDraft", url, "setSubtitles", languages.joinToString(",")) }

    fun setTrim(url: String, startTime: Int, endTime: Int) =
        write { EngineRepository.request("setDraft", url, "setTrim", startTime, endTime) }

    fun setFileName(url: String, index: Int, name: String) =
        write { EngineRepository.request("setDraft", url, "setFileName", index, name) }

    private fun write(block: suspend () -> Unit) {
        viewModelScope.launch {
            block()
            runCatching { _items.value = EngineRepository.fetch("draft") }
        }
    }

    fun confirm(context: Context) {
        viewModelScope.launch {
            EngineRepository.request("confirmDraft")
            stop()
            startKeepAlive(context)
        }
    }

    fun clear() {
        viewModelScope.launch {
            EngineRepository.request("clearDraft")
            stop()
        }
    }

    private fun stop() {
        pollJob?.cancel()
        pollJob = null
        _items.value = emptyList()
    }
}

@Composable
fun DraftScreen(viewModel: DraftViewModel, onConfirm: () -> Unit, onCancel: () -> Unit) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val isReady = items.any { !it.isParsing && it.error == null }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(start = 4.dp, end = 8.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onCancel) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = stringResource(R.string.action_back),
                )
            }
            Text(
                text = stringResource(R.string.draft_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onConfirm, enabled = isReady) {
                Text(stringResource(R.string.draft_start))
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(items, key = { it.url }) { item ->
                DraftCard(item, viewModel)
            }
        }
    }
}

@Composable
private fun DraftCard(item: DraftItem, viewModel: DraftViewModel) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        Column(Modifier.padding(12.dp)) {
            when {
                item.error != null -> {
                    Text(item.url, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    Text(
                        text = engineText(item.error.message, item.error.params),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                item.isParsing -> {
                    Text(item.url, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    Text(
                        text = stringResource(R.string.draft_parsing),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> DraftForm(item, viewModel)
            }
        }
    }
}

@Composable
private fun DraftForm(item: DraftItem, viewModel: DraftViewModel) {
    val extension = item.name.substringAfterLast('.', "")
    var name by remember(item.url, extension) { mutableStateOf(item.name) }
    var isExpanded by remember(item.url) { mutableStateOf(false) }

    OutlinedTextField(
        value = name,
        onValueChange = {
            name = it
            viewModel.setName(item.url, it)
        },
        label = { Text(stringResource(R.string.draft_name)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    Text(
        text = formatSize(item.fileSize),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
    )

    DraftTracks(item, viewModel)

    if (item.files.size <= 1) return

    val selected = item.files.filter(DraftFile::isSelected)
    TextButton(onClick = { isExpanded = !isExpanded }) {
        Text(stringResource(R.string.draft_files_selected, selected.size, item.files.size))
    }
    if (!isExpanded) return

    var renaming by remember(item.url) { mutableStateOf<DraftFile?>(null) }

    item.files.forEach { file ->
        ListItem(
            headlineContent = {
                Text(file.path, style = MaterialTheme.typography.bodySmall, maxLines = 2)
            },
            supportingContent = { Text(formatSize(file.size)) },
            leadingContent = {
                Checkbox(
                    checked = file.isSelected,
                    onCheckedChange = { isChecked ->
                        val indexes = item.files
                            .filter { if (it.index == file.index) isChecked else it.isSelected }
                            .map(DraftFile::index)
                        viewModel.setSelection(item.url, indexes)
                    },
                )
            },
            modifier = if (item.canRenameFiles) {
                Modifier.clickable { renaming = file }
            } else {
                Modifier
            },
        )
    }

    renaming?.let { file ->
        RenameFileDialog(
            initial = file.path,
            onDismiss = { renaming = null },
            onConfirm = {
                viewModel.setFileName(item.url, file.index, it)
                renaming = null
            },
        )
    }
}

@Composable
private fun RenameFileDialog(
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.draft_rename_file)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text.trim()) }) {
                Text(stringResource(R.string.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
