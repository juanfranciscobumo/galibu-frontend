package com.galibu.core.ui.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File

/**
 * Utilidades para capturar fotos con la cámara del dispositivo usando
 * [androidx.activity.result.contract.ActivityResultContracts.TakePicture],
 * que devuelve la foto en resolución completa (a diferencia de TakePicturePreview).
 */
object CameraUtils {

    /**
     * Crea un archivo temporal en la caché de la app y devuelve su [Uri] segura
     * a través del FileProvider declarado en el módulo core.
     */
    fun createCaptureUri(context: Context): Uri? {
        return try {
            val dir = File(context.cacheDir, "camera_photos").apply { mkdirs() }
            val file = File.createTempFile("galibu_", ".jpg", dir)
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            Log.e("CameraUtils", "No se pudo crear el archivo para la cámara: ${e.localizedMessage}")
            null
        }
    }
}
