package com.lernspace.app.ui.mimolab.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.lernspace.app.core.mimolab.MimoSimulation
import com.lernspace.app.ui.theme.*

/**
 * Sandboxed, hardware-accelerated interactive STEM simulation viewer.
 * Hosts Mimo-generated HTML5 Canvas applets with zero-lag touch manipulation,
 * parameter controls, and high-resolution snapshot extraction for notes.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MimoSimulationViewer(
    simulation: MimoSimulation,
    isDarkTheme: Boolean = false,
    isFullscreen: Boolean = false,
    showHeader: Boolean = true,
    onToggleFullscreen: (() -> Unit)? = null,
    onInsertSnapshotToNote: ((Bitmap) -> Unit)? = null,
    onClose: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isPageLoaded by remember { mutableStateOf(false) }

    val bgColor = if (isDarkTheme) Color(0xFF0F172A) else Color.White
    val cardColor = if (isDarkTheme) Color(0xFF1E293B) else Color.White
    val textColor = if (isDarkTheme) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val borderColor = if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0)

    Surface(
        shape = if (isFullscreen) RoundedCornerShape(0.dp) else RoundedCornerShape(16.dp),
        color = cardColor,
        border = if (isFullscreen) null else BorderStroke(1.dp, borderColor),
        shadowElevation = if (isFullscreen) 0.dp else 8.dp,
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (showHeader) {
                // ── Top Header Control Bar ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isDarkTheme) Color(0xFF0F172A) else Color.White)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isFullscreen) {
                            FilledTonalButton(
                                onClick = onClose,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = LernspaceSurfaceLow,
                                    contentColor = LernspaceDark
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Kembali ke Katalog",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Katalog", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            VerticalDivider(
                                modifier = Modifier
                                    .height(20.dp)
                                    .padding(horizontal = 4.dp),
                                color = borderColor
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Science,
                                contentDescription = "Mimo Lab",
                                tint = AccentBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(android.graphics.Color.parseColor(simulation.subject.badgeColorHex)).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = simulation.subject.displayName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(android.graphics.Color.parseColor(simulation.subject.badgeColorHex)),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Text(
                                    text = "›",
                                    fontSize = 13.sp,
                                    color = LernspaceTextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = simulation.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = textColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (simulation.description.isNotBlank()) {
                                Text(
                                    text = simulation.description,
                                    fontSize = 11.5.sp,
                                    color = LernspaceTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Reset / Reload Simulation
                        IconButton(
                            onClick = {
                                webViewRef?.reload()
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Reset Simulasi",
                                tint = LernspaceTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Snapshot to Note (If callback provided)
                        if (onInsertSnapshotToNote != null) {
                            Button(
                                onClick = {
                                    val wv = webViewRef
                                    if (wv != null && wv.width > 0 && wv.height > 0) {
                                        try {
                                            val bitmap = Bitmap.createBitmap(wv.width, wv.height, Bitmap.Config.ARGB_8888)
                                            val canvas = Canvas(bitmap)
                                            wv.draw(canvas)
                                            onInsertSnapshotToNote(bitmap)
                                            Toast.makeText(context, "Snapshot simulasi berhasil ditempel ke catatan!", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Gagal mengambil snapshot: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDarkTheme) Color.White else Color.Black,
                                    contentColor = if (isDarkTheme) Color.Black else Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Filled.ContentPaste, null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Tempel ke Catatan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Close Button
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Tutup",
                                tint = LernspaceTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                HorizontalDivider(color = borderColor)
            }

            // ── Interactive HTML5 Simulation Viewport ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(bgColor)
            ) {
                key(simulation.id) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                android.util.Log.d("MimoWebView", "Loading simulation '${simulation.title}', HTML length: ${simulation.htmlContent.length}")
                                layoutParams = android.view.ViewGroup.LayoutParams(
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                setBackgroundColor(android.graphics.Color.WHITE)
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    useWideViewPort = true
                                    loadWithOverviewMode = true
                                    allowFileAccess = false
                                    allowContentAccess = false
                                    cacheMode = WebSettings.LOAD_NO_CACHE
                                }
                                try {
                                    WebView.setWebContentsDebuggingEnabled(true)
                                } catch (e: Exception) {
                                    // ignore if not debuggable
                                }
                                webChromeClient = object : WebChromeClient() {
                                    override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                                        android.util.Log.e(
                                            "MimoWebView",
                                            "JS CONSOLE: [${consoleMessage?.messageLevel()}] ${consoleMessage?.message()} (${consoleMessage?.sourceId()}:${consoleMessage?.lineNumber()})"
                                        )
                                        return true
                                    }
                                }
                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        android.util.Log.d("MimoWebView", "onPageStarted: $url")
                                    }
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        android.util.Log.d("MimoWebView", "onPageFinished: $url")
                                        super.onPageFinished(view, url)
                                        isPageLoaded = true
                                    }
                                    override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                                        android.util.Log.e("MimoWebView", "onReceivedError: $errorCode - $description for $failingUrl")
                                    }
                                }
                                loadDataWithBaseURL(
                                    null,
                                    simulation.htmlContent,
                                    "text/html",
                                    "UTF-8",
                                    null
                                )
                                webViewRef = this
                            }
                        },
                        update = { wv ->
                            webViewRef = wv
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                if (!isPageLoaded) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = AccentBlue,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}
