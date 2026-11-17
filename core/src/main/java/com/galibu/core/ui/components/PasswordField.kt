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
import com.galibu.core.ui.components.rememberCameraCaptureLauncher
import com.galibu.core.ui.utils.ImageUtils
import com.galibu.core.ui.utils.PriceUtils
import com.galibu.core.ui.utils.capitalizeFirstLetter

import com.galibu.core.ui.theme.PolishBg as ScreenBg
import com.galibu.core.ui.theme.PolishDark as TextMain
import com.galibu.core.ui.theme.PolishGrayText as TextSec
import com.galibu.core.ui.theme.PolishBlue as BrandBlue
import com.galibu.core.ui.theme.StatusSuccessSoft as SuccessBg
import com.galibu.core.ui.theme.PolishGreen as SuccessFg
import com.galibu.core.ui.theme.PolishDark
import com.galibu.core.ui.theme.PolishBlue
import com.galibu.core.ui.theme.PolishBorder
import com.galibu.core.ui.theme.PolishGrayText

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
            color = PolishDark // PolishDark
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("........", color = PolishGrayText) },
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
                        tint = PolishGrayText
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = Color(0xFF1764FF), // PolishBlue
                unfocusedBorderColor = PolishBorder, // PolishBorder
                focusedTextColor = PolishDark, // PolishDark
                unfocusedTextColor = PolishDark // PolishDark
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        )
    }
}