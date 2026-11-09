package com.galibu.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.galibu.core.ui.theme.PolishBlue
import com.galibu.core.ui.theme.PolishDark
import com.galibu.core.ui.theme.PolishGrayText
import com.galibu.core.ui.theme.PolishIce

/**
 * BottomSheet reutilizable para elegir el origen de una foto: galería o cámara.
 * Reemplaza al antiguo PhotoSourceDialog (AlertDialog) para una experiencia
 * más moderna y acorde a Material Design 3.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoSourceBottomSheet(
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
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        PhotoSourceSheetContent(
            title = title,
            message = message,
            galleryTitle = galleryTitle,
            gallerySubtitle = gallerySubtitle,
            cameraTitle = cameraTitle,
            cameraSubtitle = cameraSubtitle,
            onCancel = onDismiss,
            onGallerySelected = onGallerySelected,
            onCameraSelected = onCameraSelected
        )
    }
}

/**
 * BottomSheet específico para elegir la foto de perfil.
 * Reemplaza al antiguo ProfilePhotoSourceDialog (AlertDialog).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilePhotoSourceBottomSheet(
    onDismiss: () -> Unit,
    onGallerySelected: () -> Unit,
    onCameraSelected: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        PhotoSourceSheetContent(
            title = "Elegir foto de perfil",
            message = "Selecciona la fuente desde la cual deseas cargar o tomar tu foto de perfil.",
            galleryTitle = "Galería",
            gallerySubtitle = "Selecciona una foto de tus archivos",
            cameraTitle = "Cámara",
            cameraSubtitle = "Captura una nueva foto con tu cámara",
            onCancel = onDismiss,
            onGallerySelected = onGallerySelected,
            onCameraSelected = onCameraSelected
        )
    }
}

@Composable
private fun PhotoSourceSheetContent(
    title: String,
    message: String,
    galleryTitle: String,
    gallerySubtitle: String,
    cameraTitle: String,
    cameraSubtitle: String,
    onCancel: () -> Unit,
    onGallerySelected: () -> Unit,
    onCameraSelected: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = PolishDark
        )

        Text(
            text = message,
            fontSize = 14.sp,
            color = PolishGrayText
        )

        Spacer(modifier = Modifier.height(4.dp))

        PhotoSourceOption(
            gallery = true,
            title = galleryTitle,
            subtitle = gallerySubtitle,
            onClick = {
                onCancel()
                onGallerySelected()
            }
        )

        PhotoSourceOption(
            gallery = false,
            title = cameraTitle,
            subtitle = cameraSubtitle,
            onClick = {
                onCancel()
                onCameraSelected()
            }
        )

        TextButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Cancelar",
                color = PolishGrayText,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun PhotoSourceOption(
    gallery: Boolean,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(PolishIce)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (gallery) Icons.Default.PhotoLibrary else Icons.Default.CameraAlt,
            contentDescription = if (gallery) "Galería" else "Cámara",
            tint = PolishBlue,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = PolishDark,
                fontSize = 15.sp
            )
            Text(
                text = subtitle,
                color = PolishGrayText,
                fontSize = 12.sp
            )
        }
    }
}
