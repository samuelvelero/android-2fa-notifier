package com.samukis.otpcopy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OtpExtractorTest {
    private fun x(s: String) = OtpExtractor.extract(s)

    @Test fun gmailSpanish() = assertEquals("482913", x("Google\nTu código de verificación es 482913"))
    @Test fun gmailEnglish() = assertEquals("771204", x("Microsoft account\nSecurity code\nPlease use the following security code: 771204"))
    @Test fun googlePrefix() = assertEquals("123456", x("G-123456 es tu código de verificación de Google."))
    @Test fun spaced() = assertEquals("123456", x("Your verification code is 123 456"))
    @Test fun dashed() = assertEquals("654321", x("Código: 654-321"))
    @Test fun fourDigitPin() = assertEquals("4821", x("BCP: Tu clave dinámica es 4821. No la compartas."))
    @Test fun eightDigits() = assertEquals("90817263", x("Use code 90817263 to sign in"))

    @Test fun ignoresAmountPicksCode() =
        assertEquals("553201", x("Compra por S/ 1500.00. Tu código de verificación: 553201"))
    @Test fun ignoresYear() = assertEquals("839201", x("© 2026 Acme. Your code: 839201"))
    @Test fun ignoresPhone() = assertEquals("4410", x("Llama al +51 987 654 321 si no fuiste tú. Código 4410"))
    @Test fun ignoresDate() = assertEquals("120934", x("Enviado el 18/11/2026. Código 120934"))

    @Test fun noKeywordNoCode() = assertNull(x("Tu pedido 12345678 fue enviado"))
    @Test fun orderConfirmation() = assertNull(x("Confirmación de pedido #556677. Total: S/ 2500"))
    @Test fun onlyAmounts() = assertNull(x("Tu código de descuento vence el 30/11. Ahorra 2500 soles"))
    @Test fun blank() = assertNull(x(""))
}
