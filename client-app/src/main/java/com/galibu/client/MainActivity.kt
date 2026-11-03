package com.galibu.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.zIndex
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.galibu.client.ui.screens.*
import com.galibu.client.ui.viewmodels.ClientViewModel
import com.galibu.professional.ui.screens.*
import com.galibu.professional.ui.viewmodels.ProfessionalViewModel
import com.galibu.core.ui.theme.*
import com.galibu.core.ui.components.GalibuLoadingOverlay
import com.galibu.core.ui.components.GalibuAlertDialog
import com.galibu.core.data.remote.ApiConfig
import com.galibu.core.data.telemetry.AppTelemetry
import androidx.lifecycle.ViewModelProvider
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.painterResource

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: ClientViewModel

    /**
     * El ViewModel del modo profesional se crea solo cuando se necesita. Antes se
     * instanciaba siempre en onCreate, lo que duplicaba la restauración de sesión,
     * los coletores de notificaciones y las cargas de configuración al arrancar.
     */
    private val proViewModel: ProfessionalViewModel by lazy {
        ViewModelProvider(this)[ProfessionalViewModel::class.java]
    }
    private val pendingOpenJobId = androidx.compose.runtime.mutableStateOf<String?>(null)

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingOpenJobId.value = intent.getStringExtra(
            com.galibu.client.ui.util.BidPushHelper.EXTRA_OPEN_JOB_ID
        )
    }

    override fun onStop() {
        AppTelemetry.flush()
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ApiConfig.baseUrl = BuildConfig.API_URL
        AppTelemetry.init("galibu", BuildConfig.VERSION_NAME)
        pendingOpenJobId.value = intent?.getStringExtra(
            com.galibu.client.ui.util.BidPushHelper.EXTRA_OPEN_JOB_ID
        )

        viewModel = ViewModelProvider(this)[ClientViewModel::class.java]

        setContent {
            GalibuTheme {
                val currentUser by viewModel.currentUser.collectAsState()
                val sessionReady by viewModel.sessionReady.collectAsState()
                val activeRole by viewModel.activeRole.collectAsState()
                val activeNotification by viewModel.activeBannerNotification.collectAsState()
                val isGlobalLoading by viewModel.isGlobalLoading.collectAsState()
                val globalLoadingMessage by viewModel.globalLoadingMessage.collectAsState()
                val globalAlert by viewModel.globalAlert.collectAsState()

                var currentRoute by rememberSaveable { mutableStateOf("AUTH") }
                var selectedTab by rememberSaveable { mutableStateOf("HOME") }

                // Único punto de entrada del cambio de modo. Mantiene los dos
                // ViewModels alineados con el modo activo y enruta al dashboard.
                val switchMode: (String) -> Unit = { role ->
                    viewModel.setAppRole(role)
                    proViewModel.setAppRole(role)
                    currentRoute = if (role == "PROFESSIONAL") "PROFESSIONAL_DASHBOARD" else "CLIENT_DASHBOARD"
                }

                LaunchedEffect(currentRoute, selectedTab) {
                    val screen = if (currentRoute == "CLIENT_DASHBOARD") {
                        "$currentRoute/$selectedTab"
                    } else {
                        currentRoute
                    }
                    AppTelemetry.screen(screen)
                }

                // Apertura desde una push del sistema: ir al detalle de la solicitud
                // usando el ViewModel del modo activo (antes siempre usaba el de cliente).
                LaunchedEffect(pendingOpenJobId.value, currentUser?.id, activeRole) {
                    val jobId = pendingOpenJobId.value
                    if (jobId != null && currentUser != null && activeRole != null) {
                        pendingOpenJobId.value = null
                        if (activeRole == "PROFESSIONAL") {
                            proViewModel.selectJob(jobId)
                            proViewModel.markJobNotificationsAsRead(jobId)
                        } else {
                            viewModel.selectJob(jobId)
                            viewModel.markJobNotificationsAsRead(jobId)
                        }
                        currentRoute = "NEGOTIATION"
                    }
                }

                LaunchedEffect(activeNotification) {
                    if (activeNotification != null) {
                        kotlinx.coroutines.delay(5000)
                        viewModel.activeBannerNotification.value = null
                    }
                }

                LaunchedEffect(currentUser, activeRole) {
                    AppTelemetry.setUser(currentUser?.id, currentUser?.role)
                    if (currentUser == null) {
                        currentRoute = "AUTH"
                    } else if (activeRole != null && currentRoute == "AUTH") {
                        // SharedViewModel resuelve el modo inicial (último usado o
                        // capacidad de la cuenta); aquí solo se enruta.
                        currentRoute = if (activeRole == "PROFESSIONAL") "PROFESSIONAL_DASHBOARD" else "CLIENT_DASHBOARD"
                    }
                }

                var showSplash by rememberSaveable { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(2000)
                    showSplash = false
                }

                // Keep the splash until the persisted-session check finishes, so a
                // restored session never flashes the login screen on cold start.
                val stillLoading = showSplash || !sessionReady || (currentUser != null && activeRole == null)

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = if (stillLoading) Color.White else DarkGrayBg
                ) {
                    if (stillLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_splash_logo),
                                    contentDescription = "Logo Galibu",
                                    modifier = Modifier.size(width = 61.dp, height = 72.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Galibu", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = PolishBlue)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Servicios que conectan, soluciones que llegan.",
                                    fontSize = 14.sp,
                                    color = Color(0xFF64748B),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                            }
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize().imePadding()) {
                            Scaffold(
                                modifier = Modifier.fillMaxSize(),
                            ) { innerPadding ->
                                Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                                    if (activeRole == "PROFESSIONAL") {
                                        when (currentRoute) {
                                            "AUTH" -> com.galibu.professional.ui.screens.AuthScreen(
                                                viewModel = proViewModel,
                                                onAuthSuccess = { currentRoute = "PROFESSIONAL_DASHBOARD" }
                                            )
                                            "PROFESSIONAL_DASHBOARD" -> ProfessionalDashboardScreen(
                                                viewModel = proViewModel,
                                                onNavigateToChat = { jobId ->
                                                    proViewModel.selectJob(jobId)
                                                    currentRoute = "CHAT"
                                                },
                                                onNavigateToNegotiation = { jobId ->
                                                    proViewModel.selectJob(jobId)
                                                    currentRoute = "NEGOTIATION"
                                                },
                                                onSwitchRole = { switchMode("CLIENT") }
                                            )
                                            "NEGOTIATION" -> PriceNegotiationScreen(
                                                viewModel = proViewModel,
                                                onOpenChat = { jobId ->
                                                    proViewModel.selectJob(jobId)
                                                    currentRoute = "CHAT"
                                                },
                                                onBack = { currentRoute = "PROFESSIONAL_DASHBOARD" }
                                            )
                                            "CHAT" -> ChatAndPaymentsScreen(
                                                viewModel = proViewModel,
                                                onBack = { currentRoute = "PROFESSIONAL_DASHBOARD" }
                                            )
                                            "NOTIFICATIONS_HISTORY" -> NotificationsAndHistoryScreen(
                                                viewModel = proViewModel,
                                                onBack = { currentRoute = "PROFESSIONAL_DASHBOARD" },
                                                onOpenJob = { jobId ->
                                                    proViewModel.selectJob(jobId)
                                                    proViewModel.markJobNotificationsAsRead(jobId)
                                                    currentRoute = "NEGOTIATION"
                                                }
                                            )
                                        }
                                    } else {
                                        when (currentRoute) {
                                            "AUTH" -> AuthScreen(
                                                viewModel = viewModel,
                                                onAuthSuccess = { currentRoute = "CLIENT_DASHBOARD" }
                                            )
                                            "CLIENT_DASHBOARD" -> ClientDashboardScreen(
                                                viewModel = viewModel,
                                                onNavigateToJobBids = { jobId ->
                                                    viewModel.selectJob(jobId)
                                                    currentRoute = "NEGOTIATION"
                                                },
                                                onNavigateToChat = { jobId ->
                                                    viewModel.selectJob(jobId)
                                                    currentRoute = "CHAT"
                                                },
                                                onNavigateToNotifications = {
                                                    selectedTab = "ALERTS"
                                                    currentRoute = "NOTIFICATIONS_HISTORY"
                                                },
                                                onSwitchRole = { switchMode("PROFESSIONAL") }
                                            )
                                            "NEGOTIATION" -> PriceNegotiationScreen(
                                                viewModel = viewModel,
                                                onOpenChat = { jobId ->
                                                    viewModel.selectJob(jobId)
                                                    currentRoute = "CHAT"
                                                },
                                                onBack = { currentRoute = "CLIENT_DASHBOARD" }
                                            )
                                            "CHAT" -> ChatAndPaymentsScreen(
                                                viewModel = viewModel,
                                                onBack = { currentRoute = "CLIENT_DASHBOARD" }
                                            )
                                            "NOTIFICATIONS_HISTORY" -> NotificationsAndHistoryScreen(
                                                viewModel = viewModel,
                                                onBack = { currentRoute = "CLIENT_DASHBOARD" },
                                                onOpenJob = { jobId ->
                                                    viewModel.selectJob(jobId)
                                                    viewModel.markJobNotificationsAsRead(jobId)
                                                    currentRoute = "NEGOTIATION"
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Global Loading Overlay
                            GalibuLoadingOverlay(
                                isLoading = isGlobalLoading,
                                message = globalLoadingMessage
                            )

                            // Global Alert Dialog
                            globalAlert?.let { alert ->
                                GalibuAlertDialog(
                                    title = alert.title,
                                    message = alert.message,
                                    type = alert.type,
                                    onConfirm = { viewModel.dismissAlert() },
                                    onDismissRequest = { viewModel.dismissAlert() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // No se llama refreshAll() aquí para evitar llamadas innecesarias a la API.
        // Cada pantalla gestiona su propia carga de datos cuando se muestra.
    }
}

