package com.samukis.otpcopy

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Registro de diagnóstico SOLO en memoria (se pierde al cerrar el proceso, nunca toca disco).
 * Sirve para ver qué texto le entrega Android a la app y saber si oculta los códigos.
 */
object DiagLog {
    private const val MAX = 25
    private val entries = ArrayDeque<String>()
    private val fmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    @Synchronized
    fun add(source: String, text: String, result: String) {
        val clipped = if (text.length > 400) text.take(400) + "…" else text
        entries.addFirst("[${fmt.format(Date())}] $source → $result\n$clipped")
        while (entries.size > MAX) entries.removeLast()
    }

    @Synchronized
    fun dump(): String = if (entries.isEmpty()) "(vacío — aún no llegó nada de las apps vigiladas)" else entries.joinToString("\n\n")

    @Synchronized
    fun clear() = entries.clear()
}
