package com.galibu.core.ui.screens.auth

import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.Manifest
import android.os.Bundle
import android.location.Geocoder
import java.util.Locale
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.util.Log
import coil.compose.AsyncImage
import com.galibu.core.ui.theme.*
import com.galibu.core.ui.utils.capitalizeFirstLetter
import com.galibu.core.ui.components.CategoryMultiSelectField
import com.galibu.core.ui.components.GalibuAlertDialog
import com.galibu.core.ui.components.GalibuAlertType
import com.galibu.core.ui.components.PasswordField
import com.galibu.core.ui.components.PhotoSourceDialog
import com.galibu.core.ui.components.rememberCameraCaptureLauncher
import com.galibu.core.ui.viewmodels.SharedViewModel
import com.galibu.core.ui.utils.ImageUtils
import com.galibu.core.ui.utils.GpsFix
import com.galibu.core.ui.utils.getCurrentGpsLocation
import com.galibu.core.ui.utils.isDeviceLocationEnabled
import com.galibu.core.ui.utils.matchCatalogName
import kotlinx.coroutines.launch
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import androidx.activity.ComponentActivity

// ==================== COLORES CONSTANTES ====================
private val brandBlue = Color(0xFF0066FF)
private val lightBlueActive = Color(0xFFE8F0FE)
private val textMain = Color(0xFF0F172A)
private val textSec = Color(0xFF64748B)
private val borderLight = Color(0xFFE2E8F0)
private val bgLightField = Color(0xFFF8FAFC)
private val successGreen = Color(0xFF10B981)
private val errorRed = Color(0xFFEF4444)
private val warningYellow = Color(0xFFD97706)
private val lightRedBg = Color(0xFFFEE2E2)
private val lightYellowBg = Color(0xFFFEF08A)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: SharedViewModel,
    logoResId: Int,
    googleIconResId: Int,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    // 0 = Login Screen, 1 = Step 1 (Rol), 2 = Step 2 (Foto & Datos), 3 = Step 3 (Ubicación), 4 = Step 4 (Contraseña)
    var registrationStep by rememberSaveable { mutableStateOf(0) }
    
    // ==================== DIÁLOGOS ====================
    var showErrorDialog by remember { mutableStateOf(false) }
    var dialogErrorMessage by remember { mutableStateOf("") }
    var dialogTitle by remember { mutableStateOf("Error de Autenticación") }

    var showGoogleSuccessDialog by remember { mutableStateOf(false) }
    var googleSuccessMessage by remember { mutableStateOf("") }
    
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var recoveryEmail by remember { mutableStateOf("") }
    var recoveryCode by remember { mutableStateOf("") }
    var recoveryNewPassword by remember { mutableStateOf("") }
    var recoveryConfirmNewPassword by remember { mutableStateOf("") }
    
    // ==================== ESTADOS DE CARGA ====================
    var isCheckingEmail by remember { mutableStateOf(false) }
    var isLoggingIn by remember { mutableStateOf(false) }
    var isRegistering by remember { mutableStateOf(false) }
    var isGoogleAuthenticating by remember { mutableStateOf(false) }
    
    // ==================== CAMPOS DE AUTENTICACIÓN ====================
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var hourlyRateInput by rememberSaveable { mutableStateOf("") }
    var selectedRole by rememberSaveable { mutableStateOf("CLIENT") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    
    // ==================== ESPECIALIDADES Y FOTOS ====================
    val specialtiesList = remember {
        listOf(
            "Plomería", "Electricidad", "Carpintería", "Pintura", "Albañilería", 
            "Jardinería", "Cerrajería", "Fumigación", "Mecánica", "Soldadura", 
            "Climatización", "Limpieza", "Mudanzas", "Techados"
        )
    }
    val selectedSpecialties = remember { mutableStateListOf<String>() }
    val workPhotos = remember { mutableStateListOf<String>() }

    // ==================== UBICACIÓN Y DEPARTAMENTOS ====================
    val firestoreDeptList by viewModel.firestoreDepartments.collectAsStateWithLifecycle()
    val firestoreCatList by viewModel.firestoreCategories.collectAsStateWithLifecycle()

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

    var selectedDept by rememberSaveable { mutableStateOf("Departamento") }
    var selectedMuni by rememberSaveable { mutableStateOf("Municipio") }
    var showDeptDropdown by remember { mutableStateOf(false) }
    var showMuniDropdown by remember { mutableStateOf(false) }
    var addressManualOrGps by rememberSaveable { mutableStateOf("") }
    var isSimulatingGpsReg by remember { mutableStateOf(false) }
    // División detectada por GPS en el registro (para ordenar los catálogos).
    var gpsRegDept by remember { mutableStateOf<String?>(null) }
    var gpsRegMuni by remember { mutableStateOf<String?>(null) }

    // ==================== ESTADOS DE FLUJO ====================
    var isGoogleAutocomplete by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var acceptedTerms by remember { mutableStateOf(false) }
    var showTermsContract by remember { mutableStateOf(false) }
    var signupPhotoUri by remember { mutableStateOf<String?>(null) }
    var showProfilePhotoSourceDialog by remember { mutableStateOf(false) }
    var googleIdToken by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // ==================== LAUNCHED EFFECTS ====================
    LaunchedEffect(firestoreDeptList) {
        if (firestoreDeptList.isNotEmpty()) {
            departmentsAndMunicipalities = firestoreDeptList.map { it.name to it.municipalities }
        }
    }

    LaunchedEffect(departmentsAndMunicipalities) {
        if (departmentsAndMunicipalities.isNotEmpty()) {
            val hasSelectedDept = departmentsAndMunicipalities.any { it.first == selectedDept }
            if (!hasSelectedDept && selectedDept != "Departamento") {
                selectedDept = departmentsAndMunicipalities.first().first
                selectedMuni = departmentsAndMunicipalities.first().second.firstOrNull() ?: ""
            }
        }
    }

    LaunchedEffect(registrationStep) {
        errorMessage = null
        if (registrationStep <= 1) {
            isGoogleAutocomplete = false
        }
    }

    // Los errores se muestran con la alerta global de Galibu (card unificada).
    LaunchedEffect(errorMessage) {
        val message = errorMessage
        if (!message.isNullOrBlank()) {
            viewModel.showError(message)
            errorMessage = null
        }
    }

    // ==================== PERMISOS Y LAUNCHERS ====================
    // Detección GPS solo para ordenar departamento/municipio (no autoselecciona).
    val detectGpsForOrdering: () -> Unit = {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (locationManager != null && !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            viewModel.showAlert("Aviso", "El GPS está apagado. Por favor enciéndelo en los ajustes para detectar tu ubicación.")
            try {
                val intent = android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                context.startActivity(intent)
            } catch (e: Exception) {}
        } else {
            isSimulatingGpsReg = true
            getCurrentGpsLocation(
                context,
                selectedDept,
                selectedMuni,
                onLocationFetched = { address ->
                    addressManualOrGps = address
                    isSimulatingGpsReg = false
                },
                onGpsUnavailable = { isSimulatingGpsReg = false },
                onDetailedFix = { fix ->
                    gpsRegDept = fix.adminArea
                    gpsRegMuni = fix.locality
                    // Auto-selecciona lo detectado (el usuario puede cambiarlo después).
                    matchCatalogName(departmentsAndMunicipalities.map { it.first }, fix.adminArea)?.let { matchedDept ->
                        selectedDept = matchedDept
                        val munis = departmentsAndMunicipalities.first { it.first == matchedDept }.second
                        matchCatalogName(munis, fix.locality)?.let { selectedMuni = it }
                    }
                }
            )
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                          permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (granted) {
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                if (locationManager != null && !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    viewModel.showAlert("Aviso", "Permiso concedido, pero el GPS está apagado. Por favor enciéndelo en los ajustes.")
                    try {
                        val intent = android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        context.startActivity(intent)
                    } catch (e: Exception) {}
                } else if (selectedDept != "Departamento" && selectedMuni != "Municipio") {
                    isSimulatingGpsReg = true
                    getCurrentGpsLocation(
                        context,
                        selectedDept,
                        selectedMuni,
                        onLocationFetched = { address ->
                            addressManualOrGps = address
                            isSimulatingGpsReg = false
                        },
                        onDetailedFix = { fix ->
                            gpsRegDept = fix.adminArea
                            gpsRegMuni = fix.locality
                        }
                    )
                } else {
                    detectGpsForOrdering()
                }
            } else {
                viewModel.showAlert("Aviso", "Permiso de ubicación denegado. Se usará una dirección de muestra.")
                addressManualOrGps = if (selectedDept != "Departamento" && selectedMuni != "Municipio") {
                    "Calle 100 # 15-22, " + selectedMuni
                } else {
                    "Calle 100 # 15-22"
                }
            }
        }
    )

    val fetchLocationByGps = {
        // Sin pre-requisitos: la dirección va primero y el GPS detecta y ordena.
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (locationManager != null && !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            viewModel.showAlert("Aviso", "El GPS está apagado. Por favor enciéndelo en los ajustes.")
            try {
                val intent = android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                context.startActivity(intent)
            } catch (e: Exception) {}
        } else {
            val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (hasFine || hasCoarse) {
                detectGpsForOrdering()
            } else {
                locationPermissionLauncher.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                )
            }
        }
    }

    // La detección GPS es manual (botón): el campo llega vacío.

    val signupPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val base64 = ImageUtils.uriToBase64(context, uri)
            signupPhotoUri = base64 ?: uri.toString()
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
                signupPhotoUri = "data:image/jpeg;base64,$base64Str"
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

    // Fotos de trabajos: galería o cámara
    var showWorkPhotoSourceDialog by remember { mutableStateOf(false) }
    val workPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val base64 = ImageUtils.uriToBase64(context, uri)
            workPhotos.add(base64 ?: uri.toString())
        }
    }

    val launchWorkCamera = rememberCameraCaptureLauncher(
        maxDim = 640,
        onImageCaptured = { base64 -> workPhotos.add(base64) },
        onError = { message -> viewModel.showAlert("Aviso", message) }
    )

    // ==================== GOOGLE SIGN-IN ====================
    fun handleGoogleAccountSelect(selectedEmail: String, idToken: String?, displayName: String?, photoUrl: String?) {
        if (isGoogleAuthenticating) return
        isGoogleAuthenticating = true
        errorMessage = null
        googleIdToken = idToken
        scope.launch {
            try {
                val normalizedEmail = selectedEmail.trim().lowercase()

                if (idToken.isNullOrEmpty()) {
                    isGoogleAuthenticating = false
                    errorMessage = "No se pudo obtener credenciales de Google. Intenta nuevamente."
                    return@launch
                }

                // Backend finds-or-creates the user and establishes the JWT session.
                viewModel.loginWithGoogle(
                    idToken = idToken,
                    email = normalizedEmail,
                    name = displayName ?: "",
                    avatarUrl = photoUrl,
                    role = selectedRole
                ) { success ->
                    isGoogleAuthenticating = false
                    if (!success) {
                        errorMessage = "No se pudieron verificar las credenciales con Google."
                        return@loginWithGoogle
                    }
                    val user = viewModel.currentUser.value
                    if (user == null) {
                        errorMessage = "No se pudieron verificar las credenciales con Google."
                        return@loginWithGoogle
                    }

                    // Intención del wizard vs. capacidad real de la cuenta.
                    // La capacidad no cambia por omisión ni sin validación; la otra
                    // se activa desde Perfil.
                    val accountRole = user.role.uppercase()
                    val hasClient = accountRole == "CLIENT" || accountRole == "BOTH"
                    val hasPro = accountRole == "PROFESSIONAL" || accountRole == "BOTH"
                    val wantsClient = selectedRole != "PROFESSIONAL"
                    val wantsPro = selectedRole != "CLIENT"
                    if (wantsPro && !hasPro) {
                        viewModel.showInfo(
                            "Entraste como cliente. Tu cuenta no tiene capacidad profesional; actívala desde Perfil > Ofrecer mis servicios."
                        )
                    } else if (wantsClient && !hasClient) {
                        viewModel.showInfo(
                            "Entraste como profesional. Tu cuenta no tiene capacidad cliente; actívala desde Perfil > Buscar un profesional."
                        )
                    }

                    // El backend ya define el rol: default CLIENT para cuentas nuevas,
                    // y respeta la capacidad existente para cuentas ya creadas. El modo
                    // activo se resuelve en SharedViewModel (persistido o capacidad).
                    onAuthSuccess()
                }
            } catch (e: Exception) {
                Log.e("AuthScreen", "Error in Google sign-in flow: ${e.localizedMessage}")
                isGoogleAuthenticating = false
                errorMessage = "Ocurrió un error inesperado. Por favor, intenta nuevamente."
            }
        }
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        dialogTitle = "Error de Autenticación"
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account = task.getResult(ApiException::class.java)
                val email = account?.email ?: ""
                val idToken = account?.idToken
                val displayName = account?.displayName
                val photoUrl = account?.photoUrl?.toString()
                if (email.isNotEmpty()) {
                    handleGoogleAccountSelect(email, idToken, displayName, photoUrl)
                } else {
                    dialogErrorMessage = "❌ No se pudo obtener el correo de tu cuenta de Google.\n\n" +
                                         "Asegúrate de haber seleccionado una cuenta válida y tener conexión a internet."
                    showErrorDialog = true
                }
            } catch (e: ApiException) {
                Log.e("AuthScreen", "Google Sign-In ApiException: ${e.statusCode}", e)
                
                if (e.statusCode == 12501) {
                    Log.d("AuthScreen", "Google Sign-In cancelado por el usuario (12501)")
                } else {
                    val errorMsg = when (e.statusCode) {
                        12500 -> "❌ Error de autenticación con Google.\n\nEl servidor de Google no pudo autenticar tu cuenta. Por favor, intenta nuevamente."
                        12502 -> "❌ Error de conexión.\n\nNo pudimos conectar con el servicio de Google. Verifica tu conexión a internet."
                        12503 -> "❌ Error de inicio de sesión.\n\nNo pudimos completar el inicio de sesión con Google. Por favor, intenta nuevamente."
                        else -> "❌ Error en Google Sign-In (${e.statusCode})\n\n${e.localizedMessage ?: "Error desconocido"}\n\nPor favor, intenta nuevamente o usa tu correo y contraseña."
                    }
                    dialogErrorMessage = errorMsg
                    showErrorDialog = true
                }
            }
        } else {
            Log.w("AuthScreen", "Google Sign-In cancelado/no-OK: ${result.resultCode}")
        }
    }

    fun launchNativeGoogleSignIn() {
        dialogTitle = "Error de Autenticación"
        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken("692995531925-vih27djj3vof10iujkgv18c4qbdqrmag.apps.googleusercontent.com")
                .build()
            
            val googleSignInClient = GoogleSignIn.getClient(context as ComponentActivity, gso)
            googleSignInClient.signOut().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                googleSignInLauncher.launch(signInIntent)
            }
        } catch (e: Exception) {
            Log.e("AuthScreen", "Error launching Google Sign-In", e)
            errorMessage = "Google Sign-In no disponible en este dispositivo."
        }
    }

    fun signInWithGoogle() {
        errorMessage = null
        launchNativeGoogleSignIn()
    }

    // ==================== UI PRINCIPAL ====================
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        if (showForgotPasswordDialog) {
            ForgotPasswordRecoveryScreen(
                viewModel = viewModel,
                email = recoveryEmail,
                onEmailChange = { recoveryEmail = it },
                code = recoveryCode,
                onCodeChange = { input ->
                    recoveryCode = input.filter { it.isDigit() }.take(6)
                },
                newPassword = recoveryNewPassword,
                onNewPasswordChange = { recoveryNewPassword = it },
                confirmNewPassword = recoveryConfirmNewPassword,
                onConfirmNewPasswordChange = { recoveryConfirmNewPassword = it },
                onDismiss = { showForgotPasswordDialog = false },
                onPasswordUpdated = { showForgotPasswordDialog = false }
            )
        } else {
            AuthContent(
                logoResId = logoResId,
                googleIconResId = googleIconResId,
                registrationStep = registrationStep,
                onRegistrationStepChange = { registrationStep = it },
                selectedRole = selectedRole,
                onSelectedRoleChange = { selectedRole = it },
                email = email,
                onEmailChange = { email = it },
                password = password,
                onPasswordChange = { password = it },
                confirmPassword = confirmPassword,
                onConfirmPasswordChange = { confirmPassword = it },
                name = name,
                onNameChange = { name = it },
                phone = phone,
                onPhoneChange = { phone = it },
                hourlyRateInput = hourlyRateInput,
                onHourlyRateInputChange = { input ->
                    val clean = input.filter { it.isDigit() }
                    if (clean.length <= 9) {
                        hourlyRateInput = com.galibu.core.ui.utils.PriceUtils.formatThousands(clean)
                    }
                },
                selectedSpecialties = selectedSpecialties,
                workPhotos = workPhotos,
                selectedDept = selectedDept,
                onSelectedDeptChange = { selectedDept = it },
                selectedMuni = selectedMuni,
                onSelectedMuniChange = { selectedMuni = it },
                addressManualOrGps = addressManualOrGps,
                onAddressManualOrGpsChange = { addressManualOrGps = it },
                signupPhotoUri = signupPhotoUri,
                onSignupPhotoUriChange = { signupPhotoUri = it },
                isGoogleAutocomplete = isGoogleAutocomplete,
                googleIdToken = googleIdToken,
                errorMessage = errorMessage,
                onErrorMessageChange = { errorMessage = it },
                acceptedTerms = acceptedTerms,
                onAcceptedTermsChange = { acceptedTerms = it },
                showTermsContract = showTermsContract,
                onShowTermsContractChange = { showTermsContract = it },
                showProfilePhotoSourceDialog = showProfilePhotoSourceDialog,
                onShowProfilePhotoSourceDialogChange = { showProfilePhotoSourceDialog = it },
                isCheckingEmail = isCheckingEmail,
                isLoggingIn = isLoggingIn,
                onIsLoggingInChange = { isLoggingIn = it },
                isRegistering = isRegistering,
                isGoogleAuthenticating = isGoogleAuthenticating,
                passwordVisible = passwordVisible,
                onPasswordVisibleChange = { passwordVisible = it },
                confirmPasswordVisible = confirmPasswordVisible,
                onConfirmPasswordVisibleChange = { confirmPasswordVisible = it },
                departmentsAndMunicipalities = departmentsAndMunicipalities,
                showDeptDropdown = showDeptDropdown,
                onShowDeptDropdownChange = { showDeptDropdown = it },
                showMuniDropdown = showMuniDropdown,
                onShowMuniDropdownChange = { showMuniDropdown = it },
                isSimulatingGpsReg = isSimulatingGpsReg,
                firestoreCatList = firestoreCatList,
                specialtiesList = specialtiesList,
                launchCamera = launchCamera,
                signupPhotoPickerLauncher = signupPhotoPickerLauncher,
                onAddWorkPhotoClick = { showWorkPhotoSourceDialog = true },
                fetchLocationByGps = fetchLocationByGps,
                gpsDetectedDept = gpsRegDept,
                gpsDetectedMuni = gpsRegMuni,
                viewModel = viewModel,
                onAuthSuccess = onAuthSuccess,
                context = context,
                scope = scope,
                onForgotPasswordClick = { showForgotPasswordDialog = true },
                onGoogleSignIn = { signInWithGoogle() }
            )
        }
    }

    // ==================== DIÁLOGOS ====================
    if (showProfilePhotoSourceDialog) {
        ProfilePhotoSourceDialog(
            onDismiss = { showProfilePhotoSourceDialog = false },
            onGallerySelected = {
                showProfilePhotoSourceDialog = false
                signupPhotoPickerLauncher.launch("image/*")
            },
            onCameraSelected = {
                showProfilePhotoSourceDialog = false
                launchCamera()
            }
        )
    }

    if (showWorkPhotoSourceDialog) {
        PhotoSourceDialog(
            title = "Agregar foto del trabajo",
            message = "Elige de dónde deseas obtener la foto:",
            onDismiss = { showWorkPhotoSourceDialog = false },
            onGallerySelected = {
                showWorkPhotoSourceDialog = false
                workPhotoPickerLauncher.launch("image/*")
            },
            onCameraSelected = {
                showWorkPhotoSourceDialog = false
                launchWorkCamera()
            }
        )
    }

    if (showTermsContract) {
        TermsAndConditionsDialog(
            onDismiss = { showTermsContract = false },
            onAccept = {
                acceptedTerms = true
                showTermsContract = false
            }
        )
    }

    if (showErrorDialog) {
        ErrorDialog(
            title = dialogTitle,
            message = dialogErrorMessage,
            onDismiss = {
                showErrorDialog = false
                dialogErrorMessage = ""
            }
        )
    }

    if (showGoogleSuccessDialog) {
        GoogleSuccessDialog(
            message = googleSuccessMessage,
            onDismiss = {
                showGoogleSuccessDialog = false
                googleSuccessMessage = ""
            }
        )
    }
}

// ==================== COMPOSABLES SEPARADOS ====================

// ForgotPasswordRecoveryScreen vive en PasswordRecoveryScreen.kt (mismo paquete).

@Composable
private fun AuthContent(
    logoResId: Int,
    googleIconResId: Int,
    registrationStep: Int,
    onRegistrationStepChange: (Int) -> Unit,
    selectedRole: String,
    onSelectedRoleChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    name: String,
    onNameChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    hourlyRateInput: String,
    onHourlyRateInputChange: (String) -> Unit,
    selectedSpecialties: MutableList<String>,
    workPhotos: MutableList<String>,
    selectedDept: String,
    onSelectedDeptChange: (String) -> Unit,
    selectedMuni: String,
    onSelectedMuniChange: (String) -> Unit,
    addressManualOrGps: String,
    onAddressManualOrGpsChange: (String) -> Unit,
    signupPhotoUri: String?,
    onSignupPhotoUriChange: (String?) -> Unit,
    isGoogleAutocomplete: Boolean,
    googleIdToken: String?,
    errorMessage: String?,
    onErrorMessageChange: (String?) -> Unit,
    acceptedTerms: Boolean,
    onAcceptedTermsChange: (Boolean) -> Unit,
    showTermsContract: Boolean,
    onShowTermsContractChange: (Boolean) -> Unit,
    showProfilePhotoSourceDialog: Boolean,
    onShowProfilePhotoSourceDialogChange: (Boolean) -> Unit,
    isCheckingEmail: Boolean,
    isLoggingIn: Boolean,
    onIsLoggingInChange: (Boolean) -> Unit = {},
    isRegistering: Boolean,
    isGoogleAuthenticating: Boolean,
    passwordVisible: Boolean,
    onPasswordVisibleChange: (Boolean) -> Unit,
    confirmPasswordVisible: Boolean,
    onConfirmPasswordVisibleChange: (Boolean) -> Unit,
    departmentsAndMunicipalities: List<Pair<String, List<String>>>,
    showDeptDropdown: Boolean,
    onShowDeptDropdownChange: (Boolean) -> Unit,
    showMuniDropdown: Boolean,
    onShowMuniDropdownChange: (Boolean) -> Unit,
    isSimulatingGpsReg: Boolean,
    firestoreCatList: List<com.galibu.core.data.model.FirebaseCategory>,
    specialtiesList: List<String>,
    launchCamera: () -> Unit,
    signupPhotoPickerLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    onAddWorkPhotoClick: () -> Unit,
    fetchLocationByGps: () -> Unit,
    gpsDetectedDept: String?,
    gpsDetectedMuni: String?,
    viewModel: SharedViewModel,
    onAuthSuccess: () -> Unit,
    context: Context,
    scope: kotlinx.coroutines.CoroutineScope,
    onForgotPasswordClick: () -> Unit,
    onGoogleSignIn: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        if (registrationStep == 0) {
            LoginContent(
                logoResId = logoResId,
                googleIconResId = googleIconResId,
                selectedRole = selectedRole,
                onSelectedRoleChange = onSelectedRoleChange,
                email = email,
                onEmailChange = onEmailChange,
                password = password,
                onPasswordChange = onPasswordChange,
                passwordVisible = passwordVisible,
                onPasswordVisibleChange = onPasswordVisibleChange,
                errorMessage = errorMessage,
                onErrorMessageChange = onErrorMessageChange,
                isLoggingIn = isLoggingIn,
                isGoogleAuthenticating = isGoogleAuthenticating,
                onForgotPasswordClick = onForgotPasswordClick,
                onLoginClick = {
                    if (email.isBlank() || password.isBlank()) {
                        onErrorMessageChange("Por favor ingresa tu correo y contraseña")
                    } else {
                        onErrorMessageChange(null)
                        onIsLoggingInChange(true)
                        viewModel.login(email.trim(), password.trim(), selectedRole) { errorMsg ->
                            onIsLoggingInChange(false)
                            if (errorMsg == null) {
                                onAuthSuccess()
                            } else {
                                onErrorMessageChange(errorMsg)
                            }
                        }
                    }
                },
                onGoogleSignIn = onGoogleSignIn,
                onRegisterClick = { onRegistrationStepChange(1) },
                context = context,
                viewModel = viewModel,
                scope = scope,
                onAuthSuccess = onAuthSuccess
            )
        } else {
            RegistrationContent(
                logoResId = logoResId,
                registrationStep = registrationStep,
                onRegistrationStepChange = onRegistrationStepChange,
                selectedRole = selectedRole,
                onSelectedRoleChange = onSelectedRoleChange,
                email = email,
                onEmailChange = onEmailChange,
                password = password,
                onPasswordChange = onPasswordChange,
                confirmPassword = confirmPassword,
                onConfirmPasswordChange = onConfirmPasswordChange,
                name = name,
                onNameChange = onNameChange,
                phone = phone,
                onPhoneChange = onPhoneChange,
                hourlyRateInput = hourlyRateInput,
                onHourlyRateInputChange = onHourlyRateInputChange,
                selectedSpecialties = selectedSpecialties,
                workPhotos = workPhotos,
                selectedDept = selectedDept,
                onSelectedDeptChange = onSelectedDeptChange,
                selectedMuni = selectedMuni,
                onSelectedMuniChange = onSelectedMuniChange,
                addressManualOrGps = addressManualOrGps,
                onAddressManualOrGpsChange = onAddressManualOrGpsChange,
                signupPhotoUri = signupPhotoUri,
                onSignupPhotoUriChange = onSignupPhotoUriChange,
                isGoogleAutocomplete = isGoogleAutocomplete,
                googleIdToken = googleIdToken,
                errorMessage = errorMessage,
                onErrorMessageChange = onErrorMessageChange,
                acceptedTerms = acceptedTerms,
                onAcceptedTermsChange = onAcceptedTermsChange,
                showTermsContract = showTermsContract,
                onShowTermsContractChange = onShowTermsContractChange,
                showProfilePhotoSourceDialog = showProfilePhotoSourceDialog,
                onShowProfilePhotoSourceDialogChange = onShowProfilePhotoSourceDialogChange,
                isCheckingEmail = isCheckingEmail,
                isLoggingIn = isLoggingIn,
                isRegistering = isRegistering,
                isGoogleAuthenticating = isGoogleAuthenticating,
                passwordVisible = passwordVisible,
                onPasswordVisibleChange = onPasswordVisibleChange,
                confirmPasswordVisible = confirmPasswordVisible,
                onConfirmPasswordVisibleChange = onConfirmPasswordVisibleChange,
                departmentsAndMunicipalities = departmentsAndMunicipalities,
                showDeptDropdown = showDeptDropdown,
                onShowDeptDropdownChange = onShowDeptDropdownChange,
                showMuniDropdown = showMuniDropdown,
                onShowMuniDropdownChange = onShowMuniDropdownChange,
                isSimulatingGpsReg = isSimulatingGpsReg,
                firestoreCatList = firestoreCatList,
                specialtiesList = specialtiesList,
                launchCamera = launchCamera,
                signupPhotoPickerLauncher = signupPhotoPickerLauncher,
                onAddWorkPhotoClick = onAddWorkPhotoClick,
                fetchLocationByGps = fetchLocationByGps,
                gpsDetectedDept = gpsDetectedDept,
                gpsDetectedMuni = gpsDetectedMuni,
                viewModel = viewModel,
                onAuthSuccess = onAuthSuccess,
                context = context,
                scope = scope,
                onGoogleSignIn = onGoogleSignIn
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoginContent(
    logoResId: Int,
    googleIconResId: Int,
    selectedRole: String,
    onSelectedRoleChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onPasswordVisibleChange: (Boolean) -> Unit,
    errorMessage: String?,
    onErrorMessageChange: (String?) -> Unit,
    isLoggingIn: Boolean,
    isGoogleAuthenticating: Boolean,
    onForgotPasswordClick: () -> Unit,
    onLoginClick: () -> Unit,
    onGoogleSignIn: () -> Unit,
    onRegisterClick: () -> Unit,
    context: Context,
    viewModel: SharedViewModel,
    scope: kotlinx.coroutines.CoroutineScope,
    onAuthSuccess: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        
        // Brand logo
        Image(
            painter = painterResource(logoResId),
            contentDescription = "Galibu Logo",
            modifier = Modifier.size(width = 61.dp, height = 72.dp),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Servicios que conectan, soluciones que llegan.",
            color = Color(0xFF64748B),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Diferenciador de app: clientes
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(brandBlue.copy(alpha = 0.1f))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                text = "Clientes y profesionales",
                color = brandBlue,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Google sign-in button
        OutlinedButton(
            onClick = onGoogleSignIn,
            enabled = !isGoogleAuthenticating,
            border = BorderStroke(1.dp, borderLight),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("login_google_btn")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    painter = painterResource(googleIconResId),
                    contentDescription = "Google Icon",
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Continuar con Google",
                    color = textMain,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Divider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = borderLight)
            Text(
                text = "o con tu correo",
                color = textSec,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = borderLight)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Email field label & field
        Text(
            text = "Correo electrónico",
            color = textMain,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            placeholder = { Text("correo@ejemplo.com", color = textSec) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = bgLightField,
                unfocusedContainerColor = bgLightField,
                focusedBorderColor = brandBlue,
                unfocusedBorderColor = borderLight,
                focusedTextColor = textMain,
                unfocusedTextColor = textMain
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("login_email_input")
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password field
        PasswordField(
            value = password,
            onValueChange = onPasswordChange,
            label = "Contraseña",
            visible = passwordVisible,
            onToggleVisibility = { onPasswordVisibleChange(!passwordVisible) },
            modifier = Modifier.fillMaxWidth().testTag("login_password_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Forgot password button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = "¿Olvidaste tu contraseña?",
                color = brandBlue,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier
                    .clickable { onForgotPasswordClick() }
                    .testTag("forgot_password_btn")
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Login Submit button
        Button(
            onClick = onLoginClick,
            enabled = !isLoggingIn,
            colors = ButtonDefaults.buttonColors(containerColor = brandBlue),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("login_submit_btn")
        ) {
            Text(
                text = "Iniciar sesión",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Register action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "¿No tienes cuenta? ",
                color = textSec,
                fontSize = 15.sp
            )
            Text(
                text = "Regístrate gratis",
                color = brandBlue,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier
                    .clickable { onRegisterClick() }
                    .testTag("login_register_btn")
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun RegistrationContent(
    logoResId: Int,
    registrationStep: Int,
    onRegistrationStepChange: (Int) -> Unit,
    selectedRole: String,
    onSelectedRoleChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    name: String,
    onNameChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    hourlyRateInput: String,
    onHourlyRateInputChange: (String) -> Unit,
    selectedSpecialties: MutableList<String>,
    workPhotos: MutableList<String>,
    selectedDept: String,
    onSelectedDeptChange: (String) -> Unit,
    selectedMuni: String,
    onSelectedMuniChange: (String) -> Unit,
    addressManualOrGps: String,
    onAddressManualOrGpsChange: (String) -> Unit,
    signupPhotoUri: String?,
    onSignupPhotoUriChange: (String?) -> Unit,
    isGoogleAutocomplete: Boolean,
    googleIdToken: String?,
    errorMessage: String?,
    onErrorMessageChange: (String?) -> Unit,
    acceptedTerms: Boolean,
    onAcceptedTermsChange: (Boolean) -> Unit,
    showTermsContract: Boolean,
    onShowTermsContractChange: (Boolean) -> Unit,
    showProfilePhotoSourceDialog: Boolean,
    onShowProfilePhotoSourceDialogChange: (Boolean) -> Unit,
    isCheckingEmail: Boolean,
    isLoggingIn: Boolean,
    isRegistering: Boolean,
    isGoogleAuthenticating: Boolean,
    passwordVisible: Boolean,
    onPasswordVisibleChange: (Boolean) -> Unit,
    confirmPasswordVisible: Boolean,
    onConfirmPasswordVisibleChange: (Boolean) -> Unit,
    departmentsAndMunicipalities: List<Pair<String, List<String>>>,
    showDeptDropdown: Boolean,
    onShowDeptDropdownChange: (Boolean) -> Unit,
    showMuniDropdown: Boolean,
    onShowMuniDropdownChange: (Boolean) -> Unit,
    isSimulatingGpsReg: Boolean,
    firestoreCatList: List<com.galibu.core.data.model.FirebaseCategory>,
    specialtiesList: List<String>,
    launchCamera: () -> Unit,
    signupPhotoPickerLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    onAddWorkPhotoClick: () -> Unit,
    fetchLocationByGps: () -> Unit,
    gpsDetectedDept: String?,
    gpsDetectedMuni: String?,
    viewModel: SharedViewModel,
    onAuthSuccess: () -> Unit,
    context: Context,
    scope: kotlinx.coroutines.CoroutineScope,
    onGoogleSignIn: () -> Unit
) {
    val customSpecialties = remember { mutableStateListOf<String>() }
    var showCustomSpecialtySheet by remember { mutableStateOf(false) }
    var customSpecialtyInput by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Step Indicator Progress Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        if (registrationStep <= 1) {
                            onRegistrationStepChange(0)
                        } else {
                            onRegistrationStepChange(registrationStep - 1)
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Atrás",
                        tint = PolishDark,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                val stepSubtitle = when (registrationStep) {
                    1 -> "Foto & Datos · Paso 1 de 3"
                    2 -> "Ubicación · Paso 2 de 3"
                    3 -> "Contraseña · Paso 3 de 3"
                    else -> "Paso $registrationStep de 3"
                }
                Column {
                    Text(
                        text = "Crear cuenta",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = textMain
                    )
                    Text(
                        text = stepSubtitle,
                        fontSize = 13.sp,
                        color = textSec
                    )
                }
            }

            Image(
                painter = painterResource(logoResId),
                contentDescription = "Logo Small",
                modifier = Modifier.size(width = 27.dp, height = 32.dp),
                contentScale = ContentScale.Fit
            )
        }

        // Progress bar segments
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (i in 1..3) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (i <= registrationStep) brandBlue else Color(0xFFE2E8F0))
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (registrationStep) {

            1 -> {
                // ================== STEP 1: PROFILE DETAILS ==================
                Text(
                    text = "Datos de tu perfil",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = textMain
                )
                Text(
                    text = "Sube una foto y completa tus datos básicos",
                    fontSize = 15.sp,
                    color = textSec,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                )

                // Intención explícita de uso. CLIENT es el default seguro;
                // BOTH nunca se activa solo y se puede cambiar antes de enviar.
                Text(
                    text = "¿Cómo vas a usar Galibu?",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = textMain,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RoleIntentCard(
                        icon = Icons.Default.Search,
                        title = "Buscar un profesional",
                        subtitle = "Quiero contratar servicios",
                        selected = selectedRole == "CLIENT",
                        onClick = { onSelectedRoleChange("CLIENT") }
                    )
                    RoleIntentCard(
                        icon = Icons.Default.Build,
                        title = "Ofrecer mis servicios",
                        subtitle = "Quiero recibir solicitudes de trabajo",
                        selected = selectedRole == "PROFESSIONAL",
                        onClick = { onSelectedRoleChange("PROFESSIONAL") }
                    )
                    RoleIntentCard(
                        icon = Icons.Default.SwapHoriz,
                        title = "Ambas cosas",
                        subtitle = "Contratar y ofrecer servicios",
                        selected = selectedRole == "BOTH",
                        onClick = { onSelectedRoleChange("BOTH") }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Avatar Photo Selection Circle
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(PolishIce)
                        .border(3.dp, PolishBlue, CircleShape)
                        .clickable { onShowProfilePhotoSourceDialogChange(true) },
                    contentAlignment = Alignment.Center
                ) {
                    if (!signupPhotoUri.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageUtils.getCoilModel(signupPhotoUri),
                            contentDescription = "Foto de perfil",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Elegir foto",
                            tint = textSec,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    // Overlay camera indicator icon
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(brandBlue)
                            .padding(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar",
                            tint = Color.White,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Name Input
                Text(
                    text = "Nombre completo",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = textMain,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    placeholder = { Text("Ej: María García", color = textSec) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = bgLightField,
                        unfocusedContainerColor = bgLightField,
                        focusedBorderColor = brandBlue,
                        unfocusedBorderColor = borderLight,
                        focusedTextColor = textMain,
                        unfocusedTextColor = textMain
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("register_name_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Email Input (Moved to Step 2 to match the images!)
                Text(
                    text = "Correo electrónico",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = textMain,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    placeholder = { Text("correo@ejemplo.com", color = textSec) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    readOnly = isGoogleAutocomplete,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = bgLightField,
                        unfocusedContainerColor = bgLightField,
                        focusedBorderColor = brandBlue,
                        unfocusedBorderColor = borderLight,
                        focusedTextColor = textMain,
                        unfocusedTextColor = textMain
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("register_email_input")
                )

                if (selectedRole != "CLIENT") {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Selector de categorías homologado con la app profesional.
                    val baseCategories = if (firestoreCatList.isNotEmpty()) {
                        firestoreCatList.map { it.name }
                    } else {
                        specialtiesList
                    }
                    val otherCategory = baseCategories.firstOrNull { it.contains("Otro", ignoreCase = true) } ?: "Otro +"
                    val baseWithoutOther = baseCategories.filter { !it.contains("Otro", ignoreCase = true) }
                    val displayCategories = baseWithoutOther + customSpecialties + otherCategory

                    CategoryMultiSelectField(
                        selected = selectedSpecialties,
                        options = displayCategories,
                        label = "Categoría",
                        placeholder = "Seleccionar categorías",
                        accentColor = brandBlue,
                        borderColor = borderLight,
                        containerColor = bgLightField,
                        textColor = textMain,
                        mutedTextColor = textSec,
                        customOptionLabel = otherCategory,
                        onAddCustom = { showCustomSpecialtySheet = true },
                        testTag = "register_category_picker"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Hourly rate estimation label
                    Text(
                        text = "Tarifa aproximada por hora (opcional)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = textMain,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(bottom = 8.dp)
                    )

                    // Hourly rate estimation input
                    OutlinedTextField(
                        value = hourlyRateInput,
                        onValueChange = onHourlyRateInputChange,
                        placeholder = { Text("Ej: 35000", color = textSec) },
                        prefix = { Text("$ ", color = textMain) },
                        suffix = { Text("/hora", color = textSec) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = bgLightField,
                            unfocusedContainerColor = bgLightField,
                            focusedBorderColor = brandBlue,
                            unfocusedBorderColor = borderLight,
                            focusedTextColor = textMain,
                            unfocusedTextColor = textMain
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("register_hourly_rate_input")
                    )

                    // Presets Row for hourly rates (Image 7)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val presets = listOf("20000", "30000", "40000", "50000", "60000")
                        presets.forEach { amt ->
                            val displayAmt = when (amt) {
                                "20000" -> "$20,000"
                                "30000" -> "$30,000"
                                "40000" -> "$40,000"
                                "50000" -> "$50,000"
                                "60000" -> "$60,000"
                                else -> "$$amt"
                            }
                            val isSelected = com.galibu.core.ui.utils.PriceUtils.cleanPrice(hourlyRateInput) == amt
                            FilterChip(
                                selected = isSelected,
                                onClick = { onHourlyRateInputChange(amt) },
                                label = { Text(displayAmt, fontSize = 13.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = lightBlueActive,
                                    selectedLabelColor = brandBlue
                                )
                            )
                        }
                    }

                    Text(
                        text = "Esta tarifa es referencial. Puedes negociar en cada servicio. Es opcional: si la dejas vacía, no bloquea tu registro.",
                        fontSize = 11.sp,
                        color = textSec,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(bottom = 16.dp)
                    )

                    // Work photos option (Image 7)
                    Text(
                        text = "Fotos de tus trabajos (opcional)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = textMain,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Add photo box
                        Card(
                            onClick = { onAddWorkPhotoClick() },
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(90.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Agregar Foto",
                                    tint = textSec,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Foto", color = textSec, fontSize = 12.sp)
                            }
                        }

                        // Display already selected work photos
                        workPhotos.forEach { photoBase64 ->
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            ) {
                                AsyncImage(
                                    model = ImageUtils.getCoilModel(photoBase64),
                                    contentDescription = "Trabajo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                IconButton(
                                    onClick = { workPhotos.remove(photoBase64) },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(24.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Quitar",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Continue Button
                Button(
                    onClick = {
                        if (signupPhotoUri.isNullOrBlank()) {
                            onErrorMessageChange("La foto de perfil es obligatoria. Tómate o elige una foto para continuar.")
                        } else if (name.isBlank() || email.isBlank()) {
                            onErrorMessageChange("Por favor completa tu nombre y correo electrónico")
                        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
                            onErrorMessageChange("Por favor ingresa un correo electrónico válido")
                        } else if (selectedRole != "CLIENT" && selectedSpecialties.isEmpty()) {
                            onErrorMessageChange("Por favor selecciona al menos una especialidad")
                        } else {
                            onErrorMessageChange(null)
                            onRegistrationStepChange(2)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = brandBlue),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("register_step_2_next_btn")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("Continuar", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            2 -> {
                // ================== STEP 2: LOCATION DETAILS ==================
                Text(
                    text = "Ubícanos en el mapa",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = textMain
                )
                Text(
                    text = "Detecta o escribe tu dirección, y selecciona departamento y municipio",
                    fontSize = 15.sp,
                    color = textSec,
                    modifier = Modifier.padding(top = 4.dp, bottom = 28.dp)
                )

                // Address label
                Text(
                    text = "Dirección de domicilio",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = textMain,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 8.dp)
                )

                // Address Input text field
                OutlinedTextField(
                    value = addressManualOrGps,
                    onValueChange = onAddressManualOrGpsChange,
                    placeholder = { Text("Ej: Calle 45 # 10-20, Piso 3", color = textSec) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = brandBlue
                        )
                    },
                    trailingIcon = {
                        if (isSimulatingGpsReg) {
                            CircularProgressIndicator(
                                color = brandBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            IconButton(
                                onClick = fetchLocationByGps,
                                modifier = Modifier.testTag("use_gps_location_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = "Autodetectar ubicación con GPS",
                                    tint = brandBlue
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = bgLightField,
                        unfocusedContainerColor = bgLightField,
                        focusedBorderColor = brandBlue,
                        unfocusedBorderColor = borderLight,
                        focusedTextColor = textMain,
                        unfocusedTextColor = textMain
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("register_address_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Department label
                Text(
                    text = "Departamento",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = textMain,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 8.dp)
                )

                // Department Selector Box
                ExposedDropdownMenuBox(
                    expanded = showDeptDropdown,
                    onExpandedChange = onShowDeptDropdownChange,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedDept,
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = bgLightField,
                            unfocusedContainerColor = bgLightField,
                            focusedBorderColor = brandBlue,
                            unfocusedBorderColor = borderLight,
                            focusedTextColor = textMain,
                            unfocusedTextColor = textMain
                        ),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showDeptDropdown) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                            .testTag("register_dept_picker")
                    )
                    ExposedDropdownMenu(
                        expanded = showDeptDropdown,
                        onDismissRequest = { onShowDeptDropdownChange(false) }
                    ) {
                        val orderedRegDepts = remember(departmentsAndMunicipalities, gpsDetectedDept) {
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
                        orderedRegDepts.forEach { pair ->
                            DropdownMenuItem(
                                text = { Text(pair.first) },
                                onClick = {
                                    onSelectedDeptChange(pair.first)
                                    // Reset municipality on dept change
                                    if (pair.second.isNotEmpty()) {
                                        onSelectedMuniChange(pair.second.first())
                                    } else {
                                        onSelectedMuniChange("Municipio")
                                    }
                                    onShowDeptDropdownChange(false)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Municipality label
                Text(
                    text = "Municipio",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = textMain,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 8.dp)
                )

                // Municipality Selector Box
                ExposedDropdownMenuBox(
                    expanded = showMuniDropdown,
                    onExpandedChange = onShowMuniDropdownChange,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedMuni,
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = bgLightField,
                            unfocusedContainerColor = bgLightField,
                            focusedBorderColor = brandBlue,
                            unfocusedBorderColor = borderLight,
                            focusedTextColor = textMain,
                            unfocusedTextColor = textMain
                        ),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showMuniDropdown) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                            .testTag("register_muni_picker")
                    )
                    ExposedDropdownMenu(
                        expanded = showMuniDropdown,
                        onDismissRequest = { onShowMuniDropdownChange(false) }
                    ) {
                        val currentDeptPair = departmentsAndMunicipalities.find { it.first == selectedDept }
                        val baseMunis = currentDeptPair?.second ?: emptyList()
                        // Si el departamento elegido es el detectado por GPS, su municipio va primero.
                        val gpsMatchedDeptReg = matchCatalogName(
                            departmentsAndMunicipalities.map { it.first },
                            gpsDetectedDept
                        )
                        val currentMunis = if (selectedDept.isNotEmpty() && selectedDept == gpsMatchedDeptReg) {
                            val matchedMuni = matchCatalogName(baseMunis, gpsDetectedMuni)
                            if (matchedMuni != null) {
                                listOf(matchedMuni) + baseMunis.filter { it != matchedMuni }
                            } else {
                                baseMunis
                            }
                        } else {
                            baseMunis
                        }
                        currentMunis.forEach { muni ->
                            DropdownMenuItem(
                                text = { Text(muni) },
                                onClick = {
                                    onSelectedMuniChange(muni)
                                    onShowMuniDropdownChange(false)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Continue Button
                Button(
                    onClick = {
                        if (selectedDept == "Departamento" || selectedMuni == "Municipio" || addressManualOrGps.isBlank()) {
                            onErrorMessageChange("Por favor selecciona un Departamento, un Municipio y escribe una dirección de residencia.")
                        } else {
                            onErrorMessageChange(null)
                            onRegistrationStepChange(3)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = brandBlue),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("register_step_3_next_btn")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("Continuar", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            3 -> {
                // ================== STEP 3: CREDENTIALS ==================
                Text(
                    text = if (isGoogleAutocomplete) "Completa tu registro" else "Crea tus credenciales",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = textMain
                )
                Text(
                    text = if (isGoogleAutocomplete) "Acepta los términos y condiciones de servicio para finalizar" else "Ingresa tu contraseña de acceso",
                    fontSize = 15.sp,
                    color = textSec,
                    modifier = Modifier.padding(top = 4.dp, bottom = 28.dp)
                )

                // Email display (Read-Only)
                Text(
                    text = "Tu correo electrónico",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = textMain,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = textSec
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = bgLightField,
                        unfocusedContainerColor = bgLightField,
                        focusedBorderColor = borderLight,
                        unfocusedBorderColor = borderLight,
                        focusedTextColor = textSec,
                        unfocusedTextColor = textSec
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("register_email_readonly")
                )

                if (!isGoogleAutocomplete) {
                    Spacer(modifier = Modifier.height(16.dp))

                // Password Input
                PasswordField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = "Contraseña (mínimo 8 caracteres)",
                    visible = passwordVisible,
                    onToggleVisibility = { onPasswordVisibleChange(!passwordVisible) },
                    modifier = Modifier.fillMaxWidth().testTag("register_password_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Confirm Password Input
                PasswordField(
                    value = confirmPassword,
                    onValueChange = onConfirmPasswordChange,
                    label = "Confirmar contraseña",
                    visible = confirmPasswordVisible,
                    onToggleVisibility = { onConfirmPasswordVisibleChange(!confirmPasswordVisible) },
                    modifier = Modifier.fillMaxWidth().testTag("register_confirm_password_input")
                )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Terms & conditions Checkbox
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = acceptedTerms,
                        onCheckedChange = onAcceptedTermsChange,
                        colors = CheckboxDefaults.colors(checkedColor = brandBlue),
                        modifier = Modifier.testTag("register_terms_checkbox")
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    val annotatedTermsText = buildAnnotatedString {
                        append("Acepto los ")
                        withStyle(
                            style = SpanStyle(
                                color = brandBlue,
                                fontWeight = FontWeight.Bold,
                                textDecoration = TextDecoration.Underline
                            )
                        ) {
                            append("Términos y Condiciones")
                        }
                        append(" de servicio")
                    }
                    Text(
                        text = annotatedTermsText,
                        color = textSec,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onShowTermsContractChange(true) }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Register Submit Button
                Button(
                    onClick = {
                        val isPasswordInvalid = !isGoogleAutocomplete && (password.isBlank() || confirmPassword.isBlank())
                        val doPasswordsMismatch = !isGoogleAutocomplete && (password != confirmPassword)
                        val isPasswordTooShort = !isGoogleAutocomplete && password.length < 8

                        if (email.isBlank() || isPasswordInvalid) {
                            onErrorMessageChange("Por favor completa todos los campos")
                        } else if (isPasswordTooShort) {
                            onErrorMessageChange("La contraseña debe tener mínimo 8 caracteres.")
                        } else if (doPasswordsMismatch) {
                            onErrorMessageChange("Las contraseñas no coinciden")
                        } else if (!acceptedTerms) {
                            onErrorMessageChange("Debes aceptar los Términos y Condiciones de Galibu para continuar.")
                        } else {
                            val rateVal = com.galibu.core.ui.utils.PriceUtils.cleanPrice(hourlyRateInput).toDoubleOrNull()
                            // Tarifa opcional: si el usuario escribe una, debe ser al menos el
                            // salario mínimo por hora; si la deja vacía, no bloquea el registro.
                            val isRateValid = rateVal == null || rateVal >= 5417.0
                            if (!isRateValid) {
                                onErrorMessageChange("Si vas a poner una tarifa por hora, debe ser de al menos $5.417 COP. Puedes dejarla vacía y definirla después en tu perfil.")
                            } else {
                                onErrorMessageChange(null)
                                // null cuando la deja vacía para que pueda definirla en su perfil.
                                val hourlyRateValue = rateVal
                                val finalCat = if (selectedRole != "CLIENT") {
                                    if (selectedSpecialties.isNotEmpty()) selectedSpecialties.joinToString(", ") else "Plomería"
                                } else {
                                    null
                                }

                                viewModel.register(
                                    name = name.trim(),
                                    email = email.trim().lowercase(),
                                    phone = if (phone.isNotBlank()) phone.trim() else "+57 300 000 0000",
                                    password = if (isGoogleAutocomplete) "" else password,
                                    role = selectedRole,
                                    category = finalCat,
                                    department = selectedDept,
                                    municipality = selectedMuni,
                                    profilePhotoUrl = signupPhotoUri,
                                    address = addressManualOrGps.trim(),
                                    workPhotos = workPhotos,
                                    hourlyRate = if (selectedRole != "CLIENT") hourlyRateValue else null,
                                    acceptedTerms = acceptedTerms,
                                    googleIdToken = googleIdToken
                                ) { errorMsg ->
                                    if (errorMsg == null) {
                                        // Registro exitoso: la sesión ya quedó establecida
                                        onAuthSuccess()
                                    } else {
                                        onErrorMessageChange(errorMsg)
                                    }
                                }
                        }
                    }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = brandBlue),
                    shape = RoundedCornerShape(14.dp),
                    enabled = !isRegistering,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("register_submit_btn")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("Registrarme", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    if (showCustomSpecialtySheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showCustomSpecialtySheet = false
                customSpecialtyInput = ""
            },
            sheetState = sheetState,
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                Text(
                    text = "Nueva Especialidad",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textMain
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Escribe una especialidad diferente a las registradas en la plataforma.",
                    fontSize = 14.sp,
                    color = textSec
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = customSpecialtyInput,
                    onValueChange = { customSpecialtyInput = it.capitalizeFirstLetter() },
                    placeholder = { Text("Ej: Carpintero Naval", color = textSec) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = bgLightField,
                        unfocusedContainerColor = bgLightField,
                        focusedBorderColor = brandBlue,
                        unfocusedBorderColor = borderLight,
                        focusedTextColor = textMain,
                        unfocusedTextColor = textMain
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            showCustomSpecialtySheet = false
                            customSpecialtyInput = ""
                        },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, borderLight),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancelar", color = textMain)
                    }
                    Button(
                        onClick = {
                            val trimmed = customSpecialtyInput.trim()
                            if (trimmed.isNotEmpty()) {
                                val existsInBase = specialtiesList.any { it.equals(trimmed, ignoreCase = true) } ||
                                        firestoreCatList.any { it.name.equals(trimmed, ignoreCase = true) }
                                if (!existsInBase && !customSpecialties.contains(trimmed)) {
                                    customSpecialties.add(trimmed)
                                }
                                if (!selectedSpecialties.contains(trimmed)) {
                                    selectedSpecialties.add(trimmed)
                                }
                            }
                            showCustomSpecialtySheet = false
                            customSpecialtyInput = ""
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = brandBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Añadir", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun ProfilePhotoSourceDialog(
    onDismiss: () -> Unit,
    onGallerySelected: () -> Unit,
    onCameraSelected: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Elegir foto de perfil", fontWeight = FontWeight.Bold) },
        text = { Text("Selecciona la fuente desde la cual deseas cargar o tomar tu foto de perfil de usuario.") },
        confirmButton = {
            Button(
                onClick = onGallerySelected,
                colors = ButtonDefaults.buttonColors(containerColor = brandBlue)
            ) {
                Text("Galería", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onCameraSelected) {
                Text("Cámara", color = brandBlue)
            }
        }
    )
}

@Composable
fun TermsAndConditionsDialog(
    onDismiss: () -> Unit,
    onAccept: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Términos y Condiciones", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .height(300.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Bienvenido a Galibu, la plataforma líder de intermediación de servicios y oficios profesionales.\n\n" +
                           "1. Aceptación de los Términos:\nAl registrarte y hacer uso de nuestra aplicación, aceptas de manera incondicional estar vinculado por las cláusulas descritas aquí de conformidad con las leyes vigentes.\n\n" +
                           "2. Alcance del Servicio:\nGalibu actúa exclusivamente como un canal de intermediación tecnológica neutral. No nos responsabilizamos por los acuerdos privados, contratos, tarifas o la calidad del servicio técnico y operativo prestado por los Proveedores de servicios independientes de la plataforma.\n\n" +
                           "3. Política de Verificación:\nRealizamos esfuerzos diligentes para validar los datos de identidad provistos por los profesionales registrados, pero no garantizamos la absoluta idoneidad de cada usuario. Se insta a los clientes a evaluar los perfiles y ratings con discreción.\n\n" +
                           "4. Solución de Controversias:\nCualquier reclamo o disputa civil y comercial deberá ser resuelto de mutuo acuerdo entre las partes involucradas, eximiendo de todo reclamo civil extracontractual a Galibu SAS.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = textSec
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(containerColor = brandBlue)
            ) {
                Text("Aceptar", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = textSec)
            }
        }
    )
}

@Composable
fun ErrorDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit
) {
    GalibuAlertDialog(
        title = title,
        message = message,
        type = GalibuAlertType.ERROR,
        confirmText = "Entendido",
        onConfirm = onDismiss,
        onDismissRequest = onDismiss
    )
}

@Composable
fun GoogleSuccessDialog(
    message: String,
    onDismiss: () -> Unit
) {
    GalibuAlertDialog(
        title = "Vinculación de cuenta exitosa",
        message = message,
        type = GalibuAlertType.SUCCESS,
        confirmText = "Completar Registro",
        onConfirm = onDismiss,
        onDismissRequest = onDismiss
    )
}

/**
 * Tarjeta de intención de registro. El usuario elige con qué capacidad quiere
 * empezar: solo cliente, solo profesional o ambas. BOTH nunca se activa por
 * defecto; se elige explícitamente aquí o se activa después desde Perfil.
 */
@Composable
private fun RoleIntentCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (selected) Color(0xFFEFF6FF) else Color.White
    val borderColor = if (selected) Color(0xFF2563EB) else Color(0xFFEFF2F5)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = bg),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(if (selected) Color(0xFF2563EB) else Color(0xFFF1F5F9))
                    .clip(RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) Color.White else Color(0xFF64748B),
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) Color(0xFF1E293B) else Color(0xFF334155)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}
