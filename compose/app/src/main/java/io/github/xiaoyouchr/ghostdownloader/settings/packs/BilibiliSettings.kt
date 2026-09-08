package io.github.xiaoyouchr.ghostdownloader.settings.packs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.chaquo.python.Python
import io.github.xiaoyouchr.ghostdownloader.R
import io.github.xiaoyouchr.ghostdownloader.settings.OptionsSettingRow
import io.github.xiaoyouchr.ghostdownloader.settings.SwitchSettingRow
import kotlinx.serialization.json.JsonObject

private val QUALITIES: List<Pair<String, String>> by lazy {
    val config = Python.getInstance().getModule("bili_pack.config")
    val values = config.get("QUALITY_VALUES")!!.asList()
    val labels = config.get("QUALITY_LABELS")!!.asList()
    values.zip(labels) { v, l -> v.toString() to l.toString() }
}

@Composable
fun BilibiliSettings(config: JsonObject, k: PackKeys, set: (String, Any) -> Unit) {
    BilibiliLoginRows()

    OptionsSettingRow(
        title = stringResource(R.string.bili_default_quality),
        value = config.int(k("defaultQuality")).toString(),
        options = QUALITIES.map { (value, label) -> value.toString() to label },
        onSelect = { set(k("defaultQuality"), it.toInt()) },
    )
    OptionsSettingRow(
        title = stringResource(R.string.bili_alternative_quality),
        value = config.str(k("alternativeQuality")),
        options = listOf(
            "max" to stringResource(R.string.bili_quality_max),
            "min" to stringResource(R.string.bili_quality_min),
        ),
        onSelect = { set(k("alternativeQuality"), it) },
    )
    SwitchSettingRow(
        title = stringResource(R.string.bili_hdr),
        subtitle = stringResource(R.string.bili_hdr_desc),
        checked = config.bool(k("shouldIncludeHdr")),
        onCheckedChange = { set(k("shouldIncludeHdr"), it) },
    )
    SwitchSettingRow(
        title = stringResource(R.string.bili_dolby),
        subtitle = stringResource(R.string.bili_dolby_desc),
        checked = config.bool(k("shouldIncludeDolby")),
        onCheckedChange = { set(k("shouldIncludeDolby"), it) },
    )
}
