package com.galibu.professional.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.galibu.core.data.model.AppNotification
import com.galibu.core.data.model.JobRequest
import com.galibu.core.ui.theme.*
import com.galibu.professional.ui.viewmodels.ProfessionalViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotificationsAndHistoryScreen(
    viewModel: ProfessionalViewModel,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onOpenJob: (String) -> Unit = {}
) {
    val notifications by viewModel.notifications.collectAsState()
    val allJobs by viewModel.allJobs.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val latestNotifications = remember(notifications) {
        notifications.sortedByDescending { it.timestamp }.take(10)
    }
    val unreadCount = remember(notifications) { notifications.count { !it.isRead } }

    val dateFormatter = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkGrayBg)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Volver
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
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
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Notificaciones",
                color = OffWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
        // Resumen de una sola lista: sin pestañas.
        Text(
            text = if (unreadCount > 0) "Tienes $unreadCount sin leer" else "Estás al día",
            color = LightGray,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Push Notification feed center
        Text(
            text = "Centro de Notificaciones Push 📣",
            color = OffWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (latestNotifications.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.NotificationsNone, contentDescription = null, tint = LightGray, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No hay notificaciones", color = OffWhite, fontWeight = FontWeight.Bold)
                    Text("Aquí se grabarán tus alertas push de ofertas, inicios, finalizaciones y recargas de saldo en tiempo real.", color = LightGray, fontSize = 11.sp, textAlign = TextAlign.Center)
                }
            }
        } else {
            latestNotifications.forEach { item ->
                Card(
                    colors = CardDefaults.cardColors(
                        // No leída: fondo azul muy suave. Leída: blanca, sin sombreado.
                        containerColor = if (item.isRead) CardSurface else Color(0xFFEFF6FF)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .border(1.dp, PolishBorder, RoundedCornerShape(12.dp))
                        .clickable {
                            viewModel.markNotificationAsRead(item.id)
                            item.jobId?.takeIf { it.isNotBlank() }?.let { onOpenJob(it) }
                        }
                        .testTag("notif_item_${item.id}")
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (item.isRead) Color.Transparent else PolishGreen)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.title,
                                    fontWeight = if (item.isRead) FontWeight.Normal else FontWeight.Black,
                                    color = if (item.isRead) LightGray else OffWhite,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = dateFormatter.format(Date(item.timestamp)),
                                    fontSize = 9.sp,
                                    color = Color.Gray
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.body,
                                fontSize = 11.sp,
                                color = Color.Gray,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

    }
}

