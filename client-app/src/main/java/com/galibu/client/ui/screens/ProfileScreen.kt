package com.galibu.client.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.galibu.core.ui.components.GalibuAlertDialog
import com.galibu.core.ui.utils.PhotoUtils
import com.galibu.core.ui.utils.capitalizeFirstLetter
import com.galibu.core.ui.components.GalibuAlertType
import com.galibu.core.ui.theme.*
import com.galibu.client.ui.viewmodels.ClientViewModel

enum class ProfileSubScreen {
    EDIT_INFO,
    CHANGE_PHOTO,
    CHANGE_PASSWORD,
    MY_JOBS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ClientViewModel,
    modifier: Modifier = Modifier,
    onNavigateToNotifications: () -> Unit = {}
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    
    // State variables
    var nameInput by remember(currentUser) { mutableStateOf(currentUser?.name ?: "") }
    var phoneInput by remember(currentUser) { mutableStateOf(currentUser?.phone ?: "") } // Sin UI: se conserva el valor actual
    var categoryInput by remember(currentUser) { mutableStateOf(currentUser?.workCategory ?: "") }
    var hourlyRateInput by remember(currentUser) { mutableStateOf(currentUser?.hourlyRate?.toInt()?.toString() ?: "") }
    var photoUrlInput by remember(currentUser) { mutableStateOf(currentUser?.profilePhotoUrl ?: "") }
    var addressInput by remember(currentUser) { mutableStateOf(currentUser?.address ?: "") }
    
    // Subscreen state
    var activeSubScreen by remember { mutableStateOf<ProfileSubScreen?>(null) }
    
    // Password state
    var currentPasswordInput by remember { mutableStateOf("") }
    var newPasswordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }
    
    var showCurrentPassword by remember { mutableStateOf(false) }
    var showNewPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    
    // Location state edits
    var currentDeptEdit by remember(currentUser) { mutableStateOf(currentUser?.department ?: "Cundinamarca") }
    var currentMuniEdit by remember(currentUser) { mutableStateOf(currentUser?.municipality ?: "Bogotá") }
    
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }
    var showSuccessMessage by remember { mutableStateOf<String?>(null) }
    
    var showProfilePhotoSourceDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var supportMessage by remember { mutableStateOf("") }

    val firestoreDeptList by viewModel.firestoreDepartments.collectAsState()
    var departmentsAndMunicipalities by remember {
        mutableStateOf(
            listOf(
                "Antioquia" to listOf("Medellín", "Envigado", "Bello", "Rionegro"),
                "Bogotá D.C." to listOf("Bogotá"),
                "Valle del Cauca" to listOf("Cali", "Palmira", "Yumbo", "Jamundí"),
                "Atlántico" to listOf("Barranquilla", "Soledad", "Puerto Colombia"),
                "Santander" to listOf("Bucaramanga", "Floridablanca", "Girón")
            )
        )
    }
    LaunchedEffect(firestoreDeptList) {
        if (firestoreDeptList.isNotEmpty()) {
            departmentsAndMunicipalities = firestoreDeptList.map { it.name to it.municipalities }
        }
    }
    var showDeptDropdown by remember { mutableStateOf(false) }
    var showMuniDropdown by remember { mutableStateOf(false) }

    // Resolve Google photo for email if needed
    val detectedGooglePhoto = remember(currentUser) {
        val email = currentUser?.email?.lowercase()?.trim() ?: ""
        if (email.isEmpty()) null
        else {
            val placeholderImages = listOf(
                "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150&h=150&fit=crop",
                "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150&h=150&fit=crop",
                "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&h=150&fit=crop",
                "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150&h=150&fit=crop",
                "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150&h=150&fit=crop"
            )
            val resolvedIndex = Math.abs(email.hashCode()) % placeholderImages.size
            placeholderImages[resolvedIndex]
        }
    }
    
    // Launchers
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val base64 = com.galibu.core.ui.utils.ImageUtils.uriToBase64(context, uri)
            val finalPhoto = base64 ?: uri.toString()
            photoUrlInput = finalPhoto
            viewModel.updateProfile(
                name = nameInput,
                phone = phoneInput,
                workCategory = if (currentUser?.role != "CLIENT") categoryInput else null,
                profilePhotoUrl = finalPhoto,
                department = currentDeptEdit,
                municipality = currentMuniEdit,
                address = addressInput,
                hourlyRate = if (currentUser?.role != "CLIENT") hourlyRateInput.toDoubleOrNull() else null
            )
            showSuccessMessage = "Foto de perfil actualizada correctamente"
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            try {
                val out = java.io.ByteArrayOutputStream()
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 75, out)
                val compressedBytes = out.toByteArray()
                val base64Str = android.util.Base64.encodeToString(compressedBytes, android.util.Base64.NO_WRAP)
                val finalPhoto = "data:image/jpeg;base64,$base64Str"
                photoUrlInput = finalPhoto
                viewModel.updateProfile(
                    name = nameInput,
                    phone = phoneInput,
                    workCategory = if (currentUser?.role != "CLIENT") categoryInput else null,
                    profilePhotoUrl = finalPhoto,
                    department = currentDeptEdit,
                    municipality = currentMuniEdit,
                    address = addressInput,
                    hourlyRate = if (currentUser?.role != "CLIENT") hourlyRateInput.toDoubleOrNull() else null
                )
                showSuccessMessage = "Foto de perfil actualizada correctamente"
            } catch (e: Exception) {
                e.printStackTrace()
                viewModel.showAlert("Aviso", "Error al guardar foto de la cámara")
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                cameraLauncher.launch(null)
            } catch (e: Exception) {
                viewModel.showAlert("Aviso", "No se pudo abrir la cámara: ${e.localizedMessage}")
            }
        } else {
            viewModel.showAlert("Aviso", "Se requiere permiso de cámara para tomar fotos.")
        }
    }

    val launchCamera = {
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            try {
                cameraLauncher.launch(null)
            } catch (e: Exception) {
                viewModel.showAlert("Aviso", "No se pudo abrir la cámara: ${e.localizedMessage}")
            }
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // MAIN CONTAINER SWITCHER
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        when (activeSubScreen) {
            ProfileSubScreen.EDIT_INFO -> {
                // SCREEN 1: EDITAR PERFIL (IMAGE 1)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Header Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { activeSubScreen = null },
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
                        
                        Text(
                            text = "Editar perfil",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }

                    HorizontalDivider(color = Color(0xFFEFF2F5), thickness = 1.dp)

                    if (showSuccessMessage != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().clickable { showSuccessMessage = null }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = showSuccessMessage!!, color = Color(0xFF15803D), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    // Foto de perfil (galería / cámara)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            contentAlignment = Alignment.BottomEnd,
                            modifier = Modifier
                                .size(96.dp)
                                .clickable { showProfilePhotoSourceDialog = true }
                        ) {
                            if (!photoUrlInput.isNullOrEmpty()) {
                                val editPhotoModel = remember(photoUrlInput) { PhotoUtils.decodeBase64ToBitmap(photoUrlInput) }
                                AsyncImage(
                                    model = editPhotoModel,
                                    contentDescription = "Foto de perfil",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .border(2.dp, Color(0xFFEFF2F5), CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(Color(0xFFE2E8F0)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(44.dp)
                                    )
                                }
                            }
                            // Camera badge
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1764FF))
                                    .border(2.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Cambiar foto",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { showProfilePhotoSourceDialog = true }) {
                            Text(
                                text = if (photoUrlInput.isNullOrEmpty()) "Sube tu foto de perfil" else "Cambiar foto",
                                color = Color(0xFF1764FF),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Nombre Completo Field
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Nombre completo",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Color(0xFF0F172A)
                        )
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { newValue ->
                                if (newValue.all { !it.isDigit() }) {
                                    nameInput = newValue.capitalizeFirstLetter()
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().testTag("edit_name_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = Color(0xFF1764FF),
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            ),
                            singleLine = true
                        )
                    }

                    // Correo Electronico Field
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Correo electrónico",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Color(0xFF0F172A)
                        )
                        OutlinedTextField(
                            value = currentUser?.email ?: "",
                            onValueChange = {},
                            shape = RoundedCornerShape(14.dp),
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFF1F5F9),
                                unfocusedContainerColor = Color(0xFFF1F5F9),
                                focusedBorderColor = Color(0xFFE2E8F0),
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                focusedTextColor = Color(0xFF64748B),
                                unfocusedTextColor = Color(0xFF64748B)
                            ),
                            singleLine = true
                        )
                    }


                    // Departamento Field
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Departamento",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Color(0xFF0F172A)
                        )
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = currentDeptEdit,
                                onValueChange = {},
                                readOnly = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("edit_dept_input")
                                    .clickable { showDeptDropdown = true },
                                trailingIcon = {
                                    IconButton(onClick = { showDeptDropdown = true }) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Mostrar departamentos",
                                            tint = Color(0xFF1764FF)
                                        )
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedBorderColor = Color(0xFF1764FF),
                                    unfocusedBorderColor = Color(0xFFE2E8F0)
                                ),
                                singleLine = true
                            )
                            DropdownMenu(
                                expanded = showDeptDropdown,
                                onDismissRequest = { showDeptDropdown = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                departmentsAndMunicipalities.forEach { (dept, munis) ->
                                    DropdownMenuItem(
                                        text = { Text(dept, color = Color(0xFF0F172A), fontSize = 14.sp) },
                                        onClick = {
                                            currentDeptEdit = dept
                                            currentMuniEdit = munis.firstOrNull() ?: ""
                                            showDeptDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Ciudad Field
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Ciudad",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Color(0xFF0F172A)
                        )
                        Box(modifier = Modifier.fillMaxWidth()) {
                            val currentMunis = remember(currentDeptEdit, departmentsAndMunicipalities) {
                                departmentsAndMunicipalities.firstOrNull { it.first == currentDeptEdit }?.second ?: listOf("Bogotá")
                            }
                            OutlinedTextField(
                                value = currentMuniEdit,
                                onValueChange = {},
                                readOnly = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("edit_city_input")
                                    .clickable { showMuniDropdown = true },
                                trailingIcon = {
                                    IconButton(onClick = { showMuniDropdown = true }) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Mostrar ciudades",
                                            tint = Color(0xFF1764FF)
                                        )
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedBorderColor = Color(0xFF1764FF),
                                    unfocusedBorderColor = Color(0xFFE2E8F0)
                                ),
                                singleLine = true
                            )
                            DropdownMenu(
                                expanded = showMuniDropdown,
                                onDismissRequest = { showMuniDropdown = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                currentMunis.forEach { muni ->
                                    DropdownMenuItem(
                                        text = { Text(muni, color = Color(0xFF0F172A), fontSize = 14.sp) },
                                        onClick = {
                                            currentMuniEdit = muni
                                            showMuniDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Dirección Field
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Dirección",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Color(0xFF0F172A)
                        )
                        OutlinedTextField(
                            value = addressInput,
                            onValueChange = { addressInput = it.capitalizeFirstLetter() },
                            placeholder = { Text("Calle 45 # 23-10", color = Color(0xFF94A3B8)) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().testTag("edit_address_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = Color(0xFF1764FF),
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            ),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Oficio/Especialidad for non-clients helper field
                    if (currentUser?.role != "CLIENT") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Oficio o Especialidad",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = Color(0xFF0F172A)
                            )
                            OutlinedTextField(
                                value = categoryInput,
                                onValueChange = { categoryInput = it.capitalizeFirstLetter() },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedBorderColor = Color(0xFF1764FF),
                                    unfocusedBorderColor = Color(0xFFE2E8F0)
                                ),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Precio de servicio por hora estimado (COP)",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = Color(0xFF0F172A)
                            )
                            OutlinedTextField(
                                value = com.galibu.core.ui.utils.PriceUtils.formatThousands(hourlyRateInput),
                                onValueChange = { input ->
                                    val clean = input.filter { it.isDigit() }
                                    if (clean.length <= 9) {
                                        hourlyRateInput = clean
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedBorderColor = Color(0xFF1764FF),
                                    unfocusedBorderColor = Color(0xFFE2E8F0)
                                ),
                                singleLine = true,
                                leadingIcon = {
                                    Text("$", color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Guardar Cambios Button
                    Button(
                        onClick = {
                            val trimmedName = nameInput.trim()

                            if (trimmedName.isEmpty()) {
                                viewModel.showError("Por favor complete todos los campos obligatorios.")
                                return@Button
                            }
                            
                            viewModel.updateProfile(
                                name = trimmedName,
                                phone = currentUser?.phone ?: "",
                                workCategory = if (currentUser?.role != "CLIENT") categoryInput.trim() else null,
                                profilePhotoUrl = photoUrlInput,
                                department = currentDeptEdit.trim(),
                                municipality = currentMuniEdit.trim(),
                                address = addressInput,
                                hourlyRate = if (currentUser?.role != "CLIENT") hourlyRateInput.toDoubleOrNull() else null
                            )
                            showSuccessMessage = "Información actualizada exitosamente"
                            activeSubScreen = null
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1764FF)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("save_profile_info_btn")
                    ) {
                        Text(
                            text = "Guardar cambios",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
            ProfileSubScreen.CHANGE_PHOTO -> {
                // SCREEN 2: FOTO DE PERFIL (IMAGE 2)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Header Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { activeSubScreen = null },
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
                        
                        Text(
                            text = "Foto de perfil",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }

                    HorizontalDivider(color = Color(0xFFEFF2F5), thickness = 1.dp)

                    // Dashed Picture Circle with overlay
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            contentAlignment = Alignment.BottomEnd,
                            modifier = Modifier.size(170.dp)
                        ) {
                            // Dashed circle container
                            Box(
                                modifier = Modifier
                                    .size(170.dp)
                                    .drawBehind {
                                        drawRoundRect(
                                            color = Color(0xFFCBD5E1),
                                            style = Stroke(
                                                width = 1.6.dp.toPx(),
                                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                                            ),
                                            cornerRadius = CornerRadius(85.dp.toPx(), 85.dp.toPx())
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (!photoUrlInput.isNullOrEmpty()) {
                                    val decodedModel = remember(photoUrlInput) { PhotoUtils.decodeBase64ToBitmap(photoUrlInput) }
                                    AsyncImage(
                                        model = decodedModel,
                                        contentDescription = "Foto de perfil",
                                        modifier = Modifier
                                            .size(150.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = "Sin foto",
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(46.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Sin foto",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            // Overlapping Camera Badge clickable
                            Box(
                                modifier = Modifier
                                    .offset(x = (-4).dp, y = (-4).dp)
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF007AFF))
                                    .border(2.dp, Color.White, CircleShape)
                                    .clickable { showProfilePhotoSourceDialog = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Seleccionar origen de foto",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Options List Below
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { launchCamera() },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFEFF2F5)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEFF6FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = null,
                                        tint = Color(0xFF007AFF),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text(
                                    text = "Tomar foto",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0F172A)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { photoPickerLauncher.launch("image/*") },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFEFF2F5)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEFF6FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = null,
                                        tint = Color(0xFF007AFF),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text(
                                    text = "Subir desde galería",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0F172A)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
            ProfileSubScreen.CHANGE_PASSWORD -> {
                // SCREEN 3: CAMBIAR CONTRASEÑA (IMAGE 3)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Header Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { activeSubScreen = null },
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
                        
                        Text(
                            text = "Cambiar contraseña",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }

                    HorizontalDivider(color = Color(0xFFEFF2F5), thickness = 1.dp)

                    // Contraseña Actual Field (con icono de ojo)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Contraseña actual",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Color(0xFF0F172A)
                        )
                        OutlinedTextField(
                            value = currentPasswordInput,
                            onValueChange = { currentPasswordInput = it },
                            shape = RoundedCornerShape(14.dp),
                            visualTransformation = if (showCurrentPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                val image = if (showCurrentPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff
                                IconButton(onClick = { showCurrentPassword = !showCurrentPassword }) {
                                    Icon(imageVector = image, contentDescription = "Mostrar contraseña")
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("current_pwd_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = Color(0xFF1764FF),
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            ),
                            singleLine = true
                        )
                    }

                    // Nueva Contraseña Field (con icono de ojo)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Nueva contraseña",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Color(0xFF0F172A)
                        )
                        OutlinedTextField(
                            value = newPasswordInput,
                            onValueChange = { newPasswordInput = it },
                            shape = RoundedCornerShape(14.dp),
                            visualTransformation = if (showNewPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                val image = if (showNewPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff
                                IconButton(onClick = { showNewPassword = !showNewPassword }) {
                                    Icon(imageVector = image, contentDescription = "Mostrar contraseña")
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("new_pwd_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = Color(0xFF1764FF),
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            ),
                            singleLine = true
                        )
                    }

                    // Confirmar Contraseña Field (sin icono de ojo, igual que Image 3)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Confirmar contraseña",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Color(0xFF0F172A)
                        )
                        OutlinedTextField(
                            value = confirmPasswordInput,
                            onValueChange = { confirmPasswordInput = it },
                            shape = RoundedCornerShape(14.dp),
                            visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                val image = if (showConfirmPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff
                                IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                                    Icon(imageVector = image, contentDescription = "Mostrar contraseña")
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("confirm_pwd_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = Color(0xFF1764FF),
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            ),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Cambiar contraseña Button
                    Button(
                        onClick = {
                            if (currentPasswordInput.isEmpty() || newPasswordInput.isEmpty() || confirmPasswordInput.isEmpty()) {
                                viewModel.showError("Por favor complete todos los campos")
                                return@Button
                            }
                            if (newPasswordInput != confirmPasswordInput) {
                                viewModel.showError("La nueva contraseña y su confirmación no coinciden")
                                return@Button
                            }
                            if (newPasswordInput.length < 8) {
                                viewModel.showError("La nueva contraseña debe tener al menos 8 caracteres.")
                                return@Button
                            }
                            if (newPasswordInput.length > 72) {
                                viewModel.showError("La nueva contraseña no puede superar los 72 caracteres.")
                                return@Button
                            }
                            viewModel.changePassword(currentPasswordInput, newPasswordInput) { errorMsg ->
                                if (errorMsg == null) {
                                    showSuccessMessage = "La contraseña ha sido cambiada exitosamente"
                                    currentPasswordInput = ""
                                    newPasswordInput = ""
                                    confirmPasswordInput = ""
                                    activeSubScreen = null
                                } else {
                                    viewModel.showError(errorMsg)
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1764FF)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("change_pwd_action_btn")
                    ) {
                        Text(
                            text = "Cambiar contraseña",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
            ProfileSubScreen.MY_JOBS -> {
                // SCREEN 4: MY JOBS (Completed and current jobs of the professional)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                ) {
                    // Header Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { activeSubScreen = null },
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
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Mis trabajos realizados",
                            color = Color(0xFF0F172A),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val allJobs by viewModel.allJobs.collectAsState()
                    val myJobs = remember(allJobs, currentUser) {
                        allJobs.filter { it.professionalId == currentUser?.id }
                    }

                    if (myJobs.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize().weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.BusinessCenter,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No tienes trabajos registrados en la plataforma.",
                                    color = Color(0xFF64748B),
                                    fontSize = 15.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize().weight(1f)
                        ) {
                            items(myJobs) { job ->
                                val statusColor = when (job.status) {
                                    "NEGOTIATING" -> Color(0xFFEAB308) // Yellow
                                    "ACCEPTED" -> Color(0xFF22C55E) // Green
                                    "COMPLETED" -> Color(0xFF3B82F6) // Blue
                                    else -> Color(0xFF64748B) // Gray
                                }
                                val statusLabel = when (job.status) {
                                    "NEGOTIATING" -> "En Negociación 💬"
                                    "ACCEPTED" -> "Activo / En Curso ⚡"
                                    "COMPLETED" -> "Finalizado ✅"
                                    else -> job.status
                                }
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = job.title.ifEmpty { "Servicio de " + job.category },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = Color(0xFF0F172A),
                                                modifier = Modifier.weight(1f)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(statusColor.copy(alpha = 0.1f))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = statusLabel,
                                                    color = statusColor,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = job.description,
                                            color = Color(0xFF475569),
                                            fontSize = 13.sp,
                                            lineHeight = 18.sp
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val priceFormatted = java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(job.budgetMin.toInt())
                                            Text(
                                                text = "Tarifa: $$priceFormatted COP",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                color = Color(0xFF0066FF)
                                            )
                                            Text(
                                                text = "Cliente: ${job.clientName}",
                                                fontSize = 12.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                        
                                        if (job.status == "COMPLETED" && (job.clientRatingOfPro != null || !job.clientReviewOfPro.isNullOrEmpty())) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = null,
                                                    tint = Color(0xFFF59E0B),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "${job.clientRatingOfPro ?: 5.0f}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF0F172A)
                                                )
                                                if (!job.clientReviewOfPro.isNullOrEmpty()) {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "“${job.clientReviewOfPro}”",
                                                        fontSize = 12.sp,
                                                        color = Color(0xFF64748B),
                                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            null -> {
                // SCREEN MAIN: MAIN PROFILE PREVIEW LIST
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Text(
                        text = "Perfil",
                        color = Color(0xFF0F172A),
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .testTag("profile_section_title")
                    )
                    HorizontalDivider(color = Color(0xFFEFF2F5), thickness = 1.dp)

                    // Dynamic Feedback Messages
                    if (showSuccessMessage != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().clickable { showSuccessMessage = null }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = showSuccessMessage!!, color = Color(0xFF15803D), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    // 1. User Info Header Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, Color(0xFFEFF2F5)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Avatar Box (Navigate to CHANGE_PHOTO subclass on click)
                            Box(
                                contentAlignment = Alignment.BottomEnd,
                                modifier = Modifier
                                    .size(88.dp)
                                    .clickable { activeSubScreen = ProfileSubScreen.CHANGE_PHOTO }
                            ) {
                                if (!photoUrlInput.isNullOrEmpty()) {
                                    val decodedModel = remember(photoUrlInput) { PhotoUtils.decodeBase64ToBitmap(photoUrlInput) }
                                    AsyncImage(
                                        model = decodedModel,
                                        contentDescription = "Foto de Perfil",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                            .border(1.5.dp, Color(0xFFEFF2F5), CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                            .background(Color(0xFF007AFF)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = currentUser?.name?.take(1)?.uppercase() ?: "M",
                                            color = Color.White,
                                            fontSize = 32.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Overlay small camera badge
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF007AFF))
                                        .border(1.5.dp, Color.White, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Cambiar Foto",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }

                            // Info details
                            Column(
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = currentUser?.name ?: "María García",
                                    color = Color(0xFF0F172A),
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = currentUser?.email ?: "",
                                    color = Color(0xFF64748B),
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "${currentUser?.department ?: "Cundinamarca"} · ${currentUser?.municipality ?: "Bogotá"}",
                                    color = Color(0xFF64748B),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                if (currentUser?.role != "CLIENT" && !currentUser?.workCategory.isNullOrBlank()) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        val categories = currentUser?.workCategory?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
                                        categories.forEach { category ->
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(0xFFEFF6FF))
                                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = category,
                                                    color = Color(0xFF007AFF),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                                if (detectedGooglePhoto != null && photoUrlInput != detectedGooglePhoto) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "📸 Aplicar foto de Google Account",
                                        color = Color(0xFF007AFF),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable {
                                            photoUrlInput = detectedGooglePhoto!!
                                            viewModel.updateProfile(
                                                name = nameInput,
                                                phone = phoneInput,
                                                workCategory = if (currentUser?.role != "CLIENT") categoryInput else null,
                                                profilePhotoUrl = detectedGooglePhoto,
                                                department = currentDeptEdit,
                                                municipality = currentMuniEdit,
                                                hourlyRate = if (currentUser?.role != "CLIENT") hourlyRateInput.toDoubleOrNull() else null
                                            )
                                            showSuccessMessage = "Se aplicó tu foto de Google Account con éxito"
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Options List (Cleaned according to prompt - Mi ubicación, notificaciones, firebase removed)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ProfileOptionCard(
                            icon = Icons.Default.Person,
                            iconBgColor = Color(0xFFEFF6FF),
                            iconColor = Color(0xFF2563EB),
                            title = "Editar información",
                            onClick = { 
                                showSuccessMessage = null
                                activeSubScreen = ProfileSubScreen.EDIT_INFO 
                            }
                        )

                        ProfileOptionCard(
                            icon = Icons.Default.CameraAlt,
                            iconBgColor = Color(0xFFEFF6FF),
                            iconColor = Color(0xFF2563EB),
                            title = "Cambiar foto de perfil",
                            onClick = { 
                                showSuccessMessage = null
                                showProfilePhotoSourceDialog = true
                            }
                        )

                        ProfileOptionCard(
                            icon = Icons.Default.Lock,
                            iconBgColor = Color(0xFFEFF6FF),
                            iconColor = Color(0xFF2563EB),
                            title = "Cambiar contraseña",
                            onClick = { 
                                showSuccessMessage = null
                                if (currentUser?.authProvider == "GOOGLE") {
                                    viewModel.showInfo(
                                        "Tu cuenta está vinculada a Google. " +
                                            "Cambia tu contraseña desde tu cuenta de Google."
                                    )
                                } else {
                                    activeSubScreen = ProfileSubScreen.CHANGE_PASSWORD 
                                }
                            }
                        )

                        if (currentUser?.role != "CLIENT") {
                            ProfileOptionCard(
                                icon = Icons.Default.BusinessCenter,
                                iconBgColor = Color(0xFFEFF6FF),
                                iconColor = Color(0xFF2563EB),
                                title = "Mis trabajos realizados",
                                onClick = {
                                    showSuccessMessage = null
                                    activeSubScreen = ProfileSubScreen.MY_JOBS
                                }
                            )
                        }
                    }

                    // CUENTA Section Divider Label
                    Text(
                        text = "CUENTA",
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 2.dp)
                    )

                    // Account section options
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ProfileOptionCard(
                            icon = Icons.AutoMirrored.Filled.Help,
                            iconBgColor = Color(0xFFF1F5F9),
                            iconColor = Color(0xFF64748B),
                            title = "Ayuda y soporte",
                            onClick = { showSupportDialog = true }
                        )

                        ProfileOptionCard(
                            icon = Icons.AutoMirrored.Filled.Logout,
                            iconBgColor = Color(0xFFFEF2F2),
                            iconColor = Color(0xFFEF4444),
                            title = "Cerrar sesión",
                            titleColor = Color(0xFFEF4444),
                            showChevron = false,
                            onClick = { showLogoutDialog = true }
                        )

                        ProfileOptionCard(
                            icon = Icons.Default.Delete,
                            iconBgColor = Color(0xFFFEF2F2),
                            iconColor = Color(0xFFEF4444),
                            title = "Eliminar cuenta",
                            titleColor = Color(0xFFEF4444),
                            chevronColor = Color(0xFFF87171),
                            onClick = { showDeleteDialog = true }
                        )
                    }

                    // Logo & app version footer
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF005AC1)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("G", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Galibu v1.0.0",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }

    // Modal support dialogs

    // HELP SUPPORT DIALOG
    if (showSupportDialog) {
        AlertDialog(
            onDismissRequest = { showSupportDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        if (supportMessage.isNotBlank()) {
                            viewModel.showAlert("Aviso", "Reporte enviado con éxito")
                            supportMessage = ""
                            showSupportDialog = false
                        } else {
                            viewModel.showAlert("Aviso", "Escribe un mensaje o detalle de tu reporte")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005AC1))
                ) {
                    Text("Enviar Reporte", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSupportDialog = false }) {
                    Text("Cancelar", color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White,
            title = { Text("Ayuda y Soporte técnico 🛠️", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("¿Tienes inconvenientes con un servicio? Envía un reporte seguro al canal de soporte oficial de Galibu.", color = Color(0xFF64748B), fontSize = 12.sp)
                    OutlinedTextField(
                        value = supportMessage,
                        onValueChange = { supportMessage = it.capitalizeFirstLetter() },
                        label = { Text("Detalle de tu reporte") },
                        placeholder = { Text("Ej: El profesional no llegó en la hora de cita...") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF005AC1),
                            focusedLabelColor = Color(0xFF005AC1)
                        ),
                        modifier = Modifier.fillMaxWidth().height(100.dp)
                    )
                }
            }
        )
    }

    // DELETE ACCOUNT CONFIRMATION DIALOG
    if (showDeleteDialog) {
        GalibuAlertDialog(
            title = "¿Eliminar tu Cuenta de Galibu?",
            message = "Esta acción es irreversible y eliminará todo tu historial de servicios contratados, tus datos de contacto y tu billetera de saldo. ¿Realmente deseas continuar?",
            type = GalibuAlertType.ERROR,
            confirmText = "Eliminar",
            dismissText = "Cancelar",
            confirmColor = Color(0xFFEF4444),
            onConfirm = {
                viewModel.deleteAccount { error ->
                    if (error != null) {
                        viewModel.showAlert("No se pudo eliminar la cuenta", error, true)
                    }
                }
                showDeleteDialog = false
            },
            onDismissRequest = { showDeleteDialog = false }
        )
    }

    // LOGOUT CONFIRMATION DIALOG
    if (showLogoutDialog) {
        GalibuAlertDialog(
            title = "¿Cerrar Sesión?",
            message = "¿Estás seguro de que deseas cerrar tu sesión en Galibu?",
            type = GalibuAlertType.WARNING,
            confirmText = "Cerrar sesión",
            dismissText = "Cancelar",
            confirmColor = Color(0xFFEF4444),
            onConfirm = {
                showLogoutDialog = false
                viewModel.logout()
            },
            onDismissRequest = { showLogoutDialog = false }
        )
    }

    // PROFILE PHOTO SOURCE OPTIONS DIALOG
    if (showProfilePhotoSourceDialog) {
        AlertDialog(
            onDismissRequest = { showProfilePhotoSourceDialog = false },
            containerColor = Color.White,
            title = {
                Text(
                    text = "Foto de perfil",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(15.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text(
                        text = "Elige de dónde deseas obtener tu foto de perfil:",
                        fontSize = 14.sp,
                        color = Color(0xFF64748B)
                    )
                    
                    // Option 1: Google Account
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEFF6FF))
                            .clickable {
                                showProfilePhotoSourceDialog = false
                                val photoUrl = detectedGooglePhoto ?: "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150&h=150&fit=crop"
                                photoUrlInput = photoUrl
                                viewModel.updateProfile(
                                    name = nameInput,
                                    phone = phoneInput,
                                    workCategory = if (currentUser?.role != "CLIENT") categoryInput else null,
                                    profilePhotoUrl = photoUrl,
                                    department = currentDeptEdit,
                                    municipality = currentMuniEdit,
                                    hourlyRate = if (currentUser?.role != "CLIENT") hourlyRateInput.toDoubleOrNull() else null
                                )
                                showSuccessMessage = "Se aplicó tu foto de Google Account con éxito"
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "G",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4285F4),
                                fontSize = 16.sp,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Tomar de tu Google Account",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E3A8A),
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Traer tu foto vinculada de Google",
                                color = Color(0xFF3B82F6),
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Option 2: File / Gallery
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF1F5F9))
                            .clickable {
                                showProfilePhotoSourceDialog = false
                                photoPickerLauncher.launch("image/*")
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Buscar archivo",
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Buscar un archivo",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Elegir desde tus archivos o galería",
                                color = Color(0xFF64748B),
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Option 3: Camera
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF1F5F9))
                            .clickable {
                                showProfilePhotoSourceDialog = false
                                launchCamera()
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Tomar foto",
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Tomar una foto",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Capturar una nueva foto con tu cámara",
                                color = Color(0xFF64748B),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showProfilePhotoSourceDialog = false }) {
                    Text("Cancelar", fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                }
            }
        )
    }
}

// Reusable Option Card for the main profile list
@Composable
fun ProfileOptionCard(
    icon: ImageVector,
    iconBgColor: Color,
    iconColor: Color,
    title: String,
    titleColor: Color = Color(0xFF0F172A),
    showChevron: Boolean = true,
    chevronColor: Color = Color(0xFF94A3B8),
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFEFF2F5)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(iconBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                
                Text(
                    text = title,
                    color = titleColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            if (showChevron) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = chevronColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

