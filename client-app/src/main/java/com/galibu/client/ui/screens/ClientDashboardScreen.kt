package com.galibu.client.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.core.content.ContextCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HistoryToggleOff
import androidx.compose.material.icons.filled.Rocket
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.ChatBubble
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.galibu.client.R
import com.galibu.core.data.model.AppNotification
import com.galibu.core.ui.utils.capitalizeFirstLetter
import com.galibu.core.data.model.Bid
import com.galibu.core.data.remote.RatingDto
import com.galibu.core.data.model.ChatMessage
import com.galibu.core.data.model.JobRequest
import com.galibu.core.data.model.UserProfile
import com.galibu.core.data.model.hasMissingRequiredFields
import com.galibu.core.data.model.isDirectChat
import com.galibu.core.ui.components.CategoryBadge
import com.galibu.core.ui.components.CategoryIconCircle
import com.galibu.core.ui.components.CategorySingleSelectField
import com.galibu.core.ui.components.GalibuAlertDialog
import com.galibu.core.ui.components.GalibuAlertType
import com.galibu.core.ui.components.GalibuRatingSheet
import com.galibu.core.ui.components.RequestSummaryCard
import com.galibu.core.ui.components.ServiceCategoryTag
import com.galibu.core.ui.theme.PolishBg
import com.galibu.core.ui.theme.PolishBlue
import com.galibu.core.ui.theme.PolishBorder
import com.galibu.core.ui.theme.PolishCard
import com.galibu.core.ui.theme.PolishDark
import com.galibu.core.ui.theme.PolishGreen
import com.galibu.core.ui.theme.PolishGrayText
import com.galibu.core.ui.theme.PolishIce
import com.galibu.core.ui.theme.PolishNavy
import com.galibu.core.ui.theme.StatusAccepted
import com.galibu.core.ui.theme.StatusCancelled
import com.galibu.core.ui.theme.StatusCompleted
import com.galibu.core.ui.theme.StatusNegotiating
import com.galibu.core.ui.theme.StatusOpen
import com.galibu.client.ui.viewmodels.ClientViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class ProfessionalModel(
    val id: String,
    val name: String,
    val ratePerHour: String,
    val tags: List<String>,
    val description: String,
    val photoUrl: String?,
    val rating: Float,
    val serviceCount: Int,
    val location: String,
    val isOnline: Boolean = false,
    val verified: Boolean = true,
    val workCategory: String? = null,
    val municipality: String? = null,
    val department: String? = null,
    val address: String? = null,
    val hourlyRate: Double? = null,
    val memberSince: String = "",
    val workPhotos: String? = null,
    val workDescription: String? = null
)

fun decodeBase64ToBitmap(base64Str: String): Bitmap? {
    return try {
        val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    } catch (e: Exception) {
        null
    }
}

/**
 * Coil no interpreta directamente las fotos guardadas como data URL base64.
 * Este helper las convierte a Bitmap antes de pasarlas a AsyncImage.
 */
fun decodePhotoModel(photoUrl: String?): Any? {
    if (photoUrl.isNullOrBlank()) return null
    if (!photoUrl.startsWith("data:image/")) return photoUrl
    return try {
        val base64Data = photoUrl.substringAfter("base64,")
            .trim()
            .replace(" ", "+")
            .replace("\n", "")
            .replace("\r", "")
        val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size) ?: photoUrl
    } catch (e: Exception) {
        photoUrl
    }
}

private fun getGreeting(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when {
        hour in 0..11 -> "Buenos días"
        hour in 12..17 -> "Buenas tardes"
        else -> "Buenas noches"
    }
}

fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = (now - timestamp).coerceAtLeast(0)
    return when {
        diff < TimeUnit.MINUTES.toMillis(1) -> "ahora"
        diff < TimeUnit.HOURS.toMillis(1) -> "hace ${diff / TimeUnit.MINUTES.toMillis(1)} min"
        diff < TimeUnit.DAYS.toMillis(1) -> "hace ${diff / TimeUnit.HOURS.toMillis(1)} h"
        diff < TimeUnit.DAYS.toMillis(7) -> "hace ${diff / TimeUnit.DAYS.toMillis(1)} d"
        else -> {
            val sdf = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}

private fun getCategoryColor(category: String): Color {
    return when (category.lowercase()) {
        "plomería", "plomeria" -> Color(0xFF2563EB)    // Blue-600
        "electricidad" -> Color(0xFF059669)              // Emerald-600
        "carpintería", "carpinteria" -> Color(0xFFD97706) // Amber-600
        "pintura" -> Color(0xFFDB2777)                   // Pink-600
        "albañilería", "albanileria" -> Color(0xFF4F46E5) // Indigo-600
        "jardinería", "jardineria" -> Color(0xFF16A34A)  // Green-600
        "cerrajería", "cerrajeria" -> Color(0xFF64748B)  // Slate-500
        "fumigación", "fumigacion" -> Color(0xFF7C3AED)  // Violet-600
        "mecánica", "mecanica" -> Color(0xFFDC2626)      // Red-600
        "limpieza" -> Color(0xFF0891B2)                  // Cyan-600
        "tecnología", "tecnologia" -> Color(0xFF2563EB)  // Blue-600
        else -> PolishBlue
    }
}

private fun getCategoryIcon(category: String): String {
    return when (category.lowercase()) {
        "plomería", "plomeria" -> "🔧"
        "electricidad" -> "⚡"
        "carpintería", "carpinteria" -> "🪚"
        "pintura" -> "🎨"
        "albañilería", "albanileria" -> "🧱"
        "jardinería", "jardineria" -> "🌿"
        "cerrajería", "cerrajeria" -> "🔑"
        "fumigación", "fumigacion" -> "🐛"
        "mecánica", "mecanica" -> "🔩"
        "limpieza" -> "🧹"
        "tecnología", "tecnologia" -> "💻"
        else -> "📋"
    }
}

private fun getCategoryEmoji(category: String): String {
    return when (category.lowercase()) {
        "plomería", "plomeria" -> "\uD83D\uDCA7"
        "electricidad" -> "⚡"
        "carpintería", "carpinteria" -> "\uD83D\uDD28"
        "pintura" -> "\uD83C\uDFA8"
        "albañilería", "albanileria" -> "\uD83E\uDDF1"
        "jardinería", "jardineria" -> "\uD83C\uDF3F"
        "cerrajería", "cerrajeria" -> "\uD83D\uDD11"
        "fumigación", "fumigacion" -> "\uD83D\uDD12"
        "mecánica", "mecanica" -> "\uD83D\uDD29"
        "limpieza" -> "\uD83E\uDDF9"
        "tecnología", "tecnologia" -> "\uD83D\uDCBB"
        else -> "\uD83D\uDCCB"
    }
}

private fun getStatusColor(status: String): Color {
    return when (status.uppercase()) {
        "OPEN", "ABIERTO" -> StatusOpen
        "NEGOTIATING", "NEGOCIANDO" -> StatusNegotiating
        "ACCEPTED", "ACEPTADO", "IN_PROGRESS", "EN_PROGRESO" -> StatusAccepted
        "COMPLETED", "COMPLETADO" -> StatusCompleted
        "CANCELLED", "CANCELADO" -> StatusCancelled
        "PENDING_RATING", "PENDIENTE_CALIFICACION" -> Color(0xFFFF9800)
        "PENDING", "PENDIENTE" -> Color(0xFFFF9800)
        else -> PolishGrayText
    }
}

private fun getStatusText(status: String): String {
    return when (status.uppercase()) {
        "OPEN", "ABIERTO" -> "Abierto"
        "NEGOTIATING", "NEGOCIANDO" -> "Negociando"
        "ACCEPTED", "ACEPTADO", "IN_PROGRESS", "EN_PROGRESO" -> "Trabajo en curso"
        "COMPLETED", "COMPLETADO" -> "Completado"
        "CANCELLED", "CANCELADO" -> "Cancelado"
        "PENDING_RATING", "PENDIENTE_CALIFICACION" -> "Por calificar"
        "PENDING", "PENDIENTE" -> "Pendiente"
        else -> status
    }
}

private fun formatCurrency(amount: Double): String {
    val formatter = java.text.NumberFormat.getCurrencyInstance(Locale("es", "CO"))
    formatter.maximumFractionDigits = 0
    return formatter.format(amount)
}

private fun getRatingDistribution(reviews: List<Pair<Float, String>>): Map<Int, Int> {
    val distribution = mutableMapOf<Int, Int>()
    for (i in 1..5) {
        distribution[i] = reviews.count { it.first.toInt() == i }
    }
    return distribution
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ClientDashboardScreen(
    viewModel: ClientViewModel,
    onNavigateToJobBids: (String) -> Unit,
    onNavigateToChat: (String) -> Unit,
    onNavigateToNotifications: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val currentUser by viewModel.currentUser.collectAsState()
    val professionals by viewModel.professionals.collectAsState()
    var historyRatings by remember { mutableStateOf<List<com.galibu.core.data.remote.RatingDto>>(emptyList()) }
    LaunchedEffect(currentUser?.id) {
        val uid = currentUser?.id ?: return@LaunchedEffect
        try { historyRatings = viewModel.repository.getUserRatings(uid) } catch (e: Exception) { historyRatings = emptyList() }
    }
    val allJobs by viewModel.allJobs.collectAsState()
    val openJobs by viewModel.openJobs.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val allChatMessages by viewModel.allChatMessages.collectAsState()
    val firestoreCatList by viewModel.firestoreCategories.collectAsState()
    val firestoreDeptList by viewModel.firestoreDepartments.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val requireCompletion by viewModel.requireProfileCompletion.collectAsState()
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showProfessionalDetail by remember { mutableStateOf(false) }
    var selectedProfessional by remember { mutableStateOf<ProfessionalModel?>(null) }
    var showJobPostForm by remember { mutableStateOf(false) }
    var customCategories by remember { mutableStateOf<List<String>>(emptyList()) }
    var showRatingDialog by remember { mutableStateOf(false) }
    var ratingJobId by remember { mutableStateOf("") }
    var showCancelJobDialog by remember { mutableStateOf(false) }
    var cancelJobId by remember { mutableStateOf("") }
    var showNotificationsPanel by remember { mutableStateOf(false) }
    var showEditProfile by remember { mutableStateOf(false) }
    var showChangePassword by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    // Aviso verde del perfil tras cambiar la contraseña (igual que en la app profesional).
    var passwordSuccessMessage by remember { mutableStateOf<String?>(null) }

    // Completado forzoso tras Google: ir a Perfil, abrir edición y bloquear
    // el resto hasta guardar todos los campos obligatorios.
    LaunchedEffect(requireCompletion, currentUser) {
        if (requireCompletion) {
            selectedTab = 4
            showEditProfile = true
            val user = currentUser
            if (user != null && !user.hasMissingRequiredFields()) {
                viewModel.setRequireProfileCompletion(false)
            }
        }
    }

    val categories = remember(firestoreCatList) {
        if (firestoreCatList.isNotEmpty()) {
            firestoreCatList.map { it.name }
        } else {
            listOf(
                "Plomería", "Electricidad", "Carpintería", "Pintura",
                "Albañilería", "Jardinería", "Cerrajería", "Fumigación"
            )
        }
    }

    val departmentsAndMunicipalities = remember(firestoreDeptList) {
        if (firestoreDeptList.isNotEmpty()) {
            firestoreDeptList.map { it.name to it.municipalities }
        } else {
            listOf(
                "Antioquia" to listOf("Medellín", "Envigado", "Bello", "Rionegro"),
                "Bogotá D.C." to listOf("Bogotá"),
                "Valle del Cauca" to listOf("Cali", "Palmira", "Yumbo", "Jamundí"),
                "Atlántico" to listOf("Barranquilla", "Soledad", "Puerto Colombia"),
                "Santander" to listOf("Bucaramanga", "Floridablanca", "Girón")
            )
        }
    }


    val professionalModels = remember(professionals) {
        professionals.filter { it.role.equals("PROFESSIONAL", ignoreCase = true) || it.role.equals("BOTH", ignoreCase = true) }.map { profile ->
            ProfessionalModel(
                id = profile.id,
                name = profile.name,
                ratePerHour = profile.hourlyRate?.let { formatCurrency(it) } ?: "A convenir",
                tags = listOfNotNull(
                    profile.workCategory,
                    profile.department
                ),
                description = "Especialista en ${profile.workCategory ?: "servicios generales"}",
                photoUrl = profile.profilePhotoUrl,
                rating = profile.rating?.toFloat() ?: 0f,
                serviceCount = profile.completedJobs ?: 0,
                location = "${profile.municipality ?: ""}, ${profile.department ?: ""}".trim().trimEnd(','),
                isOnline = false,
                verified = profile.isVerified ?: false,
                workCategory = profile.workCategory,
                municipality = profile.municipality,
                department = profile.department,
                address = profile.address,
                hourlyRate = profile.hourlyRate,
                memberSince = "",
                workPhotos = profile.workPhotos,
                workDescription = profile.workDescription
            )
        }
    }

    val filteredProfessionals = remember(professionalModels, selectedCategory) {
        professionalModels.filter { pro ->
            selectedCategory == null || pro.workCategory?.lowercase() == selectedCategory?.lowercase()
        }
    }

    val clientJobs = remember(allJobs, currentUser) {
        allJobs.filter { it.clientId == currentUser?.id && !it.isDirectChat() }
    }

    // Escucha en tiempo real de los chats de mis solicitudes activas (incluye
    // hilos directos y abiertas sin asignar: el pro puede escribir antes de ofertar).
    val myActiveChatIds = remember(allJobs, currentUser) {
        allJobs.filter { job ->
            job.clientId == currentUser?.id &&
                job.status.uppercase() !in listOf("COMPLETED", "COMPLETADO", "CANCELLED", "CANCELADO")
        }.map { it.id }
    }
    DisposableEffect(myActiveChatIds) {
        val regs = if (myActiveChatIds.isNotEmpty()) viewModel.listenToChats(myActiveChatIds) else emptyList()
        onDispose { viewModel.stopListeningToChats(regs) }
    }

    // Contador del menú: solo mensajes sin leer de conversaciones activas.
    // Antes contaba también los mensajes de servicios completados/cancelados y
    // por eso se veía un número rojo aunque no hubiera conversaciones en pantalla.
    val unreadChatCount = remember(allChatMessages, myActiveChatIds, currentUser) {
        allChatMessages.count {
            it.jobId in myActiveChatIds && !it.read && it.senderId != currentUser?.id
        }
    }

    val bidsByJob by viewModel.clientBidsByJob.collectAsState()

    // Sincroniza las ofertas de mis solicitudes abiertas para el contador,
    // con re-chequeo periódico para avisar ofertas nuevas (campanita + push).
    val openBidJobIds = remember(clientJobs) {
        clientJobs
            .filter { it.status.uppercase() !in listOf("COMPLETED", "COMPLETADO", "CANCELLED", "CANCELADO") }
            .map { it.id }
    }
    LaunchedEffect(openBidJobIds) {
        if (openBidJobIds.isNotEmpty()) {
            viewModel.refreshClientBids(openBidJobIds)
        }
        viewModel.refreshMyJobs()
        while (true) {
            kotlinx.coroutines.delay(15_000)
            if (openBidJobIds.isNotEmpty()) {
                viewModel.refreshClientBids(openBidJobIds)
            }
            // Detecta aceptar/iniciar/finalizar/calificar del otro extremo.
            viewModel.refreshMyJobs()
        }
    }

    // Al volver desde la app profesional, el contador de ofertas se actualiza
    // de inmediato en lugar de esperar el siguiente ciclo de sincronización.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshAll()
        if (openBidJobIds.isNotEmpty()) {
            viewModel.refreshClientBids(openBidJobIds)
        }
    }

    // Permiso de notificaciones del sistema (Android 13+) para las push de ofertas.
    val pushPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            pushPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val acceptedJobs = clientJobs.filter {
        it.status.uppercase() in listOf("ACCEPTED", "ACEPTADO", "IN_PROGRESS", "EN_PROGRESO")
    }

    val pendingRatingJobs = clientJobs.filter {
        it.status.uppercase() in listOf("PENDING_RATING", "PENDIENTE_CALIFICACION")
    }

    Scaffold(
        topBar = { },
        bottomBar = {
            NavigationBar(
                selectedTab,
                { if (!requireCompletion) selectedTab = it },
                unreadMessages = unreadChatCount
            )
        },
        containerColor = PolishBg,
        // El Scaffold de MainActivity ya aplica los insets del sistema; si este
        // Scaffold interno los vuelve a aplicar, el encabezado queda ~24 dp más
        // abajo que en la app profesional. Se anulan para homologar la altura.
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> InicioTab(
                    currentUser = currentUser,
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it },
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    clientJobs = clientJobs,
                    bidsByJob = bidsByJob,
                    onJobClick = { job ->
                        viewModel.selectJob(job.id)
                        onNavigateToJobBids(job.id)
                    },
                    onNotificationsClick = { onNavigateToNotifications() },
                    unreadNotifications = notifications.count { !it.isRead },
                    onPostJob = { showJobPostForm = true }
                )
                1 -> ProfesionalesTab(
                    professionals = filteredProfessionals,
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it },
                    categories = categories,
                    onProfessionalClick = { pro ->
                        selectedProfessional = pro
                        showProfessionalDetail = true
                    }
                )
                2 -> MensajesTab(
                    viewModel = viewModel,
                    clientJobs = allJobs.filter { it.clientId == currentUser?.id },
                    currentUserId = currentUser?.id,
                    onChatClick = { job ->
                        viewModel.selectJob(job.id)
                        viewModel.refreshClientBids(listOf(job.id))
                        onNavigateToChat(job.id)
                    }
                )
                3 -> HistorialTab(
                    ratings = historyRatings
                )
                4 -> PerfilTab(
                    currentUser = currentUser,
                    onEditProfile = {
                        passwordSuccessMessage = null
                        showEditProfile = true
                    },
                    onChangePassword = {
                        passwordSuccessMessage = null
                        showChangePassword = true
                    },
                    onDeleteAccount = { showDeleteAccountDialog = true },
                    onLogout = { showLogoutDialog = true },
                    onSupport = { viewModel.showAlert("Aviso", "Reporte enviado con éxito") },
                    successMessage = passwordSuccessMessage,
                    onDismissSuccess = { passwordSuccessMessage = null }
                )
            }
        }
    }

    if (showProfessionalDetail && selectedProfessional != null) {
        ProfessionalDetailScreen(
            professional = selectedProfessional!!,
            viewModel = viewModel,
            onBack = { showProfessionalDetail = false },
            onHire = { pro, offeredPrice ->
                viewModel.createJobRequest(
                    category = pro.workCategory ?: "General",
                    title = "Servicio de ${pro.workCategory ?: "General"}",
                    description = "Solicitud de servicio para ${pro.workCategory ?: "servicios generales"}",
                    budget = offeredPrice,
                    address = currentUser?.address ?: "",
                    department = currentUser?.department ?: "",
                    municipality = currentUser?.municipality ?: "",
                    professionalId = pro.id,
                    professionalName = pro.name,
                    source = "DIRECTORY",
                    onSuccess = {
                        viewModel.showSuccess("Solicitud creada exitosamente")
                        showProfessionalDetail = false
                    },
                    onError = { msg ->
                        viewModel.showAlert("No se pudo contratar", msg, true)
                    }
                )
            },
            onChat = { pro ->
                viewModel.startChatWithProfessional(
                    proId = pro.id,
                    proName = pro.name,
                    proSpecialty = pro.workCategory ?: "",
                    onStarted = { chatId ->
                        viewModel.selectJob(chatId)
                        onNavigateToChat(chatId)
                    }
                )
                showProfessionalDetail = false
            }
        )
    }

    if (showJobPostForm) {
        JobPostForm(
            currentUser = currentUser,
            categories = categories,
            customCategories = customCategories,
            onCustomCategoryAdded = { category ->
                if (customCategories.none { it.equals(category, ignoreCase = true) }) {
                    customCategories = customCategories + category
                }
            },
            departmentsAndMunicipalities = departmentsAndMunicipalities,
            onDismiss = { showJobPostForm = false },
            onShowError = { message -> viewModel.showError(message) },
            onSubmit = { category, title, description, budget, address, department, municipality, suggestedPrice, photos, onError, onSuccess ->
                viewModel.createJobRequest(
                    category = category,
                    title = title,
                    description = description,
                    budget = budget,
                    address = address,
                    department = department,
                    municipality = municipality,
                    suggestedPrice = suggestedPrice,
                    photos = photos,
                    onError = onError,
                    onSuccess = onSuccess
                )
            }
        )
    }

    if (showRatingDialog && ratingJobId.isNotEmpty()) {
        RatingDialog(
            jobId = ratingJobId,
            onDismiss = {
                showRatingDialog = false
                ratingJobId = ""
            },
            onSubmit = { rating, review ->
                viewModel.submitRating(
                    jobId = ratingJobId,
                    rating = rating.toFloat(),
                    review = review,
                    isClientRating = true
                )
                showRatingDialog = false
                ratingJobId = ""
            }
        )
    }

    if (showCancelJobDialog && cancelJobId.isNotEmpty()) {
        CancelJobDialog(
            onDismiss = {
                showCancelJobDialog = false
                cancelJobId = ""
            },
            onConfirm = { reason ->
                viewModel.cancelJob(
                    jobId = cancelJobId,
                    reason = reason,
                    onComplete = {
                        showCancelJobDialog = false
                        cancelJobId = ""
                        viewModel.showSuccess("Trabajo cancelado")
                    }
                )
            }
        )
    }

    if (showEditProfile) {
        EditProfileScreen(
            currentUser = currentUser,
            departmentsAndMunicipalities = departmentsAndMunicipalities,
            onDismiss = { if (!requireCompletion) showEditProfile = false },
            isForced = requireCompletion,
            onError = { message -> viewModel.showError(message) },
            onSave = { name, department, municipality, address, photo ->
                viewModel.updateProfile(
                    name = name,
                    workCategory = currentUser?.workCategory,
                    profilePhotoUrl = photo,
                    department = department,
                    municipality = municipality,
                    address = address
                )
                showEditProfile = false
                viewModel.showSuccess("Perfil actualizado")
            }
        )
    }

    // Cambiar contraseña: pantalla completa (homologada con el perfil profesional).
    if (showChangePassword) {
        ChangePasswordScreen(
            onDismiss = { showChangePassword = false },
            onError = { message -> viewModel.showError(message) },
            onSubmit = { current, newPassword, onResult ->
                viewModel.changePassword(current, newPassword) { errorMsg ->
                    if (errorMsg == null) {
                        showChangePassword = false
                        passwordSuccessMessage = "La contraseña ha sido cambiada exitosamente"
                    }
                    onResult(errorMsg)
                }
            }
        )
    }

    if (showDeleteAccountDialog) {
        GalibuAlertDialog(
            title = "¿Eliminar tu Cuenta de Galibu?",
            message = "Esta acción es irreversible y eliminará todo tu historial de servicios contratados, tus datos de contacto y tu billetera de saldo. ¿Realmente deseas continuar?",
            type = GalibuAlertType.ERROR,
            confirmText = "Eliminar",
            dismissText = "Cancelar",
            confirmColor = Color(0xFFEF4444),
            onConfirm = {
                showDeleteAccountDialog = false
                viewModel.deleteAccount { error ->
                    if (error != null) {
                        viewModel.showAlert("No se pudo eliminar la cuenta", error, true)
                    }
                }
            },
            onDismissRequest = { showDeleteAccountDialog = false }
        )
    }

    // Cerrar sesión: confirmación previa (igual que en la app profesional).
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

}

@Composable
fun NavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    unreadMessages: Int = 0
) {
    // Ícono relleno cuando la pestaña está seleccionada y de contorno cuando no
    // (convención actual de Material 3). Pair = (seleccionado, no seleccionado).
    val tabs = listOf(
        Triple("Inicio", Icons.Default.Home to Icons.Outlined.Home, "home"),
        Triple("Profesionales", Icons.Default.Store to Icons.Outlined.Store, "professionals"),
        Triple("Mensajes", Icons.Default.ChatBubble to Icons.Outlined.ChatBubble, "messages"),
        Triple("Historial", Icons.Default.History to Icons.Outlined.History, "history"),
        Triple("Perfil", Icons.Default.Person to Icons.Outlined.Person, "profile")
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, PolishBorder, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 80.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, (title, iconPair, _) ->
                val isSelected = selectedTab == index
                val color = if (isSelected) PolishBlue else PolishGrayText
                val weight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                val icon = if (isSelected) iconPair.first else iconPair.second

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTabSelected(index) }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Línea superior azul del tab activo (se reserva el espacio siempre).
                    Box(
                        modifier = Modifier
                            .size(28.dp, 3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (isSelected) PolishBlue else Color.Transparent)
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // Icono homologado con profesional + contador legible dentro del tab.
                    Box(
                        modifier = Modifier.size(30.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = color,
                            modifier = Modifier.size(24.dp)
                        )
                        if (title == "Mensajes" && unreadMessages > 0) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE53935))
                                    .border(1.dp, Color.White, CircleShape)
                                    .align(Alignment.TopEnd),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (unreadMessages > 9) "9+" else "$unreadMessages",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = title,
                        color = color,
                        fontSize = 10.sp,
                        fontWeight = weight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun MensajesTab(
    viewModel: ClientViewModel,
    clientJobs: List<JobRequest>,
    currentUserId: String?,
    onChatClick: (JobRequest) -> Unit
) {
    val allMessages by viewModel.allChatMessages.collectAsState()

    // Conversaciones: solicitudes activas con profesional asignado, hilos directos
    // de chat, o solicitudes con mensajes (el pro puede escribir antes de ofertar).
    val jobsWithMessages = remember(allMessages) { allMessages.map { it.jobId }.toSet() }
    val chatJobs = remember(clientJobs, currentUserId, jobsWithMessages) {
        clientJobs.filter { job ->
            job.clientId == currentUserId &&
                (!job.professionalId.isNullOrEmpty() || job.isDirectChat() || job.id in jobsWithMessages) &&
                job.status.uppercase() !in listOf("COMPLETED", "COMPLETADO", "CANCELLED", "CANCELADO")
        }
    }
    // La escucha en vivo la mantiene el dashboard (cubre también abiertas sin asignar).

    val sortedJobs = remember(chatJobs, allMessages) {
        chatJobs.sortedByDescending { job ->
            allMessages.filter { it.jobId == job.id }.maxOfOrNull { it.timestamp } ?: job.createdAt
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Mensajes",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = PolishDark
        )
        if (sortedJobs.isNotEmpty()) {
            Text(
                text = "${sortedJobs.size} ${if (sortedJobs.size == 1) "conversación activa" else "conversaciones activas"}",
                fontSize = 14.sp,
                color = PolishGrayText,
                modifier = Modifier.padding(top = 1.dp)
            )
        }

        if (sortedJobs.isEmpty()) {
            // Mismo estado vacío que la app profesional: sin tarjeta, con ícono azul.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(PolishBlue.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = null,
                        tint = PolishBlue,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Sin conversaciones activas",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = PolishDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Aquí aparecerán tus chats presenciales con profesionales.",
                    fontSize = 13.sp,
                    color = PolishGrayText,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            sortedJobs.forEach { job ->
                val jobMessages = remember(allMessages, job.id) {
                    allMessages.filter { it.jobId == job.id }
                }
                val lastMsg = remember(jobMessages) { jobMessages.maxByOrNull { it.timestamp } }
                val unread = remember(jobMessages, currentUserId) {
                    jobMessages.count { !it.read && it.senderId != currentUserId }
                }
                val peerName = job.professionalName?.takeIf { it.isNotBlank() }
                    ?: jobMessages.filter { it.senderId != currentUserId }
                        .maxByOrNull { it.timestamp }?.senderName?.takeIf { it.isNotBlank() }
                    ?: "Chat de servicio"
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, PolishBorder, RoundedCornerShape(16.dp))
                        .clickable { onChatClick(job) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(PolishBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = peerName.trim().take(2).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = peerName,
                                    color = PolishDark,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                if (unread > 0) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFE53935)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (unread > 9) "9+" else "$unread",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "Activo",
                                        color = PolishGreen,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = lastMsg?.text?.takeIf { it.isNotBlank() }
                                    ?: job.title.takeIf { it.isNotBlank() } ?: job.category,
                                color = if (unread > 0) PolishDark else PolishGrayText,
                                fontSize = 13.sp,
                                fontWeight = if (unread > 0) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistorialTab(
    ratings: List<RatingDto>
) {
    val total = ratings.size
    val avg = if (ratings.isNotEmpty()) ratings.map { it.stars }.average() else 0.0
    val last20 = ratings.take(20)
    val dateFmt = remember { java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale("es", "CO")) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Historial",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = PolishDark
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "$total calificaciones recibidas",
            fontSize = 14.sp,
            color = PolishGrayText
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, PolishBorder),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Promedio", fontSize = 12.sp, color = PolishGrayText, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(String.format(Locale.US, "%.1f", avg), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = PolishDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row {
                        (1..5).forEach { i ->
                            Icon(Icons.Default.Star, contentDescription = null, tint = if (i <= avg.toInt()) Color(0xFFF59E0B) else Color(0xFFE2E8F0), modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, PolishBorder),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Calificaciones", fontSize = 12.sp, color = PolishGrayText, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$total", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = PolishDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("comentarios", fontSize = 12.sp, color = PolishGrayText)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "ÚLTIMAS CALIFICACIONES",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = PolishGrayText,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (last20.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, PolishBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.StarBorder, contentDescription = null, tint = PolishGrayText, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Sin calificaciones", color = PolishDark, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Tus calificaciones aparecerán aquí", color = PolishGrayText, fontSize = 14.sp, textAlign = TextAlign.Center)
                }
            }
        } else {
            last20.forEach { rating ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, PolishBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            (1..5).forEach { i ->
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = if (i <= rating.stars) Color(0xFFF59E0B) else Color(0xFFE2E8F0),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = dateFmt.format(java.util.Date(rating.createdAt)),
                                fontSize = 11.sp,
                                color = PolishGrayText
                            )
                        }
                        if (!rating.comment.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = rating.comment ?: "",
                                fontSize = 13.sp,
                                color = PolishDark
                            )
                        }
                    }
                }
            }
            if (ratings.size > 20) {
                Text(
                    text = "Mostrando las 20 más recientes de $total",
                    fontSize = 12.sp,
                    color = PolishGrayText,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}
@Composable
private fun HistorialStarsRow(stars: Float) {
    Row {
        (1..5).forEach { i ->
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = if (i <= stars.toInt()) Color(0xFFF59E0B) else Color(0xFFE2E8F0),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}


/**
 * Una solicitud terminada sigue en Inicio mientras falte la finalización
 * (calificación) del cliente o del profesional. Solo desaparece cuando ambas
 * partes ya cerraron el servicio.
 */
private fun JobRequest.isVisibleOnClientHome(): Boolean {
    val normalized = status.uppercase()
    if (normalized in listOf("CANCELLED", "CANCELADO")) return false
    if (normalized in listOf("COMPLETED", "COMPLETADO")) {
        return clientRatingOfPro == null || proRatingOfClient == null
    }
    return true
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun InicioTab(
    currentUser: UserProfile?,
    categories: List<String>,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    clientJobs: List<JobRequest>,
    bidsByJob: Map<String, Int> = emptyMap(),
    onJobClick: (JobRequest) -> Unit,
    onNotificationsClick: () -> Unit,
    unreadNotifications: Int = 0,
    onPostJob: () -> Unit = {}
) {
    val greeting = remember { getGreeting() }
    val firstName = currentUser?.name?.split(" ")?.firstOrNull() ?: "Usuario"

    val activeJobs = clientJobs.filter { it.isVisibleOnClientHome() }
    val totalJobs = activeJobs.size
    val inProgressJobs = activeJobs.count {
        it.status.uppercase() in listOf("IN_PROGRESS", "EN_PROGRESO")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "\u00A1Hola, $firstName!",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = PolishGrayText
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(PolishBlue.copy(alpha = 0.1f))
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "Clientes",
                                color = PolishBlue,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Solicitudes",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PolishDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Gestiona tus solicitudes",
                        fontSize = 14.sp,
                        color = PolishGrayText,
                        lineHeight = 20.sp
                    )
                }
                Row(
                    modifier = Modifier.align(Alignment.Top),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF1F5F9))
                                .clickable { onNotificationsClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = "Notificaciones",
                                tint = PolishDark,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        if (unreadNotifications > 0) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE53935))
                                    .align(Alignment.TopEnd),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (unreadNotifications > 9) "9+" else "$unreadNotifications",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onPostJob,
                colors = ButtonDefaults.buttonColors(containerColor = PolishBlue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Nueva solicitud")
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HomeStatCard(
                    count = totalJobs,
                    label = "Solicitudes totales",
                    icon = Icons.Default.Description,
                    iconBg = Color(0xFFEDE9FE),
                    iconTint = Color(0xFF7C3AED),
                    modifier = Modifier.weight(1f)
                )
                HomeStatCard(
                    count = inProgressJobs,
                    label = "En progreso",
                    icon = Icons.Default.CheckCircle,
                    iconBg = Color(0xFFDCFCE7),
                    iconTint = Color(0xFF16A34A),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        val visibleJobs = remember(clientJobs) {
            clientJobs.filter { it.isVisibleOnClientHome() }
        }
        var originFilter by remember { mutableStateOf("ALL") }
        val filteredJobs = remember(visibleJobs, originFilter) {
            when (originFilter) {
                "PUBLISH" -> visibleJobs.filter { (it.source ?: "PUBLISH") != "DIRECTORY" }
                "DIRECTORY" -> visibleJobs.filter { (it.source ?: "PUBLISH") == "DIRECTORY" }
                else -> visibleJobs
            }
        }

        if (visibleJobs.isNotEmpty()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "Mis solicitudes",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = PolishDark
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("ALL" to "Todas", "PUBLISH" to "Publicadas", "DIRECTORY" to "Directas").forEach { (value, label) ->
                    FilterChip(
                        selected = originFilter == value,
                        onClick = { originFilter = value },
                        label = { Text(label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PolishBlue,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredJobs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF8FAFC))
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                    .padding(vertical = 36.dp, horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE2E8F0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        "No tienes solicitudes activas",
                        color = PolishDark,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Cuando publiques una solicitud aparecerá aquí con sus ofertas",
                        color = PolishGrayText,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onPostJob,
                        colors = ButtonDefaults.buttonColors(containerColor = PolishBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Crear solicitud", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        } else {
            filteredJobs.forEach { job ->
                HomeJobRequestCard(
                    job = job,
                    bidsCount = bidsByJob[job.id] ?: 0,
                    onClick = { onJobClick(job) }
                )
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun HomeStatCard(
    count: Int,
    label: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(84.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, PolishBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "$count",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PolishDark
                )
                Text(
                    text = label.replace("\n", " "),
                    fontSize = 11.sp,
                    color = PolishGrayText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun HomeJobRequestCard(
    job: JobRequest,
    bidsCount: Int = 0,
    onClick: () -> Unit
) {
    val (statusLabel, statusBg, statusTint) = when (job.status.uppercase()) {
        "ACCEPTED", "ACEPTADO" -> Triple("Aceptado", Color(0xFFEFF6FF), Color(0xFF2563EB))
        "IN_PROGRESS", "EN_PROGRESO" -> Triple("En progreso", Color(0xFFDCFCE7), Color(0xFF16A34A))
        "PENDING_RATING", "PENDIENTE_CALIFICACION" -> if (job.clientRatingOfPro == null) {
            Triple("Por calificar", Color(0xFFFFFBEB), Color(0xFFD97706))
        } else {
            Triple("Esperando cierre", Color(0xFFEFF6FF), Color(0xFF2563EB))
        }
        "COMPLETED", "COMPLETADO" -> if (job.clientRatingOfPro == null) {
            Triple("Por calificar", Color(0xFFFFFBEB), Color(0xFFD97706))
        } else {
            Triple("Esperando cierre", Color(0xFFEFF6FF), Color(0xFF2563EB))
        }
        "CANCELLED", "CANCELADO" -> Triple("Cancelado", Color(0xFFF1F5F9), Color(0xFF64748B))
        else -> Triple("En espera", Color(0xFFFFFBEB), Color(0xFFD97706))
    }
    val timeAgo = formatTimestamp(job.createdAt)
    val cardPrice: Double? = job.suggestedPrice?.takeIf { it > 0 }
        ?: job.budgetMin.takeIf { it > 0 }
    val isDirect = (job.source ?: "PUBLISH") == "DIRECTORY"
    // El badge de categoría solo se muestra cuando aporta info distinta al título.
    val badgeCategory = job.category.takeIf {
        it.isNotBlank() && !job.title.equals(it, ignoreCase = true)
    }

    RequestSummaryCard(
        timeAgo = timeAgo,
        category = badgeCategory,
        statusLabel = statusLabel,
        statusBackground = statusBg,
        statusContentColor = statusTint,
        isDirect = isDirect,
        title = job.title.ifEmpty { job.category },
        description = job.description,
        priceText = cardPrice?.let {
            "Ofertado: $${java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(it.toLong())}/h"
        },
        address = job.address.ifEmpty { "${job.municipality ?: ""}, ${job.department ?: ""}" },
        onClick = onClick,
        infoContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.LocalOffer,
                    contentDescription = null,
                    tint = Color(0xFFDB2777),
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (bidsCount == 1) "1 oferta" else "$bidsCount ofertas",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (bidsCount > 0) Color(0xFFDB2777) else PolishGrayText
                )
            }
        }
    )
}

@Composable
fun QuickServiceCard(
    category: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, PolishBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CategoryIconCircle(category = category, size = 48.dp, iconSize = 24.dp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = category,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = PolishDark,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfessionalRowCard(
    professional: ProfessionalModel,
    onClick: () -> Unit
) {
    val initials = professional.name.trim().split(" ").filter { it.isNotEmpty() }.take(2).joinToString("") { it.first().uppercase() }
    val photoModel = remember(professional.photoUrl) { decodePhotoModel(professional.photoUrl) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp))
            .border(1.dp, PolishBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Foto con iniciales si no hay foto.
            if (photoModel != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(photoModel)
                        .crossfade(true)
                        .build(),
                    contentDescription = professional.name,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .border(3.dp, PolishBlue, CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(PolishIce)
                        .border(3.dp, PolishBlue, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials.ifEmpty { "P" },
                        color = PolishBlue,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = professional.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PolishDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (professional.verified) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.Verified,
                            contentDescription = "Verificado",
                            tint = PolishBlue,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                // Categorías (mismo estilo y colores que el perfil profesional).
                if (!professional.workCategory.isNullOrBlank()) {
                    val categories = professional.workCategory
                        .split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        categories.take(3).forEach { category ->
                            ServiceCategoryTag(category = category)
                        }
                        if (categories.size > 3) {
                            ServiceCategoryTag(category = "+${categories.size - 3}")
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
                // Calificación.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = String.format("%.1f", professional.rating),
                        fontSize = 13.sp,
                        color = PolishDark,
                        fontWeight = FontWeight.Bold
                    )
                    if (professional.serviceCount > 0) {
                        Text(
                            text = " (${professional.serviceCount})",
                            fontSize = 12.sp,
                            color = PolishGrayText
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                // Ubicación.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = PolishGrayText,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = professional.location.ifEmpty { "Ubicación no disponible" },
                        fontSize = 12.sp,
                        color = PolishGrayText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun JobRequestCard(
    job: JobRequest,
    onClick: () -> Unit,
    onCancel: (() -> Unit)? = null
) {
    val statusColor = getStatusColor(job.status)
    val statusText = getStatusText(job.status)
    val isTerminal = job.status.uppercase() in listOf("CANCELLED", "CANCELADO", "COMPLETED", "COMPLETADO")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = PolishCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = job.title ?: job.category,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = PolishDark,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = statusText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        color = statusColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (job.description != null) {
                Text(
                    text = job.description,
                    fontSize = 14.sp,
                    color = PolishGrayText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (job.finalPrice != null || job.budgetMin != null) {
                    Text(
                        text = formatCurrency(job.finalPrice ?: job.budgetMin!!),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PolishBlue
                    )
                }
                if (job.professionalName != null) {
                    Text(
                        text = "con ${job.professionalName}",
                        fontSize = 13.sp,
                        color = PolishGrayText
                    )
                }
            }

            if (onCancel != null && !isTerminal) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFE53935)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE53935))
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Cancelar solicitud",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfesionalesTab(
    professionals: List<ProfessionalModel>,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit,
    categories: List<String>,
    onProfessionalClick: (ProfessionalModel) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Profesionales",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = PolishDark
        )

        Spacer(modifier = Modifier.height(12.dp))

        CategorySingleSelectField(
            selected = selectedCategory,
            options = categories,
            onSelected = onCategorySelected,
            label = "Categoría",
            placeholder = "Todas las categorías",
            allOptionLabel = "Todas las categorías",
            leadingIcon = Icons.Default.FilterList,
            accentColor = PolishBlue,
            borderColor = PolishBorder,
            containerColor = PolishCard,
            textColor = PolishDark,
            mutedTextColor = PolishGrayText
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "${professionals.size} profesionales encontrados",
            fontSize = 14.sp,
            color = PolishGrayText
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (professionals.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = PolishGrayText,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No se encontraron profesionales",
                        color = PolishGrayText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Intenta con otra búsqueda o categoría",
                        color = PolishGrayText,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(professionals) { pro ->
                    ProfessionalRowCard(
                        professional = pro,
                        onClick = { onProfessionalClick(pro) }
                    )
                }
            }
        }
    }
}

@Composable
fun BilleteraTab(
    currentUser: UserProfile?,
    onRecharge: () -> Unit
) {
    val balance = currentUser?.balance ?: 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Billetera",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = PolishDark
        )

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PolishBlue)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Saldo disponible",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = formatCurrency(balance),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onRecharge,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = PolishBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Recargar",
                        color = PolishBlue,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Transacciones recientes",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = PolishDark
        )

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Receipt,
                    contentDescription = null,
                    tint = PolishGrayText,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No hay transacciones recientes",
                    color = PolishGrayText,
                    fontSize = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun PerfilTab(
    currentUser: UserProfile?,
    onEditProfile: () -> Unit,
    onChangePassword: () -> Unit,
    onDeleteAccount: () -> Unit,
    onLogout: () -> Unit,
    onSupport: () -> Unit,
    successMessage: String? = null,
    onDismissSuccess: () -> Unit = {}
) {
    var showSupportDialog by remember { mutableStateOf(false) }
    var supportMessage by remember { mutableStateOf("") }
    val profilePhotoModel = remember(currentUser?.profilePhotoUrl) {
        decodePhotoModel(currentUser?.profilePhotoUrl)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Perfil",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = PolishDark
        )

        // Aviso de éxito (mismo estilo y ubicación que en el perfil profesional).
        if (successMessage != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDismissSuccess() }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF16A34A)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = successMessage,
                        color = Color(0xFF15803D),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = PolishCard),
            border = BorderStroke(1.dp, Color(0xFFEFF2F5)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (profilePhotoModel != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(profilePhotoModel)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Foto de perfil",
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
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = PolishBlue,
                            modifier = Modifier.size(50.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = currentUser?.name ?: "Usuario",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = PolishDark,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = currentUser?.email ?: "",
                    fontSize = 14.sp,
                    color = PolishGrayText
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val blueIconBg = Color(0xFFEFF6FF)
        val blueIconColor = Color(0xFF2563EB)
        val redIconBg = Color(0xFFFEF2F2)
        val redIconColor = Color(0xFFEF4444)

        ProfileMenuItem(
            icon = Icons.Default.Edit,
            title = "Editar perfil",
            onClick = onEditProfile,
            iconBgColor = blueIconBg,
            iconColor = blueIconColor
        )

        ProfileMenuItem(
            icon = Icons.Default.Lock,
            title = "Cambiar contraseña",
            onClick = onChangePassword,
            iconBgColor = blueIconBg,
            iconColor = blueIconColor
        )

        // CUENTA Section Divider Label
        Text(
            text = "CUENTA",
            color = PolishGrayText,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 1.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp, bottom = 12.dp)
        )

        ProfileMenuItem(
            icon = Icons.AutoMirrored.Filled.Help,
            title = "Ayuda y soporte",
            onClick = { showSupportDialog = true },
            iconBgColor = blueIconBg,
            iconColor = blueIconColor
        )

        ProfileMenuItem(
            icon = Icons.AutoMirrored.Filled.Logout,
            title = "Cerrar sesión",
            onClick = onLogout,
            iconBgColor = redIconBg,
            iconColor = redIconColor,
            titleColor = redIconColor,
            showChevron = false
        )

        ProfileMenuItem(
            icon = Icons.Default.Delete,
            title = "Eliminar cuenta",
            onClick = onDeleteAccount,
            iconBgColor = redIconBg,
            iconColor = redIconColor,
            titleColor = redIconColor,
            chevronColor = Color(0xFFF87171)
        )

        // Footer: logo + versión
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = com.galibu.client.R.drawable.img_splash_logo),
                contentDescription = "Galibu",
                modifier = Modifier.size(width = 27.dp, height = 32.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Galibu v1.0.0",
                color = PolishGrayText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(80.dp))
    }

    // Ayuda y soporte
    if (showSupportDialog) {
        AlertDialog(
            onDismissRequest = { showSupportDialog = false },
            containerColor = PolishCard,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = "Ayuda y soporte técnico",
                    color = PolishDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "¿Tienes inconvenientes con un servicio? Envía un reporte seguro al canal de soporte oficial de Galibu.",
                        color = PolishGrayText,
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = supportMessage,
                        onValueChange = { supportMessage = it.capitalizeFirstLetter() },
                        label = { Text("Detalle de tu reporte") },
                        placeholder = { Text("Ej: El profesional no llegó a la hora de la cita...") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PolishBlue,
                            focusedLabelColor = PolishBlue
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (supportMessage.isNotBlank()) {
                            supportMessage = ""
                            showSupportDialog = false
                            onSupport()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PolishBlue)
                ) {
                    Text("Enviar reporte", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSupportDialog = false }) {
                    Text("Cancelar", color = PolishGrayText)
                }
            }
        )
    }
}

@Composable
fun ProfileMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit,
    iconBgColor: Color = PolishIce,
    iconColor: Color = PolishBlue,
    titleColor: Color = PolishDark,
    showChevron: Boolean = true,
    chevronColor: Color = PolishGrayText
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PolishCard),
        border = BorderStroke(1.dp, PolishBorder),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfessionalDetailScreen(
    professional: ProfessionalModel,
    viewModel: ClientViewModel,
    onBack: () -> Unit,
    onHire: (ProfessionalModel, Double) -> Unit,
    onChat: (ProfessionalModel) -> Unit
) {
    var showRatingsSheet by remember { mutableStateOf(false) }
    var showOfferSheet by remember { mutableStateOf(false) }
    var offerPriceInput by remember { mutableStateOf("") }
    var offerError by remember { mutableStateOf<String?>(null) }
    var proRatings by remember { mutableStateOf<List<com.galibu.core.data.remote.RatingDto>>(emptyList()) }

    LaunchedEffect(professional.id) {
        showRatingsSheet = false
        proRatings = try {
            viewModel.repository.getUserRatings(professional.id)
        } catch (e: Exception) {
            emptyList()
        }
    }

    val avgRating = if (proRatings.isNotEmpty()) {
        proRatings.map { it.stars }.average()
    } else {
        professional.rating.toDouble()
    }
    val reviewsCount = if (proRatings.isNotEmpty()) proRatings.size else professional.serviceCount
    val photoModel = remember(professional.photoUrl) { decodePhotoModel(professional.photoUrl) }

    BackHandler { onBack() }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF1F6FC)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Detalle del profesional",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = PolishDark
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showRatingsSheet = true },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    if (photoModel != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(photoModel)
                                .crossfade(true)
                                .build(),
                            contentDescription = professional.name,
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(PolishIce),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = PolishBlue,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                    if (professional.isOnline) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(PolishGreen)
                                .border(2.dp, Color.White, CircleShape)
                                .align(Alignment.BottomEnd)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    val nameParts = professional.name.trim().split(Regex("\\\\s+")).filter { it.isNotBlank() }
                    val displayName = when {
                        nameParts.size >= 4 -> "${nameParts.take(2).joinToString(" ")}\\n${nameParts.drop(2).joinToString(" ")}"
                        nameParts.size == 3 -> "${nameParts.first()}\\n${nameParts.drop(1).joinToString(" ")}"
                        else -> professional.name
                    }
                    Text(
                        text = displayName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = PolishDark,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format(Locale.US, "%.1f", avgRating),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = PolishDark
                        )
                        Text(
                            text = " ($reviewsCount reseñas)",
                            color = Color(0xFF64748B),
                            fontSize = 14.sp
                        )
                    }
                    if (professional.verified) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE0E7FF))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = PolishBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Profesional verificado",
                                color = PolishBlue,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Ver reseñas",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!professional.workCategory.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.BusinessCenter,
                        contentDescription = null,
                        tint = PolishBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = professional.workCategory,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = PolishBlue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = PolishGrayText,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = professional.location.ifEmpty { "Ubicación no disponible" },
                    fontSize = 14.sp,
                    color = PolishGrayText
                )
            }

            val proWorkDescription = professional.workDescription?.trim().orEmpty()
            val proPortfolioPhotos = professional.workPhotos
                ?.split("|")
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?: emptyList()

            if (proWorkDescription.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = PolishBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Descripción del profesional",
                        color = PolishDark,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = proWorkDescription,
                    color = Color(0xFF64748B),
                    fontSize = 13.sp
                )
            }

            if (proPortfolioPhotos.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        tint = PolishBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Portafolio de trabajos",
                        color = PolishDark,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    proPortfolioPhotos.forEach { photo ->
                        AsyncImage(
                            model = com.galibu.core.ui.utils.ImageUtils.getCoilModel(photo),
                            contentDescription = "Trabajo del portafolio",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(116.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val proHourlyRate = professional.hourlyRate
            if (proHourlyRate != null && proHourlyRate > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "$${java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(proHourlyRate)}",
                        fontWeight = FontWeight.Black,
                        color = PolishDark,
                        fontSize = 22.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "COP",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
                Text(
                    text = "Tarifa por hora",
                    color = Color(0xFF64748B),
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        offerPriceInput = ""
                        offerError = null
                        showOfferSheet = true
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = PolishGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Contratar",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = { onChat(professional) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(PolishBlue)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PolishBlue)
                ) {
                    Icon(
                        Icons.Default.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Contactar",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Hoja de calificaciones al dar tap (volver retorna al detalle).
    if (showRatingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showRatingsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showRatingsSheet = false },
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
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Calificaciones y reseñas",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = PolishDark
                        )
                        Text(
                            text = professional.name,
                            color = Color(0xFF64748B),
                            fontSize = 13.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                if (proRatings.isEmpty()) {
                    Text(
                        text = "Aún no tiene reseñas.",
                        color = Color(0xFF64748B),
                        fontSize = 14.sp
                    )
                } else {
                    val dateFmt = remember { java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale("es", "CO")) }
                    proRatings.forEach { rating ->
                        val sheetReview = rating.comment
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    (1..5).forEach { i ->
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = if (i <= rating.stars) Color(0xFFF59E0B) else Color(0xFFE2E8F0),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = dateFmt.format(java.util.Date(rating.createdAt)),
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                                if (!sheetReview.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "\"${sheetReview}\"",
                                        fontSize = 13.sp,
                                        color = PolishDark
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Hoja de oferta al dar tap en Contratar.
    if (showOfferSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showOfferSheet = false
                offerError = null
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Enviar oferta",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = PolishDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Oferta para ${professional.name}",
                    color = Color(0xFF64748B),
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Precio de oferta por hora (COP)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = PolishDark
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = formatThousandsInput(offerPriceInput),
                    onValueChange = { offerPriceInput = it.filter { c -> c.isDigit() }.take(9) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Ej: 30000 por hora") },
                    prefix = { Text("$ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = offerError != null,
                    supportingText = {
                        if (offerError != null) {
                            Text(
                                text = offerError!!,
                                color = Color(0xFFDC2626)
                            )
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PolishBlue,
                        unfocusedBorderColor = PolishBorder
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val offered = offerPriceInput.toDoubleOrNull() ?: 0.0
                        if (offered < MIN_HOURLY_PRICE_COP) {
                            offerError = "El precio no puede ser menor al salario mínimo por hora ($5.417 COP)"
                        } else {
                            offerError = null
                            showOfferSheet = false
                            onHire(professional, offered)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PolishGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Enviar oferta",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun AddressFieldWithGps(
    address: String,
    onAddressChange: (String) -> Unit,
    department: String,
    municipality: String,
    placeholder: String = "Dirección del servicio",
    onGpsFix: ((dept: String?, muni: String?) -> Unit)? = null,
    showLocationIcon: Boolean = true,
    fieldShape: Shape = RoundedCornerShape(12.dp),
    containerColor: Color = Color.Transparent
) {
    val context = LocalContext.current
    var isFetchingLocation by remember { mutableStateOf(false) }
    var showEnableGpsDialog by remember { mutableStateOf(false) }

    fun requestGpsAddress() {
        if (!isDeviceLocationEnabled(context)) {
            showEnableGpsDialog = true
            return
        }
        isFetchingLocation = true
        getCurrentGpsLocation(
            context,
            department,
            municipality,
            onLocationFetched = { result ->
                onAddressChange(result)
                isFetchingLocation = false
            },
            onGpsUnavailable = {
                isFetchingLocation = false
                showEnableGpsDialog = true
            },
            onDetailedFix = { fix ->
                onGpsFix?.invoke(fix.adminArea, fix.locality)
            }
        )
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            requestGpsAddress()
        }
    }

    Column {
        OutlinedTextField(
            value = address,
            onValueChange = onAddressChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder) },
            leadingIcon = if (showLocationIcon) {
                {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = PolishGrayText)
                }
            } else null,
            trailingIcon = {
                IconButton(onClick = {
                    val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    if (hasFine || hasCoarse) {
                        requestGpsAddress()
                    } else {
                        locationPermissionLauncher.launch(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                        )
                    }
                }) {
                    if (isFetchingLocation) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = PolishBlue
                        )
                    } else {
                        Icon(
                            Icons.Default.MyLocation,
                            contentDescription = "Usar mi ubicación (GPS)",
                            tint = PolishBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = fieldShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = containerColor,
                unfocusedContainerColor = containerColor,
                focusedBorderColor = PolishBlue,
                unfocusedBorderColor = PolishBorder
            )
        )
        if (showEnableGpsDialog) {
            GalibuAlertDialog(
                title = "Activa tu ubicación",
                message = "La ubicación del dispositivo está desactivada. Actívala para usar el GPS o escribe la dirección manualmente.",
                type = GalibuAlertType.WARNING,
                confirmText = "Abrir ajustes",
                dismissText = "Escribir manual",
                onConfirm = {
                    showEnableGpsDialog = false
                    try {
                        context.startActivity(
                            android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        )
                    } catch (e: Exception) {}
                },
                onDismissRequest = { showEnableGpsDialog = false }
            )
        }
    }
}

private fun formatThousandsInput(digits: String): String {
    if (digits.isEmpty()) return ""
    return digits.reversed().chunked(3).joinToString(".").reversed()
}

/**
 * Busca [wanted] (p. ej. del geocoder) dentro del catálogo y devuelve el nombre
 * canónico. Tolera mayúsculas y diferencias menores ("Bogotá D.C." vs "Bogotá").
 */
private fun matchCatalogName(candidates: List<String>, wanted: String?): String? {
    if (wanted.isNullOrBlank()) return null
    val w = wanted.trim()
    candidates.firstOrNull { it.equals(w, ignoreCase = true) }?.let { return it }
    val normW = w.lowercase().replace(".", "").replace("d c", "").trim()
    return candidates.firstOrNull {
        val normC = it.lowercase().replace(".", "").replace("d c", "").trim()
        normC == normW || normC.contains(normW) || normW.contains(normC)
    }
}

private const val MIN_HOURLY_PRICE_COP = 5417.0

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun JobPostForm(
    currentUser: UserProfile?,
    categories: List<String>,
    customCategories: List<String>,
    onCustomCategoryAdded: (String) -> Unit,
    departmentsAndMunicipalities: List<Pair<String, List<String>>>,
    onDismiss: () -> Unit,
    onShowError: (String) -> Unit,
    onSubmit: (category: String, title: String, description: String, budget: Double, address: String, department: String, municipality: String, suggestedPrice: Double?, photos: List<String>, onError: (String) -> Unit, onSuccess: () -> Unit) -> Unit
) {
    var selectedCategory by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var municipality by remember { mutableStateOf("") }
    var suggestedPriceInput by remember { mutableStateOf("") }
    var showDeptDropdown by remember { mutableStateOf(false) }
    var showMuniDropdown by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    // División detectada por GPS (nombres tal como vienen del geocoder).
    var gpsDetectedDept by remember { mutableStateOf<String?>(null) }
    var gpsDetectedMuni by remember { mutableStateOf<String?>(null) }
    var showCustomCategoryDialog by remember { mutableStateOf(false) }
    var customCategoryInput by remember { mutableStateOf("") }

    val jobPhotos = remember { mutableStateListOf<String>() }
    val context = LocalContext.current
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            if (jobPhotos.size >= 6) {
                onShowError("Puedes agregar hasta 6 fotos.")
            } else {
                val base64 = com.galibu.core.ui.utils.ImageUtils.uriToBase64(
                    context = context,
                    uri = uri,
                    maxDim = 640
                )
                if (base64 != null) jobPhotos.add(base64)
                else onShowError("No pudimos procesar esa imagen.")
            }
        }
    }

    // Departamentos con el detectado primero (si coincide con el catálogo).
    val orderedDepartments = remember(departmentsAndMunicipalities, gpsDetectedDept) {
        val matched = matchCatalogName(departmentsAndMunicipalities.map { it.first }, gpsDetectedDept)
        if (matched == null) {
            departmentsAndMunicipalities
        } else {
            val rest = departmentsAndMunicipalities.filter { it.first != matched }
            listOf(departmentsAndMunicipalities.first { it.first == matched }) + rest
        }
    }
    val gpsMatchedDept = remember(orderedDepartments, gpsDetectedDept) {
        matchCatalogName(orderedDepartments.map { it.first }, gpsDetectedDept)
    }
    val offeredPriceError = suggestedPriceInput.isNotEmpty() &&
        (suggestedPriceInput.toDoubleOrNull() ?: 0.0) < MIN_HOURLY_PRICE_COP
    val trimmedDescription = description.trim()
    val trimmedAddress = address.trim()
    val requiredFieldsComplete =
        selectedCategory.isNotBlank() &&
            trimmedDescription.isNotEmpty() &&
            trimmedAddress.isNotEmpty() &&
            department.isNotBlank() &&
            municipality.isNotBlank() &&
            suggestedPriceInput.isNotBlank() &&
            !offeredPriceError

    val customCategoryLabel = "Otro"
    val categoryOptions = remember(categories, customCategories) {
        (
            categories.filterNot {
                it.equals(customCategoryLabel, ignoreCase = true) ||
                    it.equals("Otro +", ignoreCase = true)
            } +
                customCategories.filterNot {
                    it.equals(customCategoryLabel, ignoreCase = true) ||
                        it.equals("Otro +", ignoreCase = true)
                } +
                customCategoryLabel
            ).distinct()
    }

    BackHandler(enabled = !isSubmitting) { onDismiss() }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { },
        color = PolishCard
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        enabled = !isSubmitting,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver al inicio",
                            tint = PolishDark,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = "Publicar solicitud",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = PolishDark
                    )
                }

                HorizontalDivider(color = PolishBorder)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                ) {

                CategorySingleSelectField(
                    selected = selectedCategory.takeIf { it.isNotBlank() },
                    options = categoryOptions,
                    onSelected = { selectedCategory = it ?: "" },
                    label = "Categoría *",
                    placeholder = "Seleccionar categoría",
                    customOptionLabel = customCategoryLabel,
                    onAddCustom = {
                        customCategoryInput = ""
                        showCustomCategoryDialog = true
                    },
                    accentColor = PolishBlue,
                    borderColor = PolishBorder,
                    containerColor = Color.White,
                    textColor = PolishDark,
                    mutedTextColor = PolishGrayText
                )

                if (showCustomCategoryDialog) {
                    val trimmedCategory = customCategoryInput.trim()
                    val reservedCategory = trimmedCategory.equals(customCategoryLabel, ignoreCase = true) ||
                        trimmedCategory.equals("Otro +", ignoreCase = true)
                    AlertDialog(
                        onDismissRequest = {
                            showCustomCategoryDialog = false
                            customCategoryInput = ""
                        },
                        containerColor = PolishCard,
                        shape = RoundedCornerShape(16.dp),
                        title = {
                            Text(
                                text = "Agregar categoría",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = PolishDark
                            )
                        },
                        text = {
                            Column {
                                Text(
                                    text = "Escribe la categoría que necesitas para esta solicitud.",
                                    fontSize = 14.sp,
                                    color = PolishGrayText
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = customCategoryInput,
                                    onValueChange = { customCategoryInput = it.take(60).capitalizeFirstLetter() },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    label = { Text("Nueva categoría") },
                                    placeholder = { Text("Ej: Reparación de electrodomésticos") },
                                    isError = reservedCategory,
                                    supportingText = if (reservedCategory) {
                                        {
                                            Text(
                                                text = "Escribe un nombre diferente a Otro.",
                                                color = Color(0xFFDC2626)
                                            )
                                        }
                                    } else {
                                        null
                                    },
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Words
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PolishBlue,
                                        unfocusedBorderColor = PolishBorder
                                    )
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    val existingCategory = (categories + customCategories)
                                        .firstOrNull { it.equals(trimmedCategory, ignoreCase = true) }
                                    if (existingCategory == null) {
                                        onCustomCategoryAdded(trimmedCategory)
                                        selectedCategory = trimmedCategory
                                    } else {
                                        selectedCategory = existingCategory
                                    }
                                    customCategoryInput = ""
                                    showCustomCategoryDialog = false
                                },
                                enabled = trimmedCategory.isNotEmpty() && !reservedCategory
                            ) {
                                Text(
                                    text = "Agregar",
                                    color = if (trimmedCategory.isNotEmpty() && !reservedCategory) {
                                        PolishBlue
                                    } else {
                                        PolishGrayText
                                    },
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    showCustomCategoryDialog = false
                                    customCategoryInput = ""
                                }
                            ) {
                                Text(text = "Cancelar", color = PolishGrayText)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Descripción *",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = PolishDark
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it.capitalizeFirstLetter() },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 3,
                    placeholder = { Text("Describe el problema o servicio que necesitas...") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PolishBlue,
                        unfocusedBorderColor = PolishBorder
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Fotos (opcional)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = PolishDark
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Agrega hasta 6 fotos del problema o del área a intervenir.",
                    fontSize = 12.sp,
                    color = PolishGrayText
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (jobPhotos.size < 6) {
                        Box(
                            modifier = Modifier
                                .size(84.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .border(1.dp, PolishBorder, RoundedCornerShape(12.dp))
                                .clickable { photoPickerLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Agregar foto",
                                    tint = PolishGrayText,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Agregar",
                                    color = PolishGrayText,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    jobPhotos.forEach { photo ->
                        Box(
                            modifier = Modifier.size(84.dp)
                        ) {
                            AsyncImage(
                                model = com.galibu.core.ui.utils.ImageUtils.getCoilModel(photo),
                                contentDescription = "Foto de la solicitud",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(22.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                    .clickable { jobPhotos.remove(photo) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Quitar foto",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Dirección *",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = PolishDark
                )
                Spacer(modifier = Modifier.height(8.dp))

                AddressFieldWithGps(
                    address = address,
                    onAddressChange = { address = it.capitalizeFirstLetter() },
                    department = department,
                    municipality = municipality,
                    onGpsFix = { dept, muni ->
                        gpsDetectedDept = dept
                        gpsDetectedMuni = muni
                        // Auto-selecciona lo detectado (el usuario puede cambiarlo después).
                        matchCatalogName(departmentsAndMunicipalities.map { it.first }, dept)?.let { matchedDept ->
                            department = matchedDept
                            val munis = departmentsAndMunicipalities.first { it.first == matchedDept }.second
                            matchCatalogName(munis, muni)?.let { municipality = it }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Departamento *",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = PolishDark
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = department,
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showDeptDropdown = true },
                                placeholder = { Text("Seleccionar") },
                                trailingIcon = {
                                    IconButton(onClick = { showDeptDropdown = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Mostrar departamentos", tint = PolishBlue)
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PolishBlue,
                                    unfocusedBorderColor = PolishBorder
                                )
                            )
                            DropdownMenu(
                                expanded = showDeptDropdown,
                                onDismissRequest = { showDeptDropdown = false }
                            ) {
                                orderedDepartments.forEach { (dept, munis) ->
                                    DropdownMenuItem(
                                        text = { Text(dept) },
                                        onClick = {
                                            department = dept
                                            municipality = ""
                                            showDeptDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Municipio *",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = PolishDark
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val currentMunis = remember(department, orderedDepartments, gpsMatchedDept, gpsDetectedMuni) {
                            val base = orderedDepartments.firstOrNull { it.first == department }?.second ?: emptyList()
                            // Si el departamento elegido es el detectado por GPS, su municipio va primero.
                            if (department.isNotEmpty() && department == gpsMatchedDept) {
                                val matched = matchCatalogName(base, gpsDetectedMuni)
                                if (matched != null) {
                                    listOf(matched) + base.filter { it != matched }
                                } else {
                                    base
                                }
                            } else {
                                base
                            }
                        }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = municipality,
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showMuniDropdown = true },
                                placeholder = { Text("Seleccionar") },
                                trailingIcon = {
                                    IconButton(onClick = { showMuniDropdown = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Mostrar municipios", tint = PolishBlue)
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PolishBlue,
                                    unfocusedBorderColor = PolishBorder
                                )
                            )
                            DropdownMenu(
                                expanded = showMuniDropdown,
                                onDismissRequest = { showMuniDropdown = false }
                            ) {
                                if (department.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("Primero selecciona un departamento") },
                                        onClick = { },
                                        enabled = false
                                    )
                                } else if (currentMunis.isEmpty()) {
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(16.dp),
                                                    strokeWidth = 2.dp
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Cargando municipios...")
                                            }
                                        },
                                        onClick = { },
                                        enabled = false
                                    )
                                } else {
                                    currentMunis.forEach { muni ->
                                        DropdownMenuItem(
                                            text = { Text(muni) },
                                            onClick = {
                                                municipality = muni
                                                showMuniDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Precio ofertado por hora (COP) *",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = PolishDark
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = formatThousandsInput(suggestedPriceInput),
                    onValueChange = { suggestedPriceInput = it.filter { c -> c.isDigit() }.take(9) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Ej: 30000") },
                    prefix = { Text("$ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = offeredPriceError,
                    supportingText = {
                        if (offeredPriceError) {
                            Text(
                                text = "El precio no puede ser menor al salario mínimo por hora ($5.417 COP)",
                                color = Color(0xFFDC2626)
                            )
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PolishBlue,
                        unfocusedBorderColor = PolishBorder
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))
                }

                // Barra de acciones fija: solo Publicar.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp)
                        .background(PolishCard)
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick = {
                            if (!isSubmitting) {
                                isSubmitting = true
                                onSubmit(
                                    selectedCategory,
                                    selectedCategory,
                                    trimmedDescription,
                                    0.0,
                                    trimmedAddress,
                                    department,
                                    municipality,
                                    suggestedPriceInput.toDoubleOrNull(),
                                    jobPhotos.toList(),
                                    { error ->
                                        isSubmitting = false
                                        onShowError(error)
                                    },
                                    {
                                        isSubmitting = false
                                        onDismiss()
                                    }
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = requiredFieldsComplete && !isSubmitting,
                        colors = ButtonDefaults.buttonColors(containerColor = PolishBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Publicar",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RatingDialog(
    jobId: String,
    onDismiss: () -> Unit,
    onSubmit: (rating: Int, review: String) -> Unit
) {
    // Mismo diseño homologado de calificación que el resto de pantallas.
    GalibuRatingSheet(
        title = "Calificar al profesional",
        targetName = "el profesional",
        onDismissRequest = onDismiss,
        onSubmit = onSubmit
    )
}

@Composable
fun CancelJobDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PolishCard,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = "Cancelar trabajo",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = PolishDark
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "¿Estás seguro de que deseas cancelar este trabajo?",
                    fontSize = 14.sp,
                    color = PolishGrayText
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Razón (opcional)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = PolishDark
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it.capitalizeFirstLetter() },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Motivo de la cancelación...") },
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFE53935),
                        unfocusedBorderColor = PolishBorder
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(reason) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Sí, cancelar",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "No, mantener",
                    color = PolishGrayText
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    currentUser: UserProfile?,
    departmentsAndMunicipalities: List<Pair<String, List<String>>>,
    onDismiss: () -> Unit,
    onSave: (name: String, department: String, municipality: String, address: String, photo: String?) -> Unit,
    onError: (String) -> Unit = {},
    isForced: Boolean = false
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(currentUser?.name ?: "") }
    var department by remember { mutableStateOf(currentUser?.department ?: "") }
    var municipality by remember { mutableStateOf(currentUser?.municipality ?: "") }
    var address by remember { mutableStateOf(currentUser?.address ?: "") }
    var photoUri by remember { mutableStateOf(currentUser?.profilePhotoUrl) }
    var showDeptDropdown by remember { mutableStateOf(false) }
    var showMuniDropdown by remember { mutableStateOf(false) }
    var gpsDetectedDept by remember { mutableStateOf<String?>(null) }
    var gpsDetectedMuni by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            photoUri = com.galibu.core.ui.utils.ImageUtils.uriToBase64(context, uri) ?: uri.toString()
        }
    }

    // Selector de origen de foto (galería / cámara)
    var showPhotoSourceDialog by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            try {
                val out = java.io.ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 75, out)
                val base64Str = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
                photoUri = "data:image/jpeg;base64,$base64Str"
            } catch (e: Exception) {
                e.printStackTrace()
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
                e.printStackTrace()
            }
        }
    }

    val launchCamera = {
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            try {
                cameraLauncher.launch(null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val orderedDepartments = remember(departmentsAndMunicipalities, gpsDetectedDept) {
        val matched = matchCatalogName(departmentsAndMunicipalities.map { it.first }, gpsDetectedDept)
        if (matched == null) {
            departmentsAndMunicipalities
        } else {
            val rest = departmentsAndMunicipalities.filter { it.first != matched }
            listOf(departmentsAndMunicipalities.first { it.first == matched }) + rest
        }
    }
    val gpsMatchedDept = remember(orderedDepartments, gpsDetectedDept) {
        matchCatalogName(orderedDepartments.map { it.first }, gpsDetectedDept)
    }
    val currentMunis = remember(department, orderedDepartments, gpsMatchedDept, gpsDetectedMuni) {
        val base = orderedDepartments.firstOrNull { it.first == department }?.second ?: emptyList()
        if (department.isNotEmpty() && department == gpsMatchedDept) {
            val matched = matchCatalogName(base, gpsDetectedMuni)
            if (matched != null) {
                listOf(matched) + base.filter { it != matched }
            } else {
                base
            }
        } else {
            base
        }
    }

    // Pantalla completa (homologada con el perfil profesional): sin menú inferior.
    BackHandler(enabled = true) {
        if (!isForced) onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PolishBg)
    ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Barra de título con flecha atrás
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (!isForced) onDismiss() },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retroceder",
                            tint = PolishDark,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = "Editar perfil",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = PolishDark,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }

                HorizontalDivider(color = Color(0xFFEFF2F5), thickness = 1.dp)

                if (isForced) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(PolishBlue.copy(alpha = 0.1f))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "Completa tu perfil para continuar usando la app.",
                            color = PolishBlue,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
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
                            .clickable { showPhotoSourceDialog = true }
                    ) {
                        if (!photoUri.isNullOrBlank()) {
                            AsyncImage(
                                model = com.galibu.core.ui.utils.ImageUtils.getCoilModel(photoUri),
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
                        // Camera badge
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(PolishBlue)
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

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Nombre completo",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PolishDark
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it.capitalizeFirstLetter() },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = PolishBlue,
                            unfocusedBorderColor = PolishBorder
                        )
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Correo electrónico",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PolishDark
                    )
                    OutlinedTextField(
                        value = currentUser?.email ?: "",
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF1F5F9),
                            unfocusedContainerColor = Color(0xFFF1F5F9),
                            focusedBorderColor = PolishBorder,
                            unfocusedBorderColor = PolishBorder,
                            focusedTextColor = Color(0xFF64748B),
                            unfocusedTextColor = Color(0xFF64748B)
                        )
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Ubicación",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PolishDark
                    )
                    AddressFieldWithGps(
                        address = address,
                        onAddressChange = { address = it.capitalizeFirstLetter() },
                        department = department,
                        municipality = municipality,
                        placeholder = "Calle 45 # 23-10",
                        showLocationIcon = false,
                        fieldShape = RoundedCornerShape(14.dp),
                        containerColor = Color.White,
                        onGpsFix = { dept, muni ->
                            gpsDetectedDept = dept
                            gpsDetectedMuni = muni
                            // Auto-selecciona lo detectado (el usuario puede cambiarlo después).
                            matchCatalogName(departmentsAndMunicipalities.map { it.first }, dept)?.let { matchedDept ->
                                department = matchedDept
                                val munis = departmentsAndMunicipalities.first { it.first == matchedDept }.second
                                matchCatalogName(munis, muni)?.let { municipality = it }
                            }
                        }
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Departamento",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PolishDark
                    )
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = department,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDeptDropdown = true },
                            placeholder = { Text("Seleccionar") },
                            trailingIcon = {
                                IconButton(onClick = { showDeptDropdown = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Mostrar departamentos", tint = PolishBlue)
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = PolishBlue,
                                unfocusedBorderColor = PolishBorder
                            )
                        )
                        DropdownMenu(
                            expanded = showDeptDropdown,
                            onDismissRequest = { showDeptDropdown = false }
                        ) {
                            orderedDepartments.forEach { (dept, munis) ->
                                DropdownMenuItem(
                                    text = { Text(dept) },
                                    onClick = {
                                        department = dept
                                        municipality = ""
                                        showDeptDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Municipio",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PolishDark
                    )
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = municipality,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showMuniDropdown = true },
                            placeholder = { Text("Seleccionar") },
                            trailingIcon = {
                                IconButton(onClick = { showMuniDropdown = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Mostrar municipios", tint = PolishBlue)
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = PolishBlue,
                                unfocusedBorderColor = PolishBorder
                            )
                        )
                        DropdownMenu(
                            expanded = showMuniDropdown,
                            onDismissRequest = { showMuniDropdown = false }
                        ) {
                            if (department.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Primero selecciona un departamento") },
                                    onClick = { },
                                    enabled = false
                                )
                            } else {
                                currentMunis.forEach { muni ->
                                    DropdownMenuItem(
                                        text = { Text(muni) },
                                        onClick = {
                                            municipality = muni
                                            showMuniDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Botón al final del contenido scrolleable (igual que el perfil profesional).
                Button(
                    onClick = {
                        val trimmedName = name.trim()
                        val trimmedDept = department.trim()
                        val trimmedMuni = municipality.trim()
                        val trimmedAddress = address.trim()

                        if (trimmedName.isEmpty()) {
                            onError("Por favor complete todos los campos obligatorios.")
                            return@Button
                        }
                        if (photoUri.isNullOrBlank()) {
                            onError("Sube tu foto de perfil para continuar.")
                            return@Button
                        }
                        if (isForced && (trimmedDept.isEmpty() || trimmedMuni.isEmpty() || trimmedAddress.isEmpty())) {
                            onError("Completa departamento, municipio y dirección para continuar.")
                            return@Button
                        }
                        onSave(trimmedName, trimmedDept, trimmedMuni, trimmedAddress, photoUri)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PolishBlue),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "Guardar cambios",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
            }
    }

    // Selector de origen de la foto de perfil (galería / cámara)
    if (showPhotoSourceDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoSourceDialog = false },
            containerColor = PolishCard,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = "Foto de perfil",
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
                        text = "Elige de dónde deseas obtener tu foto de perfil:",
                        fontSize = 14.sp,
                        color = PolishGrayText
                    )

                    // Galería
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(PolishIce)
                            .clickable {
                                showPhotoSourceDialog = false
                                photoPickerLauncher.launch("image/*")
                            }
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
                                text = "Elegir de la galería",
                                fontWeight = FontWeight.Bold,
                                color = PolishDark,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Selecciona una foto de tus archivos",
                                color = PolishGrayText,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Cámara
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(PolishIce)
                            .clickable {
                                showPhotoSourceDialog = false
                                launchCamera()
                            }
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
                                text = "Tomar una foto",
                                fontWeight = FontWeight.Bold,
                                color = PolishDark,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Captura una nueva foto con tu cámara",
                                color = PolishGrayText,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPhotoSourceDialog = false }) {
                    Text(
                        text = "Cancelar",
                        fontWeight = FontWeight.Bold,
                        color = PolishBlue
                    )
                }
            }
        )
    }
}

@Composable
fun ChangePasswordScreen(
    onDismiss: () -> Unit,
    onError: (String) -> Unit = {},
    onSubmit: (currentPassword: String, newPassword: String, onResult: (String?) -> Unit) -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showCurrentPassword by remember { mutableStateOf(false) }
    var showNewPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }

    // Pantalla completa (homologada con el perfil profesional): sin menú inferior.
    BackHandler(enabled = true) { onDismiss() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PolishBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Barra de título con flecha atrás
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onDismiss() },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Retroceder",
                        tint = PolishDark,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = "Cambiar contraseña",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = PolishDark,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            HorizontalDivider(color = Color(0xFFEFF2F5), thickness = 1.dp)

            PasswordField(
                label = "Contraseña actual",
                value = currentPassword,
                onValueChange = { currentPassword = it },
                visible = showCurrentPassword,
                onToggleVisibility = { showCurrentPassword = !showCurrentPassword }
            )

            PasswordField(
                label = "Nueva contraseña",
                value = newPassword,
                onValueChange = { newPassword = it },
                visible = showNewPassword,
                onToggleVisibility = { showNewPassword = !showNewPassword }
            )

            PasswordField(
                label = "Confirmar contraseña",
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                visible = showConfirmPassword,
                onToggleVisibility = { showConfirmPassword = !showConfirmPassword }
            )

            Button(
                onClick = {
                    if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                        onError("Por favor complete todos los campos")
                        return@Button
                    }
                    if (newPassword != confirmPassword) {
                        onError("La nueva contraseña y su confirmación no coinciden")
                        return@Button
                    }
                    onSubmit(currentPassword.trim(), newPassword.trim()) { errorMsg ->
                        if (errorMsg == null) {
                            currentPassword = ""
                            newPassword = ""
                            confirmPassword = ""
                        } else {
                            onError(errorMsg)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PolishBlue),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = "Cambiar contraseña",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun PasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    visible: Boolean,
    onToggleVisibility: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = PolishDark
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = onToggleVisibility) {
                    Icon(
                        imageVector = if (visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (visible) "Ocultar contraseña" else "Mostrar contraseña"
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = PolishBlue,
                unfocusedBorderColor = PolishBorder
            )
        )
    }
}

@Composable
fun DeleteAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var confirmText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PolishCard,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = "Eliminar cuenta",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE53935)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Esta acción es permanente y no se puede deshacer. Se eliminarán todos tus datos, incluyendo perfil, trabajos y calificaciones.",
                    fontSize = 14.sp,
                    color = PolishGrayText
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Escribe \"ELIMINAR\" para confirmar:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = PolishDark
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = confirmText,
                    onValueChange = { confirmText = it.capitalizeFirstLetter() },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("ELIMINAR") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFE53935),
                        unfocusedBorderColor = PolishBorder
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = confirmText == "ELIMINAR",
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Eliminar cuenta",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancelar",
                    color = PolishGrayText
                )
            }
        }
    )
}

@Composable
fun RechargeDialog(
    onDismiss: () -> Unit,
    onSubmit: (Double) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    val presetAmounts = listOf(10000.0, 25000.0, 50000.0, 100000.0, 200000.0, 500000.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PolishCard,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = "Recargar saldo",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = PolishDark
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Selecciona un monto rápido o ingresa uno personalizado",
                    fontSize = 14.sp,
                    color = PolishGrayText
                )

                Spacer(modifier = Modifier.height(16.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetAmounts.forEach { preset ->
                        FilterChip(
                            selected = amount == preset.toLong().toString(),
                            onClick = { amount = preset.toLong().toString() },
                            label = { Text(formatCurrency(preset)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PolishBlue,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = amount,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() || it == '.' }) {
                            amount = newValue
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Monto personalizado (COP)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = {
                        Icon(
                            Icons.Default.Money,
                            contentDescription = null,
                            tint = PolishGrayText
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PolishBlue,
                        unfocusedBorderColor = PolishBorder
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountValue = amount.toDoubleOrNull() ?: 0.0
                    if (amountValue > 0) {
                        onSubmit(amountValue)
                    }
                },
                enabled = (amount.toDoubleOrNull() ?: 0.0) > 0,
                colors = ButtonDefaults.buttonColors(containerColor = PolishBlue),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Recargar",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancelar",
                    color = PolishGrayText
                )
            }
        }
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun NotificationsPanel(
    notifications: List<AppNotification>,
    onDismiss: () -> Unit,
    onNotificationClick: (AppNotification) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PolishCard,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .padding(horizontal = 140.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(PolishBorder)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Notificaciones",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = PolishDark
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = PolishGrayText
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (notifications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            tint = PolishGrayText,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No hay notificaciones",
                            color = PolishGrayText,
                            fontSize = 16.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp)
                ) {
                    items(notifications) { notification ->
                        NotificationItem(
                            notification = notification,
                            onClick = { onNotificationClick(notification) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItem(
    notification: AppNotification,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) PolishCard else PolishIce
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            if (!notification.isRead) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(PolishBlue)
                        .padding(top = 4.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = notification.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = PolishDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notification.body,
                    fontSize = 13.sp,
                    color = PolishGrayText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatTimestamp(notification.timestamp),
                    fontSize = 11.sp,
                    color = PolishGrayText
                )
            }
        }
    }
}

@Composable
fun JobDetailCard(
    job: JobRequest,
    onChat: () -> Unit,
    onBids: () -> Unit,
    onCancel: () -> Unit,
    onRate: () -> Unit,
    onPay: () -> Unit
) {
    val statusColor = getStatusColor(job.status)
    val statusText = getStatusText(job.status)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PolishCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CategoryBadge(category = job.category)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = statusText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = job.title ?: job.category,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = PolishDark
            )

            Spacer(modifier = Modifier.height(6.dp))

            if (job.description != null && job.description.isNotEmpty()) {
                Text(
                    text = job.description,
                    fontSize = 14.sp,
                    color = PolishGrayText,
                    lineHeight = 20.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(color = PolishBorder, thickness = 1.dp)

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Presupuesto",
                        fontSize = 12.sp,
                        color = PolishGrayText
                    )
                    Text(
                        text = formatCurrency(job.finalPrice ?: job.budgetMin ?: 0.0),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PolishBlue
                    )
                }
                if (job.professionalName != null) {
                    val proName = job.professionalName ?: ""
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Profesional",
                            fontSize = 12.sp,
                            color = PolishGrayText
                        )
                        Text(
                            text = proName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = PolishDark
                        )
                    }
                }
            }

            if (job.address != null && job.address.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = PolishGrayText,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = job.address,
                        fontSize = 13.sp,
                        color = PolishGrayText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (job.department != null || job.municipality != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Home,
                        contentDescription = null,
                        tint = PolishGrayText,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = listOfNotNull(job.municipality, job.department).joinToString(", "),
                        fontSize = 13.sp,
                        color = PolishGrayText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (job.status.uppercase()) {
                "OPEN", "ABIERTO" -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onBids,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                Icons.Default.Receipt,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Ver ofertas")
                        }
                        OutlinedButton(
                            onClick = onCancel,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE53935)),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFE53935))
                            )
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Cancelar")
                        }
                    }
                }
                "ACCEPTED", "ACEPTADO", "IN_PROGRESS", "EN_PROGRESO" -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onChat,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                Icons.Default.Chat,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Chat")
                        }
                        Button(
                            onClick = onPay,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = PolishGreen),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                Icons.Default.Payment,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Pagar",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                "PENDING_RATING", "PENDIENTE_CALIFICACION" -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onChat,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                Icons.Default.Chat,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Chat")
                        }
                        Button(
                            onClick = onRate,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Calificar",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                "COMPLETED", "COMPLETADO" -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (job.clientRatingOfPro == null) {
                            Button(
                                onClick = onRate,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Calificar profesional",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Trabajo completado",
                                    fontSize = 14.sp,
                                    color = PolishGreen,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    repeat(5) { index ->
                                        Icon(
                                            if (index < (job.clientRatingOfPro ?: 0f).toInt()) Icons.Default.Star else Icons.Default.StarBorder,
                                            contentDescription = null,
                                            tint = Color(0xFFFFC107),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                "CANCELLED", "CANCELADO" -> {
                    Text(
                        text = "Trabajo cancelado",
                        fontSize = 14.sp,
                        color = Color(0xFFE53935),
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (job.status.uppercase() == "CANCELLED" && job.counterOfferAmount != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Razón: ${job.completionReview ?: "No especificada"}",
                            fontSize = 13.sp,
                            color = PolishGrayText,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                "NEGOTIATING", "NEGOCIANDO" -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onChat,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                Icons.Default.Chat,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Chat")
                        }
                    }
                    if (job.counterOfferAmount != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFF3E0)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Money,
                                    contentDescription = null,
                                    tint = Color(0xFFFF9800),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Contraoferta de ${job.counterOfferSender ?: "el profesional"}",
                                        fontSize = 13.sp,
                                        color = Color(0xFFE65100),
                                        fontWeight = FontWeight.Medium
                                    )
                                    val coAmount = job.counterOfferAmount ?: 0.0
                                    Text(
                                        text = formatCurrency(coAmount),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE65100)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
fun ActiveJobsSection(
    jobs: List<JobRequest>,
    onJobClick: (JobRequest) -> Unit,
    onChatClick: (String) -> Unit,
    onCancelClick: (String) -> Unit,
    onRateClick: (String) -> Unit,
    onPayClick: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Trabajos activos",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = PolishDark
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = PolishBlue.copy(alpha = 0.1f)
            ) {
                Text(
                    text = "${jobs.size}",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = PolishBlue
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (jobs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Work,
                        contentDescription = null,
                        tint = PolishGrayText,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No hay trabajos activos",
                        color = PolishGrayText,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            jobs.forEach { job ->
                JobDetailCard(
                    job = job,
                    onChat = {
                        job.professionalId?.let { onChatClick(job.id) }
                    },
                    onBids = { onJobClick(job) },
                    onCancel = { onCancelClick(job.id) },
                    onRate = { onRateClick(job.id) },
                    onPay = { onPayClick(job.id) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun PendingRatingSection(
    jobs: List<JobRequest>,
    onRateClick: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Por calificar",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = PolishDark
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFF9800).copy(alpha = 0.1f)
            ) {
                Text(
                    text = "${jobs.size}",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF9800)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (jobs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Todos tus trabajos están calificados",
                    color = PolishGrayText,
                    fontSize = 14.sp
                )
            }
        } else {
            jobs.forEach { job ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onRateClick(job.id) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = job.title ?: job.category,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PolishDark
                            )
                            Text(
                                text = "Califica a ${job.professionalName ?: "el profesional"}",
                                fontSize = 13.sp,
                                color = PolishGrayText
                            )
                        }
                        Icon(
                            Icons.Default.ArrowForwardIos,
                            contentDescription = null,
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun CompletedJobsSection(
    jobs: List<JobRequest>
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Trabajos completados",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = PolishDark
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = PolishGreen.copy(alpha = 0.1f)
            ) {
                Text(
                    text = "${jobs.size}",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = PolishGreen
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (jobs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = PolishGrayText,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Aún no has completado trabajos",
                        color = PolishGrayText,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            jobs.take(5).forEach { job ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = PolishCard),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = job.title ?: job.category,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PolishDark
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFFC107),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = String.format("%.1f", job.clientRatingOfPro ?: 0f),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PolishDark
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "con ${job.professionalName ?: "Profesional"}",
                            fontSize = 13.sp,
                            color = PolishGrayText
                        )
                        val fp = job.finalPrice
                        if (fp != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatCurrency(fp),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = PolishGreen
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun ChatScreen(
    jobId: String,
    messages: List<ChatMessage>,
    currentUserId: String?,
    onSendMessage: (String) -> Unit,
    onBack: () -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    val listState = rememberScrollState()

    LaunchedEffect(messages.size) {
        listState.animateScrollTo(listState.maxValue)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PolishBg)
    ) {
        Surface(
            color = PolishCard,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
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
                Text(
                    text = "Chat",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = PolishDark,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(listState)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            messages.forEach { message ->
                val isCurrentUser = message.senderId == currentUserId
                ChatBubble(
                    message = message,
                    isCurrentUser = isCurrentUser
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Chat,
                            contentDescription = null,
                            tint = PolishGrayText,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Inicia la conversación",
                            color = PolishGrayText,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }

        Surface(
            color = PolishCard,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it.capitalizeFirstLetter() },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Escribe un mensaje...") },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PolishBlue,
                        unfocusedBorderColor = PolishBorder,
                        focusedContainerColor = PolishBg,
                        unfocusedContainerColor = PolishBg
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            onSendMessage(messageText)
                            messageText = ""
                        }
                    },
                    enabled = messageText.isNotBlank()
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Enviar",
                        tint = if (messageText.isNotBlank()) PolishBlue else PolishGrayText
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubble(
    message: ChatMessage,
    isCurrentUser: Boolean
) {
    val bubbleColor = if (isCurrentUser) PolishBlue else PolishCard
    val textColor = if (isCurrentUser) Color.White else PolishDark
    val alignment = if (isCurrentUser) Alignment.CenterEnd else Alignment.CenterStart

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 280.dp),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isCurrentUser) 16.dp else 4.dp,
                bottomEnd = if (isCurrentUser) 4.dp else 16.dp
            ),
            color = bubbleColor,
            shadowElevation = 1.dp
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                if (!isCurrentUser) {
                    Text(
                        text = message.senderName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PolishBlue
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
                Text(
                    text = message.text,
                    fontSize = 14.sp,
                    color = textColor,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatTimestamp(message.timestamp),
                    fontSize = 10.sp,
                    color = if (isCurrentUser) Color.White.copy(alpha = 0.7f) else PolishGrayText,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun CounterOfferDialog(
    currentAmount: Double?,
    onDismiss: () -> Unit,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onCounterOffer: (Double) -> Unit
) {
    var counterAmount by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PolishCard,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = "Contraoferta",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = PolishDark
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                if (currentAmount != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PolishIce
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Money,
                                contentDescription = null,
                                tint = PolishBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Monto propuesto",
                                    fontSize = 12.sp,
                                    color = PolishGrayText
                                )
                                Text(
                                    text = formatCurrency(currentAmount),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PolishBlue
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Tu contraoferta",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = PolishDark
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = counterAmount,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() || it == '.' }) {
                            counterAmount = newValue
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Monto en COP") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PolishBlue,
                        unfocusedBorderColor = PolishBorder
                    )
                )
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val amount = counterAmount.toDoubleOrNull()
                        if (amount != null && amount > 0) {
                            onCounterOffer(amount)
                        }
                    },
                    enabled = (counterAmount.toDoubleOrNull() ?: 0.0) > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = PolishBlue),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Enviar",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        dismissButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onReject,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE53935)),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFE53935))
                    )
                ) {
                    Text(text = "Rechazar")
                }
                OutlinedButton(
                    onClick = onAccept,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PolishGreen),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(PolishGreen)
                    )
                ) {
                    Text(text = "Aceptar")
                }
            }
        }
    )
}

@Composable
fun BidCard(
    bid: Bid,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onCounterOffer: () -> Unit,
    onChat: () -> Unit
) {
    val photoModel = remember(bid.professionalPhoto) { decodePhotoModel(bid.professionalPhoto) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = PolishCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (photoModel != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(photoModel)
                            .crossfade(true)
                            .build(),
                        contentDescription = bid.professionalName,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .border(3.dp, PolishBlue, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(PolishIce)
                            .border(3.dp, PolishBlue, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = PolishBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = bid.professionalName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = PolishDark
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format("%.1f", bid.professionalRating),
                            fontSize = 13.sp,
                            color = PolishGrayText
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatCurrency(bid.amount),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PolishBlue
                    )
                    if (bid.durationHours != null) {
                        Text(
                            text = "${bid.durationHours}h estimadas",
                            fontSize = 12.sp,
                            color = PolishGrayText
                        )
                    }
                }
            }

            if (bid.comment != null && bid.comment.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = bid.comment,
                    fontSize = 13.sp,
                    color = PolishGrayText,
                    lineHeight = 18.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            val lcoAmount = bid.lastCounterOfferAmount
            if (lcoAmount != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFFFF3E0)
                ) {
                    Text(
                        text = "Contraoferta: ${formatCurrency(lcoAmount)}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        color = Color(0xFFE65100),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (bid.status.uppercase()) {
                "PENDING", "PENDIENTE" -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onReject,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE53935)),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFE53935))
                            )
                        ) {
                            Text(text = "Rechazar", fontSize = 13.sp)
                        }
                        OutlinedButton(
                            onClick = onCounterOffer,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF9800)),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFFF9800))
                            )
                        ) {
                            Text(text = "Contraoferta", fontSize = 13.sp)
                        }
                        Button(
                            onClick = onAccept,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = PolishGreen),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Aceptar",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
                "ACCEPTED", "ACEPTADO" -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onChat,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Chat,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Chatear", fontSize = 13.sp)
                        }
                    }
                }
                "REJECTED", "RECHAZADA" -> {
                    Text(
                        text = "Oferta rechazada",
                        fontSize = 13.sp,
                        color = Color(0xFFE53935),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfessionalPortfolioSheet(
    professionalName: String,
    portfolioItems: List<Pair<String, String>>,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PolishCard,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .padding(horizontal = 140.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(PolishBorder)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Portafolio",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = PolishDark
                    )
                    Text(
                        text = professionalName,
                        fontSize = 14.sp,
                        color = PolishGrayText
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = PolishGrayText
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (portfolioItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = PolishGrayText,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No hay trabajos en el portafolio",
                            color = PolishGrayText,
                            fontSize = 16.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp)
                ) {
                    items(portfolioItems) { (title, description) ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = PolishBg)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PolishDark
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = description,
                                    fontSize = 13.sp,
                                    color = PolishGrayText,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun JobHistorySection(
    jobs: List<JobRequest>,
    onJobClick: (JobRequest) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Historial de trabajos",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = PolishDark
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (jobs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Receipt,
                        contentDescription = null,
                        tint = PolishGrayText,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Sin historial de trabajos",
                        color = PolishGrayText,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            jobs.forEach { job ->
                JobHistoryItem(
                    job = job,
                    onClick = { onJobClick(job) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun JobHistoryItem(
    job: JobRequest,
    onClick: () -> Unit
) {
    val statusColor = getStatusColor(job.status)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = PolishCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryIconCircle(
                category = job.category,
                size = 40.dp,
                iconSize = 20.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = job.title ?: job.category,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PolishDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = job.professionalName ?: "Sin asignar",
                    fontSize = 13.sp,
                    color = PolishGrayText
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = getStatusText(job.status),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 11.sp,
                        color = statusColor,
                        fontWeight = FontWeight.Medium
                    )
                }
                val fp2 = job.finalPrice
                if (fp2 != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatCurrency(fp2),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PolishDark
                    )
                }
            }
        }
    }
}

@Composable
fun PaymentDialog(
    jobId: String,
    amount: Double?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var paymentMethod by remember { mutableStateOf("card") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PolishCard,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = "Confirmar pago",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = PolishDark
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                if (amount != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PolishIce
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Total a pagar",
                                fontSize = 14.sp,
                                color = PolishGrayText
                            )
                            Text(
                                text = formatCurrency(amount),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = PolishBlue
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Método de pago",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = PolishDark
                )
                Spacer(modifier = Modifier.height(12.dp))

                PaymentMethodOption(
                    title = "Tarjeta de crédito/débito",
                    icon = Icons.Default.Payment,
                    selected = paymentMethod == "card",
                    onClick = { paymentMethod = "card" }
                )
                Spacer(modifier = Modifier.height(8.dp))
                PaymentMethodOption(
                    title = "PSE",
                    icon = Icons.Default.Store,
                    selected = paymentMethod == "pse",
                    onClick = { paymentMethod = "pse" }
                )
                Spacer(modifier = Modifier.height(8.dp))
                PaymentMethodOption(
                    title = "Efectivo",
                    icon = Icons.Default.Money,
                    selected = paymentMethod == "cash",
                    onClick = { paymentMethod = "cash" }
                )
                Spacer(modifier = Modifier.height(8.dp))
                PaymentMethodOption(
                    title = "Saldo en billetera",
                    icon = Icons.Default.Payment,
                    selected = paymentMethod == "wallet",
                    onClick = { paymentMethod = "wallet" }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(paymentMethod) },
                colors = ButtonDefaults.buttonColors(containerColor = PolishGreen),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Pagar ahora",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancelar",
                    color = PolishGrayText
                )
            }
        }
    )
}

@Composable
fun PaymentMethodOption(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = if (selected) PolishBlue.copy(alpha = 0.08f) else Color.Transparent,
        border = if (selected) androidx.compose.foundation.BorderStroke(2.dp, PolishBlue) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) PolishBlue else PolishGrayText,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                color = if (selected) PolishBlue else PolishDark,
                modifier = Modifier.weight(1f)
            )
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = PolishBlue,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PolishGrayText.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = PolishDark,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = PolishGrayText,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            if (actionText != null && onAction != null) {
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = PolishBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = actionText,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color = PolishBlue
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = PolishCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = PolishDark
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = PolishGrayText
            )
        }
    }
}

@Composable
fun ErrorSnackBar(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFE53935),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                fontSize = 14.sp,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Cerrar",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun SuccessSnackBar(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        color = PolishGreen,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                fontSize = 14.sp,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Cerrar",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun ProfessionalSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onFilterClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = {
                Text("Buscar profesionales...")
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Buscar",
                    tint = PolishGrayText
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Limpiar",
                            tint = PolishGrayText
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PolishBlue,
                unfocusedBorderColor = PolishBorder,
                focusedContainerColor = PolishCard,
                unfocusedContainerColor = PolishCard
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            onClick = onFilterClick,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(PolishCard)
        ) {
            Icon(
                Icons.Default.FilterList,
                contentDescription = "Filtros",
                tint = PolishBlue
            )
        }
    }
}

@Composable
fun CategoryFilterSection(
    categories: List<String>,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Categorías",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = PolishDark
            )
            if (selectedCategory != null) {
                TextButton(onClick = { onCategorySelected(null) }) {
                    Text(
                        text = "Limpiar",
                        fontSize = 13.sp,
                        color = PolishBlue
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { onCategorySelected(null) },
                    label = { Text("Todas") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PolishBlue,
                        selectedLabelColor = Color.White
                    )
                )
            }
            items(categories) { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = {
                        onCategorySelected(if (selectedCategory == category) null else category)
                    },
                    label = { Text(category) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = getCategoryColor(category),
                        selectedLabelColor = Color.White
                    )
                )
            }
        }
    }
}

@Composable
fun ProfessionalGridCard(
    professional: ProfessionalModel,
    onClick: () -> Unit
) {
    val tagColor = getCategoryColor(professional.workCategory ?: "")
    val photoModel = remember(professional.photoUrl) { decodePhotoModel(professional.photoUrl) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = PolishCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box {
                if (photoModel != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(photoModel)
                            .crossfade(true)
                            .build(),
                        contentDescription = professional.name,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .border(3.dp, PolishBlue, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(PolishIce)
                            .border(3.dp, PolishBlue, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = PolishBlue,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
                if (professional.isOnline) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(PolishGreen)
                            .border(2.dp, PolishCard, CircleShape)
                            .align(Alignment.BottomEnd)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = professional.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = PolishDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (professional.workCategory != null) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = tagColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = professional.workCategory,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 11.sp,
                        color = tagColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFC107),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = String.format("%.1f", professional.rating),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = PolishDark
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "(${professional.serviceCount})",
                    fontSize = 12.sp,
                    color = PolishGrayText
                )
            }
        }
    }
}

@Composable
fun WeatherLocationHeader(
    location: String?
) {
    if (location != null && location.isNotEmpty()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.LocationOn,
                contentDescription = null,
                tint = PolishBlue,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = location,
                fontSize = 13.sp,
                color = PolishGrayText
            )
        }
    }
}

@Composable
fun JobStatsOverview(jobs: List<JobRequest>) {
    val activeCount = jobs.count {
        it.status.uppercase() in listOf("ACCEPTED", "ACEPTADO", "IN_PROGRESS", "EN_PROGRESO", "NEGOTIATING", "NEGOCIANDO")
    }
    val completedCount = jobs.count {
        it.status.uppercase() in listOf("COMPLETED", "COMPLETADO")
    }
    val pendingCount = jobs.count {
        it.status.uppercase() in listOf("OPEN", "ABIERTO", "PENDING", "PENDIENTE")
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(
            label = "Activos",
            value = "$activeCount",
            icon = Icons.Default.Work,
            color = PolishBlue
        )
        StatCard(
            label = "Completados",
            value = "$completedCount",
            icon = Icons.Default.Check,
            color = PolishGreen
        )
        StatCard(
            label = "Pendientes",
            value = "$pendingCount",
            icon = Icons.Default.Receipt,
            color = Color(0xFFFF9800)
        )
    }
}

@Composable
fun QuickActionsRow(
    onPostJob: () -> Unit,
    onSearchPro: () -> Unit,
    onWallet: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier
                .weight(1f)
                .clickable { onPostJob() },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = PolishBlue)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Publicar",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Card(
            modifier = Modifier
                .weight(1f)
                .clickable { onSearchPro() },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = PolishGreen)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Buscar",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Card(
            modifier = Modifier
                .weight(1f)
                .clickable { onWallet() },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFF9800))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Payment,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Billetera",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun JobRequestFormStep(
    step: Int,
    totalSteps: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalSteps) { index ->
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        if (index <= step) PolishBlue else PolishBorder
                    )
            )
            if (index < totalSteps - 1) {
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(2.dp)
                        .background(
                            if (index < step) PolishBlue else PolishBorder
                        )
                )
            }
        }
    }
}

@Composable
fun ImageCaptureSection(
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    capturedImages: List<Uri>,
    onRemoveImage: (Uri) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Fotos del problema",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = PolishDark
        )
        Text(
            text = "Opcional - Ayuda al profesional a entender mejor",
            fontSize = 12.sp,
            color = PolishGrayText
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onCameraClick,
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Cámara", fontSize = 13.sp)
            }
            OutlinedButton(
                onClick = onGalleryClick,
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Galería", fontSize = 13.sp)
            }
        }

        if (capturedImages.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(capturedImages) { uri ->
                    Box(modifier = Modifier.size(80.dp)) {
                        AsyncImage(
                            model = uri,
                            contentDescription = "Imagen capturada",
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { onRemoveImage(uri) },
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.TopEnd)
                                .background(
                                    Color.Black.copy(alpha = 0.5f),
                                    CircleShape
                                )
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Eliminar",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OnlineStatusDot(isOnline: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(if (isOnline) PolishGreen else PolishBorder)
            .border(2.dp, PolishCard, CircleShape)
    )
}

@Composable
fun CategoryTag(
    category: String,
    modifier: Modifier = Modifier
) {
    ServiceCategoryTag(category = category, modifier = modifier)
}

@Composable
fun RatingStars(
    rating: Float,
    modifier: Modifier = Modifier,
    starSize: Int = 16
) {
    Row(modifier = modifier) {
        repeat(5) { index ->
            Icon(
                imageVector = if (index < rating.toInt()) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = null,
                tint = Color(0xFFFFC107),
                modifier = Modifier.size(starSize.dp)
            )
        }
    }
}

@Composable
fun ReviewItem(
    rating: Float,
    text: String,
    reviewerName: String,
    timestamp: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RatingStars(rating = rating, starSize = 14)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = reviewerName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = PolishDark
                )
            }
            Text(
                text = timestamp,
                fontSize = 11.sp,
                color = PolishGrayText
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = text,
            fontSize = 13.sp,
            color = PolishGrayText,
            lineHeight = 18.sp
        )
    }
}

@Composable
fun PriceRangeDisplay(
    minPrice: Double,
    maxPrice: Double
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = PolishIce
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Money,
                contentDescription = null,
                tint = PolishBlue,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${formatCurrency(minPrice)} - ${formatCurrency(maxPrice)}",
                fontSize = 13.sp,
                color = PolishBlue,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    count: Int? = null,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = PolishDark
            )
            if (count != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = PolishBlue.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "$count",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PolishBlue
                    )
                }
            }
        }
        if (actionText != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(
                    text = actionText,
                    fontSize = 14.sp,
                    color = PolishBlue
                )
            }
        }
    }
}

@Composable
fun ProfessionalAboutSection(
    description: String,
    specialties: List<String>,
    memberSince: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Sobre mí",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = PolishDark
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            fontSize = 14.sp,
            color = PolishGrayText,
            lineHeight = 20.sp
        )

        if (specialties.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Especialidades",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = PolishDark
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                specialties.forEach { specialty ->
                    CategoryTag(category = specialty)
                }
            }
        }

        if (memberSince.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Store,
                    contentDescription = null,
                    tint = PolishGrayText,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Miembro desde $memberSince",
                    fontSize = 13.sp,
                    color = PolishGrayText
                )
            }
        }
    }
}

@Composable
fun ProfessionalStatsRow(
    rating: Float,
    reviewCount: Int,
    completedJobs: Int,
    location: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFC107),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = String.format("%.1f", rating),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = PolishDark
                )
            }
            Text(
                text = "$reviewCount reseñas",
                fontSize = 12.sp,
                color = PolishGrayText
            )
        }

        Box(
            modifier = Modifier
                .width(1.dp)
                .height(40.dp)
                .background(PolishBorder)
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$completedJobs",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = PolishDark
            )
            Text(
                text = "Trabajos",
                fontSize = 12.sp,
                color = PolishGrayText
            )
        }

        Box(
            modifier = Modifier
                .width(1.dp)
                .height(40.dp)
                .background(PolishBorder)
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = PolishGrayText,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = location,
                    fontSize = 13.sp,
                    color = PolishDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "Ubicación",
                fontSize = 12.sp,
                color = PolishGrayText
            )
        }
    }
}

@Composable
fun ProfessionalPricingSection(
    hourlyRate: Double?,
    negotiable: Boolean = true
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Tarifas",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = PolishDark
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Money,
                    contentDescription = null,
                    tint = PolishBlue,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tarifa por hora",
                    fontSize = 14.sp,
                    color = PolishGrayText
                )
            }
            Text(
                text = if (hourlyRate != null) formatCurrency(hourlyRate) else "A convenir",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = PolishBlue
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Balance,
                    contentDescription = null,
                    tint = PolishGreen,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Negociación",
                    fontSize = 14.sp,
                    color = PolishGrayText
                )
            }
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (negotiable) PolishGreen.copy(alpha = 0.1f) else Color(0xFFFFF3E0)
            ) {
                Text(
                    text = if (negotiable) "Disponible" else "No disponible",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (negotiable) PolishGreen else Color(0xFFE65100)
                )
            }
        }
    }
}

@Composable
fun ProfessionalReviewsSection(
    reviews: List<Pair<Float, String>>,
    totalRating: Float,
    totalReviews: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Reseñas",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = PolishDark
        )
        Spacer(modifier = Modifier.height(12.dp))

        val distribution = getRatingDistribution(reviews)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = String.format("%.1f", totalRating),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = PolishDark
                )
                RatingStars(rating = totalRating, starSize = 18)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$totalReviews reseñas",
                    fontSize = 13.sp,
                    color = PolishGrayText
                )
            }

            Column(
                modifier = Modifier.width(180.dp)
            ) {
                for (i in 5 downTo 1) {
                    val count = distribution[i] ?: 0
                    val fraction = if (totalReviews > 0) count.toFloat() / totalReviews else 0f
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$i",
                            fontSize = 12.sp,
                            color = PolishGrayText,
                            modifier = Modifier.width(12.dp)
                        )
                        LinearProgressIndicator(
                            progress = { fraction },
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFFFFC107),
                            trackColor = PolishBorder
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$count",
                            fontSize = 12.sp,
                            color = PolishGrayText,
                            modifier = Modifier.width(20.dp),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val sampleReviews = listOf(
            Pair(5f, "Excelente trabajo, muy profesional y puntual. Lo recomiendo ampliamente."),
            Pair(4f, "Buen servicio, quedé satisfecho con el resultado. Volveré a contratarlo."),
            Pair(5f, "Muy recomendable, resolvería el problema rápidamente y con buena calidad.")
        )

        sampleReviews.forEach { (rating, text) ->
            ReviewItem(
                rating = rating,
                text = text,
                reviewerName = "Cliente verificado",
                timestamp = "Hace 2 días"
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun ProfessionalBottomActions(
    onHire: () -> Unit,
    onChat: () -> Unit,
    onViewPortfolio: () -> Unit
) {
    Surface(
        color = PolishCard,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onViewPortfolio,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Portafolio", fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = onChat,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Chatear", fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onHire,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PolishGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Contratar ahora",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
