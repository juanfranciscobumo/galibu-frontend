package com.galibu.client.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.galibu.core.data.model.Bid
import com.galibu.core.ui.components.PrimaryContactButton
import com.galibu.core.data.remote.RatingDto
import com.galibu.core.ui.theme.PolishBlue
import com.galibu.core.ui.theme.PolishCard
import com.galibu.core.ui.theme.PolishDark
import com.galibu.core.ui.theme.PolishGreen
import com.galibu.core.ui.theme.PolishGrayText
import com.galibu.core.ui.theme.PolishIce
import com.galibu.core.ui.utils.ImageUtils
import com.galibu.client.ui.viewmodels.ClientViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
private fun DetailInfoRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PolishDark,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            color = PolishDark,
            fontSize = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DetailStatColumn(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFFE0E7FF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PolishDark,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            color = PolishDark,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Text(
            text = subtitle,
            color = Color(0xFF64748B),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfessionalProfileDetailScreen(
    bid: Bid,
    viewModel: ClientViewModel,
    onBack: () -> Unit,
    onAccept: () -> Unit,
    onCounter: (Double) -> Unit,
    onReject: () -> Unit,
    onContact: () -> Unit
) {
    var showCounterInline by remember { mutableStateOf(false) }
    var showRatingsHistory by remember { mutableStateOf(false) }
    var proRatings by remember { mutableStateOf<List<RatingDto>>(emptyList()) }
    val professionals by viewModel.professionals.collectAsState()
    val selectedJob by viewModel.selectedJob.collectAsState()
    val proProfile = professionals.find { it.id == bid.professionalId }
    var portfolioPreview by remember { mutableStateOf<String?>(null) }
    val portfolioPhotos = remember(proProfile?.workPhotos) {
        proProfile?.workPhotos
            ?.split("|")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?: emptyList()
    }
    val proWorkDescription = proProfile?.workDescription?.trim().orEmpty()

    LaunchedEffect(bid.professionalId) {
        proRatings = viewModel.repository.getUserRatings(bid.professionalId)
    }

    val avgRating = if (proRatings.isNotEmpty()) {
        (proRatings.map { it.stars }.average().toFloat())
    } else {
        bid.professionalRating.takeIf { it > 0 }
            ?: proProfile?.rating?.takeIf { it > 0 } ?: 5.0f
    }
    val reviewsCount = if (proRatings.isNotEmpty()) proRatings.size else proProfile?.completedJobs?.takeIf { it > 0 }
    val proCategory = proProfile?.workCategory?.takeIf { !it.isNullOrBlank() }
        ?: selectedJob?.category?.takeIf { it.isNotBlank() } ?: "Servicio"
    val proMuni = proProfile?.municipality?.takeIf { !it.isNullOrBlank() }
    val proDept = proProfile?.department?.takeIf { !it.isNullOrBlank() }
    val proLocation = when {
        proMuni != null && proDept != null && !proMuni.equals(proDept, ignoreCase = true) -> "$proMuni, $proDept"
        proMuni != null -> proMuni
        proDept != null -> proDept
        else -> selectedJob?.address ?: ""
    }
    val fivePct = proRatings.takeIf { it.isNotEmpty() }?.let { list ->
        (list.count { it.stars == 5 } * 100f / list.size).toInt()
    } ?: ((avgRating - 4f) * 100f).toInt().coerceIn(0, 100)
    val fourPct = proRatings.takeIf { it.isNotEmpty() }?.let { list ->
        (list.count { it.stars == 4 } * 100f / list.size).toInt()
    } ?: if (avgRating >= 4f) 100 - fivePct else 0
    val threePct = proRatings.takeIf { it.isNotEmpty() }?.let { list ->
        (list.count { it.stars == 3 } * 100f / list.size).toInt()
    } ?: 0
    val twoPct = proRatings.takeIf { it.isNotEmpty() }?.let { list ->
        (list.count { it.stars == 2 } * 100f / list.size).toInt()
    } ?: 0
    val onePct = proRatings.takeIf { it.isNotEmpty() }?.let { list ->
        (list.count { it.stars == 1 } * 100f / list.size).toInt()
    } ?: 0
    fun barPct(star: Int) = when (star) {
        5 -> fivePct
        4 -> fourPct
        3 -> threePct
        2 -> twoPct
        else -> onePct
    }

    val docModel = remember(bid.professionalPhoto) {
        decodePhotoModel(bid.professionalPhoto)
    }

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
            // Header
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

            // Profile header (tap para ver historial de reseñas)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showRatingsHistory = true },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(PolishIce)
                            .border(3.dp, PolishBlue, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = docModel,
                            contentDescription = bid.professionalName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .align(Alignment.BottomEnd)
                            .background(Color(0xFF22C55E), CircleShape)
                            .border(2.dp, Color.White, CircleShape)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    val nameParts = bid.professionalName.trim().split(Regex("\\\\s+")).filter { it.isNotBlank() }
                    val displayName = when {
                        nameParts.size >= 4 -> "${nameParts.take(2).joinToString(" ")}\\n${nameParts.drop(2).joinToString(" ")}"
                        nameParts.size == 3 -> "${nameParts.first()}\\n${nameParts.drop(1).joinToString(" ")}"
                        else -> bid.professionalName
                    }
                    Text(
                        text = displayName,
                        fontWeight = FontWeight.Bold,
                        color = PolishDark,
                        fontSize = 20.sp,
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
                            color = PolishDark,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (reviewsCount != null) {
                            Text(
                                text = " ($reviewsCount reseñas)",
                                color = Color(0xFF64748B),
                                fontSize = 14.sp
                            )
                        }
                    }
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
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Ver reseñas",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Categoría
            if (!proCategory.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.BusinessCenter,
                        contentDescription = null,
                        tint = PolishBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = proCategory,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = PolishBlue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Ubicación
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = PolishGrayText,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = proLocation.ifEmpty { "Ubicación no disponible" },
                    fontSize = 14.sp,
                    color = PolishGrayText
                )
            }

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

            if (portfolioPhotos.isNotEmpty()) {
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
                    portfolioPhotos.forEach { photo ->
                        AsyncImage(
                            model = ImageUtils.getCoilModel(photo),
                            contentDescription = "Trabajo del portafolio",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(116.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { portfolioPreview = photo }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))


            // Price + Contact
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$${
                                NumberFormat.getNumberInstance(Locale.US).format(bid.amount)
                            }",
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
                        text = "Precio de la oferta",
                        color = Color(0xFF64748B),
                        fontSize = 12.sp
                    )
                }
                PrimaryContactButton(
                    onClick = onContact,
                    modifier = Modifier.weight(1.3f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(containerColor = PolishGreen),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
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

            Spacer(modifier = Modifier.height(12.dp))

            // Si el profesional aceptó de inmediato el precio ofertado (sin contraoferta),
            // no hay nada que contraofertar: se oculta el botón.
            val proAcceptedOutright = selectedJob?.suggestedPrice
                ?.takeIf { it > 0 }
                ?.let { it == bid.amount } ?: false
            androidx.compose.animation.AnimatedVisibility(visible = !showCounterInline && !proAcceptedOutright) {
                OutlinedButton(
                    onClick = { showCounterInline = true },
                    border = BorderStroke(1.dp, Color(0xFFFF9800)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF9800)),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Contraofertar",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            androidx.compose.animation.AnimatedVisibility(visible = showCounterInline && !proAcceptedOutright) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        com.galibu.core.ui.components.InlineCounterOfferInput(
                            initialAmount = 0.0,
                            accentColor = PolishBlue,
                            onSend = { amount ->
                                showCounterInline = false
                                onCounter(amount)
                            },
                            onCancel = { showCounterInline = false }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onReject,
                border = BorderStroke(1.dp, Color(0xFFDC2626)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Rechazar oferta",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    // Historial de calificaciones y reseñas del profesional (volver retorna al detalle).
    if (showRatingsHistory) {
        ModalBottomSheet(
            onDismissRequest = { showRatingsHistory = false },
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
                        onClick = { showRatingsHistory = false },
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
                    Column {
                        Text(
                            text = "Calificaciones y reseñas",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = PolishDark
                        )
                        Text(
                            text = bid.professionalName,
                            fontSize = 13.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (proRatings.isEmpty()) {
                    Text(
                        text = "Aún no tiene reseñas.",
                        color = Color(0xFF64748B),
                        fontSize = 14.sp
                    )
                } else {
                    val dateFmt = remember { SimpleDateFormat("dd MMM yyyy", Locale.US) }
                    proRatings.forEach { rating ->
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
                                        text = dateFmt.format(Date(rating.createdAt)),
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

    portfolioPreview?.let { photo ->
        Dialog(
            onDismissRequest = { portfolioPreview = null },
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
                    contentDescription = "Trabajo del portafolio ampliado",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(520.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(32.dp)
                        .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                        .clickable { portfolioPreview = null },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✕",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}