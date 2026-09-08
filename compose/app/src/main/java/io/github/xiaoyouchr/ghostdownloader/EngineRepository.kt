package io.github.xiaoyouchr.ghostdownloader

import com.chaquo.python.Python
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

object EngineRepository {

    private val engine by lazy { Python.getInstance().getModule("engine") }

    val json = Json { ignoreUnknownKeys = true }

    suspend fun raw(name: String, vararg args: Any?): String = withContext(Dispatchers.IO) {
        engine.callAttr(name, *args).toString()
    }

    suspend inline fun <reified T> fetch(name: String, vararg args: Any?): T =
        json.decodeFromString(raw(name, *args))

    suspend fun request(name: String, vararg args: Any?) {
        withContext(Dispatchers.IO) { engine.callAttr(name, *args) }
    }

    inline fun <reified T> poll(name: String, interval: Long): Flow<T> = flow {
        while (true) {
            runCatching { emit(fetch<T>(name)) }
            delay(interval)
        }
    }
}
