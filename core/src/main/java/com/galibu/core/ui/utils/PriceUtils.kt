package com.galibu.core.ui.utils

import java.text.NumberFormat
import java.util.Locale

object PriceUtils {
    fun formatThousands(value: String): String {
        val clean = value.replace(".", "").replace(",", "").replace("$", "").trim()
        val cleanDigits = clean.filter { it.isDigit() }
        if (cleanDigits.isEmpty()) return ""
        return try {
            val parsed = cleanDigits.toLong()
            NumberFormat.getNumberInstance(Locale.forLanguageTag("es-CO")).format(parsed)
        } catch (e: Exception) {
            cleanDigits
        }
    }

    fun cleanPrice(value: String): String {
        return value.filter { it.isDigit() }
    }
}

