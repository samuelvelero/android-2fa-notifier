package com.samukis.otpcopy

/**
 * Saca un código de verificación de un texto libre (asunto/cuerpo de un correo, SMS).
 * Solo devuelve algo si el texto menciona una palabra de verificación; si no, null.
 * Devuelve solo los dígitos ("G-123 456" -> "123456").
 */
object OtpExtractor {

    private val KEYWORD = Regex(
        // Estricto a propósito: palabras como "confirmación" o "acceso" traen números de pedido
        // y pisarían el portapapeles sin motivo.
        "(c[oó]digo|\\bcodes?\\b|\\bcod\\.|verificaci[oó]n|verification|\\bverif|\\botp\\b|\\bpin\\b|" +
            "\\bclave\\b|contrase[nñ]a|passcode|\\btoken\\b|\\b2fa\\b|\\bmfa\\b|one[- ]time|un solo uso|" +
            "autenticaci[oó]n|authenticat)",
        RegexOption.IGNORE_CASE
    )

    // 4-8 dígitos seguidos, o 3+3 separados por espacio/guion. Opcional prefijo tipo "G-".
    private val CANDIDATE = Regex(
        "(?<![\\p{L}\\d])(?:[A-Z]{1,4}-)?(\\d{3}[ -]\\d{3}|\\d{4,8})(?![\\p{L}\\d])"
    )

    private val CURRENCY_BEFORE = Regex("(s/\\.?|\\$|€|£|usd|pen|eur|mxn|cop|clp|ars|soles?)\\s*$", RegexOption.IGNORE_CASE)
    private val UNIT_AFTER = Regex("^\\s*(%|usd|pen|soles|dólares|dolares|eur|kb|mb|gb|km|kg|px)\\b", RegexOption.IGNORE_CASE)

    fun extract(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val keywords = KEYWORD.findAll(text).map { it.range }.toList()
        if (keywords.isEmpty()) return null

        var best: String? = null
        var bestScore = Int.MIN_VALUE
        for (m in CANDIDATE.findAll(text)) {
            val start = m.range.first
            val end = m.range.last + 1
            val raw = m.groupValues[1]
            val digits = raw.filter { it.isDigit() }

            val before = text.substring(maxOf(0, start - 8), start)
            val after = text.substring(end, minOf(text.length, end + 8))

            // Números que claramente no son códigos.
            if (CURRENCY_BEFORE.containsMatchIn(before)) continue
            if (UNIT_AFTER.containsMatchIn(after)) continue
            if (before.endsWith("+") || before.matches(Regex(".*\\+\\d{1,3}\\s?$"))) continue // teléfono
            if (before.endsWith("/") || after.startsWith("/")) continue // fecha o ruta
            if (Regex("[\\d][.,]$").containsMatchIn(before)) continue // 1.234 / 1,234
            if (Regex("^[.,]\\d").containsMatchIn(after)) continue // 1234.50
            if (Regex("\\d[ -]$").containsMatchIn(before) || Regex("^[ -]\\d").containsMatchIn(after)) continue // trozo de teléfono/tarjeta
            if (before.endsWith("#") || before.lowercase().endsWith("n°") || before.lowercase().endsWith("nº")) {
                // "pedido #12345" suele ser número de orden
                continue
            }

            val distance = keywords.minOf { k ->
                when {
                    k.last < start -> start - k.last
                    k.first > end -> k.first - end
                    else -> 0
                }
            }
            if (distance > 200) continue

            var score = -distance
            if (keywords.any { it.last < start && start - it.last <= 40 }) score += 15 // "código: 123456"
            if (digits.length == 6) score += 10
            if (digits.length in 5..8) score += 3
            if (digits.length == 4 && digits.toInt() in 1900..2099) score -= 60 // año
            if (score > bestScore) {
                bestScore = score
                best = digits
            }
        }
        return best
    }
}
