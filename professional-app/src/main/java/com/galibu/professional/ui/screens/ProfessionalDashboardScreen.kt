package com.galibu.professional.ui.screens

import android.widget.Toast
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.PaddingValues
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.galibu.core.data.model.JobRequest
import com.galibu.core.data.model.UserProfile
import com.galibu.core.data.model.hasMissingRequiredFields
import com.galibu.core.data.model.hasMissingProfessionalFields
import com.galibu.core.data.model.isDirectChat
import com.galibu.core.data.remote.RatingDto
import com.galibu.core.ui.components.CategoryBadge
import com.galibu.core.ui.components.GalibuAlertDialog
import com.galibu.core.ui.components.GalibuAlertType
import com.galibu.core.ui.components.BoldCheckoutDialog
import com.galibu.core.ui.components.GalibuRatingSheet
import com.galibu.core.ui.components.PrimaryContactButton
import com.galibu.core.ui.components.RequestSummaryCard
import com.galibu.core.ui.theme.PolishBlue
import com.galibu.core.ui.theme.PolishIce
import com.galibu.professional.ui.viewmodels.ProfessionalViewModel
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

// Clean premium Light Palette matching the high-fidelity screenshots 
private val proBg = Color(0xFFF8FAFC)      // Ultra-clean light gray/blue page background
private val proCardBg = Color(0xFFFFFFFF)  // Solid crisp white cards
private val proPrimary = Color(0xFF0066FF)  // Eye-catching brand blue
private val proTextMain = Color(0xFF0F172A) // Sleek slate near-black primary texts
private val proTextSec = Color(0xFF64748B)  // Muted gray support texts
private val proBorder = Color(0xFFE2E8F0)   // Light gray borders for dividers
private val proGreenStyle = Color(0xFF16A34A)// Active/Verified emerald green
private val proRedStyle = Color(0xFFDC2626)  // Warning/Critical red
private val proOrangeStyle = Color(0xFFEAB308)// Gold Star Rating yellow

data class ProDisplayJob(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val budgetMin: Double,
    val duration: Int,
    val address: String,
    val clientName: String,
    val clientId: String = "",
    val isDemo: Boolean = false,
    val isHighPriority: Boolean = false,
    val timeAgo: String = "Hace 5 min",
    val createdAt: Long = 0L,
    val distance: String = "1.2 km",
    val clientRating: String = "4.9",
    val isDirectHire: Boolean = false,
    val counterOfferAmount: Double? = null,
    val counterOfferSender: String? = null,
    val proHourlyRate: Double? = null,
    val status: String = "NEGOTIATING",
    val suggestedPrice: Double? = null,
    val proRatingOfClient: Float? = null,
    val clientRatingOfPro: Float? = null,
    val proReviewOfClient: String? = null,
    val clientReviewOfPro: String? = null,
    val municipality: String = "",
    val department: String = "",
    val source: String? = null,
    val photos: String? = null
)

fun formatDate(timestamp: Long): String {
    val ts = if (timestamp < 1_000_000_000_000L) timestamp * 1000 else timestamp
    val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(ts))
}

fun formatTimeAgo(timestamp: Long): String {
    // Normaliza segundos vs milisegundos y calcula en cada composición.
    val ts = if (timestamp < 1_000_000_000_000L) timestamp * 1000 else timestamp
    val diff = (System.currentTimeMillis() - ts).coerceAtLeast(0)
    if (diff < 0) return "Hace un momento"
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    val weeks = days / 7
    val months = days / 30

    return when {
        minutes < 1 -> "Hace un momento"
        minutes < 60 -> "Hace $minutes min"
        hours < 24 -> "Hace $hours ${if (hours == 1L) "hora" else "horas"}"
        days < 7 -> "Hace $days ${if (days == 1L) "día" else "días"}"
        weeks < 4 -> "Hace $weeks ${if (weeks == 1L) "semana" else "semanas"}"
        else -> "Hace $months ${if (months == 1L) "mes" else "meses"}"
    }
}

sealed class ProfessionalScreenState {
    object Loading : ProfessionalScreenState()
    data class Success(
        val currentUser: UserProfile,
        val openJobs: List<JobRequest>,
        val allJobs: List<JobRequest>
    ) : ProfessionalScreenState()
    data class Error(val message: String) : ProfessionalScreenState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfessionalDashboardScreen(
    viewModel: ProfessionalViewModel,
    onNavigateToVerification: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    onNavigateToNegotiation: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val openJobs by viewModel.openJobs.collectAsState()
    val allJobs by viewModel.allJobs.collectAsState()
    val jobIdsWithBids by viewModel.jobIdsWithBids.collectAsState()
    val proBids by viewModel.proBids.collectAsState()
    val notifications by viewModel.notifications.collectAsState(initial = emptyList())
    val allMessages by viewModel.allChatMessages.collectAsState()
    val boldCheckout by viewModel.boldCheckout.collectAsState()

    // Escucha en tiempo real de mis chats activos (vistas previas y no leídos del tab Mensajes).
    // Incluye abiertas donde escribí antes de ofertar/aceptar.
    val myChatJobIds = remember(allJobs, allMessages, currentUser) {
        val sentIds = allMessages.filter { it.senderId == currentUser?.id }.map { it.jobId }.toSet()
        allJobs.filter {
            (it.professionalId == currentUser?.id || it.id in sentIds) && !it.isDirectChat() &&
                it.status.uppercase() !in listOf("COMPLETED", "COMPLETADO", "CANCELLED", "CANCELADO")
        }.map { it.id }
    }
    DisposableEffect(myChatJobIds) {
        val regs = if (myChatJobIds.isNotEmpty()) viewModel.listenToChats(myChatJobIds) else emptyList()
        onDispose { viewModel.stopListeningToChats(regs) }
    }

    LaunchedEffect(myChatJobIds) {
        if (myChatJobIds.isEmpty()) return@LaunchedEffect
        while (true) {
            viewModel.refreshChatsForJobs(myChatJobIds)
            kotlinx.coroutines.delay(5000)
        }
    }

    // Conversaciones activas (las mismas que muestra el tab Mensajes).
    val activeChatJobIds = remember(allJobs, allMessages, currentUser) {
        val sentIds = allMessages.filter { it.senderId == currentUser?.id }.map { it.jobId }.toSet()
        allJobs.filter { job ->
            (job.professionalId == currentUser?.id || job.id in sentIds) &&
                job.status.uppercase() !in listOf("COMPLETED", "COMPLETADO", "CANCELLED", "CANCELADO")
        }.map { it.id }.toSet()
    }

    // Contador del menú: solo mensajes sin leer de conversaciones activas.
    // Antes sumaba mensajes de servicios completados/cancelados y quedaba en rojo
    // aunque no hubiera conversaciones en pantalla.
    val unreadChatCount = remember(allMessages, activeChatJobIds, currentUser) {
        allMessages.count {
            it.jobId in activeChatJobIds && !it.read && it.senderId != currentUser?.id
        }
    }

    val scope = rememberCoroutineScope()
    var showNotificationsDialog by rememberSaveable { mutableStateOf(false) }

    // Re-chequeo periódico de mis solicitudes: detecta oferta aceptada,
    // cliente que finaliza/califica y servicio completado (campanita + push).
    // Permiso de notificaciones del sistema (Android 13+) para las push de eventos.
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

    // Screen State Centralized
    val screenState = remember(currentUser, openJobs, allJobs) {
        val user = currentUser
        if (user == null) {
            ProfessionalScreenState.Loading
        } else {
            ProfessionalScreenState.Success(user, openJobs, allJobs)
        }
    }

    // 5 main navigation tabs requested by user: "trabajos", "mi wallet", "mensajes", "historial", "perfil"
    var selectedTab by rememberSaveable { mutableStateOf("Inicio") }
    // El subformulario de Perfil (editar/foto/clave) oculta el menú inferior.
    var hideBottomBar by rememberSaveable { mutableStateOf(false) }

    // Re-chequeo periódico de mis solicitudes: solo mientras el usuario está
    // en la pestaña "Profesionales" (donde se muestran los jobs). Se detiene
    // automáticamente al salir de esa pestaña o de la pantalla.
    LaunchedEffect(currentUser?.id, selectedTab) {
        if (currentUser?.id.isNullOrEmpty()) return@LaunchedEffect
        if (selectedTab != "Profesionales") return@LaunchedEffect
        viewModel.refreshMyJobs()
        while (true) {
            delay(60_000)
            try {
                viewModel.refreshMyJobs()
            } catch (e: Exception) {}
        }
    }

    // Completado forzoso tras Google: ir a Perfil, abrir edición y bloquear
    // el resto hasta guardar todos los campos obligatorios.
    val requireCompletion by viewModel.requireProfileCompletion.collectAsState()
    LaunchedEffect(requireCompletion, currentUser) {
        val user = currentUser
        if (user != null && (user.hasMissingRequiredFields() || user.hasMissingProfessionalFields())) {
            viewModel.setRequireProfileCompletion(true)
        }
        if (requireCompletion) {
            selectedTab = "Perfil"
            if (user != null && !user.hasMissingRequiredFields() && !user.hasMissingProfessionalFields()) {
                viewModel.setRequireProfileCompletion(false)
            }
        }
    }

    var isAvailable by rememberSaveable { mutableStateOf(true) }
    var dismissedJobIds by rememberSaveable { mutableStateOf(emptySet<String>()) }

    // Dialog trigger states
    var showBidDialog by remember { mutableStateOf(false) }
    var selectedJobForBid by remember { mutableStateOf<ProDisplayJob?>(null) }

    var showRechargeDialog by remember { mutableStateOf(false) }

    var selectedJobForRating by remember { mutableStateOf<JobRequest?>(null) }
    var rechargeAmountInput by remember { mutableStateOf("10000") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Real jobs from Firestore matching professional specifications
    val proCategory = currentUser?.workCategory ?: ""
    val proMuni = currentUser?.municipality ?: ""
    val proDept = currentUser?.department ?: ""

    // Filter: OPEN jobs + NEGOTIATING jobs where professional has a bid
    val availableJobsForPro = remember(openJobs, allJobs, jobIdsWithBids, currentUser) {
        val openJobsList = openJobs.filter { job ->
            val notCreatedByMe = job.clientId != (currentUser?.id ?: "")
            val notAssignedToMe = (job.professionalId ?: "") != (currentUser?.id ?: "")
            notCreatedByMe && notAssignedToMe
        }
        
        // Add NEGOTIATING jobs where this professional has an active bid
        val negotiatingWithMyBids = allJobs.filter { job ->
            job.status == "NEGOTIATING" && 
            job.id in jobIdsWithBids && 
            job.professionalId.isNullOrEmpty() && // Not yet assigned
            job.clientId != (currentUser?.id ?: "")
        }
        
        (openJobsList + negotiatingWithMyBids).distinctBy { it.id }
    }

    val filteredRealJobs = availableJobsForPro

    val realMappedJobs = remember(filteredRealJobs, currentUser) {
        filteredRealJobs.map { job ->
            val isHigh = job.budgetMin >= 30000
            val distText = "${String.format(Locale.US, "%.1f", 1.0 + (job.id.hashCode() % 10) * 0.4)} km"
            val crText = "${4.5 + (job.id.hashCode() % 5) * 0.1}"
            ProDisplayJob(
                id = job.id,
                title = job.title,
                description = job.description,
                category = job.category,
                budgetMin = job.budgetMin,
                duration = 2,
                address = job.address,
                clientName = job.clientName,
                clientId = job.clientId,
                isDemo = false,
                isHighPriority = isHigh,
                timeAgo = formatTimeAgo(job.createdAt),
                distance = distText,
                clientRating = crText,
                status = job.status,
                createdAt = job.createdAt,
                suggestedPrice = job.suggestedPrice,
                proRatingOfClient = job.proRatingOfClient,
                clientRatingOfPro = job.clientRatingOfPro,
                proReviewOfClient = job.proReviewOfClient,
                clientReviewOfPro = job.clientReviewOfPro,
                municipality = job.municipality.orEmpty(),
                department = job.department.orEmpty(),
                source = job.source,
                photos = job.photos,
                proHourlyRate = job.proHourlyRate ?: currentUser?.hourlyRate
            )
        }
    }

    // Combined active jobs filtered out if dismissed
    val combinedJobs = remember(realMappedJobs, dismissedJobIds) {
        realMappedJobs.filter { it.id !in dismissedJobIds }
    }

    val matchingJobs = remember(combinedJobs) {
        combinedJobs
    }
    
    val otherJobs = remember(combinedJobs, matchingJobs) {
        emptyList<ProDisplayJob>()
    }

    // Direct hiring requests from clients (direct-chat threads are not solicitudes).
    // Incluye mi trabajo asignado activo (ACCEPTED/IN_PROGRESS): su hogar es esta
    // sección, no el marketplace (que excluye lo asignado a mí).
    val directRealJobs = remember(allJobs, currentUser) {
        allJobs.filter { job ->
            val status = job.status.uppercase()
            val activeStatus = status in listOf(
                "OPEN", "NEGOTIATING", "NEGOCIANDO", "ACCEPTED", "ACEPTADO",
                "IN_PROGRESS", "EN_PROGRESO", "PENDING_RATING", "PENDIENTE_CALIFICACION"
            )
            // Si el cliente ya finalizó pero el profesional aún no calificó,
            // la solicitud debe seguir visible en Inicio para poder cerrarla.
            val completedPendingMyRating = status in listOf("COMPLETED", "COMPLETADO") &&
                job.proRatingOfClient == null

            !job.isDirectChat() &&
                job.professionalId == currentUser?.id &&
                (activeStatus || completedPendingMyRating)
        }
    }

    val directMappedJobs = remember(directRealJobs, currentUser) {
        directRealJobs.map { job ->
            val isHigh = job.budgetMin >= 30000
            val distText = "${String.format(Locale.US, "%.1f", 1.0 + (job.id.hashCode() % 10) * 0.4)} km"
            val crText = "${4.5 + (job.id.hashCode() % 5) * 0.1}"
            ProDisplayJob(
                id = job.id,
                title = job.title,
                description = job.description,
                category = job.category,
                budgetMin = job.budgetMin,
                duration = 2,
                address = job.address,
                clientName = job.clientName,
                clientId = job.clientId,
                isDemo = false,
                isHighPriority = isHigh,
                timeAgo = "Contratación Directa 🎯",
                distance = distText,
                clientRating = crText,
                isDirectHire = true,
                counterOfferAmount = job.counterOfferAmount,
                counterOfferSender = job.counterOfferSender,
                proHourlyRate = job.proHourlyRate ?: currentUser?.hourlyRate,
                suggestedPrice = job.suggestedPrice,
                createdAt = job.createdAt,
                status = job.status,
                proRatingOfClient = job.proRatingOfClient,
                clientRatingOfPro = job.clientRatingOfPro,
                proReviewOfClient = job.proReviewOfClient,
                clientReviewOfPro = job.clientReviewOfPro,
                source = job.source,
                photos = job.photos
            )
        }
    }

    // Client Profile popup/dialog states
    var selectedClientIdForProfile by remember { mutableStateOf<String?>(null) }
    var selectedClientProfile by remember { mutableStateOf<UserProfile?>(null) }
    var selectedJobForProfileDetails by remember { mutableStateOf<ProDisplayJob?>(null) }
    var isLoadingClientProfile by remember { mutableStateOf(false) }
    var clientDetailCounterOfferAmount by remember { mutableStateOf("") }
    var showDetailCounterInline by remember { mutableStateOf(false) }
    var showFirstOfferInline by remember { mutableStateOf(false) }
    var showProRatingSheet by remember { mutableStateOf(false) }
    var clientRatings by remember { mutableStateOf<List<RatingDto>>(emptyList()) }
    var showClientRatingsHistory by remember { mutableStateOf(false) }

    LaunchedEffect(selectedClientIdForProfile, selectedJobForProfileDetails?.id) {
        clientDetailCounterOfferAmount = ""
        showDetailCounterInline = false
        showFirstOfferInline = false
        val clientId = selectedClientIdForProfile
        if (clientId != null && clientId.isNotEmpty()) {
            isLoadingClientProfile = true
            var user = viewModel.repository.getUserById(clientId)
            if (user == null) {
                // Let's create a robust fallback client profile matching the user's name on the card
                val matchedJob = (combinedJobs + directMappedJobs).find { it.clientId == clientId }
                val clientName = matchedJob?.clientName ?: "Juan Francisco Builes"
                val clientAddr = matchedJob?.address ?: "Cl. 51 #62-25, Rionegro"
                user = UserProfile(
                    id = clientId,
                    name = clientName,
                    email = "${clientId}@galibu.com",
                    phone = "3123456789",
                    role = "CLIENT",
                    rating = 4.9f,
                    completedJobs = 32,
                    municipality = "Rionegro",
                    department = "Antioquia",
                    address = clientAddr,
                    profilePhotoUrl = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150&h=150&fit=crop",
                    createdAt = System.currentTimeMillis() - 86400000L * 400
                )
                viewModel.repository.updateUserDirectly(user)
            } else if (user.profilePhotoUrl.isNullOrEmpty()) {
                // Ensure profilePhotoUrl is never empty for clients
                user = user.copy(profilePhotoUrl = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150&h=150&fit=crop")
                viewModel.repository.updateUserDirectly(user)
            }
            selectedClientProfile = user
            clientRatings = viewModel.repository.getUserRatings(clientId)
            selectedJobForProfileDetails?.let { job ->
                val defaultAmount = (job.counterOfferAmount ?: job.budgetMin).toInt().toString()
                clientDetailCounterOfferAmount = com.galibu.core.ui.utils.PriceUtils.formatThousands(defaultAmount)
            }
            isLoadingClientProfile = false
        } else {
            selectedClientProfile = null
        }
    }

    // Real historical active jobs
    val professionalActiveJobs = remember(allJobs, currentUser, jobIdsWithBids) {
        allJobs.filter { job ->
            val isAssignedPro = job.professionalId == currentUser?.id
            val isBidder = job.id in jobIdsWithBids
            val proId = job.professionalId
            val isAssignedToOther = proId != null && proId.isNotBlank() && proId != currentUser?.id
            
            val isMyJob = isAssignedPro || (isBidder && !isAssignedToOther)
            
            !job.isDirectChat() && isMyJob && (job.status == "OPEN" || job.status == "ACCEPTED" || job.status == "IN_PROGRESS" || job.status == "COMPLETED" || job.status == "NEGOTIATING" || job.status == "PENDING_RATING")
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(proBg)
    ) {
        when (val state = screenState) {
            is ProfessionalScreenState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = proPrimary)
                }
            }
            is ProfessionalScreenState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.message,
                        color = Color.Red,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
            is ProfessionalScreenState.Success -> {
                // MAIN SCROLLABLE CONTENT BODY
                Column(
                    modifier = if (selectedTab == "Perfil") {
                        Modifier
                            .fillMaxSize()
                            .padding(bottom = if (hideBottomBar) 0.dp else 90.dp)
                    } else {
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 90.dp) // Leave exact space for bottom menu
                    }
                ) {
            when (selectedTab) {
                "Inicio" -> {
                    val firstName = remember(currentUser?.name) {
                        currentUser?.name?.trim()?.substringBefore(" ")?.ifBlank { "Usuario" } ?: "Usuario"
                    }
                    // --- HEADER BLOCK (homologado con el inicio del cliente) ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "¡Hola, $firstName!",
                                    color = proTextSec,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color(0xFF16A34A).copy(alpha = 0.1f))
                                        .padding(horizontal = 12.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = "Profesionales",
                                        color = Color(0xFF16A34A),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Solicitudes",
                                color = proTextMain,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Gestiona las solicitudes de servicio\nde tus clientes.",
                                color = proTextSec,
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                            if (combinedJobs.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(30.dp))
                                        .background(proPrimary.copy(alpha = 0.1f))
                                        .border(1.dp, proPrimary.copy(alpha = 0.2f), RoundedCornerShape(30.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(proPrimary)
                                        )
                                        Text(
                                            text = "${combinedJobs.size} ${if (combinedJobs.size == 1) "nueva" else "nuevas"}",
                                            color = proPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Column(
                            modifier = Modifier.align(Alignment.Top),
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Bell notification icon (igual que el cliente)
                            val unreadRealCount = notifications.count { !it.isRead }
                            Box {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFF1F5F9))
                                        .clickable { showNotificationsDialog = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Notifications,
                                        contentDescription = "Notificaciones",
                                        tint = Color(0xFF0F172A),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                if (unreadRealCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFE53935))
                                            .align(Alignment.TopEnd),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (unreadRealCount > 9) "9+" else unreadRealCount.toString(),
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            // Dynamic Balance Pill showing $150,000 COP or active balance, Clicking hops to Wallet Tab
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(30.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(Color(0xFF10B981), Color(0xFF059669))
                                        )
                                    )
                                    .clickable { selectedTab = "Mi Wallet" }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                                    .shadow(1.dp, RoundedCornerShape(30.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = "Wallet",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "$${NumberFormat.getNumberInstance(Locale.US).format(currentUser?.balance ?: 150000.0)}",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    // --- VERIFICATION DEMAND BANNER ---
                    if (false) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(12.dp))
                                .clickable { onNavigateToVerification() }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Warning",
                                    tint = proRedStyle,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Validación requerida de identidad 🛡️",
                                        color = proTextMain,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Valida tu documento de identidad para recibir clientes de forma segura.",
                                        color = proTextSec,
                                        fontSize = 11.sp
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Verificar",
                                    tint = proPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // --- DIRECT HIRING REQUESTS ---
                    if (directMappedJobs.isNotEmpty()) {
                        Text(
                            text = "SOLICITUDES DIRECTAS",
                            color = proPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                        directMappedJobs.forEach { job ->
                            JobRequestItem(
                                job = job,
                                isVerified = currentUser?.isVerified == true,
                                balance = currentUser?.balance ?: 150000.0,
                                hasBid = job.id in jobIdsWithBids,
                                onAccept = {
                                    scope.launch {
                                        try {
                                            viewModel.acceptDirectHire(job.id) {
                                                viewModel.showAlert("Aviso", "¡Aceptaste la contratación directa!")
                                            }
                                        } catch (e: Exception) {
                                            viewModel.showAlert("Aviso", "Error: ${e.message}")
                                        }
                                    }
                                },
                                onContraofertar = { proposedRate ->
                                    scope.launch {
                                        try {
                                            viewModel.submitCounterOfferDirectHire(job.id, proposedRate, isClient = false) {
                                                val formattedRate = com.galibu.core.ui.utils.PriceUtils.formatThousands(proposedRate.toInt().toString())
                                                viewModel.showAlert("Aviso", "¡Contraoferta de $$formattedRate COP enviada!")
                                            }
                                        } catch (e: Exception) {
                                            viewModel.showAlert("Aviso", "Error: ${e.message}")
                                        }
                                    }
                                },
                                onIgnore = {
                                    scope.launch {
                                        try {
                                            viewModel.rejectDirectHire(job.id) {
                                                viewModel.showAlert("Aviso", "Contratación declinada")
                                            }
                                        } catch (e: Exception) {
                                            viewModel.showAlert("Aviso", "Error: ${e.message}")
                                        }
                                    }
                                },
                                onNavigateToChat = {
                                    viewModel.selectJob(job.id)
                                    onNavigateToChat(job.id)
                                },
                                onClientClick = { clickedId ->
                                    selectedClientIdForProfile = clickedId
                                    selectedJobForProfileDetails = job
                                },
                                onFinalize = {
                                    val realJob = allJobs.find { it.id == job.id }
                                    viewModel.payAndCompleteJob(job.id, "Transferencia / Nequi") {
                                        selectedJobForRating = realJob
                                        viewModel.showAlert("Aviso", "¡Servicio finalizado con éxito! Califica al cliente.")
                                    }
                                },
                                onRateClient = {
                                    val realJob = allJobs.find { it.id == job.id }
                                    selectedJobForRating = realJob
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // --- LIST OF JOB REQUEST REQUISITIONS ---
                    // Trabajos completados esperando calificación del cliente: arriba del todo.
                    val jobsPendingRating = remember(allJobs, currentUser) {
                        allJobs.filter {
                            it.professionalId == currentUser?.id &&
                                it.status.uppercase() in listOf("COMPLETED", "COMPLETADO") &&
                                it.proRatingOfClient != null &&
                                it.clientRatingOfPro == null
                        }
                    }

                    jobsPendingRating.forEach { job ->
                        AcceptedServiceYellowBanner(
                            job = job
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Display Solicitudes cards separated in two categories if any are OPEN/available
                    if (combinedJobs.isNotEmpty()) {
                        if (matchingJobs.isNotEmpty()) {
                            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp)) {
                                Text(
                                    text = "Clientes que requieren tu servicio",
                                    color = proTextMain,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Nuevas solicitudes",
                                    color = proTextSec,
                                    fontSize = 13.sp
                                )
                            }
                            matchingJobs.forEach { job ->
                                val myBid = remember(proBids, job.id) { proBids.find { it.jobId == job.id } }
                                JobRequestItem(
                                    job = job,
                                    isVerified = currentUser?.isVerified == true,
                                    balance = currentUser?.balance ?: 150000.0,
                                    hasBid = job.id in jobIdsWithBids,
                                    myBid = myBid,
                                    onAcceptBidCounter = {
                                        myBid?.let { bid -> viewModel.acceptBid(bid.id) }
                                    },
                                    onProposeBidCounter = { counterVal ->
                                        myBid?.let { bid -> viewModel.proposeCounterOffer(bid.id, counterVal) }
                                    },
                                    onRejectBidCounter = {
                                        myBid?.let { bid -> viewModel.rejectBid(bid.id) }
                                    },
                                    onAccept = {
                                        if (currentUser?.isVerified != true) {
                                            viewModel.showAlert("Aviso", "Valida tu identidad de profesional primero")
                                            onNavigateToVerification()
                                        } else if ((currentUser?.balance ?: 150000.0) <= 0.0) {
                                            showRechargeDialog = true
                                        } else {
                                            viewModel.submitBid(job.id, job.budgetMin, "Acepto tus condiciones sugeridas.", job.duration)
                                        }
                                    },
                                    onContraofertar = { proposedRate ->
                                        selectedJobForBid = job
                                        showBidDialog = true
                                    },
                                    onIgnore = {
                                        dismissedJobIds = dismissedJobIds + job.id
                                        viewModel.showAlert("Aviso", "Servicio archivado")
                                    },
                                    onNavigateToChat = {
                                        viewModel.selectJob(job.id)
                                        onNavigateToChat(job.id)
                                    },
                                    onClientClick = { clickedId ->
                                        selectedClientIdForProfile = clickedId
                                        selectedJobForProfileDetails = job
                                    },
                                    onFinalize = {
                                        val realJob = allJobs.find { it.id == job.id }
                                        viewModel.payAndCompleteJob(job.id, "Transferencia / Nequi") {
                                            selectedJobForRating = realJob
                                            viewModel.showAlert("Aviso", "¡Servicio finalizado con éxito! Califica al cliente.")
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }

                        if (otherJobs.isNotEmpty()) {
                            Text(
                                text = "Servicios disponibles",
                                color = proTextMain,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp)
                            )
                            otherJobs.forEach { job ->
                                val myBid = remember(proBids, job.id) { proBids.find { it.jobId == job.id } }
                                JobRequestItem(
                                    job = job,
                                    isVerified = currentUser?.isVerified == true,
                                    balance = currentUser?.balance ?: 150000.0,
                                    hasBid = job.id in jobIdsWithBids,
                                    myBid = myBid,
                                    onAcceptBidCounter = {
                                        myBid?.let { bid -> viewModel.acceptBid(bid.id) }
                                    },
                                    onProposeBidCounter = { counterVal ->
                                        myBid?.let { bid -> viewModel.proposeCounterOffer(bid.id, counterVal) }
                                    },
                                    onRejectBidCounter = {
                                        myBid?.let { bid -> viewModel.rejectBid(bid.id) }
                                    },
                                    onAccept = {
                                        if (currentUser?.isVerified != true) {
                                            viewModel.showAlert("Aviso", "Valida tu identidad de profesional primero")
                                            onNavigateToVerification()
                                        } else if ((currentUser?.balance ?: 150000.0) <= 0.0) {
                                            showRechargeDialog = true
                                        } else {
                                            viewModel.submitBid(job.id, job.budgetMin, "Acepto tus condiciones sugeridas.", job.duration)
                                        }
                                    },
                                    onContraofertar = { proposedRate ->
                                        selectedJobForBid = job
                                        showBidDialog = true
                                    },
                                    onIgnore = {
                                        dismissedJobIds = dismissedJobIds + job.id
                                        viewModel.showAlert("Aviso", "Servicio archivado")
                                    },
                                    onNavigateToChat = {
                                        viewModel.selectJob(job.id)
                                        onNavigateToChat(job.id)
                                    },
                                    onClientClick = { clickedId ->
                                        selectedClientIdForProfile = clickedId
                                        selectedJobForProfileDetails = job
                                    },
                                    onFinalize = {
                                        val realJob = allJobs.find { it.id == job.id }
                                        viewModel.payAndCompleteJob(job.id, "Transferencia / Nequi") {
                                            selectedJobForRating = realJob
                                            viewModel.showAlert("Aviso", "¡Servicio finalizado con éxito! Califica al cliente.")
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .clickable { selectedTab = "Historial" },
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Undo,
                                    contentDescription = null,
                                    tint = proPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Ver solicitudes anteriores",
                                    color = proPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    } else if (directMappedJobs.isEmpty()) {
                        // Only show seeking state if there are neither requests nor active ones
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 50.dp, horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.HourglassEmpty,
                                contentDescription = null,
                                tint = proTextSec,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Buscando requerimientos...",
                                color = proTextMain,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            val locationLabel = remember(currentUser) {
                                val muni = currentUser?.municipality ?: "Bogotá"
                                val dept = currentUser?.department ?: "Bogotá"
                                if (muni.equals(dept, ignoreCase = true) || dept.contains(muni, ignoreCase = true)) {
                                    "Atendiendo $muni y alrededores."
                                } else {
                                    "Atendiendo $muni, $dept y alrededores."
                                }
                            }
                            Text(
                                text = locationLabel,
                                color = proTextSec,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }
                }

                "Mi Wallet" -> {
                    // --- MI WALLET VIEW (Matching Image 2) ---
                    LaunchedEffect(Unit) { viewModel.refreshWallet() }
                    val walletTx by viewModel.walletHistory.collectAsState()
                    val boldOrders by viewModel.boldOrders.collectAsState()
                    val myCompleted = remember(allJobs, currentUser) {
                        allJobs.filter { it.professionalId == currentUser?.id && it.status == "COMPLETED" }
                    }
                    val monthStartMs = remember {
                        java.util.Calendar.getInstance().apply {
                            set(java.util.Calendar.DAY_OF_MONTH, 1)
                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                            set(java.util.Calendar.MINUTE, 0)
                            set(java.util.Calendar.SECOND, 0)
                            set(java.util.Calendar.MILLISECOND, 0)
                        }.timeInMillis
                    }
                    val monthEarnings = myCompleted.filter { it.createdAt >= monthStartMs }.sumOf { it.finalPrice ?: 0.0 }
                    val totalEarnings = myCompleted.sumOf { it.finalPrice ?: 0.0 }
                    val acceptCount = remember(allJobs, currentUser) {
                        allJobs.count { it.professionalId == currentUser?.id && it.status in listOf("ACCEPTED", "IN_PROGRESS", "COMPLETED") }
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Mi Wallet",
                            color = proTextMain,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Gestiona tu saldo para aceptar trabajos",
                            color = proTextSec,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 1.dp)
                        )

                        // Big blue gradient active card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(6.dp, RoundedCornerShape(20.dp))
                        ) {
                            Column(
                                modifier = Modifier
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color(0xFF2563EB), Color(0xFF1D4ED8))
                                        )
                                    )
                                    .padding(24.dp)
                            ) {
                                Text(
                                    text = "Saldo disponible",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "$${NumberFormat.getNumberInstance(Locale.US).format(currentUser?.balance ?: 150000.0)}",
                                    color = Color.White,
                                    fontSize = 38.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(modifier = Modifier.height(18.dp))
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "Costo por aceptación: $3.000",
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val leftAttempts = ((currentUser?.balance ?: 150000.0) / 3000).toInt()
                                    Text(
                                        text = "Capacidad restante: $leftAttempts aceptaciones",
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                }
                            }
                        }

                        // Recharge Button (Solid brand blue)
                        Button(
                            onClick = { showRechargeDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = proPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .shadow(2.dp, RoundedCornerShape(14.dp)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Recharge",
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Recargar saldo",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Stats Row of 3 Cards
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Col 1: Este mes
                            Card(
                                colors = CardDefaults.cardColors(containerColor = proCardBg),
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.dp, proBorder, RoundedCornerShape(12.dp)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = null,
                                        tint = proRedStyle,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "$${com.galibu.core.ui.utils.PriceUtils.formatThousands(monthEarnings.toLong().toString())}",
                                        color = proTextMain,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Este mes",
                                        color = proTextSec,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            // Col 2: Ingresos
                            Card(
                                colors = CardDefaults.cardColors(containerColor = proCardBg),
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.dp, proBorder, RoundedCornerShape(12.dp)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = null,
                                        tint = proGreenStyle,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "$${com.galibu.core.ui.utils.PriceUtils.formatThousands(totalEarnings.toLong().toString())}",
                                        color = proTextMain,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Ingresos",
                                        color = proTextSec,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            // Col 3: Aceptaciones
                            Card(
                                colors = CardDefaults.cardColors(containerColor = proCardBg),
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.dp, proBorder, RoundedCornerShape(12.dp)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = proPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "$acceptCount",
                                        color = proTextMain,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Aceptaciones",
                                        color = proTextSec,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Movements block
                        Text(
                            text = "Movimientos",
                            color = proTextMain,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        // Movimientos reales del backend (recargas y tarifas de aceptación).
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val pendingOrders = boldOrders.filter { it.status != "PAID" }
                            // Combinar pendientes y completados en una sola lista ordenada
                            // por fecha descendente (más reciente primero).
                            data class MovementItem(val timestamp: Long, val isPending: Boolean, val order: com.galibu.core.data.remote.BoldOrderDto? = null, val tx: com.galibu.core.data.remote.WalletTransactionDto? = null)
                            val allMovements = (pendingOrders.map { MovementItem(it.createdAt, true, it, null) } +
                                walletTx.map { MovementItem(it.createdAt, false, null, it) })
                                .sortedByDescending { it.timestamp }
                            if (walletTx.isEmpty() && pendingOrders.isEmpty()) {
                                Text(
                                    text = "Sin movimientos aún. Recarga saldo para empezar a aceptar trabajos.",
                                    color = proTextSec,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            } else {
                                allMovements.forEach { movement ->
                                    if (movement.isPending) {
                                        val order = movement.order!!
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(proCardBg)
                                                .border(1.dp, proBorder, RoundedCornerShape(12.dp))
                                                .padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFFFF7ED)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Schedule,
                                                    contentDescription = null,
                                                    tint = Color(0xFFF59E0B),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Recarga pendiente",
                                                    color = proTextMain,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "$${com.galibu.core.ui.utils.PriceUtils.formatThousands(order.amount.toLong().toString())} COP · ${order.status} · ${formatDate(order.createdAt)}",
                                                    color = proTextSec,
                                                    fontSize = 11.sp
                                                )
                                            }
                                            Text(
                                                text = "Pendiente",
                                                color = Color(0xFFF59E0B),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    } else {
                                        val tx = movement.tx!!
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(proCardBg)
                                                .border(1.dp, proBorder, RoundedCornerShape(12.dp))
                                                .padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val isRecharge = tx.type == "RECHARGE"
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isRecharge) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (isRecharge) Icons.Default.Check else Icons.Default.ArrowOutward,
                                                    contentDescription = null,
                                                    tint = if (isRecharge) proGreenStyle else proRedStyle,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = if (isRecharge) "Recarga de saldo" else (tx.description?.takeIf { it.isNotBlank() } ?: "Aceptación de servicio"),
                                                    color = proTextMain,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${formatDate(tx.createdAt)} · ${formatTimeAgo(tx.createdAt)}",
                                                    color = proTextSec,
                                                    fontSize = 11.sp
                                                )
                                            }
                                            Text(
                                                text = (if (tx.amount < 0) "-$" else "+$") + com.galibu.core.ui.utils.PriceUtils.formatThousands(kotlin.math.abs(tx.amount).toLong().toString()),
                                                color = if (isRecharge) proGreenStyle else proRedStyle,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 15.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                "Mensajes" -> {
                    // --- MENSAJES VIEW ---
                    // Solo solicitudes activas: las cerradas no generan conversación.
                    // Incluye jobs donde escribí aunque aún no me asignen (pre-oferta).
                    val activeChatJobs = remember(allJobs, activeChatJobIds) {
                        allJobs.filter { it.id in activeChatJobIds }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Mensajes",
                            color = proTextMain,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (activeChatJobs.isNotEmpty()) {
                            Text(
                                text = "${activeChatJobs.size} conversaciones activas",
                                color = proTextSec,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(top = 1.dp)
                            )
                        }

                        if (activeChatJobs.isEmpty()) {
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
                                        .background(proPrimary.copy(alpha = 0.05f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Chat,
                                        contentDescription = null,
                                        tint = proPrimary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "Sin conversaciones activas",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = proTextMain
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Aquí aparecerán tus chats presenciales con clientes.",
                                    fontSize = 13.sp,
                                    color = proTextSec,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        } else {
                            activeChatJobs.forEach { jobItem ->
                                val initials = (jobItem.clientName ?: "Cliente").trim().take(2).uppercase()
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = proCardBg),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, proBorder, RoundedCornerShape(16.dp))
                                        .clickable {
                                            viewModel.selectJob(jobItem.id)
                                            onNavigateToChat(jobItem.id)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Avatar with initials
                                        Box(
                                            modifier = Modifier
                                                .size(50.dp)
                                                .clip(CircleShape)
                                                .background(proPrimary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = initials,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            val itemUnread = remember(allMessages, jobItem.id, currentUser) {
                                                allMessages.count { it.jobId == jobItem.id && !it.read && it.senderId != currentUser?.id }
                                            }
                                            val itemLastMsg = remember(allMessages, jobItem.id) {
                                                allMessages.filter { it.jobId == jobItem.id }.maxByOrNull { it.timestamp }
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = jobItem.clientName ?: "Cliente",
                                                    color = proTextMain,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                if (itemUnread > 0) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(20.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0xFFE53935)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = if (itemUnread > 9) "9+" else "$itemUnread",
                                                            color = Color.White,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                } else {
                                                    Text(
                                                        text = "Activo",
                                                        color = proGreenStyle,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = itemLastMsg?.text?.takeIf { it.isNotBlank() } ?: jobItem.title,
                                                color = if (itemUnread > 0) proTextMain else proTextSec,
                                                fontSize = 13.sp,
                                                fontWeight = if (itemUnread > 0) FontWeight.SemiBold else FontWeight.Normal,
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

                "Historial" -> {
                    // --- HISTORIAL VIEW: calificaciones recibidas ---
                    var historyRatings by remember { mutableStateOf<List<RatingDto>>(emptyList()) }
                    LaunchedEffect(currentUser?.id) {
                        val uid = currentUser?.id ?: return@LaunchedEffect
                        try { historyRatings = viewModel.repository.getUserRatings(uid) } catch (e: Exception) { historyRatings = emptyList() }
                    }
                    val totalRatings = historyRatings.size
                    val avgRating = if (historyRatings.isNotEmpty()) historyRatings.map { it.stars }.average() else 0.0
                    val last20Ratings = historyRatings.take(20)
                    val dateFmt = remember { java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale("es", "CO")) }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Historial",
                            color = proTextMain,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$totalRatings calificaciones recibidas",
                            color = proTextSec,
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = proCardBg),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Promedio", fontSize = 12.sp, color = proTextSec, fontWeight = FontWeight.Medium)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(String.format(Locale.US, "%.1f", avgRating), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = proTextMain)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row {
                                        (1..5).forEach { i ->
                                            Icon(Icons.Default.Star, contentDescription = null, tint = if (i <= avgRating.toInt()) Color(0xFFF59E0B) else Color(0xFFE2E8F0), modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                            Card(
                                colors = CardDefaults.cardColors(containerColor = proCardBg),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Calificaciones", fontSize = 12.sp, color = proTextSec, fontWeight = FontWeight.Medium)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("$totalRatings", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = proTextMain)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("comentarios", fontSize = 12.sp, color = proTextSec)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "ÚLTIMAS CALIFICACIONES",
                            color = proTextSec,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (last20Ratings.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 80.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(proPrimary.copy(alpha = 0.05f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = proPrimary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "Aún no tienes calificaciones",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = proTextMain
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Tus calificaciones aparecerán aquí",
                                    color = proTextSec,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            last20Ratings.forEach { rating ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = proCardBg),
                                    shape = RoundedCornerShape(14.dp),
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
                                                color = proTextSec
                                            )
                                        }
                                        if (!rating.comment.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = rating.comment ?: "",
                                                fontSize = 13.sp,
                                                color = proTextMain
                                            )
                                        }
                                    }
                                }
                            }
                            if (historyRatings.size > 20) {
                                Text(
                                    text = "Mostrando las 20 más recientes de $totalRatings",
                                    fontSize = 12.sp,
                                    color = proTextSec,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                "Perfil" -> {
                    ProfileScreen(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize(),
                        onNavigateToNotifications = { showNotificationsDialog = true },
                        forceEdit = requireCompletion,
                        onSubScreenOpen = { hideBottomBar = it }
                    )
                }
            }
        }

        // --- FIXED PREMIUM NAVIGATION BAR (Bottom Menu requested by user) ---
        // Oculto en subformularios de Perfil (tienen su flecha de atrás).
        if (selectedTab != "Perfil" || !hideBottomBar) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .border(2.dp, Color(0xFFE2E8F0), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ícono relleno cuando la pestaña está seleccionada y de contorno cuando no
                // (convención actual de Material 3). Pair = (seleccionado, no seleccionado).
                listOf(
                    Triple("Inicio", Icons.Default.Home to Icons.Outlined.Home, "Inicio"),
                    Triple("Mi Wallet", Icons.Default.AccountBalanceWallet to Icons.Outlined.AccountBalanceWallet, "Mi Wallet"),
                    Triple("Mensajes", Icons.Default.ChatBubble to Icons.Outlined.ChatBubble, "Mensajes"),
                    Triple("Historial", Icons.Default.History to Icons.Outlined.History, "Historial"),
                    Triple("Perfil", Icons.Default.Person to Icons.Outlined.Person, "Perfil")
                ).forEach { (label, iconPair, value) ->
                    val isSelected = selectedTab == value
                    val icon = if (isSelected) iconPair.first else iconPair.second
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { if (!requireCompletion) selectedTab = value }
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Tiny top blue line for selected tab (Matching Image 1)
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .width(28.dp)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(proPrimary)
                            )
                        } else {
                            Spacer(modifier = Modifier.height(3.dp))
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Icono homologado con cliente + contador legible dentro del tab.
                        Box(
                            modifier = Modifier.size(30.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (isSelected) proPrimary else proTextSec,
                                modifier = Modifier.size(24.dp)
                            )
                            // Red count indicator with real unread messages inside Mensajes
                            if (label == "Mensajes" && unreadChatCount > 0) {
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
                                        text = if (unreadChatCount > 9) "9+" else "$unreadChatCount",
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
                            text = label,
                            color = if (isSelected) proPrimary else proTextSec,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        }

    // Modal Bid Proposal Dialog (Contraofertar) -> shared CounterOfferSheet
    if (showBidDialog && selectedJobForBid != null) {
        val job = selectedJobForBid!!
        com.galibu.core.ui.components.CounterOfferSheet(
            offerLabel = "Oferta del cliente:",
            descriptionText = "Ingresa un valor negociado inferior o superior. El cliente decidirá de forma inmediata.",
            currentOffer = job.counterOfferAmount ?: job.budgetMin,
            category = job.category,
            city = job.municipality.takeIf { it.isNotBlank() } ?: "Bogotá",
            initialAmount = job.counterOfferAmount ?: job.budgetMin,
            onDismiss = { showBidDialog = false },
            onSubmit = { amount ->
                if (amount < 5417.0) {
                    viewModel.showAlert("Aviso", "La tarifa es muy baja. El salario mínimo legal por hora en Colombia es de $5.417 COP.")
                } else {
                    val hours = job.duration
                    viewModel.submitBid(job.id, amount, "Ofrezco servicio especializado de calidad.", hours)
                    showBidDialog = false
                }
            }
        )
    }

    // Hoja inferior para recargar la billetera.
    if (showRechargeDialog) {
        ModalBottomSheet(
            onDismissRequest = { showRechargeDialog = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = proCardBg,
            dragHandle = { BottomSheetDefaults.DragHandle(color = proBorder) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
                    .navigationBarsPadding()
                    .imePadding()
                    .testTag("recharge_bottom_sheet")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(proPrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = proPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Recargar saldo",
                            fontWeight = FontWeight.Bold,
                            color = proTextMain,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "Pago seguro con Bold",
                            color = proTextSec,
                            fontSize = 12.sp
                        )
                    }
                    IconButton(onClick = { showRechargeDialog = false }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = proTextSec
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Para poder ofertar tarifas y atender servicios de clientes, debes contar con saldo recargado. Los servicios consumen una pequeña tarifa de conexión (3.000 COP).",
                    color = proTextSec,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "El pago se procesa en la pasarela segura de Bold. Puedes pagar con tarjeta, PSE, Nequi o Botón Bancolombia.",
                    color = proPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )

                if ((currentUser?.balance ?: 150000.0) <= 0.0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "⚠️ Tienes saldo insuficiente para ofertar. ¡Recarga ahora!",
                            color = proRedStyle,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "Selecciona un monto",
                    color = proTextMain,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("10000", "20000", "50000", "100000").forEach { preset ->
                        val isChosen = rechargeAmountInput == preset
                        Button(
                            onClick = { rechargeAmountInput = preset },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isChosen) proPrimary else Color(0xFFF1F5F9),
                                contentColor = if (isChosen) Color.White else proTextMain
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("$$preset", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = com.galibu.core.ui.utils.PriceUtils.formatThousands(rechargeAmountInput),
                    onValueChange = { input ->
                        val clean = input.filter { it.isDigit() }
                        if (clean.length <= 9) {
                            rechargeAmountInput = clean
                        }
                    },
                    label = { Text("Monto Personalizado (COP)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = proTextMain,
                        unfocusedTextColor = proTextMain,
                        focusedBorderColor = proPrimary,
                        unfocusedBorderColor = proBorder,
                        focusedLabelColor = proPrimary,
                        unfocusedLabelColor = proTextSec
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("recharge_input_field")
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Mínimo \$1.000 · Máximo \$5.000.000 COP",
                    color = proTextSec,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = {
                        val amount = rechargeAmountInput.toDoubleOrNull()
                        if (amount == null || amount <= 0.0) {
                            viewModel.showWarning("Ingresa un monto válido para recargar.")
                        } else {
                            showRechargeDialog = false
                            viewModel.startBoldRecharge(amount)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = proPrimary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("dialog_recharge_confirm")
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pagar con Bold",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 15.sp
                    )
                }
                TextButton(
                    onClick = { showRechargeDialog = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Volver", color = proTextSec)
                }
            }
        }
    }

    // Checkout de Bold (recarga de la billetera del profesional).
    boldCheckout?.let { checkout ->
        BoldCheckoutDialog(
            url = checkout.checkoutUrl,
            onClose = { viewModel.closeBoldCheckout() },
            onPaymentReturn = { viewModel.confirmBoldPayment() }
        )
    }

    // Calificación del cliente: hoja homologada en todas las pantallas.
    if (selectedJobForRating != null) {
        val ratingJob = selectedJobForRating!!
        val clientName = ratingJob.clientName
        GalibuRatingSheet(
            title = "Calificar al cliente",
            targetName = clientName.ifBlank { "el cliente" },
            onDismissRequest = { selectedJobForRating = null },
            onSubmit = { rating, comment ->
                viewModel.submitRating(
                    jobId = ratingJob.id,
                    rating = rating.toFloat(),
                    review = comment,
                    isClientRating = false,
                    onSuccess = {
                        viewModel.showAlert("Aviso", "Calificaste a $clientName con $rating estrellas. ¡Gracias!")
                    }
                )
                selectedJobForProfileDetails = selectedJobForProfileDetails?.copy(
                    proRatingOfClient = rating.toFloat(),
                    proReviewOfClient = comment
                )
                selectedJobForRating = null
            }
        )
    }

    if (showDeleteDialog) {
        GalibuAlertDialog(
            title = "¿Eliminar tu Cuenta de Galibu?",
            message = "Esta acción es irreversible y eliminará todo tu historial de servicios, tus datos de contacto y tu billetera de saldo. ¿Realmente deseas continuar?",
            type = GalibuAlertType.ERROR,
            confirmText = "Eliminar",
            dismissText = "Cancelar",
            confirmColor = Color(0xFFEF4444),
            onConfirm = {
                viewModel.deleteAccount()
                showDeleteDialog = false
            },
            onDismissRequest = { showDeleteDialog = false }
        )
    }

    if (selectedClientProfile != null) {
        val client = selectedClientProfile!!
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(proBg)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { selectedClientIdForProfile = null },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = proTextMain,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Solicitud de servicio",
                            color = proTextMain,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = selectedJobForProfileDetails?.category ?: "",
                            color = proTextSec,
                            fontSize = 13.sp
                        )
                    }
                }

                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {
                    // Client Card (tap para ver calificaciones y reseñas reales)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showClientRatingsHistory = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val initials = client.name.trim().split(" ").take(2).map { it.take(1) }.joinToString("").uppercase()
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .border(3.dp, PolishBlue, CircleShape)
                                    .background(PolishIce),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!client.profilePhotoUrl.isNullOrEmpty()) {
                                    val clientPhotoModel = remember(client.profilePhotoUrl) {
                                        com.galibu.core.ui.utils.ImageUtils.getCoilModel(client.profilePhotoUrl)
                                    }
                                    coil.compose.AsyncImage(
                                        model = clientPhotoModel,
                                        contentDescription = "Foto de perfil",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                } else {
                                    Text(
                                        text = initials.ifEmpty { "U" },
                                        color = Color.White,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF22C55E))
                                        .border(2.dp, Color.White, CircleShape)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = client.name,
                                    color = proTextMain,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val ratingsAvg = if (clientRatings.isNotEmpty()) {
                                    clientRatings.map { it.stars }.average().toFloat()
                                } else {
                                    client.rating
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = proOrangeStyle,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = String.format(java.util.Locale.US, "%.1f", ratingsAvg),
                                        color = proTextMain,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (clientRatings.isNotEmpty()) " · ${clientRatings.size} reseñas" else " · Sin reseñas",
                                        color = proTextSec,
                                        fontSize = 14.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Place,
                                        contentDescription = null,
                                        tint = proTextSec,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    val muni = client.municipality?.takeIf { it.isNotBlank() } ?: "Rionegro"
                                    val dept = client.department?.takeIf { it.isNotBlank() } ?: "Antioquia"
                                    Text(
                                        text = if (muni.equals(dept, ignoreCase = true)) muni else "$muni, $dept",
                                        color = proTextSec,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = proTextSec,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Service Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F7FF)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            val jobDetail = selectedJobForProfileDetails
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = jobDetail?.category ?: "Servicio",
                                    color = proTextMain,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = jobDetail?.description?.takeIf { it.isNotBlank() }
                                        ?: jobDetail?.title?.takeIf { it.isNotBlank() }
                                        ?: jobDetail?.category ?: "",
                                    color = proTextSec,
                                    fontSize = 14.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = proTextSec,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = jobDetail?.let { formatTimeAgo(it.createdAt) } ?: "",
                                        color = proTextSec,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = proTextSec,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = jobDetail?.address?.takeIf { it.isNotBlank() } ?: "Sin dirección",
                                        color = proTextSec,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))



                    // Fotos adjuntas por el cliente (opcionales).
                    val jobPhotos = selectedJobForProfileDetails?.photos
                        ?.split("|")
                        ?.map { it.trim() }
                        ?.filter { it.isNotBlank() }
                        ?: emptyList()
                    if (jobPhotos.isNotEmpty()) {
                        Text(
                            text = "Fotos de la solicitud",
                            color = proTextMain,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            jobPhotos.forEach { photo ->
                                AsyncImage(
                                    model = com.galibu.core.ui.utils.ImageUtils.getCoilModel(photo),
                                    contentDescription = "Foto de la solicitud",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Precio de mi oferta + Contactar (homologado sheet cliente)
                    selectedJobForProfileDetails?.let { detailPrice ->
                        val livePriceJob = allJobs.find { it.id == detailPrice.id }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                val detailOffered = (livePriceJob?.suggestedPrice ?: detailPrice.suggestedPrice)?.takeIf { it > 0 }
                                    ?: (livePriceJob?.counterOfferAmount ?: detailPrice.counterOfferAmount)?.takeIf { it > 0 }
                                    ?: (livePriceJob?.budgetMin ?: detailPrice.budgetMin).takeIf { it > 0 }
                                detailOffered?.let { offered ->
                                    Text(
                                        text = "$ ${com.galibu.core.ui.utils.PriceUtils.formatThousands(offered.toLong().toString())} COP / hora",
                                        color = proTextMain,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = "Precio ofertado por el cliente",
                                        color = proTextSec,
                                        fontSize = 13.sp
                                    )
                                } ?: Text(
                                    text = "Sin precio ofertado",
                                    color = proTextSec,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            PrimaryContactButton(
                                onClick = {
                                    viewModel.selectJob(detailPrice.id)
                                    onNavigateToChat(detailPrice.id)
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Offer Card
                    selectedJobForProfileDetails?.let { detail ->
                        val realJob = allJobs.find { it.id == detail.id }
                        val assignedToMe = realJob?.professionalId == currentUser?.id
                        // Usar el estado más reciente: el job en Room se actualiza al
                        // iniciar/finalizar el servicio y este detalle es una copia.
                        val detailStatus = realJob?.status ?: detail.status
                        // Igual con los campos de negociación: la copia se queda vieja
                        // cuando el cliente responde o cuando yo contraoferto.
                        val liveBudgetMin = realJob?.budgetMin ?: detail.budgetMin
                        val liveSuggestedPrice = realJob?.suggestedPrice ?: detail.suggestedPrice
                        val liveCounterAmount = realJob?.counterOfferAmount ?: detail.counterOfferAmount
                        val liveCounterSender = realJob?.counterOfferSender ?: detail.counterOfferSender
                        if (detailStatus == "ACCEPTED" && assignedToMe) {
                            // Aceptado por el cliente: ya no se oferta, se inicia el servicio.
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE9F7EF)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = "Servicio aceptado",
                                        color = proGreenStyle,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "El cliente aceptó tu oferta. Inicia el servicio cuando llegues.",
                                        color = proTextSec,
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = { viewModel.startService(detail.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = proPrimary),
                                        shape = RoundedCornerShape(14.dp),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Iniciar servicio",
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        } else if (detailStatus == "IN_PROGRESS" && assignedToMe) {
                            // En progreso: solo queda finalizar.
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE9F7EF)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = "Servicio en progreso",
                                        color = proGreenStyle,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Finaliza el servicio cuando termines el trabajo.",
                                        color = proTextSec,
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = {
                                            showProRatingSheet = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                        shape = RoundedCornerShape(14.dp),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Finalizar servicio",
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        } else if (detailStatus == "COMPLETED" || detailStatus == "CANCELLED") {
                            // Servicio cerrado: sin botones.
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE9F7EF)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = if (detailStatus == "COMPLETED") "Servicio finalizado" else "Servicio cancelado",
                                        color = proGreenStyle,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Gracias por usar Galibu.",
                                        color = proTextSec,
                                        fontSize = 12.sp
                                    )
                                    if (detailStatus == "COMPLETED" && detail.proRatingOfClient == null) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Button(
                                            onClick = {
                                                allJobs.find { it.id == detail.id }?.let { real ->
                                                    selectedJobForRating = real
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = proPrimary),
                                            shape = RoundedCornerShape(14.dp),
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Calificar al cliente",
                                                color = Color.White,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE9F7EF)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                val receivedBid = remember(proBids, detail.id) {
                                    proBids.filter { it.jobId == detail.id }.maxByOrNull { it.createdAt }
                                }
                                // The client only puts a price on the table via counter-offer
                                // (direct counter touches the job, per-bid counter stamps the bid).
                                // Without it, the professional defines the price with "Enviar oferta".
                                val directClientCounter = liveCounterAmount
                                    ?.takeIf { liveCounterSender == "CLIENT" && it > 0 }
                                val bidClientCounter = receivedBid
                                    ?.takeIf { it.status == "COUNTERED_BY_CLIENT" }
                                    ?.let { (it.lastCounterOfferAmount?.takeIf { v -> v > 0 }) ?: it.amount.takeIf { v -> v > 0 } }
                                val clientOfferAmount = directClientCounter ?: bidClientCounter
                                if (clientOfferAmount != null) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFD9F0DF)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LocalOffer,
                                                contentDescription = null,
                                                tint = proGreenStyle,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Oferta recibida",
                                            color = proGreenStyle,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "$" + com.galibu.core.ui.utils.PriceUtils.formatThousands(clientOfferAmount.toLong().toString()),
                                        color = proTextMain,
                                        fontSize = 30.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    // Aceptar: contratación directa asignada usa accept-direct;
                                    // en negociación abierta se acepta la oferta (el backend ahora
                                    // permite al profesional aceptar su contraoferta del cliente).
                                    // Sin oferta propia no hay nada que aceptar: solo contraofertar.
                                    val isDirectAssigned = detail.isDirectHire && assignedToMe
                                    if (isDirectAssigned || receivedBid != null) {
                                    Button(
                                        onClick = {
                                            if (isDirectAssigned) {
                                                viewModel.acceptDirectHire(detail.id) {
                                                    selectedClientIdForProfile = null
                                                }
                                            } else {
                                                receivedBid?.let {
                                                    viewModel.acceptBid(it.id)
                                                    selectedClientIdForProfile = null
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = proGreenStyle),
                                        shape = RoundedCornerShape(14.dp),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Aceptar oferta",
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    // Inline: tap Contraofertar -> se oculta el botón y aparece input + Enviar oferta
                                    if (!showDetailCounterInline) {
                                        OutlinedButton(
                                            onClick = { showDetailCounterInline = true },
                                            border = BorderStroke(1.dp, Color(0xFFFF9800)),
                                            shape = RoundedCornerShape(14.dp),
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.SwapHoriz,
                                                contentDescription = null,
                                                tint = Color(0xFFFF9800),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Contraofertar",
                                                color = Color(0xFFFF9800),
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    } else {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color.White),
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, proBorder),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                com.galibu.core.ui.components.InlineCounterOfferInput(
                                                    initialAmount = 0.0,
                                                    accentColor = proPrimary,
                                                    onSend = { amount ->
                                                        showDetailCounterInline = false
                                                        if (receivedBid != null && receivedBid.status == "COUNTERED_BY_CLIENT") {
                                                            viewModel.proposeCounterOffer(receivedBid.id, amount)
                                                        } else {
                                                            scope.launch {
                                                                try {
                                                                    viewModel.submitCounterOfferDirectHire(detail.id, amount, isClient = false) {
                                                                        viewModel.showAlert("Aviso", "¡Contraoferta enviada!")
                                                                    }
                                                                } catch (e: Exception) {
                                                                    viewModel.showAlert("Aviso", "Error: ${e.message}")
                                                                }
                                                            }
                                                        }
                                                    },
                                                    onCancel = { showDetailCounterInline = false }
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFD9F0DF)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LocalOffer,
                                                contentDescription = null,
                                                tint = proGreenStyle,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "Tu oferta",
                                                color = proGreenStyle,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Define el precio de tu servicio",
                                                color = proTextSec,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    // Si ya tengo oferta en espera del cliente, no se permite otra
                                    // hasta que acepte o contraoferte.
                                    // En directas la negociación vive en el job (counterOfferSender):
                                    // un bid PENDING suelto no debe bloquear los botones.
                                    val awaitingClient = if (detail.isDirectHire) {
                                        liveCounterSender == "PROFESSIONAL" && (liveCounterAmount ?: 0.0) > 0
                                    } else {
                                        receivedBid != null &&
                                            (receivedBid.status == "PENDING" ||
                                                receivedBid.status == "COUNTERED_BY_PROFESSIONAL")
                                    }
                                    if (awaitingClient) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color(0xFFEFF6FF))
                                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                        ) {
                                            Text(
                                                text = "Oferta enviada, esperando respuesta del cliente",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = proPrimary
                                            )
                                        }
                                    } else if (!showFirstOfferInline) {
                                        val offeredPrice = liveSuggestedPrice?.takeIf { it > 0 }
                                        // Directa sin contraoferta aún: el budgetMin ya es la oferta
                                        // inicial del cliente → se acepta con accept-direct.
                                        val isDirectInitialOffer = offeredPrice == null &&
                                            detail.isDirectHire && assignedToMe && liveBudgetMin > 0
                                        if (offeredPrice != null) {
                                            Button(
                                                onClick = {
                                                    viewModel.submitBid(detail.id, offeredPrice, "Acepto el precio ofertado.", detail.duration)
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = proGreenStyle),
                                                shape = RoundedCornerShape(14.dp),
                                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Aceptar oferta",
                                                    color = Color.White,
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(10.dp))
                                        } else if (isDirectInitialOffer) {
                                            Button(
                                                onClick = {
                                                    viewModel.acceptDirectHire(detail.id) {
                                                        selectedClientIdForProfile = null
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = proGreenStyle),
                                                shape = RoundedCornerShape(14.dp),
                                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Aceptar oferta",
                                                    color = Color.White,
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(10.dp))
                                        }
                                        OutlinedButton(
                                            onClick = { showFirstOfferInline = true },
                                            border = BorderStroke(1.dp, Color(0xFFFF9800)),
                                            shape = RoundedCornerShape(14.dp),
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.SwapHoriz,
                                                contentDescription = null,
                                                tint = Color(0xFFFF9800),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Contraofertar",
                                                color = Color(0xFFFF9800),
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    } else {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color.White),
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, proBorder),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                com.galibu.core.ui.components.InlineCounterOfferInput(
                                                    initialAmount = 0.0,
                                                    accentColor = proPrimary,
                                                    sendLabel = "Enviar contraoferta",
                                                    onSend = { amount ->
                                                        showFirstOfferInline = false
                                                        if (amount < 5417.0) {
                                                            viewModel.showAlert("Aviso", "La tarifa es muy baja. El mínimo legal por hora es de $5.417 COP.")
                                                        } else if (detail.isDirectHire && assignedToMe) {
                                                            // Directa: la contraoferta vive en el job para que el cliente la vea.
                                                            viewModel.submitCounterOfferDirectHire(detail.id, amount, isClient = false) {
                                                                viewModel.showAlert("Aviso", "¡Contraoferta enviada!")
                                                            }
                                                        } else {
                                                            viewModel.submitBid(detail.id, amount, "Ofrezco servicio especializado de calidad.", detail.duration)
                                                        }
                                                    },
                                                    onCancel = { showFirstOfferInline = false }
                                                )
                                            }
                                        }
                                    }
                                }

                                if (detail.isDirectHire && assignedToMe && detailStatus == "NEGOTIATING") {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    HorizontalDivider(color = Color(0xFFD9F0DF))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.rejectDirectHire(detail.id) {
                                                selectedClientIdForProfile = null
                                                selectedJobForProfileDetails = null
                                            }
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                                        border = BorderStroke(1.5.dp, Color(0xFFDC2626)),
                                        shape = RoundedCornerShape(14.dp),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                    ) {
                                        Text(
                                            text = "Rechazar solicitud",
                                            color = Color(0xFFDC2626),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
}
}
    // Calificación del profesional al cliente: se abre al finalizar el servicio.
    // Si se cierra sin calificar no se finaliza nada (se puede reintentar).
    if (showProRatingSheet && selectedJobForProfileDetails != null) {
        val ratingJob = selectedJobForProfileDetails!!
        GalibuRatingSheet(
            title = "Calificar al cliente",
            targetName = ratingJob.clientName.ifBlank { "el cliente" },
            onDismissRequest = { showProRatingSheet = false },
            onSubmit = { rating, comment ->
                // Cerrar la hoja de inmediato: la finalización sigue en segundo
                // plano y el overlay de carga queda visible encima.
                showProRatingSheet = false
                viewModel.finishService(ratingJob.id, rating, comment) {
                    scope.launch {
                        val snap = try {
                            viewModel.repository.getJobSnapshot(ratingJob.id)
                        } catch (e: Exception) {
                            null
                        }
                        if (snap != null && snap.status == "COMPLETED" && snap.clientRatingOfPro != null) {
                            // El cliente ya finalizó y calificó: volver al home.
                            selectedJobForProfileDetails = null
                            selectedClientIdForProfile = null
                            selectedTab = "Inicio"
                            viewModel.showAlert("Aviso", "¡Servicio completado! Gracias por calificar.")
                        } else {
                            // Reflejar el nuevo estado en pantalla sin salir de ella.
                            selectedJobForProfileDetails = selectedJobForProfileDetails?.copy(
                                status = snap?.status ?: "COMPLETED",
                                proRatingOfClient = snap?.proRatingOfClient,
                                clientRatingOfPro = snap?.clientRatingOfPro
                            )
                        }
                    }
                }
            }
        )
    }

    // Historial de calificaciones y reseñas del cliente (vuelve al detalle al cerrar).
    if (showClientRatingsHistory) {
        ModalBottomSheet(
            onDismissRequest = { showClientRatingsHistory = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = "Calificaciones y reseñas",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = proTextMain
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = selectedClientProfile?.name?.takeIf { it.isNotBlank() } ?: "Cliente",
                    color = proTextSec,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                if (clientRatings.isEmpty()) {
                    Text(
                        text = "Aún no tiene reseñas.",
                        color = proTextSec,
                        fontSize = 14.sp
                    )
                } else {
                    clientRatings.forEach { rating ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row {
                                    (1..5).forEach { i ->
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = if (i <= rating.stars) proOrangeStyle else Color(0xFFE2E8F0),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                if (!rating.comment.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "\"${rating.comment}\"",
                                        fontSize = 13.sp,
                                        color = proTextSec,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
    // Pantalla completa de notificaciones (igual que el cliente).
    if (showNotificationsDialog) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            NotificationsAndHistoryScreen(
                viewModel = viewModel,
                onBack = { showNotificationsDialog = false },
                onOpenJob = { jobId ->
                    showNotificationsDialog = false
                    viewModel.markJobNotificationsAsRead(jobId)
                    val displayJob = (combinedJobs + directMappedJobs).find { it.id == jobId }
                    if (displayJob != null) {
                        selectedJobForProfileDetails = displayJob
                        selectedClientIdForProfile = displayJob.clientId
                    }
                }
            )
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobRequestItem(
    job: ProDisplayJob,
    isVerified: Boolean,
    balance: Double,
    onAccept: () -> Unit,
    onContraofertar: (Double) -> Unit,
    onIgnore: () -> Unit,
    onNavigateToChat: () -> Unit,
    onClientClick: (String) -> Unit,
    onFinalize: () -> Unit,
    onRateClient: (() -> Unit)? = null,
    hasBid: Boolean = false,
    myBid: com.galibu.core.data.model.Bid? = null,
    onAcceptBidCounter: (() -> Unit)? = null,
    onProposeBidCounter: ((Double) -> Unit)? = null,
    onRejectBidCounter: (() -> Unit)? = null
) {
    val isBidCountered = myBid != null && myBid.status == "COUNTERED_BY_CLIENT"
    val isBidPending = myBid != null && myBid.status == "COUNTERED_BY_PROFESSIONAL"

    val finalPriceVal = if (isBidCountered || isBidPending) {
        myBid!!.amount
    } else if (job.isDirectHire) {
        job.counterOfferAmount ?: job.budgetMin
    } else {
        job.budgetMin
    }

    val offerLabel = if (isBidCountered) {
        "Cliente Propone"
    } else if (isBidPending) {
        "Tú propusiste"
    } else {
        when (job.counterOfferSender) {
            "CLIENT" -> "Cliente Propone"
            "PROFESSIONAL" -> "Tú propusiste"
            else -> "Ofrece (Cliente)"
        }
    }

    var showCounterOfferSheet by remember { mutableStateOf(false) }
    var inputBidVal by remember(job) { mutableStateOf(finalPriceVal.toInt().toString()) }

    val statusTriple = when (job.status.uppercase()) {
        "IN_PROGRESS", "EN_PROGRESO" -> Triple("En progreso", Color(0xFFDCFCE7), Color(0xFF16A34A))
        "ACCEPTED", "ACEPTADO" -> Triple("Aceptado", Color(0xFFEFF6FF), Color(0xFF2563EB))
        "PENDING_RATING", "POR_CALIFICAR", "PENDIENTE_CALIFICACION" -> if (job.proRatingOfClient == null) {
            Triple("Por calificar", Color(0xFFFFFBEB), Color(0xFFD97706))
        } else {
            Triple("Esperando cierre", Color(0xFFEFF6FF), Color(0xFF2563EB))
        }
        "COMPLETED", "COMPLETADO" -> if (job.proRatingOfClient == null) {
            Triple("Por calificar", Color(0xFFFFFBEB), Color(0xFFD97706))
        } else {
            Triple("Completado", Color(0xFFF1F5F9), Color(0xFF475569))
        }
        "CANCELLED", "CANCELADO" -> Triple("Cancelado", Color(0xFFF1F5F9), Color(0xFF64748B))
        "NEGOTIATING", "NEGOCIANDO" -> Triple("En negociación", Color(0xFFFFFBEB), Color(0xFFD97706))
        else -> Triple("Abierta", Color(0xFFEFF6FF), Color(0xFF2563EB))
    }
    val cardPrice = job.suggestedPrice?.takeIf { it > 0 }
        ?: if (job.isDirectHire || (job.source ?: "PUBLISH") == "DIRECTORY") finalPriceVal.takeIf { it > 0 } else null

    RequestSummaryCard(
        timeAgo = formatTimeAgo(job.createdAt),
        category = job.category,
        statusLabel = statusTriple.first,
        statusBackground = statusTriple.second,
        statusContentColor = statusTriple.third,
        isDirect = (job.source ?: "PUBLISH") == "DIRECTORY",
        title = job.title.ifEmpty { "Servicio de ${job.category}" },
        description = job.description,
        priceText = cardPrice?.let { "$ ${com.galibu.core.ui.utils.PriceUtils.formatThousands(it.toLong().toString())} COP / hora" },
        address = job.address.ifEmpty { "Sin dirección" },
        onClick = { onClientClick(job.clientId) },
        infoContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = proOrangeStyle, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = job.clientRating, color = proTextMain, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "•", color = proTextSec, fontSize = 13.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = job.clientName,
                    color = proTextSec,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    )
}

@Composable
fun ProRadarScanner(
    isAvailable: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isAvailable) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
            shape = RoundedCornerShape(16.dp),
            modifier = modifier
                .height(180.dp)
                .border(1.dp, proBorder, RoundedCornerShape(16.dp))
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE2E8F0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = "Radar inactivo",
                            tint = proTextSec,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Radar en Pausa",
                        color = proTextMain,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Ponte 'On' para recibir alertas activas",
                        color = proTextSec,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        return
    }

    val infiniteTransition = rememberInfiniteTransition(label = "RadarSweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = proCardBg),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .height(180.dp)
            .border(1.dp, proPrimary.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(proGreenStyle)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "RADAR ACTIVO",
                        color = proGreenStyle,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
                Text(
                    text = "Buscando...",
                    color = proTextSec,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val centerOffset = Offset(size.width / 2f, size.height / 2f)
                    val maxRadius = Math.min(size.width, size.height) / 2.1f

                    drawCircle(
                        color = proPrimary.copy(alpha = 0.04f),
                        radius = maxRadius,
                        center = centerOffset
                    )
                    drawCircle(
                        color = proPrimary.copy(alpha = 0.1f),
                        radius = maxRadius,
                        center = centerOffset,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                    )
                    drawCircle(
                        color = proPrimary.copy(alpha = 0.15f),
                        radius = maxRadius * 0.66f,
                        center = centerOffset,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                    )
                    drawCircle(
                        color = proPrimary.copy(alpha = 0.2f),
                        radius = maxRadius * 0.33f,
                        center = centerOffset,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                    )

                    drawLine(
                        color = proBorder,
                        start = Offset(centerOffset.x - maxRadius, centerOffset.y),
                        end = Offset(centerOffset.x + maxRadius, centerOffset.y),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = proBorder,
                        start = Offset(centerOffset.x, centerOffset.y - maxRadius),
                        end = Offset(centerOffset.x, centerOffset.y + maxRadius),
                        strokeWidth = 1.dp.toPx()
                    )

                    val sweepRad = Math.toRadians(sweepAngle.toDouble())
                    val sweepTarget = Offset(
                        (centerOffset.x + maxRadius * Math.cos(sweepRad)).toFloat(),
                        (centerOffset.y + maxRadius * Math.sin(sweepRad)).toFloat()
                    )
                    drawLine(
                        color = proPrimary.copy(alpha = 0.8f),
                        start = centerOffset,
                        end = sweepTarget,
                        strokeWidth = 2.dp.toPx()
                    )
                    
                    drawCircle(
                        color = proPrimary.copy(alpha = 0.3f * (1f - pulseScale)),
                        radius = 16.dp.toPx() * pulseScale,
                        center = centerOffset
                    )
                    drawCircle(
                        color = proPrimary,
                        radius = 4.dp.toPx(),
                        center = centerOffset
                    )

                    // Target 1: chapinero
                    val target1 = Offset(centerOffset.x + maxRadius * 0.5f, centerOffset.y - maxRadius * 0.4f)
                    drawCircle(
                        color = proGreenStyle.copy(alpha = 0.4f + 0.6f * pulseScale),
                        radius = 5.dp.toPx() * (0.8f + 0.4f * pulseScale),
                        center = target1
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.dp.toPx(),
                        center = target1
                    )

                    // Target 2: usaquen
                    val target2 = Offset(centerOffset.x - maxRadius * 0.6f, centerOffset.y + maxRadius * 0.3f)
                    drawCircle(
                        color = proPrimary.copy(alpha = 0.5f + 0.5f * (1.0f - pulseScale)),
                        radius = 4.dp.toPx() * (1.2f - 0.2f * pulseScale),
                        center = target2
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.dp.toPx(),
                        center = target2
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.9f))
                        .border(0.5.dp, proPrimary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "GPS Sincronizado",
                        color = proTextSec,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun AcceptedServiceYellowBanner(
    job: com.galibu.core.data.model.JobRequest
) {
    val agreedPrice = job.finalPrice ?: job.budgetMin
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)), // light slate/gray border
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // "Completado" badge pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF1F5F9))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Completado",
                        color = Color(0xFF475569),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Price display: agreed price
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Precio acordado",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                    Text(
                        text = "$${com.galibu.core.ui.utils.PriceUtils.formatThousands(agreedPrice.toLong().toString())}",
                        color = Color(0xFF0066FF),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Real job data
            Text(
                text = job.title.ifEmpty { job.category },
                color = Color(0xFF0F172A),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${job.category} · ${formatTimeAgo(job.createdAt)}",
                color = Color(0xFF64748B),
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Ya califiqué al finalizar: sin botón, solo espera de la calificación del cliente.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFEFF6FF))
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Esperando calificación de ${job.clientName}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2563EB)
                    )
                }
            }
        }
    }
}

@Composable
fun RespondidasSection(
    respondedJobs: List<Pair<String, String>> // list of (Title, Status: "Aceptado", "Rechazado")
) {
    if (respondedJobs.isNotEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = "RESPONDIDAS",
                color = Color(0xFF94A3B8), // slate/gray supporting text secondary
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    respondedJobs.forEachIndexed { index, (title, status) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                // Status colored dot
                                val dotColor = when (status) {
                                    "Aceptado" -> Color(0xFF22C55E) // Green dot
                                    else -> Color(0xFF94A3B8) // Gray dot
                                }
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(dotColor)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = title,
                                    color = Color(0xFF475569), // Slate dark text
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            
                            // Trailing text and icon
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                when (status) {
                                    "Aceptado" -> {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Aceptado",
                                            color = Color(0xFF475569),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    "Rechazado" -> {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = null,
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Rechazado",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    else -> {
                                        // Custom formatted status
                                        Text(
                                            text = status,
                                            color = Color(0xFFEA580C),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                        
                        if (index < respondedJobs.size - 1) {
                            HorizontalDivider(
                                color = Color(0xFFF1F5F9),
                                thickness = 1.dp,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
