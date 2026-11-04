package com.galibu.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.galibu.core.ui.theme.PolishBlue
import com.galibu.core.ui.utils.PriceUtils
import com.galibu.core.ui.utils.capitalizeFirstLetter

/**
 * Diálogo de upgrade explícito (Cliente → profesional).
 *
 * El cliente elige sus categorías (obligatorias); la tarifa por hora es
 * opcional y, si se indica, debe ser al menos el salario mínimo por hora.
 * Al confirmar, el repositorio activa la capacidad profesional vía
 * `POST /users/me/roles` (que en backend otorga las 3 cortesías una sola vez)
 * y persiste la categoría.
 */
@Composable
fun ActivateProfessionalDialog(
    viewModel: com.galibu.core.ui.viewmodels.SharedViewModel,
    suggestions: List<String>,
    onDismiss: () -> Unit,
    onActivated: () -> Unit
) {
    var categoryInput by remember { mutableStateOf("") }
    var hourlyRateInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = {
            Text(
                text = "Ofrecer mis servicios",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Para recibir solicitudes necesitas una categoría profesional. La tarifa por hora es opcional.",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B)
                )

                Text(
                    text = "Categoría(s) de servicio",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF0F172A)
                )
                OutlinedTextField(
                    value = categoryInput,
                    onValueChange = { categoryInput = it.capitalizeFirstLetter() },
                    placeholder = { Text("Ej: Plomería, Electricidad") },
                    singleLine = false,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (suggestions.isNotEmpty()) {
                    Text(
                        text = "Sugerencias: ${suggestions.joinToString(", ")}",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                Text(
                    text = "Tarifa aproximada por hora (opcional)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF0F172A)
                )
                OutlinedTextField(
                    value = hourlyRateInput,
                    onValueChange = { input ->
                        val clean = input.filter { it.isDigit() }
                        if (clean.length <= 9) {
                            hourlyRateInput = PriceUtils.formatThousands(clean)
                        }
                    },
                    placeholder = { Text("Ej: 35000", color = Color(0xFF94A3B8)) },
                    prefix = { Text("$ ") },
                    suffix = { Text("/hora") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = Color(0xFFDC2626),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val category = categoryInput.trim()
                    val rateVal = PriceUtils.cleanPrice(hourlyRateInput).toDoubleOrNull()
                    val rateError = rateVal != null && rateVal < 5417.0
                    when {
                        category.isBlank() -> {
                            errorMessage = "Ingresa al menos una categoría de servicio."
                        }
                        rateError -> {
                            errorMessage = "Si vas a poner una tarifa por hora, debe ser de al menos $5.417 COP. Puedes dejarla vacía."
                        }
                        else -> {
                            isSubmitting = true
                            errorMessage = null
                            viewModel.upgradeCapability("PROFESSIONAL", category) { error ->
                                isSubmitting = false
                                if (error == null) {
                                    onActivated()
                                } else {
                                    errorMessage = error
                                }
                            }
                        }
                    }
                },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = PolishBlue)
            ) {
                Text(
                    text = if (isSubmitting) "Activando..." else "Activar modo profesional",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = { if (!isSubmitting) onDismiss() }) {
                Text("Cancelar")
            }
        }
    )
}
