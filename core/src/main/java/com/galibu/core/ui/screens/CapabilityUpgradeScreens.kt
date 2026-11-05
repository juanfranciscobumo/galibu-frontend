package com.galibu.core.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.galibu.core.ui.components.CategoryMultiSelectField
import com.galibu.core.ui.components.PhotoSourceDialog
import com.galibu.core.ui.components.rememberCameraCaptureLauncher
import com.galibu.core.ui.utils.ImageUtils
import com.galibu.core.ui.utils.PriceUtils
import com.galibu.core.ui.utils.capitalizeFirstLetter

private val ScreenBg = Color(0xFFF8FAFC)
private val TextMain = Color(0xFF0F172A)
private val TextSec = Color(0xFF64748B)
private val BrandBlue = Color(0xFF2563EB)
private val SuccessBg = Color(0xFFDCFCE7)
private val SuccessFg = Color(0xFF16A34A)
private val PolishDark = Color(0xFF0F172A)
private val PolishBlue = Color(0xFF2563EB)

private const val MAX_PORTFOLIO_PHOTOS = 6

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ActivateProfessionalScreen(
    viewModel: com.galibu.core.ui.viewmodels.SharedViewModel,
    suggestions: List<String>,
    onBack: () -> Unit,
    onActivated: () -> Unit
) {
    val context = LocalContext.current
    val selectedCategories = remember { mutableStateListOf<String>() }
    var hourlyRateInput by remember { mutableStateOf("") }
    var workDescription by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    val photos = remember { mutableStateListOf<String>() }
    var showPhotoSource by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryInput by remember { mutableStateOf("") }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null && photos.size < MAX_PORTFOLIO_PHOTOS) {
            val base64 = try {
                ImageUtils.uriToBase64(context, uri, 640)
            } catch (e: Exception) {
                null
            }
            photos.add(base64 ?: uri.toString())
        }
    }
    val launchCamera = rememberCameraCaptureLauncher(
        maxDim = 640,
        onImageCaptured = { base64 -> if (photos.size < MAX_PORTFOLIO_PHOTOS) photos.add(base64) },
        onError = { viewModel.showAlert("Aviso", it) }
    )

    Box(modifier = Modifier.fillMaxSize().background(ScreenBg)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Ofrecer mis servicios",
                        color = TextMain,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 26.sp
                    )
                    Text(
                        text = "Activa tu capacidad profesional",
                        color = TextSec,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                colors = CardDefaults.cardColors(containerColor = SuccessBg),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SuccessFg,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Al activarla recibirás 3 primeros trabajos de cortesía y podrás aparecer en la lista de profesionales.",
                        color = Color(0xFF15803D),
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                // --- Categoría(s) ---
                CategoryMultiSelectField(
                    selected = selectedCategories,
                    options = suggestions,
                    label = "Categoría(s) de servicio *",
                    placeholder = "Seleccionar categorías",
                    accentColor = BrandBlue,
                    borderColor = Color(0xFFE2E8F0),
                    customOptionLabel = "Otra categoría",
                    onAddCustom = { showAddCategoryDialog = true },
                    testTag = "category_multi_select"
                )

                Spacer(modifier = Modifier.height(20.dp))

                // --- Descripción de trabajo ---
                Text(
                    text = "Descripción",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextMain,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = workDescription,
                    onValueChange = { workDescription = it },
                    placeholder = { Text("Cuéntanos qué servicios ofreces...") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    minLines = 2
                )

                Spacer(modifier = Modifier.height(20.dp))

                // --- Tarifa ---
                Text(
                    text = "Tarifa aproximada por hora (opcional)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextMain,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = hourlyRateInput,
                    onValueChange = { input ->
                        val clean = input.filter { it.isDigit() }
                        if (clean.length <= 9) {
                            hourlyRateInput = PriceUtils.formatThousands(clean)
                        }
                    },
                    placeholder = { Text("Ej: 35000", color = Color(0xFF94A3B8)) },
                    prefix = { Text("$ ") },
                    suffix = { Text("/hora") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // --- Fotos ---
                Text(
                    text = "Fotos de tus trabajos (opcional, hasta $MAX_PORTFOLIO_PHOTOS)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextMain,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (photos.size < MAX_PORTFOLIO_PHOTOS) {
                        Card(
                            onClick = { showPhotoSource = true },
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(90.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Agregar foto",
                                    tint = TextSec,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Foto", color = TextSec, fontSize = 12.sp)
                            }
                        }
                    }
                    photos.forEach { photo ->
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            AsyncImage(
                                model = ImageUtils.getCoilModel(photo),
                                contentDescription = "Foto de trabajo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .padding(6.dp)
                                    .align(Alignment.TopEnd)
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xCC0F172A))
                                    .clickable { photos.remove(photo) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Quitar foto",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = errorMessage!!,
                        color = Color(0xFFDC2626),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        val category = selectedCategories.joinToString(", ").trim()
                        val rateVal = PriceUtils.cleanPrice(hourlyRateInput).toDoubleOrNull()
                        val rateError = rateVal != null && rateVal < 5417.0
                        when {
                            category.isBlank() -> {
                                errorMessage = "Selecciona al menos una categoría de servicio."
                            }
                            rateError -> {
                                errorMessage = "Si vas a poner una tarifa por hora, debe ser de al menos $5.417 COP. Puedes dejarla vacía."
                            }
                            else -> {
                                isSubmitting = true
                                errorMessage = null
                                val rate = rateVal
                                val finalPhotos = photos.toList().ifEmpty { null }
                                viewModel.upgradeCapability(
                                    role = "PROFESSIONAL",
                                    workCategory = category,
                                    hourlyRate = rate,
                                    workPhotos = finalPhotos,
                                    workDescription = workDescription.trim().takeIf { it.isNotBlank() }
                                ) { error ->
                                    isSubmitting = false
                                    if (error == null) {
                                        onActivated()
                                    } else {
                                        errorMessage = error
                                    }
                                }
                            }
                        }
                    },
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Text(
                        text = if (isSubmitting) "Activando..." else "Activar capacidad profesional",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // Diálogo para agregar categoría personalizada
    if (showAddCategoryDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = {
                Text(
                    text = "Nueva categoría",
                    fontWeight = FontWeight.Bold,
                    color = TextMain,
                    fontSize = 20.sp
                )
            },
            text = {
                var customInput by remember { mutableStateOf("") }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Escribe el nombre de tu categoría",
                        fontSize = 14.sp,
                        color = TextSec
                    )
                    OutlinedTextField(
                        value = customInput,
                        onValueChange = { newCategoryInput = it },
                        placeholder = { Text("Ej: Domótica") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCategoryInput.isNotBlank() && !selectedCategories.contains(newCategoryInput.trim())) {
                            selectedCategories.add(newCategoryInput.trim())
                        }
                        showAddCategoryDialog = false
                    },
                    enabled = newCategoryInput.isNotBlank() && !selectedCategories.contains(newCategoryInput.trim()),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                ) {
                    Text(
                        text = "Agregar",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddCategoryDialog = false }
                ) {
                    Text("Cancelar", color = TextSec)
                }
            }
        )
    }

    if (showPhotoSource) {
        PhotoSourceDialog(
            onDismiss = { showPhotoSource = false },
            onGallerySelected = {
                showPhotoSource = false
                galleryLauncher.launch("image/*")
            },
            onCameraSelected = {
                showPhotoSource = false
                launchCamera()
            },
            title = "Agregar foto del trabajo",
            message = "Elige cómo deseas agregar la foto:"
        )
    }
}

@Composable
fun ActivateClientScreen(
    viewModel: com.galibu.core.ui.viewmodels.SharedViewModel,
    onBack: () -> Unit,
    onActivated: () -> Unit
) {
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(ScreenBg)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Buscar un profesional",
                        color = TextMain,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 26.sp
                    )
                    Text(
                        text = "Activa tu capacidad cliente",
                        color = TextSec,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Al activarla podrás:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextMain
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    val benefits = listOf(
                        "Publicar solicitudes de servicio",
                        "Recibir y negociar propuestas de profesionales",
                        "Contratar directo desde la lista de profesionales",
                        "Chatear, calificar y llevar historial de tus trabajos"
                    )
                    benefits.forEach { benefit ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SuccessFg,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(benefit, color = TextMain, fontSize = 14.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = errorMessage!!,
                        color = Color(0xFFDC2626),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Button(
                    onClick = {
                        isSubmitting = true
                        errorMessage = null
                        viewModel.upgradeCapability("CLIENT") { error ->
                            isSubmitting = false
                            if (error == null) {
                                onActivated()
                            } else {
                                errorMessage = error
                            }
                        }
                    },
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Text(
                        text = if (isSubmitting) "Activando..." else "Activar capacidad cliente",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
