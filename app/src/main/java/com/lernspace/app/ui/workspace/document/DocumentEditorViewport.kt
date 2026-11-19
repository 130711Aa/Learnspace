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
    private val onFormatStateChanged: (
        bold: Boolean,
        italic: Boolean,
        underline: Boolean,
        strike: Boolean,
        alignIdx: Int,
        fontSizePx: Int,
        fontFamilyName: String,
        blockTag: String,
        textColorHex: String,
        lineSpacingStr: String,
        indentPx: Int,
        indentMode: String,
        spaceBeforePt: Int,
        spaceAfterPt: Int
    ) -> Unit,
    private val onRequestReplaceImage: () -> Unit
) {
    @JavascriptInterface
    fun onContentChange(html: String) {
        mainHandler.post { onContentChanged(html) }
    }

    @JavascriptInterface
    fun onFormatState(
        bold: Boolean,
        italic: Boolean,
        underline: Boolean,
        strike: Boolean,
        alignIdx: Int,
        fontSizePx: Int,
        fontFamilyName: String,
        blockTag: String,
        textColorHex: String,
        lineSpacingStr: String,
        indentPx: Int,
        indentMode: String,
        spaceBeforePt: Int,
        spaceAfterPt: Int
    ) {
        mainHandler.post {
            onFormatStateChanged(
                bold, italic, underline, strike, alignIdx,
                fontSizePx, fontFamilyName, blockTag, textColorHex,
                lineSpacingStr, indentPx, indentMode, spaceBeforePt, spaceAfterPt
            )
        }
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
    font-size: 14.67px;
    line-height: 1.15;
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

  #editor p, #editor div { margin: 0; min-height: 1em; }
  #editor h1 { font-size: 26.67px; font-weight: 700; margin: 16px 0 10px 0; clear: both; }
  #editor h2 { font-size: 21.33px; font-weight: 700; margin: 14px 0 8px 0; clear: both; }
  #editor h3 { font-size: 18.67px; font-weight: 600; margin: 12px 0 6px 0; clear: both; }
  #editor h4 { font-size: 16px; font-weight: 600; margin: 10px 0 4px 0; clear: both; }
  #editor h5 { font-size: 14.67px; font-weight: 600; margin: 8px 0 4px 0; clear: both; }
  #editor > h1:first-child:not([data-space-before]),
  #editor > h2:first-child:not([data-space-before]),
  #editor > h3:first-child:not([data-space-before]) { margin-top: 0; }
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

  /* ── ZERO-DOM-NODE PAGE PUSH (SMOOTH MARGIN-TOP JUMP, NO SPACER DIVS) ── */
  #editor [data-page-push] { clear: both; }
  #editor [data-page-parked] {
    height: 1px !important;
    min-height: 1px !important;
    line-height: 1px !important;
    margin: 0 !important;
    padding: 0 !important;
    overflow: hidden !important;
    opacity: 0 !important;
    pointer-events: none !important;
  }
  body.pageless-mode #editor [data-page-push] { margin-top: 0 !important; }
  body.pageless-mode #editor [data-page-parked] {
    height: auto !important; min-height: 1.15em !important; line-height: inherit !important;
    margin: 0 !important; opacity: 1 !important; pointer-events: auto !important;
  }
  #editor .doc-page-spacer { display: none !important; height: 0 !important; }

  /* ── LISTS & CHECKLIST ── */
  #editor ul { list-style-type: disc; margin: 4px 0 6px 0; padding-left: 28px; }
  #editor ol { list-style-type: decimal; margin: 4px 0 6px 0; padding-left: 28px; }
  #editor li { margin: 2px 0; line-height: 1.3; }
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
    min-height: 1.5em;
  }
  #editor blockquote:empty::before,
  #editor blockquote:has(br:only-child)::before {
    content: 'Tulis kutipan di sini...';
    color: var(--muted);
    font-style: italic;
    pointer-events: none;
    opacity: 0.6;
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
    #wc-pill, #wc-popover, #img-resizer, #img-toolbar, #tbl-toolbar, #page-sheets-bg, #editor .doc-page-spacer { display: none !important; }
    #editor [data-page-push] { margin-top: 0 !important; }
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

function isBlockEmpty(el) {
  if (!el) return true;
  if (el.classList.contains('doc-page-break')) return false;
  if (el.querySelector('img, table, hr, iframe')) return false;
  return !(el.textContent || '').trim();
}

function getLastContentIndex(children) {
  for (var i = children.length - 1; i >= 0; i--) {
    var el = children[i];
    if (el.nodeType !== 1) continue;
    if (el.classList.contains('doc-page-spacer')) continue;
    if (el.classList.contains('doc-page-break')) return i;
    if (!isBlockEmpty(el)) return i;
  }
  return -1;
}

function getScaledPageMetrics() {
  var cfg = pageConfig;
  return {
    pageH: cfg.heightPx,
    pageW: cfg.widthPx,
    padT: cfg.mTopPx,
    padR: cfg.mRightPx,
    padB: cfg.mBottomPx,
    padL: cfg.mLeftPx
  };
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

  var shell = document.getElementById('paper-shell');
  if (!cfg.pageless) {
    var m = getScaledPageMetrics();
    if (shell) shell.style.width = cfg.widthPx + 'px';
    paper.style.width = cfg.widthPx + 'px';
    paper.style.padding = m.padT + 'px ' + m.padR + 'px ' + m.padB + 'px ' + m.padL + 'px';
    editor.style.minHeight = Math.max(200, m.pageH - m.padT - m.padB) + 'px';
  } else {
    if (shell) shell.style.width = '';
    paper.style.minHeight = '';
    editor.style.minHeight = '';
  }

  requestAnimationFrame(paginateDocument);
}

function splitBlockAtPageBoundary(block, maxBottom) {
  if (block.tagName !== 'P' && block.tagName !== 'DIV') return false;
  if (block.querySelector('table, pre, ul, ol, img, iframe, .doc-page-break')) return false;

  var edRect = editor.getBoundingClientRect();
  var bRect = block.getBoundingClientRect();
  if (bRect.height < 36) return false;

  var walker = document.createTreeWalker(block, NodeFilter.SHOW_TEXT, null, false);
  var textNodes = [];
  var node;
  while ((node = walker.nextNode())) { textNodes.push(node); }
  if (textNodes.length === 0) return false;

  var range = document.createRange();
  var splitNode = null;
  var splitOffset = -1;

  for (var n = 0; n < textNodes.length; n++) {
    var tn = textNodes[n];
    var val = tn.nodeValue || '';
    for (var i = 0; i < val.length; i++) {
      range.setStart(tn, i);
      range.setEnd(tn, Math.min(i + 1, val.length));
      var r = range.getBoundingClientRect();
      if ((r.bottom - edRect.top) > maxBottom) {
        var cut = i;
        while (cut > 0 && !/\s/.test(val[cut])) cut--;
        if (cut === 0) cut = i;
        splitNode = tn;
        splitOffset = cut;
        break;
      }
    }
    if (splitNode) break;
  }

  if (!splitNode || splitOffset <= 0) return false;

  var secondText = splitNode.splitText(splitOffset);
  var newP = document.createElement('p');
  newP.style.cssText = block.style.cssText;
  newP.style.marginTop = '';
  delete newP.dataset.pagePush;

  var curr = secondText;
  while (curr) {
    var next = curr.nextSibling;
    newP.appendChild(curr);
    curr = next;
  }

  block.parentNode.insertBefore(newP, block.nextSibling);

  var sel = window.getSelection();
  if (sel && sel.rangeCount > 0) {
    var sr = sel.getRangeAt(0);
    if (sr.startContainer === splitNode && sr.startOffset >= splitOffset) {
      var newRange = document.createRange();
      newRange.setStart(secondText, Math.max(0, sr.startOffset - splitOffset));
      newRange.collapse(true);
      sel.removeAllRanges();
      sel.addRange(newRange);
    }
  }

  return true;
}

var isPaginating = false;
var lastPaginatedEditorH = -1;

function getNaturalMarginTop(el) {
  if (el.dataset && el.dataset.spaceBefore) {
    return parseFloat(el.dataset.spaceBefore) || 0;
  }
  if (el.classList.contains('doc-page-break')) return 22;
  switch (el.tagName) {
    case 'H1': return 16;
    case 'H2': return 14;
    case 'H3': return 12;
    case 'H4': return 10;
    case 'H5': return 8;
    case 'HR': return 16;
    case 'UL': case 'OL': return 6;
    case 'BLOCKQUOTE': case 'PRE': return 10;
    case 'TABLE': return 12;
    default: return 0;
  }
}

function createSheetCard(pageNum, m) {
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
  if (pageConfig.showPageNumbers) ftrParts.push('Halaman ' + pageNum);
  if (ftrParts.length > 0) {
    var ftr = document.createElement('div');
    ftr.className = 'sheet-ftr';
    ftr.style.left = m.padL + 'px';
    ftr.style.right = m.padR + 'px';
    ftr.textContent = ftrParts.join('  ·  ');
    card.appendChild(ftr);
  }
  return card;
}

var lastCaretBlock = null;

function getCaretBlock() {
  if (document.activeElement !== editor) return null;
  var sel = window.getSelection();
  if (!sel || sel.rangeCount === 0) return null;
  var node = sel.getRangeAt(0).startContainer;
  if (!editor.contains(node)) return null;
  if (node === editor) {
    var off = sel.getRangeAt(0).startOffset;
    return editor.children[Math.min(off, editor.children.length - 1)] || null;
  }
  return findClosestBlock(node);
}

function hasRealContent(el) {
  if (!el) return false;
  if (el.classList.contains('doc-page-break')) return true;
  if (el.querySelector('img, table, hr, iframe')) return true;
  return (el.textContent || '').replace(/\u200B/g, '').trim().length > 0;
}

function clearAllPagePushes() {
  var pushed = editor.querySelectorAll('[data-page-push]');
  for (var i = 0; i < pushed.length; i++) {
    pushed[i].style.marginTop = pushed[i].dataset.spaceBefore ? (pushed[i].dataset.spaceBefore + 'px') : '';
    delete pushed[i].dataset.pagePush;
  }
  var parked = editor.querySelectorAll('[data-page-parked]');
  for (var p = 0; p < parked.length; p++) {
    delete parked[p].dataset.pageParked;
  }
  var oldSpacers = editor.querySelectorAll('.doc-page-spacer');
  for (var s = 0; s < oldSpacers.length; s++) {
    oldSpacers[s].parentNode.removeChild(oldSpacers[s]);
  }
}

function paginateDocument() {
  if (isPaginating) return;
  if (!pageConfig || pageConfig.pageless) {
    clearAllPagePushes();
    pageSheetsBg.innerHTML = '';
    estimatedPageCount = 1;
    lastSheetsRenderKey = 'pageless';
    lastPaginatedEditorH = editor.offsetHeight;
    updateWordCount();
    return;
  }

  isPaginating = true;
  try {
    var m = getScaledPageMetrics();
    var usableH = Math.max(180, m.pageH - m.padT - m.padB);
    var stride = m.pageH + PAGE_GAP;

    function pageStart(p) { return (p - 1) * stride; }
    function pageEnd(p) { return (p - 1) * stride + usableH; }
    function nextPageStart(p) { return p * stride; }

    // Remove any legacy spacer divs (only if present from older saved content)
    var legacySpacers = editor.querySelectorAll('.doc-page-spacer');
    for (var ls = 0; ls < legacySpacers.length; ls++) {
      legacySpacers[ls].parentNode.removeChild(legacySpacers[ls]);
    }

    var children = Array.prototype.slice.call(editor.children);
    var caretBlock = getCaretBlock();
    lastCaretBlock = caretBlock;

    // Highest block index that has real typed content OR currently holds the cursor
    var lastActiveIdx = -1;
    for (var a = children.length - 1; a >= 0; a--) {
      var cEl = children[a];
      if (cEl.nodeType !== 1) continue;
      if (hasRealContent(cEl) || (caretBlock && (cEl === caretBlock || cEl.contains(caretBlock)))) {
        lastActiveIdx = a;
        break;
      }
    }

    var currentPage = 1;
    var prevBottom = 0;
    var prevMb = 0;
    var forceBreakBeforeNext = false;
    var writes = [];
    var pushedWrites = [];
    var parkedKept = false;
    var toRemove = [];

    // Pure in-memory measurement pass: NEVER strip existing marginTop before measuring,
    // so Page 2+ NEVER collapses/expands on every keystroke!
    for (var i = 0; i < children.length; i++) {
      var el = children[i];
      if (el.nodeType !== 1) continue;

      var isTrailingEmpty = (i > lastActiveIdx);
      if (isTrailingEmpty && el.dataset.pageParked && currentPage > 1) {
        toRemove.push(el);
        continue;
      }
      if (!isTrailingEmpty && el.dataset.pageParked) {
        delete el.dataset.pageParked;
      }

      var r = el.getBoundingClientRect();
      var h = r.height;
      var cs = window.getComputedStyle(el);
      var mb = parseFloat(cs.marginBottom) || 0;
      var hasPush = !!el.dataset.pagePush;
      var currentPush = hasPush ? (parseFloat(el.dataset.pagePush) || 0) : 0;
      var mt = hasPush ? getNaturalMarginTop(el) : (parseFloat(cs.marginTop) || 0);

      if (el.classList.contains('doc-page-break')) {
        if (hasPush) {
          writes.push({ el: el, push: 0, targetNext: 0 });
        }
        var brkGap = (i === 0) ? mt : Math.max(prevMb, mt);
        prevBottom = prevBottom + brkGap + h;
        prevMb = mb;
        forceBreakBeforeNext = true;
        continue;
      }

      var gap = (i === 0) ? mt : Math.max(prevMb, mt);
      var naturalTop = prevBottom + gap;
      var naturalBottom = naturalTop + (el.dataset.pageParked ? 25 : h);
      var currentMax = pageEnd(currentPage);

      var isAtPageStart = !forceBreakBeforeNext && (prevBottom <= pageStart(currentPage) + 2);
      var fitsOnPage = !forceBreakBeforeNext && (naturalBottom <= currentMax + 1);

      if (fitsOnPage || isAtPageStart) {
        if (el.dataset.pageParked) {
          delete el.dataset.pageParked;
        }
        if (hasPush) {
          writes.push({ el: el, push: 0, targetNext: 0 });
        }
        prevBottom = naturalBottom;
        prevMb = mb;
        forceBreakBeforeNext = false;
      } else if (isTrailingEmpty) {
        // No typed content AND no cursor on this overflow block -> do NOT keep an empty page open!
        if (hasPush) {
          writes.push({ el: el, push: 0, targetNext: 0 });
        }
        if (!parkedKept) {
          parkedKept = true;
          if (!el.dataset.pageParked) {
            el.dataset.pageParked = '1';
          }
        } else {
          toRemove.push(el);
        }
      } else {
        if (!forceBreakBeforeNext && naturalTop < currentMax - 20 && h >= 36 && splitBlockAtPageBoundary(el, currentMax)) {
          if (hasPush) {
            writes.push({ el: el, push: 0, targetNext: 0 });
          }
          var hFirst = el.getBoundingClientRect().height;
          prevBottom = naturalTop + hFirst;
          prevMb = mb;
          if (el.nextSibling) {
            children.splice(i + 1, 0, el.nextSibling);
            if (lastActiveIdx >= i) lastActiveIdx++;
          }
          continue;
        }

        // Exceeds currentPage! Push cleanly via margin-top to exact top of nextPage (zero extra DOM nodes)
        var targetNext = nextPageStart(currentPage);
        var desiredPush = Math.max(0, Math.round((targetNext - prevBottom) * 100) / 100);
        if (!hasPush || Math.abs(desiredPush - currentPush) > 0.5) {
          var wItem = { el: el, push: desiredPush, targetNext: targetNext };
          writes.push(wItem);
          pushedWrites.push(wItem);
        }
        currentPage++;
        prevBottom = targetNext + h;
        prevMb = mb;
        forceBreakBeforeNext = false;
      }
    }

    // Remove redundant extra empty trailing blocks beyond the 1 parked block
    for (var rm = 0; rm < toRemove.length; rm++) {
      if (toRemove[rm].parentNode) {
        toRemove[rm].parentNode.removeChild(toRemove[rm]);
      }
    }

    // Single write pass: ZERO DOM writes when typing normally on Page 1 or Page 2+!
    for (var w = 0; w < writes.length; w++) {
      var item = writes[w];
      if (item.push > 0) {
        item.el.style.marginTop = item.push + 'px';
        item.el.dataset.pagePush = String(item.push);
      } else {
        item.el.style.marginTop = item.el.dataset.spaceBefore ? (item.el.dataset.spaceBefore + 'px') : '';
        delete item.el.dataset.pagePush;
      }
    }

    // Drift verification ONLY when a page break push was newly created/updated
    if (pushedWrites.length > 0) {
      var edRectAfter = editor.getBoundingClientRect();
      for (var pw = 0; pw < pushedWrites.length; pw++) {
        var pItem = pushedWrites[pw];
        var actualTop = pItem.el.getBoundingClientRect().top - edRectAfter.top;
        var drift = actualTop - pItem.targetNext;
        if (Math.abs(drift) > 0.5) {
          var adj = Math.max(0, Math.round((pItem.push - drift) * 100) / 100);
          pItem.el.style.marginTop = adj + 'px';
          pItem.el.dataset.pagePush = String(adj);
        }
      }
    }

    var pageCount = Math.max(1, currentPage);
    estimatedPageCount = pageCount;

    // Snap #paper height to exact N full sheets + gaps
    var totalPaperH = pageCount * m.pageH + (pageCount - 1) * PAGE_GAP;
    var targetMinH = totalPaperH + 'px';
    if (paper.style.minHeight !== targetMinH) {
      paper.style.minHeight = targetMinH;
    }

    // Incremental sheet card updates: NEVER flash/wipe existing sheets when pageCount changes!
    var layoutKey = m.pageH + '|' + m.padL + '|' + m.padR + '|' +
      (pageConfig.headerText || '') + '|' + (pageConfig.footerText || '') + '|' + pageConfig.showPageNumbers;

    if (layoutKey !== lastSheetsRenderKey) {
      lastSheetsRenderKey = layoutKey;
      var frag = document.createDocumentFragment();
      for (var p = 1; p <= pageCount; p++) {
        frag.appendChild(createSheetCard(p, m));
      }
      pageSheetsBg.innerHTML = '';
      pageSheetsBg.appendChild(frag);
    } else {
      var curSheets = pageSheetsBg.children.length;
      while (curSheets < pageCount) {
        curSheets++;
        pageSheetsBg.appendChild(createSheetCard(curSheets, m));
      }
      while (curSheets > pageCount) {
        pageSheetsBg.removeChild(pageSheetsBg.lastChild);
        curSheets--;
      }
    }

    lastPaginatedEditorH = editor.offsetHeight;
  } finally {
    isPaginating = false;
  }

  updateWordCount();
}

function rebuildPageGuides() {
  paginateDocument();
}

window.addEventListener('resize', function() {
  applyPageSetup(pageConfig);
});

if (typeof ResizeObserver !== 'undefined') {
  var ro = new ResizeObserver(function() {
    if (!isPaginating && editor.offsetHeight !== lastPaginatedEditorH) {
      paginateDocument();
    }
  });
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
    if (node.nodeType === 1 && !node.classList.contains('doc-page-spacer') && /^(P|DIV|H1|H2|H3|H4|H5|H6|LI|BLOCKQUOTE|PRE)$/.test(node.tagName)) {
      return node;
    }
    node = node.parentNode;
  }
  return null;
}

function findClosestBlockquote(node) {
  while (node && node !== editor) {
    if (node.nodeType === 1 && node.tagName === 'BLOCKQUOTE') {
      return node;
    }
    node = node.parentNode;
  }
  return null;
}

function isCaretAtBlockStart(block, range) {
  if (!block || !range || !range.collapsed) return false;
  var testRange = document.createRange();
  testRange.setStart(block, 0);
  testRange.setEnd(range.startContainer, range.startOffset);
  var beforeTxt = testRange.toString().replace(/\u200B/g, '');
  return beforeTxt.length === 0;
}

function splitSoftBreaksIfNeeded(block) {
  if (!block || (block.tagName !== 'P' && block.tagName !== 'DIV')) return false;
  if (block.classList.contains('doc-page-break')) return false;
  var brs = block.querySelectorAll('br');
  if (brs.length === 0) return false;
  // Check if any direct-child <br> actually separates two lines (has content after it)
  var hasSeparatingBr = false;
  for (var i = 0; i < block.childNodes.length; i++) {
    var cn = block.childNodes[i];
    if (cn.nodeType === 1 && cn.tagName === 'BR') {
      var nxt = cn.nextSibling;
      while (nxt && nxt.nodeType === 3 && !nxt.nodeValue) nxt = nxt.nextSibling;
      if (nxt) {
        hasSeparatingBr = true;
        break;
      }
    }
  }
  if (!hasSeparatingBr) return false;

  // Save caret position before splitting soft breaks into per-line <p> elements
  var sel = window.getSelection();
  var sc = null, so = 0, ec = null, eo = 0;
  if (sel && sel.rangeCount > 0) {
    var sr = sel.getRangeAt(0);
    sc = sr.startContainer; so = sr.startOffset;
    ec = sr.endContainer; eo = sr.endOffset;
  }

  var groups = [[]];
  while (block.firstChild) {
    var child = block.firstChild;
    block.removeChild(child);
    if (child.nodeType === 1 && child.tagName === 'BR') {
      groups.push([]);
    } else {
      groups[groups.length - 1].push(child);
    }
  }
  // Remove trailing empty group if block ended with a placeholder <br>
  if (groups.length > 1 && groups[groups.length - 1].length === 0) {
    groups.pop();
  }

  var parent = block.parentNode;
  var refNext = block.nextSibling;
  var baseStyle = block.style.cssText;

  groups.forEach(function(nodes, idx) {
    var p = (idx === 0) ? block : document.createElement('p');
    if (idx > 0) {
      p.style.cssText = baseStyle;
      p.style.marginTop = '';
      delete p.dataset.pagePush;
      delete p.dataset.pageParked;
      parent.insertBefore(p, refNext);
    }
    if (nodes.length === 0) {
      p.innerHTML = '<br>';
    } else {
      nodes.forEach(function(n) { p.appendChild(n); });
    }
  });

  if (sel && sc && editor.contains(sc)) {
    try {
      var nr = document.createRange();
      nr.setStart(sc, so);
      nr.setEnd(ec && editor.contains(ec) ? ec : sc, ec && editor.contains(ec) ? eo : so);
      sel.removeAllRanges();
      sel.addRange(nr);
      savedRange = nr.cloneRange();
    } catch(_) {}
  }
  return true;
}

function getSelectedBlocks() {
  var blocks = [];
  var sel = window.getSelection();
  if (!sel || sel.rangeCount === 0) return blocks;
  var range = sel.getRangeAt(0);
  if (!editor.contains(range.commonAncestorContainer)) return blocks;

  // Wrap bare top-level text node into <p> so per-line/per-paragraph formatting always works
  if (range.startContainer.nodeType === 3 && range.startContainer.parentNode === editor) {
    var tn = range.startContainer;
    var pWrap = document.createElement('p');
    editor.insertBefore(pWrap, tn);
    pWrap.appendChild(tn);
    var nr = document.createRange();
    nr.setStart(tn, range.startOffset);
    nr.setEnd(range.endContainer === tn ? tn : range.endContainer, range.endOffset);
    sel.removeAllRanges();
    sel.addRange(nr);
    savedRange = nr.cloneRange();
    range = nr;
  }

  var startBlock = findClosestBlock(range.startContainer);
  var endBlock = findClosestBlock(range.endContainer);
  if (!startBlock && !endBlock) return blocks;

  // Split any soft <br> breaks inside the active paragraph so indentation applies strictly per line!
  if (startBlock && splitSoftBreaksIfNeeded(startBlock)) {
    range = sel.getRangeAt(0);
    startBlock = findClosestBlock(range.startContainer);
    endBlock = findClosestBlock(range.endContainer);
  }
  if (endBlock && endBlock !== startBlock && splitSoftBreaksIfNeeded(endBlock)) {
    range = sel.getRangeAt(0);
    endBlock = findClosestBlock(range.endContainer);
  }

  if (startBlock && (!endBlock || startBlock === endBlock)) {
    return [startBlock];
  }
  var all = editor.querySelectorAll('p, div:not(.doc-page-break):not(.doc-page-spacer), h1, h2, h3, h4, h5, li, blockquote');
  var inRange = false;
  for (var i = 0; i < all.length; i++) {
    var el = all[i];
    if (el === startBlock) inRange = true;
    if (inRange) blocks.push(el);
    if (el === endBlock) break;
  }
  return blocks.length ? blocks : (startBlock ? [startBlock] : []);
}

function normalizeLineSpacingStr(raw) {
  if (!raw) return '1.15';
  var num = parseFloat(raw);
  if (isNaN(num)) return '1.15';
  if (Math.abs(num - 1.0) < 0.03) return '1.0';
  if (Math.abs(num - 1.15) < 0.03) return '1.15';
  if (Math.abs(num - 1.5) < 0.03) return '1.5';
  if (Math.abs(num - 2.0) < 0.03) return '2.0';
  return String(num);
}

function getBlockIndentInfo(b) {
  if (!b) return { indentPx: 0, mode: 'none', lineSpacing: '1.15', spaceBeforePt: 0, spaceAfterPt: 0 };
  var cs = window.getComputedStyle(b);
  var ml = Math.round(parseFloat(b.style.marginLeft || '0') || 0);
  var ti = Math.round(parseFloat(cs.textIndent || '0') || 0);
  var mode = 'none';
  if (ti > 4) mode = 'first';
  else if (ti < -4) mode = 'hanging';
  var rawLs = b.style.lineHeight || editor.style.lineHeight || '1.15';
  var mtPx = b.dataset.spaceBefore
    ? (parseFloat(b.dataset.spaceBefore) || 0)
    : (b.dataset.pagePush ? 0 : (parseFloat(b.style.marginTop || '0') || 0));
  var mbPx = parseFloat(b.style.marginBottom || '0') || 0;
  return {
    indentPx: ml,
    mode: mode,
    lineSpacing: normalizeLineSpacingStr(rawLs),
    spaceBeforePt: mtPx > 0.5 ? Math.round(mtPx * 0.75) : 0,
    spaceAfterPt: mbPx > 0.5 ? Math.round(mbPx * 0.75) : 0
  };
}

function setLineSpacing(multiplier) {
  restoreSelection();
  var blocks = getSelectedBlocks();
  if (blocks.length === 0) {
    editor.style.lineHeight = String(multiplier);
  } else {
    blocks.forEach(function(b) {
      b.style.lineHeight = String(multiplier);
      b.querySelectorAll('span, font').forEach(function(el) {
        if (el.style && el.style.lineHeight) el.style.lineHeight = '';
      });
      syncBlockStrutFontSize(b);
    });
  }
  paginateDocument();
  syncToolbarState();
  notifyChange();
}

function applySpaceBeforePx(b, px) {
  if (px > 0.5) {
    var rounded = Math.round(px * 100) / 100;
    b.dataset.spaceBefore = String(rounded);
    if (!b.dataset.pagePush) {
      b.style.marginTop = rounded + 'px';
    }
  } else {
    delete b.dataset.spaceBefore;
    if (!b.dataset.pagePush) {
      b.style.marginTop = '';
    }
  }
}

function toggleSpaceBeforeParagraph() {
  restoreSelection();
  var blocks = getSelectedBlocks();
  if (blocks.length === 0) return;
  var info = getBlockIndentInfo(blocks[0]);
  var nextPx = info.spaceBeforePt > 0 ? 0 : (10 * 4 / 3); // Google Docs standard 10pt = 13.33px
  blocks.forEach(function(b) { applySpaceBeforePx(b, nextPx); });
  paginateDocument();
  syncToolbarState();
  notifyChange();
}

function toggleSpaceAfterParagraph() {
  restoreSelection();
  var blocks = getSelectedBlocks();
  if (blocks.length === 0) return;
  var info = getBlockIndentInfo(blocks[0]);
  var nextVal = info.spaceAfterPt > 0 ? '' : '13.33px'; // Google Docs standard 10pt = 13.33px
  blocks.forEach(function(b) { b.style.marginBottom = nextVal; });
  paginateDocument();
  syncToolbarState();
  notifyChange();
}

function setParagraphSpacing(beforePt, afterPt) {
  restoreSelection();
  var blocks = getSelectedBlocks();
  if (blocks.length === 0) return;
  var beforePx = beforePt > 0 ? (Math.round(beforePt * 400 / 3) / 100) : 0;
  var afterVal = afterPt > 0 ? ((Math.round(afterPt * 400 / 3) / 100) + 'px') : '';
  blocks.forEach(function(b) {
    applySpaceBeforePx(b, beforePx);
    b.style.marginBottom = afterVal;
  });
  paginateDocument();
  syncToolbarState();
  notifyChange();
}

function adjustIndent(deltaPx) {
  restoreSelection();
  var blocks = getSelectedBlocks();
  if (blocks.length === 0) return;
  blocks.forEach(function(b) {
    var cur = Math.round(parseFloat(b.style.marginLeft || '0') || 0);
    var ti = Math.round(parseFloat(window.getComputedStyle(b).textIndent || '0') || 0);
    if (deltaPx < 0 && ti < -4 && cur <= 48) {
      // Decreasing indent on a hanging paragraph cleanly resets hanging indent
      b.style.textIndent = '';
      b.style.marginLeft = '';
      return;
    }
    var next = Math.max(0, Math.min(288, cur + deltaPx));
    b.style.marginLeft = next > 0 ? (next + 'px') : '';
    if (next === 0 && ti < -4) {
      b.style.textIndent = '';
    }
  });
  paginateDocument();
  syncToolbarState();
  notifyChange();
}

function setIndentMode(mode) {
  restoreSelection();
  var blocks = getSelectedBlocks();
  if (blocks.length === 0) return;
  blocks.forEach(function(b) {
    var curMl = Math.round(parseFloat(b.style.marginLeft || '0') || 0);
    if (mode === 'first') {
      // First-line indent (1.27cm ≈ 48px), keep any existing left indent >= 0
      var curTi = Math.round(parseFloat(window.getComputedStyle(b).textIndent || '0') || 0);
      if (curTi < -4 && curMl >= 48) {
        var restoredMl = curMl - 48;
        b.style.marginLeft = restoredMl > 0 ? (restoredMl + 'px') : '';
      }
      b.style.textIndent = '48px';
    } else if (mode === 'hanging') {
      // Hanging indent (Daftar Pustaka / APA): wrapped lines indented 48px, first line back -48px
      var curTi2 = Math.round(parseFloat(window.getComputedStyle(b).textIndent || '0') || 0);
      if (curTi2 >= 0) {
        b.style.marginLeft = (curMl + 48) + 'px';
      } else if (curMl < 48) {
        b.style.marginLeft = '48px';
      }
      b.style.textIndent = '-48px';
    } else {
      // 'none': remove first-line / hanging special indent while preserving regular left indent
      var curTi3 = Math.round(parseFloat(window.getComputedStyle(b).textIndent || '0') || 0);
      if (curTi3 < -4 && curMl >= 48) {
        var newMl = curMl - 48;
        b.style.marginLeft = newMl > 0 ? (newMl + 'px') : '';
      }
      b.style.textIndent = '';
    }
  });
  paginateDocument();
  syncToolbarState();
  notifyChange();
}

function toggleFirstLineIndent() {
  restoreSelection();
  var blocks = getSelectedBlocks();
  if (blocks.length === 0) return;
  var info = getBlockIndentInfo(blocks[0]);
  setIndentMode(info.mode === 'first' ? 'none' : 'first');
}

function resetIndent() {
  restoreSelection();
  var blocks = getSelectedBlocks();
  if (blocks.length === 0) return;
  blocks.forEach(function(b) {
    b.style.marginLeft = '';
    b.style.textIndent = '';
  });
  paginateDocument();
  syncToolbarState();
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

function cleanEmptyBlockquotes(excludeActive) {
  var bqs = editor.querySelectorAll('blockquote');
  var sel = window.getSelection();
  var activeNode = (sel && sel.rangeCount > 0) ? sel.getRangeAt(0).commonAncestorContainer : null;
  for (var i = 0; i < bqs.length; i++) {
    var b = bqs[i];
    if (!b.textContent.trim() && !b.querySelector('img, table')) {
      if (excludeActive && activeNode && b.contains(activeNode)) {
        continue;
      }
      b.parentNode.removeChild(b);
    }
  }
}

// ── CHANGE NOTIFICATION ──
function getCleanHtml() {
  var clone = editor.cloneNode(true);
  var spacers = clone.querySelectorAll('.doc-page-spacer');
  for (var i = 0; i < spacers.length; i++) {
    spacers[i].parentNode.removeChild(spacers[i]);
  }
  var parked = clone.querySelectorAll('[data-page-parked]');
  for (var p = 0; p < parked.length; p++) {
    parked[p].parentNode.removeChild(parked[p]);
  }
  var isBlankElem = function(el) {
    if (!el) return false;
    if (el.tagName !== 'P' && el.tagName !== 'DIV') return false;
    if (el.classList.contains('doc-page-break')) return false;
    if (el.querySelector('img, table, iframe')) return false;
    return !(el.textContent || '').replace(/\u200B/g, '').trim();
  };

  var pushed = clone.querySelectorAll('[data-page-push]');
  for (var k = 0; k < pushed.length; k++) {
    pushed[k].style.marginTop = pushed[k].dataset.spaceBefore ? (pushed[k].dataset.spaceBefore + 'px') : '';
    pushed[k].removeAttribute('data-page-push');
    if (!pushed[k].getAttribute('style')) pushed[k].removeAttribute('style');
  }

  // Collapse excessive consecutive blank paragraphs (e.g. repeated Enters typed to reach next page)
  var rootChildren = Array.prototype.slice.call(clone.children);
  var blankStreak = [];
  for (var ci = 0; ci < rootChildren.length; ci++) {
    var cNode = rootChildren[ci];
    if (isBlankElem(cNode)) {
      blankStreak.push(cNode);
    } else {
      if (blankStreak.length >= 4) {
        var pb = document.createElement('div');
        pb.className = 'doc-page-break';
        cNode.parentNode.insertBefore(pb, cNode);
        for (var bi = 0; bi < blankStreak.length; bi++) {
          if (blankStreak[bi].parentNode) {
            blankStreak[bi].parentNode.removeChild(blankStreak[bi]);
          }
        }
      }
      blankStreak = [];
    }
  }

  var pbs = clone.querySelectorAll('.doc-page-break');
  for (var b = 0; b < pbs.length; b++) {
    pbs[b].innerHTML = '';
  }
  var anchors = clone.querySelectorAll('[data-typing-anchor]');
  for (var a = 0; a < anchors.length; a++) {
    var anc = anchors[a];
    anc.removeAttribute('data-typing-anchor');
    var rawTxt = (anc.textContent || '').replace(/\u200B/g, '');
    if (!rawTxt && !anc.querySelector('img, br')) {
      anc.parentNode.removeChild(anc);
    }
  }
  var bqs = clone.querySelectorAll('blockquote');
  for (var j = 0; j < bqs.length; j++) {
    if (!bqs[j].textContent.trim() && !bqs[j].querySelector('img, table')) {
      bqs[j].parentNode.removeChild(bqs[j]);
    }
  }
  // Trim excessive trailing empty paragraphs so they don't pollute saved HTML or exports
  while (clone.lastElementChild &&
         (clone.lastElementChild.tagName === 'P' || clone.lastElementChild.tagName === 'DIV') &&
         !clone.lastElementChild.classList.contains('doc-page-break') &&
         !clone.lastElementChild.querySelector('img, table, iframe') &&
         !(clone.lastElementChild.textContent || '').replace(/\u200B/g, '').trim()) {
    clone.removeChild(clone.lastElementChild);
  }
  return clone.innerHTML.replace(/\u200B/g, '');
}

function notifyChange() {
  updateWordCount();
  clearTimeout(debounceTimer);
  debounceTimer = setTimeout(function() {
    LernspaceDoc.onContentChange(getCleanHtml());
  }, 350);
}

function flushChangeNow() {
  clearTimeout(debounceTimer);
  updateWordCount();
  LernspaceDoc.onContentChange(getCleanHtml());
}

editor.addEventListener('input', function() {
  if (selectedImg) deselectImage();
  // Clean up typing anchor marker once user has typed real characters inside it
  var anchors = editor.querySelectorAll('span[data-typing-anchor="1"]');
  for (var a = 0; a < anchors.length; a++) {
    var anc = anchors[a];
    var txt = anc.textContent || '';
    if (txt.replace(/\u200B/g, '').length > 0) {
      delete anc.dataset.typingAnchor;
    }
  }
  paginateDocument();
  syncToolbarState();
  notifyChange();
  scheduleScrollIntoView();
});

editor.addEventListener('blur', function() {
  cleanEmptyBlockquotes(false);
  lastCaretBlock = null;
  paginateDocument();
  flushChangeNow();
});

// ── SELECTION TRACKING & PER-CHARACTER TOOLBAR SYNC (GOOGLE DOCS STYLE) ──
document.addEventListener('selectionchange', function() {
  var sel = window.getSelection();
  if (!sel || sel.rangeCount === 0) return;
  var range = sel.getRangeAt(0);
  if (editor.contains(range.commonAncestorContainer)) {
    savedRange = range.cloneRange();
    syncToolbarState();
    var blk = getCaretBlock();
    if (blk !== lastCaretBlock) {
      lastCaretBlock = blk;
      paginateDocument();
      scheduleScrollIntoView();
    }
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

function rgbToHex(rgb) {
  if (!rgb) return '#1E293B';
  if (rgb.charAt(0) === '#') return rgb;
  var m = rgb.match(/\d+/g);
  if (!m || m.length < 3) return '#1E293B';
  var r = parseInt(m[0], 10).toString(16).padStart(2, '0');
  var g = parseInt(m[1], 10).toString(16).padStart(2, '0');
  var b = parseInt(m[2], 10).toString(16).padStart(2, '0');
  return '#' + r + g + b;
}

function matchFontFamilyName(computedFont) {
  var f = (computedFont || '').toLowerCase();
  if (f.indexOf('poppins') !== -1) return 'POPPINS';
  if (f.indexOf('montserrat') !== -1) return 'MONTSERRAT';
  if (f.indexOf('sans-serif-light') !== -1) return 'ROBOTO_LIGHT';
  if (f.indexOf('merriweather') !== -1) return 'MERRIWEATHER';
  if (f.indexOf('lora') !== -1) return 'LORA';
  if (f.indexOf('playfair') !== -1) return 'PLAYFAIR';
  if (f.indexOf('roboto mono') !== -1 || f.indexOf('courier') !== -1) return 'ROBOTO_MONO';
  if (f.indexOf('casual') !== -1 || f.indexOf('comic sans') !== -1) return 'CASUAL';
  if (f.indexOf('caveat') !== -1) return 'CAVEAT';
  return 'INTER';
}

function getIntersectingTextNodes(range) {
  var common = range.commonAncestorContainer;
  if (common.nodeType === 3) {
    return (common.nodeValue && common.nodeValue.length > 0) ? [common] : [];
  }
  var walker = document.createTreeWalker(common, NodeFilter.SHOW_TEXT, null, false);
  var out = [];
  var tn;
  while ((tn = walker.nextNode())) {
    if (!tn.nodeValue || tn.nodeValue.length === 0) continue;
    var nr = document.createRange();
    nr.selectNodeContents(tn);
    if (range.compareBoundaryPoints(Range.END_TO_START, nr) < 0 &&
        range.compareBoundaryPoints(Range.START_TO_END, nr) > 0) {
      out.push(tn);
    }
  }
  return out;
}

function pxToPt(px) {
  var val = parseFloat(px);
  if (isNaN(val) || val <= 0) return 11;
  return Math.max(6, Math.round(val * 0.75));
}
function ptToPx(pt) {
  return (Math.round(pt * 400 / 3) / 100) + 'px';
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

    var fontSizePt = 11;
    var fontFamilyName = 'INTER';
    var blockTag = 'p';
    var textColorHex = '#1E293B';
    var lineSpacingStr = '1.15';
    var indentPx = 0;
    var indentMode = 'none';
    var spaceBeforePt = 0;
    var spaceAfterPt = 0;

    var sel = window.getSelection();
    if (sel && sel.rangeCount > 0 && editor.contains(sel.getRangeAt(0).commonAncestorContainer)) {
      var range = sel.getRangeAt(0);
      var blk = findClosestBlock(range.startContainer);
      if (blk) {
        var tg = blk.tagName.toLowerCase();
        if (/^(p|h1|h2|h3|h4|h5)$/.test(tg)) blockTag = tg;
        var indInfo = getBlockIndentInfo(blk);
        indentPx = indInfo.indentPx;
        indentMode = indInfo.mode;
        lineSpacingStr = indInfo.lineSpacing;
        spaceBeforePt = indInfo.spaceBeforePt;
        spaceAfterPt = indInfo.spaceAfterPt;
      }

      if (range.collapsed) {
        var node = range.startContainer;
        var el = (node.nodeType === 3) ? node.parentElement : node;
        if (!el || !editor.contains(el)) el = editor;
        var cs = window.getComputedStyle(el);
        fontSizePt = pxToPt(cs.fontSize);
        fontFamilyName = matchFontFamilyName(cs.fontFamily);
        textColorHex = rgbToHex(cs.color);
      } else {
        var texts = getIntersectingTextNodes(range);
        if (texts.length > 0) {
          var firstEl = texts[0].parentElement || editor;
          var firstCs = window.getComputedStyle(firstEl);
          var fs = pxToPt(firstCs.fontSize);
          var ff = matchFontFamilyName(firstCs.fontFamily);
          textColorHex = rgbToHex(firstCs.color);
          for (var t = 1; t < texts.length; t++) {
            var tCs = window.getComputedStyle(texts[t].parentElement || editor);
            var tFs = pxToPt(tCs.fontSize);
            var tFf = matchFontFamilyName(tCs.fontFamily);
            if (tFs !== fs) fs = -1;
            if (tFf !== ff) ff = '';
            if (fs === -1 && ff === '') break;
          }
          fontSizePt = fs;
          fontFamilyName = ff;
        } else {
          var elFallback = (range.startContainer.nodeType === 3) ? range.startContainer.parentElement : range.startContainer;
          var csFb = window.getComputedStyle(elFallback || editor);
          fontSizePt = pxToPt(csFb.fontSize);
          fontFamilyName = matchFontFamilyName(csFb.fontFamily);
          textColorHex = rgbToHex(csFb.color);
        }
      }
    }

    var key = b + '|' + i + '|' + u + '|' + s + '|' + alignIdx + '|' + fontSizePt + '|' + fontFamilyName + '|' + blockTag + '|' + textColorHex + '|' + lineSpacingStr + '|' + indentPx + '|' + indentMode + '|' + spaceBeforePt + '|' + spaceAfterPt;
    if (key !== lastFmtKey) {
      lastFmtKey = key;
      LernspaceDoc.onFormatState(b, i, u, s, alignIdx, fontSizePt, fontFamilyName, blockTag, textColorHex, lineSpacingStr, indentPx, indentMode, spaceBeforePt, spaceAfterPt);
    }
  } catch(_) {}
}

// ── GENTLE SCROLL INTO VIEW (ZERO-LAG RAF) ──
var scrollRaf = 0;
function scheduleScrollIntoView() {
  if (scrollRaf) cancelAnimationFrame(scrollRaf);
  scrollRaf = requestAnimationFrame(function() {
    scrollRaf = 0;
    scrollCaretIntoView();
  });
}
function scrollCaretIntoView() {
  var sel = window.getSelection();
  if (!sel || sel.rangeCount === 0 || !sel.isCollapsed) return;
  var range = sel.getRangeAt(0);
  if (!editor.contains(range.commonAncestorContainer)) return;
  var rect = range.getBoundingClientRect();
  if (!rect || (rect.top === 0 && rect.bottom === 0)) {
    var block = findClosestBlock(range.startContainer);
    if (block) {
      rect = block.getBoundingClientRect();
    }
  }
  if (!rect || (rect.top === 0 && rect.bottom === 0)) return;
  var vvTop = (window.visualViewport && window.visualViewport.offsetTop) || 0;
  var vvH = (window.visualViewport && window.visualViewport.height) || window.innerHeight;
  var vvBottom = vvTop + vvH;
  if (rect.bottom > vvBottom - 40) {
    window.scrollBy(0, rect.bottom - vvBottom + 64);
  } else if (rect.top < vvTop + 20) {
    window.scrollBy(0, rect.top - vvTop - 48);
  }
}
if (window.visualViewport) {
  window.visualViewport.addEventListener('resize', function() {
    scheduleScrollIntoView();
    if (selectedImg) repositionImageOverlay();
  });
}

// ── PER-CHARACTER INLINE STYLE ENGINE (GOOGLE DOCS MODEL) ──
function syncBlockStrutFontSize(block) {
  if (!block || (block.tagName !== 'P' && block.tagName !== 'DIV' && block.tagName !== 'LI')) return;
  if (block.classList.contains('doc-page-break')) return;

  // 1. Strip fontSize from any outer wrapper span/font that has NO direct non-empty text of its own
  //    (prevents a giant outer <span style="font-size:26px"><span style="font-size:12px"> from inflating line height!)
  var inlines = block.querySelectorAll('span, font');
  for (var i = 0; i < inlines.length; i++) {
    var el = inlines[i];
    if ((el.style && el.style.fontSize) || (el.tagName === 'FONT' && el.hasAttribute('size'))) {
      var hasDirectText = false;
      for (var c = 0; c < el.childNodes.length; c++) {
        var ch = el.childNodes[c];
        if (ch.nodeType === 3 && (ch.nodeValue || '').replace(/\u200B/g, '').length > 0) {
          hasDirectText = true;
          break;
        }
      }
      if (!hasDirectText && el.querySelector('span[style*="font-size"], font[size]')) {
        if (el.style) el.style.fontSize = '';
        if (el.tagName === 'FONT') el.removeAttribute('size');
      }
    }
  }

  // 2. Sync block's own strut fontSize to the smallest explicit span size if all text in block is inside spans
  var walker = document.createTreeWalker(block, NodeFilter.SHOW_TEXT, null, false);
  var tn;
  var minPx = Infinity;
  var hasAnyText = false;
  while ((tn = walker.nextNode())) {
    var v = (tn.nodeValue || '').replace(/\u200B/g, '');
    if (!v.length) continue;
    hasAnyText = true;
    var pEl = tn.parentElement;
    if (pEl === block) return;
    var sz = parseFloat(window.getComputedStyle(pEl).fontSize || '14.67');
    if (sz < minPx) minPx = sz;
  }
  if (hasAnyText && minPx < Infinity) {
    block.style.fontSize = (Math.round(minPx * 100) / 100) + 'px';
  }
}

function normalizeAllBlockLineBoxes() {
  var blocks = editor.querySelectorAll('p, div:not(.doc-page-break), li');
  for (var i = 0; i < blocks.length; i++) {
    syncBlockStrutFontSize(blocks[i]);
  }
}

function applyInlineStyle(prop, value) {
  restoreSelection();
  var sel = window.getSelection();
  if (!sel || sel.rangeCount === 0) return;
  var range = sel.getRangeAt(0);
  if (!editor.contains(range.commonAncestorContainer)) return;

  // Case 1: Collapsed caret -> update or insert typing anchor span so next typed letters get exact style
  if (range.collapsed) {
    var curBlk = findClosestBlock(range.startContainer);
    if (prop === 'fontSize' && curBlk && (curBlk.tagName === 'P' || curBlk.tagName === 'DIV' || curBlk.tagName === 'LI')) {
      var rawBlkTxt = (curBlk.textContent || '').replace(/\u200B/g, '').trim();
      if (!rawBlkTxt) {
        curBlk.style.fontSize = value;
      }
    }
    var container = range.startContainer;
    var parentEl = (container.nodeType === 3) ? container.parentElement : container;
    if (parentEl && parentEl.tagName === 'SPAN' && parentEl.dataset.typingAnchor === '1' &&
        parentEl.childNodes.length === 1 && parentEl.firstChild.nodeType === 3 &&
        parentEl.firstChild.nodeValue === '\u200B') {
      parentEl.style[prop] = value;
      var rReuse = document.createRange();
      rReuse.setStart(parentEl.firstChild, 1);
      rReuse.collapse(true);
      sel.removeAllRanges();
      sel.addRange(rReuse);
      savedRange = rReuse.cloneRange();
      paginateDocument();
      syncToolbarState();
      return;
    }
    var anchor = document.createElement('span');
    anchor.dataset.typingAnchor = '1';
    anchor.style[prop] = value;
    var zws = document.createTextNode('\u200B');
    anchor.appendChild(zws);
    range.insertNode(anchor);
    var rNew = document.createRange();
    rNew.setStart(zws, 1);
    rNew.collapse(true);
    sel.removeAllRanges();
    sel.addRange(rNew);
    savedRange = rNew.cloneRange();
    paginateDocument();
    syncToolbarState();
    return;
  }

  // Case 2: Selected text -> split boundary text nodes & style each character run cleanly, preserving selection!
  var startNode = range.startContainer;
  var startOff = range.startOffset;
  var endNode = range.endContainer;
  var endOff = range.endOffset;

  if (endNode.nodeType === 3 && endOff > 0 && endOff < endNode.nodeValue.length) {
    endNode.splitText(endOff);
  }
  if (startNode.nodeType === 3 && startOff > 0 && startOff < startNode.nodeValue.length) {
    var afterStart = startNode.splitText(startOff);
    if (startNode === endNode) {
      endNode = afterStart;
      endOff = afterStart.nodeValue.length;
    }
    startNode = afterStart;
    startOff = 0;
  }

  var workRange = document.createRange();
  workRange.setStart(startNode, startOff);
  workRange.setEnd(endNode, endOff);

  var selectedTexts = getIntersectingTextNodes(workRange);
  if (selectedTexts.length === 0) return;

  var touchedBlocks = [];
  for (var i = 0; i < selectedTexts.length; i++) {
    var t = selectedTexts[i];
    var p = t.parentElement;
    // Strip legacy <font size/face/color> attributes if overriding
    if (p && p.tagName === 'FONT') {
      if (prop === 'fontSize' && p.hasAttribute('size')) p.removeAttribute('size');
      if (prop === 'fontFamily' && p.hasAttribute('face')) p.removeAttribute('face');
      if (prop === 'color' && p.hasAttribute('color')) p.removeAttribute('color');
    }
    if (p && p.tagName === 'SPAN' && p.childNodes.length === 1 && !p.classList.contains('math-inline')) {
      p.style[prop] = value;
      delete p.dataset.typingAnchor;
    } else {
      var sp = document.createElement('span');
      sp.style[prop] = value;
      t.parentNode.insertBefore(sp, t);
      sp.appendChild(t);
    }
    if (prop === 'fontSize') {
      var bTouched = findClosestBlock(t);
      if (bTouched && touchedBlocks.indexOf(bTouched) === -1) touchedBlocks.push(bTouched);
    }
  }

  if (prop === 'fontSize') {
    touchedBlocks.forEach(syncBlockStrutFontSize);
  }

  // Keep exact selection intact so user can tap +/- or switch fonts repeatedly
  var firstT = selectedTexts[0];
  var lastT = selectedTexts[selectedTexts.length - 1];
  var finalRange = document.createRange();
  finalRange.setStart(firstT, 0);
  finalRange.setEnd(lastT, lastT.nodeValue.length);
  sel.removeAllRanges();
  sel.addRange(finalRange);
  savedRange = finalRange.cloneRange();

  paginateDocument();
  syncToolbarState();
  notifyChange();
}

// ── FORMATTING COMMANDS ──
function exec(cmd, val) {
  restoreSelection();
  document.execCommand(cmd, false, val !== undefined ? val : null);
  paginateDocument();
  syncToolbarState();
  notifyChange();
}
function formatBlock(tag) {
  restoreSelection();
  document.execCommand('formatBlock', false, '<' + tag + '>');
  // Clear inline fontSize overrides inside the newly formatted heading/paragraph so block style takes effect
  var blocks = getSelectedBlocks();
  blocks.forEach(function(blk) {
    blk.style.fontSize = '';
    blk.querySelectorAll('span, font').forEach(function(el) {
      if (el.style && el.style.fontSize) el.style.fontSize = '';
      if (el.tagName === 'FONT' && el.hasAttribute('size')) el.removeAttribute('size');
    });
  });
  paginateDocument();
  syncToolbarState();
  notifyChange();
}
function toggleBold() { exec('bold'); }
function toggleItalic() { exec('italic'); }
function toggleUnderline() { exec('underline'); }
function toggleStrikethrough() { exec('strikeThrough'); }
function setFontFamily(css) { applyInlineStyle('fontFamily', css); }
function setTextColor(color) { applyInlineStyle('color', color); }
function setHighlight(color) { applyInlineStyle('backgroundColor', color || 'transparent'); }
function setFontSize(pt) { applyInlineStyle('fontSize', ptToPx(pt)); }
function alignText(mode) {
  var cmds = { left: 'justifyLeft', center: 'justifyCenter', right: 'justifyRight', justify: 'justifyFull' };
  exec(cmds[mode] || 'justifyLeft');
}
function undo() { exec('undo'); }
function redo() { exec('redo'); }
function clearFormatting() {
  restoreSelection();
  document.execCommand('removeFormat', false, null);
  var sel = window.getSelection();
  if (sel && sel.rangeCount > 0 && !sel.isCollapsed) {
    var texts = getIntersectingTextNodes(sel.getRangeAt(0));
    texts.forEach(function(t) {
      var p = t.parentElement;
      if (p && (p.tagName === 'SPAN' || p.tagName === 'FONT') && p.childNodes.length === 1 && !p.classList.contains('math-inline')) {
        p.style.fontSize = '';
        p.style.fontFamily = '';
        p.style.color = '';
        p.style.backgroundColor = '';
        if (p.tagName === 'FONT') {
          p.removeAttribute('size');
          p.removeAttribute('face');
          p.removeAttribute('color');
        }
      }
    });
  }
  paginateDocument();
  syncToolbarState();
  notifyChange();
}

// ── LISTS & BLOCKS ──
function insertBulletList() { exec('insertUnorderedList'); }
function insertNumberedList() { exec('insertOrderedList'); }
function insertBlockquote() {
  restoreSelection();
  var sel = window.getSelection();
  if (sel && sel.rangeCount > 0) {
    var bq = findClosestBlockquote(sel.getRangeAt(0).commonAncestorContainer);
    if (bq) {
      exec('formatBlock', '<p>');
      notifyChange();
      return;
    }
  }
  exec('formatBlock', '<blockquote>');
  notifyChange();
}
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
  normalizeAllBlockLineBoxes();
  rebuildPageGuides();
}
function applyDarkTheme(dark) {
  document.documentElement.classList.toggle('dark', !!dark);
}

editor.addEventListener('keydown', function(e) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
    e.preventDefault();
    insertPageBreak();
    paginateDocument();
    notifyChange();
    scheduleScrollIntoView();
    return;
  }
  if (e.key === 'Tab') {
    e.preventDefault();
    var selTab = window.getSelection();
    if (!selTab || selTab.rangeCount === 0) return;
    var rTab = selTab.getRangeAt(0);
    if (e.shiftKey) {
      var bShift = findClosestBlock(rTab.startContainer);
      var infoShift = getBlockIndentInfo(bShift);
      if (infoShift.mode !== 'none') {
        setIndentMode('none');
      } else {
        adjustIndent(-36);
      }
      return;
    }
    var blocksTab = getSelectedBlocks();
    if (blocksTab.length > 1) {
      adjustIndent(36);
      return;
    }
    var singleBlk = blocksTab[0] || findClosestBlock(rTab.startContainer);
    if (singleBlk && isCaretAtBlockStart(singleBlk, rTab)) {
      if (singleBlk.tagName === 'LI') {
        adjustIndent(36);
      } else {
        var infoTab = getBlockIndentInfo(singleBlk);
        if (infoTab.mode === 'none' && infoTab.indentPx === 0) {
          setIndentMode('first');
        } else {
          adjustIndent(36);
        }
      }
      return;
    }
    document.execCommand('insertHTML', false, '&nbsp;&nbsp;&nbsp;&nbsp;');
    paginateDocument();
    notifyChange();
    return;
  }

  var sel = window.getSelection();
  if (!sel || !sel.isCollapsed || sel.rangeCount === 0) return;
  var range = sel.getRangeAt(0);

  // ── INDENT BACKSPACE OUTDENT (GOOGLE DOCS / WORD BEHAVIOR) ──
  if (e.key === 'Backspace') {
    var indBlk = findClosestBlock(range.startContainer);
    if (indBlk && indBlk.tagName !== 'BLOCKQUOTE' && isCaretAtBlockStart(indBlk, range)) {
      var indInfo = getBlockIndentInfo(indBlk);
      if (indInfo.mode !== 'none') {
        e.preventDefault();
        setIndentMode('none');
        return;
      } else if (indInfo.indentPx > 0) {
        e.preventDefault();
        adjustIndent(-36);
        return;
      }
    }
  }

  // ── BLOCKQUOTE CLEANUP (BACKSPACE & ENTER) ──
  var bq = findClosestBlockquote(range.startContainer);
  if (bq) {
    var bqText = (bq.textContent || '').trim();
    var hasMedia = !!bq.querySelector('img, table');
    var isBqEmpty = !bqText && !hasMedia;

    if (e.key === 'Backspace') {
      if (isBqEmpty) {
        e.preventDefault();
        var prev = bq.previousElementSibling;
        while (prev && prev.classList.contains('doc-page-spacer')) {
          prev = prev.previousElementSibling;
        }
        if (prev) {
          bq.parentNode.removeChild(bq);
          var r = document.createRange();
          r.selectNodeContents(prev);
          r.collapse(false);
          sel.removeAllRanges();
          sel.addRange(r);
        } else {
          var p = document.createElement('p');
          p.innerHTML = '<br>';
          bq.parentNode.replaceChild(p, bq);
          var r = document.createRange();
          r.setStart(p, 0);
          r.collapse(true);
          sel.removeAllRanges();
          sel.addRange(r);
        }
        paginateDocument();
        notifyChange();
        scheduleScrollIntoView();
        return;
      } else if (range.startOffset === 0) {
        var walker = document.createTreeWalker(bq, NodeFilter.SHOW_TEXT, null, false);
        var firstText = walker.nextNode();
        if (!firstText || range.startContainer === firstText || range.startContainer === bq) {
          e.preventDefault();
          document.execCommand('formatBlock', false, '<p>');
          paginateDocument();
          notifyChange();
          scheduleScrollIntoView();
          return;
        }
      }
    } else if (e.key === 'Enter' && isBqEmpty) {
      e.preventDefault();
      var p = document.createElement('p');
      p.innerHTML = '<br>';
      bq.parentNode.replaceChild(p, bq);
      var r = document.createRange();
      r.setStart(p, 0);
      r.collapse(true);
      sel.removeAllRanges();
      sel.addRange(r);
      paginateDocument();
      notifyChange();
      scheduleScrollIntoView();
      return;
    }
  }
});

// Init
attachImageHandlers();
attachChecklistHandlers();
hydratePageBreaks();
normalizeAllBlockLineBoxes();
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
    onOpenGraph: () -> Unit = {},
    onRegisterInsertImage: (((Bitmap) -> Unit) -> Unit)? = null,
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

    // Toolbar UI state (synced per-character from active caret / selection, Google Docs style)
    var selectedStyle by remember { mutableStateOf(DocParagraphStyle.NORMAL) }
    var selectedFont by remember { mutableStateOf<DocFontFamily?>(DocFontFamily.INTER) }
    var fontSizePx by remember { mutableIntStateOf(11) }
    var activeTextColor by remember { mutableStateOf(Color(0xFF2563EB)) }
    var alignIndex by remember { mutableIntStateOf(0) }
    var lineSpacing by remember { mutableStateOf("1.15") }
    var spaceBeforePt by remember { mutableIntStateOf(0) }
    var spaceAfterPt by remember { mutableIntStateOf(0) }
    var activeIndentPx by remember { mutableIntStateOf(0) }
    var activeIndentMode by remember { mutableStateOf("none") }
    var isBoldActive by remember { mutableStateOf(false) }
    var isItalicActive by remember { mutableStateOf(false) }
    var isUnderlineActive by remember { mutableStateOf(false) }
    var isStrikeActive by remember { mutableStateOf(false) }
    var showWordCount by remember { mutableStateOf(false) }

    var showStyleDropdown by remember { mutableStateOf(false) }
    var showFontDropdown by remember { mutableStateOf(false) }
    var showFontSizeDropdown by remember { mutableStateOf(false) }
    var showColorDropdown by remember { mutableStateOf(false) }
    var showHighlightDropdown by remember { mutableStateOf(false) }
    var showSpacingDropdown by remember { mutableStateOf(false) }
    var showParagraphSpacingDialog by remember { mutableStateOf(false) }
    var showInsertDropdown by remember { mutableStateOf(false) }
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

    DisposableEffect(Unit) {
        val handler: (Bitmap) -> Unit = { bitmap ->
            scope.launch {
                val dataUri = withContext(Dispatchers.IO) {
                    val stream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    val base64 = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
                    "data:image/png;base64,$base64"
                }
                val quoted = JSONObject.quote(dataUri)
                js("insertImage($quoted);")
                Toast.makeText(context, "Grafik kartesius disisipkan ke dokumen", Toast.LENGTH_SHORT).show()
            }
        }
        onRegisterInsertImage?.invoke(handler)
        onDispose {
            onRegisterInsertImage?.invoke { }
        }
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
        // ── TOOLBAR RIBBON (Google Docs-style clean floating pill) ─────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = if (isDarkTheme) Color(0xFF1E202B) else Color(0xFFF0F4F9),
                border = BorderStroke(1.dp, LernspaceBorder.copy(alpha = 0.85f)),
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 10.dp, vertical = 4.dp),
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
                            Text(selectedFont?.label?.substringBefore(" (") ?: "Campuran", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = LernspaceTextPrimary)
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

                // Font Size Stepper + Quick Preset Picker (Google Docs pt scale, per-character)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                    DocToolbarIcon(Icons.Filled.Remove, "Kecilkan") {
                        val nextSize = if (fontSizePx <= 0) 10 else (fontSizePx - 1).coerceAtLeast(6)
                        fontSizePx = nextSize
                        js("setFontSize($nextSize);")
                    }
                    Box {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isDarkTheme) Color(0xFF2A2D3A) else Color.White,
                            border = BorderStroke(1.dp, LernspaceBorder),
                            modifier = Modifier
                                .size(width = 34.dp, height = 26.dp)
                                .clickable { showFontSizeDropdown = true }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    if (fontSizePx > 0) "$fontSizePx" else "–",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LernspaceTextPrimary
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = showFontSizeDropdown,
                            onDismissRequest = { showFontSizeDropdown = false }
                        ) {
                            listOf(8, 9, 10, 11, 12, 14, 16, 18, 20, 24, 28, 32, 36, 48, 72).forEach { sz ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "$sz",
                                            fontSize = 13.sp,
                                            fontWeight = if (sz == fontSizePx) FontWeight.Bold else FontWeight.Normal,
                                            color = if (sz == fontSizePx) AccentBlue else LernspaceTextPrimary
                                        )
                                    },
                                    onClick = {
                                        fontSizePx = sz
                                        showFontSizeDropdown = false
                                        js("setFontSize($sz);")
                                    }
                                )
                            }
                        }
                    }
                    DocToolbarIcon(Icons.Filled.Add, "Besarkan") {
                        val nextSize = if (fontSizePx <= 0) 12 else (fontSizePx + 1).coerceAtMost(72)
                        fontSizePx = nextSize
                        js("setFontSize($nextSize);")
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
                            Box(Modifier.size(width = 14.dp, height = 3.dp).background(activeTextColor))
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
                DocToolbarIcon(Icons.Filled.FormatClear, "Hapus Format") { js("clearFormatting();") }
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

                // Line Spacing & Per-Paragraph Indentation Dropdown
                Box {
                    DocToolbarIcon(Icons.Filled.FormatLineSpacing, "Spasi & Indentasi Paragraf") { showSpacingDropdown = true }
                    DropdownMenu(expanded = showSpacingDropdown, onDismissRequest = { showSpacingDropdown = false }) {
                        listOf(
                            "1.0" to "Single (1.0)",
                            "1.15" to "1.15 · Standar GDocs",
                            "1.5" to "1.5 · Makalah",
                            "2.0" to "Double (2.0 · Skripsi)"
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
                            text = {
                                Text(
                                    if (spaceBeforePt > 0) "Hapus spasi sebelum paragraf (${spaceBeforePt}pt)"
                                    else "Tambah spasi sebelum paragraf",
                                    fontSize = 12.5.sp,
                                    fontWeight = if (spaceBeforePt > 0) FontWeight.Bold else FontWeight.Normal,
                                    color = if (spaceBeforePt > 0) AccentBlue else LernspaceTextPrimary
                                )
                            },
                            onClick = {
                                js("toggleSpaceBeforeParagraph();")
                                showSpacingDropdown = false
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (spaceAfterPt > 0) "Hapus spasi setelah paragraf (${spaceAfterPt}pt)"
                                    else "Tambah spasi setelah paragraf",
                                    fontSize = 12.5.sp,
                                    fontWeight = if (spaceAfterPt > 0) FontWeight.Bold else FontWeight.Normal,
                                    color = if (spaceAfterPt > 0) AccentBlue else LernspaceTextPrimary
                                )
                            },
                            onClick = {
                                js("toggleSpaceAfterParagraph();")
                                showSpacingDropdown = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Spasi paragraf kustom...", fontSize = 12.5.sp, color = LernspaceTextPrimary) },
                            onClick = {
                                showSpacingDropdown = false
                                showParagraphSpacingDialog = true
                            }
                        )
                        HorizontalDivider()
                        listOf(
                            "none" to "Indentasi Normal (Tanpa Khusus)",
                            "first" to "Indentasi Baris Pertama (1.27 cm)",
                            "hanging" to "Indentasi Gantung (Daftar Pustaka)"
                        ).forEach { (mode, label) ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        label,
                                        fontSize = 12.5.sp,
                                        fontWeight = if (activeIndentMode == mode) FontWeight.Bold else FontWeight.Normal,
                                        color = if (activeIndentMode == mode) AccentBlue else LernspaceTextPrimary
                                    )
                                },
                                onClick = {
                                    activeIndentMode = mode
                                    js("setIndentMode(${JSONObject.quote(mode)});")
                                    showSpacingDropdown = false
                                }
                            )
                        }
                        if (activeIndentPx > 0 || activeIndentMode != "none") {
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Reset Indentasi Paragraf", fontSize = 12.5.sp, color = Color(0xFFDC2626)) },
                                onClick = {
                                    js("resetIndent();")
                                    showSpacingDropdown = false
                                }
                            )
                        }
                    }
                }

                DocToolbarIcon(
                    Icons.AutoMirrored.Filled.FormatIndentDecrease,
                    "Kurangi Indent",
                    enabled = activeIndentPx > 0 || activeIndentMode != "none"
                ) {
                    if (activeIndentPx <= 0 && activeIndentMode != "none") {
                        js("setIndentMode('none');")
                    } else {
                        js("adjustIndent(-36);")
                    }
                }
                DocToolbarIcon(Icons.AutoMirrored.Filled.FormatIndentIncrease, "Tambah Indent") { js("adjustIndent(36);") }
                DocToolbarDiv()

                // Lists
                DocToolbarIcon(Icons.AutoMirrored.Filled.FormatListBulleted, "Bullet List") { js("insertBulletList();") }
                DocToolbarIcon(Icons.Filled.FormatListNumbered, "Numbered List") { js("insertNumberedList();") }
                DocToolbarIcon(Icons.Filled.Checklist, "Checklist") { js("insertChecklist();") }
                DocToolbarIcon(Icons.Filled.FormatQuote, "Kutipan") { js("insertBlockquote();") }
                DocToolbarDiv()

                // Consolidated Insert Dropdown (+ Sisipkan ▾)
                Box {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (showInsertDropdown) AccentBlue.copy(alpha = 0.12f) else Color.Transparent,
                        modifier = Modifier
                            .height(30.dp)
                            .clickable { showInsertDropdown = true }
                            .padding(horizontal = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Icon(Icons.Filled.Add, "Sisipkan", Modifier.size(16.dp), tint = if (showInsertDropdown) AccentBlue else LernspaceTextPrimary)
                            Text("Sisipkan", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = if (showInsertDropdown) AccentBlue else LernspaceTextPrimary)
                            Icon(Icons.Filled.ArrowDropDown, null, Modifier.size(15.dp), tint = LernspaceTextSecondary)
                        }
                    }
                    DropdownMenu(expanded = showInsertDropdown, onDismissRequest = { showInsertDropdown = false }) {
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Filled.Image, null, Modifier.size(18.dp), tint = AccentBlue) },
                            text = { Text("Gambar", fontSize = 13.sp) },
                            onClick = {
                                showInsertDropdown = false
                                isReplacingImage = false
                                imagePickerLauncher.launch("image/*")
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Filled.TableChart, null, Modifier.size(18.dp), tint = Color(0xFF059669)) },
                            text = { Text("Tabel", fontSize = 13.sp) },
                            onClick = { showInsertDropdown = false; showTableDialog = true }
                        )
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Filled.Link, null, Modifier.size(18.dp), tint = Color(0xFF2563EB)) },
                            text = { Text("Tautan (Link)", fontSize = 13.sp) },
                            onClick = { showInsertDropdown = false; showLinkDialog = true }
                        )
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Filled.Functions, null, Modifier.size(18.dp), tint = Color(0xFF7C3AED)) },
                            text = { Text("Rumus Matematika", fontSize = 13.sp) },
                            onClick = { showInsertDropdown = false; showMathDialog = true }
                        )
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Filled.ShowChart, null, Modifier.size(18.dp), tint = Color(0xFF0284C7)) },
                            text = { Text("Grafik Kartesius", fontSize = 13.sp) },
                            onClick = { showInsertDropdown = false; onOpenGraph() }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Filled.HorizontalRule, null, Modifier.size(18.dp), tint = LernspaceTextSecondary) },
                            text = { Text("Garis Pemisah", fontSize = 13.sp) },
                            onClick = { showInsertDropdown = false; js("insertHR();") }
                        )
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Filled.InsertPageBreak, null, Modifier.size(18.dp), tint = LernspaceTextSecondary) },
                            text = { Text("Pemisah Halaman", fontSize = 13.sp) },
                            onClick = { showInsertDropdown = false; js("insertPageBreak();") }
                        )
                    }
                }
                DocToolbarDiv()

                // Quick Paper Format Chip (1-Tap Page Setup!)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDarkTheme) Color(0xFF2A2D3A) else Color.White,
                    border = BorderStroke(1.dp, LernspaceBorder),
                    modifier = Modifier
                        .height(28.dp)
                        .clickable { showPageSetupDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp),
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
                        shape = RoundedCornerShape(14.dp),
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

                // Mimo AI Summary Quick Action (Cheat Sheet is already in top workspace bar)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = AccentBlue.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.28f)),
                    modifier = Modifier.height(28.dp).clickable(onClick = onTriggerSummary)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("📝", fontSize = 11.sp)
                        Text("Rangkum", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = AccentBlue)
                    }
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
                    onFormatStateChanged = { b, i, u, s, align, fsPx, fontName, blkTag, colorHex, lsStr, indPx, indMode, beforePt, afterPt ->
                        isBoldActive = b
                        isItalicActive = i
                        isUnderlineActive = u
                        isStrikeActive = s
                        alignIndex = align.coerceIn(0, 3)
                        fontSizePx = fsPx
                        selectedFont = if (fontName.isBlank()) null else {
                            DocFontFamily.entries.firstOrNull { it.name == fontName } ?: DocFontFamily.INTER
                        }
                        selectedStyle = DocParagraphStyle.entries.firstOrNull { it.htmlTag == blkTag } ?: DocParagraphStyle.NORMAL
                        runCatching { Color(android.graphics.Color.parseColor(colorHex)) }
                            .onSuccess { activeTextColor = it }
                        lineSpacing = lsStr
                        activeIndentPx = indPx
                        activeIndentMode = indMode
                        spaceBeforePt = beforePt
                        spaceAfterPt = afterPt
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
                        textZoom = 100
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

    // ── PARAGRAPH SPACING DIALOG ──────────────────────────────────────────
    if (showParagraphSpacingDialog) {
        ParagraphSpacingDialog(
            initialBeforePt = spaceBeforePt,
            initialAfterPt = spaceAfterPt,
            onDismiss = { showParagraphSpacingDialog = false },
            onApply = { beforePt, afterPt ->
                spaceBeforePt = beforePt
                spaceAfterPt = afterPt
                js("setParagraphSpacing($beforePt, $afterPt);")
                showParagraphSpacingDialog = false
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
fun ParagraphSpacingDialog(
    initialBeforePt: Int,
    initialAfterPt: Int,
    onDismiss: () -> Unit,
    onApply: (beforePt: Int, afterPt: Int) -> Unit
) {
    var beforePt by remember { mutableIntStateOf(initialBeforePt.coerceIn(0, 48)) }
    var afterPt by remember { mutableIntStateOf(initialAfterPt.coerceIn(0, 48)) }

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
                    Icon(Icons.Filled.FormatLineSpacing, null, tint = AccentBlue)
                    Text("Spasi Antar Paragraf", style = MaterialTheme.typography.titleMedium, color = LernspaceDark)
                }

                // Quick presets
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(
                        "Rapat (0pt)" to (0 to 0),
                        "Sedang (6pt)" to (0 to 6),
                        "GDocs (10pt)" to (0 to 10),
                        "Lega (12pt)" to (6 to 12)
                    ).forEach { (label, pair) ->
                        val active = beforePt == pair.first && afterPt == pair.second
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (active) AccentBlue.copy(0.14f) else Color.Transparent,
                            border = BorderStroke(1.dp, if (active) AccentBlue else LernspaceBorder),
                            modifier = Modifier.weight(1f).clickable {
                                beforePt = pair.first
                                afterPt = pair.second
                            }
                        ) {
                            Box(Modifier.padding(vertical = 6.dp, horizontal = 4.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    label,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                    color = if (active) AccentBlue else LernspaceTextPrimary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text("Sebelum paragraf (pt)", fontSize = 13.5.sp, fontWeight = FontWeight.Medium, color = LernspaceDark)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        IconButton(onClick = { beforePt = (beforePt - 2).coerceAtLeast(0) }, Modifier.size(32.dp)) {
                            Icon(Icons.Filled.Remove, null, Modifier.size(16.dp))
                        }
                        Text("${beforePt}pt", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LernspaceDark)
                        IconButton(onClick = { beforePt = (beforePt + 2).coerceAtMost(48) }, Modifier.size(32.dp)) {
                            Icon(Icons.Filled.Add, null, Modifier.size(16.dp))
                        }
                    }
                }

                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text("Setelah paragraf (pt)", fontSize = 13.5.sp, fontWeight = FontWeight.Medium, color = LernspaceDark)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        IconButton(onClick = { afterPt = (afterPt - 2).coerceAtLeast(0) }, Modifier.size(32.dp)) {
                            Icon(Icons.Filled.Remove, null, Modifier.size(16.dp))
                        }
                        Text("${afterPt}pt", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LernspaceDark)
                        IconButton(onClick = { afterPt = (afterPt + 2).coerceAtMost(48) }, Modifier.size(32.dp)) {
                            Icon(Icons.Filled.Add, null, Modifier.size(16.dp))
                        }
                    }
                }

                Row(Modifier.fillMaxWidth(), Arrangement.End, Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) { Text("Batal", color = LernspaceTextSecondary) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onApply(beforePt, afterPt) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                    ) {
                        Text("Terapkan", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

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
    Box(
        Modifier
            .padding(horizontal = 3.dp)
            .height(18.dp)
            .width(1.dp)
            .background(LernspaceBorder.copy(alpha = 0.65f))
    )
}
