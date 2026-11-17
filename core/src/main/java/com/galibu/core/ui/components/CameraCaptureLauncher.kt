package com.galibu.core.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.galibu.core.ui.utils.CameraUtils
import com.galibu.core.ui.utils.ImageUtils
import android.net.Uri

/**
 * Crea un lanzador de cámara (resolución completa) que entrega la foto ya
 * comprimida y codificada en base64 lista para enviarse al backend.
 */
@Composable
fun rememberCameraCaptureLauncher(
    maxDim: Int = 300,
    onImageCaptured: (String) -> Unit,
    onError: (String) -> Unit = {}
): () -> Unit {
    val context = LocalContext.current
    var pendingUri by remember { mutableStateOf<Uri?>(null) }

    val captureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingUri
        pendingUri = null
        if (success && uri != null) {
            val capturedUri: Uri = uri
            val base64 = ImageUtils.uriToBase64(context, capturedUri, maxDim)
            if (base64 != null) {
                onImageCaptured(base64)
            } else {
                onError("No pudimos procesar la foto tomada. Intenta de nuevo.")
            }
        }
    }

    val startCapture = {
        val uri = CameraUtils.createCaptureUri(context)
        if (uri != null) {
            pendingUri = uri
            try {
                captureLauncher.launch(uri)
            } catch (e: Exception) {
                onError("No se pudo abrir la cámara: ${e.localizedMessage}")
            }
        } else {
            onError("No se pudo abrir la cámara.")
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startCapture()
        } else {
            onError("Se requiere permiso de cámara para tomar fotos.")
        }
    }

    return {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            startCapture()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
}
