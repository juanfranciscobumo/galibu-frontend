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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.galibu.client.ui.screens.*
import com.galibu.client.ui.viewmodels.ClientViewModel
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
        AppTelemetry.init("client", BuildConfig.VERSION_NAME)
        pendingOpenJobId.value = intent?.getStringExtra(
            com.galibu.client.ui.util.BidPushHelper.EXTRA_OPEN_JOB_ID
        )

        viewModel = ViewModelProvider(this)[ClientViewModel::class.java]
        viewModel.setAppRole("CLIENT")

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

                LaunchedEffect(currentRoute, selectedTab) {
                    val screen = if (currentRoute == "CLIENT_DASHBOARD") {
                        "$currentRoute/$selectedTab"
                    } else {
                        currentRoute
                    }
                    AppTelemetry.screen(screen)
                }

                // Apertura desde una push del sistema: ir al detalle de la solicitud.
                LaunchedEffect(pendingOpenJobId.value, currentUser) {
                    val jobId = pendingOpenJobId.value
                    if (jobId != null && currentUser != null) {
                        pendingOpenJobId.value = null
                        viewModel.selectJob(jobId)
                        viewModel.markJobNotificationsAsRead(jobId)
                        currentRoute = "NEGOTIATION"
                    }
                }

                LaunchedEffect(activeNotification) {
                    if (activeNotification != null) {
                        kotlinx.coroutines.delay(5000)
                        viewModel.activeBannerNotification.value = null
                    }
                }

                LaunchedEffect(currentUser) {
                    AppTelemetry.setUser(currentUser?.id, currentUser?.role)
                    if (currentUser == null) {
                        currentRoute = "AUTH"
                    } else if (currentRoute == "AUTH") {
                        currentRoute = "CLIENT_DASHBOARD"
                    }
                }

                var showSplash by rememberSaveable { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(2000)
                    showSplash = false
                }

                // Keep the splash until the persisted-session check finishes, so a
                // restored session never flashes the login screen on cold start.
                val stillLoading = showSplash || !sessionReady

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
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Clientes", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color(0xFF64748B))
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
                                            }
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
                                        "PROFILE" -> ProfileScreen(
                                            viewModel = viewModel,
                                            onNavigateToNotifications = {
                                                selectedTab = "ALERTS"
                                                currentRoute = "NOTIFICATIONS_HISTORY"
                                            }
                                        )
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

