package com.galibu.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import java.text.NumberFormat
import java.util.Locale

/**
 * Shared counter-offer bottom sheet used by both the client and the professional
 * apps. Shows the current offer, quick adjustments, a price input, a RateAssist
 * analysis and Cancel / Propose actions.
 *
 * @param offerLabel label of the reference offer ("Oferta del profesional:" / "Oferta del cliente:")
 * @param descriptionText helper text under the title (mentions who decides)
 * @param currentOffer amount of the offer being countered
 * @param category service category (RateAssist + ranges)
 * @param city city used in the RateAssist average text
 * @param initialAmount prefilled counter-offer value
 * @param onDismiss close without proposing
 * @param onSubmit called with the parsed counter-offer amount (> 0)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CounterOfferSheet(
    offerLabel: String,
    descriptionText: String,
    currentOffer: Double,
    category: String,
    city: String,
    initialAmount: Double,
    onDismiss: () -> Unit,
    onSubmit: (Double) -> Unit
) {
    var raw by remember(initialAmount) { mutableStateOf(initialAmount.toInt().toString()) }
    val amount = PriceUtils.cleanPrice(raw).toDoubleOrNull() ?: 0.0
    val (rangeMin, rangeMax) = remember(category) { RateAssistData.rangeFor(category) }
    val verdict = remember(amount, rangeMin, rangeMax) {
        RateAssistData.verdictFor(amount, rangeMin, rangeMax)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .navigationBarsPadding()
                .imePadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Negociar Tarifa 🔄",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = Color(0xFF64748B)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = descriptionText,
                color = Color(0xFF64748B),
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = offerLabel,
                    fontSize = 14.sp,
                    color = Color(0xFF475569)
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$${
                            NumberFormat.getNumberInstance(Locale.US).format(currentOffer)
                        }",
                        color = Color(0xFF0F172A),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "COP",
                        color = Color(0xFF64748B),
                        fontSize = 13.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Ajuste rápido:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF475569),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(-10000, -5000, 5000, 10000).forEach { adjustment ->
                    val positive = adjustment > 0
                    val accent = if (positive) Color(0xFF059669) else Color(0xFFDC2626)
                    OutlinedButton(
                        onClick = {
                            val current = PriceUtils.cleanPrice(raw).toDoubleOrNull() ?: initialAmount
                            val newVal = (current + adjustment).coerceAtLeast(10000.0)
                            raw = PriceUtils.formatThousands(newVal.toInt().toString())
                        },
                        border = BorderStroke(width = 1.dp, color = accent),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = accent),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "${if (positive) "+$" else "-$"}${Math.abs(adjustment) / 1000}.000",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = PriceUtils.formatThousands(raw),
                onValueChange = { input ->
                    val clean = input.filter { it.isDigit() }
                    if (clean.length <= 9) {
                        raw = PriceUtils.formatThousands(clean)
                    }
                },
                label = { Text("Tu Tarifa de Contraoferta (COP)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PolishBlue,
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    focusedTextColor = Color(0xFF1E293B),
                    unfocusedTextColor = Color(0xFF1E293B),
                    focusedLabelColor = PolishBlue,
                    unfocusedLabelColor = Color(0xFF64748B)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("counter_amount_input")
            )
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "RateAssist (IA Analista)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    val (verdictText, verdictColor) = when (verdict) {
                        RateAssistData.Verdict.Low ->
                            "BAJA ⚠️ (Es probable que ignoren esta oferta)" to Color(0xFFD97706)
                        RateAssistData.Verdict.Fair ->
                            "COMPETITIVA ✅ (Velocidad de respuesta óptima)" to Color(0xFF059669)
                        RateAssistData.Verdict.High ->
                            "PREMIUM ⭐ (Atracción inmediata de especialistas estrella)" to Color(0xFF059669)
                    }
                    Text(
                        text = "Categoría: $category | Oferta: $verdictText",
                        fontSize = 11.sp,
                        color = verdictColor,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "El promedio local en $city para $category oscila entre $${
                            PriceUtils.formatThousands(rangeMin.toString())
                        } y $${
                            PriceUtils.formatThousands(rangeMax.toString())
                        } COP dependiendo de la complejidad exacta.",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B),
                        lineHeight = 15.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF64748B)
                    ),
                    contentPadding = PaddingValues(vertical = 14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Cancelar",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Button(
                    onClick = { if (amount > 0.0) onSubmit(amount) },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = PolishBlue),
                    contentPadding = PaddingValues(vertical = 14.dp),
                    modifier = Modifier
                        .weight(1.5f)
                        .testTag("submit_counter_offer")
                ) {
                    Text(
                        text = "Proponer Contraoferta",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
