package com.lernspace.app.ui.workspace.document

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.lernspace.app.ui.theme.*

// ── ENUMS ────────────────────────────────────────────────────────────────────
enum class DocParagraphStyle(val label: String, val htmlTag: String) {
    NORMAL("Teks Normal", "p"),
    TITLE("Judul Dokumen", "h1"),
    HEADING_1("Heading 1 (Besar)", "h2"),
    HEADING_2("Heading 2 (Sedang)", "h3"),
    HEADING_3("Heading 3 (Kecil)", "h4")
}

enum class DocFontFamily(val label: String, val cssValue: String, val composeFont: FontFamily) {
    DEFAULT("Calibri / Sans", "Inter, system-ui, sans-serif", FontFamily.SansSerif),
    SERIF("Merriweather / Serif", "Georgia, 'Times New Roman', serif", FontFamily.Serif),
    MONO("JetBrains / Mono", "'Courier New', Courier, monospace", FontFamily.Monospace)
}

// ── WEBVIEW BRIDGE ────────────────────────────────────────────────────────────
class DocumentJsBridge(
    private val onContentChanged: (String) -> Unit,
    private val onWordCountChanged: (Int, Int) -> Unit
) {
    @JavascriptInterface
    fun onContentChange(html: String) {
        onContentChanged(html)
        // Count words from plain text
        val plain = html.replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ").trim()
        val words = if (plain.isBlank()) 0 else plain.split(" ").filter { it.isNotBlank() }.size
        onWordCountChanged(words, plain.length)
    }
}

// ── WEBVIEW DOCUMENT EDITOR ───────────────────────────────────────────────────
@SuppressLint("SetJavaScriptEnabled")
fun buildDocumentWebView(
    context: Context,
    isDarkTheme: Boolean,
    initialHtml: String,
    bridge: DocumentJsBridge
): WebView {
    return WebView(context).apply {
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            loadWithOverviewMode = true
            useWideViewPort = true
            cacheMode = WebSettings.LOAD_NO_CACHE
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
        }
        addJavascriptInterface(bridge, "LernspaceDoc")
        webChromeClient = WebChromeClient()
        webViewClient = WebViewClient()
        isHorizontalScrollBarEnabled = false
        isVerticalScrollBarEnabled = true
        setBackgroundColor(0x00000000)
        isNestedScrollingEnabled = false
    }
}

// ── GENERATE EDITOR HTML PAGE ─────────────────────────────────────────────────
fun generateEditorHtml(isDarkTheme: Boolean, initialContent: String): String {
    val bg = if (isDarkTheme) "#1E202B" else "#FFFFFF"
    val textColor = if (isDarkTheme) "#F1F5F9" else "#1E293B"
    val borderColor = if (isDarkTheme) "#334155" else "#E2E8F0"
    val selectionColor = if (isDarkTheme) "#2563EB33" else "#2563EB22"
    val tableBorder = if (isDarkTheme) "#475569" else "#CBD5E1"
    val tableHeaderBg = if (isDarkTheme) "#1E293B" else "#F8FAFC"
    val placeholderColor = if (isDarkTheme) "#64748B" else "#94A3B8"
    val linkColor = if (isDarkTheme) "#60A5FA" else "#2563EB"

    val safeContent = if (initialContent.isBlank()) "<p><br></p>" else initialContent

    return """
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; }
  
  html, body {
    background: transparent;
    font-family: Inter, system-ui, sans-serif;
    -webkit-text-size-adjust: 100%;
    height: 100%;
  }

  #editor {
    min-height: 100vh;
    padding: 0 0 120px 0;
    outline: none;
    caret-color: #2563EB;
    color: $textColor;
    font-size: 15px;
    line-height: 1.75;
    word-wrap: break-word;
    overflow-wrap: break-word;
  }

  #editor:empty::before {
    content: 'Mulai mengetik dokumen di sini...';
    color: $placeholderColor;
    font-style: italic;
    pointer-events: none;
  }
  
  #editor p { margin: 0 0 8px 0; min-height: 1.75em; }
  #editor h1 { font-size: 26px; font-weight: 700; margin: 16px 0 8px 0; color: $textColor; }
  #editor h2 { font-size: 21px; font-weight: 700; margin: 14px 0 6px 0; color: $textColor; }
  #editor h3 { font-size: 18px; font-weight: 600; margin: 12px 0 5px 0; color: $textColor; }
  #editor h4 { font-size: 16px; font-weight: 600; margin: 10px 0 4px 0; color: $textColor; }
  #editor strong { font-weight: 700; }
  #editor em { font-style: italic; }
  #editor u { text-decoration: underline; }
  #editor s { text-decoration: line-through; }
  
  /* Links */
  #editor a { color: $linkColor; text-decoration: underline; cursor: pointer; }
  
  /* Lists */
  #editor ul {
    list-style-type: disc;
    margin: 6px 0 10px 0;
    padding-left: 28px;
  }
  #editor ol {
    list-style-type: decimal;
    margin: 6px 0 10px 0;
    padding-left: 28px;
  }
  #editor li {
    margin: 3px 0;
    line-height: 1.65;
  }
  
  /* Checklist */
  #editor ul.checklist { list-style: none; padding-left: 6px; }
  #editor ul.checklist li { display: flex; align-items: flex-start; gap: 8px; margin: 4px 0; }
  #editor ul.checklist li input[type="checkbox"] {
    margin-top: 3px;
    width: 16px;
    height: 16px;
    accent-color: #2563EB;
    cursor: pointer;
    flex-shrink: 0;
  }
  #editor ul.checklist li.done span { text-decoration: line-through; opacity: 0.55; }
  
  /* Blockquote / callout */
  #editor blockquote {
    border-left: 3px solid #2563EB;
    margin: 10px 0;
    padding: 8px 16px;
    background: ${if (isDarkTheme) "#1E293B" else "#EFF6FF"};
    border-radius: 0 6px 6px 0;
    color: ${if (isDarkTheme) "#93C5FD" else "#1D4ED8"};
    font-style: italic;
  }
  
  /* Horizontal rule */
  #editor hr {
    border: none;
    border-top: 1.5px solid $borderColor;
    margin: 16px 0;
  }
  
  /* Code block */
  #editor pre {
    background: ${if (isDarkTheme) "#0F172A" else "#F1F5F9"};
    border: 1px solid $borderColor;
    border-radius: 6px;
    padding: 12px 16px;
    font-family: 'Courier New', monospace;
    font-size: 13px;
    overflow-x: auto;
    margin: 10px 0;
    color: ${if (isDarkTheme) "#7DD3FC" else "#0F172A"};
  }

  /* ── TABLES ── */
  #editor table {
    border-collapse: collapse;
    width: 100%;
    margin: 12px 0;
    font-size: 14px;
    table-layout: fixed;
  }
  #editor table td, #editor table th {
    border: 1.5px solid $tableBorder;
    padding: 8px 12px;
    min-width: 60px;
    min-height: 36px;
    vertical-align: top;
    outline: none;
    word-wrap: break-word;
    line-height: 1.6;
  }
  #editor table th {
    background: $tableHeaderBg;
    font-weight: 600;
    text-align: left;
  }
  #editor table td:focus, #editor table th:focus {
    outline: 2px solid #2563EB;
    outline-offset: -2px;
  }
  
  /* Table toolbar popup */
  #table-toolbar {
    display: none;
    position: fixed;
    background: ${if (isDarkTheme) "#1E202B" else "#FFFFFF"};
    border: 1.5px solid $borderColor;
    border-radius: 8px;
    box-shadow: 0 4px 16px rgba(0,0,0,0.18);
    padding: 4px 8px;
    gap: 4px;
    flex-direction: row;
    align-items: center;
    z-index: 9999;
    flex-wrap: wrap;
    max-width: 280px;
  }
  #table-toolbar button {
    background: transparent;
    border: none;
    border-radius: 4px;
    padding: 4px 8px;
    font-size: 11px;
    font-weight: 600;
    color: $textColor;
    cursor: pointer;
    white-space: nowrap;
  }
  #table-toolbar button:active { background: ${if (isDarkTheme) "#334155" else "#EFF6FF"}; }
  #table-toolbar .sep {
    width: 1px; height: 18px;
    background: $borderColor;
    margin: 0 2px;
  }

  /* Images */
  #editor img {
    max-width: 100%;
    height: auto;
    display: block;
    margin: 10px auto;
    border-radius: 6px;
    border: 1.5px solid $borderColor;
  }
  #editor img.selected { outline: 2px solid #2563EB; outline-offset: 2px; }

  /* Selection color */
  ::selection { background: $selectionColor; }

</style>
</head>
<body>
<div id="table-toolbar">
  <button onclick="addRowAbove()">↑ Baris di atas</button>
  <button onclick="addRowBelow()">↓ Baris di bawah</button>
  <div class="sep"></div>
  <button onclick="addColLeft()">← Kolom kiri</button>
  <button onclick="addColRight()">→ Kolom kanan</button>
  <div class="sep"></div>
  <button onclick="deleteCurrentRow()" style="color:#DC2626">✕ Hapus Baris</button>
  <button onclick="deleteCurrentCol()" style="color:#DC2626">✕ Hapus Kolom</button>
  <div class="sep"></div>
  <button onclick="deleteTable()" style="color:#DC2626">🗑 Hapus Tabel</button>
</div>

<div id="editor" contenteditable="true">$safeContent</div>

<script>
var editor = document.getElementById('editor');
var tableToolbar = document.getElementById('table-toolbar');
var debounceTimer = null;
var currentCell = null;

// ── Notify Kotlin on content change ──
function notifyChange() {
  clearTimeout(debounceTimer);
  debounceTimer = setTimeout(function() {
    var html = editor.innerHTML;
    LernspaceDoc.onContentChange(html);
  }, 400);
}
editor.addEventListener('input', notifyChange);
editor.addEventListener('keyup', notifyChange);

// ── EXEC COMMAND WRAPPERS ──
function exec(cmd, val) {
  editor.focus();
  document.execCommand(cmd, false, val || null);
  notifyChange();
}

function formatBlock(tag) { exec('formatBlock', tag); }
function toggleBold() { exec('bold'); }
function toggleItalic() { exec('italic'); }
function toggleUnderline() { exec('underline'); }
function toggleStrikethrough() { exec('strikeThrough'); }
function setFontFamily(css) { exec('fontName', css); }
function setFontSize(px) {
  // execCommand fontSize is limited (1-7), use span instead
  var sel = window.getSelection();
  if (!sel || sel.rangeCount === 0) return;
  var range = sel.getRangeAt(0);
  if (range.collapsed) return;
  var span = document.createElement('span');
  span.style.fontSize = px + 'px';
  range.surroundContents(span);
  notifyChange();
}
function setTextColor(color) { exec('foreColor', color); }
function setHighlight(color) { exec('hiliteColor', color); }
function alignLeft() { exec('justifyLeft'); }
function alignCenter() { exec('justifyCenter'); }
function alignRight() { exec('justifyRight'); }
function alignJustify() { exec('justifyFull'); }

function undo() { exec('undo'); }
function redo() { exec('redo'); }

// ── LISTS ──
function insertBulletList() {
  exec('insertUnorderedList');
}
function insertNumberedList() {
  exec('insertOrderedList');
}
function insertChecklist() {
  editor.focus();
  var sel = window.getSelection();
  if (!sel || sel.rangeCount === 0) return;
  var range = sel.getRangeAt(0);
  var ul = document.createElement('ul');
  ul.className = 'checklist';
  var li = document.createElement('li');
  var cb = document.createElement('input');
  cb.type = 'checkbox';
  cb.addEventListener('change', function() {
    li.classList.toggle('done', cb.checked);
    notifyChange();
  });
  var span = document.createElement('span');
  span.textContent = sel.toString() || 'Item';
  li.appendChild(cb);
  li.appendChild(span);
  ul.appendChild(li);
  range.deleteContents();
  range.insertNode(ul);
  // Move cursor to span
  var newRange = document.createRange();
  newRange.setStart(span, 0);
  newRange.collapse(true);
  sel.removeAllRanges();
  sel.addRange(newRange);
  notifyChange();
}

function insertBlockquote() {
  exec('formatBlock', 'blockquote');
}
function insertHorizontalRule() {
  exec('insertHorizontalRule');
}

// ── LINK ──
function insertLink(text, url) {
  editor.focus();
  var sel = window.getSelection();
  if (!sel || sel.rangeCount === 0) return;
  var range = sel.getRangeAt(0);
  var a = document.createElement('a');
  a.href = url;
  a.textContent = text;
  range.deleteContents();
  range.insertNode(a);
  var newRange = document.createRange();
  newRange.setStartAfter(a);
  newRange.collapse(true);
  sel.removeAllRanges();
  sel.addRange(newRange);
  notifyChange();
}

// ── MATH EQUATION ──
function insertMath(latex) {
  editor.focus();
  var sel = window.getSelection();
  if (!sel || sel.rangeCount === 0) return;
  var range = sel.getRangeAt(0);
  var span = document.createElement('span');
  span.className = 'math-inline';
  span.style.cssText = 'font-family: Courier New, monospace; color: #1D4ED8; background: #EFF6FF; padding: 2px 6px; border-radius: 4px; font-size: 14px;';
  span.textContent = latex;
  span.setAttribute('data-latex', latex);
  range.deleteContents();
  range.insertNode(span);
  var newRange = document.createRange();
  newRange.setStartAfter(span);
  newRange.collapse(true);
  sel.removeAllRanges();
  sel.addRange(newRange);
  notifyChange();
}

// ── IMAGE ──
function insertImage(dataUri) {
  editor.focus();
  var sel = window.getSelection();
  if (!sel || sel.rangeCount === 0) return;
  var range = sel.getRangeAt(0);
  var img = document.createElement('img');
  img.src = dataUri;
  img.style.maxWidth = '100%';
  range.insertNode(img);
  var newRange = document.createRange();
  newRange.setStartAfter(img);
  newRange.collapse(true);
  sel.removeAllRanges();
  sel.addRange(newRange);
  notifyChange();
}

// ── TABLE INSERTION ──
function insertTable(rows, cols) {
  editor.focus();
  var table = document.createElement('table');
  for (var r = 0; r < rows; r++) {
    var tr = document.createElement('tr');
    for (var c = 0; c < cols; c++) {
      var cell = (r === 0) ? document.createElement('th') : document.createElement('td');
      cell.contentEditable = 'true';
      cell.textContent = (r === 0) ? 'Kolom ' + (c + 1) : '';
      tr.appendChild(cell);
    }
    table.appendChild(tr);
  }
  var sel = window.getSelection();
  if (sel && sel.rangeCount > 0) {
    var range = sel.getRangeAt(0);
    range.deleteContents();
    range.insertNode(table);
    var br = document.createElement('p');
    br.innerHTML = '<br>';
    table.parentNode.insertBefore(br, table.nextSibling);
  } else {
    editor.appendChild(table);
  }
  notifyChange();
}

// ── TABLE TOOLBAR ──
var currentRow = null;
var currentTable = null;

function getParentCell(node) {
  while (node) {
    if (node.tagName === 'TD' || node.tagName === 'TH') return node;
    node = node.parentNode;
  }
  return null;
}
function getParentTable(node) {
  while (node) {
    if (node.tagName === 'TABLE') return node;
    node = node.parentNode;
  }
  return null;
}

editor.addEventListener('click', function(e) {
  var cell = getParentCell(e.target);
  if (cell) {
    currentCell = cell;
    currentRow = cell.parentNode;
    currentTable = getParentTable(cell);
    // Position toolbar
    var rect = cell.getBoundingClientRect();
    tableToolbar.style.display = 'flex';
    tableToolbar.style.top = (rect.bottom + 6) + 'px';
    tableToolbar.style.left = Math.max(4, rect.left) + 'px';
  } else {
    tableToolbar.style.display = 'none';
    currentCell = null;
  }
});

document.addEventListener('click', function(e) {
  if (!tableToolbar.contains(e.target) && !getParentCell(e.target)) {
    tableToolbar.style.display = 'none';
  }
});

function addRowBelow() {
  if (!currentTable || !currentRow) return;
  var newRow = document.createElement('tr');
  var colCount = currentTable.rows[0].cells.length;
  for (var i = 0; i < colCount; i++) {
    var td = document.createElement('td');
    td.contentEditable = 'true';
    newRow.appendChild(td);
  }
  currentRow.parentNode.insertBefore(newRow, currentRow.nextSibling);
  notifyChange();
}

function addRowAbove() {
  if (!currentTable || !currentRow) return;
  var newRow = document.createElement('tr');
  var colCount = currentTable.rows[0].cells.length;
  for (var i = 0; i < colCount; i++) {
    var td = document.createElement('td');
    td.contentEditable = 'true';
    newRow.appendChild(td);
  }
  currentRow.parentNode.insertBefore(newRow, currentRow);
  notifyChange();
}

function addColRight() {
  if (!currentTable || !currentCell) return;
  var colIdx = currentCell.cellIndex;
  for (var r = 0; r < currentTable.rows.length; r++) {
    var row = currentTable.rows[r];
    var newCell = (r === 0) ? document.createElement('th') : document.createElement('td');
    newCell.contentEditable = 'true';
    if (r === 0) newCell.textContent = 'Kolom';
    if (colIdx + 1 < row.cells.length) {
      row.insertBefore(newCell, row.cells[colIdx + 1]);
    } else {
      row.appendChild(newCell);
    }
  }
  notifyChange();
}

function addColLeft() {
  if (!currentTable || !currentCell) return;
  var colIdx = currentCell.cellIndex;
  for (var r = 0; r < currentTable.rows.length; r++) {
    var row = currentTable.rows[r];
    var newCell = (r === 0) ? document.createElement('th') : document.createElement('td');
    newCell.contentEditable = 'true';
    if (r === 0) newCell.textContent = 'Kolom';
    row.insertBefore(newCell, row.cells[colIdx]);
  }
  notifyChange();
}

function deleteCurrentRow() {
  if (!currentRow) return;
  currentRow.parentNode.removeChild(currentRow);
  tableToolbar.style.display = 'none';
  currentRow = null;
  currentCell = null;
  notifyChange();
}

function deleteCurrentCol() {
  if (!currentTable || !currentCell) return;
  var colIdx = currentCell.cellIndex;
  for (var r = 0; r < currentTable.rows.length; r++) {
    var row = currentTable.rows[r];
    if (colIdx < row.cells.length) {
      row.removeChild(row.cells[colIdx]);
    }
  }
  tableToolbar.style.display = 'none';
  currentCell = null;
  notifyChange();
}

function deleteTable() {
  if (!currentTable) return;
  currentTable.parentNode.removeChild(currentTable);
  tableToolbar.style.display = 'none';
  currentTable = null;
  currentRow = null;
  currentCell = null;
  notifyChange();
}

// ── SET CONTENT (from Kotlin) ──
function setContent(html) {
  editor.innerHTML = html || '<p><br></p>';
  notifyChange();
}

// ── GET CURRENT SELECTION TEXT ──
function getSelectedText() {
  var sel = window.getSelection();
  return sel ? sel.toString() : '';
}

// ── CLEAR FORMATTING ──
function clearFormatting() { exec('removeFormat'); }

// ── Keep focus ──
editor.focus();
</script>
</body>
</html>
""".trimIndent()
}

// ── MAIN DOCUMENT EDITOR VIEWPORT COMPOSABLE ──────────────────────────────────
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

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var wordCount by remember { mutableIntStateOf(0) }
    var charCount by remember { mutableIntStateOf(0) }
    var lastLoadedContent by remember { mutableStateOf("") }
    var isWebViewReady by remember { mutableStateOf(false) }

    // Toolbar dropdowns
    var selectedStyle by remember { mutableStateOf(DocParagraphStyle.NORMAL) }
    var selectedFont by remember { mutableStateOf(DocFontFamily.DEFAULT) }
    var fontSizeSp by remember { mutableIntStateOf(15) }
    var showStyleDropdown by remember { mutableStateOf(false) }
    var showFontDropdown by remember { mutableStateOf(false) }
    var showColorDropdown by remember { mutableStateOf(false) }
    var showHighlightDropdown by remember { mutableStateOf(false) }
    var showTableDialog by remember { mutableStateOf(false) }

    // Dialogs
    var showLinkDialog by remember { mutableStateOf(false) }
    var showMathDialog by remember { mutableStateOf(false) }

    // JS executor helper
    fun js(script: String) {
        webViewRef?.evaluateJavascript(script, null)
    }

    // Image picker – encode to base64 data URI so WebView can render it natively
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()
                if (bytes != null) {
                    val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                    val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    val dataUri = "data:$mimeType;base64,$b64"
                    // Escape for JS string
                    js("insertImage('$dataUri');")
                    Toast.makeText(context, "Gambar berhasil disisipkan", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal memuat gambar: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("DocEditor", "Image load failed", e)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDarkTheme) Color(0xFF13141C) else Color(0xFFF1F5F9))
    ) {
        // ── TOOLBAR ────────────────────────────────────────────────────────
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
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                // Undo / Redo / Print
                DocToolbarIcon(Icons.AutoMirrored.Filled.Undo, "Undo") { js("undo();") }
                DocToolbarIcon(Icons.AutoMirrored.Filled.Redo, "Redo") { js("redo();") }
                DocToolbarIcon(Icons.Filled.Print, "Print/Export PDF", onClick = onExportPdf)

                DocToolbarDiv()

                // Paragraph Style
                Box {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Transparent,
                        modifier = Modifier.height(32.dp).clickable { showStyleDropdown = true }.padding(horizontal = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(selectedStyle.label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = LernspaceTextPrimary)
                            Icon(Icons.Filled.ArrowDropDown, null, Modifier.size(16.dp), tint = LernspaceTextSecondary)
                        }
                    }
                    DropdownMenu(expanded = showStyleDropdown, onDismissRequest = { showStyleDropdown = false }) {
                        DocParagraphStyle.entries.forEach { style ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        style.label,
                                        fontWeight = if (style == DocParagraphStyle.TITLE || style == DocParagraphStyle.HEADING_1) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    )
                                },
                                onClick = {
                                    selectedStyle = style
                                    showStyleDropdown = false
                                    js("formatBlock('<${style.htmlTag}>');")
                                }
                            )
                        }
                    }
                }

                DocToolbarDiv()

                // Font Family
                Box {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Transparent,
                        modifier = Modifier.height(32.dp).clickable { showFontDropdown = true }.padding(horizontal = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(selectedFont.label.substringBefore(" /"), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = LernspaceTextPrimary)
                            Icon(Icons.Filled.ArrowDropDown, null, Modifier.size(16.dp), tint = LernspaceTextSecondary)
                        }
                    }
                    DropdownMenu(expanded = showFontDropdown, onDismissRequest = { showFontDropdown = false }) {
                        DocFontFamily.entries.forEach { font ->
                            DropdownMenuItem(
                                text = { Text(font.label, fontSize = 13.sp) },
                                onClick = {
                                    selectedFont = font
                                    showFontDropdown = false
                                    val safeCss = font.cssValue.replace("'", "\\'")
                                    js("setFontFamily('$safeCss');")
                                }
                            )
                        }
                    }
                }

                DocToolbarDiv()

                // Font Size Stepper
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    DocToolbarIcon(Icons.Filled.Remove, "Kecilkan") {
                        if (fontSizeSp > 10) { fontSizeSp--; js("setFontSize($fontSizeSp);") }
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isDarkTheme) Color(0xFF2A2D3A) else Color.White,
                        border = BorderStroke(1.dp, LernspaceBorder),
                        modifier = Modifier.size(width = 32.dp, height = 26.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("$fontSizeSp", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LernspaceTextPrimary)
                        }
                    }
                    DocToolbarIcon(Icons.Filled.Add, "Besarkan") {
                        if (fontSizeSp < 48) { fontSizeSp++; js("setFontSize($fontSizeSp);") }
                    }
                }

                DocToolbarDiv()

                // Bold / Italic / Underline / Strikethrough
                DocToolbarTextBtn("B", FontWeight.Bold, tooltip = "Tebal") { js("toggleBold();") }
                DocToolbarTextBtn("I", tooltip = "Miring") { js("toggleItalic();") }
                DocToolbarTextBtn("U", tooltip = "Garis Bawah") { js("toggleUnderline();") }
                DocToolbarTextBtn("S", tooltip = "Coret") { js("toggleStrikethrough();") }

                // Text Color
                Box {
                    IconButton(onClick = { showColorDropdown = true }, modifier = Modifier.size(30.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text("A", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = LernspaceTextPrimary)
                            Box(Modifier.size(width = 14.dp, height = 3.dp).background(Color(0xFF2563EB)))
                        }
                    }
                    DropdownMenu(expanded = showColorDropdown, onDismissRequest = { showColorDropdown = false }) {
                        listOf("Hitam" to "#1E293B", "Biru" to "#2563EB", "Merah" to "#DC2626",
                            "Hijau" to "#059669", "Ungu" to "#7C3AED", "Oranye" to "#EA580C").forEach { (name, hex) ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(Modifier.size(14.dp).clip(CircleShape).background(Color(android.graphics.Color.parseColor(hex))))
                                        Text(name, fontSize = 12.sp)
                                    }
                                },
                                onClick = { js("setTextColor('$hex');"); showColorDropdown = false }
                            )
                        }
                    }
                }

                // Highlighter
                Box {
                    DocToolbarIcon(Icons.Filled.BorderColor, "Sorotan") { showHighlightDropdown = true }
                    DropdownMenu(expanded = showHighlightDropdown, onDismissRequest = { showHighlightDropdown = false }) {
                        listOf("Kuning" to "#FEF08A", "Hijau Muda" to "#BBF7D0",
                            "Biru Muda" to "#BFDBFE", "Pink Muda" to "#FBCFE8", "Hapus Sorotan" to "").forEach { (name, hex) ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (hex.isNotEmpty()) Box(Modifier.size(14.dp).clip(RoundedCornerShape(2.dp)).background(Color(android.graphics.Color.parseColor(hex))))
                                        Text(name, fontSize = 12.sp)
                                    }
                                },
                                onClick = {
                                    if (hex.isNotEmpty()) js("setHighlight('$hex');") else js("setHighlight(false);")
                                    showHighlightDropdown = false
                                }
                            )
                        }
                    }
                }

                DocToolbarDiv()

                // Alignment
                DocToolbarIcon(Icons.Filled.FormatAlignLeft, "Rata Kiri") { js("alignLeft();") }
                DocToolbarIcon(Icons.Filled.FormatAlignCenter, "Rata Tengah") { js("alignCenter();") }
                DocToolbarIcon(Icons.Filled.FormatAlignRight, "Rata Kanan") { js("alignRight();") }
                DocToolbarIcon(Icons.Filled.FormatAlignJustify, "Rata Penuh") { js("alignJustify();") }

                DocToolbarDiv()

                // Lists
                DocToolbarIcon(Icons.AutoMirrored.Filled.FormatListBulleted, "Daftar Poin") { js("insertBulletList();") }
                DocToolbarIcon(Icons.Filled.FormatListNumbered, "Daftar Angka") { js("insertNumberedList();") }
                DocToolbarIcon(Icons.Filled.Checklist, "Checklist") { js("insertChecklist();") }
                DocToolbarIcon(Icons.Filled.FormatQuote, "Kutipan") { js("insertBlockquote();") }

                DocToolbarDiv()

                // Insert: Link, Math, Image, Table, HR
                DocToolbarIcon(Icons.Filled.Link, "Sisipkan Tautan") { showLinkDialog = true }
                DocToolbarIcon(Icons.Filled.Functions, "Rumus Matematika") { showMathDialog = true }
                DocToolbarIcon(Icons.Filled.Image, "Sisipkan Gambar") { imagePickerLauncher.launch("image/*") }
                DocToolbarIcon(Icons.Filled.TableChart, "Sisipkan Tabel") { showTableDialog = true }
                DocToolbarIcon(Icons.Filled.HorizontalRule, "Garis Pembatas") { js("insertHorizontalRule();") }
                DocToolbarIcon(Icons.Filled.FormatClear, "Hapus Format") { js("clearFormatting();") }

                DocToolbarDiv()

                // Mimo AI
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF8B5CF6).copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.35f)),
                    modifier = Modifier.height(30.dp).clickable(onClick = onTriggerCheatSheet)
                ) {
                    Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
                    Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("📝", fontSize = 11.sp)
                        Text("Rangkum", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentBlue)
                    }
                }
            }
        }

        // ── DOCUMENT PAGE (CENTERED A4 WHITE SHEET) ────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .background(if (isDarkTheme) Color(0xFF13141C) else Color(0xFFF1F5F9))
                .padding(vertical = 20.dp, horizontal = 16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 840.dp)
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 1100.dp)
                    .shadow(elevation = 3.dp, shape = RoundedCornerShape(4.dp)),
                shape = RoundedCornerShape(4.dp),
                color = if (isDarkTheme) Color(0xFF1E202B) else Color.White,
                border = BorderStroke(1.dp, LernspaceBorder)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 48.dp, vertical = 44.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header meta row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = AccentBlue.copy(alpha = 0.10f),
                            border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.25f))
                        ) {
                            Text(
                                "📄 $subject · Dokumen Teks",
                                fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentBlue,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("$wordCount kata • $charCount karakter", fontSize = 11.sp, color = LernspaceTextSecondary)
                            Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF10B981)))
                        }
                    }

                    // Editable Title
                    BasicTextField(
                        value = title,
                        onValueChange = onTitleChange,
                        textStyle = TextStyle(fontWeight = FontWeight.Bold, fontSize = 26.sp, color = LernspaceTextPrimary),
                        cursorBrush = SolidColor(Color(0xFF2563EB)),
                        decorationBox = { inner ->
                            if (title.isBlank()) {
                                Text("Judul Dokumen...", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = LernspaceTextSecondary.copy(alpha = 0.4f))
                            }
                            inner()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(color = LernspaceBorder.copy(alpha = 0.5f))

                    // ── WEBVIEW RICH TEXT EDITOR ──────────────────────────
                    AndroidView(
                        factory = { ctx ->
                            val bridge = DocumentJsBridge(
                                onContentChanged = { html ->
                                    lastLoadedContent = html
                                    onContentChange(html)
                                },
                                onWordCountChanged = { w, c ->
                                    wordCount = w
                                    charCount = c
                                }
                            )
                            buildDocumentWebView(ctx, isDarkTheme, content, bridge).also { wv ->
                                wv.webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        isWebViewReady = true
                                        webViewRef = view
                                    }
                                }
                                val html = generateEditorHtml(isDarkTheme, content)
                                wv.loadDataWithBaseURL("about:blank", html, "text/html", "UTF-8", null)
                                webViewRef = wv
                            }
                        },
                        update = { wv ->
                            // Only reload content if changed externally (e.g. AI injection)
                            if (content != lastLoadedContent && isWebViewReady) {
                                val escaped = content
                                    .replace("\\", "\\\\")
                                    .replace("'", "\\'")
                                    .replace("\n", "\\n")
                                    .replace("\r", "")
                                wv.evaluateJavascript("setContent('$escaped');", null)
                                lastLoadedContent = content
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 600.dp)
                    )
                }
            }
        }
    }

    // ── TABLE INSERT DIALOG ────────────────────────────────────────────────
    if (showTableDialog) {
        TableInsertDialog(
            onDismiss = { showTableDialog = false },
            onInsert = { rows, cols ->
                js("insertTable($rows, $cols);")
                showTableDialog = false
            }
        )
    }

    // ── LINK DIALOG ────────────────────────────────────────────────────────
    if (showLinkDialog) {
        InsertLinkDialog(
            initialText = "",
            onDismiss = { showLinkDialog = false },
            onApplyLink = { text, url ->
                val safeText = text.replace("'", "\\'")
                val safeUrl = url.replace("'", "\\'")
                js("insertLink('$safeText', '$safeUrl');")
                showLinkDialog = false
            }
        )
    }

    // ── MATH DIALOG ────────────────────────────────────────────────────────
    if (showMathDialog) {
        VisualMathEquationDialog(
            initialEquation = "",
            onDismiss = { showMathDialog = false },
            onInsertEquation = { latex ->
                val safeLaTeX = latex
                    .replace("\\", "\\\\")
                    .replace("'", "\\'")
                    .replace("\n", "")
                js("insertMath('$safeLaTeX');")
                showMathDialog = false
            }
        )
    }
}

// ── TABLE INSERT DIALOG ───────────────────────────────────────────────────────
@Composable
fun TableInsertDialog(
    onDismiss: () -> Unit,
    onInsert: (rows: Int, cols: Int) -> Unit
) {
    var rows by remember { mutableIntStateOf(3) }
    var cols by remember { mutableIntStateOf(3) }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = LernspaceSurfaceCard,
            border = BorderStroke(1.dp, LernspaceBorder),
            shadowElevation = 8.dp,
            modifier = Modifier.widthIn(min = 300.dp, max = 380.dp).wrapContentHeight()
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.TableChart, null, tint = AccentBlue)
                    Text("Sisipkan Tabel", style = MaterialTheme.typography.titleMedium, color = LernspaceDark)
                }

                // Grid visual preview
                val previewRows = rows.coerceAtMost(5)
                val previewCols = cols.coerceAtMost(5)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    repeat(previewRows) { r ->
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            repeat(previewCols) { _ ->
                                Box(
                                    Modifier
                                        .size(width = 36.dp, height = 20.dp)
                                        .background(
                                            if (r == 0) AccentBlue.copy(alpha = 0.18f) else Color(0xFFE2E8F0),
                                            RoundedCornerShape(3.dp)
                                        )
                                )
                            }
                        }
                    }
                }

                // Rows stepper
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Baris", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = LernspaceDark)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconButton(onClick = { if (rows > 1) rows-- }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.Remove, null, modifier = Modifier.size(16.dp))
                        }
                        Text("$rows", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = LernspaceDark)
                        IconButton(onClick = { if (rows < 20) rows++ }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // Cols stepper
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Kolom", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = LernspaceDark)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconButton(onClick = { if (cols > 1) cols-- }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.Remove, null, modifier = Modifier.size(16.dp))
                        }
                        Text("$cols", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = LernspaceDark)
                        IconButton(onClick = { if (cols < 10) cols++ }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) { Text("Batal", color = LernspaceTextSecondary) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onInsert(rows, cols) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                    ) {
                        Text("Sisipkan Tabel ${rows}×${cols}", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ── REUSABLE TOOLBAR COMPONENTS ───────────────────────────────────────────────
@Composable
private fun DocToolbarIcon(
    icon: ImageVector,
    tooltip: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(30.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = tooltip,
            modifier = Modifier.size(17.dp),
            tint = if (enabled) LernspaceTextPrimary else LernspaceTextSecondary.copy(alpha = 0.4f)
        )
    }
}

@Composable
private fun DocToolbarTextBtn(
    label: String,
    fontWeight: FontWeight = FontWeight.Normal,
    tooltip: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = Color.Transparent,
        modifier = Modifier.size(30.dp).clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, fontSize = 13.sp, fontWeight = fontWeight, color = LernspaceTextPrimary)
        }
    }
}

@Composable
private fun DocToolbarDiv() {
    Box(
        modifier = Modifier
            .height(18.dp)
            .width(1.dp)
            .background(LernspaceBorder.copy(alpha = 0.6f))
    )
}
