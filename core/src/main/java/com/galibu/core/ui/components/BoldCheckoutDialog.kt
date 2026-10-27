package com.galibu.core.ui.components

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.galibu.core.ui.theme.PolishBlue
import com.galibu.core.ui.theme.PolishNavy

/** Debe coincidir con app.bold.callback-url del backend. */
const val BOLD_CALLBACK_PREFIX = "https://galibu.app/bold/return"

/**
 * Checkout de Bold dentro de la app.
 *
 * Se detecta el retorno de Bold por la URL de callback: no se carga, se cierra
 * el diálogo y la pantalla consulta el estado real de la orden en el backend.
 * El botón "Ya realicé el pago" cubre los métodos en los que Bold no redirige
 * automáticamente (por ejemplo, pagos con Nequi).
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BoldCheckoutDialog(
    url: String,
    onClose: () -> Unit,
    onPaymentReturn: () -> Unit
) {
    val context = LocalContext.current
    var webView by remember { mutableStateOf<WebView?>(null) }
    var loading by remember { mutableStateOf(true) }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        BackHandler {
            val view = webView
            if (view != null && view.canGoBack()) {
                view.goBack()
            } else {
                onClose()
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Pago seguro con Bold",
                            color = PolishNavy,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tarjeta, PSE, Nequi o Botón Bancolombia",
                            color = Color(0xFF64748B),
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = PolishNavy
                        )
                    }
                }

                if (loading) {
                    LinearProgressIndicator(
                        color = PolishBlue,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                    )
                }

                AndroidView(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color.White),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            setBackgroundColor(android.graphics.Color.WHITE)
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            // Bold y los bancos usan cookies de terceros durante el 3DS.
                            CookieManager.getInstance().setAcceptCookie(true)
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                            settings.loadsImagesAutomatically = true
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(
                                    view: WebView?,
                                    url: String?,
                                    favicon: Bitmap?
                                ) {
                                    if (url?.startsWith(BOLD_CALLBACK_PREFIX) == true) {
                                        onPaymentReturn()
                                        return
                                    }
                                    loading = true
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    loading = false
                                }

                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    val target = request?.url?.toString() ?: return false
                                    if (target.startsWith(BOLD_CALLBACK_PREFIX)) {
                                        onPaymentReturn()
                                        return true
                                    }
                                    if (!target.startsWith("http://") && !target.startsWith("https://")) {
                                        // Enlaces a apps bancarias (Nequi, Bancolombia, etc.).
                                        return try {
                                            ctx.startActivity(
                                                Intent(Intent.ACTION_VIEW, Uri.parse(target))
                                            )
                                            true
                                        } catch (e: Exception) {
                                            true
                                        }
                                    }
                                    return false
                                }
                            }
                            webChromeClient = WebChromeClient()
                            loadUrl(url)
                            webView = this
                        }
                    }
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Al terminar el pago volverás automáticamente a Galibu. " +
                                "Si tu banco no redirige, usa el botón de abajo.",
                            color = Color(0xFF64748B),
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        TextButton(
                            onClick = onPaymentReturn,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Ya realicé el pago · Verificar",
                                color = PolishBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webView?.stopLoading()
            webView?.destroy()
        }
    }
}
