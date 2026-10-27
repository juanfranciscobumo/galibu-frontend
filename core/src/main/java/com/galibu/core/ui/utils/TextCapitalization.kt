package com.galibu.core.ui.utils

fun String.capitalizeFirstLetter(): String {
    if (isEmpty()) return this
    return replaceFirstChar { it.uppercaseChar() }
}
