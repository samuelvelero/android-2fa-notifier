package com.samukis.otpcopy

import android.content.Context

class Prefs(ctx: Context) {
    private val p = ctx.getSharedPreferences("prefs", Context.MODE_PRIVATE)

    var gmail: Boolean
        get() = p.getBoolean("gmail", true)
        set(v) = p.edit().putBoolean("gmail", v).apply()

    /** Lee el SMS directamente (no depende de la notificación). */
    var sms: Boolean
        get() = p.getBoolean("sms", true)
        set(v) = p.edit().putBoolean("sms", v).apply()

    /** Lee notificaciones de apps de mensajería (cubre RCS, que no llega como SMS). */
    var messagingNotifs: Boolean
        get() = p.getBoolean("msg_notifs", true)
        set(v) = p.edit().putBoolean("msg_notifs", v).apply()

    var clearClipboard: Boolean
        get() = p.getBoolean("clear_clip", true)
        set(v) = p.edit().putBoolean("clear_clip", v).apply()

    var showNotice: Boolean
        get() = p.getBoolean("notice", true)
        set(v) = p.edit().putBoolean("notice", v).apply()

    var diagnostics: Boolean
        get() = p.getBoolean("diag", true)
        set(v) = p.edit().putBoolean("diag", v).apply()
}
