package com.galibu.professional.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.galibu.core.data.model.Bid
import com.galibu.core.ui.utils.capitalizeFirstLetter
import com.galibu.core.data.model.JobRequest
import com.galibu.core.ui.components.GalibuRatingSheet
import com.galibu.core.ui.components.HireDecisionButtons
import com.galibu.core.ui.components.categoryVisual
import com.galibu.core.ui.theme.*
import com.galibu.professional.ui.viewmodels.ProfessionalViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceNegotiationScreen(
    viewModel: ProfessionalViewModel,
    onOpenChat: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedJob by viewModel.selectedJob.collectAsState()
    val bids by viewModel.selectedJobBids.collectAsState()
    val professionals by viewModel.professionals.collectAsState()

    var showDirectCounterInline by remember { mutableStateOf(false) }
    var showClientRatingSheet by remember { mutableStateOf(false) }

    var showPortfolioDialog by remember { mutableStateOf(false) }
    var portfolioProId by remember { mutableStateOf<String?>(null) }
    var portfolioProName by remember { mutableStateOf("") }
    var showProfileDetailForBid by remember { mutableStateOf<Bid?>(null) }
    val hasAssignedProfessional = selectedJob?.professionalId?.isNotEmpty() == true
    val isDirectNegotiation =
        selectedJob?.let { it.status == "NEGOTIATING" && !it.professionalId.isNullOrEmpty() }
            ?: false


    Column(
        modifier = modifier
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
            Text(
                text = "Ofertas de Profesionales",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = PolishDark
            )
        }
        selectedJob?.let { job ->
            val professionalUser = professionals.find { it.id == job.professionalId }
            val baseRateVal = job.proHourlyRate ?: professionalUser?.hourlyRate ?: job.budgetMin
            val proposedRateValue = when (job.counterOfferSender) {
                "CLIENT" -> job.counterOfferAmount ?: job.budgetMin
                "PROFESSIONAL" -> job.counterOfferAmount ?: baseRateVal
                else -> baseRateVal
            }
            val isWaitingForClient = job.counterOfferSender == "PROFESSIONAL"
            Card(
                colors = CardDefaults.cardColors(containerColor = PolishBlue),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Icon(
                        imageVector = Icons.Default.EmojiObjects,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.1f),
                        modifier = Modifier.size(140.dp).align(Alignment.BottomEnd)
                            .offset(x = 20.dp, y = 20.dp)
                    )
                    Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                        val heroCategory = (job.category ?: "Servicio")
                            .split(",")
                            .firstOrNull()
                            ?.trim()
                            ?.takeIf { it.isNotEmpty() }
                            ?: "Servicio"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Oferta del cliente",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "$${
                                            NumberFormat.getNumberInstance(Locale.US)
                                                .format(job.budgetMin)
                                        } COP",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = job.title,
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = job.description,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
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
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (isDirectNegotiation) {
                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(
                                color = Color.White.copy(alpha = 0.28f),
                                thickness = 1.dp
                            )
                            Spacer(modifier = Modifier.height(12.dp))

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
                                    text = if (isWaitingForClient) {
                                        "Contraoferta enviada · Esperando respuesta del cliente."
                                    } else {
                                        "Solicitud directa del cliente · Acepta o contraoferta."
                                    },
                                    color = Color.White.copy(alpha = 0.92f),
                                    fontSize = 12.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            val rejectDirect = {
                                viewModel.rejectDirectHire(job.id) {
                                    viewModel.showAlert(
                                        "Aviso",
                                        "Solicitud rechazada exitosamente"
                                    )
                                }
                            }
                            if (!isWaitingForClient) {
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
                                    onReject = rejectDirect,
                                    acceptLabel = "Aceptar oferta",
                                    rejectLabel = "Rechazar solicitud"
                                )
                            } else {
                                HireDecisionButtons(
                                    onAccept = null,
                                    onCounter = null,
                                    onReject = rejectDirect,
                                    rejectLabel = "Rechazar solicitud"
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
                                            initialAmount = 0.0,
                                            accentColor = Color(0xFF1D4ED8),
                                            onSend = { amount ->
                                                viewModel.submitCounterOfferDirectHire(
                                                    job.id,
                                                    amount,
                                                    isClient = false
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

            if (hasAssignedProfessional) {
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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            val detailStatusLabel = when (job.status) {
                                "ACCEPTED" -> "Trabajo en curso"
                                "IN_PROGRESS" -> "En progreso"
                                "PENDING_RATING" -> "Por calificar"
                                "COMPLETED" -> "Completado"
                                "CANCELLED" -> "Cancelado"
                                else -> if (isWaitingForClient) "Contraoferta enviada" else "Oferta recibida"
                            }
                            val detailStatusColor = when (job.status) {
                                "ACCEPTED" -> Color(0xFF2563EB)
                                "IN_PROGRESS" -> Color(0xFF16A34A)
                                "PENDING_RATING" -> Color(0xFFD97706)
                                "COMPLETED" -> Color(0xFF475569)
                                "CANCELLED" -> Color(0xFF64748B)
                                else -> Color(0xFFEA580C)
                            }
                            val detailStatusBg = when (job.status) {
                                "ACCEPTED" -> Color(0xFFEFF6FF)
                                "IN_PROGRESS" -> Color(0xFFDCFCE7)
                                "PENDING_RATING" -> Color(0xFFFFFBEB)
                                "COMPLETED" -> Color(0xFFF1F5F9)
                                "CANCELLED" -> Color(0xFFF1F5F9)
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
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Mi precio (cliente)",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                                Text(
                                    text = "$${
                                        NumberFormat.getNumberInstance(Locale.US)
                                            .format(job.budgetMin)
                                    }/h",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2563EB)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = job.title,
                            color = Color(0xFF1E293B),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${job.category} · ${formatTimeAgo(job.createdAt)}", color = Color(0xFF64748B), fontSize = 13.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val proName = job.professionalName ?: "Profesional"
                            val initials = proName.split(" ").filter { it.isNotEmpty() }.take(2)
                                .joinToString("") { it.first().uppercase() }
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEFF6FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    initials,
                                    color = Color(0xFF2563EB),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = proName,
                                    color = Color(0xFF1E293B),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFFFB000),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "4.8 (142 reseñas)",
                                        color = Color(0xFF64748B),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Tarifa del profesional",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                                Text(
                                    text = "$${
                                        NumberFormat.getNumberInstance(Locale.US)
                                            .format(baseRateVal)
                                    }/h",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF64748B)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (job.status == "NEGOTIATING") {
                                        if (job.counterOfferSender == "CLIENT") "Tú propusiste" else "Profesional propone"
                                    } else {
                                        "Tarifa acordada"
                                    },
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                                Text(
                                    text = "$${
                                        NumberFormat.getNumberInstance(Locale.US)
                                            .format(proposedRateValue)
                                    }/h",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEA580C)
                                )
                            }
                        }

                        if (job.status == "ACCEPTED") {
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

                                Button(
                                    onClick = {
                                        viewModel.payAndCompleteJob(job.id, "Transferencia / Nequi") {
                                            viewModel.showAlert("Aviso", "Servicio finalizado. Ahora puedes calificar al cliente.")
                                        }
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
                            
                            // Botón Cancelar para trabajos ACCEPTED
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

                        if (job.status == "PENDING_RATING" && job.proRatingOfClient == null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { showClientRatingSheet = true },
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
                                Text("Calificar al cliente", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        if (!isDirectNegotiation && selectedJob?.status == "NEGOTIATING" && bids.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        color = PolishBlue,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Buscando más ofertas alrededor",
                            color = PolishDark,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "en tiempo real...",
                            color = Color(0xFF64748B),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        if (!isDirectNegotiation) {
            // Bids Stream List Title (Corrected visibility: Slate-800 instead of White)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Box(
                    modifier = Modifier.size(28.dp).background(Color(0xFFE0E7FF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Gavel,
                        contentDescription = null,
                        tint = PolishBlue,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Pujas entrantes (${bids.size})",
                    color = Color(0xFF1E293B),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            if (bids.isEmpty()) {
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
                        text = "Esperando ofertas",
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
                bids.forEach { bid ->
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
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box {
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
                                            modifier = Modifier.size(52.dp).clip(CircleShape)
                                                .border(2.dp, PolishBlue, CircleShape)
                                        )
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = PolishGreen,
                                            modifier = Modifier.size(16.dp)
                                                .align(Alignment.BottomEnd)
                                                .background(Color.White, CircleShape)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = bid.professionalName,
                                            fontWeight = FontWeight.Bold,
                                            color = PolishDark,
                                            fontSize = 14.sp
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = Color(0xFFE09F00),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "${bid.professionalRating} • Verificado ✅",
                                                color = PolishGreen,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Tarifa propuesta",
                                        fontSize = 10.sp,
                                        color = Color(0xFF64748B)
                                    )
                                    Text(
                                        text = "$${
                                            NumberFormat.getNumberInstance(Locale.US)
                                                .format(bid.amount)
                                        } COP",

                                        fontWeight = FontWeight.Black,
                                        color = PolishGreen,
                                        fontSize = 18.sp,
                                        textAlign = TextAlign.End,
                                        lineHeight = 20.sp,
                                        modifier = Modifier.testTag("bid_price_${bid.id}")
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                                    .padding(16.dp), verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "“",
                                    color = PolishBlue,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.offset(y = (-8).dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = bid.comment,
                                    color = Color(0xFF475569),
                                    fontSize = 13.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }

                            // Status tags for counters
                            if (bid.status == "COUNTERED_BY_CLIENT") {
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
        if (showProfileDetailForBid != null) {
            val currentBid = showProfileDetailForBid!!
            ProfessionalProfileDetailSheet(
                bid = currentBid,
                viewModel = viewModel,
                onDismiss = { showProfileDetailForBid = null },
                onAccept = {
                    viewModel.acceptBid(currentBid.id)
                    showProfileDetailForBid = null
                },
                onReject = {
                    viewModel.rejectBid(currentBid.id)
                    showProfileDetailForBid = null
                },
                onCounter = { amount ->
                    var counterVal = amount
                    if (counterVal < 1000.0) counterVal *= 1000.0
                    viewModel.proposeCounterOffer(currentBid.id, counterVal)
                    showProfileDetailForBid = null
                }
            )
        }

        // Calificación del cliente: hoja homologada en todas las pantallas.
        if (showClientRatingSheet) {
            selectedJob?.let { activeJob ->
                GalibuRatingSheet(
                    title = "Calificar al cliente",
                    targetName = activeJob.clientName.ifBlank { "el cliente" },
                    onDismissRequest = { showClientRatingSheet = false },
                    onSubmit = { rating, comment ->
                        viewModel.submitRating(
                            activeJob.id,
                            rating.toFloat(),
                            comment,
                            isClientRating = false,
                            onSuccess = {
                                viewModel.showAlert("Aviso", "¡Calificación enviada exitosamente!")
                            }
                        )
                        showClientRatingSheet = false
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
}


