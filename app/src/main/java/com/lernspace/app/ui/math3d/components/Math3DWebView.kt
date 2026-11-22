package com.lernspace.app.ui.math3d.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.lernspace.app.core.math3d.GeoElementModel
import org.json.JSONArray
import org.json.JSONObject

class Math3DWebViewController(
    private val webViewProvider: () -> WebView?
) {
    fun setElements(elements: List<GeoElementModel>) {
        val wv = webViewProvider() ?: return
        val jsonArray = JSONArray()
        elements.forEach { elem ->
            val obj = JSONObject().apply {
                put("id", elem.id)
                put("type", elem.type.name)
                put("name", elem.name)
                put("color", elem.color)
                put("visible", elem.visible)
                put("expr", elem.expr)
                put("a", elem.a)
                put("b", elem.b)
                put("axis", elem.axis)
                put("angle", elem.angle)
                put("range", elem.range)
                put("x", elem.x)
                put("y", elem.y)
                put("z", elem.z)
                put("coeffA", elem.coeffA)
                put("coeffB", elem.coeffB)
                put("coeffC", elem.coeffC)
                put("coeffD", elem.coeffD)
                put("subType", elem.subType)
                put("size", elem.size)
                put("height", elem.height)
                put("radius", elem.radius)
            }
            jsonArray.put(obj)
        }
        val escapedJson = jsonArray.toString().replace("'", "\\'")
        wv.post {
            wv.evaluateJavascript("window.Math3D && window.Math3D.setElements('$escapedJson');", null)
        }
    }

    fun setVariable(name: String, value: Float) {
        val wv = webViewProvider() ?: return
        wv.post {
            wv.evaluateJavascript("window.Math3D && window.Math3D.setVariable('$name', $value);", null)
        }
    }

    fun setCameraView(preset: String) {
        val wv = webViewProvider() ?: return
        wv.post {
            wv.evaluateJavascript("window.Math3D && window.Math3D.setCameraView('$preset');", null)
        }
    }

    fun captureSnapshot() {
        val wv = webViewProvider() ?: return
        wv.post {
            wv.evaluateJavascript("window.Math3D && window.Math3D.captureSnapshot();", null)
        }
    }
}

class Math3DJavascriptBridge(
    private val onEngineReadyCallback: () -> Unit,
    private val onMetricsCallback: (String) -> Unit,
    private val onSnapshotCallback: (Bitmap) -> Unit
) {
    @JavascriptInterface
    fun onEngineReady(jsonStr: String) {
        onEngineReadyCallback()
    }

    @JavascriptInterface
    fun onElementsUpdated(jsonStr: String) {
        onMetricsCallback(jsonStr)
    }

    @JavascriptInterface
    fun onSnapshotCaptured(jsonStr: String) {
        try {
            val json = JSONObject(jsonStr)
            val dataUrl = json.optString("dataUrl", "")
            if (dataUrl.startsWith("data:image/png;base64,")) {
                val base64Data = dataUrl.substring("data:image/png;base64,".length)
                val imageBytes = Base64.decode(base64Data, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                if (bitmap != null) {
                    onSnapshotCallback(bitmap)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun Math3DWebView(
    onControllerReady: (Math3DWebViewController) -> Unit,
    onMetricsUpdated: (String) -> Unit,
    onSnapshotReady: (Bitmap) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = true
                    allowContentAccess = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    cacheMode = WebSettings.LOAD_NO_CACHE
                }
                setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                    }
                }

                val bridge = Math3DJavascriptBridge(
                    onEngineReadyCallback = {},
                    onMetricsCallback = { json -> post { onMetricsUpdated(json) } },
                    onSnapshotCallback = { bmp -> post { onSnapshotReady(bmp) } }
                )
                addJavascriptInterface(bridge, "LernspaceBridge")

                loadUrl("file:///android_asset/math3d/index.html")

                val controller = Math3DWebViewController { this }
                onControllerReady(controller)
            }
        },
        modifier = modifier.fillMaxSize()
    )
}
