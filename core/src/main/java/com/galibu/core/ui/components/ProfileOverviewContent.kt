package com.galibu.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.galibu.core.data.model.UserProfile
import com.galibu.core.data.model.canSwitchToClient
import com.galibu.core.data.model.canSwitchToProfessional
import com.galibu.core.ui.utils.capitalizeFirstLetter
import com.galibu.core.ui.theme.PolishBlue
import com.galibu.core.ui.theme.PolishGrayText
import com.galibu.core.ui.theme.PolishGreen
import com.galibu.core.ui.theme.PolishIce
import com.galibu.core.ui.utils.ImageUtils

/**
 * Vista principal de perfil compartida por el modo cliente y el profesional.
 *
 * Contiene la tarjeta de identidad y el menú de cuenta. Lo que cambia por modo
 * es solo el texto del cambio de rol, que además se oculta si la cuenta no
 * tiene la capacidad opuesta. Los formularios de edición siguen viviendo en
 * cada modo porque sus campos difieren de verdad.
 *
 * [isProfessional] se usa únicamente para conservar los espaciados actuales de
 * cada modo; no cambia el contenido del menú.
 */
@Composable
fun ProfileOverviewContent(
    currentUser: UserProfile?,
    isProfessional: Boolean,
    logoResId: Int,
    modifier: Modifier = Modifier,
    showTitle: Boolean = true,
    successMessage: String? = null,
    onDismissSuccess: () -> Unit = {},
    onEditProfile: () -> Unit,
    onChangePassword: () -> Unit,
    onSupport: () -> Unit,
    onSwitchRole: () -> Unit,
    onActivateProfessional: () -> Unit = {},
    onActivateClient: () -> Unit = {},
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    val profilePhotoModel = remember(currentUser?.profilePhotoUrl) {
        ImageUtils.getCoilModel(currentUser?.profilePhotoUrl)
    }
    val canSwitchRole = if (isProfessional) {
        currentUser?.canSwitchToClient() == true
    } else {
        currentUser?.canSwitchToProfessional() == true
    }
    val needsProfessionalActivation = !isProfessional && currentUser?.canSwitchToProfessional() != true
    val needsClientActivation = isProfessional && currentUser?.canSwitchToClient() != true
    val switchRoleLabel = if (isProfessional) {
        "Cambiar a modo cliente"
    } else {
        "Cambiar a modo profesional"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (showTitle) {
            Text(
                text = "Perfil",
                color = Color(0xFF0F172A),
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            )
            HorizontalDivider(color = Color(0xFFEFF2F5), thickness = 1.dp)
        }

        if (successMessage != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDismissSuccess() }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF16A34A)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = successMessage,
                        color = Color(0xFF15803D),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Tarjeta de identidad
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, Color(0xFFEFF2F5)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (profilePhotoModel != null) {
                    AsyncImage(
                        model = profilePhotoModel,
                        contentDescription = "Foto de perfil",
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .border(3.dp, PolishBlue, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(PolishIce)
                            .border(3.dp, PolishBlue, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = PolishBlue,
                            modifier = Modifier.size(50.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = currentUser?.name ?: "Usuario",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = currentUser?.email ?: "",
                    fontSize = 14.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )

                if (isProfessional) {
                    val category = currentUser?.workCategory?.takeIf { it.isNotBlank() }
                    val hourlyRate = currentUser?.hourlyRate?.takeIf { it > 0.0 }
                    if (category != null || hourlyRate != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            category?.let { ProfileInfoPill(it) }
                            hourlyRate?.let {
                                ProfileInfoPill(
                                    "$" + String.format(java.util.Locale.US, "%,.0f", it) + " / hora"
                                )
                            }
                        }
                    }
                    if (currentUser?.verificationStatus == "VERIFIED" && currentUser?.isVerified == true) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = PolishGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Perfil verificado",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PolishGreen
                            )
                        }
                    }
                }
            }
        }

        ProfileSectionLabel("MI PERFIL")

        // Acciones principales
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ProfileOptionCard(
                icon = Icons.Default.Edit,
                iconBgColor = Color(0xFFEFF6FF),
                iconColor = PolishBlue,
                title = "Editar perfil",
                onClick = onEditProfile
            )

            ProfileOptionCard(
                icon = Icons.Default.Lock,
                iconBgColor = Color(0xFFEFF6FF),
                iconColor = PolishBlue,
                title = "Cambiar contraseña",
                onClick = onChangePassword
            )
        }

        // Sección CUENTA
        ProfileSectionLabel("CUENTA")

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ProfileOptionCard(
                icon = Icons.AutoMirrored.Filled.Help,
                iconBgColor = Color(0xFFEFF6FF),
                iconColor = PolishBlue,
                title = "Ayuda y soporte",
                onClick = onSupport
            )

            if (needsProfessionalActivation) {
                ProfileOptionCard(
                    icon = Icons.Default.Work,
                    iconBgColor = Color(0xFFDCFCE7),
                    iconColor = Color(0xFF16A34A),
                    title = "Ofrecer mis servicios",
                    onClick = onActivateProfessional
                )
            }

            if (needsClientActivation) {
                ProfileOptionCard(
                    icon = Icons.Default.Search,
                    iconBgColor = Color(0xFFDCFCE7),
                    iconColor = Color(0xFF16A34A),
                    title = "Buscar un profesional",
                    onClick = onActivateClient
                )
            }

            if (canSwitchRole) {
                ProfileOptionCard(
                    icon = Icons.Default.SwapHoriz,
                    iconBgColor = Color(0xFFEFF6FF),
                    iconColor = PolishBlue,
                    title = switchRoleLabel,
                    onClick = onSwitchRole
                )
            }

        }

        ProfileSectionLabel("SESIÓN")

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Cerrar sesión no es destructivo: se muestra neutro.
            ProfileOptionCard(
                icon = Icons.AutoMirrored.Filled.Logout,
                iconBgColor = Color(0xFFF1F5F9),
                iconColor = Color(0xFF475569),
                title = "Cerrar sesión",
                showChevron = false,
                onClick = onLogout
            )

            // Ambas abren un diálogo de confirmación, por eso ninguna lleva chevron.
            ProfileOptionCard(
                icon = Icons.Default.Delete,
                iconBgColor = Color(0xFFFEF2F2),
                iconColor = Color(0xFFEF4444),
                title = "Eliminar cuenta",
                titleColor = Color(0xFFEF4444),
                showChevron = false,
                onClick = onDeleteAccount
            )
        }

        // Pie: logo + versión
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = logoResId),
                contentDescription = "Galibu",
                modifier = Modifier.size(width = 27.dp, height = 32.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Galibu v1.0.0",
                color = PolishGrayText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun ProfileSectionLabel(text: String) {
    Text(
        text = text,
        color = PolishGrayText,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        letterSpacing = 1.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    )
}

@Composable
private fun ProfileInfoPill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFEFF6FF))
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            color = PolishBlue,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
