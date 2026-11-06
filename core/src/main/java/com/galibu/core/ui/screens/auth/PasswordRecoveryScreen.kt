package com.galibu.core.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.galibu.core.ui.components.PasswordField
import com.galibu.core.ui.viewmodels.SharedViewModel

private val RecoveryBrandBlue = Color(0xFF2563EB)
private val RecoveryTextMain = Color(0xFF0F172A)
private val RecoveryTextSec = Color(0xFF64748B)
private val RecoveryError = Color(0xFFDC2626)

/**
 * Pantalla de recuperación de contraseña en 2 pasos:
 * 1) correo -> el backend envía un código de 6 dígitos.
 * 2) código + nueva contraseña -> restablece la contraseña.
 *
 * El backend responde igual aunque el correo no exista o sea de Google
 * (no revela el registro). El código vence en 15 minutos.
 */
@Composable
fun ForgotPasswordRecoveryScreen(
    viewModel: SharedViewModel,
    email: String,
    onEmailChange: (String) -> Unit,
    code: String,
    onCodeChange: (String) -> Unit,
    newPassword: String,
    onNewPasswordChange: (String) -> Unit,
    confirmNewPassword: String,
    onConfirmNewPasswordChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onPasswordUpdated: () -> Unit
) {
    var step by rememberSaveable { mutableIntStateOf(1) }
    var step1Email by rememberSaveable { mutableStateOf(email) }
    var showNewPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    var confirmedEmail by rememberSaveable { mutableStateOf("") }

    val back = {
        when {
            step == 2 -> {
                step = 1
                localError = null
            }
            else -> onDismiss()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = back,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = RecoveryTextMain,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = if (step == 3) "Contraseña actualizada" else "Recuperar contraseña",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = RecoveryTextMain,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            if (step != 3) {
                Spacer(modifier = Modifier.height(14.dp))
            }

            when (step) {
                1 -> {
                    Text(
                        text = "Ingresa tu correo electrónico y te enviaremos un código de 6 dígitos para restablecer tu contraseña.",
                        fontSize = 15.sp,
                        color = RecoveryTextSec,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                    )

                    Spacer(modifier = Modifier.height(36.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { onEmailChange(it); localError = null },
                        placeholder = { Text("Correo electrónico", color = RecoveryTextSec) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = RecoveryBrandBlue,
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedTextColor = RecoveryTextMain,
                            unfocusedTextColor = RecoveryTextMain
                        ),
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    )

                    if (localError != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = localError!!,
                            color = RecoveryError,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Button(
                        onClick = {
                            val targetEmail = email.trim().lowercase()
                            if (targetEmail.isEmpty()) {
                                localError = "Por favor, ingresa tu correo electrónico."
                                return@Button
                            }
                            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(targetEmail).matches()) {
                                localError = "Por favor, ingresa un correo electrónico válido."
                                return@Button
                            }
                            localError = null
                            viewModel.sendPasswordResetEmail(targetEmail) { error ->
                                if (error == null) {
                                    confirmedEmail = targetEmail
                                    onCodeChange("")
                                    onNewPasswordChange("")
                                    onConfirmNewPasswordChange("")
                                    step = 2
                                } else {
                                    localError = error
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RecoveryBrandBlue),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Text(
                            text = "Enviar código",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }
                }

                2 -> {
                    Text(
                        text = "Enviamos un código de 6 dígitos a $confirmedEmail. Vence en 15 minutos. Ingrésalo y escribe tu nueva contraseña.",
                        fontSize = 15.sp,
                        color = RecoveryTextSec,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    OutlinedTextField(
                        value = code,
                        onValueChange = { input -> onCodeChange(input.filter { it.isDigit() }.take(6)); localError = null },
                        placeholder = { Text("Código de 6 dígitos", color = RecoveryTextSec) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = RecoveryBrandBlue,
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedTextColor = RecoveryTextMain,
                            unfocusedTextColor = RecoveryTextMain
                        ),
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    PasswordField(
                        value = newPassword,
                        onValueChange = { onNewPasswordChange(it); localError = null },
                        label = "Nueva contraseña (mínimo 8 caracteres)",
                        visible = showNewPassword,
                        onToggleVisibility = { showNewPassword = !showNewPassword },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    PasswordField(
                        value = confirmNewPassword,
                        onValueChange = { onConfirmNewPasswordChange(it); localError = null },
                        label = "Confirmar nueva contraseña",
                        visible = showConfirmPassword,
                        onToggleVisibility = { showConfirmPassword = !showConfirmPassword },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (localError != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = localError!!,
                            color = RecoveryError,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Button(
                        onClick = {
                            when {
                                code.length != 6 -> {
                                    localError = "Ingresa el código de 6 dígitos enviado a tu correo."
                                }
                                newPassword.length < 8 -> {
                                    localError = "La nueva contraseña debe tener al menos 8 caracteres."
                                }
                                newPassword != confirmNewPassword -> {
                                    localError = "Las contraseñas no coinciden."
                                }
                                else -> {
                                    localError = null
                                    viewModel.resetPassword(confirmedEmail, code, newPassword) { error ->
                                        if (error == null) {
                                            step = 3
                                        } else {
                                            localError = error
                                        }
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RecoveryBrandBlue),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Text(
                            text = "Restablecer contraseña",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(
                        onClick = {
                            localError = null
                            viewModel.sendPasswordResetEmail(confirmedEmail) { error ->
                                if (error == null) {
                                    onCodeChange("")
                                    onNewPasswordChange("")
                                    onConfirmNewPasswordChange("")
                                    viewModel.showAlert("Aviso", "Te enviamos un código nuevo a $confirmedEmail.")
                                } else {
                                    localError = error
                                }
                            }
                        }
                    ) {
                        Text("Reenviar código", color = RecoveryBrandBlue)
                    }
                }

                else -> {
                    Text(
                        text = "Tu contraseña se actualizó correctamente. Ya puedes iniciar sesión con tu nueva contraseña.",
                        fontSize = 15.sp,
                        color = RecoveryTextSec,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = RecoveryBrandBlue),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Text(
                            text = "Volver a iniciar sesión",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }

        if (step != 3) {
            Text(
                text = "Volver a iniciar sesión",
                color = RecoveryBrandBlue,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp, top = 24.dp)
                    .clickable { onDismiss() }
            )
        }
    }
}
