package com.galibu.core.ui.utils

import android.graphics.BitmapFactory
import android.util.Base64

object PhotoUtils {
    fun decodeBase64ToBitmap(photoUrl: String?): Any? {
        if (photoUrl.isNullOrEmpty()) return null
        if (photoUrl.startsWith("data:image/")) {
            return try {
                val base64Data = photoUrl.substringAfter("base64,")
                val cleanedBase64 = base64Data.trim().replace(" ", "+").replace("\n", "").replace("\r", "")
                val decodedBytes = Base64.decode(cleanedBase64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size) ?: photoUrl
            } catch (e: Exception) {
                photoUrl
            }
        }
        return photoUrl
    }

    fun parsePortfolioPhotos(photos: String?): List<String> {
        if (photos.isNullOrBlank()) return emptyList()
        return photos.split("|")
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }
}
