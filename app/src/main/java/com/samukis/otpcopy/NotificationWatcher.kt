package com.samukis.otpcopy

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/** Escucha SOLO notificaciones de Gmail y apps de mensajería. Todo lo demás se ignora sin leerlo. */
class NotificationWatcher : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName
        val prefs = Prefs(this)
        val source = when {
            pkg == GMAIL && prefs.gmail -> "Gmail"
            pkg in MESSAGING && prefs.messagingNotifs -> "Mensajes"
            else -> return
        }
        val n = sbn.notification
        // El resumen del grupo repite correos viejos: se salta.
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        // Gmail vuelve a publicar notificaciones antiguas al llegar otras: solo las recientes.
        if (n.`when` > 0 && System.currentTimeMillis() - n.`when` > MAX_AGE_MS) return

        val text = textOf(n)
        if (text.isBlank()) return
        CodeHandler.handle(this, source, text)
    }

    private fun textOf(n: Notification): String {
        val e = n.extras
        val parts = mutableListOf<CharSequence?>()
        parts += e.getCharSequence(Notification.EXTRA_TITLE)
        parts += e.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: e.getCharSequence(Notification.EXTRA_TEXT)
        parts += e.getCharSequence(Notification.EXTRA_SUB_TEXT)
        e.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.let { parts.addAll(it) }
        // Estilo de mensajería (Google Messages / RCS).
        @Suppress("DEPRECATION")
        e.getParcelableArray(Notification.EXTRA_MESSAGES)?.lastOrNull()?.let { b ->
            (b as? android.os.Bundle)?.getCharSequence("text")?.let { parts += it }
        }
        return parts.filterNotNull().map { it.toString() }.distinct().joinToString("\n")
    }

    companion object {
        const val GMAIL = "com.google.android.gm"
        val MESSAGING = setOf(
            "com.google.android.apps.messaging",
            "com.samsung.android.messaging",
            "com.android.mms",
        )
        private const val MAX_AGE_MS = 5 * 60_000L
    }
}
