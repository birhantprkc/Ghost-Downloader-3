package io.github.xiaoyouchr.ghostdownloader

import kotlinx.serialization.Serializable

@Serializable
data class TaskError(
    val message: String = "",
    val params: Map<String, String> = emptyMap(),
)
