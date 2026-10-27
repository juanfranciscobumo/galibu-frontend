package com.galibu.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Acciones homologadas para aceptar, contraofertar o rechazar una negociación.
 * Aceptar: verde sólido. Contraofertar: naranja outline. Rechazar: rojo outline.
 */
@Composable
fun HireDecisionButtons(
    onAccept: (() -> Unit)?,
    onCounter: (() -> Unit)?,
    onReject: () -> Unit,
    modifier: Modifier = Modifier,
    acceptLabel: String = "Aceptar oferta",
    counterLabel: String = "Contraofertar",
    rejectLabel: String = "Rechazar",
    acceptContainerColor: Color = Color(0xFF16A34A)
) {
    if (onAccept != null) {
        Button(
            onClick = onAccept,
            colors = ButtonDefaults.buttonColors(containerColor = acceptContainerColor),
            shape = RoundedCornerShape(14.dp),
            modifier = modifier.fillMaxWidth().height(48.dp)
        ) {
            Text(
                text = acceptLabel,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }

    if (onCounter != null) {
        OutlinedButton(
            onClick = onCounter,
            border = BorderStroke(1.5.dp, Color(0xFFFF9800)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text(
                text = counterLabel,
                color = Color(0xFFFF9800),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }

    OutlinedButton(
        onClick = onReject,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
        border = BorderStroke(1.5.dp, Color(0xFFDC2626)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().height(48.dp)
    ) {
        Text(
            text = rejectLabel,
            color = Color(0xFFDC2626),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
