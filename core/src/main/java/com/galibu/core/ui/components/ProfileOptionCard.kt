package com.galibu.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.galibu.core.ui.theme.*

/**
 * Tarjeta de opción de perfil compartida entre el modo cliente y el modo profesional.
 * Icono a la izquierda, título centrado y chevron a la derecha.
 */
@Composable
fun ProfileOptionCard(
    icon: ImageVector,
    iconBgColor: Color,
    iconColor: Color,
    title: String,
    titleColor: Color = Color(0xFF0F172A),
    showChevron: Boolean = true,
    chevronColor: Color = Color(0xFF94A3B8),
    isProminent: Boolean = false,
    onClick: () -> Unit
) {
    val bg = if (isProminent) Color(0xFFEFF6FF) else Color.White
    val borderColor = if (isProminent) PolishBlue else Color(0xFFEFF2F5)
    val tColor = if (isProminent) PolishBlue else titleColor
    val weight = if (isProminent) FontWeight.Bold else FontWeight.SemiBold
    val size = if (isProminent) 16.sp else 15.sp
    val iconSize = if (isProminent) 20.dp else 18.dp
    val boxSize = if (isProminent) 42.dp else 38.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = bg),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, borderColor),
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
                        .size(boxSize)
                        .clip(CircleShape)
                        .background(iconBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(iconSize)
                    )
                }

                Text(
                    text = title,
                    color = tColor,
                    fontSize = size,
                    fontWeight = weight
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
