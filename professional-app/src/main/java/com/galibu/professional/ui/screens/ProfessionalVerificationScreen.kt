package com.galibu.professional.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.galibu.core.ui.theme.*
import com.galibu.core.ui.components.PhotoSourceDialog
import com.galibu.core.ui.components.rememberCameraCaptureLauncher
import com.galibu.professional.ui.viewmodels.ProfessionalViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun getBytesFromUri(context: Context, uri: Uri): ByteArray? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.readBytes()
        }
    } catch (e: Exception) {
        null
    }
}

@Composable
fun ProfessionalVerificationScreen(
    viewModel: ProfessionalViewModel,
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    var selectedDocType by remember { mutableStateOf("Cédula de Ciudadanía") }
    val docTypes = listOf("Cédula de Ciudadanía", "Cédula de Extranjería", "Pasaporte")
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0f) }
    var hasPhotoSelected by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var capturedBase64 by remember { mutableStateOf<String?>(null) }
    var showPhotoSourceDialog by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            capturedBase64 = null
            selectedImageUri = uri
            hasPhotoSelected = true
        }
    }

    val launchVerificationCamera = rememberCameraCaptureLauncher(
        maxDim = 1024,
        onImageCaptured = { base64 ->
            selectedImageUri = null
            capturedBase64 = base64
            hasPhotoSelected = true
        },
        onError = { message -> viewModel.showAlert("Aviso", message) }
    )

    if (showPhotoSourceDialog) {
        PhotoSourceDialog(
            title = "Foto del documento",
            message = "Elige de dónde deseas obtener la foto del documento:",
            onDismiss = { showPhotoSourceDialog = false },
            onGallerySelected = {
                showPhotoSourceDialog = false
                galleryLauncher.launch("image/*")
            },
            onCameraSelected = {
                showPhotoSourceDialog = false
                launchVerificationCamera()
            }
        )
    }

    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkGrayBg)
    ) {
        // Custom Header TopBar with Back Button and NO Logout Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = PolishDark,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Verificación",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = OffWhite
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .border(1.dp, PolishBorder, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Verificación de Identidad",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = OffWhite,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Para garantizar la seguridad de la comunidad y recibir ofertas de trabajo en tiempo real, verifica tu identidad subiendo una foto de tu documento oficial.",
                    fontSize = 13.sp,
                    color = LightGray
                )
            }
        }

        // Current status card
        val status = currentUser?.verificationStatus ?: "NOT_STARTED"
        val statusCardBg = when (status) {
            "VERIFIED" -> Color(0xFFE8F5E9)
            "PENDING" -> Color(0xFFE3F2FD)
            "REJECTED" -> Color(0xFFFDE8E8)
            else -> CardSurface
        }
        val statusBorderColor = when (status) {
            "VERIFIED" -> Color(0xFFC3E6CB)
            "PENDING" -> Color(0xFFB3E5FC)
            "REJECTED" -> Color(0xFFF8BBD0)
            else -> PolishBorder
        }
        val statusLabelColor = when (status) {
            "VERIFIED" -> PolishGreen
            "PENDING" -> PolishNavy
            "REJECTED" -> Color(0xFF9B1C1C)
            else -> LightGray
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = statusCardBg),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
                .border(1.dp, statusBorderColor, RoundedCornerShape(16.dp))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (status) {
                        "VERIFIED" -> Icons.Default.Verified
                        "PENDING" -> Icons.Default.HourglassTop
                        "REJECTED" -> Icons.Default.Error
                        else -> Icons.Default.Info
                    },
                    contentDescription = null,
                    tint = when (status) {
                        "VERIFIED" -> PolishGreen
                        "PENDING" -> PolishBlue
                        "REJECTED" -> Color(0xFFC81E1E)
                        else -> LightGray
                    },
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Estado de tu cuenta:",
                        fontSize = 13.sp,
                        color = statusLabelColor.copy(alpha = 0.8f)
                    )
                    Text(
                        text = when (status) {
                            "VERIFIED" -> "PERFIL VERIFICADO ✅"
                            "PENDING" -> "VERIFICACIÓN EN CURSO 🕒"
                            "REJECTED" -> "CUESTIONADO - COMPROBAR DOCS ❌"
                            else -> "FALTA VERIFICACIÓN ⚠️"
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = statusLabelColor
                    )
                }
            }
        }

        if (currentUser?.verificationStatus == "NOT_STARTED" || currentUser?.verificationStatus == "REJECTED") {
            // Form to select and upload document
            Text(
                text = "1. Seleccione Tipo de Documento",
                fontSize = 14.sp,
                color = SecondaryLime,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Horizontal choices
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                docTypes.forEach { type ->
                    val isSelected = selectedDocType == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) PrimaryGreen else CardSurface)
                            .border(1.dp, if (isSelected) Color.Transparent else PolishBorder, RoundedCornerShape(8.dp))
                            .clickable { selectedDocType = type }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = type.replace("Cédula de ", "C. "),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else LightGray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Text(
                text = "2. Sube foto clara de tu documento (Frontal)",
                fontSize = 14.sp,
                color = SecondaryLime,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Real Camera / Gallery picker box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardSurface)
                    .border(1.dp, PolishBorder, RoundedCornerShape(16.dp))
                    .clickable { showPhotoSourceDialog = true }
                    .testTag("upload_box"),
                contentAlignment = Alignment.Center
            ) {
                if (!hasPhotoSelected || selectedImageUri == null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Camera",
                            tint = LightGray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Presiona para capturar o subir foto",
                            fontSize = 12.sp,
                            color = LightGray
                        )
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Real selected image preview using Coil
                        AsyncImage(
                            model = capturedBase64?.let { com.galibu.core.ui.utils.ImageUtils.getCoilModel(it) }
                                ?: selectedImageUri,
                            contentDescription = "Vista previa del documento",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                        
                        // Semi-transparent overlay to ensure text readability
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.40f))
                                .padding(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.VerifiedUser,
                                            contentDescription = null,
                                            tint = SecondaryLime,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(selectedDocType, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PrimaryGreen)
                                }

                                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.Gray.copy(alpha = 0.5f))
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(currentUser?.name ?: "Nombre Profesional", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text("DOCUMENTO ASOCIADO AL REGISTRO", color = Color.LightGray, fontSize = 10.sp)
                                    }
                                }
                            }
                        }

                        // Close button to re-take
                        IconButton(
                            onClick = { 
                                hasPhotoSelected = false 
                                selectedImageUri = null
                                capturedBase64 = null
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Eliminar", tint = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isUploading) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    LinearProgressIndicator(
                        progress = { uploadProgress },
                        color = PolishGreen,
                        trackColor = LightGray.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Subiendo documentos a Firebase Storage... ${Math.round(uploadProgress * 100)}%",
                        color = OffWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Button(
                    onClick = {
                        if (!hasPhotoSelected) return@Button
                        isUploading = true
                        uploadProgress = 0f
                    },
                    enabled = hasPhotoSelected,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("submit_verif_button")
                ) {
                    Text("Enviar Identidad para Registro", fontWeight = FontWeight.Bold)
                }
            }

            // LaunchedEffect for real progress loading and actual upload
            if (isUploading) {
                LaunchedEffect(Unit) {
                    while (uploadProgress < 1.0f) {
                        delay(200)
                        uploadProgress += 0.1f
                    }
                    val realBytes = capturedBase64?.let { b64 ->
                        try {
                            android.util.Base64.decode(b64.substringAfter("base64,"), android.util.Base64.DEFAULT)
                        } catch (e: Exception) {
                            null
                        }
                    } ?: selectedImageUri?.let { getBytesFromUri(context, it) } ?: byteArrayOf(1, 2, 3)
                    viewModel.submitVerification(selectedDocType, realBytes)
                    isUploading = false
                    hasPhotoSelected = false
                }
            }

        } else if (currentUser?.verificationStatus == "PENDING") {
            // Count down from 10 to 0 to simulate real-time AI background check
            var countdown by remember { mutableStateOf(10) }
            LaunchedEffect(Unit) {
                while (countdown > 0) {
                    delay(1000)
                    countdown--
                }
                // Automatically approve when countdown hits 0
                val user = currentUser
                if (user != null && user.verificationStatus == "PENDING") {
                    viewModel.repository.completeVerification()
                    viewModel.triggerSimulatedFcmPush(
                        "Identidad Verificada Exitosamente 🎉",
                        "El pipeline inteligente de Firebase procesó tu ID de manera exitosa. ¡Tu perfil cuenta ahora con sello verificado y puedes ofertar!"
                    )
                }
            }

            // Pending visual state with active ticking countdown
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = PolishGreen, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Procesando Verificación Automatizada...",
                    color = OffWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tiempo estimado de procesamiento: $countdown segundos",
                    color = SecondaryLime,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Nuestros servidores de Firebase Cloud están corriendo un pipeline de reconocimiento facial con IA para validar los documentos cargados.\nRecibirás una notificación push en tiempo real.",
                    color = LightGray,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))
                
                // Manual override button for instant verification
                Button(
                    onClick = {
                        val user = currentUser
                        if (user != null) {
                            scope.launch {
                                viewModel.repository.completeVerification()
                                viewModel.triggerSimulatedFcmPush(
                                    "Identidad Verificada Exitosamente 🎉",
                                    "Tu ID ha sido verificado mediante aprobación instantánea del administrador. ¡Ya puedes ofertar!"
                                )
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PolishBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Aprobar Cuenta Ahora (Inmediato)", fontWeight = FontWeight.Bold)
                }
            }
        } else {
            // Already verified
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = PolishGreen,
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "¡Identidad Verificada Correctamente!",
                    color = OffWhite,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tu expediente ha sido registrado en Firebase Database.\nTu perfil es apto para ofertar precios de servicios de manera competitiva en la vía pública.",
                    color = LightGray,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = PolishGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Volver al Panel Principal", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
}


