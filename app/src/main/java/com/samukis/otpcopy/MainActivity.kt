package com.samukis.otpcopy

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Pantalla única y sencilla: permisos, interruptores, prueba y diagnóstico. */
class MainActivity : Activity() {

    private lateinit var prefs: Prefs
    private lateinit var listenerBtn: Button
    private lateinit var smsBtn: Button
    private lateinit var notifBtn: Button
    private lateinit var diagText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        val dp = resources.displayMetrics.density
        val pad = (16 * dp).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad * 2, pad, pad * 2)
        }
        fun title(t: String) = root.addView(TextView(this).apply {
            text = t; textSize = 18f; setTypeface(typeface, Typeface.BOLD); setPadding(0, pad, 0, pad / 2)
        })
        fun note(t: String) = root.addView(TextView(this).apply { text = t; textSize = 13f; alpha = 0.75f })
        fun button(t: String, onClick: () -> Unit) = Button(this).apply {
            text = t; isAllCaps = false; setOnClickListener { onClick() }; root.addView(this)
        }
        fun check(t: String, get: () -> Boolean, set: (Boolean) -> Unit) = root.addView(CheckBox(this).apply {
            text = t; isChecked = get(); setOnCheckedChangeListener { _, v -> set(v) }
        })

        title("1. Permisos")
        listenerBtn = button("") { openListenerSettings() }
        smsBtn = button("") { requestPermissions(arrayOf(Manifest.permission.RECEIVE_SMS), 1) }
        notifBtn = button("") {
            if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 2)
        }
        note(
            "Si Android dice \"ajuste restringido\": Info de la app → ⋮ (arriba a la derecha) → " +
                "\"Permitir ajustes restringidos\", y vuelve a intentarlo."
        )
        button("Abrir info de la app") {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }

        title("2. Fuentes")
        check("Gmail (notificaciones)", { prefs.gmail }, { prefs.gmail = it })
        check("SMS (lectura directa)", { prefs.sms }, { prefs.sms = it })
        check("Notificaciones de Mensajes (para RCS)", { prefs.messagingNotifs }, { prefs.messagingNotifs = it })
        note("Gmail debe tener las notificaciones ACTIVADAS (pueden ser silenciosas o minimizadas).")

        title("3. Comportamiento")
        check("Mostrar aviso con el código copiado", { prefs.showNotice }, { prefs.showNotice = it })
        check("Borrar el portapapeles a los 60 s", { prefs.clearClipboard }, { prefs.clearClipboard = it })
        check("Guardar diagnóstico (solo en memoria)", { prefs.diagnostics }, { prefs.diagnostics = it })

        title("4. Probar el detector")
        val input = EditText(this).apply {
            hint = "Pega aquí un texto con un código"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 2
        }
        root.addView(input)
        val result = TextView(this).apply { setPadding(0, pad / 2, 0, 0) }
        button("Probar") {
            val code = OtpExtractor.extract(input.text.toString())
            result.text = if (code == null) "No encontré un código." else "Código: $code (copiado)"
            if (code != null) CodeHandler.copy(this, code)
        }
        root.addView(result)

        title("5. Diagnóstico")
        note(
            "Muestra exactamente el texto que Android le entrega a la app. Si en vez del código ves " +
                "algo como \"contenido sensible oculto\", desactiva en Ajustes → Notificaciones → " +
                "\"Notificaciones mejoradas\" / \"Sugerir acciones y respuestas\"."
        )
        button("Refrescar") { refreshDiag() }
        button("Borrar") { DiagLog.clear(); refreshDiag() }
        diagText = TextView(this).apply {
            typeface = Typeface.MONOSPACE; textSize = 12f; setTextIsSelectable(true); setPadding(0, pad / 2, 0, 0)
        }
        root.addView(diagText)

        root.addView(TextView(this).apply {
            text = "v${packageManager.getPackageInfo(packageName, 0).versionName} · sin acceso a internet"
            textSize = 12f; alpha = 0.5f; setPadding(0, pad * 2, 0, 0)
        })

        setContentView(ScrollView(this).apply { addView(root); isFillViewport = true })
    }

    override fun onResume() {
        super.onResume()
        val listener = isListenerEnabled()
        listenerBtn.text = (if (listener) "✅" else "❌") + " Acceso a notificaciones" + (if (listener) "" else " — tocar para activar")
        val sms = checkSelfPermission(Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        smsBtn.text = (if (sms) "✅" else "❌") + " Recibir SMS" + (if (sms) "" else " — tocar para conceder")
        if (Build.VERSION.SDK_INT >= 33) {
            val ok = checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            notifBtn.text = (if (ok) "✅" else "❌") + " Mostrar avisos" + (if (ok) "" else " — tocar para conceder")
        } else notifBtn.visibility = View.GONE
        refreshDiag()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onResume()
    }

    private fun refreshDiag() {
        diagText.text = DiagLog.dump()
    }

    private fun component() = ComponentName(this, NotificationWatcher::class.java)

    private fun isListenerEnabled(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: return false
        return flat.split(':').any { ComponentName.unflattenFromString(it) == component() }
    }

    private fun openListenerSettings() {
        val intent = if (Build.VERSION.SDK_INT >= 30) {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, component().flattenToString())
        } else Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        runCatching { startActivity(intent) }.onFailure {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }
}
