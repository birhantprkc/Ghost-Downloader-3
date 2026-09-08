package io.github.xiaoyouchr.ghostdownloader

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class Peers(val active: Int = 0, val total: Int = 0)

@Serializable
data class TaskUiState(
    val id: String = "",
    val name: String = "",
    val progress: Double = 0.0,
    val speed: Long = 0,
    val received: Long = 0,
    val status: String = "",
    val fileSize: Long = 0,
    val fileCount: Int = 0,
    val error: TaskError? = null,
    val upload: Long = 0,
    val peers: Peers? = null,
    val isSeeding: Boolean = false,
    val seedingSeconds: Long = 0,
    val shareRatio: Double = 0.0,
    val stateText: String = "",
    val isLive: Boolean = false,
)

class TaskViewModel : ViewModel() {

    val tasks: StateFlow<List<TaskUiState>> =
        EngineRepository.poll<List<TaskUiState>>("tasks", 500)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun pause(taskId: String) {
        viewModelScope.launch { EngineRepository.request("pause", taskId) }
    }

    fun resume(context: Context, taskId: String) {
        viewModelScope.launch { EngineRepository.request("resume", taskId) }
        startKeepAlive(context)
    }

    fun remove(taskId: String) {
        viewModelScope.launch { EngineRepository.request("remove", taskId) }
    }
}
