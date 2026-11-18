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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.lernspace.app.ui.theme.*

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
    INTER("Inter / Sans-Serif", "Inter, system-ui, sans-serif"),
    ARIAL("Arial", "Arial, Helvetica, sans-serif"),
    GEORGIA("Georgia / Serif", "Georgia, 'Times New Roman', serif"),
    TIMES("Times New Roman", "'Times New Roman', Times, serif"),
    VERDANA("Verdana", "Verdana, Geneva, Tahoma, sans-serif"),
    TREBUCHET("Trebuchet MS", "'Trebuchet MS', Helvetica, sans-serif"),
    PALATINO("Palatino", "'Palatino Linotype', 'Book Antiqua', Palatino, serif"),
    COURIER("Courier New / Mono", "'Courier New', Courier, monospace")
}

// ── JS BRIDGE ─────────────────────────────────────────────────────────────────
class DocumentJsBridge(
    private val onContentChanged: (String) -> Unit,
    private val onTitleChanged: (String) -> Unit,
    private val onWordCount: (Int, Int) -> Unit
) {
    @JavascriptInterface
    fun onContentChange(html: String) {
        onContentChanged(html)
        val plain = html.replace(Regex("<[^>]+>"), " ").replace(Regex("&[a-zA-Z]+;"), " ")
            .replace(Regex("\\s+"), " ").trim()
        val words = if (plain.isBlank()) 0 else plain.split(" ").filter { it.isNotBlank() }.size
        onWordCount(words, plain.length)
    }

    @JavascriptInterface
    fun onTitleChange(newTitle: String) {
        onTitleChanged(newTitle)
    }
}

// ── HTML GENERATOR ────────────────────────────────────────────────────────────
fun generateEditorHtml(
    isDark: Boolean,
    subject: String,
    title: String,
    bodyContent: String,
    wordCount: Int
): String {
    val bg = if (isDark) "#13141C" else "#F1F5F9"
    val paperBg = if (isDark) "#1E202B" else "#FFFFFF"
    val textColor = if (isDark) "#F1F5F9" else "#1E293B"
    val border = if (isDark) "#334155" else "#E2E8F0"
    val subBorder = if (isDark) "#1D4ED855" else "#2563EB33"
    val subjectChipBg = if (isDark) "#1D4ED820" else "#EFF6FF"
    val subjectChipColor = if (isDark) "#60A5FA" else "#2563EB"
    val titlePlaceholder = if (isDark) "#475569" else "#94A3B8"
    val placeholder = if (isDark) "#475569" else "#94A3B8"
    val linkColor = if (isDark) "#60A5FA" else "#2563EB"
    val tableBorder = if (isDark) "#475569" else "#CBD5E1"
    val tableHeaderBg = if (isDark) "#1E293B" else "#F8FAFC"
    val blockquoteBg = if (isDark) "#1E293B" else "#EFF6FF"
    val blockquoteColor = if (isDark) "#93C5FD" else "#1D4ED8"
    val codeBg = if (isDark) "#0F172A" else "#F1F5F9"
    val codeColor = if (isDark) "#7DD3FC" else "#0F172A"
    val imgToolbarBg = if (isDark) "#1E202B" else "#FFFFFF"
    val selectionColor = if (isDark) "#2563EB44" else "#2563EB22"
    val greenDot = "#10B981"
    val safeContent = bodyContent.ifBlank { "<p><br></p>" }
    val safeTitle = title.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    return """<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; }
  html { background: $bg; height: 100%; }
  body { background: $bg; padding: 20px 16px 120px 16px; min-height: 100%; font-family: Inter, system-ui, sans-serif; }

  /* ── PAPER ── */
  #paper {
    max-width: 840px;
    margin: 0 auto;
    background: $paperBg;
    border: 1px solid $border;
    border-radius: 4px;
    box-shadow: 0 2px 10px rgba(0,0,0,${if (isDark) "0.4" else "0.08"});
    padding: 44px 48px 60px 48px;
  }

  /* ── DOC HEADER ── */
  #doc-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
  }
  .subject-chip {
    background: $subjectChipBg;
    color: $subjectChipColor;
    border: 1px solid $subBorder;
    border-radius: 12px;
    padding: 3px 10px;
    font-size: 11px;
    font-weight: 700;
  }
  .wc-row { display: flex; align-items: center; gap: 8px; }
  #word-count { font-size: 11px; color: ${if (isDark) "#64748B" else "#94A3B8"}; }
  .green-dot { width: 7px; height: 7px; background: $greenDot; border-radius: 50%; display: inline-block; }

  /* ── TITLE ── */
  #doc-title {
    font-size: 26px;
    font-weight: 700;
    color: $textColor;
    outline: none;
    min-height: 36px;
    line-height: 1.3;
    margin-bottom: 12px;
    word-break: break-word;
  }
  #doc-title:empty::before {
    content: attr(data-placeholder);
    color: $titlePlaceholder;
    pointer-events: none;
  }
  #title-divider { border: none; border-top: 1px solid $border; margin-bottom: 16px; }

  /* ── EDITOR ── */
  #editor {
    outline: none;
    caret-color: #2563EB;
    color: $textColor;
    font-size: 15px;
    line-height: 1.8;
    word-wrap: break-word;
    overflow-wrap: break-word;
    min-height: 500px;
  }
  #editor:empty::before {
    content: 'Mulai mengetik di sini...';
    color: $placeholder;
    font-style: italic;
    pointer-events: none;
  }

  /* ── TYPOGRAPHY ── */
  #editor p { margin: 0 0 6px 0; min-height: 1.8em; }
  #editor h1 { font-size: 26px; font-weight: 700; margin: 18px 0 8px 0; }
  #editor h2 { font-size: 21px; font-weight: 700; margin: 15px 0 7px 0; }
  #editor h3 { font-size: 18px; font-weight: 600; margin: 12px 0 5px 0; }
  #editor h4 { font-size: 16px; font-weight: 600; margin: 10px 0 4px 0; }
  #editor h5 { font-size: 14px; font-weight: 600; margin: 8px 0 3px 0; }
  #editor strong { font-weight: 700; }
  #editor em { font-style: italic; }
  #editor u { text-decoration: underline; }
  #editor s { text-decoration: line-through; }
  #editor a { color: $linkColor; text-decoration: underline; }

  /* ── LISTS ── */
  #editor ul { list-style-type: disc; margin: 6px 0 10px 0; padding-left: 28px; }
  #editor ol { list-style-type: decimal; margin: 6px 0 10px 0; padding-left: 28px; }
  #editor li { margin: 3px 0; line-height: 1.7; }

  /* ── CHECKLIST ── */
  #editor ul.checklist { list-style: none; padding-left: 4px; }
  #editor ul.checklist li {
    display: flex; align-items: flex-start; gap: 8px; margin: 5px 0;
  }
  #editor ul.checklist li input[type="checkbox"] {
    margin-top: 4px; width: 16px; height: 16px;
    accent-color: #2563EB; cursor: pointer; flex-shrink: 0;
  }
  #editor ul.checklist li.done > span { text-decoration: line-through; opacity: 0.5; }

  /* ── BLOCKQUOTE ── */
  #editor blockquote {
    border-left: 3px solid #2563EB;
    margin: 10px 0;
    padding: 8px 16px;
    background: $blockquoteBg;
    border-radius: 0 6px 6px 0;
    color: $blockquoteColor;
    font-style: italic;
  }

  /* ── CODE ── */
  #editor pre {
    background: $codeBg;
    border: 1px solid $border;
    border-radius: 6px;
    padding: 12px 16px;
    font-family: 'Courier New', monospace;
    font-size: 13px;
    overflow-x: auto;
    margin: 10px 0;
    color: $codeColor;
    white-space: pre-wrap;
  }

  /* ── TABLES ── */
  #editor table {
    border-collapse: collapse;
    width: 100%;
    margin: 12px 0;
    font-size: 14px;
    table-layout: auto;
  }
  #editor table td, #editor table th {
    border: 1.5px solid $tableBorder;
    padding: 8px 12px;
    min-width: 60px;
    min-height: 36px;
    vertical-align: top;
    outline: none;
    word-break: break-word;
    line-height: 1.6;
  }
  #editor table th { background: $tableHeaderBg; font-weight: 600; }
  #editor table td:focus, #editor table th:focus { outline: 2px solid #2563EB; outline-offset: -2px; }

  /* ── IMAGES ── */
  #editor img {
    max-width: 100%;
    height: auto;
    display: block;
    margin: 10px auto;
    border-radius: 6px;
    border: 2px solid transparent;
    cursor: pointer;
    transition: border-color 0.15s;
  }
  #editor img.selected { border-color: #2563EB; border-radius: 6px; }

  /* ── MATH INLINE ── */
  .math-inline {
    font-family: 'Courier New', monospace;
    color: #1D4ED8;
    background: #EFF6FF;
    padding: 2px 7px;
    border-radius: 4px;
    font-size: 14px;
    display: inline-block;
  }

  /* ── FLOATING TOOLBARS ── */
  .floating-toolbar {
    display: none;
    position: fixed;
    background: $imgToolbarBg;
    border: 1.5px solid $border;
    border-radius: 10px;
    box-shadow: 0 4px 20px rgba(0,0,0,${if (isDark) "0.5" else "0.15"});
    padding: 5px 8px;
    gap: 4px;
    flex-direction: row;
    align-items: center;
    z-index: 9999;
    flex-wrap: wrap;
    max-width: 340px;
  }
  .floating-toolbar.visible { display: flex; }
  .ft-btn {
    background: transparent;
    border: none;
    border-radius: 5px;
    padding: 5px 9px;
    font-size: 11.5px;
    font-weight: 600;
    color: $textColor;
    cursor: pointer;
    white-space: nowrap;
    line-height: 1.3;
  }
  .ft-btn:active { background: ${if (isDark) "#334155" else "#EFF6FF"}; }
  .ft-btn.danger { color: #DC2626; }
  .ft-sep { width: 1px; height: 18px; background: $border; margin: 0 2px; }

  ::selection { background: $selectionColor; }
</style>
</head>
<body>

<!-- Image Toolbar -->
<div id="img-toolbar" class="floating-toolbar">
  <button class="ft-btn" onclick="resizeImg(25)">25%</button>
  <button class="ft-btn" onclick="resizeImg(50)">50%</button>
  <button class="ft-btn" onclick="resizeImg(75)">75%</button>
  <button class="ft-btn" onclick="resizeImg(100)">100%</button>
  <div class="ft-sep"></div>
  <button class="ft-btn" onclick="positionImg('block')">⬛ Tengah</button>
  <button class="ft-btn" onclick="positionImg('inline')">📄 Inline</button>
  <div class="ft-sep"></div>
  <button class="ft-btn danger" onclick="deleteImg()">🗑 Hapus</button>
</div>

<!-- Table Toolbar -->
<div id="tbl-toolbar" class="floating-toolbar">
  <button class="ft-btn" onclick="addRowAbove()">↑ Baris atas</button>
  <button class="ft-btn" onclick="addRowBelow()">↓ Baris bawah</button>
  <div class="ft-sep"></div>
  <button class="ft-btn" onclick="addColLeft()">← Kolom kiri</button>
  <button class="ft-btn" onclick="addColRight()">→ Kolom kanan</button>
  <div class="ft-sep"></div>
  <button class="ft-btn danger" onclick="deleteCurrentRow()">✕ Baris</button>
  <button class="ft-btn danger" onclick="deleteCurrentCol()">✕ Kolom</button>
  <button class="ft-btn danger" onclick="deleteTable()">🗑 Tabel</button>
</div>

<!-- Paper -->
<div id="paper">
  <div id="doc-header">
    <span class="subject-chip">📄 $subject · Dokumen Teks</span>
    <div class="wc-row">
      <span id="word-count">$wordCount kata</span>
      <span class="green-dot"></span>
    </div>
  </div>

  <div id="doc-title"
       contenteditable="true"
       data-placeholder="Judul Dokumen..."
       spellcheck="false"
       onkeydown="if(event.key==='Enter'){event.preventDefault();document.getElementById('editor').focus();}"
  >$safeTitle</div>

  <hr id="title-divider">

  <div id="editor" contenteditable="true" spellcheck="true">$safeContent</div>
</div>

<script>
var editor = document.getElementById('editor');
var docTitle = document.getElementById('doc-title');
var imgToolbar = document.getElementById('img-toolbar');
var tblToolbar = document.getElementById('tbl-toolbar');
var debounceTimer = null;
var selectedImg = null;
var currentCell = null;
var currentRow = null;
var currentTable = null;

// ── NOTIFY CHANGE ──
function notifyChange() {
  clearTimeout(debounceTimer);
  debounceTimer = setTimeout(function() {
    var html = editor.innerHTML;
    LernspaceDoc.onContentChange(html);
    updateWordCount(html);
  }, 350);
}

function updateWordCount(html) {
  var plain = html.replace(/<[^>]+>/g, ' ').replace(/&[a-zA-Z]+;/g, ' ').replace(/\s+/g, ' ').trim();
  var words = plain ? plain.split(' ').filter(function(w){ return w.length > 0; }).length : 0;
  var el = document.getElementById('word-count');
  if (el) el.textContent = words + ' kata';
}

editor.addEventListener('input', notifyChange);
editor.addEventListener('keyup', notifyChange);

// ── SCROLL INTO VIEW (fix keyboard coverage) ──
function scrollCaretIntoView() {
  var sel = window.getSelection();
  if (!sel || sel.rangeCount === 0) return;
  var range = sel.getRangeAt(0);
  var rect = range.getBoundingClientRect();
  if (rect.bottom > window.innerHeight - 20) {
    window.scrollBy(0, rect.bottom - window.innerHeight + 80);
  }
}
editor.addEventListener('keydown', function(e) {
  setTimeout(scrollCaretIntoView, 50);
});
editor.addEventListener('click', scrollCaretIntoView);

// Title changes notify Kotlin
docTitle.addEventListener('input', function() {
  LernspaceDoc.onTitleChange(docTitle.textContent || '');
});

// ── EXEC HELPERS ──
function exec(cmd, val) {
  editor.focus();
  document.execCommand(cmd, false, val !== undefined ? val : null);
  notifyChange();
}
function formatBlock(tag) { editor.focus(); exec('formatBlock', '<' + tag + '>'); }
function toggleBold() { exec('bold'); }
function toggleItalic() { exec('italic'); }
function toggleUnderline() { exec('underline'); }
function toggleStrikethrough() { exec('strikeThrough'); }
function setFontFamily(css) { exec('fontName', css); }
function setTextColor(color) { exec('foreColor', color); }
function setHighlight(color) {
  if (!color) { exec('hiliteColor', 'transparent'); }
  else { exec('hiliteColor', color); }
}
function alignText(mode) {
  var cmds = { left:'justifyLeft', center:'justifyCenter', right:'justifyRight', justify:'justifyFull' };
  exec(cmds[mode] || 'justifyLeft');
}
function undo() { exec('undo'); }
function redo() { exec('redo'); }
function clearFormatting() { exec('removeFormat'); }

// Font size via span
function setFontSize(px) {
  editor.focus();
  var sel = window.getSelection();
  if (!sel || sel.rangeCount === 0) return;
  var range = sel.getRangeAt(0);
  if (range.collapsed) {
    // Apply to future typing via document font size
    document.execCommand('fontSize', false, '7');
    var fontEls = editor.querySelectorAll('font[size="7"]');
    fontEls.forEach(function(el) {
      el.removeAttribute('size');
      el.style.fontSize = px + 'px';
    });
  } else {
    var span = document.createElement('span');
    span.style.fontSize = px + 'px';
    try { range.surroundContents(span); } catch(e) {}
  }
  notifyChange();
}

// ── LISTS ──
function insertBulletList() { exec('insertUnorderedList'); }
function insertNumberedList() { exec('insertOrderedList'); }
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
  var selectedText = sel.toString();
  cb.addEventListener('change', function() {
    li.classList.toggle('done', cb.checked);
    notifyChange();
  });
  var span = document.createElement('span');
  span.textContent = selectedText || 'Item';
  li.appendChild(cb);
  li.appendChild(span);
  ul.appendChild(li);
  range.deleteContents();
  range.insertNode(ul);
  var newRange = document.createRange();
  newRange.setStart(span, 0);
  newRange.collapse(true);
  sel.removeAllRanges();
  sel.addRange(newRange);
  notifyChange();
}
function insertBlockquote() { exec('formatBlock', 'blockquote'); }
function insertHR() { exec('insertHorizontalRule'); }

// ── LINK ──
function insertLink(text, url) {
  editor.focus();
  var sel = window.getSelection();
  if (!sel || sel.rangeCount === 0) return;
  var range = sel.getRangeAt(0);
  var a = document.createElement('a');
  a.href = url;
  a.textContent = text || url;
  range.deleteContents();
  range.insertNode(a);
  var newRange = document.createRange();
  newRange.setStartAfter(a);
  newRange.collapse(true);
  sel.removeAllRanges();
  sel.addRange(newRange);
  notifyChange();
}

// ── MATH ──
function insertMath(latex) {
  editor.focus();
  var sel = window.getSelection();
  if (!sel || sel.rangeCount === 0) return;
  var range = sel.getRangeAt(0);
  var span = document.createElement('span');
  span.className = 'math-inline';
  span.textContent = latex;
  span.setAttribute('data-latex', latex);
  range.deleteContents();
  range.insertNode(span);
  var br = document.createTextNode('\u00A0');
  span.parentNode.insertBefore(br, span.nextSibling);
  var newRange = document.createRange();
  newRange.setStartAfter(br);
  newRange.collapse(true);
  sel.removeAllRanges();
  sel.addRange(newRange);
  notifyChange();
}

// ── IMAGE INSERT ──
function insertImage(dataUri) {
  editor.focus();
  var sel = window.getSelection();
  var img = document.createElement('img');
  img.src = dataUri;
  img.style.maxWidth = '100%';
  img.style.display = 'block';
  img.style.margin = '10px auto';
  img.setAttribute('data-orig', dataUri.substring(0, 30));

  // Click to select
  img.addEventListener('click', function(e) {
    e.stopPropagation();
    selectImage(img, e);
  });

  if (sel && sel.rangeCount > 0) {
    var range = sel.getRangeAt(0);
    range.insertNode(img);
    var newRange = document.createRange();
    newRange.setStartAfter(img);
    newRange.collapse(true);
    sel.removeAllRanges();
    sel.addRange(newRange);
  } else {
    editor.appendChild(img);
  }
  notifyChange();
}

// Add click to existing images after setContent
function attachImageHandlers() {
  editor.querySelectorAll('img').forEach(function(img) {
    img.onclick = function(e) {
      e.stopPropagation();
      selectImage(img, e);
    };
  });
}

// Image selection
function selectImage(img, e) {
  if (selectedImg) selectedImg.classList.remove('selected');
  selectedImg = img;
  img.classList.add('selected');
  // Show toolbar
  imgToolbar.classList.add('visible');
  var rect = img.getBoundingClientRect();
  imgToolbar.style.top = (rect.bottom + 6) + 'px';
  imgToolbar.style.left = Math.min(rect.left, window.innerWidth - 350) + 'px';
}

function resizeImg(pct) {
  if (!selectedImg) return;
  selectedImg.style.width = pct + '%';
  selectedImg.style.maxWidth = pct + '%';
  selectedImg.style.height = 'auto';
  notifyChange();
}
function positionImg(mode) {
  if (!selectedImg) return;
  if (mode === 'block') {
    selectedImg.style.display = 'block';
    selectedImg.style.margin = '10px auto';
  } else {
    selectedImg.style.display = 'inline';
    selectedImg.style.margin = '4px 8px';
  }
  notifyChange();
}
function deleteImg() {
  if (!selectedImg) return;
  selectedImg.parentNode && selectedImg.parentNode.removeChild(selectedImg);
  selectedImg = null;
  imgToolbar.classList.remove('visible');
  notifyChange();
}

// Delete selected image with backspace/delete
document.addEventListener('keydown', function(e) {
  if ((e.key === 'Backspace' || e.key === 'Delete') && selectedImg) {
    e.preventDefault();
    deleteImg();
  }
});

// Dismiss image toolbar when clicking elsewhere
document.addEventListener('click', function(e) {
  if (!imgToolbar.contains(e.target) && e.target !== selectedImg) {
    if (selectedImg) {
      selectedImg.classList.remove('selected');
      selectedImg = null;
    }
    imgToolbar.classList.remove('visible');
  }
  if (!tblToolbar.contains(e.target) && !getParentCell(e.target)) {
    tblToolbar.classList.remove('visible');
  }
});

// ── TABLE INSERT ──
function insertTable(rows, cols) {
  editor.focus();
  var table = document.createElement('table');
  for (var r = 0; r < rows; r++) {
    var tr = document.createElement('tr');
    for (var c = 0; c < cols; c++) {
      var cell = (r === 0) ? document.createElement('th') : document.createElement('td');
      cell.contentEditable = 'true';
      if (r === 0) cell.textContent = 'Kolom ' + (c + 1);
      tr.appendChild(cell);
    }
    table.appendChild(tr);
  }
  var sel = window.getSelection();
  if (sel && sel.rangeCount > 0) {
    var range = sel.getRangeAt(0);
    range.deleteContents();
    var p = document.createElement('p');
    p.innerHTML = '<br>';
    range.insertNode(p);
    p.parentNode.insertBefore(table, p);
  } else {
    editor.appendChild(table);
  }
  notifyChange();
}

// ── TABLE TOOLBAR ──
function getParentCell(node) {
  while (node && node !== editor) {
    if (node.tagName === 'TD' || node.tagName === 'TH') return node;
    node = node.parentNode;
  }
  return null;
}
function getParentTable(node) {
  while (node && node !== editor) {
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
    var rect = cell.getBoundingClientRect();
    tblToolbar.classList.add('visible');
    tblToolbar.style.top = (rect.bottom + 6) + 'px';
    tblToolbar.style.left = Math.min(Math.max(4, rect.left), window.innerWidth - 360) + 'px';
  }
});

function addRowBelow() {
  if (!currentTable || !currentRow) return;
  var newRow = document.createElement('tr');
  for (var i = 0; i < currentTable.rows[0].cells.length; i++) {
    var td = document.createElement('td'); td.contentEditable = 'true'; newRow.appendChild(td);
  }
  currentRow.parentNode.insertBefore(newRow, currentRow.nextSibling);
  notifyChange();
}
function addRowAbove() {
  if (!currentTable || !currentRow) return;
  var newRow = document.createElement('tr');
  for (var i = 0; i < currentTable.rows[0].cells.length; i++) {
    var td = document.createElement('td'); td.contentEditable = 'true'; newRow.appendChild(td);
  }
  currentRow.parentNode.insertBefore(newRow, currentRow);
  notifyChange();
}
function addColRight() {
  if (!currentTable || !currentCell) return;
  var colIdx = currentCell.cellIndex;
  for (var r = 0; r < currentTable.rows.length; r++) {
    var row = currentTable.rows[r];
    var newCell = r === 0 ? document.createElement('th') : document.createElement('td');
    newCell.contentEditable = 'true';
    if (colIdx + 1 < row.cells.length) row.insertBefore(newCell, row.cells[colIdx + 1]);
    else row.appendChild(newCell);
  }
  notifyChange();
}
function addColLeft() {
  if (!currentTable || !currentCell) return;
  var colIdx = currentCell.cellIndex;
  for (var r = 0; r < currentTable.rows.length; r++) {
    var row = currentTable.rows[r];
    var newCell = r === 0 ? document.createElement('th') : document.createElement('td');
    newCell.contentEditable = 'true';
    row.insertBefore(newCell, row.cells[colIdx]);
  }
  notifyChange();
}
function deleteCurrentRow() {
  if (!currentRow) return;
  currentRow.parentNode.removeChild(currentRow);
  tblToolbar.classList.remove('visible');
  currentRow = null; currentCell = null;
  notifyChange();
}
function deleteCurrentCol() {
  if (!currentTable || !currentCell) return;
  var colIdx = currentCell.cellIndex;
  for (var r = 0; r < currentTable.rows.length; r++) {
    var row = currentTable.rows[r];
    if (colIdx < row.cells.length) row.removeChild(row.cells[colIdx]);
  }
  tblToolbar.classList.remove('visible');
  currentCell = null;
  notifyChange();
}
function deleteTable() {
  if (!currentTable) return;
  currentTable.parentNode && currentTable.parentNode.removeChild(currentTable);
  tblToolbar.classList.remove('visible');
  currentTable = null; currentRow = null; currentCell = null;
  notifyChange();
}

// ── SET CONTENT (from Kotlin for AI injection) ──
function setContent(html) {
  editor.innerHTML = html || '<p><br></p>';
  attachImageHandlers();
  notifyChange();
}
function setTitle(text) {
  docTitle.textContent = text;
}

// Tab key → indent in editor
editor.addEventListener('keydown', function(e) {
  if (e.key === 'Tab') {
    e.preventDefault();
    exec('insertHTML', '&nbsp;&nbsp;&nbsp;&nbsp;');
  }
});

// Init
attachImageHandlers();
editor.focus();
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
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var lastContent by remember { mutableStateOf("") }
    var lastTitle by remember { mutableStateOf("") }
    var isReady by remember { mutableStateOf(false) }
    var wordCount by remember { mutableIntStateOf(0) }
    var charCount by remember { mutableIntStateOf(0) }

    // Toolbar state
    var selectedStyle by remember { mutableStateOf(DocParagraphStyle.NORMAL) }
    var selectedFont by remember { mutableStateOf(DocFontFamily.INTER) }
    var fontSizePx by remember { mutableIntStateOf(15) }
    var alignIndex by remember { mutableIntStateOf(0) } // 0=left,1=center,2=right,3=justify
    var showStyleDropdown by remember { mutableStateOf(false) }
    var showFontDropdown by remember { mutableStateOf(false) }
    var showColorDropdown by remember { mutableStateOf(false) }
    var showHighlightDropdown by remember { mutableStateOf(false) }
    var showTableDialog by remember { mutableStateOf(false) }
    var showLinkDialog by remember { mutableStateOf(false) }
    var showMathDialog by remember { mutableStateOf(false) }

    val alignModes = listOf("left", "center", "right", "justify")
    val alignIcons = listOf(Icons.Filled.FormatAlignLeft, Icons.Filled.FormatAlignCenter,
        Icons.Filled.FormatAlignRight, Icons.Filled.FormatAlignJustify)
    val alignLabels = listOf("Rata Kiri", "Rata Tengah", "Rata Kanan", "Rata Penuh")

    fun js(script: String) = webViewRef?.evaluateJavascript(script, null)

    // Image picker
    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()
                if (bytes != null) {
                    val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                    // Limit large images to avoid OOM
                    val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    if (b64.length > 4_000_000) {
                        Toast.makeText(context, "Gambar terlalu besar, coba gambar yang lebih kecil", Toast.LENGTH_LONG).show()
                        return@rememberLauncherForActivityResult
                    }
                    val dataUri = "data:$mimeType;base64,$b64"
                    js("insertImage('$dataUri');")
                    Toast.makeText(context, "Gambar disisipkan ✓", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("DocEditor", "Image error", e)
                Toast.makeText(context, "Gagal memuat gambar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Root: toolbar fixed top + WebView fills rest + imePadding so keyboard doesn't cover
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDarkTheme) Color(0xFF13141C) else Color(0xFFF1F5F9))
            .imePadding()
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
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Undo / Redo / Print
                DocToolbarIcon(Icons.AutoMirrored.Filled.Undo, "Undo") { js("undo();") }
                DocToolbarIcon(Icons.AutoMirrored.Filled.Redo, "Redo") { js("redo();") }
                DocToolbarIcon(Icons.Filled.Print, "Export PDF", onClick = onExportPdf)
                DocToolbarDiv()

                // Paragraph Style dropdown
                Box {
                    Surface(
                        shape = RoundedCornerShape(6.dp), color = Color.Transparent,
                        modifier = Modifier.height(32.dp).clickable { showStyleDropdown = true }.padding(horizontal = 6.dp)
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
                                onClick = { selectedStyle = style; showStyleDropdown = false; js("formatBlock('${style.htmlTag}');") }
                            )
                        }
                    }
                }
                DocToolbarDiv()

                // Font family dropdown
                Box {
                    Surface(
                        shape = RoundedCornerShape(6.dp), color = Color.Transparent,
                        modifier = Modifier.height(32.dp).clickable { showFontDropdown = true }.padding(horizontal = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(selectedFont.label.substringBefore(" /"), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = LernspaceTextPrimary)
                            Icon(Icons.Filled.ArrowDropDown, null, Modifier.size(15.dp), tint = LernspaceTextSecondary)
                        }
                    }
                    DropdownMenu(expanded = showFontDropdown, onDismissRequest = { showFontDropdown = false }) {
                        DocFontFamily.entries.forEach { font ->
                            DropdownMenuItem(
                                text = { Text(font.label, fontSize = 13.sp) },
                                onClick = {
                                    selectedFont = font; showFontDropdown = false
                                    val safeCss = font.css.replace("'", "\\'")
                                    js("setFontFamily('$safeCss');")
                                }
                            )
                        }
                    }
                }
                DocToolbarDiv()

                // Font size stepper
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                    DocToolbarIcon(Icons.Filled.Remove, "Kecil") { if (fontSizePx > 10) { fontSizePx--; js("setFontSize($fontSizePx);") } }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isDarkTheme) Color(0xFF2A2D3A) else Color.White,
                        border = BorderStroke(1.dp, LernspaceBorder),
                        modifier = Modifier.size(width = 32.dp, height = 26.dp)
                    ) { Box(contentAlignment = Alignment.Center) { Text("$fontSizePx", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LernspaceTextPrimary) } }
                    DocToolbarIcon(Icons.Filled.Add, "Besar") { if (fontSizePx < 72) { fontSizePx++; js("setFontSize($fontSizePx);") } }
                }
                DocToolbarDiv()

                // Bold / Italic / Underline / Strike
                DocToolbarTextBtn("B", FontWeight.Bold, "Tebal") { js("toggleBold();") }
                DocToolbarTextBtn("I", tooltip = "Miring") { js("toggleItalic();") }
                DocToolbarTextBtn("U", tooltip = "Garis Bawah") { js("toggleUnderline();") }
                DocToolbarTextBtn("S", tooltip = "Coret") { js("toggleStrikethrough();") }

                // Text color picker
                Box {
                    IconButton(onClick = { showColorDropdown = true }, Modifier.size(30.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text("A", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = LernspaceTextPrimary)
                            Box(Modifier.size(width = 14.dp, height = 3.dp).background(Color(0xFF2563EB)))
                        }
                    }
                    DropdownMenu(expanded = showColorDropdown, onDismissRequest = { showColorDropdown = false }) {
                        listOf("Hitam" to "#1E293B", "Biru" to "#2563EB", "Merah" to "#DC2626",
                            "Hijau" to "#059669", "Ungu" to "#7C3AED", "Oranye" to "#EA580C",
                            "Merah Muda" to "#EC4899", "Kuning Tua" to "#CA8A04").forEach { (name, hex) ->
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

                // Highlight picker
                Box {
                    DocToolbarIcon(Icons.Filled.BorderColor, "Sorotan") { showHighlightDropdown = true }
                    DropdownMenu(expanded = showHighlightDropdown, onDismissRequest = { showHighlightDropdown = false }) {
                        listOf("Kuning" to "#FEF08A", "Hijau" to "#BBF7D0", "Biru" to "#BFDBFE",
                            "Pink" to "#FBCFE8", "Oranye" to "#FED7AA", "Hapus" to "").forEach { (name, hex) ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (hex.isNotEmpty()) Box(Modifier.size(14.dp).clip(RoundedCornerShape(2.dp)).background(Color(android.graphics.Color.parseColor(hex))))
                                        Text(name, fontSize = 12.sp)
                                    }
                                },
                                onClick = {
                                    if (hex.isNotEmpty()) js("setHighlight('$hex');") else js("setHighlight(null);")
                                    showHighlightDropdown = false
                                }
                            )
                        }
                    }
                }
                DocToolbarDiv()

                // ── ALIGNMENT: single cycling button ──
                Surface(
                    shape = RoundedCornerShape(5.dp),
                    color = Color.Transparent,
                    modifier = Modifier.size(30.dp).clickable {
                        alignIndex = (alignIndex + 1) % 4
                        js("alignText('${alignModes[alignIndex]}');")
                    }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(alignIcons[alignIndex], alignLabels[alignIndex], Modifier.size(17.dp), tint = LernspaceTextPrimary)
                    }
                }
                DocToolbarDiv()

                // Lists
                DocToolbarIcon(Icons.AutoMirrored.Filled.FormatListBulleted, "Bullet List") { js("insertBulletList();") }
                DocToolbarIcon(Icons.Filled.FormatListNumbered, "Numbered List") { js("insertNumberedList();") }
                DocToolbarIcon(Icons.Filled.Checklist, "Checklist") { js("insertChecklist();") }
                DocToolbarIcon(Icons.Filled.FormatQuote, "Kutipan") { js("insertBlockquote();") }
                DocToolbarDiv()

                // Insert tools
                DocToolbarIcon(Icons.Filled.Link, "Tautan") { showLinkDialog = true }
                DocToolbarIcon(Icons.Filled.Functions, "Rumus") { showMathDialog = true }
                DocToolbarIcon(Icons.Filled.Image, "Gambar") { imagePickerLauncher.launch("image/*") }
                DocToolbarIcon(Icons.Filled.TableChart, "Tabel") { showTableDialog = true }
                DocToolbarIcon(Icons.Filled.HorizontalRule, "Garis") { js("insertHR();") }
                DocToolbarIcon(Icons.Filled.FormatClear, "Hapus Format") { js("clearFormatting();") }
                DocToolbarDiv()

                // AI
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF8B5CF6).copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.35f)),
                    modifier = Modifier.height(30.dp).clickable(onClick = onTriggerCheatSheet)
                ) {
                    Row(modifier = Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
                    Row(modifier = Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("📝", fontSize = 11.sp)
                        Text("Rangkum", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentBlue)
                    }
                }
            }
        }

        // ── WEBVIEW (fills remaining space, handles own scroll) ───────────
        AndroidView(
            factory = { ctx ->
                val bridge = DocumentJsBridge(
                    onContentChanged = { html ->
                        lastContent = html
                        onContentChange(html)
                    },
                    onTitleChanged = { newTitle ->
                        lastTitle = newTitle
                        onTitleChange(newTitle)
                    },
                    onWordCount = { w, c -> wordCount = w; charCount = c }
                )

                val wv = WebView(ctx).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        allowFileAccess = true
                        allowContentAccess = true
                        cacheMode = WebSettings.LOAD_NO_CACHE
                        setSupportZoom(false)
                        builtInZoomControls = false
                        displayZoomControls = false
                    }
                    addJavascriptInterface(bridge, "LernspaceDoc")
                    webChromeClient = WebChromeClient()
                    setBackgroundColor(0x00000000)
                    isHorizontalScrollBarEnabled = false
                    isNestedScrollingEnabled = true
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isReady = true
                        }
                    }
                }

                val html = generateEditorHtml(isDarkTheme, subject, title, content, wordCount)
                wv.loadDataWithBaseURL("about:blank", html, "text/html", "UTF-8", null)
                webViewRef = wv
                lastContent = content
                lastTitle = title
                wv
            },
            update = { wv ->
                // Sync content from AI injection (content changed externally)
                if (content != lastContent && isReady) {
                    val escaped = content
                        .replace("\\", "\\\\")
                        .replace("'", "\\'")
                        .replace("\n", "\\n")
                        .replace("\r", "")
                    wv.evaluateJavascript("setContent('$escaped');", null)
                    lastContent = content
                }
                // Sync title if changed from outside
                if (title != lastTitle && isReady) {
                    val safeTitle = title.replace("'", "\\'")
                    wv.evaluateJavascript("setTitle('$safeTitle');", null)
                    lastTitle = title
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
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
            val t = text.replace("'", "\\'")
            val u = url.replace("'", "\\'")
            js("insertLink('$t', '$u');")
            showLinkDialog = false
        }
    }

    // ── MATH DIALOG ───────────────────────────────────────────────────────
    if (showMathDialog) {
        VisualMathEquationDialog(initialEquation = "", onDismiss = { showMathDialog = false }) { latex ->
            val safe = latex.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "")
            js("insertMath('$safe');")
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
            shape = RoundedCornerShape(16.dp), color = LernspaceSurfaceCard,
            border = BorderStroke(1.dp, LernspaceBorder), shadowElevation = 8.dp,
            modifier = Modifier.widthIn(min = 300.dp, max = 380.dp).wrapContentHeight()
        ) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.TableChart, null, tint = AccentBlue)
                    Text("Sisipkan Tabel", style = MaterialTheme.typography.titleMedium, color = LernspaceDark)
                }
                // Grid preview
                val pr = rows.coerceAtMost(5); val pc = cols.coerceAtMost(6)
                Column(Modifier.fillMaxWidth().background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp)).padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(pr) { r ->
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            repeat(pc) {
                                Box(Modifier.height(18.dp).weight(1f).background(if (r == 0) AccentBlue.copy(0.2f) else Color(0xFFE2E8F0), RoundedCornerShape(2.dp)))
                            }
                        }
                    }
                }
                // Rows
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text("Baris", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = LernspaceDark)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconButton(onClick = { if (rows > 1) rows-- }, Modifier.size(32.dp)) { Icon(Icons.Filled.Remove, null, Modifier.size(16.dp)) }
                        Text("$rows", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = LernspaceDark)
                        IconButton(onClick = { if (rows < 25) rows++ }, Modifier.size(32.dp)) { Icon(Icons.Filled.Add, null, Modifier.size(16.dp)) }
                    }
                }
                // Cols
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text("Kolom", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = LernspaceDark)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconButton(onClick = { if (cols > 1) cols-- }, Modifier.size(32.dp)) { Icon(Icons.Filled.Remove, null, Modifier.size(16.dp)) }
                        Text("$cols", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = LernspaceDark)
                        IconButton(onClick = { if (cols < 10) cols++ }, Modifier.size(32.dp)) { Icon(Icons.Filled.Add, null, Modifier.size(16.dp)) }
                    }
                }
                Row(Modifier.fillMaxWidth(), Arrangement.End, Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) { Text("Batal", color = LernspaceTextSecondary) }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onInsert(rows, cols) }, shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)) {
                        Text("Sisipkan ${rows}×${cols}", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ── HELPERS ───────────────────────────────────────────────────────────────────
private fun DocParagraphStyle.isBold() = this == DocParagraphStyle.TITLE || this == DocParagraphStyle.HEADING_1

@Composable
private fun DocToolbarIcon(icon: ImageVector, tooltip: String, enabled: Boolean = true, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(30.dp)) {
        Icon(icon, tooltip, Modifier.size(17.dp), tint = if (enabled) LernspaceTextPrimary else LernspaceTextSecondary.copy(alpha = 0.4f))
    }
}

@Composable
private fun DocToolbarTextBtn(label: String, fontWeight: FontWeight = FontWeight.Normal, tooltip: String, onClick: () -> Unit) {
    Surface(shape = RoundedCornerShape(4.dp), color = Color.Transparent, modifier = Modifier.size(30.dp).clickable(onClick = onClick)) {
        Box(contentAlignment = Alignment.Center) { Text(label, fontSize = 13.sp, fontWeight = fontWeight, color = LernspaceTextPrimary) }
    }
}

@Composable
private fun DocToolbarDiv() {
    Box(Modifier.height(18.dp).width(1.dp).background(LernspaceBorder.copy(alpha = 0.6f)))
}
