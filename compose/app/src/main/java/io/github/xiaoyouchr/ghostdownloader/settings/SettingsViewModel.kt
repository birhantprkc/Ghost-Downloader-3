package io.github.xiaoyouchr.ghostdownloader.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.xiaoyouchr.ghostdownloader.EngineRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

@Serializable
data class Settings(
    val downloadFolder: String = "",
    val maxTaskNum: Int = 3,
    val preBlockNum: Int = 8,
    val autoSpeedUp: Boolean = true,
    val isSpeedLimitEnabled: Boolean = false,
    val speedLimitation: Int = 4194304,
    val shouldDeleteFilesOnRemove: Boolean = false,
    val shouldVerifySsl: Boolean = false,
    val proxyServer: String = "Auto",
    val isAria2RpcEnabled: Boolean = false,
    val aria2RpcPort: Int = 16800,
    val aria2RpcToken: String = "",
    val aria2RpcEmulateFingerprint: Boolean = false,
    val isBrowserExtensionEnabled: Boolean = false,
    val browserExtensionPort: Int = 14370,
)

sealed interface SettingsUiState {
    data object Loading : SettingsUiState

    data class Success(val config: JsonObject) : SettingsUiState {
        val settings: Settings = EngineRepository.json.decodeFromJsonElement(config)
    }
}

class SettingsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Loading)
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { _uiState.value = SettingsUiState.Success(read()) }
    }

    private suspend fun read(): JsonObject = EngineRepository.fetch("settings")

    fun set(name: String, value: Any) {
        viewModelScope.launch {
            EngineRepository.request("setSetting", name, value)
            _uiState.value = SettingsUiState.Success(read())
        }
    }
}
