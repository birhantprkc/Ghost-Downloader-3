package io.github.xiaoyouchr.ghostdownloader

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable

const val NOTIF_ID_PAIR = 2

private const val ACTION_APPROVE = "io.github.xiaoyouchr.ghostdownloader.PAIR_APPROVE"
private const val ACTION_REJECT = "io.github.xiaoyouchr.ghostdownloader.PAIR_REJECT"
private const val EXTRA_REQUEST_ID = "requestId"

@Serializable
data class PairRequest(
    val requestId: String = "",
    val clientKind: String = "",
    val extensionVersion: String = "",
)

fun buildPairNotification(context: Context, pair: PairRequest): Notification =
    NotificationCompat.Builder(context, CHANNEL_PAIR)
        .setSmallIcon(R.drawable.ic_download)
        .setContentTitle(context.getString(R.string.pair_title))
        .setContentText(
            context.getString(
                R.string.pair_message,
                pair.clientKind.ifEmpty { context.getString(R.string.pair_unknown_client) },
                pair.extensionVersion,
            )
        )
        .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
        .setAutoCancel(true)
        .setOnlyAlertOnce(true)
        .addAction(
            0, context.getString(R.string.pair_approve),
            pairIntent(context, ACTION_APPROVE, pair.requestId),
        )
        .addAction(
            0, context.getString(R.string.pair_reject),
            pairIntent(context, ACTION_REJECT, pair.requestId),
        )
        .build()

private fun pairIntent(context: Context, action: String, requestId: String) =
    PendingIntent.getBroadcast(
        context,
        action.hashCode(),
        Intent(context, BrowserPairReceiver::class.java)
            .setAction(action)
            .putExtra(EXTRA_REQUEST_ID, requestId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

class BrowserPairReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val requestId = intent.getStringExtra(EXTRA_REQUEST_ID) ?: return
        val isApproved = when (intent.action) {
            ACTION_APPROVE -> true
            ACTION_REJECT -> false
            else -> return
        }
        val pending = goAsync()
        Thread {
            runBlocking { EngineRepository.request("respondBrowserPair", requestId, isApproved) }
            context.getSystemService(NotificationManager::class.java).cancel(NOTIF_ID_PAIR)
            pending.finish()
        }.start()
    }
}
