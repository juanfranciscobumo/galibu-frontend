package com.galibu.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.galibu.core.ui.theme.PolishBlue
import com.galibu.core.ui.utils.capitalizeFirstLetter

/**
 * Hoja única de calificación de Galibu, compartida por las apps cliente y
 * profesional.
 *
 * Mantiene el mismo diseño en todas las pantallas: título según quién recibe
 * la calificación ("Calificar al cliente" / "Calificar al profesional"),
 * texto "Comparte tu experiencia con <nombre>." y botón "Finalizar y
 * calificar servicio". El comentario es opcional; las estrellas obligatorias.
 *
 * Siempre se puede cerrar (toque fuera, arrastre o atrás): si se cierra sin
 * enviar no se ejecuta ninguna acción. La validación de estrellas es inline
 * para no abrir otra ventana de diálogo sobre la hoja.
 *
 * @param title Título de la hoja (quién recibe la calificación).
 * @param targetName Nombre de la persona calificada.
 * @param onDismissRequest Toque fuera, arrastre o atrás.
 * @param onSubmit Entrega la calificación (1..5) y el comentario ya recortado.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalibuRatingSheet(
    title: String,
    targetName: String,
    onDismissRequest: () -> Unit,
    onSubmit: (rating: Int, comment: String) -> Unit
) {
    var stars by remember { mutableStateOf(0) }
    var comment by remember { mutableStateOf("") }
    var starError by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .navigationBarsPadding()
                .imePadding()
        ) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Comparte tu experiencia con ${targetName.ifBlank { "el usuario" }}.",
                color = Color(0xFF64748B),
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..5).forEach { star ->
                    IconButton(onClick = {
                        stars = star
                        starError = false
                    }) {
                        Icon(
                            imageVector = if (star <= stars) Icons.Default.Star else Icons.Default.StarOutline,
                            contentDescription = "Estrella $star",
                            tint = if (star <= stars) Color(0xFFF59E0B) else Color(0xFFCBD5E1)
                        )
                    }
                }
            }

            if (starError) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Selecciona una calificación de 1 a 5 estrellas.",
                    color = Color(0xFFEF4444),
                    fontSize = 12.sp
                )
            }

            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it.capitalizeFirstLetter() },
                label = { Text("Comentario (opcional)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (stars <= 0) {
                        starError = true
                        return@Button
                    }
                    onSubmit(stars, comment.trim())
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PolishBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Text(
                    text = "Finalizar y calificar servicio",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
