package io.github.xiaoyouchr.ghostdownloader

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
data class KeepAlive(
    val reason: String = "",
    val count: Int = 0,
    val progress: Double = 0.0,
    val speed: Long = 0,
    val pair: PairRequest? = null,
)

fun startKeepAlive(context: Context) {
    ContextCompat.startForegroundService(context, Intent(context, KeepAliveService::class.java))
}

suspend fun startKeepAliveIfNeeded(context: Context) {
    val reason = withContext(Dispatchers.IO) {
        runCatching { EngineRepository.fetch<KeepAlive>("keepAlive").reason }
            .getOrDefault("")
    }
    if (reason.isNotEmpty()) startKeepAlive(context)
}

class KeepAliveService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var supervisor: Job? = null

    private val openApp by lazy {
        PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ServiceCompat.startForeground(
            this, NOTIF_ID_KEEP_ALIVE,
            buildNotification(KeepAlive(reason = "starting")),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            else 0,
        )
        if (supervisor?.isActive != true) supervisor = scope.launch { supervise() }
        return START_STICKY
    }

    private suspend fun supervise() {
        val notifications = getSystemService(NotificationManager::class.java)

        while (scope.isActive) {
            delay(1000)

            val state = runCatching { EngineRepository.fetch<KeepAlive>("keepAlive") }
                .getOrNull()

            if (state != null && state.reason.isEmpty()) {
                stopSelf()
                return
            }
            if (state != null) {
                notifications.notify(NOTIF_ID_KEEP_ALIVE, buildNotification(state))
                state.pair?.let {
                    notifications.notify(NOTIF_ID_PAIR, buildPairNotification(this, it))
                } ?: notifications.cancel(NOTIF_ID_PAIR)
            }
        }
    }

    private fun buildNotification(state: KeepAlive): Notification {
        val builder = NotificationCompat.Builder(this, CHANNEL_RUNNING)
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle(getString(R.string.app_name))
            .setContentIntent(openApp)
            .setOngoing(true)
            .setSilent(true)

        return when (state.reason) {
            "downloading" -> builder
                .setContentText(
                    getString(
                        R.string.notification_downloading,
                        state.count, formatSpeed(state.speed),
                    )
                )
                .setProgress(100, (state.progress * 100).toInt().coerceIn(0, 100), false)
                .build()

            "serving" -> builder
                .setContentText(getString(R.string.notification_serving))
                .build()

            else -> builder
                .setContentText(getString(R.string.notification_preparing))
                .setProgress(0, 0, true)
                .build()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
