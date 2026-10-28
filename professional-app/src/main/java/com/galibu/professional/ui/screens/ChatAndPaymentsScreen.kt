package com.galibu.professional.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.galibu.core.data.model.ChatMessage
import com.galibu.core.ui.utils.capitalizeFirstLetter
import com.galibu.core.ui.components.GalibuRatingSheet
import com.galibu.core.ui.components.PhotoSourceDialog
import com.galibu.core.ui.components.rememberCameraCaptureLauncher
import com.galibu.core.ui.theme.*
import com.galibu.professional.ui.viewmodels.ProfessionalViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatAndPaymentsScreen(
    viewModel: ProfessionalViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showImageSourceDialog by remember { mutableStateOf(false) }
    val imagePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes: ByteArray? = inputStream?.readBytes()
                inputStream?.close()
                if (bytes != null) {
                    viewModel.sendImageMessage(bytes)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val launchChatCamera = rememberCameraCaptureLauncher(
        maxDim = 1024,
        onImageCaptured = { base64 ->
            try {
                val bytes = android.util.Base64.decode(
                    base64.substringAfter("base64,"),
                    android.util.Base64.DEFAULT
                )
                viewModel.sendImageMessage(bytes)
            } catch (e: Exception) {
                e.printStackTrace()
                viewModel.showAlert("Aviso", "No se pudo enviar la foto.")
            }
        },
        onError = { message -> viewModel.showAlert("Aviso", message) }
    )

    if (showImageSourceDialog) {
        PhotoSourceDialog(
            title = "Adjuntar foto",
            message = "Elige de dónde deseas obtener la foto:",
            onDismiss = { showImageSourceDialog = false },
            onGallerySelected = {
                showImageSourceDialog = false
                imagePickerLauncher.launch("image/*")
            },
            onCameraSelected = {
                showImageSourceDialog = false
                launchChatCamera()
            }
        )
    }

    val currentUser by viewModel.currentUser.collectAsState()
    val activeRole by viewModel.activeRole.collectAsState()
    val job by viewModel.selectedJob.collectAsState()
    // Chat finalizado: se ve el historial pero no se puede escribir.
    val isChatLocked = remember(job?.status) {
        job?.status?.uppercase() in listOf("COMPLETED", "COMPLETADO", "CANCELLED", "CANCELADO")
    }
    val jobId by viewModel.selectedJobId.collectAsState()

    val messages by viewModel.messages.collectAsState()
    val isPartnerTyping by viewModel.isPartnerTyping.collectAsState()

    var messageText by remember { mutableStateOf("") }
    val lazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var showPaymentSheet by remember { mutableStateOf(false) }
    var selectedPaymentMethod by remember { mutableStateOf("Nequi") }
    var isPayingState by remember { mutableStateOf(false) }
    var showReviewDialog by remember { mutableStateOf(false) }
    var showProFinalizeConfirm by remember { mutableStateOf(false) }
    var isSavingState by remember { mutableStateOf(false) }

    val quickMessages = listOf(
        "Estoy en camino 🚗",
        "Llego en 10 minutos 🕒",
        "Estoy afuera del domicilio 🏠",
        "Trabajo terminado con éxito 🛠️",
        "Por favor califícame en la app! ✅"
    )

    // Manage Chat Subscription Lifecycles dynamically
    LaunchedEffect(jobId) {
        val currentId = jobId
        if (currentId != null) {
            viewModel.repository.refreshJobDetails(currentId)
            viewModel.startListening(currentId)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopListening()
        }
    }

    // Dynamic Typing Detection
    LaunchedEffect(messageText) {
        if (messageText.isNotEmpty()) {
            viewModel.setTyping(true)
            delay(3000)
            viewModel.setTyping(false)
        } else {
            viewModel.setTyping(false)
        }
    }

    // Helper functions inside composable
    fun getAvatarInitialsAndColor(name: String): Pair<String, Color> {
        val cleanName = name.trim()
        if (cleanName.startsWith("Carlos")) return "CM" to Color(0xFF0066FF)
        if (cleanName.startsWith("Ana")) return "AT" to Color(0xFFFA7FC1)
        val words = cleanName.split("\\s+".toRegex())
        val initials = if (words.size >= 2) {
            "${words[0].take(1)}${words[1].take(1)}"
        } else if (cleanName.isNotEmpty()) {
            cleanName.take(2)
        } else {
            "U"
        }
        return initials.uppercase() to Color(0xFF005AC1)
    }

    fun formatCustomTime(msg: ChatMessage): String {
        if (msg.id == "101") return "9:10"
        if (msg.id == "102") return "9:11"
        if (msg.id == "103") return "9:12"
        if (msg.id == "-100") return "9:09"
        val sdf = java.text.SimpleDateFormat("H:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(msg.timestamp))
    }

    // Scroll to bottom on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            lazyListState.animateScrollToItem(messages.size - 1)
        }
    }

    var otherUserProfile by remember { mutableStateOf<com.galibu.core.data.model.UserProfile?>(null) }
    LaunchedEffect(job, activeRole) {
        val otherUserId = if (activeRole == "CLIENT") job?.professionalId else job?.clientId
        if (otherUserId != null) {
            otherUserProfile = viewModel.repository.getUserById(otherUserId)
        }
    }

    val otherUserPhotoUrl = remember(otherUserProfile, job, activeRole) {
        val otherUserId = if (activeRole == "CLIENT") job?.professionalId else job?.clientId
        val dbPhoto = otherUserProfile?.profilePhotoUrl
        if (!dbPhoto.isNullOrEmpty()) {
            dbPhoto
        } else {
            when (otherUserId) {
                "bot_carlos" -> "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=150&h=150&fit=crop"
                "bot_ana" -> "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150&h=150&fit=crop"
                else -> null
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
    ) {
        val targetUserName = if (activeRole == "CLIENT") {
            job?.professionalName ?: "Carlos Mendoza"
        } else {
            job?.clientName ?: "Cliente"
        }

        val (initials, avatarColor) = getAvatarInitialsAndColor(targetUserName)

        // Toolbar Header styled exactly to Image 2
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .windowInsetsPadding(WindowInsets.statusBars)
                .border(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular grey Back Button (Image 2)
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

            // Professional Avatar with optional Green Online indicator
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .border(2.dp, PolishBlue, CircleShape)
            ) {
                if (!otherUserPhotoUrl.isNullOrEmpty()) {
                    val otherPhotoModel = remember(otherUserPhotoUrl) {
                        com.galibu.core.ui.utils.ImageUtils.getCoilModel(otherUserPhotoUrl)
                    }
                    coil.compose.AsyncImage(
                        model = otherPhotoModel,
                        contentDescription = "Foto de perfil",
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(avatarColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
                
                // Online Dot Badge (for active chat)
                if (job?.status == "ACCEPTED") {
                    Box(
                        modifier = Modifier
                            .size(13.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .padding(1.5.dp)
                            .align(Alignment.BottomEnd)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color(0xFF22C55E)) // Bright green online circle
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // User Info Column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = targetUserName,
                    color = Color(0xFF0F172A),
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    maxLines = 1
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val (statusText, statusColor) = if (job == null) {
                        Pair("Cargando...", Color(0xFF94A3B8))
                    } else {
                        when (job?.status) {
                            "ACCEPTED" -> Pair("Servicio activo", Color(0xFF22C55E))
                            "IN_PROGRESS" -> Pair("En progreso", Color(0xFF16A34A))
                            "PENDING_RATING" -> Pair("Por calificar", Color(0xFFD97706))
                            "COMPLETED" -> Pair("Finalizado", Color(0xFF94A3B8))
                            "OPEN", "NEGOTIATING", "PENDING", "COUNTERED_BY_CLIENT", "COUNTERED_BY_PROFESSIONAL" -> Pair("En negociación", Color(0xFF3B82F6))
                            "CANCELLED" -> Pair("Cancelado", Color(0xFFEF4444))
                            else -> Pair("Finalizado", Color(0xFF94A3B8))
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Text(
                        text = statusText,
                        color = Color(0xFF64748B),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Live Chat Messages Column
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(messages) { msg ->
                    val isOwn = msg.senderId == currentUser?.id
                    val isSystem = msg.senderId == "system"
                    val isSystemLightning = msg.senderId == "system_lightning"

                    if (isSystemLightning) {
                        // High fidelity Electrical Bolt Card right side (Image 2)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 160.dp, height = 110.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(Color(0xFF0091FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FlashOn,
                                        contentDescription = "Electricidad",
                                        tint = Color(0xFFFFD600),
                                        modifier = Modifier.size(54.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "9:09",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            }
                        }
                    } else if (isSystem) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 24.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF1F5F9))
                                .padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = msg.text,
                                color = Color(0xFF475569),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        // Regular message bubbles styled exactly as in Image 2
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start
                        ) {
                            Column(
                                horizontalAlignment = if (isOwn) Alignment.End else Alignment.Start,
                                modifier = Modifier.widthIn(max = 280.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(
                                            RoundedCornerShape(
                                                topStart = 16.dp,
                                                topEnd = 16.dp,
                                                bottomStart = if (isOwn) 16.dp else 4.dp,
                                                bottomEnd = if (isOwn) 4.dp else 16.dp
                                            )
                                        )
                                        .background(
                                            if (isOwn) Color(0xFF0066FF) else Color.White
                                        )
                                        .then(
                                            if (!isOwn) {
                                                Modifier.border(
                                                    width = 1.dp,
                                                    color = Color(0xFFF1F5F9),
                                                    shape = RoundedCornerShape(
                                                        topStart = 16.dp,
                                                        topEnd = 16.dp,
                                                        bottomStart = 4.dp,
                                                        bottomEnd = 16.dp
                                                    )
                                                )
                                            } else Modifier
                                        )
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    if (msg.type == "image") {
                                        coil.compose.AsyncImage(
                                            model = msg.text,
                                            contentDescription = "Imagen",
                                            modifier = Modifier
                                                .widthIn(max = 200.dp)
                                                .heightIn(max = 250.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                        )
                                    } else {
                                        Text(
                                            text = msg.text,
                                            color = if (isOwn) Color.White else Color(0xFF1E293B),
                                            fontSize = 15.sp,
                                            lineHeight = 20.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                ) {
                                    Text(
                                        text = formatCustomTime(msg),
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp
                                    )
                                    if (isOwn) {
                                        if (msg.read) {
                                            Text(
                                                text = "✓✓ leído",
                                                color = Color(0xFF0066FF),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        } else {
                                            Text(
                                                text = "✓ enviado",
                                                color = Color(0xFF94A3B8),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (isPartnerTyping) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Column(
                                horizontalAlignment = Alignment.Start,
                                modifier = Modifier.widthIn(max = 280.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(
                                            RoundedCornerShape(
                                                topStart = 16.dp,
                                                topEnd = 16.dp,
                                                bottomStart = 4.dp,
                                                bottomEnd = 16.dp
                                            )
                                        )
                                        .background(Color.White)
                                        .border(
                                            width = 1.dp,
                                            color = Color(0xFFF1F5F9),
                                            shape = RoundedCornerShape(
                                                topStart = 16.dp,
                                                topEnd = 16.dp,
                                                bottomStart = 4.dp,
                                                bottomEnd = 16.dp
                                            )
                                        )
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    val partnerName = job?.professionalName ?: job?.clientName ?: "Compañero"
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(12.dp),
                                            strokeWidth = 2.dp,
                                            color = Color(0xFF0066FF)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "$partnerName está escribiendo...",
                                            color = Color(0xFF64748B),
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Quick dialogue buttons for rapid response
        if (job?.status == "ACCEPTED") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                quickMessages.forEach { item ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White)
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp))
                            .clickable { viewModel.sendChatMessage(item) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(item, color = Color(0xFF0066FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Chat finalizado: historial visible, sin escritura.
        if (isChatLocked) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF1F5F9))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Chat finalizado. Puedes ver el historial, pero ya no se pueden enviar mensajes.",
                    color = Color(0xFF64748B),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
        // Chat input bar matching Image 2 exactly
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .border(width = 1.dp, color = Color(0xFFF1F5F9))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Attachment Icon Button in a border circle
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(width = 1.dp, color = Color(0xFFE2E8F0), shape = RoundedCornerShape(12.dp))
                    .clickable { showImageSourceDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = "Adjuntar Foto",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Message text input bar
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it.capitalizeFirstLetter() },
                placeholder = { Text("Escribe un mensaje...", color = Color(0xFF94A3B8), fontSize = 14.sp) },
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFE2E8F0),
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    focusedContainerColor = Color(0xFFF8FAFC),
                    unfocusedContainerColor = Color(0xFFF8FAFC),
                    focusedTextColor = Color(0xFF1E293B),
                    unfocusedTextColor = Color(0xFF1E293B)
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_field")
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Send circular blue button
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0066FF))
                    .clickable {
                        val input = messageText.trim()
                        if (input.isNotEmpty()) {
                            messageText = ""
                            viewModel.sendMessage(input)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Enviar",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        }
    }

    // Integrated Payment Gateway Simulation dialog
    if (showPaymentSheet) {
        val finalPaymentValue = job?.finalPrice ?: job?.budgetMin ?: 20000.0
        AlertDialog(
            onDismissRequest = { if (!isPayingState) showPaymentSheet = false },
            confirmButton = {
                if (!isPayingState) {
                    Button(
                        onClick = {
                            isPayingState = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        modifier = Modifier.testTag("submit_gateway_payment")
                    ) {
                        Text("Iniciar Pago Seguro", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                if (!isPayingState) {
                    TextButton(onClick = { showPaymentSheet = false }) {
                        Text("Volver", color = Color.Gray)
                    }
                }
            },
            containerColor = CardSurface,
            title = {
                Text(
                    text = "Pasarela de Pago Integrada 💳",
                    fontWeight = FontWeight.Bold,
                    color = OffWhite,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!isPayingState) {
                        Text(
                            text = "Seleccione el método de pago para depositar de manera segura sus honorarios a cargo de Firebase Cloud Vault:",
                            color = LightGray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Payment Methods Choice
                        listOf("Nequi 📱", "Daviplata 💵", "Tarjeta de Crédito (Visa) 💳", "PSE / Red CoBancos 🏦").forEach { pm ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selectedPaymentMethod == pm.substringBefore(" ")) PrimaryGreen.copy(alpha = 0.15f) else Color.Transparent)
                                    .border(
                                        1.dp,
                                        if (selectedPaymentMethod == pm.substringBefore(" ")) PrimaryGreen else Color.Gray.copy(alpha = 0.2f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedPaymentMethod = pm.substringBefore(" ") }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(pm, color = OffWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                RadioButton(
                                    selected = selectedPaymentMethod == pm.substringBefore(" "),
                                    onClick = { selectedPaymentMethod = pm.substringBefore(" ") },
                                    colors = RadioButtonDefaults.colors(selectedColor = PrimaryGreen)
                                )
                            }
                        }

                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Monto Convenido:", fontSize = 13.sp, color = LightGray)
                            Text(
                                "$${NumberFormat.getNumberInstance(Locale.US).format(finalPaymentValue)} COP",
                                color = PolishGreen,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    } else {
                        // PAYING ANIMATED TRANSITION
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = PolishGreen, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Enlazando con Gateway de $selectedPaymentMethod...",
                                color = OffWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                "Debitando fondos and certificando transferencia SSL en Firebase Cloud Vault.",
                                color = LightGray,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        LaunchedEffect(Unit) {
                            delay(4000) // 4 seconds SSL loading sim
                            job?.id?.let { id ->
                                viewModel.payAndCompleteJob(id, selectedPaymentMethod) {
                                    isPayingState = false
                                    showPaymentSheet = false
                                    showReviewDialog = true
                                }
                            }
                        }
                    }
                }
            }
        )
    }

    // Calificación del servicio: hoja homologada en todas las pantallas.
    if (showReviewDialog) {
        val targetName = if (activeRole == "CLIENT") {
            job?.professionalName ?: "el profesional"
        } else {
            job?.clientName ?: "el cliente"
        }
        GalibuRatingSheet(
            title = if (activeRole == "CLIENT") "Calificar al profesional" else "Calificar al cliente",
            targetName = targetName,
            onDismissRequest = { showReviewDialog = false },
            onSubmit = { rating, comment ->
                job?.id?.let { id ->
                    viewModel.submitRating(id, rating.toFloat(), comment, activeRole == "CLIENT")
                }
                showReviewDialog = false
            }
        )
    }

    // Professional Dialog to Confirm Finalizing & Requesting Payment
    if (showProFinalizeConfirm) {
        val finalPaymentValue = job?.finalPrice ?: job?.budgetMin ?: 20000.0
        AlertDialog(
            onDismissRequest = { if (!isSavingState) showProFinalizeConfirm = false },
            confirmButton = {
                if (!isSavingState) {
                    Button(
                        onClick = {
                            isSavingState = true
                            job?.id?.let { id ->
                                viewModel.payAndCompleteJob(id, "Transferencia / Nequi") {
                                    isSavingState = false
                                    showProFinalizeConfirm = false
                                    showReviewDialog = true
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        modifier = Modifier.testTag("submit_pro_finalize")
                    ) {
                        Text("Confirmar y Terminar", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                if (!isSavingState) {
                    TextButton(onClick = { showProFinalizeConfirm = false }) {
                        Text("Cancelar", color = Color.Gray)
                    }
                }
            },
            containerColor = CardSurface,
            title = {
                Text(
                    text = "Finalizar Servicio 🛠️",
                    fontWeight = FontWeight.Bold,
                    color = OffWhite,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!isSavingState) {
                        Text(
                            text = "¿Estás seguro de que has completado el servicio de ${job?.title ?: "labor técnica"}?",
                            color = OffWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Al finalizar, se registrará el trabajo como completado con éxito, lo que notificará al cliente para liberar de forma segura tus honorarios de $${NumberFormat.getNumberInstance(Locale.US).format(finalPaymentValue)} COP de la bóveda Galibu.",
                            color = LightGray,
                            fontSize = 12.sp
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = PolishGreen, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Registrando entrega de servicio...",
                                color = OffWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        )
    }
}


