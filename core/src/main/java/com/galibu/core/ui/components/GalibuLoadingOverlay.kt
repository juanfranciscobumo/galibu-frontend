package com.galibu.core.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.galibu.core.ui.theme.PolishBlue
import com.galibu.core.ui.theme.PolishNavy

/**
 * Único overlay de carga global para cliente y profesional.
 * Evita spinners duplicados dentro de botones cuando ya hay un proceso bloqueante.
 *
 * Se dibuja en su propia ventana de diálogo para quedar SIEMPRE por encima de
 * las hojas inferiores y de otros diálogos (por ejemplo, la hoja de
 * calificación), sin afectar el resto de la app.
 */
@Composable
fun GalibuLoadingOverlay(
    isLoading: Boolean,
    message: String? = null,
    progressColor: Color = PolishBlue
) {
    // Mientras hay un proceso bloqueante en curso, la navegación atrás también
    // queda bloqueada para no abandonar la pantalla a mitad de la operación.
    BackHandler(enabled = isLoading) { }

    if (!isLoading) return

    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x990F172A))
                .clickable(enabled = true, onClick = {})
                .testTag("global_loading_overlay"),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                modifier = Modifier.width(260.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = progressColor,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = message?.takeIf { it.isNotBlank() } ?: "Procesando...",
                        color = PolishNavy,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
