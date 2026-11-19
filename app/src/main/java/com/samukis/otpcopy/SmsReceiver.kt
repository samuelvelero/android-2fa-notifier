package com.samukis.otpcopy

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

/** Lee el SMS en el momento que llega (no depende de la app de mensajes ni de su notificación). */
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        if (!Prefs(ctx).sms) return
        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        // Un SMS largo llega partido; se agrupa por remitente.
        parts.groupBy { it.displayOriginatingAddress }.forEach { (_, msgs) ->
            val body = msgs.joinToString("") { it.displayMessageBody ?: "" }
            CodeHandler.handle(ctx, "SMS", body)
        }
    }
}
