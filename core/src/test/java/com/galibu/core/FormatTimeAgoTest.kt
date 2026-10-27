package com.galibu.core

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTimeAgoTest {

    private fun formatTimeAgo(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        if (diff < 0) return "Hace un momento"
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        val weeks = days / 7
        val months = days / 30

        return when {
            minutes < 1 -> "Hace un momento"
            minutes < 60 -> "Hace $minutes min"
            hours < 24 -> "Hace $hours ${if (hours == 1L) "hora" else "horas"}"
            days < 7 -> "Hace $days ${if (days == 1L) "d\u00eda" else "d\u00edas"}"
            weeks < 4 -> "Hace $weeks ${if (weeks == 1L) "semana" else "semanas"}"
            else -> "Hace $months ${if (months == 1L) "mes" else "meses"}"
        }
    }

    @Test
    fun `returns Hace un momento for recent timestamp`() {
        val now = System.currentTimeMillis()
        val result = formatTimeAgo(now - 30_000L)
        assertEquals("Hace un momento", result)
    }

    @Test
    fun `returns Hace un momento for future timestamp`() {
        val future = System.currentTimeMillis() + 60_000L
        val result = formatTimeAgo(future)
        assertEquals("Hace un momento", result)
    }

    @Test
    fun `returns minutes for 5 minutes ago`() {
        val fiveMinutesAgo = System.currentTimeMillis() - 5L * 60 * 1000
        val result = formatTimeAgo(fiveMinutesAgo)
        assertEquals("Hace 5 min", result)
    }

    @Test
    fun `returns 1 hora for 1 hour ago`() {
        val oneHourAgo = System.currentTimeMillis() - 1L * 60 * 60 * 1000
        val result = formatTimeAgo(oneHourAgo)
        assertEquals("Hace 1 hora", result)
    }

    @Test
    fun `returns horas for multiple hours ago`() {
        val threeHoursAgo = System.currentTimeMillis() - 3L * 60 * 60 * 1000
        val result = formatTimeAgo(threeHoursAgo)
        assertEquals("Hace 3 horas", result)
    }

    @Test
    fun `returns 1 dia for 1 day ago`() {
        val oneDayAgo = System.currentTimeMillis() - 1L * 24 * 60 * 60 * 1000
        val result = formatTimeAgo(oneDayAgo)
        assertEquals("Hace 1 d\u00eda", result)
    }

    @Test
    fun `returns dias for multiple days ago`() {
        val threeDaysAgo = System.currentTimeMillis() - 3L * 24 * 60 * 60 * 1000
        val result = formatTimeAgo(threeDaysAgo)
        assertEquals("Hace 3 d\u00edas", result)
    }

    @Test
    fun `returns 1 semana for 1 week ago`() {
        val oneWeekAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
        val result = formatTimeAgo(oneWeekAgo)
        assertEquals("Hace 1 semana", result)
    }

    @Test
    fun `returns semanas for multiple weeks ago`() {
        val twoWeeksAgo = System.currentTimeMillis() - 14L * 24 * 60 * 60 * 1000
        val result = formatTimeAgo(twoWeeksAgo)
        assertEquals("Hace 2 semanas", result)
    }

    @Test
    fun `returns 1 mes for 1 month ago`() {
        val oneMonthAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        val result = formatTimeAgo(oneMonthAgo)
        assertEquals("Hace 1 mes", result)
    }

    @Test
    fun `returns meses for multiple months ago`() {
        val threeMonthsAgo = System.currentTimeMillis() - 90L * 24 * 60 * 60 * 1000
        val result = formatTimeAgo(threeMonthsAgo)
        assertEquals("Hace 3 meses", result)
    }
}
