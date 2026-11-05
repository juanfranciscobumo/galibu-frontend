package com.galibu.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.galibu.core.ui.components.PhotoSourceDialog
import com.galibu.core.ui.components.rememberCameraCaptureLauncher
import com.galibu.core.ui.utils.ImageUtils
import com.galibu.core.ui.utils.PriceUtils
import com.galibu.core.ui.utils.capitalizeFirstLetter

private val ScreenBg = Color(0xFFF8FAFC)
private val TextMain = Color(0xFF0F172A)
private val TextSec = Color(0xFF64748B)
private val BrandBlue = Color(0xFF2563EB)
private val SuccessBg = Color(0xFFDCFCE7)
private val SuccessFg = Color(0xFF16A34A)
private val PolishDark = Color(0xFF0F172A)
private val PolishBlue = Color(0xFF2563EB)

private const val MAX_PORTFOLIO_PHOTOS = 6

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    visible: Boolean = false,
    onToggleVisibility: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF0F172A) // PolishDark
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("........", color = Color(0xFF64748B)) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(
                    onClick = onToggleVisibility,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (visible) "Ocultar contraseña" else "Mostrar contraseña",
                        tint = Color(0xFF64748B)
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = Color(0xFF1764FF), // PolishBlue
                unfocusedBorderColor = Color(0xFFE2E8F0), // PolishBorder
                focusedTextColor = Color(0xFF0F172A), // PolishDark
                unfocusedTextColor = Color(0xFF0F172A) // PolishDark
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        )
    }
}