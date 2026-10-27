package com.galibu.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.galibu.core.ui.theme.PolishBlue
import com.galibu.core.ui.theme.PolishNavy

/** Tipo visual de una alerta de Galibu. */
enum class GalibuAlertType {
    SUCCESS,
    ERROR,
    WARNING,
    INFO
}

private data class AlertVisual(
    val accent: Color,
    val icon: ImageVector
)

private fun visualFor(type: GalibuAlertType): AlertVisual = when (type) {
    GalibuAlertType.SUCCESS -> AlertVisual(Color(0xFF10B981), Icons.Default.Check)
    GalibuAlertType.ERROR -> AlertVisual(Color(0xFFEF4444), Icons.Default.Close)
    GalibuAlertType.WARNING -> AlertVisual(Color(0xFFF59E0B), Icons.Default.Warning)
    GalibuAlertType.INFO -> AlertVisual(Color(0xFF2563EB), Icons.Default.Info)
}

/**
 * Alerta única de Galibu para cliente y profesional.
 *
 * Mantiene el mismo lenguaje visual para success, error, warning e info.
 * Si se envía [dismissText] se muestran dos acciones; si no, solo [confirmText].
 */
@Composable
fun GalibuAlertDialog(
    title: String,
    message: String,
    type: GalibuAlertType = GalibuAlertType.INFO,
    confirmText: String = "Entendido",
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit = onConfirm,
    dismissText: String? = null,
    confirmColor: Color = PolishBlue
) {
    val visual = visualFor(type)

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x990F172A))
                .clickable { onDismissRequest() }
                .padding(horizontal = 24.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AlertBurst(color = visual.accent)
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(visual.accent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = visual.icon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(25.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = title,
                            color = PolishNavy,
                            fontSize = 20.sp,
                            lineHeight = 25.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = message,
                        color = Color(0xFF475569),
                        fontSize = 14.sp,
                        lineHeight = 21.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (dismissText != null) {
                            TextButton(onClick = onDismissRequest) {
                                Text(
                                    text = dismissText,
                                    color = Color(0xFF64748B),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Button(
                            onClick = onConfirm,
                            colors = ButtonDefaults.buttonColors(containerColor = confirmColor),
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = confirmText,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertBurst(color: Color) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val stroke = 2.2.dp.toPx()
        val centerY = size.height / 2f
        drawLine(
            color = color,
            start = Offset(size.width * 0.04f, centerY),
            end = Offset(size.width * 0.32f, centerY),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.18f, size.height * 0.16f),
            end = Offset(size.width * 0.40f, size.height * 0.34f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.18f, size.height * 0.84f),
            end = Offset(size.width * 0.40f, size.height * 0.66f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}
