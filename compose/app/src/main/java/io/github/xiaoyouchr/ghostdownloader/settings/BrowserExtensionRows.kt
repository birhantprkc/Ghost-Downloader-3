package io.github.xiaoyouchr.ghostdownloader.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Environment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import io.github.xiaoyouchr.ghostdownloader.EngineRepository
import io.github.xiaoyouchr.ghostdownloader.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class BrowserExtension(
    val port: Int = 0,
    val token: String = "",
    val installType: String = "",
    val extensionVersion: String = "",
)

@Composable
fun BrowserExtensionRows(isEnabled: Boolean, port: Int, set: (String, Any) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf(BrowserExtension()) }

    LaunchedEffect(isEnabled) {
        while (isEnabled) {
            state = runCatching { EngineRepository.fetch<BrowserExtension>("browserExtension") }
                .getOrDefault(BrowserExtension())
            delay(2000)
        }
    }

    if (!isEnabled) return

    NumberSettingRow(
        title = stringResource(R.string.settings_browser_port),
        value = port,
        range = 1024..65535,
        onConfirm = { set("browserExtensionPort", it) },
    )
    ActionSettingRow(
        title = stringResource(R.string.settings_browser_token),
        subtitle = state.token,
        onClick = { context.copyToClipboard(state.token) },
    )
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_browser_status)) },
        supportingContent = {
            Text(
                if (state.installType.isEmpty())
                    stringResource(R.string.settings_browser_disconnected)
                else stringResource(R.string.settings_browser_connected, state.extensionVersion)
            )
        },
    )
    var exported by remember { mutableStateOf("") }
    ActionSettingRow(
        title = stringResource(R.string.settings_browser_export),
        subtitle = exported.ifEmpty { stringResource(R.string.settings_browser_export_desc) },
        onClick = {
            scope.launch {
                exported = exportBrowserExtension(context)
            }
        },
    )
    ActionSettingRow(
        title = stringResource(R.string.settings_browser_regenerate),
        subtitle = stringResource(R.string.settings_browser_regenerate_desc),
        onClick = {
            scope.launch {
                runCatching {
                    EngineRepository.request("regenerateBrowserToken")
                    state = EngineRepository.fetch("browserExtension")
                }
            }
        },
    )
}

private suspend fun exportBrowserExtension(context: Context): String = withContext(Dispatchers.IO) {
    runCatching {
        val crx = File(context.cacheDir, "chrome_extension.crx")
        context.assets.open("chrome_extension.crx").use { input ->
            crx.outputStream().use(input::copyTo)
        }
        val folder = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "GhostDownloaderExtension",
        )
        EngineRepository.request("exportBrowserExtension", crx.absolutePath, folder.absolutePath)
        folder.absolutePath
    }.getOrElse { it.message ?: "" }
}

private fun Context.copyToClipboard(text: String) {
    getSystemService(ClipboardManager::class.java)
        .setPrimaryClip(ClipData.newPlainText("token", text))
}
