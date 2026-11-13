package com.galibu.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.galibu.core.ui.theme.PolishBlue
import com.galibu.core.ui.utils.PriceUtils

/** Datos que el profesional envia al cotizar un servicio. */
data class BidQuote(
    val amount: Double,
    val comment: String,
    val durationHours: Int,
    val quoteType: String,
    val visitRequired: Boolean,
    val visitFee: Double?,
    val materialsIncluded: Boolean,
    val materialsCost: Double?,
    val visitScheduledAt: Long? = null
)

const val QUOTE_TYPE_ESTIMATED = "ESTIMATED"
const val QUOTE_TYPE_AFTER_VISIT = "AFTER_VISIT"
const val QUOTE_TYPE_FIXED = "FIXED"

fun quoteTypeLabel(type: String?): String = when (type) {
    QUOTE_TYPE_AFTER_VISIT -> "Tras visita"
    QUOTE_TYPE_FIXED -> "Cerrada"
    else -> "Estimada"
}

/**
 * Hoja inferior para que el profesional envie su cotizacion completa:
 * tipo de cotizacion, valor, tiempo estimado, visita tecnica y materiales.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BidQuoteSheet(
    initialAmount: Double,
    initialComment: String = "",
    initialDurationHours: Int = 2,
    title: String = "Enviar cotización",
    subtitle: String = "Cuéntale al cliente cómo llegaste a este valor. Podrás ajustarlo si te hacen una contraoferta.",
    onDismiss: () -> Unit,
    onSubmit: (BidQuote) -> Unit
) {
    var amountRaw by remember(initialAmount) {
        mutableStateOf(if (initialAmount > 0) initialAmount.toInt().toString() else "")
    }
    var comment by remember(initialComment) { mutableStateOf(initialComment) }
    var durationRaw by remember(initialDurationHours) {
        mutableStateOf(initialDurationHours.coerceAtLeast(1).toString())
    }
    var quoteType by remember { mutableStateOf(QUOTE_TYPE_ESTIMATED) }
    var visitRequired by remember { mutableStateOf(false) }
    var visitFeeRaw by remember { mutableStateOf("") }
    var materialsIncluded by remember { mutableStateOf(false) }
    var materialsCostRaw by remember { mutableStateOf("") }
    var visitDateMillis by remember { mutableStateOf<Long?>(null) }
    var visitTimeRaw by remember { mutableStateOf("09:00") }
    var showVisitDatePicker by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }

    val amount = PriceUtils.cleanPrice(amountRaw).toDoubleOrNull() ?: 0.0
    val duration = durationRaw.toIntOrNull()?.coerceIn(1, 720) ?: 1
    val visitFee = PriceUtils.cleanPrice(visitFeeRaw).toDoubleOrNull()
    val materialsCost = PriceUtils.cleanPrice(materialsCostRaw).toDoubleOrNull()

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
                .imePadding()
                .testTag("bid_quote_sheet")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
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
            Text(
                text = subtitle,
                color = Color(0xFF64748B),
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Tipo de cotización",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF475569)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    QUOTE_TYPE_ESTIMATED to "Estimada",
                    QUOTE_TYPE_AFTER_VISIT to "Tras visita",
                    QUOTE_TYPE_FIXED to "Cerrada"
                ).forEach { (type, label) ->
                    FilterChip(
                        selected = quoteType == type,
                        onClick = {
                            quoteType = type
                            if (type == QUOTE_TYPE_AFTER_VISIT) visitRequired = true
                        },
                        label = { Text(label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFDBEAFE),
                            selectedLabelColor = PolishBlue
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = PriceUtils.formatThousands(amountRaw),
                onValueChange = { input ->
                    val clean = input.filter { it.isDigit() }
                    if (clean.length <= 9) amountRaw = PriceUtils.formatThousands(clean)
                    showError = false
                },
                label = { Text("Valor de la cotización (COP)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = showError,
                supportingText = if (showError) {
                    { Text("Ingresa un valor mayor a 0", color = Color(0xFFDC2626)) }
                } else null,
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
                    .testTag("quote_amount_input")
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = durationRaw,
                onValueChange = { input ->
                    val clean = input.filter { it.isDigit() }
                    if (clean.length <= 3) durationRaw = clean
                },
                label = { Text("Tiempo estimado (horas)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PolishBlue,
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    focusedTextColor = Color(0xFF1E293B),
                    unfocusedTextColor = Color(0xFF1E293B),
                    focusedLabelColor = PolishBlue,
                    unfocusedLabelColor = Color(0xFF64748B)
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = comment,
                onValueChange = { if (it.length <= 2000) comment = it },
                label = { Text("¿Qué incluye tu cotización? (opcional)") },
                minLines = 2,
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PolishBlue,
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    focusedTextColor = Color(0xFF1E293B),
                    unfocusedTextColor = Color(0xFF1E293B),
                    focusedLabelColor = PolishBlue,
                    unfocusedLabelColor = Color(0xFF64748B)
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            QuoteOptionRow(
                title = "Requiere visita técnica",
                description = "El valor final se confirma después de revisar el lugar.",
                checked = visitRequired,
                onCheckedChange = { visitRequired = it }
            )
            if (visitRequired) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = PriceUtils.formatThousands(visitFeeRaw),
                    onValueChange = { input ->
                        val clean = input.filter { it.isDigit() }
                        if (clean.length <= 9) visitFeeRaw = PriceUtils.formatThousands(clean)
                    },
                    label = { Text("Costo de la visita (opcional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PolishBlue,
                        unfocusedBorderColor = Color(0xFFCBD5E1),
                        focusedTextColor = Color(0xFF1E293B),
                        unfocusedTextColor = Color(0xFF1E293B),
                        focusedLabelColor = PolishBlue,
                        unfocusedLabelColor = Color(0xFF64748B)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showVisitDatePicker = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = visitDateMillis?.let { formatVisitDate(it) } ?: "Elegir fecha",
                            fontSize = 12.sp
                        )
                    }
                    OutlinedTextField(
                        value = visitTimeRaw,
                        onValueChange = { input ->
                            if (input.length <= 5) visitTimeRaw = input.filter { it.isDigit() || it == ':' }
                        },
                        label = { Text("Hora") },
                        placeholder = { Text("09:00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PolishBlue,
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedTextColor = Color(0xFF1E293B),
                            unfocusedTextColor = Color(0xFF1E293B),
                            focusedLabelColor = PolishBlue,
                            unfocusedLabelColor = Color(0xFF64748B)
                        ),
                        modifier = Modifier.width(110.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            QuoteOptionRow(
                title = "Incluye materiales",
                description = "El cliente sabrá que no debe comprarlos por separado.",
                checked = materialsIncluded,
                onCheckedChange = { materialsIncluded = it }
            )
            if (materialsIncluded) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = PriceUtils.formatThousands(materialsCostRaw),
                    onValueChange = { input ->
                        val clean = input.filter { it.isDigit() }
                        if (clean.length <= 9) materialsCostRaw = PriceUtils.formatThousands(clean)
                    },
                    label = { Text("Costo estimado de materiales (opcional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PolishBlue,
                        unfocusedBorderColor = Color(0xFFCBD5E1),
                        focusedTextColor = Color(0xFF1E293B),
                        unfocusedTextColor = Color(0xFF1E293B),
                        focusedLabelColor = PolishBlue,
                        unfocusedLabelColor = Color(0xFF64748B)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Galibu no cobra ni fija el precio: la cotización la defines tú y el cliente decide.",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    lineHeight = 15.sp
                )
            }
            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF64748B)),
                    contentPadding = PaddingValues(vertical = 14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancelar", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Button(
                    onClick = {
                        if (amount <= 0.0) {
                            showError = true
                        } else {
                            onSubmit(
                                BidQuote(
                                    amount = amount,
                                    comment = comment.trim(),
                                    durationHours = duration,
                                    quoteType = quoteType,
                                    visitRequired = visitRequired,
                                    visitFee = if (visitRequired) visitFee else null,
                                    materialsIncluded = materialsIncluded,
                                    materialsCost = if (materialsIncluded) materialsCost else null,
                                    visitScheduledAt = if (visitRequired) {
                                        combineVisitDateTime(visitDateMillis, visitTimeRaw)
                                    } else null
                                )
                            )
                        }
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = PolishBlue),
                    contentPadding = PaddingValues(vertical = 14.dp),
                    modifier = Modifier
                        .weight(1.5f)
                        .testTag("submit_quote_button")
                ) {
                    Text(
                        text = "Enviar cotización",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }
            }
        }
    }

    if (showVisitDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = visitDateMillis ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showVisitDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    visitDateMillis = datePickerState.selectedDateMillis
                    showVisitDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showVisitDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

private fun combineVisitDateTime(dateMillis: Long?, timeRaw: String): Long? {
    if (dateMillis == null) return null
    val parts = timeRaw.split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 9
    val minute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
    val calendar = java.util.Calendar.getInstance()
    calendar.timeInMillis = dateMillis
    calendar.set(java.util.Calendar.HOUR_OF_DAY, hour)
    calendar.set(java.util.Calendar.MINUTE, minute)
    calendar.set(java.util.Calendar.SECOND, 0)
    calendar.set(java.util.Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

private fun formatVisitDate(millis: Long): String {
    val formatter = java.text.SimpleDateFormat("dd MMM", java.util.Locale("es", "CO"))
    return formatter.format(java.util.Date(millis))
}

@Composable
private fun QuoteOptionRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            Text(
                text = description,
                fontSize = 11.sp,
                color = Color(0xFF64748B),
                lineHeight = 15.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PolishBlue
            )
        )
    }
}

/**
 * Chips informativos de una cotizacion (tipo, visita, materiales y ronda).
 * Se usa en las tarjetas de oferta del cliente y del profesional.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BidQuoteDetails(
    quoteType: String?,
    visitRequired: Boolean,
    visitFee: Double?,
    materialsIncluded: Boolean,
    materialsCost: Double?,
    counterRound: Int,
    visitScheduledAt: Long? = null,
    modifier: Modifier = Modifier
) {
    val amountFormat = remember { java.text.NumberFormat.getNumberInstance(java.util.Locale.US) }
    val visitFormat = remember {
        java.text.SimpleDateFormat("dd MMM · HH:mm", java.util.Locale("es", "CO"))
    }
    val chips = remember(
        quoteType, visitRequired, visitFee, materialsIncluded, materialsCost,
        counterRound, visitScheduledAt
    ) {
        buildList {
            add(Triple(quoteTypeLabel(quoteType), Color(0xFFEFF6FF), Color(0xFF2563EB)))
            if (visitRequired) {
                val feeText = visitFee?.takeIf { it > 0 }?.let { " · $${amountFormat.format(it)}" } ?: ""
                add(Triple("Requiere visita$feeText", Color(0xFFFEF3C7), Color(0xFFB45309)))
            }
            if (visitScheduledAt != null) {
                add(
                    Triple(
                        "Visita: ${visitFormat.format(java.util.Date(visitScheduledAt))}",
                        Color(0xFFFFEDD5),
                        Color(0xFFC2410C)
                    )
                )
            }
            if (materialsIncluded) {
                val costText = materialsCost?.takeIf { it > 0 }?.let { " · $${amountFormat.format(it)}" } ?: ""
                add(Triple("Incluye materiales$costText", Color(0xFFDCFCE7), Color(0xFF15803D)))
            }
            if (counterRound > 0) {
                add(Triple("Ronda $counterRound", Color(0xFFF3E8FF), Color(0xFF7E22CE)))
            }
        }
    }
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        chips.forEach { (label, background, foreground) ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(background)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = label,
                    color = foreground,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
