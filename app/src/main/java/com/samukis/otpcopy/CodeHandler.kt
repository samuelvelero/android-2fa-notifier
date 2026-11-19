package com.samukis.otpcopy

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import android.os.SystemClock

/** Recibe texto de cualquier fuente, extrae el código, lo copia y avisa. */
object CodeHandler {
    private const val CHANNEL = "codes"
    private const val NOTIF_ID = 1
    private const val CLIP_LABEL = "2FA code"
    private const val DEDUPE_MS = 90_000L
    private const val CLEAR_MS = 60_000L

    private val main = Handler(Looper.getMainLooper())
    private var lastCode: String? = null
    private var lastAt = 0L

    /** @return el código encontrado (o null). */
    fun handle(ctx: Context, source: String, text: String): String? {
        val code = OtpExtractor.extract(text)
        val prefs = Prefs(ctx)
        val now = SystemClock.elapsedRealtime()
        val dup = code != null && code == lastCode && now - lastAt < DEDUPE_MS

        if (prefs.diagnostics) {
            DiagLog.add(source, text, when {
                code == null -> "sin código"
                dup -> "$code (repetido, ignorado)"
                else -> "$code COPIADO"
            })
        }
        if (code == null || dup) return code

        lastCode = code
        lastAt = now
        copy(ctx, code)
        if (prefs.showNotice) notify(ctx, code, source)
        if (prefs.clearClipboard) {
            val stamp = now
            main.postDelayed({ if (lastAt == stamp) clearClip(ctx) }, CLEAR_MS)
        }
        return code
    }

    fun copy(ctx: Context, code: String) {
        val cm = ctx.getSystemService(ClipboardManager::class.java)
        val clip = ClipData.newPlainText(CLIP_LABEL, code)
        // Marca sensible: el teclado/sistema no muestra la vista previa (Android 13+).
        clip.description.extras = PersistableBundle().apply {
            putBoolean(if (Build.VERSION.SDK_INT >= 33) ClipDescription.EXTRA_IS_SENSITIVE else "android.content.extra.IS_SENSITIVE", true)
        }
        cm.setPrimaryClip(clip)
    }

    private fun clearClip(ctx: Context) {
        val cm = ctx.getSystemService(ClipboardManager::class.java)
        if (Build.VERSION.SDK_INT >= 28) cm.clearPrimaryClip()
        else cm.setPrimaryClip(ClipData.newPlainText("", ""))
        ctx.getSystemService(NotificationManager::class.java).cancel(NOTIF_ID)
    }

    private fun notify(ctx: Context, code: String, source: String) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 33 && !nm.areNotificationsEnabled()) return
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Códigos copiados", NotificationManager.IMPORTANCE_HIGH).apply {
                    setSound(null, null)
                    enableVibration(false)
                    lockscreenVisibility = Notification.VISIBILITY_PRIVATE
                }
            )
        }
        val again = PendingIntent.getBroadcast(
            ctx, 0,
            Intent(ctx, CopyAgainReceiver::class.java).putExtra(CopyAgainReceiver.EXTRA_CODE, code),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val publicVersion = Notification.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle("Código copiado")
            .build()
        val n = Notification.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle("Copiado: $code")
            .setContentText("Desde $source")
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion)
            .setOnlyAlertOnce(true)
            .setTimeoutAfter(CLEAR_MS)
            .setAutoCancel(true)
            .addAction(Notification.Action.Builder(null, "Copiar otra vez", again).build())
            .build()
        nm.notify(NOTIF_ID, n)
    }
}
