package io.github.xiaoyouchr.ghostdownloader.settings.packs

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.chaquo.python.Python
import io.github.xiaoyouchr.ghostdownloader.R
import io.github.xiaoyouchr.ghostdownloader.settings.OptionsSettingRow
import io.github.xiaoyouchr.ghostdownloader.settings.SwitchSettingRow
import io.github.xiaoyouchr.ghostdownloader.settings.TextSettingRow
import kotlinx.serialization.json.JsonObject

private val PROXY_SITES: List<String> by lazy {
    Python.getInstance().getModule("huggingface_pack.config")
        .get("HF_PROXY_SITES")!!.asList().map { it.toString() }
}

@Composable
fun HuggingFaceSettings(config: JsonObject, k: PackKeys, set: (String, Any) -> Unit) {
    val custom = config.str(k("customSite"))
    val token = config.str(k("accessToken"))

    SwitchSettingRow(
        title = stringResource(R.string.huggingface_enabled),
        subtitle = stringResource(R.string.huggingface_enabled_desc),
        checked = config.bool(k("isEnabled")),
        onCheckedChange = { set(k("isEnabled"), it) },
    )
    OptionsSettingRow(
        title = stringResource(R.string.proxy_site),
        value = config.str(k("selectedSite")),
        options = (PROXY_SITES + custom).filter(String::isNotEmpty).distinct().map { it to it },
        onSelect = { set(k("selectedSite"), it) },
    )
    TextSettingRow(
        title = stringResource(R.string.proxy_site_custom),
        value = custom,
        onConfirm = { set(k("customSite"), it) },
        emptyHint = stringResource(R.string.proxy_site_custom_desc),
        placeholder = "https://example.com",
    )
    TextSettingRow(
        title = stringResource(R.string.huggingface_access_token),
        value = token,
        onConfirm = { set(k("accessToken"), it) },
        emptyHint = stringResource(R.string.huggingface_access_token_desc),
        placeholder = "hf_...",
    )
}
