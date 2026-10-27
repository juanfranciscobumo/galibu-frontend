package com.galibu.core.ui.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.graphics.scale
import android.net.Uri
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File

object ImageUtils {
    fun uriToBase64(context: Context, uri: Uri, maxDim: Int = 300): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val photoBytes = inputStream.readBytes()
                val bitmap = BitmapFactory.decodeByteArray(photoBytes, 0, photoBytes.size)
                if (bitmap != null) {
                    val width = bitmap.width
                    val height = bitmap.height
                    val (newWidth, newHeight) = if (width > height) {
                        val ratio = height.toFloat() / width
                        (maxDim to (maxDim * ratio).toInt())
                    } else {
                        val ratio = width.toFloat() / height
                        (((maxDim * ratio).toInt()) to maxDim)
                    }
                    val scaledBitmap = bitmap.scale(newWidth, newHeight)
                    val out = ByteArrayOutputStream()
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 75, out)
                    val compressedBytes = out.toByteArray()
                    val base64Str = Base64.encodeToString(compressedBytes, Base64.NO_WRAP)
                    "data:image/jpeg;base64,$base64Str"
                } else null
            }
        } catch (e: Exception) {
            Log.e("ImageUtils", "Error converting uri to base64: ${e.localizedMessage}")
            null
        }
    }

    fun filePathToBase64(filePath: String, maxDim: Int = 300): String? {
        return try {
            val file = File(filePath)
            if (file.exists()) {
                val photoBytes = file.readBytes()
                val bitmap = BitmapFactory.decodeByteArray(photoBytes, 0, photoBytes.size)
                if (bitmap != null) {
                    val width = bitmap.width
                    val height = bitmap.height
                    val (newWidth, newHeight) = if (width > height) {
                        val ratio = height.toFloat() / width
                        (maxDim to (maxDim * ratio).toInt())
                    } else {
                        val ratio = width.toFloat() / height
                        (((maxDim * ratio).toInt()) to maxDim)
                    }
                    val scaledBitmap = bitmap.scale(newWidth, newHeight)
                    val out = ByteArrayOutputStream()
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 75, out)
                    val compressedBytes = out.toByteArray()
                    val base64Str = Base64.encodeToString(compressedBytes, Base64.NO_WRAP)
                    "data:image/jpeg;base64,$base64Str"
                } else null
            } else null
        } catch (e: Exception) {
            Log.e("ImageUtils", "Error converting filePath to base64: ${e.localizedMessage}")
            null
        }
    }

    fun downloadUrlToBase64(urlStr: String, maxDim: Int = 150): String? {
        return try {
            val url = java.net.URL(urlStr)
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.doInput = true
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.connect()
            val input = connection.inputStream
            val photoBytes = input.readBytes()
            val bitmap = BitmapFactory.decodeByteArray(photoBytes, 0, photoBytes.size)
            if (bitmap != null) {
                val width = bitmap.width
                val height = bitmap.height
                val (newWidth, newHeight) = if (width > height) {
                    val ratio = height.toFloat() / width
                    (maxDim to (maxDim * ratio).toInt())
                } else {
                    val ratio = width.toFloat() / height
                    (((maxDim * ratio).toInt()) to maxDim)
                }
                val scaledBitmap = bitmap.scale(newWidth, newHeight)
                val out = ByteArrayOutputStream()
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 75, out)
                val compressedBytes = out.toByteArray()
                val base64Str = Base64.encodeToString(compressedBytes, Base64.NO_WRAP)
                "data:image/jpeg;base64,$base64Str"
            } else null
        } catch (e: Exception) {
            Log.e("ImageUtils", "Error downloading URL to base64: ${e.localizedMessage}")
            null
        }
    }

    fun getCoilModel(photoUri: String?): Any? {
        if (photoUri.isNullOrBlank()) return null
        if (photoUri.startsWith("data:image/")) {
            val commaIndex = photoUri.indexOf(",")
            if (commaIndex != -1) {
                val base64Part = photoUri.substring(commaIndex + 1)
                try {
                    val cleanedBase64 = base64Part.trim().replace(" ", "+").replace("\n", "").replace("\r", "")
                    return Base64.decode(cleanedBase64, Base64.DEFAULT)
                } catch (e: Exception) {
                    Log.e("ImageUtils", "Failed to decode base64 for Coil: ${e.localizedMessage}")
                }
            }
        }
        return photoUri
    }
}

