package com.galibu.core.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.galibu.core.ui.theme.PolishBlue
import com.galibu.core.ui.theme.PolishDark
import com.galibu.core.ui.theme.PolishGrayText
import com.galibu.core.ui.theme.PolishIce
import com.galibu.core.ui.utils.CameraUtils
import com.galibu.core.ui.utils.ImageUtils

/**
 * Diálogo reutilizable para elegir el origen de una foto: galería o cámara.
 */
@Composable
fun PhotoSourceDialog(
    onDismiss: () -> Unit,
    onGallerySelected: () -> Unit,
    onCameraSelected: () -> Unit,
    title: String = "Agregar foto",
    message: String = "Elige de dónde deseas obtener la foto:",
    galleryTitle: String = "Elegir de la galería",
    gallerySubtitle: String = "Selecciona una foto de tus archivos",
    cameraTitle: String = "Tomar una foto",
    cameraSubtitle: String = "Captura una nueva foto con tu cámara"
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = PolishDark
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = PolishGrayText
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(PolishIce)
                        .clickable { onGallerySelected() }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Galería",
                        tint = PolishBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = galleryTitle,
                            fontWeight = FontWeight.Bold,
                            color = PolishDark,
                            fontSize = 14.sp
                        )
                        Text(
                            text = gallerySubtitle,
                            color = PolishGrayText,
                            fontSize = 11.sp
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(PolishIce)
                        .clickable { onCameraSelected() }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Cámara",
                        tint = PolishBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = cameraTitle,
                            fontWeight = FontWeight.Bold,
                            color = PolishDark,
                            fontSize = 14.sp
                        )
                        Text(
                            text = cameraSubtitle,
                            color = PolishGrayText,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = PolishGrayText)
            }
        }
    )
}

/**
 * Crea un lanzador de cámara (resolución completa) que entrega la foto ya
 * comprimida y codificada en base64 lista para enviarse al backend.
 *
 * Incluye la solicitud del permiso CAMERA y el FileProvider configurado en
 * el módulo core.
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
            val base64 = ImageUtils.uriToBase64(context, uri, maxDim)
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
