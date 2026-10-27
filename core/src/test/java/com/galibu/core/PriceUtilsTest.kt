package com.galibu.core

import org.junit.Assert.assertEquals
import org.junit.Test
import com.galibu.core.ui.utils.PriceUtils

class PriceUtilsTest {

    @Test
    fun `formatThousands formats clean number correctly`() {
        val result = PriceUtils.formatThousands("150000")
        assertEquals("150.000", result)
    }

    @Test
    fun `formatThousands removes existing dots`() {
        val result = PriceUtils.formatThousands("150.000")
        assertEquals("150.000", result)
    }

    @Test
    fun `formatThousands removes dollar sign`() {
        val result = PriceUtils.formatThousands("$150000")
        assertEquals("150.000", result)
    }

    @Test
    fun `formatThousands removes commas`() {
        val result = PriceUtils.formatThousands("150,000")
        assertEquals("150.000", result)
    }

    @Test
    fun `formatThousands returns empty string for non-numeric input`() {
        val result = PriceUtils.formatThousands("abc")
        assertEquals("", result)
    }

    @Test
    fun `formatThousands handles empty string`() {
        val result = PriceUtils.formatThousands("")
        assertEquals("", result)
    }

    @Test
    fun `formatThousands handles large number`() {
        val result = PriceUtils.formatThousands("1500000")
        assertEquals("1.500.000", result)
    }

    @Test
    fun `formatThousands handles small number`() {
        val result = PriceUtils.formatThousands("50000")
        assertEquals("50.000", result)
    }

    @Test
    fun `cleanPrice removes all non-digit characters`() {
        val result = PriceUtils.cleanPrice("$150.000 COP")
        assertEquals("150000", result)
    }

    @Test
    fun `cleanPrice handles empty string`() {
        val result = PriceUtils.cleanPrice("")
        assertEquals("", result)
    }

    @Test
    fun `cleanPrice handles already clean string`() {
        val result = PriceUtils.cleanPrice("150000")
        assertEquals("150000", result)
    }

    @Test
    fun `cleanPrice removes dots and commas`() {
        val result = PriceUtils.cleanPrice("150.000,50")
        assertEquals("15000050", result)
    }

    @Test
    fun `cleanPrice removes spaces`() {
        val result = PriceUtils.cleanPrice("150 000")
        assertEquals("150000", result)
    }
}
