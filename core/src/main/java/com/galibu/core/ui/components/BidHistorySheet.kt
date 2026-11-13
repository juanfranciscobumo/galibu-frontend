package com.galibu.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.galibu.core.data.remote.BidEventDto
import com.galibu.core.ui.theme.PolishBlue
import com.galibu.core.ui.viewmodels.SharedViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Historial de rondas de una oferta. Se carga desde el backend porque una
 * solicitud puede tener varias ofertas y el chat del job no debe mezclarlas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BidHistorySheet(
    bidId: String,
    viewModel: SharedViewModel,
    onDismiss: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    var events by remember(bidId) { mutableStateOf<List<BidEventDto>>(emptyList()) }
    var loading by remember(bidId) { mutableStateOf(true) }
    val dateFormat = remember {
        SimpleDateFormat("dd MMM · HH:mm", Locale("es", "CO"))
    }
    val amountFormat = remember { NumberFormat.getNumberInstance(Locale.US) }

    LaunchedEffect(bidId) {
        loading = true
        events = viewModel.repository.getBidHistory(bidId)
        loading = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 24.dp)
                .navigationBarsPadding()
                .testTag("bid_history_sheet")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = PolishBlue,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Historial de la negociación",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = Color(0xFF64748B)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Cada ronda queda registrada: quién propuso, el valor y cuándo.",
                fontSize = 12.sp,
                color = Color(0xFF64748B)
            )
            Spacer(modifier = Modifier.height(18.dp))

            when {
                loading -> Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PolishBlue, strokeWidth = 2.dp)
                }
                events.isEmpty() -> Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aún no hay movimientos registrados para esta oferta.",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )
                }
                else -> events.forEachIndexed { index, event ->
                    TimelineItem(
                        event = event,
                        isMine = event.authorId == currentUser?.id,
                        dateFormat = dateFormat,
                        amountFormat = amountFormat,
                        isLast = index == events.lastIndex
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineItem(
    event: BidEventDto,
    isMine: Boolean,
    dateFormat: SimpleDateFormat,
    amountFormat: NumberFormat,
    isLast: Boolean
) {
    val (icon, color, label) = when (event.action) {
        "ACCEPT" -> Triple(Icons.Default.CheckCircle, Color(0xFF16A34A), "Precio aceptado")
        "REJECT" -> Triple(Icons.Default.Close, Color(0xFFDC2626), "Oferta rechazada")
        "COUNTER" -> Triple(Icons.Default.SwapHoriz, Color(0xFF7C3AED), "Contraoferta")
        else -> Triple(Icons.Default.LocalOffer, Color(0xFF2563EB), "Oferta inicial")
    }
    val author = when {
        isMine -> "Tú"
        event.authorRole == "CLIENT" -> "Cliente"
        else -> "Profesional"
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(color.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(56.dp)
                        .background(Color(0xFFE2E8F0))
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.padding(bottom = if (isLast) 0.dp else 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFF1E293B)
                )
                if (event.round > 0) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFF3E8FF), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Ronda ${event.round}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF7E22CE)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$author · ${dateFormat.format(Date(event.createdAt))}",
                fontSize = 11.sp,
                color = Color(0xFF64748B)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$${amountFormat.format(event.amount)} COP",
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF1E293B)
            )
            if (!event.note.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = event.note.orEmpty(),
                    fontSize = 11.sp,
                    color = Color(0xFF475569),
                    lineHeight = 15.sp
                )
            }
        }
    }
}
