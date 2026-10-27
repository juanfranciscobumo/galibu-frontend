package com.galibu.client.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import coil.compose.AsyncImage
import com.galibu.core.data.model.Bid
import com.galibu.core.ui.utils.capitalizeFirstLetter
import com.galibu.core.data.model.JobRequest
import com.galibu.core.data.remote.RatingDto
import com.galibu.core.ui.components.GalibuAlertDialog
import com.galibu.core.ui.components.GalibuAlertType
import com.galibu.core.ui.components.GalibuRatingSheet
import com.galibu.core.ui.components.HireDecisionButtons
import com.galibu.core.ui.components.PrimaryContactButton
import com.galibu.core.ui.components.categoryVisual
import com.galibu.core.ui.theme.*
import com.galibu.client.ui.viewmodels.ClientViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceNegotiationScreen(
    viewModel: ClientViewModel,
    onOpenChat: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedJob by viewModel.selectedJob.collectAsState()
    val bids by viewModel.selectedJobBids.collectAsState()
    val professionals by viewModel.professionals.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var showDirectCounterInline by remember { mutableStateOf(false) }
    var showClientRatingSheet by remember { mutableStateOf(false) }
    var clientJustRated by remember { mutableStateOf(false) }
    var finishServiceOnRating by remember { mutableStateOf(false) }

    // Las ofertas del profesional pueden llegar con esta pantalla abierta:
    // se consultan cada 5 s y se refresca el detalle completo cada 20 s.
    val openJobId = selectedJob?.id
    LaunchedEffect(openJobId) {
        val jobId = openJobId ?: return@LaunchedEffect
        var tick = 0
        while (true) {
            try {
                if (tick % 4 == 0) {
                    viewModel.refreshSelectedJob()
                } else {
                    viewModel.refreshSelectedJobBids(jobId)
                }
            } catch (e: Exception) {
                android.util.Log.e("PriceNegotiation", "Falla refresco periódico: ${e.localizedMessage}")
            }
            tick++
            kotlinx.coroutines.delay(5_000)
        }
    }

    // Al volver del segundo plano (por ejemplo, tras usar la app profesional)
    // se refresca de inmediato para no depender del siguiente ciclo del polling.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        openJobId?.let { viewModel.refreshSelectedJob() }
    }

    var showPortfolioDialog by remember { mutableStateOf(false) }
    var portfolioProId by remember { mutableStateOf<String?>(null) }
    var portfolioProName by remember { mutableStateOf("") }
    var showProfileDetailForBid by remember { mutableStateOf<Bid?>(null) }
    var showAssignedRatingsSheet by remember { mutableStateOf(false) }
    var assignedProRatings by remember { mutableStateOf<List<RatingDto>>(emptyList()) }
    val hasAssignedProfessional = selectedJob?.professionalId?.isNotEmpty() == true
    val isDirectNegotiation =
        selectedJob?.let { it.status == "NEGOTIATING" && !it.professionalId.isNullOrEmpty() }
            ?: false

    LaunchedEffect(selectedJob?.professionalId) {
        val proId = selectedJob?.professionalId
        assignedProRatings = if (proId.isNullOrEmpty()) {
            emptyList()
        } else {
            viewModel.repository.getUserRatings(proId)
        }
    }

    // Si acabo de calificar y el servicio quedó completado por ambas partes,
    // volver al home en lugar de quedarse en la pantalla.
    LaunchedEffect(selectedJob?.status, selectedJob?.clientRatingOfPro, selectedJob?.proRatingOfClient) {
        val j = selectedJob
        if (clientJustRated && j != null && j.status == "COMPLETED" &&
            j.clientRatingOfPro != null && j.proRatingOfClient != null
        ) {
            clientJustRated = false
            viewModel.showAlert("Aviso", "¡Servicio completado! Gracias por calificar.")
            onBack()
        }
    }


    Box(modifier = modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PolishBg)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Back toolbar (Clean, dark-colored text on light background)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Ofertas de Profesionales",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = PolishDark
                )
            }
        }
        selectedJob?.let { job ->
            val professionalUser = professionals.find { it.id == job.professionalId }
            val baseRateVal = job.proHourlyRate ?: professionalUser?.hourlyRate ?: job.budgetMin
            val proposedRateValue = when (job.counterOfferSender) {
                "CLIENT" -> job.counterOfferAmount ?: job.budgetMin
                "PROFESSIONAL" -> job.counterOfferAmount ?: baseRateVal
                // Directa sin contraoferta aún: el precio es el que envió el cliente (budgetMin).
                else -> if (!job.professionalId.isNullOrEmpty() && job.budgetMin > 0) job.budgetMin else baseRateVal
            }
            val isWaitingForPro =
                job.counterOfferSender == null || job.counterOfferSender == "CLIENT"
            // Detalle de contratación directa con estilo ficha del profesional;
            // la lista de ofertas no aplica aquí.
            if (isDirectNegotiation) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { showAssignedRatingsSheet = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val proName = job.professionalName ?: professionalUser?.name ?: "Profesional"
                        val proPhotoUrl = professionalUser?.profilePhotoUrl
                            ?: bids.find { it.professionalId == job.professionalId }?.professionalPhoto
                        val proPhotoModel = remember(proPhotoUrl) { decodePhotoModel(proPhotoUrl) }
                        val initials = proName.split(" ").filter { it.isNotEmpty() }.take(2)
                            .joinToString("") { it.first().uppercase() }
                        Box(modifier = Modifier.size(72.dp)) {
                            Box(
                                modifier = Modifier.size(72.dp).clip(CircleShape).background(Color(0xFFEFF6FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (proPhotoModel != null) {
                                    AsyncImage(
                                        model = proPhotoModel,
                                        contentDescription = proName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text(initials.ifEmpty { "P" }, color = PolishBlue, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Box(
                                modifier = Modifier.size(18.dp).align(Alignment.BottomEnd)
                                    .clip(CircleShape).background(Color(0xFF22C55E))
                                    .border(2.dp, Color.White, CircleShape)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = proName,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = PolishDark,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB000), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (assignedProRatings.isNotEmpty()) {
                                        "%.1f".format(assignedProRatings.map { it.stars }.average())
                                    } else {
                                        "5.0"
                                    },
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PolishDark
                                )
                            }
                            if (professionalUser?.isVerified == true) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFDBEAFE))
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Verified, contentDescription = null, tint = PolishBlue, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "Profesional verificado", color = PolishBlue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                        Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Ver calificaciones", tint = Color(0xFF64748B), modifier = Modifier.size(24.dp))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val proLocationText = listOf(
                        professionalUser?.municipality?.takeIf { it.isNotBlank() },
                        professionalUser?.department?.takeIf { it.isNotBlank() }
                    ).filterNotNull().joinToString(", ").ifEmpty { job.address }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Work, contentDescription = null, tint = PolishDark, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = job.category.takeIf { it.isNotBlank() } ?: "Servicio",
                            fontSize = 15.sp,
                            color = PolishDark,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = PolishDark, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = proLocationText.ifEmpty { "Sin ubicación" },
                            fontSize = 15.sp,
                            color = PolishDark,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Schedule, contentDescription = null, tint = PolishDark, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = formatTimestamp(job.createdAt).replaceFirstChar { it.uppercase() },
                            fontSize = 15.sp,
                            color = PolishDark
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "$${NumberFormat.getNumberInstance(Locale.US).format(proposedRateValue)}",
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PolishDark,
                                    maxLines = 1
                                )
                                Text(text = " COP", fontSize = 14.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                            }
                            Text(
                                text = when (job.counterOfferSender) {
                                    "PROFESSIONAL" -> "Contraoferta del profesional"
                                    "CLIENT" -> "Tu contraoferta"
                                    else -> "Precio de la oferta"
                                },
                                fontSize = 13.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        PrimaryContactButton(
                            onClick = {
                                viewModel.selectJob(job.id)
                                onOpenChat(job.id)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (!isWaitingForPro) {
                    Button(
                        onClick = {
                            viewModel.acceptDirectHire(job.id) {
                                viewModel.showAlert("Aviso", "¡Contratación directa aceptada exitosamente!")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PolishGreen),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Aceptar oferta", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    if (!showDirectCounterInline) {
                        OutlinedButton(
                            onClick = { showDirectCounterInline = true },
                            border = BorderStroke(1.5.dp, Color(0xFFFF9800)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) {
                            Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Contraofertar", color = Color(0xFFFF9800), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    AnimatedVisibility(visible = showDirectCounterInline) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                com.galibu.core.ui.components.InlineCounterOfferInput(
                                    initialAmount = 0.0,
                                    accentColor = PolishBlue,
                                    onSend = { amount ->
                                        viewModel.submitCounterOfferDirectHire(job.id, amount, isClient = true) {
                                            viewModel.showAlert("Aviso", "¡Contraoferta enviada exitosamente!")
                                        }
                                        showDirectCounterInline = false
                                    },
                                    onCancel = { showDirectCounterInline = false }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFEFF6FF))
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Oferta enviada, esperando la respuesta del profesional",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PolishBlue,
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    OutlinedButton(
                        onClick = {
                            viewModel.cancelJob(job.id, "Cancelada por el cliente") {
                                viewModel.showAlert("Aviso", "Solicitud cancelada exitosamente")
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                        border = BorderStroke(1.5.dp, Color(0xFFDC2626)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isWaitingForPro) "Cancelar solicitud" else "Rechazar oferta",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
            // Hero only for assigned/direct negotiations; the bids list below covers the rest.
            } else if (hasAssignedProfessional) {
            Card(
                colors = CardDefaults.cardColors(containerColor = PolishBlue),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier.size(150.dp).align(Alignment.BottomEnd)
                            .offset(x = 24.dp, y = 24.dp)
                    )
                    Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                        val heroCategory = (job.category ?: "Servicio").split(",").firstOrNull()?.trim()?.takeIf { it.isNotEmpty() } ?: "Servicio"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.background(
                                    Color(0xFF004CB0),
                                    RoundedCornerShape(16.dp)
                                ).padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = categoryVisual(heroCategory).icon,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = heroCategory.uppercase(),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        val heroTitle = job.title?.takeIf { it.isNotBlank() }
                            ?: job.description.takeIf { it.isNotBlank() }
                            ?: heroCategory
                        if (!heroTitle.equals(heroCategory, ignoreCase = true)) {
                            Text(
                                text = heroTitle,
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (job.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = job.description,
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 13.sp,
                                lineHeight = 17.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = job.address,
                                color = Color.White,
                                fontSize = 13.sp,
                                lineHeight = 17.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = formatTimestamp(job.createdAt).replaceFirstChar { it.uppercase() },
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 12.sp
                            )
                        }

                        if (isDirectNegotiation) {
                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(
                                color = Color.White.copy(alpha = 0.28f),
                                thickness = 1.dp
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            if (!isWaitingForPro) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Groups,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Contraoferta recibida · Revisa y decide.",
                                    color = Color.White.copy(alpha = 0.92f),
                                    fontSize = 12.sp
                                )
                            }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            val cancelDirect = {
                                viewModel.cancelJob(job.id, "Cancelada por el cliente") {
                                    viewModel.showAlert(
                                        "Aviso",
                                        "Solicitud cancelada exitosamente"
                                    )
                                }
                            }
                            if (!isWaitingForPro) {
                                HireDecisionButtons(
                                    onAccept = {
                                        viewModel.acceptDirectHire(job.id) {
                                            viewModel.showAlert(
                                                "Aviso",
                                                "¡Contratación directa aceptada exitosamente!"
                                            )
                                        }
                                    },
                                    onCounter = if (!showDirectCounterInline) {
                                        { showDirectCounterInline = true }
                                    } else {
                                        null
                                    },
                                    onReject = cancelDirect,
                                    acceptLabel = "Aceptar oferta",
                                    rejectLabel = "Rechazar oferta"
                                )
                            } else {
                                HireDecisionButtons(
                                    onAccept = null,
                                    onCounter = null,
                                    onReject = cancelDirect,
                                    rejectLabel = "Cancelar solicitud"
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    viewModel.selectJob(job.id)
                                    onOpenChat(job.id)
                                },
                                border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.7f)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChatBubbleOutline,
                                    contentDescription = "Conversación",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Contactar",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Campo en línea: reemplaza al bottom-sheet de contraoferta directa.
                            // Al dar tap a Contraofertar desaparece el botón y aparece input + Enviar.
                            AnimatedVisibility(visible = showDirectCounterInline) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        com.galibu.core.ui.components.InlineCounterOfferInput(
                                            initialAmount = proposedRateValue,
                                            accentColor = PolishBlue,
                                            onSend = { amount ->
                                                viewModel.submitCounterOfferDirectHire(
                                                    job.id,
                                                    amount,
                                                    isClient = true
                                                ) {
                                                    viewModel.showAlert(
                                                        "Aviso",
                                                        "¡Contraoferta enviada exitosamente!"
                                                    )
                                                }
                                                showDirectCounterInline = false
                                            },
                                            onCancel = { showDirectCounterInline = false }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (hasAssignedProfessional && !isDirectNegotiation) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val detailStatusLabel = when (job.status) {
                                "ACCEPTED" -> "Trabajo en curso"
                                "IN_PROGRESS" -> "En progreso"
                                "PENDING_RATING" -> "Por calificar"
                                "COMPLETED" -> "Completado"
                                else -> if (isWaitingForPro) "Propuesta enviada" else "Contraoferta recibida"
                            }
                            val detailStatusColor = when (job.status) {
                                "ACCEPTED" -> Color(0xFF2563EB)
                                "IN_PROGRESS" -> Color(0xFF16A34A)
                                "PENDING_RATING" -> Color(0xFFD97706)
                                "COMPLETED" -> Color(0xFF475569)
                                else -> Color(0xFFEA580C)
                            }
                            val detailStatusBg = when (job.status) {
                                "ACCEPTED" -> Color(0xFFEFF6FF)
                                "IN_PROGRESS" -> Color(0xFFDCFCE7)
                                "PENDING_RATING" -> Color(0xFFFFFBEB)
                                "COMPLETED" -> Color(0xFFF1F5F9)
                                else -> Color(0xFFFFF7ED)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(30.dp))
                                    .background(detailStatusBg)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = detailStatusLabel,
                                    color = detailStatusColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }


                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showAssignedRatingsSheet = true },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val proName = job.professionalName ?: "Profesional"
                            val initials = proName.split(" ").filter { it.isNotEmpty() }.take(2)
                                .joinToString("") { it.first().uppercase() }
                            val proPhotoUrl = professionals.find { it.id == job.professionalId }?.profilePhotoUrl ?: bids.find { it.professionalId == job.professionalId }?.professionalPhoto
                            val proDir = professionals.find { it.id == job.professionalId }
                            val proLocation = listOf(
                                proDir?.municipality?.takeIf { it.isNotBlank() },
                                proDir?.department?.takeIf { it.isNotBlank() }
                            ).filterNotNull().joinToString(", ").ifEmpty { job.address }
                            val proPhotoModel = remember(proPhotoUrl) { decodePhotoModel(proPhotoUrl) }
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEFF6FF))
                                    .border(2.dp, Color(0xFFBFDBFE), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (proPhotoModel != null) {
                                    AsyncImage(
                                        model = proPhotoModel,
                                        contentDescription = proName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text(
                                        initials.ifEmpty { "P" },
                                        color = Color(0xFF2563EB),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = proName,
                                    color = Color(0xFF1E293B),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFFFB000),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (assignedProRatings.isNotEmpty()) {
                                            val avg = assignedProRatings.map { it.stars }.average()
                                            "%.1f · %d reseñas".format(avg, assignedProRatings.size)
                                        } else {
                                            "5.0 · Sin reseñas"
                                        },
                                        color = Color(0xFF1E293B),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                if (proLocation.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = Color(0xFF64748B),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = proLocation,
                                            color = Color(0xFF64748B),
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Ver historial",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (job.status == "NEGOTIATING") "Precio ofertado" else "Precio final aceptado",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = "$${
                                NumberFormat.getNumberInstance(Locale.US)
                                    .format(if (job.status == "NEGOTIATING") proposedRateValue else (job.finalPrice ?: proposedRateValue))
                            }/h",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEA580C)
                        )

                        if (job.status == "ACCEPTED" || job.status == "IN_PROGRESS") {
                            if (job.clientRatingOfPro != null) {
                                // Ya califiqué al profesional: sin botones, solo espera.
                                Spacer(modifier = Modifier.height(14.dp))
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
                                            text = "Esperando finalización del profesional",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF2563EB)
                                        )
                                    }
                                }
                            } else {
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.selectJob(job.id)
                                        onOpenChat(job.id)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PolishBlue),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .height(44.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ChatBubbleOutline,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Chatear", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }

                                // Finalizar solo cuando el servicio está en progreso, no en aceptado.
                                if (job.status == "IN_PROGRESS") {
                                    Button(
                                    onClick = {
                                        // Ambos pueden finalizar: el cliente califica
                                        // y el backend cierra el servicio en la misma acción.
                                        finishServiceOnRating = true
                                        showClientRatingSheet = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Finalizar", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                                }
                            }
                            
                            // Botón Cancelar (no aplica en progreso: el servicio ya arrancó).
                            if (job.status != "IN_PROGRESS") {
                            Spacer(modifier = Modifier.height(8.dp))
                            var showCancelDialog by remember { mutableStateOf(false) }
                            var cancelReason by remember { mutableStateOf("") }
                            
                            OutlinedButton(
                                onClick = { showCancelDialog = true },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = Color(0xFFDC2626)
                                ),
                                border = BorderStroke(1.dp, Color(0xFFDC2626)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Cancelar trabajo", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            }
                            
                            if (showCancelDialog) {
                                AlertDialog(
                                    onDismissRequest = { showCancelDialog = false },
                                    title = { 
                                        Text(
                                            "¿Cancelar trabajo aceptado?", 
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        ) 
                                    },
                                    text = {
                                        Column {
                                            Text(
                                                "Esta acción no se puede deshacer. El trabajo será cancelado y se notificará al profesional asignado.",
                                                fontSize = 14.sp,
                                                color = Color(0xFF64748B)
                                            )
                                            Spacer(Modifier.height(16.dp))
                                            OutlinedTextField(
                                                value = cancelReason,
                                                onValueChange = { cancelReason = it.capitalizeFirstLetter() },
                                                label = { Text("Razón (opcional)", fontSize = 13.sp) },
                                                placeholder = { Text("Ej: Ya no necesito el servicio", fontSize = 12.sp) },
                                                modifier = Modifier.fillMaxWidth(),
                                                maxLines = 3,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = PolishBlue,
                                                    focusedLabelColor = PolishBlue
                                                )
                                            )
                                        }
                                    },
                                    confirmButton = {
                                        Button(
                                            onClick = {
                                                viewModel.cancelJob(job.id, cancelReason) {
                                                    showCancelDialog = false
                                                    cancelReason = ""
                                                    viewModel.showAlert("Aviso", "Trabajo cancelado exitosamente")
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFFDC2626)
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Sí, cancelar", fontWeight = FontWeight.Bold)
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(
                                            onClick = { 
                                                showCancelDialog = false
                                                cancelReason = ""
                                            }
                                        ) {
                                            Text("No, volver", color = Color(0xFF64748B))
                                        }
                                    },
                                    shape = RoundedCornerShape(16.dp)
                                )
                            }
                            }
                            }
                        }

                        // Calificar: en PENDING_RATING o en COMPLETED sin mi calificación
                        // (el pro puede finalizar primero y el job llega COMPLETED directo).
                        if ((job.status == "PENDING_RATING" || job.status == "COMPLETED") && job.clientRatingOfPro == null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    viewModel.selectJob(job.id)
                                    finishServiceOnRating = false
                                    showClientRatingSheet = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFFFBEB),
                                    contentColor = Color(0xFFD97706)
                                ),
                                border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.StarOutline,
                                    contentDescription = null,
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Calificar al profesional", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            }
        }

        selectedJob?.let { job ->
            if (!isDirectNegotiation && !hasAssignedProfessional &&
                job.status.uppercase() !in listOf("CANCELLED", "CANCELADO", "COMPLETED", "COMPLETADO")
            ) {
                OpenJobRequestCard(
                    job = job,
                    bidsCount = bids.size,
                    onCancel = {
                        viewModel.cancelJob(job.id, "Cancelada por el cliente") {
                            viewModel.showAlert("Aviso", "Solicitud cancelada exitosamente")
                            onBack()
                        }
                    }
                )
            }
        }

        // Sin sección de ofertas cuando ya hay profesional asignado:
        // la negociación terminó y solo queda la tarjeta resumen.
        if (!isDirectNegotiation && !hasAssignedProfessional) {
            val filteredBids = remember(bids, selectedJob) {
                // El profesional asignado ya tiene su tarjeta resumen arriba;
                // no se repite su oferta en la lista.
                val assignedId = selectedJob?.professionalId?.takeIf { it.isNotEmpty() }
                if (assignedId == null) {
                    bids
                } else {
                    bids.filter { it.professionalId != assignedId }
                }
            }
            // Reseñas reales por profesional (mismo formato que la tarjeta de
            // cliente en la app profesional: "4.0 · 2 reseñas").
            val proRatingsByProfessional = remember { mutableStateMapOf<String, List<RatingDto>>() }
            LaunchedEffect(filteredBids) {
                filteredBids.map { it.professionalId }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .forEach { proId ->
                        if (!proRatingsByProfessional.containsKey(proId)) {
                            proRatingsByProfessional[proId] = viewModel.repository.getUserRatings(proId)
                        }
                    }
            }
            if (filteredBids.isEmpty()) {
                // Mismo estado vacío que Mensajes: sin tarjeta, ícono azul y texto centrado.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(PolishBlue.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forum,
                            contentDescription = null,
                            tint = PolishBlue,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (bids.isEmpty()) "Esperando ofertas" else "Sin ofertas disponibles",
                        color = PolishDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Profesionales cercanos están revisando tu solicitud.\nRecibirás contraofertas de precios en un momento.",
                        color = PolishGrayText,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                filteredBids.forEachIndexed { index, bid ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .border(1.dp, PolishBorder, RoundedCornerShape(16.dp))
                            .testTag("bid_card_${bid.id}")
                            .clickable { showProfileDetailForBid = bid }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val docModel = remember(bid.professionalPhoto) {
                                    if (bid.professionalPhoto.isNullOrEmpty()) {
                                        "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&h=150&fit=crop"
                                    } else if (bid.professionalPhoto.startsWith("data:image/")) {
                                        try {
                                            val base64Data =
                                                bid.professionalPhoto.substringAfter("base64,")
                                            val decodedBytes = android.util.Base64.decode(
                                                base64Data,
                                                android.util.Base64.DEFAULT
                                            )
                                            android.graphics.BitmapFactory.decodeByteArray(
                                                decodedBytes,
                                                0,
                                                decodedBytes.size
                                            )
                                        } catch (e: Exception) {
                                            bid.professionalPhoto
                                        }
                                    } else {
                                        bid.professionalPhoto
                                    }
                                }
                                AsyncImage(
                                    model = docModel,
                                    contentDescription = bid.professionalName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(56.dp).clip(CircleShape)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = bid.professionalName,
                                            fontWeight = FontWeight.Bold,
                                            color = PolishDark,
                                            fontSize = 16.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = Color(0xFFF59E0B),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        val bidProRatings = proRatingsByProfessional[bid.professionalId]
                                        val bidProAvg = if (!bidProRatings.isNullOrEmpty()) {
                                            bidProRatings.map { it.stars }.average().toFloat()
                                        } else {
                                            bid.professionalRating
                                        }
                                        Text(
                                            text = String.format(Locale.US, "%.1f", bidProAvg),
                                            color = PolishDark,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        if (bidProRatings != null) {
                                            Text(
                                                text = if (bidProRatings.isNotEmpty()) " · ${bidProRatings.size} reseñas" else " · Sin reseñas",
                                                color = Color(0xFF64748B),
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFE0E7FF))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Verified,
                                            contentDescription = null,
                                            tint = PolishBlue,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Profesional verificado",
                                            color = PolishBlue,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            val bidProLocation = remember(bid.professionalId, professionals, selectedJob) {
                                val pro = professionals.find { it.id == bid.professionalId }
                                val muni = pro?.municipality?.takeIf { it.isNotBlank() }
                                val dept = pro?.department?.takeIf { it.isNotBlank() }
                                when {
                                    muni != null && dept != null && !muni.equals(dept, ignoreCase = true) -> "$muni, $dept"
                                    muni != null -> muni
                                    dept != null -> dept
                                    else -> selectedJob?.address ?: ""
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.BusinessCenter,
                                    contentDescription = null,
                                    tint = PolishDark,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = selectedJob?.category?.takeIf { it.isNotBlank() } ?: "Servicio",
                                    color = Color(0xFF475569),
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = null,
                                    tint = PolishDark,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = bidProLocation,
                                    color = Color(0xFF475569),
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = PolishDark,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Hace ${formatTimestamp(bid.createdAt)}",
                                    color = Color(0xFF475569),
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = "$${
                                            NumberFormat.getNumberInstance(Locale.US)
                                                .format(bid.amount)
                                        }",
                                        fontWeight = FontWeight.Black,
                                        color = PolishDark,
                                        fontSize = 20.sp,
                                        modifier = Modifier.testTag("bid_price_${bid.id}")
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "COP",
                                        fontSize = 12.sp,
                                        color = Color(0xFF64748B),
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                }
                                if (index == 0) {
                                    Button(
                                        onClick = { showProfileDetailForBid = bid },
                                        colors = ButtonDefaults.buttonColors(containerColor = PolishBlue),
                                        shape = RoundedCornerShape(20.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text("Ver oferta", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = { showProfileDetailForBid = bid },
                                        border = BorderStroke(1.dp, PolishBlue),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PolishBlue),
                                        shape = RoundedCornerShape(20.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text("Ver oferta", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }

                            // Status tags for counters
                            if (bid.status == "COUNTERED_BY_CLIENT") {
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF2196F3).copy(alpha = 0.1f))
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Les hiciste una contraoferta. Esperando respuesta...",
                                        color = Color(0xFF2196F3),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else if (bid.status == "COUNTERED_BY_PROFESSIONAL") {
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(PolishGreen.copy(alpha = 0.15f))
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "¡El profesional contraofertó! Revisa su nueva propuesta.",
                                        color = PolishGreen,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

        }
        // Historial de reseñas del profesional asignado (volver retorna a la pantalla).
        if (showAssignedRatingsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAssignedRatingsSheet = false },
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
                        text = "Calificaciones y reseñas",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = PolishDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = selectedJob?.professionalName?.takeIf { it.isNotBlank() } ?: "Profesional",
                        color = Color(0xFF64748B),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    if (assignedProRatings.isEmpty()) {
                        Text(
                            text = "Aún no tiene reseñas.",
                            color = Color(0xFF64748B),
                            fontSize = 14.sp
                        )
                    } else {
                        val dateFmt = remember { java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale("es", "CO")) }
                        assignedProRatings.forEach { rating ->
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
                                    if (!rating.comment.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "\"${rating.comment}\"",
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

        // Hoja de calificación (diseño homologado en todas las pantallas).
        // Se abre al finalizar o al calificar; si se cierra sin enviar no se
        // ejecuta ninguna acción y el servicio sigue como estaba.
        if (showClientRatingSheet) {
            selectedJob?.let { activeJob ->
                GalibuRatingSheet(
                    title = "Calificar al profesional",
                    targetName = (activeJob.professionalName ?: "").ifBlank { "el profesional" },
                    onDismissRequest = { showClientRatingSheet = false },
                    onSubmit = { rating, comment ->
                        // Cerrar la hoja de inmediato: el envío sigue en segundo
                        // plano y el overlay de carga queda visible encima.
                        showClientRatingSheet = false
                        if (finishServiceOnRating) {
                            viewModel.finishService(activeJob.id, rating, comment) {
                                finishServiceOnRating = false
                                viewModel.showAlert(
                                    "Aviso",
                                    "Servicio finalizado. Esperando la calificación del profesional."
                                )
                            }
                        } else {
                            viewModel.submitRating(
                                activeJob.id,
                                rating.toFloat(),
                                comment,
                                isClientRating = true,
                                onSuccess = {
                                    viewModel.showAlert("Aviso", "¡Calificación enviada exitosamente!")
                                }
                            )
                            // Si con esta calificación se completa (el pro ya había
                            // finalizado), el efecto de arriba retorna al home;
                            // si no, el estado se refleja en pantalla.
                            clientJustRated = true
                        }
                    }
                )
            }
        }

        if (showPortfolioDialog) {
            val gallery = viewModel.getPortfolio(portfolioProId ?: "pro_125")
            AlertDialog(
                onDismissRequest = { showPortfolioDialog = false },
                confirmButton = {
                    Button(
                        onClick = { showPortfolioDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = PolishBlue)
                    ) {
                        Text("Cerrar", fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = Color.White,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = PolishGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Portafolio de $portfolioProName 📂",
                            fontWeight = FontWeight.Bold,
                            color = PolishDark,
                            fontSize = 16.sp
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Trabajos y certificaciones verificadas:",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )

                        if (gallery.isEmpty()) {
                            Text(
                                text = "No se cargaron muestras de fotos previas.",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }

                        gallery.forEach { item ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, PolishBorder, RoundedCornerShape(12.dp))
                            ) {
                                Column {
                                    AsyncImage(
                                        model = item.imageUrl,
                                        contentDescription = item.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(110.dp)
                                    )
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = item.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = PolishDark
                                        )
                                        Text(
                                            text = item.desc,
                                            fontSize = 10.sp,
                                            color = Color(0xFF64748B),
                                            lineHeight = 12.sp,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            )
        }

    }

    if (showProfileDetailForBid != null) {
        val currentBid = showProfileDetailForBid!!
        ProfessionalProfileDetailScreen(
            bid = currentBid,
            viewModel = viewModel,
            onBack = { showProfileDetailForBid = null },
            onAccept = {
                viewModel.acceptBid(currentBid.id)
                showProfileDetailForBid = null
            },
            onCounter = { amount ->
                var counterVal = amount
                if (counterVal < 1000.0) counterVal *= 1000.0
                viewModel.proposeCounterOffer(currentBid.id, counterVal)
                showProfileDetailForBid = null
            },
            onReject = {
                viewModel.rejectBid(currentBid.id)
                viewModel.refreshSelectedJobBids(currentBid.jobId)
                showProfileDetailForBid = null
            },
            onContact = {
                showProfileDetailForBid = null
                viewModel.startChatWithProfessional(
                    currentBid.professionalId,
                    currentBid.professionalName,
                    selectedJob?.category ?: "Servicio",
                    jobId = currentBid.jobId,
                    onStarted = { chatId -> onOpenChat(chatId) }
                )
            }
        )
    }
    }
}

/**
 * Resumen de la solicitud abierta dentro de la pantalla de ofertas.
 * Replica la información de la tarjeta de "Mis solicitudes" y agrega
 * la acción para cancelar la solicitud.
 */
@Composable
private fun OpenJobRequestCard(
    job: JobRequest,
    bidsCount: Int,
    onCancel: () -> Unit
) {
    var showCancelDialog by remember { mutableStateOf(false) }
    val (statusLabel, statusBg, statusTint) = when (job.status.uppercase()) {
        "ACCEPTED", "ACEPTADO" -> Triple("Aceptado", Color(0xFFEFF6FF), Color(0xFF2563EB))
        "IN_PROGRESS", "EN_PROGRESO" -> Triple("En progreso", Color(0xFFDCFCE7), Color(0xFF16A34A))
        "CANCELLED", "CANCELADO" -> Triple("Cancelado", Color(0xFFF1F5F9), Color(0xFF64748B))
        else -> Triple("En espera", Color(0xFFFFFBEB), Color(0xFFD97706))
    }
    val cardPrice: Double? = job.suggestedPrice?.takeIf { it > 0 }
        ?: job.budgetMin.takeIf { it > 0 }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, PolishBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(statusBg)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = statusLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = statusTint
                        )
                    }
                    if ((job.source ?: "PUBLISH") == "DIRECTORY") {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFEDE9FE))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "Directa",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF7C3AED)
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = PolishGrayText,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = formatTimestamp(job.createdAt),
                        fontSize = 11.sp,
                        color = PolishGrayText
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.LocalOffer,
                    contentDescription = null,
                    tint = Color(0xFFDB2777),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (bidsCount == 1) "1 oferta" else "$bidsCount ofertas",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (bidsCount > 0) Color(0xFFDB2777) else PolishGrayText
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = job.title.ifEmpty { job.category },
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = PolishDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (job.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = job.description,
                    fontSize = 12.sp,
                    color = PolishGrayText,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (cardPrice != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Payment,
                        contentDescription = null,
                        tint = Color(0xFF16A34A),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Ofertado: $${NumberFormat.getNumberInstance(Locale.US).format(cardPrice.toLong())}/h",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF16A34A)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = PolishGrayText,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = job.address.ifEmpty {
                        "${job.municipality ?: ""}, ${job.department ?: ""}"
                    },
                    fontSize = 11.sp,
                    color = PolishGrayText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { showCancelDialog = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                border = BorderStroke(1.dp, Color(0xFFDC2626)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Cancelar solicitud",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }

    if (showCancelDialog) {
        GalibuAlertDialog(
            title = "¿Cancelar solicitud?",
            message = "Esta acción no se puede deshacer. Los profesionales dejarán de ver tu solicitud.",
            type = GalibuAlertType.WARNING,
            confirmText = "Sí, cancelar",
            dismissText = "No, volver",
            confirmColor = Color(0xFFDC2626),
            onConfirm = {
                showCancelDialog = false
                onCancel()
            },
            onDismissRequest = { showCancelDialog = false }
        )
    }
}


