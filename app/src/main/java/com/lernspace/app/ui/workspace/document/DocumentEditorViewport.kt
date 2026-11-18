package com.lernspace.app.ui.workspace.document

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.print.PrintAttributes
import android.print.PrintManager
import android.util.Base64
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.automirrored.filled.FormatIndentDecrease
import androidx.compose.material.icons.automirrored.filled.FormatIndentIncrease
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.lernspace.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

// ── ENUMS ────────────────────────────────────────────────────────────────────
enum class DocParagraphStyle(val label: String, val htmlTag: String) {
    NORMAL("Teks Normal", "p"),
    TITLE("Judul", "h1"),
    HEADING_1("Heading 1", "h2"),
    HEADING_2("Heading 2", "h3"),
    HEADING_3("Heading 3", "h4"),
    HEADING_4("Heading 4", "h5")
}

enum class DocFontFamily(val label: String, val css: String) {
    INTER("Inter (Default)", "'Inter', 'Roboto', sans-serif"),
    POPPINS("Poppins", "'Poppins', 'sans-serif-medium', sans-serif"),
    MONTSERRAT("Montserrat", "'Montserrat', 'sans-serif-condensed', sans-serif"),
    ROBOTO_LIGHT("Roboto Light", "'sans-serif-light', sans-serif"),
    MERRIWEATHER("Merriweather", "'Merriweather', Georgia, serif"),
    LORA("Lora (Jurnal)", "'Lora', 'Times New Roman', serif"),
    PLAYFAIR("Playfair Display", "'Playfair Display', Georgia, serif"),
    ROBOTO_MONO("Roboto Mono", "'Roboto Mono', 'Courier New', monospace"),
    CASUAL("Casual", "casual, 'Comic Sans MS', sans-serif"),
    CAVEAT("Caveat (Handwriting)", "'Caveat', cursive")
}

// ── THREAD-SAFE SYNC HOLDER (NOT COMPOSE STATE — PREVENTS RECOMPOSITION LOOP) ─
private class DocumentSyncState {
    var lastEmittedPacked: String? = null
    var lastAppliedDark: Boolean? = null
    var isReady: Boolean = false
}

// ── JS BRIDGE (ALWAYS DISPATCHES TO MAIN LOOPER) ──────────────────────────────
class DocumentJsBridge(
    private val mainHandler: Handler,
    private val onContentChanged: (String) -> Unit,
    private val onFormatStateChanged: (bold: Boolean, italic: Boolean, underline: Boolean, strike: Boolean, alignIdx: Int) -> Unit,
    private val onRequestReplaceImage: () -> Unit
) {
    @JavascriptInterface
    fun onContentChange(html: String) {
        mainHandler.post { onContentChanged(html) }
    }

    @JavascriptInterface
    fun onFormatState(bold: Boolean, italic: Boolean, underline: Boolean, strike: Boolean, alignIdx: Int) {
        mainHandler.post { onFormatStateChanged(bold, italic, underline, strike, alignIdx) }
    }

    @JavascriptInterface
    fun requestReplaceImage() {
        mainHandler.post { onRequestReplaceImage() }
    }
}

// ── IMAGE COMPRESSION TO BASE64 DATA URI ──────────────────────────────────────
private fun encodeUriToDataUri(context: Context, uri: Uri): String? {
    val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
    val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, bounds)

    val maxDim = 1280
    var sampleSize = 1
    while (bounds.outWidth / sampleSize > maxDim * 2 || bounds.outHeight / sampleSize > maxDim * 2) {
        sampleSize *= 2
    }
    val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    val decoded = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOpts) ?: return null

    val scaled = if (decoded.width > maxDim || decoded.height > maxDim) {
        val ratio = minOf(maxDim.toFloat() / decoded.width, maxDim.toFloat() / decoded.height)
        val w = (decoded.width * ratio).toInt().coerceAtLeast(1)
        val h = (decoded.height * ratio).toInt().coerceAtLeast(1)
        Bitmap.createScaledBitmap(decoded, w, h, true).also {
            if (it !== decoded) decoded.recycle()
        }
    } else {
        decoded
    }

    val out = ByteArrayOutputStream()
    val isPng = mimeType.contains("png", ignoreCase = true)
    if (isPng) {
        scaled.compress(Bitmap.CompressFormat.PNG, 95, out)
    } else {
        scaled.compress(Bitmap.CompressFormat.JPEG, 86, out)
    }
    scaled.recycle()

    val outMime = if (isPng) "image/png" else "image/jpeg"
    val b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    return "data:$outMime;base64,$b64"
}

// ── NORMALIZE PLAIN/MARKDOWN CONTENT INTO HTML IF NEEDED ──────────────────────
private fun normalizeInitialBody(raw: String): String {
    if (raw.isBlank()) return "<p><br></p>"
    val trimmed = raw.trim()
    val looksHtml = trimmed.contains("<p") || trimmed.contains("<div") ||
        trimmed.contains("<h1") || trimmed.contains("<h2") || trimmed.contains("<h3") ||
        trimmed.contains("<ul") || trimmed.contains("<ol") || trimmed.contains("<table") ||
        trimmed.contains("<img") || trimmed.contains("<br")
    if (looksHtml) return raw
    return raw.split("\n").joinToString("") { line ->
        val esc = line.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        if (esc.isBlank()) "<p><br></p>" else "<p>$esc</p>"
    }
}

private fun sanitizeFileName(raw: String): String {
    val clean = raw.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
    return clean.ifBlank { "Dokumen_LearnSpace" }
}

// ── NATIVE WEBVIEW PRINT / EXPORT PDF (MATCHES EXACT PAPER SIZE & MARGINS) ────
private fun printWebViewDocumentToPdf(
    context: Context,
    webView: WebView?,
    title: String,
    pageSetup: DocPageSetup
) {
    if (webView == null) {
        Toast.makeText(context, "Editor belum siap untuk dicetak", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        // Hide any active overlays before printing
        webView.evaluateJavascript("prepareForPrint();", null)
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager == null) {
            Toast.makeText(context, "Layanan cetak tidak tersedia di perangkat ini", Toast.LENGTH_SHORT).show()
            return
        }
        val jobName = sanitizeFileName(title)
        val adapter = webView.createPrintDocumentAdapter(jobName)
        val mediaSize = pageSetup.paperSize.toPrintMediaSize(
            isLandscape = pageSetup.orientation == DocPageOrientation.LANDSCAPE
        )
        val attrs = PrintAttributes.Builder()
            .setMediaSize(mediaSize)
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
            .build()
        printManager.print(jobName, adapter, attrs)
    } catch (e: Exception) {
        Log.e("DocEditor", "Print PDF failed", e)
        Toast.makeText(context, "Gagal membuka cetak PDF: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

// ── GENERATE CLEAN HEADERLESS GOOGLE DOCS-STYLE EDITOR HTML WITH PAGE SETUP ───
fun generateEditorHtml(
    isDark: Boolean,
    bodyContent: String,
    pageSetup: DocPageSetup
): String {
    val safeContent = normalizeInitialBody(bodyContent)
    val darkClass = if (isDark) "dark" else ""
    val initialConfigJson = pageSetup.toJsConfigJson()

    return """<!DOCTYPE html>
<html class="$darkClass">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Caveat:wght@500;700&family=Inter:wght@400;600;700&family=Lora:ital,wght@0,400;0,700;1,400&family=Merriweather:ital,wght@0,400;0,700;1,400&family=Montserrat:wght@400;600;700&family=Playfair+Display:ital,wght@0,400;0,700;1,400&family=Poppins:wght@400;600;700&family=Roboto+Mono:wght@400;600&display=swap">
<style id="dynamic-page-style"></style>
<style>
  :root {
    --bg: #EEF2F6;
    --paper: #FFFFFF;
    --text: #1E293B;
    --muted: #94A3B8;
    --border: #DCE3EC;
    --link: #2563EB;
    --tbl-border: #CBD5E1;
    --tbl-head: #F8FAFC;
    --quote-bg: #EFF6FF;
    --quote-text: #1D4ED8;
    --code-bg: #F1F5F9;
    --code-text: #0F172A;
    --toolbar-bg: #FFFFFF;
    --toolbar-shadow: 0 6px 24px rgba(15,23,42,0.16);
    --toolbar-hover: #F1F5F9;
    --sel: rgba(37,99,235,0.18);
    --guide-gap: #EEF2F6;
  }
  html.dark {
    --bg: #13141C;
    --paper: #1E202B;
    --text: #F1F5F9;
    --muted: #64748B;
    --border: #334155;
    --link: #60A5FA;
    --tbl-border: #475569;
    --tbl-head: #262A38;
    --quote-bg: #1E293B;
    --quote-text: #93C5FD;
    --code-bg: #0F172A;
    --code-text: #7DD3FC;
    --toolbar-bg: #262938;
    --toolbar-shadow: 0 8px 28px rgba(0,0,0,0.55);
    --toolbar-hover: #334155;
    --sel: rgba(96,165,250,0.28);
    --guide-gap: #13141C;
  }

  * { box-sizing: border-box; margin: 0; padding: 0; -webkit-tap-highlight-color: transparent; }
  html, body {
    background: var(--bg);
    min-height: 100%;
    font-family: 'Inter', 'Roboto', system-ui, sans-serif;
  }
  body {
    padding: 18px 14px 200px 14px;
  }
  body.pageless-mode {
    padding: 0 0 180px 0;
    background: var(--paper);
  }

  /* ── PAPER STACK & DISCRETE FULL-PAGE SHEETS (GOOGLE DOCS / WORD STYLE) ── */
  #paper-shell {
    position: relative;
    margin: 0 auto;
    max-width: 100%;
  }
  #paper {
    position: relative;
    margin: 0 auto;
    background: transparent;
    border: none;
    width: 794px;
    max-width: 100%;
    min-height: 1123px;
    padding: 96px 96px 96px 96px;
    transition: width 0.18s ease, padding 0.18s ease;
  }
  body.pageless-mode #paper {
    background: var(--paper);
    width: 100% !important;
    max-width: 960px !important;
    min-height: 100vh !important;
    border: none;
    border-radius: 0;
    box-shadow: none;
    padding: 28px 36px 96px 36px !important;
  }

  /* ── INDIVIDUAL FULL-SIZE PAGE SHEET CARDS BEHIND EDITOR ── */
  #page-sheets-bg {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    pointer-events: none;
    z-index: 1;
    display: flex;
    flex-direction: column;
    gap: 20px;
  }
  body.pageless-mode #page-sheets-bg { display: none; }
  .sheet-card {
    position: relative;
    width: 100%;
    background: var(--paper);
    border: 1px solid var(--border);
    border-radius: 3px;
    box-shadow: 0 2px 10px rgba(15,23,42,0.08);
    flex-shrink: 0;
  }
  .sheet-hdr {
    position: absolute;
    top: 16px;
    font-size: 11px;
    color: var(--muted);
    text-align: right;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  .sheet-ftr {
    position: absolute;
    bottom: 14px;
    font-size: 11px;
    color: var(--muted);
    text-align: center;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  /* ── MAIN EDITOR ── */
  #editor {
    position: relative;
    z-index: 5;
    outline: none;
    caret-color: #2563EB;
    color: var(--text);
    font-size: 15px;
    line-height: 1.65;
    word-wrap: break-word;
    overflow-wrap: break-word;
    min-height: 860px;
  }
  #editor::after { content: ""; display: table; clear: both; }
  #editor:empty::before {
    content: 'Mulai mengetik dokumen di sini...';
    color: var(--muted);
    font-style: italic;
    pointer-events: none;
  }

  #editor p, #editor div { margin: 0 0 6px 0; min-height: 1.5em; }
  #editor h1 { font-size: 26px; font-weight: 700; margin: 16px 0 8px 0; clear: both; }
  #editor h2 { font-size: 21px; font-weight: 700; margin: 14px 0 6px 0; clear: both; }
  #editor h3 { font-size: 18px; font-weight: 600; margin: 12px 0 5px 0; clear: both; }
  #editor h4 { font-size: 16px; font-weight: 600; margin: 10px 0 4px 0; clear: both; }
  #editor h5 { font-size: 14px; font-weight: 600; margin: 8px 0 3px 0; clear: both; }
  #editor a { color: var(--link); text-decoration: underline; }
  #editor hr { border: none; border-top: 1.5px solid var(--border); margin: 16px 0; clear: both; }

  /* ── MANUAL PAGE BREAK BLOCK ── */
  #editor .doc-page-break {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10px;
    margin: 22px 0;
    padding: 6px 0;
    user-select: none;
    clear: both;
    position: relative;
  }
  #editor .doc-page-break::before,
  #editor .doc-page-break::after {
    content: '';
    flex: 1;
    border-top: 1.5px dashed #94A3B8;
  }
  #editor .doc-page-break .pb-label {
    font-size: 10.5px;
    font-weight: 600;
    color: #64748B;
    background: var(--bg);
    padding: 2px 10px;
    border-radius: 10px;
    border: 1px solid var(--border);
  }
  #editor .doc-page-break .pb-del {
    background: transparent;
    border: none;
    color: #EF4444;
    font-size: 12px;
    font-weight: 700;
    cursor: pointer;
    padding: 2px 5px;
  }

  /* ── LISTS & CHECKLIST ── */
  #editor ul { list-style-type: disc; margin: 6px 0 10px 0; padding-left: 28px; }
  #editor ol { list-style-type: decimal; margin: 6px 0 10px 0; padding-left: 28px; }
  #editor li { margin: 3px 0; line-height: 1.65; }
  #editor ul.checklist { list-style: none; padding-left: 4px; }
  #editor ul.checklist li { display: flex; align-items: flex-start; gap: 8px; margin: 5px 0; }
  #editor ul.checklist li input[type="checkbox"] {
    margin-top: 5px; width: 16px; height: 16px;
    accent-color: #2563EB; cursor: pointer; flex-shrink: 0;
  }
  #editor ul.checklist li.done > span { text-decoration: line-through; opacity: 0.52; }

  /* ── QUOTE & CODE ── */
  #editor blockquote {
    border-left: 3px solid #2563EB;
    margin: 10px 0;
    padding: 8px 16px;
    background: var(--quote-bg);
    border-radius: 0 6px 6px 0;
    color: var(--quote-text);
    font-style: italic;
  }
  #editor pre {
    background: var(--code-bg);
    border: 1px solid var(--border);
    border-radius: 6px;
    padding: 12px 16px;
    font-family: 'Roboto Mono', 'Courier New', monospace;
    font-size: 13px;
    overflow-x: auto;
    margin: 10px 0;
    color: var(--code-text);
    white-space: pre-wrap;
  }

  /* ── TABLES ── */
  #editor table {
    border-collapse: collapse;
    width: 100%;
    margin: 12px 0;
    font-size: 14px;
    clear: both;
  }
  #editor table td, #editor table th {
    border: 1.5px solid var(--tbl-border);
    padding: 8px 12px;
    min-width: 60px;
    min-height: 36px;
    vertical-align: top;
    outline: none;
    word-break: break-word;
    line-height: 1.6;
  }
  #editor table th { background: var(--tbl-head); font-weight: 600; text-align: left; }
  #editor table td:focus, #editor table th:focus { outline: 2px solid #2563EB; outline-offset: -2px; }

  /* ── IMAGES & WRAP MODES ── */
  #editor img {
    max-width: 100%;
    height: auto;
    border-radius: 4px;
    cursor: pointer;
    user-select: none;
    -webkit-user-drag: none;
  }
  #editor img[data-wrap="break"], #editor img:not([data-wrap]) {
    display: block; float: none; margin: 12px auto;
  }
  #editor img[data-wrap="inline"] {
    display: inline-block; float: none; vertical-align: bottom; margin: 2px 6px;
  }
  #editor img[data-wrap="left"] {
    display: block; float: left; margin: 4px 18px 10px 0;
  }
  #editor img[data-wrap="right"] {
    display: block; float: right; margin: 4px 0 10px 18px;
  }

  /* ── MATH INLINE ── */
  .math-inline {
    font-family: 'Roboto Mono', 'Courier New', monospace;
    color: #1D4ED8;
    background: #EFF6FF;
    padding: 2px 7px;
    border-radius: 4px;
    font-size: 14px;
    display: inline-block;
  }

  /* ── OPTIONAL FLOATING WORD COUNT PILL (BOTTOM-LEFT, OFF BY DEFAULT) ── */
  #wc-pill {
    display: none;
    position: fixed;
    left: 14px;
    bottom: 14px;
    background: var(--toolbar-bg);
    color: var(--text);
    border: 1px solid var(--border);
    border-radius: 20px;
    padding: 6px 13px;
    font-size: 11.5px;
    font-weight: 600;
    box-shadow: var(--toolbar-shadow);
    z-index: 8800;
    cursor: pointer;
    user-select: none;
    align-items: center;
    gap: 6px;
  }
  #wc-pill.visible { display: inline-flex; }
  #wc-popover {
    display: none;
    position: fixed;
    left: 14px;
    bottom: 52px;
    background: var(--toolbar-bg);
    color: var(--text);
    border: 1px solid var(--border);
    border-radius: 12px;
    padding: 12px 16px;
    min-width: 215px;
    box-shadow: var(--toolbar-shadow);
    z-index: 8900;
    font-size: 12px;
    user-select: none;
  }
  #wc-popover.visible { display: block; }
  .wc-stat-row {
    display: flex; justify-content: space-between; padding: 4px 0; border-bottom: 1px solid var(--border);
  }
  .wc-stat-row:last-child { border-bottom: none; }
  .wc-stat-label { color: var(--muted); }
  .wc-stat-val { font-weight: 700; color: var(--text); }

  /* ── GOOGLE DOCS IMAGE RESIZE OVERLAY ── */
  #img-resizer {
    display: none; position: fixed; border: 2px solid #2563EB; border-radius: 4px;
    pointer-events: none; z-index: 9000; box-shadow: 0 0 0 1px rgba(255,255,255,0.6);
  }
  #img-resizer.visible { display: block; }
  .rz-handle {
    position: absolute; width: 34px; height: 34px; pointer-events: auto; touch-action: none;
    display: flex; align-items: center; justify-content: center; z-index: 9005;
  }
  .rz-handle::after {
    content: ''; width: 12px; height: 12px; background: #FFFFFF;
    border: 2.5px solid #2563EB; border-radius: 50%; box-shadow: 0 1px 4px rgba(0,0,0,0.28);
  }
  .rz-handle[data-dir="nw"] { top: -17px; left: -17px; cursor: nwse-resize; }
  .rz-handle[data-dir="ne"] { top: -17px; right: -17px; cursor: nesw-resize; }
  .rz-handle[data-dir="sw"] { bottom: -17px; left: -17px; cursor: nesw-resize; }
  .rz-handle[data-dir="se"] { bottom: -17px; right: -17px; cursor: nwse-resize; }
  .rz-handle[data-dir="w"]  { top: 50%; left: -17px; transform: translateY(-50%); cursor: ew-resize; }
  .rz-handle[data-dir="e"]  { top: 50%; right: -17px; transform: translateY(-50%); cursor: ew-resize; }
  .rz-handle[data-dir="w"]::after, .rz-handle[data-dir="e"]::after { width: 7px; height: 18px; border-radius: 4px; }
  #img-size-badge {
    display: none; position: absolute; top: 8px; right: 8px;
    background: rgba(15, 23, 42, 0.85); color: #FFFFFF; font-size: 11px; font-weight: 600;
    padding: 3px 8px; border-radius: 6px; pointer-events: none; white-space: nowrap;
  }

  /* ── FLOATING CONTEXT TOOLBARS ── */
  .floating-toolbar {
    display: none; position: fixed; background: var(--toolbar-bg);
    border: 1px solid var(--border); border-radius: 10px; box-shadow: var(--toolbar-shadow);
    padding: 5px 6px; gap: 3px; flex-direction: row; align-items: center;
    z-index: 9500; flex-wrap: wrap; max-width: calc(100vw - 16px); user-select: none;
  }
  .floating-toolbar.visible { display: flex; }
  .ft-btn {
    background: transparent; border: 1px solid transparent; border-radius: 6px;
    padding: 5px 8px; font-size: 11.5px; font-weight: 600; color: var(--text);
    cursor: pointer; white-space: nowrap; display: inline-flex; align-items: center; gap: 5px; line-height: 1.2;
  }
  .ft-btn:active { background: var(--toolbar-hover); }
  .ft-btn.active { background: rgba(37, 99, 235, 0.13); border-color: rgba(37, 99, 235, 0.38); color: #2563EB; }
  .ft-btn.danger { color: #DC2626; }
  .ft-sep { width: 1px; height: 20px; background: var(--border); margin: 0 3px; }
  .ft-icon { width: 16px; height: 16px; display: inline-block; flex-shrink: 0; }

  ::selection { background: var(--sel); }

  /* ── CLEAN NATIVE PDF / PRINT STYLES ── */
  @media print {
    html, body { background: #FFFFFF !important; padding: 0 !important; margin: 0 !important; }
    #paper {
      width: 100% !important; max-width: none !important; min-height: 0 !important;
      border: none !important; box-shadow: none !important; border-radius: 0 !important;
      background: #FFFFFF !important; padding: 0 !important; margin: 0 !important;
    }
    #editor { color: #0F172A !important; min-height: 0 !important; }
    #wc-pill, #wc-popover, #img-resizer, #img-toolbar, #tbl-toolbar, #page-sheets-bg { display: none !important; }
    #editor .doc-page-break {
      margin: 0 !important; padding: 0 !important; height: 0 !important;
      break-after: page; page-break-after: always; visibility: hidden;
    }
    #editor .doc-page-break::before, #editor .doc-page-break::after,
    #editor .doc-page-break .pb-label, #editor .doc-page-break .pb-del { display: none !important; }
  }
</style>
</head>
<body>

<!-- Optional Word Count Pill (Bottom-Left, off by default) -->
<div id="wc-pill" onclick="toggleWcPopover(event)">
  <span id="wc-pill-text">0 kata</span>
</div>
<div id="wc-popover">
  <div class="wc-stat-row"><span class="wc-stat-label">Halaman</span><span class="wc-stat-val" id="st-pages">1</span></div>
  <div class="wc-stat-row"><span class="wc-stat-label">Kata</span><span class="wc-stat-val" id="st-words">0</span></div>
  <div class="wc-stat-row"><span class="wc-stat-label">Karakter</span><span class="wc-stat-val" id="st-chars">0</span></div>
  <div class="wc-stat-row"><span class="wc-stat-label">Tanpa spasi</span><span class="wc-stat-val" id="st-nospace">0</span></div>
  <div class="wc-stat-row"><span class="wc-stat-label">Paragraf</span><span class="wc-stat-val" id="st-paras">0</span></div>
  <div class="wc-stat-row"><span class="wc-stat-label">Waktu baca</span><span class="wc-stat-val" id="st-read">&lt; 1 mnt</span></div>
</div>

<!-- Native Google Docs Style Image Resize Frame -->
<div id="img-resizer">
  <div class="rz-handle" data-dir="nw"></div>
  <div class="rz-handle" data-dir="ne"></div>
  <div class="rz-handle" data-dir="sw"></div>
  <div class="rz-handle" data-dir="se"></div>
  <div class="rz-handle" data-dir="w"></div>
  <div class="rz-handle" data-dir="e"></div>
  <div id="img-size-badge"></div>
</div>

<!-- Native Google Docs Style Image Wrap & Actions Bar -->
<div id="img-toolbar" class="floating-toolbar">
  <button class="ft-btn" id="wrap-inline" onclick="setImageWrap('inline')" title="Sejajar teks">
    <svg class="ft-icon" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.6">
      <line x1="2" y1="4" x2="18" y2="4"/><rect x="3" y="8" width="6" height="6" rx="1" fill="currentColor" opacity="0.25"/><line x1="11" y1="14" x2="18" y2="14"/><line x1="2" y1="17" x2="18" y2="17"/>
    </svg>
    <span>Sejajar</span>
  </button>
  <button class="ft-btn" id="wrap-left" onclick="setImageWrap('left')" title="Bungkus teks (Kiri)">
    <svg class="ft-icon" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.6">
      <rect x="2" y="4" width="7" height="7" rx="1" fill="currentColor" opacity="0.25"/><line x1="11" y1="5" x2="18" y2="5"/><line x1="11" y1="8.5" x2="18" y2="8.5"/><line x1="11" y1="12" x2="18" y2="12"/><line x1="2" y1="16" x2="18" y2="16"/>
    </svg>
    <span>Kiri</span>
  </button>
  <button class="ft-btn" id="wrap-break" onclick="setImageWrap('break')" title="Pisah teks (Tengah)">
    <svg class="ft-icon" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.6">
      <line x1="2" y1="3.5" x2="18" y2="3.5"/><rect x="5" y="6.5" width="10" height="7" rx="1" fill="currentColor" opacity="0.25"/><line x1="2" y1="16.5" x2="18" y2="16.5"/>
    </svg>
    <span>Tengah</span>
  </button>
  <button class="ft-btn" id="wrap-right" onclick="setImageWrap('right')" title="Bungkus teks (Kanan)">
    <svg class="ft-icon" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.6">
      <rect x="11" y="4" width="7" height="7" rx="1" fill="currentColor" opacity="0.25"/><line x1="2" y1="5" x2="9" y2="5"/><line x1="2" y1="8.5" x2="9" y2="8.5"/><line x1="2" y1="12" x2="9" y2="12"/><line x1="2" y1="16" x2="18" y2="16"/>
    </svg>
    <span>Kanan</span>
  </button>
  <div class="ft-sep"></div>
  <button class="ft-btn" onclick="triggerReplaceImage()" title="Ganti gambar">Ganti</button>
  <button class="ft-btn danger" onclick="deleteSelectedImage()" title="Hapus gambar">Hapus</button>
</div>

<!-- Table Context Bar -->
<div id="tbl-toolbar" class="floating-toolbar">
  <button class="ft-btn" onclick="addRowAbove()">↑ Baris</button>
  <button class="ft-btn" onclick="addRowBelow()">↓ Baris</button>
  <div class="ft-sep"></div>
  <button class="ft-btn" onclick="addColLeft()">← Kolom</button>
  <button class="ft-btn" onclick="addColRight()">→ Kolom</button>
  <div class="ft-sep"></div>
  <button class="ft-btn danger" onclick="deleteCurrentRow()">✕ Baris</button>
  <button class="ft-btn danger" onclick="deleteCurrentCol()">✕ Kolom</button>
  <button class="ft-btn danger" onclick="deleteTable()">Hapus Tabel</button>
</div>

<!-- Pure Clean Document Paper with Discrete Full-Size Sheets -->
<div id="paper-shell">
  <div id="paper">
    <div id="page-sheets-bg"></div>
    <div id="editor" contenteditable="true" spellcheck="true">$safeContent</div>
  </div>
</div>

<script>
var paper = document.getElementById('paper');
var editor = document.getElementById('editor');
var pageSheetsBg = document.getElementById('page-sheets-bg');
var dynamicPageStyle = document.getElementById('dynamic-page-style');

var imgResizer = document.getElementById('img-resizer');
var imgSizeBadge = document.getElementById('img-size-badge');
var imgToolbar = document.getElementById('img-toolbar');
var tblToolbar = document.getElementById('tbl-toolbar');
var wcPill = document.getElementById('wc-pill');
var wcPopover = document.getElementById('wc-popover');

var debounceTimer = null;
var savedRange = null;
var selectedImg = null;
var currentCell = null;
var currentRow = null;
var currentTable = null;
var lastFmtKey = '';
var wordCountEnabled = false;
var pageConfig = $initialConfigJson;
var estimatedPageCount = 1;
var lastSheetsRenderKey = '';
var PAGE_GAP = 20;

try { document.execCommand('defaultParagraphSeparator', false, 'p'); } catch(_) {}

function getScaledPageMetrics() {
  var cfg = pageConfig;
  var availW = Math.max(300, window.innerWidth - 28);
  var scale = Math.min(1, availW / cfg.widthPx);
  var pageH = Math.max(400, Math.round(cfg.heightPx * scale));
  var padT = Math.max(16, Math.round(cfg.mTopPx * scale));
  var padR = Math.max(16, Math.round(cfg.mRightPx * scale));
  var padB = Math.max(16, Math.round(cfg.mBottomPx * scale));
  var padL = Math.max(16, Math.round(cfg.mLeftPx * scale));
  return { scale: scale, pageH: pageH, padT: padT, padR: padR, padB: padB, padL: padL };
}

function measureContentHeight() {
  var edRect = editor.getBoundingClientRect();
  var maxBottom = 0;
  var children = editor.children;
  for (var i = 0; i < children.length; i++) {
    var r = children[i].getBoundingClientRect();
    var b = r.bottom - edRect.top;
    if (b > maxBottom) maxBottom = b;
  }
  if (maxBottom === 0 && (editor.textContent || '').trim().length > 0) {
    try {
      var range = document.createRange();
      range.selectNodeContents(editor);
      var rr = range.getBoundingClientRect();
      maxBottom = Math.max(0, rr.bottom - edRect.top);
    } catch(_) {}
  }
  return maxBottom;
}

// ── PAGE SETUP ENGINE (FULL-SIZE DISCRETE SHEETS) ──
function applyPageSetup(cfg) {
  if (!cfg) return;
  pageConfig = cfg;
  lastSheetsRenderKey = '';
  document.body.classList.toggle('pageless-mode', !!cfg.pageless);

  // Update @page print CSS dynamically
  dynamicPageStyle.textContent =
    '@page { size: ' + cfg.widthMm + 'mm ' + cfg.heightMm + 'mm; ' +
    'margin: ' + cfg.mTopCm + 'cm ' + cfg.mRightCm + 'cm ' + cfg.mBottomCm + 'cm ' + cfg.mLeftCm + 'cm; }';

  if (!cfg.pageless) {
    var m = getScaledPageMetrics();
    paper.style.width = cfg.widthPx + 'px';
    paper.style.padding = m.padT + 'px ' + m.padR + 'px ' + m.padB + 'px ' + m.padL + 'px';
    editor.style.minHeight = Math.max(200, m.pageH - m.padT - m.padB) + 'px';
  } else {
    paper.style.minHeight = '';
    editor.style.minHeight = '';
  }

  requestAnimationFrame(rebuildPageGuides);
}

function rebuildPageGuides() {
  if (!pageConfig || pageConfig.pageless) {
    pageSheetsBg.innerHTML = '';
    estimatedPageCount = 1;
    lastSheetsRenderKey = 'pageless';
    updateWordCount();
    return;
  }

  var m = getScaledPageMetrics();
  var firstPageUsable = Math.max(180, m.pageH - m.padT - m.padB);
  var stride = m.pageH + PAGE_GAP;
  var contentH = measureContentHeight();

  // Snap strictly to whole full-size pages!
  // The moment text exceeds Page 1, immediately render Page 2 at full sheet height!
  var autoPages = 1;
  if (contentH > firstPageUsable + 2) {
    autoPages = 1 + Math.ceil((contentH - firstPageUsable) / stride);
  }
  var manualBreaks = editor.querySelectorAll('.doc-page-break').length;
  var pageCount = Math.max(1, autoPages, manualBreaks + 1);
  estimatedPageCount = pageCount;

  // Snap #paper height to exact N full sheets + gaps
  var totalPaperH = pageCount * m.pageH + (pageCount - 1) * PAGE_GAP;
  paper.style.minHeight = totalPaperH + 'px';

  var renderKey = pageCount + '|' + m.pageH + '|' + m.padL + '|' + m.padR + '|' +
    (pageConfig.headerText || '') + '|' + (pageConfig.footerText || '') + '|' + pageConfig.showPageNumbers;

  if (renderKey !== lastSheetsRenderKey) {
    lastSheetsRenderKey = renderKey;
    var frag = document.createDocumentFragment();
    for (var p = 1; p <= pageCount; p++) {
      var card = document.createElement('div');
      card.className = 'sheet-card';
      card.style.height = m.pageH + 'px';

      if (pageConfig.headerText) {
        var hdr = document.createElement('div');
        hdr.className = 'sheet-hdr';
        hdr.style.left = m.padL + 'px';
        hdr.style.right = m.padR + 'px';
        hdr.textContent = pageConfig.headerText;
        card.appendChild(hdr);
      }

      var ftrParts = [];
      if (pageConfig.footerText) ftrParts.push(pageConfig.footerText);
      if (pageConfig.showPageNumbers) ftrParts.push('Halaman ' + p);
      if (ftrParts.length > 0) {
        var ftr = document.createElement('div');
        ftr.className = 'sheet-ftr';
        ftr.style.left = m.padL + 'px';
        ftr.style.right = m.padR + 'px';
        ftr.textContent = ftrParts.join('  ·  ');
        card.appendChild(ftr);
      }

      frag.appendChild(card);
    }
    pageSheetsBg.innerHTML = '';
    pageSheetsBg.appendChild(frag);
  }

  updateWordCount();
}

window.addEventListener('resize', function() {
  applyPageSetup(pageConfig);
});

if (typeof ResizeObserver !== 'undefined') {
  var ro = new ResizeObserver(function() { rebuildPageGuides(); });
  ro.observe(editor);
}

// ── MANUAL PAGE BREAK ──
function insertPageBreak() {
  restoreSelection();
  var brk = document.createElement('div');
  brk.className = 'doc-page-break';
  brk.setAttribute('contenteditable', 'false');
  brk.innerHTML = '<span class="pb-label">Pemisah Halaman</span><button class="pb-del" onclick="removePageBreak(this)">✕</button>';
  var pAfter = document.createElement('p');
  pAfter.innerHTML = '<br>';

  var sel = window.getSelection();
  if (sel && sel.rangeCount > 0 && editor.contains(sel.getRangeAt(0).commonAncestorContainer)) {
    var block = findClosestBlock(sel.getRangeAt(0).startContainer);
    if (block && block.parentNode === editor) {
      editor.insertBefore(brk, block.nextSibling);
      editor.insertBefore(pAfter, brk.nextSibling);
    } else {
      var range = sel.getRangeAt(0);
      range.deleteContents();
      range.insertNode(pAfter);
      pAfter.parentNode.insertBefore(brk, pAfter);
    }
  } else {
    editor.appendChild(brk);
    editor.appendChild(pAfter);
  }

  var nr = document.createRange();
  nr.setStart(pAfter, 0);
  nr.collapse(true);
  sel.removeAllRanges();
  sel.addRange(nr);
  rebuildPageGuides();
  notifyChange();
}

function removePageBreak(btn) {
  var brk = btn.closest('.doc-page-break');
  if (brk && brk.parentNode) brk.parentNode.removeChild(brk);
  rebuildPageGuides();
  notifyChange();
}

function hydratePageBreaks() {
  editor.querySelectorAll('.doc-page-break').forEach(function(brk) {
    brk.setAttribute('contenteditable', 'false');
    if (!brk.querySelector('.pb-label')) {
      brk.innerHTML = '<span class="pb-label">Pemisah Halaman</span><button class="pb-del" onclick="removePageBreak(this)">✕</button>';
    }
  });
}

// ── LINE SPACING & INDENTATION (SKRIPSI / MAKALAH READY) ──
function findClosestBlock(node) {
  while (node && node !== editor) {
    if (node.nodeType === 1 && /^(P|DIV|H1|H2|H3|H4|H5|H6|LI|BLOCKQUOTE|PRE)$/.test(node.tagName)) {
      return node;
    }
    node = node.parentNode;
  }
  return null;
}

function getSelectedBlocks() {
  var blocks = [];
  var sel = window.getSelection();
  if (!sel || sel.rangeCount === 0) return blocks;
  var range = sel.getRangeAt(0);
  var startBlock = findClosestBlock(range.startContainer);
  var endBlock = findClosestBlock(range.endContainer);
  if (!startBlock && !endBlock) return blocks;
  if (startBlock && (!endBlock || startBlock === endBlock)) {
    return [startBlock];
  }
  var all = editor.querySelectorAll('p, div:not(.doc-page-break), h1, h2, h3, h4, h5, li, blockquote');
  var inRange = false;
  for (var i = 0; i < all.length; i++) {
    var el = all[i];
    if (el === startBlock) inRange = true;
    if (inRange) blocks.push(el);
    if (el === endBlock) break;
  }
  return blocks.length ? blocks : (startBlock ? [startBlock] : []);
}

function setLineSpacing(multiplier) {
  restoreSelection();
  var blocks = getSelectedBlocks();
  if (blocks.length === 0) {
    editor.style.lineHeight = String(multiplier);
  } else {
    blocks.forEach(function(b) { b.style.lineHeight = String(multiplier); });
  }
  rebuildPageGuides();
  notifyChange();
}

function adjustIndent(deltaPx) {
  restoreSelection();
  var blocks = getSelectedBlocks();
  if (blocks.length === 0) return;
  blocks.forEach(function(b) {
    var cur = parseInt(b.style.marginLeft || '0', 10) || 0;
    var next = Math.max(0, Math.min(288, cur + deltaPx));
    b.style.marginLeft = next > 0 ? (next + 'px') : '';
  });
  notifyChange();
}

function toggleFirstLineIndent() {
  restoreSelection();
  var blocks = getSelectedBlocks();
  if (blocks.length === 0) return;
  var hasIndent = blocks[0].style.textIndent && blocks[0].style.textIndent !== '0px' && blocks[0].style.textIndent !== '0cm';
  blocks.forEach(function(b) {
    b.style.textIndent = hasIndent ? '' : '1.27cm';
  });
  notifyChange();
}

// ── OPTIONAL WORD COUNT ENGINE ──
function updateWordCount() {
  if (!wordCountEnabled) return;
  var raw = (editor.innerText || '');
  var trimmed = raw.trim();
  var words = trimmed ? trimmed.split(/\s+/).filter(Boolean).length : 0;
  var chars = trimmed.length;
  var noSpace = trimmed.replace(/\s+/g, '').length;
  var paras = trimmed ? trimmed.split(/\n+/).filter(function(l){ return l.trim().length > 0; }).length : 0;
  var mins = Math.max(1, Math.round(words / 200));

  document.getElementById('wc-pill-text').textContent = words + ' kata · ' + chars + ' karakter';
  document.getElementById('st-pages').textContent = pageConfig.pageless ? 'Pageless' : String(estimatedPageCount);
  document.getElementById('st-words').textContent = words;
  document.getElementById('st-chars').textContent = chars;
  document.getElementById('st-nospace').textContent = noSpace;
  document.getElementById('st-paras').textContent = paras;
  document.getElementById('st-read').textContent = words < 100 ? '< 1 mnt' : mins + ' mnt';
}

function setWordCountVisible(show) {
  wordCountEnabled = !!show;
  wcPill.classList.toggle('visible', wordCountEnabled);
  if (!wordCountEnabled) wcPopover.classList.remove('visible');
  else updateWordCount();
}

function toggleWcPopover(e) {
  e.stopPropagation();
  updateWordCount();
  wcPopover.classList.toggle('visible');
}

function prepareForPrint() {
  deselectImage();
  hideTableToolbar();
  wcPopover.classList.remove('visible');
}

// ── CHANGE NOTIFICATION ──
function notifyChange() {
  updateWordCount();
  clearTimeout(debounceTimer);
  debounceTimer = setTimeout(function() {
    LernspaceDoc.onContentChange(editor.innerHTML);
  }, 350);
}

function flushChangeNow() {
  clearTimeout(debounceTimer);
  updateWordCount();
  LernspaceDoc.onContentChange(editor.innerHTML);
}

editor.addEventListener('input', function() {
  if (selectedImg) deselectImage();
  requestAnimationFrame(rebuildPageGuides);
  notifyChange();
  scheduleScrollIntoView();
});

editor.addEventListener('blur', function() {
  flushChangeNow();
});

// ── SELECTION TRACKING & ACTIVE TOOLBAR SYNC ──
document.addEventListener('selectionchange', function() {
  var sel = window.getSelection();
  if (!sel || sel.rangeCount === 0) return;
  var range = sel.getRangeAt(0);
  if (editor.contains(range.commonAncestorContainer)) {
    savedRange = range.cloneRange();
    syncToolbarState();
  }
});

function restoreSelection() {
  editor.focus();
  if (savedRange) {
    try {
      var sel = window.getSelection();
      sel.removeAllRanges();
      sel.addRange(savedRange);
    } catch(_) {}
  }
}

function syncToolbarState() {
  try {
    var b = document.queryCommandState('bold');
    var i = document.queryCommandState('italic');
    var u = document.queryCommandState('underline');
    var s = document.queryCommandState('strikeThrough');
    var alignIdx = 0;
    if (document.queryCommandState('justifyCenter')) alignIdx = 1;
    else if (document.queryCommandState('justifyRight')) alignIdx = 2;
    else if (document.queryCommandState('justifyFull')) alignIdx = 3;
    var key = b + '|' + i + '|' + u + '|' + s + '|' + alignIdx;
    if (key !== lastFmtKey) {
      lastFmtKey = key;
      LernspaceDoc.onFormatState(b, i, u, s, alignIdx);
    }
  } catch(_) {}
}

// ── GENTLE SCROLL INTO VIEW ──
var scrollTimer = null;
function scheduleScrollIntoView() {
  clearTimeout(scrollTimer);
  scrollTimer = setTimeout(scrollCaretIntoView, 60);
}
function scrollCaretIntoView() {
  var sel = window.getSelection();
  if (!sel || sel.rangeCount === 0 || !sel.isCollapsed) return;
  var range = sel.getRangeAt(0);
  if (!editor.contains(range.commonAncestorContainer)) return;
  var rect = range.getBoundingClientRect();
  if (!rect || (rect.top === 0 && rect.bottom === 0)) return;
  var vh = (window.visualViewport && window.visualViewport.height) || window.innerHeight;
  if (rect.bottom > vh - 44) {
    window.scrollBy({ top: rect.bottom - vh + 72, behavior: 'auto' });
  } else if (rect.top < 16) {
    window.scrollBy({ top: rect.top - 48, behavior: 'auto' });
  }
}
if (window.visualViewport) {
  window.visualViewport.addEventListener('resize', function() {
    scheduleScrollIntoView();
    if (selectedImg) repositionImageOverlay();
  });
}

// ── FORMATTING COMMANDS ──
function exec(cmd, val) {
  restoreSelection();
  document.execCommand(cmd, false, val !== undefined ? val : null);
  syncToolbarState();
  notifyChange();
}
function formatBlock(tag) { exec('formatBlock', '<' + tag + '>'); }
function toggleBold() { exec('bold'); }
function toggleItalic() { exec('italic'); }
function toggleUnderline() { exec('underline'); }
function toggleStrikethrough() { exec('strikeThrough'); }
function setFontFamily(css) { exec('fontName', css); }
function setTextColor(color) { exec('foreColor', color); }
function setHighlight(color) { exec('hiliteColor', color || 'transparent'); }
function alignText(mode) {
  var cmds = { left: 'justifyLeft', center: 'justifyCenter', right: 'justifyRight', justify: 'justifyFull' };
  exec(cmds[mode] || 'justifyLeft');
}
function undo() { exec('undo'); }
function redo() { exec('redo'); }
function clearFormatting() { exec('removeFormat'); }

function setFontSize(px) {
  restoreSelection();
  document.execCommand('fontSize', false, '7');
  var fonts = editor.querySelectorAll('font[size="7"]');
  fonts.forEach(function(el) {
    var span = document.createElement('span');
    span.style.fontSize = px + 'px';
    while (el.firstChild) span.appendChild(el.firstChild);
    el.parentNode.replaceChild(span, el);
  });
  notifyChange();
}

// ── LISTS & BLOCKS ──
function insertBulletList() { exec('insertUnorderedList'); }
function insertNumberedList() { exec('insertOrderedList'); }
function insertBlockquote() { exec('formatBlock', '<blockquote>'); }
function insertHR() { exec('insertHorizontalRule'); }

function insertChecklist() {
  restoreSelection();
  var sel = window.getSelection();
  var ul = document.createElement('ul');
  ul.className = 'checklist';
  var li = document.createElement('li');
  var cb = document.createElement('input');
  cb.type = 'checkbox';
  bindCheckbox(cb, li);
  var span = document.createElement('span');
  span.textContent = (sel && sel.toString()) ? sel.toString() : 'Tugas baru';
  li.appendChild(cb);
  li.appendChild(span);
  ul.appendChild(li);

  if (sel && sel.rangeCount > 0) {
    var range = sel.getRangeAt(0);
    range.deleteContents();
    range.insertNode(ul);
  } else {
    editor.appendChild(ul);
  }
  var nr = document.createRange();
  nr.selectNodeContents(span);
  nr.collapse(false);
  sel.removeAllRanges();
  sel.addRange(nr);
  notifyChange();
}

function bindCheckbox(cb, li) {
  cb.onchange = function() {
    li.classList.toggle('done', cb.checked);
    if (cb.checked) cb.setAttribute('checked', 'checked');
    else cb.removeAttribute('checked');
    notifyChange();
  };
}
function attachChecklistHandlers() {
  editor.querySelectorAll('ul.checklist li').forEach(function(li) {
    var cb = li.querySelector('input[type="checkbox"]');
    if (cb) bindCheckbox(cb, li);
  });
}

// ── LINK & MATH ──
function insertLink(text, url) {
  restoreSelection();
  var sel = window.getSelection();
  var a = document.createElement('a');
  a.href = url;
  a.textContent = text || (sel && sel.toString()) || url;
  if (sel && sel.rangeCount > 0) {
    var range = sel.getRangeAt(0);
    range.deleteContents();
    range.insertNode(a);
    var sp = document.createTextNode('\u00A0');
    a.parentNode.insertBefore(sp, a.nextSibling);
    var nr = document.createRange();
    nr.setStartAfter(sp);
    nr.collapse(true);
    sel.removeAllRanges();
    sel.addRange(nr);
  } else {
    editor.appendChild(a);
  }
  notifyChange();
}

function insertMath(latex) {
  restoreSelection();
  var sel = window.getSelection();
  var span = document.createElement('span');
  span.className = 'math-inline';
  span.textContent = latex;
  span.setAttribute('data-latex', latex);
  if (sel && sel.rangeCount > 0) {
    var range = sel.getRangeAt(0);
    range.deleteContents();
    range.insertNode(span);
    var sp = document.createTextNode('\u00A0');
    span.parentNode.insertBefore(sp, span.nextSibling);
    var nr = document.createRange();
    nr.setStartAfter(sp);
    nr.collapse(true);
    sel.removeAllRanges();
    sel.addRange(nr);
  } else {
    editor.appendChild(span);
  }
  notifyChange();
}

// ── NATIVE GOOGLE DOCS IMAGE INSERT, DRAG-RESIZE & WRAP ──
function insertImage(dataUri) {
  restoreSelection();
  var img = document.createElement('img');
  img.setAttribute('data-wrap', 'break');
  img.style.width = Math.min(360, Math.round((editor.clientWidth || 500) * 0.72)) + 'px';
  img.onload = function() { if (selectedImg === img) repositionImageOverlay(); rebuildPageGuides(); };
  img.src = dataUri;
  bindImageEvents(img);

  var sel = window.getSelection();
  if (sel && sel.rangeCount > 0 && editor.contains(sel.getRangeAt(0).commonAncestorContainer)) {
    var range = sel.getRangeAt(0);
    range.deleteContents();
    range.insertNode(img);
    var nr = document.createRange();
    nr.setStartAfter(img);
    nr.collapse(true);
    sel.removeAllRanges();
    sel.addRange(nr);
  } else {
    editor.appendChild(img);
  }
  notifyChange();
  setTimeout(function() { selectImage(img); }, 80);
}

function replaceSelectedImage(dataUri) {
  if (!selectedImg) { insertImage(dataUri); return; }
  var target = selectedImg;
  target.onload = function() { if (selectedImg === target) repositionImageOverlay(); };
  target.src = dataUri;
  setTimeout(function() { repositionImageOverlay(); notifyChange(); }, 80);
}

function bindImageEvents(img) {
  img.onclick = function(e) {
    e.preventDefault();
    e.stopPropagation();
    selectImage(img);
  };
}
function attachImageHandlers() {
  editor.querySelectorAll('img').forEach(bindImageEvents);
}

function selectImage(img) {
  hideTableToolbar();
  selectedImg = img;
  if (document.activeElement === editor) editor.blur();
  imgResizer.classList.add('visible');
  imgToolbar.classList.add('visible');
  updateWrapButtons();
  repositionImageOverlay();
}

function deselectImage() {
  selectedImg = null;
  imgResizer.classList.remove('visible');
  imgToolbar.classList.remove('visible');
  imgSizeBadge.style.display = 'none';
}

function updateWrapButtons() {
  if (!selectedImg) return;
  var mode = selectedImg.getAttribute('data-wrap') || 'break';
  ['inline', 'left', 'break', 'right'].forEach(function(m) {
    var btn = document.getElementById('wrap-' + m);
    if (btn) btn.classList.toggle('active', m === mode);
  });
}

function setImageWrap(mode) {
  if (!selectedImg) return;
  selectedImg.setAttribute('data-wrap', mode);
  var maxW = editor.clientWidth || 600;
  var curW = selectedImg.getBoundingClientRect().width;
  if ((mode === 'left' || mode === 'right') && curW > maxW * 0.62) {
    selectedImg.style.width = Math.round(maxW * 0.45) + 'px';
  } else if (mode === 'inline' && curW > maxW * 0.5) {
    selectedImg.style.width = Math.round(maxW * 0.35) + 'px';
  }
  updateWrapButtons();
  requestAnimationFrame(repositionImageOverlay);
  notifyChange();
}

function triggerReplaceImage() {
  if (!selectedImg) return;
  LernspaceDoc.requestReplaceImage();
}

function deleteSelectedImage() {
  if (!selectedImg) return;
  var img = selectedImg;
  deselectImage();
  if (img.parentNode) img.parentNode.removeChild(img);
  rebuildPageGuides();
  notifyChange();
}

function repositionImageOverlay() {
  if (!selectedImg || !document.body.contains(selectedImg)) {
    deselectImage();
    return;
  }
  var rect = selectedImg.getBoundingClientRect();
  imgResizer.style.left = rect.left + 'px';
  imgResizer.style.top = rect.top + 'px';
  imgResizer.style.width = rect.width + 'px';
  imgResizer.style.height = rect.height + 'px';

  var tbW = imgToolbar.offsetWidth || 320;
  var tbH = imgToolbar.offsetHeight || 42;
  var vh = (window.visualViewport && window.visualViewport.height) || window.innerHeight;
  var topPos = rect.bottom + 10;
  if (topPos + tbH > vh - 10) topPos = Math.max(8, rect.top - tbH - 10);
  var leftPos = rect.left + (rect.width - tbW) / 2;
  leftPos = Math.max(8, Math.min(leftPos, window.innerWidth - tbW - 8));
  imgToolbar.style.top = topPos + 'px';
  imgToolbar.style.left = leftPos + 'px';
}

// ── DRAG-TO-RESIZE HANDLES ──
imgResizer.querySelectorAll('.rz-handle').forEach(function(handle) {
  handle.addEventListener('pointerdown', function(e) {
    if (!selectedImg) return;
    e.preventDefault();
    e.stopPropagation();
    try { handle.setPointerCapture(e.pointerId); } catch(_) {}

    var dir = handle.getAttribute('data-dir');
    var startX = e.clientX, startY = e.clientY;
    var rect = selectedImg.getBoundingClientRect();
    var startW = rect.width, startH = rect.height || 1;
    var aspect = startW / startH;
    var maxW = editor.clientWidth || 720, minW = 56;

    imgSizeBadge.style.display = 'block';

    function onMove(ev) {
      ev.preventDefault();
      var dx = ev.clientX - startX, dy = ev.clientY - startY;
      var newW = startW;
      if (dir === 'e') newW = startW + dx;
      else if (dir === 'w') newW = startW - dx;
      else if (dir === 'se') { var wX = startW + dx, wY = (startH + dy) * aspect; newW = Math.abs(dy) > Math.abs(dx) ? wY : wX; }
      else if (dir === 'sw') { var wX = startW - dx, wY = (startH + dy) * aspect; newW = Math.abs(dy) > Math.abs(dx) ? wY : wX; }
      else if (dir === 'ne') { var wX = startW + dx, wY = (startH - dy) * aspect; newW = Math.abs(dy) > Math.abs(dx) ? wY : wX; }
      else if (dir === 'nw') { var wX = startW - dx, wY = (startH - dy) * aspect; newW = Math.abs(dy) > Math.abs(dx) ? wY : wX; }

      newW = Math.max(minW, Math.min(maxW, Math.round(newW)));
      selectedImg.style.width = newW + 'px';
      selectedImg.style.maxWidth = '100%';
      selectedImg.style.height = 'auto';

      var curRect = selectedImg.getBoundingClientRect();
      var pct = Math.round((newW / maxW) * 100);
      imgSizeBadge.textContent = newW + ' × ' + Math.round(curRect.height) + ' (' + pct + '%)';
      repositionImageOverlay();
    }

    function onUp(ev) {
      try { handle.releasePointerCapture(ev.pointerId); } catch(_) {}
      handle.removeEventListener('pointermove', onMove);
      handle.removeEventListener('pointerup', onUp);
      handle.removeEventListener('pointercancel', onUp);
      imgSizeBadge.style.display = 'none';
      repositionImageOverlay();
      rebuildPageGuides();
      notifyChange();
    }

    handle.addEventListener('pointermove', onMove);
    handle.addEventListener('pointerup', onUp);
    handle.addEventListener('pointercancel', onUp);
  });
});

window.addEventListener('scroll', function() {
  if (selectedImg) repositionImageOverlay();
  if (currentCell) positionTableToolbar(currentCell);
}, { passive: true });

document.addEventListener('keydown', function(e) {
  if ((e.key === 'Backspace' || e.key === 'Delete') && selectedImg) {
    e.preventDefault();
    deleteSelectedImage();
  }
});

document.addEventListener('click', function(e) {
  if (selectedImg && !imgToolbar.contains(e.target) && !imgResizer.contains(e.target) && e.target !== selectedImg) {
    deselectImage();
  }
  if (!tblToolbar.contains(e.target) && !getParentCell(e.target)) {
    hideTableToolbar();
  }
  if (!wcPill.contains(e.target) && !wcPopover.contains(e.target)) {
    wcPopover.classList.remove('visible');
  }
});

// ── TABLES ──
function insertTable(rows, cols) {
  restoreSelection();
  var table = document.createElement('table');
  for (var r = 0; r < rows; r++) {
    var tr = document.createElement('tr');
    for (var c = 0; c < cols; c++) {
      var cell = (r === 0) ? document.createElement('th') : document.createElement('td');
      if (r === 0) cell.textContent = 'Kolom ' + (c + 1);
      else cell.innerHTML = '<br>';
      tr.appendChild(cell);
    }
    table.appendChild(tr);
  }
  var pAfter = document.createElement('p');
  pAfter.innerHTML = '<br>';
  var sel = window.getSelection();
  if (sel && sel.rangeCount > 0 && editor.contains(sel.getRangeAt(0).commonAncestorContainer)) {
    var range = sel.getRangeAt(0);
    range.deleteContents();
    range.insertNode(pAfter);
    pAfter.parentNode.insertBefore(table, pAfter);
  } else {
    editor.appendChild(table);
    editor.appendChild(pAfter);
  }
  rebuildPageGuides();
  notifyChange();
}

function getParentCell(node) {
  while (node && node !== editor && node !== document.body) {
    if (node.tagName === 'TD' || node.tagName === 'TH') return node;
    node = node.parentNode;
  }
  return null;
}
function getParentTable(node) {
  while (node && node !== editor && node !== document.body) {
    if (node.tagName === 'TABLE') return node;
    node = node.parentNode;
  }
  return null;
}
function hideTableToolbar() {
  tblToolbar.classList.remove('visible');
  currentCell = null; currentRow = null; currentTable = null;
}
function positionTableToolbar(cell) {
  var rect = cell.getBoundingClientRect();
  var tbW = tblToolbar.offsetWidth || 300;
  var tbH = tblToolbar.offsetHeight || 40;
  var vh = (window.visualViewport && window.visualViewport.height) || window.innerHeight;
  var topPos = rect.bottom + 6;
  if (topPos + tbH > vh - 8) topPos = Math.max(8, rect.top - tbH - 6);
  var leftPos = Math.max(8, Math.min(rect.left, window.innerWidth - tbW - 8));
  tblToolbar.style.top = topPos + 'px';
  tblToolbar.style.left = leftPos + 'px';
}

editor.addEventListener('click', function(e) {
  if (e.target && e.target.tagName === 'IMG') return;
  var cell = getParentCell(e.target);
  if (cell) {
    deselectImage();
    currentCell = cell;
    currentRow = cell.parentNode;
    currentTable = getParentTable(cell);
    tblToolbar.classList.add('visible');
    positionTableToolbar(cell);
  }
});

function addRowBelow() {
  if (!currentTable || !currentRow) return;
  var newRow = document.createElement('tr');
  var cols = currentTable.rows[0] ? currentTable.rows[0].cells.length : 1;
  for (var i = 0; i < cols; i++) { var td = document.createElement('td'); td.innerHTML = '<br>'; newRow.appendChild(td); }
  currentRow.parentNode.insertBefore(newRow, currentRow.nextSibling);
  notifyChange();
}
function addRowAbove() {
  if (!currentTable || !currentRow) return;
  var newRow = document.createElement('tr');
  var cols = currentTable.rows[0] ? currentTable.rows[0].cells.length : 1;
  for (var i = 0; i < cols; i++) { var td = document.createElement('td'); td.innerHTML = '<br>'; newRow.appendChild(td); }
  currentRow.parentNode.insertBefore(newRow, currentRow);
  notifyChange();
}
function addColRight() {
  if (!currentTable || !currentCell) return;
  var colIdx = currentCell.cellIndex;
  for (var r = 0; r < currentTable.rows.length; r++) {
    var row = currentTable.rows[r];
    var nc = r === 0 ? document.createElement('th') : document.createElement('td');
    nc.innerHTML = r === 0 ? 'Kolom' : '<br>';
    if (colIdx + 1 < row.cells.length) row.insertBefore(nc, row.cells[colIdx + 1]);
    else row.appendChild(nc);
  }
  notifyChange();
}
function addColLeft() {
  if (!currentTable || !currentCell) return;
  var colIdx = currentCell.cellIndex;
  for (var r = 0; r < currentTable.rows.length; r++) {
    var row = currentTable.rows[r];
    var nc = r === 0 ? document.createElement('th') : document.createElement('td');
    nc.innerHTML = r === 0 ? 'Kolom' : '<br>';
    row.insertBefore(nc, row.cells[colIdx]);
  }
  notifyChange();
}
function deleteCurrentRow() {
  if (!currentRow || !currentTable) return;
  if (currentTable.rows.length <= 1) { deleteTable(); return; }
  currentRow.parentNode.removeChild(currentRow);
  hideTableToolbar();
  notifyChange();
}
function deleteCurrentCol() {
  if (!currentTable || !currentCell) return;
  if (currentTable.rows[0] && currentTable.rows[0].cells.length <= 1) { deleteTable(); return; }
  var colIdx = currentCell.cellIndex;
  for (var r = 0; r < currentTable.rows.length; r++) {
    var row = currentTable.rows[r];
    if (colIdx < row.cells.length) row.removeChild(row.cells[colIdx]);
  }
  hideTableToolbar();
  notifyChange();
}
function deleteTable() {
  if (!currentTable) return;
  if (currentTable.parentNode) currentTable.parentNode.removeChild(currentTable);
  hideTableToolbar();
  notifyChange();
}

// ── EXTERNAL UPDATES FROM KOTLIN (NO ECHO LOOP!) ──
function setContentExternal(html) {
  deselectImage();
  hideTableToolbar();
  editor.innerHTML = html || '<p><br></p>';
  attachImageHandlers();
  attachChecklistHandlers();
  hydratePageBreaks();
  rebuildPageGuides();
}
function applyDarkTheme(dark) {
  document.documentElement.classList.toggle('dark', !!dark);
}

editor.addEventListener('keydown', function(e) {
  if (e.key === 'Tab') {
    e.preventDefault();
    document.execCommand('insertHTML', false, '&nbsp;&nbsp;&nbsp;&nbsp;');
    notifyChange();
  }
});

// Init
attachImageHandlers();
attachChecklistHandlers();
hydratePageBreaks();
applyPageSetup(pageConfig);
</script>
</body>
</html>"""
}

// ── MAIN COMPOSABLE ───────────────────────────────────────────────────────────
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DocumentEditorViewport(
    title: String,
    onTitleChange: (String) -> Unit,
    subject: String,
    content: String,
    onContentChange: (String) -> Unit,
    isDarkTheme: Boolean = false,
    onTriggerCheatSheet: () -> Unit = {},
    onTriggerSummary: () -> Unit = {},
    onExportPdf: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val syncState = remember { DocumentSyncState() }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // Extract initial Page Setup + body cleanly
    val initialParsed = remember { DocPageSetup.extractFromContent(content) }
    var pageSetup by remember { mutableStateOf(initialParsed.first) }

    val latestOnContentChange by rememberUpdatedState(onContentChange)
    val latestOnTitleChange by rememberUpdatedState(onTitleChange)
    val latestContent by rememberUpdatedState(content)
    val latestTitle by rememberUpdatedState(title)
    val latestPageSetup by rememberUpdatedState(pageSetup)

    // Toolbar UI state
    var selectedStyle by remember { mutableStateOf(DocParagraphStyle.NORMAL) }
    var selectedFont by remember { mutableStateOf(DocFontFamily.INTER) }
    var fontSizePx by remember { mutableIntStateOf(15) }
    var alignIndex by remember { mutableIntStateOf(0) }
    var lineSpacing by remember { mutableStateOf("1.5") }
    var isBoldActive by remember { mutableStateOf(false) }
    var isItalicActive by remember { mutableStateOf(false) }
    var isUnderlineActive by remember { mutableStateOf(false) }
    var isStrikeActive by remember { mutableStateOf(false) }
    var showWordCount by remember { mutableStateOf(false) }

    var showStyleDropdown by remember { mutableStateOf(false) }
    var showFontDropdown by remember { mutableStateOf(false) }
    var showColorDropdown by remember { mutableStateOf(false) }
    var showHighlightDropdown by remember { mutableStateOf(false) }
    var showSpacingDropdown by remember { mutableStateOf(false) }
    var showToolsDropdown by remember { mutableStateOf(false) }
    var showPageSetupDialog by remember { mutableStateOf(false) }
    var showTableDialog by remember { mutableStateOf(false) }
    var showLinkDialog by remember { mutableStateOf(false) }
    var showMathDialog by remember { mutableStateOf(false) }
    var isReplacingImage by remember { mutableStateOf(false) }

    val alignModes = listOf("left", "center", "right", "justify")
    val alignIcons = listOf(
        Icons.AutoMirrored.Filled.FormatAlignLeft,
        Icons.Filled.FormatAlignCenter,
        Icons.AutoMirrored.Filled.FormatAlignRight,
        Icons.Filled.FormatAlignJustify
    )
    val alignLabels = listOf("Rata Kiri", "Rata Tengah", "Rata Kanan", "Rata Penuh")

    fun js(script: String) {
        webViewRef?.evaluateJavascript(script, null)
    }

    fun applyAndPersistPageSetup(newSetup: DocPageSetup) {
        pageSetup = newSetup
        js("applyPageSetup(${newSetup.toJsConfigJson()});")
        val (_, cleanBody) = DocPageSetup.extractFromContent(latestContent)
        val packed = DocPageSetup.packWithContent(newSetup, cleanBody)
        syncState.lastEmittedPacked = packed
        latestOnContentChange(packed)
    }

    // ── IMAGE PICKER ──
    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        val replaceMode = isReplacingImage
        isReplacingImage = false
        if (uri != null) {
            scope.launch {
                val dataUri = withContext(Dispatchers.IO) {
                    try { encodeUriToDataUri(context, uri) } catch (e: Exception) {
                        Log.e("DocEditor", "Image encode error", e); null
                    }
                }
                if (dataUri != null) {
                    val quoted = JSONObject.quote(dataUri)
                    if (replaceMode) {
                        js("replaceSelectedImage($quoted);")
                        Toast.makeText(context, "Gambar diganti", Toast.LENGTH_SHORT).show()
                    } else {
                        js("insertImage($quoted);")
                        Toast.makeText(context, "Gambar disisipkan — tarik sudut untuk ubah ukuran", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Gagal memuat gambar", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ── IMPORT EXTERNAL FILE (.docx / .html / .md / .txt / .rtf) ──
    val importDocLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    DocxDocumentCodec.importFromUri(context, uri)
                }
                result.onSuccess { imported ->
                    val (importedSetup, importedBody) = DocPageSetup.extractFromContent(imported.html)
                    pageSetup = importedSetup
                    val packed = DocPageSetup.packWithContent(importedSetup, importedBody)
                    syncState.lastEmittedPacked = packed
                    js("applyPageSetup(${importedSetup.toJsConfigJson()}); setContentExternal(${JSONObject.quote(importedBody)});")
                    latestOnContentChange(packed)
                    if (latestTitle.isBlank() || latestTitle.startsWith("Dokumen Teks Baru")) {
                        latestOnTitleChange(imported.title)
                    }
                    Toast.makeText(context, "Berhasil membuka ${imported.title} (${imported.formatLabel})", Toast.LENGTH_SHORT).show()
                }.onFailure { err ->
                    Toast.makeText(context, "Gagal membuka file: ${err.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // ── EXPORT TO .DOCX ──
    val exportDocxLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(DocxDocumentCodec.MIME_DOCX)
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val res = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            DocxDocumentCodec.exportToDocx(latestContent, out)
                        } ?: error("Tidak dapat menulis file")
                    }
                }
                res.onSuccess { Toast.makeText(context, "Berhasil diekspor ke .docx ✓", Toast.LENGTH_SHORT).show() }
                    .onFailure { err -> Toast.makeText(context, "Gagal ekspor .docx: ${err.message}", Toast.LENGTH_LONG).show() }
            }
        }
    }

    // ── EXPORT TO .HTML ──
    val exportHtmlLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(DocxDocumentCodec.MIME_HTML)
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val res = withContext(Dispatchers.IO) {
                    runCatching {
                        val fullHtml = DocxDocumentCodec.exportToHtmlFile(latestTitle, latestContent)
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(fullHtml.toByteArray(Charsets.UTF_8))
                        } ?: error("Tidak dapat menulis file")
                    }
                }
                res.onSuccess { Toast.makeText(context, "Berhasil diekspor ke .html ✓", Toast.LENGTH_SHORT).show() }
                    .onFailure { err -> Toast.makeText(context, "Gagal ekspor .html: ${err.message}", Toast.LENGTH_LONG).show() }
            }
        }
    }

    // ── EXPORT TO .TXT ──
    val exportTxtLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(DocxDocumentCodec.MIME_TXT)
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val res = withContext(Dispatchers.IO) {
                    runCatching {
                        val plain = DocxDocumentCodec.exportToPlainText(latestContent)
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(plain.toByteArray(Charsets.UTF_8))
                        } ?: error("Tidak dapat menulis file")
                    }
                }
                res.onSuccess { Toast.makeText(context, "Berhasil diekspor ke .txt ✓", Toast.LENGTH_SHORT).show() }
                    .onFailure { err -> Toast.makeText(context, "Gagal ekspor .txt: ${err.message}", Toast.LENGTH_LONG).show() }
            }
        }
    }

    // ── SHARE AS .DOCX ──
    fun shareAsDocx() {
        scope.launch {
            val res = withContext(Dispatchers.IO) {
                runCatching {
                    val dir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
                    val file = File(dir, "${sanitizeFileName(latestTitle)}.docx")
                    FileOutputStream(file).use { out -> DocxDocumentCodec.exportToDocx(latestContent, out) }
                    file
                }
            }
            res.onSuccess { file ->
                try {
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = DocxDocumentCodec.MIME_DOCX
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_SUBJECT, latestTitle)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Bagikan Dokumen (.docx)"))
                } catch (e: Exception) {
                    Toast.makeText(context, "Gagal membagikan file: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }.onFailure { err ->
                Toast.makeText(context, "Gagal membuat .docx: ${err.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val pageBadgeText = remember(pageSetup) {
        if (pageSetup.isPageless) "Pageless"
        else {
            val sz = pageSetup.paperSize.label.substringBefore(" /")
            if (pageSetup.orientation == DocPageOrientation.LANDSCAPE) "$sz · L" else sz
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDarkTheme) Color(0xFF13141C) else Color(0xFFEEF2F6))
            .imePadding()
    ) {
        // ── TOOLBAR RIBBON ─────────────────────────────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = if (isDarkTheme) Color(0xFF1E202B) else Color(0xFFEDF2FA),
            border = BorderStroke(1.dp, LernspaceBorder),
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Undo / Redo
                DocToolbarIcon(Icons.AutoMirrored.Filled.Undo, "Undo") { js("undo();") }
                DocToolbarIcon(Icons.AutoMirrored.Filled.Redo, "Redo") { js("redo();") }
                DocToolbarDiv()

                // Paragraph Style
                Box {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Transparent,
                        modifier = Modifier
                            .height(32.dp)
                            .clickable { showStyleDropdown = true }
                            .padding(horizontal = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(selectedStyle.label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = LernspaceTextPrimary)
                            Icon(Icons.Filled.ArrowDropDown, null, Modifier.size(15.dp), tint = LernspaceTextSecondary)
                        }
                    }
                    DropdownMenu(expanded = showStyleDropdown, onDismissRequest = { showStyleDropdown = false }) {
                        DocParagraphStyle.entries.forEach { style ->
                            DropdownMenuItem(
                                text = { Text(style.label, fontWeight = if (style.isBold()) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp) },
                                onClick = {
                                    selectedStyle = style
                                    showStyleDropdown = false
                                    js("formatBlock(${JSONObject.quote(style.htmlTag)});")
                                }
                            )
                        }
                    }
                }
                DocToolbarDiv()

                // Font Family (10 distinct fonts)
                Box {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Transparent,
                        modifier = Modifier
                            .height(32.dp)
                            .clickable { showFontDropdown = true }
                            .padding(horizontal = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(selectedFont.label.substringBefore(" ("), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = LernspaceTextPrimary)
                            Icon(Icons.Filled.ArrowDropDown, null, Modifier.size(15.dp), tint = LernspaceTextSecondary)
                        }
                    }
                    DropdownMenu(expanded = showFontDropdown, onDismissRequest = { showFontDropdown = false }) {
                        DocFontFamily.entries.forEach { font ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        font.label,
                                        fontSize = 13.sp,
                                        fontWeight = if (font == selectedFont) FontWeight.Bold else FontWeight.Normal,
                                        color = if (font == selectedFont) AccentBlue else LernspaceTextPrimary
                                    )
                                },
                                onClick = {
                                    selectedFont = font
                                    showFontDropdown = false
                                    js("setFontFamily(${JSONObject.quote(font.css)});")
                                }
                            )
                        }
                    }
                }
                DocToolbarDiv()

                // Font Size Stepper
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                    DocToolbarIcon(Icons.Filled.Remove, "Kecilkan") {
                        if (fontSizePx > 10) { fontSizePx--; js("setFontSize($fontSizePx);") }
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isDarkTheme) Color(0xFF2A2D3A) else Color.White,
                        border = BorderStroke(1.dp, LernspaceBorder),
                        modifier = Modifier.size(width = 32.dp, height = 26.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("$fontSizePx", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LernspaceTextPrimary)
                        }
                    }
                    DocToolbarIcon(Icons.Filled.Add, "Besarkan") {
                        if (fontSizePx < 72) { fontSizePx++; js("setFontSize($fontSizePx);") }
                    }
                }
                DocToolbarDiv()

                // Inline Formatting
                DocToolbarTextBtn("B", FontWeight.Bold, isActive = isBoldActive, tooltip = "Tebal") { js("toggleBold();") }
                DocToolbarTextBtn("I", fontStyle = FontStyle.Italic, isActive = isItalicActive, tooltip = "Miring") { js("toggleItalic();") }
                DocToolbarTextBtn("U", isActive = isUnderlineActive, tooltip = "Garis Bawah") { js("toggleUnderline();") }
                DocToolbarTextBtn("S", isActive = isStrikeActive, tooltip = "Coret") { js("toggleStrikethrough();") }

                // Text Color
                Box {
                    IconButton(onClick = { showColorDropdown = true }, modifier = Modifier.size(30.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text("A", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = LernspaceTextPrimary)
                            Box(Modifier.size(width = 14.dp, height = 3.dp).background(Color(0xFF2563EB)))
                        }
                    }
                    DropdownMenu(expanded = showColorDropdown, onDismissRequest = { showColorDropdown = false }) {
                        listOf(
                            "Hitam" to "#1E293B", "Abu-abu" to "#64748B", "Biru" to "#2563EB",
                            "Merah" to "#DC2626", "Hijau" to "#059669", "Ungu" to "#7C3AED",
                            "Oranye" to "#EA580C", "Merah Muda" to "#EC4899"
                        ).forEach { (name, hex) ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(Modifier.size(14.dp).clip(CircleShape).background(Color(android.graphics.Color.parseColor(hex))))
                                        Text(name, fontSize = 12.sp)
                                    }
                                },
                                onClick = { js("setTextColor(${JSONObject.quote(hex)});"); showColorDropdown = false }
                            )
                        }
                    }
                }

                // Highlight Color
                Box {
                    DocToolbarIcon(Icons.Filled.BorderColor, "Sorotan") { showHighlightDropdown = true }
                    DropdownMenu(expanded = showHighlightDropdown, onDismissRequest = { showHighlightDropdown = false }) {
                        listOf(
                            "Kuning" to "#FEF08A", "Hijau" to "#BBF7D0", "Biru" to "#BFDBFE",
                            "Pink" to "#FBCFE8", "Oranye" to "#FED7AA", "Hapus Sorotan" to ""
                        ).forEach { (name, hex) ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (hex.isNotEmpty()) {
                                            Box(Modifier.size(14.dp).clip(RoundedCornerShape(2.dp)).background(Color(android.graphics.Color.parseColor(hex))))
                                        }
                                        Text(name, fontSize = 12.sp)
                                    }
                                },
                                onClick = {
                                    if (hex.isNotEmpty()) js("setHighlight(${JSONObject.quote(hex)});")
                                    else js("setHighlight('');")
                                    showHighlightDropdown = false
                                }
                            )
                        }
                    }
                }
                DocToolbarDiv()

                // Alignment Cycling + Line Spacing + Indent
                Surface(
                    shape = RoundedCornerShape(5.dp),
                    color = Color.Transparent,
                    modifier = Modifier.size(30.dp).clickable {
                        alignIndex = (alignIndex + 1) % 4
                        js("alignText(${JSONObject.quote(alignModes[alignIndex])});")
                    }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(alignIcons[alignIndex], alignLabels[alignIndex], Modifier.size(17.dp), tint = LernspaceTextPrimary)
                    }
                }

                // Line Spacing & First-line Indent Dropdown
                Box {
                    DocToolbarIcon(Icons.Filled.FormatLineSpacing, "Spasi Baris") { showSpacingDropdown = true }
                    DropdownMenu(expanded = showSpacingDropdown, onDismissRequest = { showSpacingDropdown = false }) {
                        listOf(
                            "1.0" to "1.0 · Rapat (Single)",
                            "1.15" to "1.15 · Standar GDocs",
                            "1.5" to "1.5 · Standar Makalah",
                            "2.0" to "2.0 · Spasi Ganda (Skripsi)"
                        ).forEach { (mult, label) ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        label,
                                        fontSize = 12.5.sp,
                                        fontWeight = if (lineSpacing == mult) FontWeight.Bold else FontWeight.Normal,
                                        color = if (lineSpacing == mult) AccentBlue else LernspaceTextPrimary
                                    )
                                },
                                onClick = {
                                    lineSpacing = mult
                                    js("setLineSpacing($mult);")
                                    showSpacingDropdown = false
                                }
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Toggle Indentasi Baris Pertama (1.27 cm)", fontSize = 12.5.sp) },
                            onClick = {
                                js("toggleFirstLineIndent();")
                                showSpacingDropdown = false
                            }
                        )
                    }
                }

                DocToolbarIcon(Icons.AutoMirrored.Filled.FormatIndentDecrease, "Kurangi Indent") { js("adjustIndent(-36);") }
                DocToolbarIcon(Icons.AutoMirrored.Filled.FormatIndentIncrease, "Tambah Indent") { js("adjustIndent(36);") }
                DocToolbarDiv()

                // Lists
                DocToolbarIcon(Icons.AutoMirrored.Filled.FormatListBulleted, "Bullet List") { js("insertBulletList();") }
                DocToolbarIcon(Icons.Filled.FormatListNumbered, "Numbered List") { js("insertNumberedList();") }
                DocToolbarIcon(Icons.Filled.Checklist, "Checklist") { js("insertChecklist();") }
                DocToolbarIcon(Icons.Filled.FormatQuote, "Kutipan") { js("insertBlockquote();") }
                DocToolbarDiv()

                // Insert Tools
                DocToolbarIcon(Icons.Filled.Link, "Tautan") { showLinkDialog = true }
                DocToolbarIcon(Icons.Filled.Functions, "Rumus") { showMathDialog = true }
                DocToolbarIcon(Icons.Filled.Image, "Sisipkan Gambar") {
                    isReplacingImage = false
                    imagePickerLauncher.launch("image/*")
                }
                DocToolbarIcon(Icons.Filled.TableChart, "Tabel") { showTableDialog = true }
                DocToolbarIcon(Icons.Filled.InsertPageBreak, "Pemisah Halaman") { js("insertPageBreak();") }
                DocToolbarIcon(Icons.Filled.HorizontalRule, "Garis") { js("insertHR();") }
                DocToolbarIcon(Icons.Filled.FormatClear, "Hapus Format") { js("clearFormatting();") }
                DocToolbarDiv()

                // Quick Paper Format Chip (1-Tap Page Setup!)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isDarkTheme) Color(0xFF2A2D3A) else Color.White,
                    border = BorderStroke(1.dp, LernspaceBorder),
                    modifier = Modifier
                        .height(28.dp)
                        .clickable { showPageSetupDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Filled.Description, null, Modifier.size(13.dp), tint = AccentBlue)
                        Text(pageBadgeText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = LernspaceTextPrimary)
                    }
                }

                // Tools & File Compatibility Menu
                Box {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (showWordCount) AccentBlue.copy(alpha = 0.10f) else Color.Transparent,
                        modifier = Modifier
                            .height(30.dp)
                            .clickable { showToolsDropdown = true }
                            .padding(horizontal = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Icon(Icons.Filled.Tune, "Alat & File", Modifier.size(15.dp), tint = if (showWordCount) AccentBlue else LernspaceTextPrimary)
                            Text("Alat", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = if (showWordCount) AccentBlue else LernspaceTextPrimary)
                            Icon(Icons.Filled.ArrowDropDown, null, Modifier.size(15.dp), tint = LernspaceTextSecondary)
                        }
                    }
                    DropdownMenu(expanded = showToolsDropdown, onDismissRequest = { showToolsDropdown = false }) {
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Filled.Description, null, Modifier.size(18.dp), tint = AccentBlue) },
                            text = { Text("Penyiapan Halaman (A4, F4, Margin)", fontSize = 13.sp) },
                            onClick = { showToolsDropdown = false; showPageSetupDialog = true }
                        )
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Filled.InsertPageBreak, null, Modifier.size(18.dp), tint = LernspaceTextSecondary) },
                            text = { Text("Sisipkan Pemisah Halaman", fontSize = 13.sp) },
                            onClick = { showToolsDropdown = false; js("insertPageBreak();") }
                        )
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    if (showWordCount) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                                    null,
                                    tint = if (showWordCount) AccentBlue else LernspaceTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            text = { Text("Tampilkan Jumlah Kata", fontSize = 13.sp) },
                            onClick = {
                                showWordCount = !showWordCount
                                js("setWordCountVisible($showWordCount);")
                                showToolsDropdown = false
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Filled.FolderOpen, null, Modifier.size(18.dp), tint = AccentBlue) },
                            text = { Text("Buka / Impor (.docx, .md, .txt, .html)", fontSize = 13.sp) },
                            onClick = { showToolsDropdown = false; importDocLauncher.launch(DocxDocumentCodec.SUPPORTED_IMPORT_MIMES) }
                        )
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Filled.SaveAs, null, Modifier.size(18.dp), tint = Color(0xFF2563EB)) },
                            text = { Text("Simpan sebagai Word / GDocs (.docx)", fontSize = 13.sp) },
                            onClick = { showToolsDropdown = false; exportDocxLauncher.launch("${sanitizeFileName(latestTitle)}.docx") }
                        )
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Filled.Share, null, Modifier.size(18.dp), tint = Color(0xFF059669)) },
                            text = { Text("Bagikan sebagai .docx", fontSize = 13.sp) },
                            onClick = { showToolsDropdown = false; shareAsDocx() }
                        )
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Filled.Code, null, Modifier.size(18.dp), tint = LernspaceTextSecondary) },
                            text = { Text("Ekspor ke HTML (.html)", fontSize = 13.sp) },
                            onClick = { showToolsDropdown = false; exportHtmlLauncher.launch("${sanitizeFileName(latestTitle)}.html") }
                        )
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, null, Modifier.size(18.dp), tint = LernspaceTextSecondary) },
                            text = { Text("Ekspor ke Teks (.txt)", fontSize = 13.sp) },
                            onClick = { showToolsDropdown = false; exportTxtLauncher.launch("${sanitizeFileName(latestTitle)}.txt") }
                        )
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Filled.Print, null, Modifier.size(18.dp), tint = Color(0xFFDC2626)) },
                            text = { Text("Cetak / Simpan PDF", fontSize = 13.sp) },
                            onClick = {
                                showToolsDropdown = false
                                printWebViewDocumentToPdf(context, webViewRef, latestTitle, latestPageSetup)
                            }
                        )
                    }
                }
                DocToolbarDiv()

                // Mimo AI Quick Actions
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF8B5CF6).copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.35f)),
                    modifier = Modifier.height(30.dp).clickable(onClick = onTriggerCheatSheet)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("⚡", fontSize = 11.sp)
                        Text("Cheat Sheet", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8B5CF6))
                    }
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = AccentBlue.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.3f)),
                    modifier = Modifier.height(30.dp).clickable(onClick = onTriggerSummary)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("📝", fontSize = 11.sp)
                        Text("Rangkum", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentBlue)
                    }
                }
            }
        }

        // ── WEBVIEW EDITOR (CLEAN HEADERLESS PAPER + PAGE SETUP) ───────────
        AndroidView(
            factory = { ctx ->
                val bridge = DocumentJsBridge(
                    mainHandler = mainHandler,
                    onContentChanged = { bodyHtml ->
                        val packed = DocPageSetup.packWithContent(latestPageSetup, bodyHtml)
                        syncState.lastEmittedPacked = packed
                        latestOnContentChange(packed)
                    },
                    onFormatStateChanged = { b, i, u, s, align ->
                        isBoldActive = b
                        isItalicActive = i
                        isUnderlineActive = u
                        isStrikeActive = s
                        alignIndex = align.coerceIn(0, 3)
                    },
                    onRequestReplaceImage = {
                        isReplacingImage = true
                        imagePickerLauncher.launch("image/*")
                    }
                )

                WebView(ctx).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        allowFileAccess = true
                        allowContentAccess = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                        setSupportZoom(false)
                        builtInZoomControls = false
                        displayZoomControls = false
                    }
                    addJavascriptInterface(bridge, "LernspaceDoc")
                    webChromeClient = WebChromeClient()
                    setBackgroundColor(0x00000000)
                    isHorizontalScrollBarEnabled = false
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            syncState.isReady = true
                            view?.evaluateJavascript("setWordCountVisible($showWordCount);", null)
                        }
                    }

                    syncState.lastEmittedPacked = content
                    syncState.lastAppliedDark = isDarkTheme

                    val (initSetup, initBody) = DocPageSetup.extractFromContent(content)
                    val html = generateEditorHtml(isDarkTheme, initBody, initSetup)
                    loadDataWithBaseURL("https://learnspace.local/", html, "text/html", "UTF-8", null)
                    webViewRef = this
                }
            },
            update = { wv ->
                if (!syncState.isReady) return@AndroidView

                if (content != syncState.lastEmittedPacked) {
                    syncState.lastEmittedPacked = content
                    val (extSetup, extBody) = DocPageSetup.extractFromContent(content)
                    if (extSetup != pageSetup) {
                        pageSetup = extSetup
                        wv.evaluateJavascript("applyPageSetup(${extSetup.toJsConfigJson()});", null)
                    }
                    val normalized = normalizeInitialBody(extBody)
                    wv.evaluateJavascript("setContentExternal(${JSONObject.quote(normalized)});", null)
                }

                if (isDarkTheme != syncState.lastAppliedDark) {
                    syncState.lastAppliedDark = isDarkTheme
                    wv.evaluateJavascript("applyDarkTheme($isDarkTheme);", null)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
    }

    // ── PAGE SETUP DIALOG ─────────────────────────────────────────────────
    if (showPageSetupDialog) {
        PageSetupDialog(
            initialSetup = pageSetup,
            onDismiss = { showPageSetupDialog = false },
            onApply = { updated ->
                applyAndPersistPageSetup(updated)
                showPageSetupDialog = false
            }
        )
    }

    // ── TABLE DIALOG ──────────────────────────────────────────────────────
    if (showTableDialog) {
        TableInsertDialog(onDismiss = { showTableDialog = false }) { rows, cols ->
            js("insertTable($rows, $cols);")
            showTableDialog = false
        }
    }

    // ── LINK DIALOG ───────────────────────────────────────────────────────
    if (showLinkDialog) {
        InsertLinkDialog(initialText = "", onDismiss = { showLinkDialog = false }) { text, url ->
            js("insertLink(${JSONObject.quote(text)}, ${JSONObject.quote(url)});")
            showLinkDialog = false
        }
    }

    // ── MATH DIALOG ───────────────────────────────────────────────────────
    if (showMathDialog) {
        VisualMathEquationDialog(initialEquation = "", onDismiss = { showMathDialog = false }) { latex ->
            js("insertMath(${JSONObject.quote(latex)});")
            showMathDialog = false
        }
    }
}

// ── TABLE INSERT DIALOG ───────────────────────────────────────────────────────
@Composable
fun TableInsertDialog(onDismiss: () -> Unit, onInsert: (Int, Int) -> Unit) {
    var rows by remember { mutableIntStateOf(3) }
    var cols by remember { mutableIntStateOf(3) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = LernspaceSurfaceCard,
            border = BorderStroke(1.dp, LernspaceBorder),
            shadowElevation = 8.dp,
            modifier = Modifier.widthIn(min = 300.dp, max = 380.dp).wrapContentHeight()
        ) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.TableChart, null, tint = AccentBlue)
                    Text("Sisipkan Tabel", style = MaterialTheme.typography.titleMedium, color = LernspaceDark)
                }
                val pr = rows.coerceAtMost(5)
                val pc = cols.coerceAtMost(6)
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    repeat(pr) { r ->
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            repeat(pc) {
                                Box(
                                    Modifier
                                        .height(18.dp)
                                        .weight(1f)
                                        .background(
                                            if (r == 0) AccentBlue.copy(0.2f) else Color(0xFFE2E8F0),
                                            RoundedCornerShape(2.dp)
                                        )
                                )
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text("Baris", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = LernspaceDark)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconButton(onClick = { if (rows > 1) rows-- }, Modifier.size(32.dp)) {
                            Icon(Icons.Filled.Remove, null, Modifier.size(16.dp))
                        }
                        Text("$rows", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = LernspaceDark)
                        IconButton(onClick = { if (rows < 25) rows++ }, Modifier.size(32.dp)) {
                            Icon(Icons.Filled.Add, null, Modifier.size(16.dp))
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text("Kolom", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = LernspaceDark)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconButton(onClick = { if (cols > 1) cols-- }, Modifier.size(32.dp)) {
                            Icon(Icons.Filled.Remove, null, Modifier.size(16.dp))
                        }
                        Text("$cols", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = LernspaceDark)
                        IconButton(onClick = { if (cols < 10) cols++ }, Modifier.size(32.dp)) {
                            Icon(Icons.Filled.Add, null, Modifier.size(16.dp))
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), Arrangement.End, Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) { Text("Batal", color = LernspaceTextSecondary) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onInsert(rows, cols) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                    ) {
                        Text("Sisipkan ${rows}×${cols}", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ── TOOLBAR HELPERS ───────────────────────────────────────────────────────────
private fun DocParagraphStyle.isBold() = this == DocParagraphStyle.TITLE || this == DocParagraphStyle.HEADING_1

@Composable
private fun DocToolbarIcon(icon: ImageVector, tooltip: String, enabled: Boolean = true, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(30.dp)) {
        Icon(
            icon,
            tooltip,
            Modifier.size(17.dp),
            tint = if (enabled) LernspaceTextPrimary else LernspaceTextSecondary.copy(alpha = 0.4f)
        )
    }
}

@Composable
private fun DocToolbarTextBtn(
    label: String,
    fontWeight: FontWeight = FontWeight.Normal,
    fontStyle: FontStyle = FontStyle.Normal,
    isActive: Boolean = false,
    tooltip: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(5.dp),
        color = if (isActive) AccentBlue.copy(alpha = 0.14f) else Color.Transparent,
        border = if (isActive) BorderStroke(1.dp, AccentBlue.copy(alpha = 0.35f)) else null,
        modifier = Modifier.size(30.dp).clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label,
                fontSize = 13.sp,
                fontWeight = fontWeight,
                fontStyle = fontStyle,
                color = if (isActive) AccentBlue else LernspaceTextPrimary
            )
        }
    }
}

@Composable
private fun DocToolbarDiv() {
    Box(Modifier.height(18.dp).width(1.dp).background(LernspaceBorder.copy(alpha = 0.6f)))
}
