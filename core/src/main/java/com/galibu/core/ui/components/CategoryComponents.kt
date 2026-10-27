package com.galibu.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.Hardware
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.galibu.core.ui.theme.PolishBlue
import com.galibu.core.ui.theme.PolishBorder
import com.galibu.core.ui.theme.PolishDark
import com.galibu.core.ui.theme.PolishGrayText

/**
 * Identidad visual única para las categorías de servicio.
 *
 * Tanto la app cliente como la profesional usan el mismo ícono y color para
 * cada categoría, evitando que la misma categoría se vea distinta en cada app.
 */
data class CategoryVisual(
    val icon: ImageVector,
    val color: Color
)

fun categoryVisual(category: String): CategoryVisual {
    val key = category.trim().lowercase()
    return when {
        key.contains("plomer") -> CategoryVisual(Icons.Default.WaterDrop, Color(0xFF2563EB))
        key.contains("electric") -> CategoryVisual(Icons.Default.Bolt, Color(0xFFCA8A04))
        key.contains("carpinter") -> CategoryVisual(Icons.Default.Hardware, Color(0xFFD97706))
        key.contains("pintura") -> CategoryVisual(Icons.Default.FormatPaint, Color(0xFFDB2777))
        key.contains("alba") -> CategoryVisual(Icons.Default.Construction, Color(0xFF4F46E5))
        key.contains("jardin") -> CategoryVisual(Icons.Default.Eco, Color(0xFF16A34A))
        key.contains("cerraj") -> CategoryVisual(Icons.Default.Key, Color(0xFF64748B))
        key.contains("fumig") -> CategoryVisual(Icons.Default.BugReport, Color(0xFF7C3AED))
        key.contains("mecan") -> CategoryVisual(Icons.Default.Build, Color(0xFFDC2626))
        key.contains("soldad") -> CategoryVisual(Icons.Default.Whatshot, Color(0xFFB45309))
        key.contains("climat") || key.contains("aire") ->
            CategoryVisual(Icons.Default.AcUnit, Color(0xFF0891B2))
        key.contains("limpieza") -> CategoryVisual(Icons.Default.CleaningServices, Color(0xFF0D9488))
        key.contains("mudanza") -> CategoryVisual(Icons.Default.LocalShipping, Color(0xFF2563EB))
        key.contains("techad") || key.contains("techo") ->
            CategoryVisual(Icons.Default.Home, Color(0xFF92400E))
        key.contains("tecnolog") || key.contains("comput") ->
            CategoryVisual(Icons.Default.Computer, Color(0xFF4F46E5))
        key.contains("otro") -> CategoryVisual(Icons.Default.Category, Color(0xFF64748B))
        else -> CategoryVisual(Icons.Default.Build, PolishBlue)
    }
}

/** Etiqueta tipo píldora con ícono, usada para la categoría principal de un servicio. */
@Composable
fun CategoryBadge(
    category: String,
    modifier: Modifier = Modifier,
    showIcon: Boolean = true
) {
    if (category.isBlank()) return
    val visual = categoryVisual(category)
    Row(
        modifier = modifier
            .background(
                color = visual.color.copy(alpha = 0.12f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showIcon) {
            Icon(
                imageVector = visual.icon,
                contentDescription = null,
                tint = visual.color,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = category,
            color = visual.color,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Etiqueta compacta para especialidades de un profesional. */
@Composable
fun ServiceCategoryTag(
    category: String,
    modifier: Modifier = Modifier
) {
    if (category.isBlank()) return
    val visual = categoryVisual(category)
    Box(
        modifier = modifier
            .background(
                color = visual.color.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = category,
            color = visual.color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Círculo con el ícono de la categoría, para listas y tarjetas. */
@Composable
fun CategoryIconCircle(
    category: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = size * 0.5f
) {
    val visual = categoryVisual(category)
    Box(
        modifier = modifier
            .size(size)
            .background(color = visual.color.copy(alpha = 0.12f), shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = visual.icon,
            contentDescription = null,
            tint = visual.color,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Selector de una sola categoría.
 *
 * Se usa para la categoría de una solicitud y para el filtro de profesionales.
 * Mantiene el mismo campo, altura, borde, tipografía y menú en ambas apps.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySingleSelectField(
    options: List<String>,
    selected: String?,
    onSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = "Categoría",
    placeholder: String = "Seleccionar categoría",
    allOptionLabel: String? = null,
    customOptionLabel: String? = null,
    onAddCustom: (() -> Unit)? = null,
    leadingIcon: ImageVector? = null,
    accentColor: Color = PolishBlue,
    borderColor: Color = PolishBorder,
    containerColor: Color = Color.White,
    textColor: Color = PolishDark,
    mutedTextColor: Color = PolishGrayText,
    testTag: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val fieldText = selected ?: allOptionLabel ?: ""

    Column(modifier = modifier.fillMaxWidth()) {
        if (!label.isNullOrBlank()) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = fieldText,
                onValueChange = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                    .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
                readOnly = true,
                singleLine = true,
                placeholder = if (fieldText.isBlank()) {
                    { Text(placeholder, color = mutedTextColor, fontSize = 14.sp) }
                } else {
                    null
                },
                leadingIcon = leadingIcon?.let { icon ->
                    {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = mutedTextColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = borderColor,
                    focusedContainerColor = containerColor,
                    unfocusedContainerColor = containerColor,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    focusedPlaceholderColor = mutedTextColor,
                    unfocusedPlaceholderColor = mutedTextColor
                )
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                shape = RoundedCornerShape(12.dp),
                containerColor = Color.White,
                border = BorderStroke(1.dp, borderColor)
            ) {
                if (allOptionLabel != null) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = allOptionLabel,
                                color = textColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        trailingIcon = {
                            if (selected == null) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        onClick = {
                            onSelected(null)
                            expanded = false
                        }
                    )
                }

                options.forEach { option ->
                    val isCustomOption = customOptionLabel != null && option == customOptionLabel
                    val visual = categoryVisual(option)
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isCustomOption) {
                                        Icons.Default.AddCircleOutline
                                    } else {
                                        visual.icon
                                    },
                                    contentDescription = null,
                                    tint = if (isCustomOption) accentColor else visual.color,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = option,
                                    color = if (isCustomOption) accentColor else textColor,
                                    fontSize = 14.sp,
                                    fontWeight = if (isCustomOption) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        },
                        trailingIcon = {
                            if (!isCustomOption && selected == option) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        onClick = {
                            if (isCustomOption) {
                                expanded = false
                                onAddCustom?.invoke()
                            } else {
                                onSelected(option)
                                expanded = false
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Selector múltiple de categorías con checkboxes y chips de las elegidas.
 * Es el mismo componente para el registro cliente, registro profesional y
 * edición de perfil profesional.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CategoryMultiSelectField(
    selected: MutableList<String>,
    options: List<String>,
    modifier: Modifier = Modifier,
    label: String? = "Categoría",
    placeholder: String = "Seleccionar categorías",
    accentColor: Color = PolishBlue,
    borderColor: Color = PolishBorder,
    containerColor: Color = Color.White,
    textColor: Color = PolishDark,
    mutedTextColor: Color = PolishGrayText,
    customOptionLabel: String? = null,
    onAddCustom: (() -> Unit)? = null,
    testTag: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val summary = when {
        selected.isEmpty() -> ""
        selected.size == 1 -> selected.first()
        else -> "${selected.size} categorías seleccionadas"
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (!label.isNullOrBlank()) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = summary,
                onValueChange = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                    .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
                readOnly = true,
                singleLine = true,
                placeholder = {
                    Text(placeholder, color = mutedTextColor, fontSize = 14.sp)
                },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = borderColor,
                    focusedContainerColor = containerColor,
                    unfocusedContainerColor = containerColor,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    focusedPlaceholderColor = mutedTextColor,
                    unfocusedPlaceholderColor = mutedTextColor
                )
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                shape = RoundedCornerShape(12.dp),
                containerColor = Color.White,
                border = BorderStroke(1.dp, borderColor)
            ) {
                options.forEach { option ->
                    val isCustomOption = customOptionLabel != null && option == customOptionLabel
                    val isChecked = selected.contains(option)
                    val visual = categoryVisual(option)
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isCustomOption) {
                                    Icon(
                                        imageVector = Icons.Default.AddCircleOutline,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = null,
                                        colors = CheckboxDefaults.colors(checkedColor = accentColor)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = visual.icon,
                                    contentDescription = null,
                                    tint = visual.color,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = option,
                                    color = textColor,
                                    fontSize = 14.sp
                                )
                            }
                        },
                        onClick = {
                            when {
                                isCustomOption -> {
                                    expanded = false
                                    onAddCustom?.invoke()
                                }
                                isChecked -> selected.remove(option)
                                else -> selected.add(option)
                            }
                        }
                    )
                }
            }
        }

        if (selected.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                selected.toList().forEach { chosen ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = accentColor.copy(alpha = 0.08f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(
                                start = 10.dp,
                                end = 6.dp,
                                top = 6.dp,
                                bottom = 6.dp
                            )
                        ) {
                            Text(
                                text = chosen,
                                fontSize = 12.sp,
                                color = accentColor,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            IconButton(
                                onClick = { selected.remove(chosen) },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Quitar",
                                    tint = accentColor,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
