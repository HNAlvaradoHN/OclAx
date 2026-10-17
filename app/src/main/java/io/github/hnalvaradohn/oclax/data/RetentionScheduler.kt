package io.github.hnalvaradohn.oclax.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

internal object RetentionScheduler {
    private const val ACTION_CLEANUP = "io.github.hnalvaradohn.oclax.RETENTION_CLEANUP"
    private const val REQUEST_CODE = 4102

    fun schedule(context: Context) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(AlarmManager::class.java) ?: return
        val pendingIntent = cleanupIntent(appContext)
        val next = ItemStore(appContext).nextExpirationAt()

        if (next == null) {
            alarmManager.cancel(pendingIntent)
            return
        }

        val triggerAt = next.coerceAtLeast(System.currentTimeMillis() + 1_000L)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent,
            )
        } else {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent,
            )
        }
    }

    private fun cleanupIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, RetentionCleanupReceiver::class.java).setAction(ACTION_CLEANUP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}

class RetentionCleanupReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (
            intent?.action != Intent.ACTION_BOOT_COMPLETED &&
            intent?.action != "io.github.hnalvaradohn.oclax.RETENTION_CLEANUP"
        ) {
            return
        }

        ItemStore(context.applicationContext).cleanupExpired()
    }
}
