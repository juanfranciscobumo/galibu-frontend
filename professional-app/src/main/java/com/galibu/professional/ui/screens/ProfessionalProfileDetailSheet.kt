package com.galibu.professional.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.galibu.core.data.model.Bid
import com.galibu.core.ui.components.CategoryIconCircle
import com.galibu.core.ui.theme.PolishBlue
import com.galibu.core.ui.theme.PolishIce
import com.galibu.professional.ui.viewmodels.ProfessionalViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ProfessionalProfileDetailSheet(
    bid: Bid,
    viewModel: ProfessionalViewModel,
    onDismiss: () -> Unit,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onCounter: (Double) -> Unit
) {
    var showCounterInline by remember { mutableStateOf(false) }
    val professionals by viewModel.professionals.collectAsState()
    val proProfile = professionals.find { it.id == bid.professionalId }

    val categories = remember(proProfile?.workCategory) {
        proProfile?.workCategory?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
    }

    val allJobs by viewModel.allJobs.collectAsState()
    val relatedJob = remember(allJobs, bid.jobId) { allJobs.find { it.id == bid.jobId } }
    val jobCategory = relatedJob?.category ?: ""
    val completedJobs = remember(allJobs, bid.professionalId) {
        allJobs.filter { it.professionalId == bid.professionalId && it.status == "COMPLETED" }
    }

    val completedCount = completedJobs.size
    val totalReviews = proProfile?.completedJobs ?: completedCount
    val rating = proProfile?.rating ?: bid.professionalRating

    val memberDate = remember(proProfile?.createdAt) {
        try {
            "Miembro desde " + java.text.SimpleDateFormat("MMM yyyy", java.util.Locale.forLanguageTag("es-CO"))
                .format(java.util.Date(proProfile?.createdAt ?: System.currentTimeMillis()))
                .replaceFirstChar { it.uppercase() }
        } catch (e: Exception) {
            "Miembro desde 2024"
        }
    }

    val address = remember(proProfile) {
        val m = proProfile?.municipality?.trim() ?: ""
        val d = proProfile?.department?.trim() ?: ""
        val a = proProfile?.address?.trim() ?: ""
        when {
            a.isNotEmpty() && m.isNotEmpty() -> "$a, $m, $d"
            m.isNotEmpty() && d.isNotEmpty() -> if (m.lowercase() == d.lowercase()) m else "$m, $d"
            m.isNotEmpty() -> m
            d.isNotEmpty() -> d
            else -> "Colombia"
        }
    }

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFFE2E8F0))
                )
            }

            // Close button
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopEnd) {
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar", tint = Color(0xFF64748B), modifier = Modifier.size(22.dp))
                }
            }

            // Profile photo + Name + Location + Member since
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                // Photo
                Box(modifier = Modifier.size(90.dp)) {
                    val photoUrl = proProfile?.profilePhotoUrl
                    val initials = (proProfile?.name ?: bid.professionalName).split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString("").uppercase()

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(PolishIce)
                            .border(3.dp, PolishBlue, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!photoUrl.isNullOrEmpty()) {
                            val docModel = remember(photoUrl) {
                                if (photoUrl.startsWith("data:image/")) {
                                    try {
                                        val base64Data = photoUrl.substringAfter("base64,")
                                        val decodedBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                                        android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                                    } catch (e: Exception) { photoUrl }
                                } else { photoUrl }
                            }
                            AsyncImage(model = docModel, contentDescription = "Foto", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
                        } else {
                            Text(text = initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 28.sp)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.BottomEnd)
                            .background(Color(0xFF22C55E), CircleShape)
                            .border(3.dp, Color.White, CircleShape)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = proProfile?.name ?: bid.professionalName, color = Color(0xFF0F172A), fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 24.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = address, color = Color(0xFF64748B), fontSize = 12.sp, lineHeight = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = memberDate, color = Color(0xFF64748B), fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Stats row: Rating | Services
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "%.1f".format(rating), color = Color(0xFF0F172A), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(text = "${totalReviews} reseñas", color = Color(0xFF64748B), fontSize = 12.sp)
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.BusinessCenter, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "$completedCount", color = Color(0xFF0F172A), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Servicios realizados", color = Color(0xFF64748B), fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Pricing row: Tarifa base | Precio de negociación
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    val hourlyRate = proProfile?.hourlyRate ?: bid.amount
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Tarifa base", color = Color(0xFF64748B), fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "$${NumberFormat.getNumberInstance(Locale.US).format(hourlyRate.toInt())} COP", color = Color(0xFF0066FF), fontSize = 18.sp, fontWeight = FontWeight.Black)
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Precio de negociación", color = Color(0xFF64748B), fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        val minPrice = (hourlyRate * 0.8).toInt()
                        val maxPrice = hourlyRate.toInt()
                        Text(text = "$${NumberFormat.getNumberInstance(Locale.US).format(minPrice)} - $${NumberFormat.getNumberInstance(Locale.US).format(maxPrice)} COP", color = Color(0xFF059669), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Detalle de la solicitud
            Text(text = "Detalle de la solicitud", color = Color(0xFF0F172A), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    CategoryIconCircle(
                        category = jobCategory.ifEmpty { "Servicio general" },
                        size = 44.dp,
                        iconSize = 22.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = jobCategory.ifEmpty { "Servicio general" }, color = Color(0xFF0F172A), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Solicitud de servicio para ${bid.professionalName}.", color = Color(0xFF64748B), fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Calificaciones
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Calificaciones", color = Color(0xFF0F172A), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "%.1f (%d)".format(rating, totalReviews), color = Color(0xFF0F172A), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Star breakdown
            val breakdown = listOf(
                5 to if (totalReviews > 0) 0.65f else 0f,
                4 to if (totalReviews > 0) 0.25f else 0f,
                3 to if (totalReviews > 0) 0.07f else 0f,
                2 to if (totalReviews > 0) 0.02f else 0f,
                1 to if (totalReviews > 0) 0.01f else 0f
            )
            breakdown.forEach { (stars, pct) ->
                val count = (pct * totalReviews).toInt()
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "$stars", color = Color(0xFF64748B), fontSize = 13.sp, modifier = Modifier.width(16.dp))
                    Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    LinearProgressIndicator(progress = { pct }, modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)), color = Color(0xFFF59E0B), trackColor = Color(0xFFE2E8F0))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "${(pct * 100).toInt()}% ($count)", color = Color(0xFF64748B), fontSize = 12.sp, modifier = Modifier.width(60.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Comentarios recientes
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Comentarios recientes", color = Color(0xFF0F172A), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(text = "Ver todos", color = Color(0xFF0066FF), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(10.dp))

            val reviews = completedJobs.filter { !it.clientReviewOfPro.isNullOrEmpty() }.take(3)
            if (reviews.isNotEmpty()) {
                reviews.forEach { job ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFFDBEAFE)), contentAlignment = Alignment.Center) {
                                        Text(text = job.clientName.take(1).uppercase(), color = Color(0xFF2563EB), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(text = job.clientName, color = Color(0xFF0F172A), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            repeat(5) { i ->
                                                Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = if (i < (job.clientRatingOfPro ?: 5f).toInt()) Color(0xFFF59E0B) else Color(0xFFE2E8F0), modifier = Modifier.size(10.dp))
                                            }
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFFDCFCE7)).padding(horizontal = 4.dp, vertical = 1.dp)) {
                                                Text(text = "Verificado", color = Color(0xFF059669), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                                val timeAgo = remember(job.createdAt) {
                                    val diff = System.currentTimeMillis() - job.createdAt
                                    val days = diff / (1000 * 60 * 60 * 24)
                                    when {
                                        days < 7 -> "Hace ${days} días"
                                        days < 30 -> "Hace ${days / 7} semana${if (days / 7 > 1) "s" else ""}"
                                        else -> "Hace ${days / 30} mes${if (days / 30 > 1) "es" else ""}"
                                    }
                                }
                                Text(text = timeAgo, color = Color(0xFF94A3B8), fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "\"${job.clientReviewOfPro}\"", color = Color(0xFF475569), fontSize = 13.sp, lineHeight = 18.sp)
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFDBEAFE)), contentAlignment = Alignment.Center) {
                            Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "Aún no hay reseñas. Sé el primero en valorar su servicio.", color = Color(0xFF64748B), fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Inline counter-offer: tap Contraofertar -> se oculta el botón y aparece input + Enviar
            androidx.compose.animation.AnimatedVisibility(visible = !showCounterInline) {
                OutlinedButton(
                    onClick = { showCounterInline = true },
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFF9800)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF9800)),
                    contentPadding = PaddingValues(vertical = 14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Contraofertar", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
            androidx.compose.animation.AnimatedVisibility(visible = showCounterInline) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        com.galibu.core.ui.components.InlineCounterOfferInput(
                            initialAmount = 0.0,
                            accentColor = Color(0xFF0066FF),
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

            // Bottom buttons: Aceptar + Chat
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onAccept,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                    contentPadding = PaddingValues(vertical = 14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Aceptar", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onReject,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF0066FF)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0066FF)),
                    contentPadding = PaddingValues(vertical = 14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Chat", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
