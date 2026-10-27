package com.galibu.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.galibu.core.ui.theme.PolishBlue
import com.galibu.core.ui.utils.PriceUtils

/**
 * Campo en línea para contraofertar dentro de la misma pantalla.
 *
 * Comportamiento pedido:
 * - Al dar tap a "Contraofertar" ese botón desaparece y aparece este bloque:
 *   [input valor] + [botón Enviar oferta].
 *
 * @param initialAmount valor precargado (oferta actual).
 * @param onSend llamado con el monto validado (> 0).
 * @param onCancel colapsa el campo (volver a mostrar botón Contraofertar).
 * @param sendLabel texto del botón de envío (default "Enviar oferta").
 * @param accentColor color del botón enviar (azul cliente / verde pro).
 */
@Composable
fun InlineCounterOfferInput(
    initialAmount: Double,
    onSend: (Double) -> Unit,
    onCancel: () -> Unit = {},
    sendLabel: String = "Enviar oferta",
    accentColor: Color = PolishBlue,
    modifier: Modifier = Modifier
) {
    var raw by remember(initialAmount) {
        mutableStateOf(
            if (initialAmount > 0) initialAmount.toInt().toString() else ""
        )
    }
    val amount = PriceUtils.cleanPrice(raw).toDoubleOrNull() ?: 0.0
    val isValid = amount >= 1000.0

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Ingresa el nuevo valor de la oferta (COP)",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF475569)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = PriceUtils.formatThousands(raw),
                onValueChange = { input ->
                    val clean = input.filter { it.isDigit() }
                    if (clean.length <= 9) raw = clean
                },
                placeholder = { Text("Ej: 80.000", fontSize = 13.sp) },
                prefix = { Text("$", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    focusedTextColor = Color(0xFF1E293B),
                    unfocusedTextColor = Color(0xFF1E293B)
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("inline_counter_input")
            )
            Button(
                onClick = {
                    var v = amount
                    // Compat: si escriben "80" interpretarlo como miles.
                    if (v in 1.0..999.99) v *= 1000.0
                    if (v > 0) onSend(v)
                },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("inline_send_offer")
            ) {
                Text(
                    text = sendLabel,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.White,
                    maxLines = 1
                )
            }
        }
        if (!isValid && raw.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Mínimo $1.000 COP",
                fontSize = 11.sp,
                color = Color(0xFFDC2626)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedButton(
            onClick = onCancel,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Cancelar",
                fontSize = 12.sp,
                color = Color(0xFF64748B)
            )
        }
        Spacer(modifier = Modifier.width(0.dp))
    }
}
