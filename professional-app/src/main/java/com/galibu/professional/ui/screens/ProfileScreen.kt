package com.galibu.professional.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.galibu.core.ui.utils.ImageUtils
import com.galibu.core.ui.utils.PhotoUtils
import com.galibu.core.ui.utils.capitalizeFirstLetter
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.galibu.core.ui.theme.*
import com.galibu.core.ui.components.CategoryMultiSelectField
import com.galibu.core.ui.components.GalibuAlertDialog
import com.galibu.core.ui.components.GalibuAlertType
import com.galibu.core.ui.components.ServiceCategoryTag
import com.galibu.core.data.model.hasMissingRequiredFields
import com.galibu.professional.ui.viewmodels.ProfessionalViewModel

enum class ProfileSubScreen {
    EDIT_INFO,
    CHANGE_PHOTO,
    CHANGE_PASSWORD
}

/** Límite de fotos del portafolio para no inflar el perfil ni la base local. */
private const val MAX_PORTFOLIO_PHOTOS = 6

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfessionalViewModel,
    modifier: Modifier = Modifier,
    onNavigateToNotifications: () -> Unit = {},
    forceEdit: Boolean = false,
    onSubScreenOpen: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    
    // State variables
    var nameInput by remember(currentUser) { mutableStateOf(currentUser?.name ?: "") }
    var phoneInput by remember(currentUser) { mutableStateOf(currentUser?.phone ?: "") }
    var categoryInput by remember(currentUser) { mutableStateOf(currentUser?.workCategory ?: "") }
    var hourlyRateInput by remember(currentUser) { mutableStateOf(currentUser?.hourlyRate?.toInt()?.toString() ?: "") }
    var photoUrlInput by remember(currentUser) { mutableStateOf(currentUser?.profilePhotoUrl ?: "") }
    var addressInput by remember(currentUser) { mutableStateOf(currentUser?.address ?: "") }

    // Portafolio: se inicializa por id para no perder lo escrito cuando cambia
    // la foto de perfil (ese flujo ya guarda el perfil y emite un nuevo usuario).
    var workDescriptionInput by remember(currentUser?.workDescription) {
        mutableStateOf(currentUser?.workDescription ?: "")
    }
    val portfolioPhotos = remember(currentUser?.workPhotos) {
        mutableStateListOf<String>().apply {
            addAll(
                currentUser?.workPhotos
                    ?.split("|")
                    ?.map { it.trim() }
                    ?.filter { it.isNotBlank() }
                    ?: emptyList()
            )
        }
    }
    val savedPortfolioPhotos = remember(currentUser?.workPhotos) {
        currentUser?.workPhotos
            ?.split("|")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?: emptyList()
    }
    val savedWorkDescription = currentUser?.workDescription?.trim().orEmpty()
    var portfolioPreviewPhoto by remember { mutableStateOf<String?>(null) }
    
    // Subscreen state
    var activeSubScreen by remember { mutableStateOf<ProfileSubScreen?>(null) }

    // Completado forzoso: abrir edición y no permitir salir hasta completar.
    LaunchedEffect(forceEdit) {
        if (forceEdit && activeSubScreen == null) {
            activeSubScreen = ProfileSubScreen.EDIT_INFO
        }
    }

    // Avisa al contenedor para ocultar el menú inferior en subformularios.
    LaunchedEffect(activeSubScreen) {
        onSubScreenOpen(activeSubScreen != null)
    }
    
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
    var gpsDetectedDept by remember { mutableStateOf<String?>(null) }
    var gpsDetectedMuni by remember { mutableStateOf<String?>(null) }
    var isFetchingGps by remember { mutableStateOf(false) }
    
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
    var showEditCategoryDialog by remember { mutableStateOf(false) }
    var customCategoryInput by remember { mutableStateOf("") }
    val editCategories = remember(currentUser) {
        mutableStateListOf<String>().apply {
            addAll(
                currentUser?.workCategory?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
            )
        }
    }

    fun requestGpsForProfile() {
        if (!isDeviceLocationEnabled(context)) {
            viewModel.showAlert("Aviso", "La ubicación está desactivada. Actívala para usar el GPS o escribe la dirección manualmente.")
            try {
                context.startActivity(
                    android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                )
            } catch (e: Exception) {}
            return
        }
        isFetchingGps = true
        getCurrentGpsLocation(
            context,
            currentDeptEdit,
            currentMuniEdit,
            onLocationFetched = { result ->
                addressInput = result
                isFetchingGps = false
            },
            onGpsUnavailable = { isFetchingGps = false },
            onDetailedFix = { fix ->
                gpsDetectedDept = fix.adminArea
                gpsDetectedMuni = fix.locality
                // Auto-selecciona lo detectado (el usuario puede cambiarlo después).
                matchCatalogName(departmentsAndMunicipalities.map { it.first }, fix.adminArea)?.let { matchedDept ->
                    currentDeptEdit = matchedDept
                    val munis = departmentsAndMunicipalities.first { it.first == matchedDept }.second
                    matchCatalogName(munis, fix.locality)?.let { currentMuniEdit = it }
                }
            }
        )
    }

    val profileGpsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            requestGpsForProfile()
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

    val portfolioPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            if (portfolioPhotos.size >= MAX_PORTFOLIO_PHOTOS) {
                viewModel.showWarning("Puedes agregar hasta $MAX_PORTFOLIO_PHOTOS fotos al portafolio.")
            } else {
                val base64 = ImageUtils.uriToBase64(context, uri, maxDim = 640)
                if (base64 != null) {
                    portfolioPhotos.add(base64)
                } else {
                    viewModel.showError("No pudimos procesar esa imagen. Intenta con otra foto.")
                }
            }
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

    // Refresca departamentos y categorías del API al entrar.
    LaunchedEffect(Unit) {
        viewModel.refreshGeoConfig()
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
                            onClick = {
                                if (forceEdit && (currentUser?.hasMissingRequiredFields() == true)) {
                                    viewModel.showError("Completa tu perfil para continuar usando la app.")
                                } else {
                                    activeSubScreen = null
                                }
                            },
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

                    // Foto de perfil (obligatoria): vista previa + subir.
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
                                        .border(3.dp, PolishBlue, CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(PolishIce),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = PolishBlue,
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                            }

                            // Ícono de cámara para cambiar la foto
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


                    // Dirección Field (Ubicación con GPS: primero).
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Ubicación",
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
                            trailingIcon = {
                                IconButton(onClick = {
                                    val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                    val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                    if (hasFine || hasCoarse) {
                                        requestGpsForProfile()
                                    } else {
                                        profileGpsPermissionLauncher.launch(
                                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                                        )
                                    }
                                }) {
                                    if (isFetchingGps) {
                                        CircularProgressIndicator(
                                            color = Color(0xFF1764FF),
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.MyLocation,
                                            contentDescription = "Usar mi ubicación (GPS)",
                                            tint = Color(0xFF1764FF),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
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
                                val orderedDepts = remember(departmentsAndMunicipalities, gpsDetectedDept) {
                                    val matched = matchCatalogName(
                                        departmentsAndMunicipalities.map { it.first },
                                        gpsDetectedDept
                                    )
                                    if (matched == null) {
                                        departmentsAndMunicipalities
                                    } else {
                                        val rest = departmentsAndMunicipalities.filter { it.first != matched }
                                        listOf(departmentsAndMunicipalities.first { it.first == matched }) + rest
                                    }
                                }
                                orderedDepts.forEach { (dept, munis) ->
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

                    // Municipio Field
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Municipio",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Color(0xFF0F172A)
                        )
                        Box(modifier = Modifier.fillMaxWidth()) {
                            val baseMunis = remember(currentDeptEdit, departmentsAndMunicipalities) {
                                departmentsAndMunicipalities.firstOrNull { it.first == currentDeptEdit }?.second ?: listOf("Bogotá")
                            }
                            // Si el departamento elegido es el detectado por GPS, su municipio va primero.
                            val gpsMatchedDeptHere = matchCatalogName(
                                departmentsAndMunicipalities.map { it.first },
                                gpsDetectedDept
                            )
                            val currentMunis = if (currentDeptEdit.isNotEmpty() && currentDeptEdit == gpsMatchedDeptHere) {
                                val matchedMuni = matchCatalogName(baseMunis, gpsDetectedMuni)
                                if (matchedMuni != null) {
                                    listOf(matchedMuni) + baseMunis.filter { it != matchedMuni }
                                } else {
                                    baseMunis
                                }
                            } else {
                                baseMunis
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
                                            contentDescription = "Mostrar municipios",
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


                    // Categoría: mismo selector compartido del registro.
                    if (currentUser?.role != "CLIENT") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            val firestoreCatListHere by viewModel.firestoreCategories.collectAsState()
                            val editBaseCategories = remember(firestoreCatListHere) {
                                if (firestoreCatListHere.isNotEmpty()) {
                                    firestoreCatListHere.map { it.name }
                                } else {
                                    listOf(
                                        "Plomería", "Electricidad", "Carpintería", "Pintura", "Albañilería",
                                        "Jardinería", "Cerrajería", "Fumigación", "Mecánica", "Soldadura",
                                        "Climatización", "Limpieza", "Mudanzas", "Techados"
                                    )
                                }
                            }
                            val otherCategory = "Otro +"
                            val editOptions = (
                                editBaseCategories.filter { !it.contains("Otro", ignoreCase = true) } +
                                    editCategories.filter { it !in editBaseCategories } +
                                    otherCategory
                                ).distinct()

                            CategoryMultiSelectField(
                                selected = editCategories,
                                options = editOptions,
                                label = "Categoría",
                                placeholder = "Seleccionar categorías",
                                accentColor = Color(0xFF1764FF),
                                borderColor = Color(0xFFE2E8F0),
                                containerColor = Color.White,
                                textColor = Color(0xFF0F172A),
                                mutedTextColor = Color(0xFF64748B),
                                customOptionLabel = otherCategory,
                                onAddCustom = { showEditCategoryDialog = true }
                            )
                        }
                    }

                    if (showEditCategoryDialog) {
                        AlertDialog(
                            onDismissRequest = {
                                showEditCategoryDialog = false
                                customCategoryInput = ""
                            },
                            title = {
                                Text(
                                    text = "Agregar categoría",
                                    color = Color(0xFF0F172A),
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            text = {
                                OutlinedTextField(
                                    value = customCategoryInput,
                                    onValueChange = { customCategoryInput = it.capitalizeFirstLetter() },
                                    label = { Text("Nombre de la categoría") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF1764FF),
                                        unfocusedBorderColor = Color(0xFFE2E8F0)
                                    )
                                )
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        val trimmed = customCategoryInput.trim()
                                        if (trimmed.isNotEmpty() && editCategories.none { it.equals(trimmed, ignoreCase = true) }) {
                                            editCategories.add(trimmed)
                                        }
                                        customCategoryInput = ""
                                        showEditCategoryDialog = false
                                    }
                                ) {
                                    Text("Agregar", color = Color(0xFF1764FF), fontWeight = FontWeight.SemiBold)
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = {
                                        customCategoryInput = ""
                                        showEditCategoryDialog = false
                                    }
                                ) {
                                    Text("Cancelar", color = Color(0xFF64748B))
                                }
                            },
                            containerColor = Color.White,
                            shape = RoundedCornerShape(16.dp)
                        )
                    }

                    // Portafolio: descripción y fotos de trabajos anteriores.
                    if (currentUser?.role != "CLIENT") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFEFF6FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoLibrary,
                                        contentDescription = null,
                                        tint = Color(0xFF1764FF),
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Portafolio de trabajos",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "Muestra tus mejores trabajos a los clientes",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = workDescriptionInput,
                                onValueChange = { newValue ->
                                    if (newValue.length <= 300) workDescriptionInput = newValue.capitalizeFirstLetter()
                                },
                                placeholder = {
                                    Text(
                                        "Ej: Instalaciones eléctricas, reparaciones locativas y mantenimiento preventivo.",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 13.sp
                                    )
                                },
                                minLines = 3,
                                maxLines = 5,
                                shape = RoundedCornerShape(14.dp),
                                supportingText = {
                                    Text(
                                        text = "${workDescriptionInput.length}/300",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 10.sp,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.End
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("edit_portfolio_description"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedBorderColor = Color(0xFF1764FF),
                                    unfocusedBorderColor = Color(0xFFE2E8F0),
                                    focusedTextColor = Color(0xFF0F172A),
                                    unfocusedTextColor = Color(0xFF0F172A)
                                )
                            )

                            Text(
                                text = "Fotos (${portfolioPhotos.size}/$MAX_PORTFOLIO_PHOTOS)",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = Color(0xFF0F172A)
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (portfolioPhotos.size < MAX_PORTFOLIO_PHOTOS) {
                                    Box(
                                        modifier = Modifier
                                            .size(90.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White)
                                            .border(
                                                width = 1.dp,
                                                color = Color(0xFFCBD5E1),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable {
                                                portfolioPhotoPickerLauncher.launch("image/*")
                                            }
                                            .testTag("edit_portfolio_add_photo"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Agregar foto",
                                                tint = Color(0xFF64748B),
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Agregar",
                                                color = Color(0xFF64748B),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }

                                portfolioPhotos.forEach { photo ->
                                    Box(
                                        modifier = Modifier
                                            .size(90.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    ) {
                                        AsyncImage(
                                            model = ImageUtils.getCoilModel(photo),
                                            contentDescription = "Foto del portafolio",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clickable { portfolioPreviewPhoto = photo }
                                        )
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .size(24.dp)
                                                .background(
                                                    Color.Black.copy(alpha = 0.6f),
                                                    CircleShape
                                                )
                                                .clickable { portfolioPhotos.remove(photo) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Quitar foto",
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Text(
                                text = "Toca una foto para verla en grande. Máximo $MAX_PORTFOLIO_PHOTOS fotos.",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                // Botón al final del contenido scrolleable (no overlay).
                Button(
                        onClick = {
                            val trimmedName = nameInput.trim()
                            val trimmedDept = currentDeptEdit.trim()
                            val trimmedMuni = currentMuniEdit.trim()
                            val trimmedAddress = addressInput.trim()

                            if (trimmedName.isEmpty()) {
                                viewModel.showError("Por favor complete todos los campos obligatorios.")
                                return@Button
                            }
                            if (photoUrlInput.isNullOrBlank()) {
                                viewModel.showError("Sube tu foto de perfil para continuar.")
                                return@Button
                            }
                            if (editCategories.isEmpty()) {
                                viewModel.showError("Selecciona al menos una categoría.")
                                return@Button
                            }
                            if (trimmedDept.isEmpty() || trimmedMuni.isEmpty() || trimmedAddress.isEmpty()) {
                                viewModel.showError("Completa departamento, municipio y dirección para continuar.")
                                return@Button
                            }

                            viewModel.updateProfile(
                                name = trimmedName,
                                phone = currentUser?.phone ?: "",
                                workCategory = editCategories.joinToString(", "),
                                profilePhotoUrl = photoUrlInput,
                                department = trimmedDept,
                                municipality = trimmedMuni,
                                address = trimmedAddress,
                                hourlyRate = currentUser?.hourlyRate,
                                workPhotos = portfolioPhotos.toList(),
                                workDescription = workDescriptionInput.trim()
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
                            viewModel.changePassword(currentPasswordInput.trim(), newPasswordInput.trim()) { errorMsg ->
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
                        fontSize = 24.sp,
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
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Foto de perfil (solo lectura; se cambia desde Editar perfil)
                            if (!photoUrlInput.isNullOrEmpty()) {
                                val decodedModel = remember(photoUrlInput) { PhotoUtils.decodeBase64ToBitmap(photoUrlInput) }
                                AsyncImage(
                                    model = decodedModel,
                                    contentDescription = "Foto de Perfil",
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(CircleShape)
                                        .border(3.dp, PolishBlue, CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(CircleShape)
                                        .background(PolishIce)
                                        .border(3.dp, PolishBlue, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = PolishBlue,
                                        modifier = Modifier.size(50.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Nombre
                            Text(
                                text = currentUser?.name ?: "Usuario",
                                color = Color(0xFF0F172A),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Email
                            Text(
                                text = currentUser?.email ?: "",
                                color = Color(0xFF64748B),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )

                            // Categorías

            }

                    // Options List (Cleaned according to prompt - Mi ubicación, notificaciones, firebase removed)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ProfileOptionCard(
                            icon = Icons.Default.Edit,
                            iconBgColor = Color(0xFFEFF6FF),
                            iconColor = Color(0xFF2563EB),
                            title = "Editar perfil",
                            onClick = { 
                                showSuccessMessage = null
                                activeSubScreen = ProfileSubScreen.EDIT_INFO 
                            }
                        )

                        ProfileOptionCard(
                            icon = Icons.Default.Lock,
                            iconBgColor = Color(0xFFEFF6FF),
                            iconColor = Color(0xFF2563EB),
                            title = "Cambiar contraseña",
                            onClick = { 
                                showSuccessMessage = null
                                activeSubScreen = ProfileSubScreen.CHANGE_PASSWORD 
                            }
                        )
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
                            iconBgColor = Color(0xFFEFF6FF),
                            iconColor = Color(0xFF2563EB),
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
                        Image(
                            painter = painterResource(id = com.galibu.professional.R.drawable.img_splash_logo),
                            contentDescription = "Galibu",
                            modifier = Modifier.size(width = 27.dp, height = 32.dp),
                            contentScale = ContentScale.Fit
                        )
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

    // Vista ampliada de una foto del portafolio.
    }
    portfolioPreviewPhoto?.let { photo ->
        Dialog(
            onDismissRequest = { portfolioPreviewPhoto = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageUtils.getCoilModel(photo),
                    contentDescription = "Foto del portafolio ampliada",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 560.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(32.dp)
                        .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                        .clickable { portfolioPreviewPhoto = null },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar foto",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
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

    // DELETE ACCOUNT CONFIRMATION DIALOG (homologado con la app de clientes)
    if (showDeleteDialog) {
        GalibuAlertDialog(
            title = "¿Eliminar tu Cuenta de Galibu?",
            message = "Esta acción es irreversible y eliminará todo tu historial de servicios contratados, tus datos de contacto y tu billetera de saldo. ¿Realmente deseas continuar?",
            type = GalibuAlertType.ERROR,
            confirmText = "Eliminar",
            dismissText = "Cancelar",
            confirmColor = Color(0xFFEF4444),
            onConfirm = {
                showDeleteDialog = false
                viewModel.deleteAccount { error ->
                    if (error != null) {
                        viewModel.showAlert("No se pudo eliminar la cuenta", error, true)
                    }
                }
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
                    
                    // Opción 1: Galería
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

                    // Opción 2: Cámara
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

