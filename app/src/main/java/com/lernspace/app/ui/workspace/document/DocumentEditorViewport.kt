package com.lernspace.app.ui.workspace.document

import android.content.Context
import android.graphics.Typeface
import android.text.Editable
import android.text.Html
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.AbsoluteSizeSpan
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
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

// ── CUSTOM EDITTEXT FOR SELECTION LISTENING ──────────────────────────────────
class DocumentRichEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.editTextStyle
) : EditText(context, attrs, defStyleAttr) {

    var onSelectionChangedListener: ((start: Int, end: Int) -> Unit)? = null

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        onSelectionChangedListener?.invoke(selStart, selEnd)
    }
}

// ── RICH DOCUMENT CONVERTER (HTML <-> SPANNED) ──────────────────────────────
object RichDocumentConverter {
    fun toHtml(spanned: Spanned): String {
        return try {
            val baseHtml = Html.toHtml(spanned, Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)
            val bgSpans = spanned.getSpans(0, spanned.length, BackgroundColorSpan::class.java)
            if (bgSpans.isEmpty()) return baseHtml

            var result = baseHtml
            for (span in bgSpans) {
                val start = spanned.getSpanStart(span)
                val end = spanned.getSpanEnd(span)
                if (start in 0 until end && end <= spanned.length) {
                    val rawPiece = spanned.subSequence(start, end).toString()
                    if (rawPiece.isNotBlank()) {
                        val hex = String.format("#%06X", 0xFFFFFF and span.backgroundColor)
                        val escaped = Html.escapeHtml(rawPiece)
                        if (!result.contains("background-color:$hex")) {
                            result = result.replace(escaped, "<mark style=\"background-color:$hex\">$escaped</mark>")
                        }
                    }
                }
            }
            result
        } catch (_: Exception) {
            spanned.toString()
        }
    }

    fun fromHtml(raw: String): Spanned {
        if (raw.isBlank()) return SpannableStringBuilder("")
        return try {
            val containsHtml = raw.contains("<") && raw.contains(">")
            val spanned = if (containsHtml) {
                Html.fromHtml(raw, Html.FROM_HTML_MODE_COMPACT)
            } else {
                SpannableStringBuilder(raw)
            }
            val ssb = SpannableStringBuilder(spanned)

            // Parse background colors from <mark style="background-color:..."> or <span style="background-color:...">
            val bgPattern = Regex("""<(?:mark|span)\s+style="background-color:\s*(#[0-9a-fA-F]{6})">(.*?)<\/(?:mark|span)>""", RegexOption.DOT_MATCHES_ALL)
            for (match in bgPattern.findAll(raw)) {
                val colorHex = match.groupValues[1]
                val innerHtml = match.groupValues[2]
                val plainText = Html.fromHtml(innerHtml, Html.FROM_HTML_MODE_COMPACT).toString()
                if (colorHex.isNotEmpty() && plainText.isNotEmpty()) {
                    val colorInt = try { android.graphics.Color.parseColor(colorHex) } catch (_: Exception) { null }
                    if (colorInt != null) {
                        var searchStart = 0
                        while (searchStart < ssb.length) {
                            val idx = ssb.indexOf(plainText, searchStart)
                            if (idx == -1) break
                            ssb.setSpan(BackgroundColorSpan(colorInt), idx, idx + plainText.length, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
                            searchStart = idx + plainText.length
                        }
                    }
                }
            }
            ssb
        } catch (_: Exception) {
            SpannableStringBuilder(raw)
        }
    }
}

// ── ENUMS FOR PARAGRAPH STYLE & FONT FAMILY ─────────────────────────────────
enum class DocParagraphStyle(val label: String, val sizeSp: Int, val isBold: Boolean) {
    NORMAL("Teks Normal", 15, false),
    TITLE("Judul Dokumen", 28, true),
    HEADING_1("Heading 1 (Besar)", 22, true),
    HEADING_2("Heading 2 (Sedang)", 18, true),
    HEADING_3("Heading 3 (Kecil)", 16, true)
}

enum class DocFontFamily(val label: String, val typefaceName: String, val composeFont: FontFamily) {
    DEFAULT("Calibri / Sans", "sans-serif", FontFamily.SansSerif),
    SERIF("Merriweather / Serif", "serif", FontFamily.Serif),
    MONO("JetBrains / Mono", "monospace", FontFamily.Monospace)
}

// ── UNDO / REDO MANAGER ─────────────────────────────────────────────────────
class RichTextUndoRedoManager(private val maxHistory: Int = 30) {
    private val undoStack = mutableListOf<String>()
    private val redoStack = mutableListOf<String>()
    var isUndoingOrRedoing = false

    fun pushState(stateHtml: String) {
        if (isUndoingOrRedoing) return
        if (undoStack.isNotEmpty() && undoStack.last() == stateHtml) return
        undoStack.add(stateHtml)
        if (undoStack.size > maxHistory) {
            undoStack.removeAt(0)
        }
        redoStack.clear()
    }

    fun canUndo(): Boolean = undoStack.size > 1
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    fun undo(): String? {
        if (!canUndo()) return null
        isUndoingOrRedoing = true
        val current = undoStack.removeAt(undoStack.lastIndex)
        redoStack.add(current)
        val prev = undoStack.last()
        isUndoingOrRedoing = false
        return prev
    }

    fun redo(): String? {
        if (!canRedo()) return null
        isUndoingOrRedoing = true
        val next = redoStack.removeAt(redoStack.lastIndex)
        undoStack.add(next)
        isUndoingOrRedoing = false
        return next
    }
}

// ── MAIN DOCUMENT EDITOR VIEWPORT ───────────────────────────────────────────
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

    // Reference to native EditText
    var editTextRef by remember { mutableStateOf<DocumentRichEditText?>(null) }
    val undoRedoManager = remember { RichTextUndoRedoManager() }

    // Text metrics
    var textSnapshot by remember { mutableStateOf("") }
    val wordCount = remember(textSnapshot) {
        textSnapshot.trim().split(Regex("""\s+""")).filter { it.isNotBlank() }.size
    }
    val charCount = textSnapshot.length

    // Formatting Toolbar States
    var selectedParagraphStyle by remember { mutableStateOf(DocParagraphStyle.NORMAL) }
    var selectedFontFamily by remember { mutableStateOf(DocFontFamily.DEFAULT) }
    var fontSizeSp by remember { mutableIntStateOf(15) }
    var isSelectionBold by remember { mutableStateOf(false) }
    var isSelectionItalic by remember { mutableStateOf(false) }
    var isSelectionUnderline by remember { mutableStateOf(false) }
    var isSelectionStrike by remember { mutableStateOf(false) }

    // Dropdown visibility states
    var showStyleDropdown by remember { mutableStateOf(false) }
    var showFontDropdown by remember { mutableStateOf(false) }
    var showColorDropdown by remember { mutableStateOf(false) }
    var showHighlightDropdown by remember { mutableStateOf(false) }

    // Dialogs
    var showLinkDialog by remember { mutableStateOf(false) }
    var linkDialogInitialText by remember { mutableStateOf("") }
    var showMathDialog by remember { mutableStateOf(false) }

    // Internal sync flags
    var isInternalUpdate by remember { mutableStateOf(false) }
    var lastExportedHtml by remember { mutableStateOf("") }

    // Selection target helper
    fun getTargetRange(editText: DocumentRichEditText): Pair<Int, Int>? {
        val start = editText.selectionStart
        val end = editText.selectionEnd
        if (start < 0) return null
        if (start != end) {
            return Pair(minOf(start, end), maxOf(start, end))
        }
        val text = editText.text ?: return null
        if (text.isEmpty()) return null
        var wStart = (start - 1).coerceAtLeast(0)
        while (wStart >= 0 && !text[wStart].isWhitespace()) {
            wStart--
        }
        wStart++
        var wEnd = start.coerceAtMost(text.length)
        while (wEnd < text.length && !text[wEnd].isWhitespace()) {
            wEnd++
        }
        return if (wStart < wEnd) Pair(wStart, wEnd) else null
    }

    // Line helper
    fun getLineRange(text: CharSequence, cursor: Int): Pair<Int, Int> {
        if (text.isEmpty()) return Pair(0, 0)
        val c = cursor.coerceIn(0, text.length)
        val lastNewLine = text.lastIndexOf('\n', (c - 1).coerceAtLeast(0))
        val lineStart = if (lastNewLine == -1) 0 else lastNewLine + 1
        val nextNewLine = text.indexOf('\n', c)
        val lineEnd = if (nextNewLine == -1) text.length else nextNewLine
        return Pair(lineStart, lineEnd)
    }

    // Sync HTML back to parent
    fun notifyContentChanged() {
        val et = editTextRef ?: return
        val html = RichDocumentConverter.toHtml(et.text)
        lastExportedHtml = html
        undoRedoManager.pushState(html)
        onContentChange(html)
        textSnapshot = et.text.toString()
    }

    // ── FORMATTING OPERATIONS ───────────────────────────────────────────────
    fun toggleBold() {
        val et = editTextRef ?: return
        val (start, end) = getTargetRange(et) ?: return
        val editable = et.text ?: return
        val spans = editable.getSpans(start, end, StyleSpan::class.java)
        var hasBold = false
        for (span in spans) {
            val sStart = editable.getSpanStart(span)
            val sEnd = editable.getSpanEnd(span)
            if (sStart <= start && sEnd >= end && (span.style == Typeface.BOLD || span.style == Typeface.BOLD_ITALIC)) {
                hasBold = true
                editable.removeSpan(span)
                if (span.style == Typeface.BOLD_ITALIC) {
                    editable.setSpan(StyleSpan(Typeface.ITALIC), sStart, sEnd, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
                }
            }
        }
        if (!hasBold) {
            editable.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
        }
        isSelectionBold = !hasBold
        notifyContentChanged()
    }

    fun toggleItalic() {
        val et = editTextRef ?: return
        val (start, end) = getTargetRange(et) ?: return
        val editable = et.text ?: return
        val spans = editable.getSpans(start, end, StyleSpan::class.java)
        var hasItalic = false
        for (span in spans) {
            val sStart = editable.getSpanStart(span)
            val sEnd = editable.getSpanEnd(span)
            if (sStart <= start && sEnd >= end && (span.style == Typeface.ITALIC || span.style == Typeface.BOLD_ITALIC)) {
                hasItalic = true
                editable.removeSpan(span)
                if (span.style == Typeface.BOLD_ITALIC) {
                    editable.setSpan(StyleSpan(Typeface.BOLD), sStart, sEnd, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
                }
            }
        }
        if (!hasItalic) {
            editable.setSpan(StyleSpan(Typeface.ITALIC), start, end, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
        }
        isSelectionItalic = !hasItalic
        notifyContentChanged()
    }

    fun toggleUnderline() {
        val et = editTextRef ?: return
        val (start, end) = getTargetRange(et) ?: return
        val editable = et.text ?: return
        val spans = editable.getSpans(start, end, UnderlineSpan::class.java)
        var hasUnderline = false
        for (span in spans) {
            val sStart = editable.getSpanStart(span)
            val sEnd = editable.getSpanEnd(span)
            if (sStart <= start && sEnd >= end) {
                hasUnderline = true
                editable.removeSpan(span)
            }
        }
        if (!hasUnderline) {
            editable.setSpan(UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
        }
        isSelectionUnderline = !hasUnderline
        notifyContentChanged()
    }

    fun toggleStrikethrough() {
        val et = editTextRef ?: return
        val (start, end) = getTargetRange(et) ?: return
        val editable = et.text ?: return
        val spans = editable.getSpans(start, end, StrikethroughSpan::class.java)
        var hasStrike = false
        for (span in spans) {
            val sStart = editable.getSpanStart(span)
            val sEnd = editable.getSpanEnd(span)
            if (sStart <= start && sEnd >= end) {
                hasStrike = true
                editable.removeSpan(span)
            }
        }
        if (!hasStrike) {
            editable.setSpan(StrikethroughSpan(), start, end, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
        }
        isSelectionStrike = !hasStrike
        notifyContentChanged()
    }

    fun applyTextColor(colorHex: String) {
        val et = editTextRef ?: return
        val (start, end) = getTargetRange(et) ?: return
        val editable = et.text ?: return
        val colorInt = try { android.graphics.Color.parseColor(colorHex) } catch (_: Exception) { return }
        val existing = editable.getSpans(start, end, ForegroundColorSpan::class.java)
        for (s in existing) editable.removeSpan(s)
        editable.setSpan(ForegroundColorSpan(colorInt), start, end, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
        notifyContentChanged()
    }

    fun applyHighlightColor(colorHex: String?) {
        val et = editTextRef ?: return
        val (start, end) = getTargetRange(et) ?: return
        val editable = et.text ?: return
        val existing = editable.getSpans(start, end, BackgroundColorSpan::class.java)
        for (s in existing) editable.removeSpan(s)
        if (!colorHex.isNullOrEmpty()) {
            val colorInt = try { android.graphics.Color.parseColor(colorHex) } catch (_: Exception) { return }
            editable.setSpan(BackgroundColorSpan(colorInt), start, end, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
        }
        notifyContentChanged()
    }

    fun applyFontSize(sizeSp: Int) {
        val et = editTextRef ?: return
        val (start, end) = getTargetRange(et) ?: return
        val editable = et.text ?: return
        val existing = editable.getSpans(start, end, AbsoluteSizeSpan::class.java)
        for (s in existing) editable.removeSpan(s)
        editable.setSpan(AbsoluteSizeSpan(sizeSp, true), start, end, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
        notifyContentChanged()
    }

    fun applyFontFamily(typefaceName: String) {
        val et = editTextRef ?: return
        val (start, end) = getTargetRange(et) ?: return
        val editable = et.text ?: return
        val existing = editable.getSpans(start, end, TypefaceSpan::class.java)
        for (s in existing) editable.removeSpan(s)
        editable.setSpan(TypefaceSpan(typefaceName), start, end, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
        notifyContentChanged()
    }

    fun applyParagraphStyle(style: DocParagraphStyle) {
        val et = editTextRef ?: return
        val editable = et.text ?: return
        val cursor = et.selectionStart.coerceAtLeast(0)
        val (lineStart, lineEnd) = getLineRange(editable, cursor)
        if (lineStart >= lineEnd) return

        val sizeSpans = editable.getSpans(lineStart, lineEnd, AbsoluteSizeSpan::class.java)
        for (s in sizeSpans) editable.removeSpan(s)
        val styleSpans = editable.getSpans(lineStart, lineEnd, StyleSpan::class.java)
        for (s in styleSpans) editable.removeSpan(s)

        editable.setSpan(AbsoluteSizeSpan(style.sizeSp, true), lineStart, lineEnd, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
        if (style.isBold) {
            editable.setSpan(StyleSpan(Typeface.BOLD), lineStart, lineEnd, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
        }
        notifyContentChanged()
    }

    fun applyLink(linkText: String, url: String) {
        val et = editTextRef ?: return
        val editable = et.text ?: return
        val start = minOf(et.selectionStart, et.selectionEnd).coerceAtLeast(0)
        val end = maxOf(et.selectionStart, et.selectionEnd).coerceAtLeast(0)

        if (start != end) {
            val existing = editable.getSpans(start, end, URLSpan::class.java)
            for (s in existing) editable.removeSpan(s)
            editable.setSpan(URLSpan(url), start, end, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
            editable.setSpan(ForegroundColorSpan(android.graphics.Color.parseColor("#2563EB")), start, end, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
            editable.setSpan(UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
        } else {
            editable.insert(start, linkText)
            val newEnd = start + linkText.length
            editable.setSpan(URLSpan(url), start, newEnd, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
            editable.setSpan(ForegroundColorSpan(android.graphics.Color.parseColor("#2563EB")), start, newEnd, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
            editable.setSpan(UnderlineSpan(), start, newEnd, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
            et.setSelection(newEnd)
        }
        notifyContentChanged()
    }

    fun insertMathEquation(equation: String) {
        val et = editTextRef ?: return
        val editable = et.text ?: return
        val cursor = et.selectionStart.coerceAtLeast(0)
        val formatted = " $equation "
        editable.insert(cursor, formatted)
        val endPos = cursor + formatted.length
        editable.setSpan(TypefaceSpan("monospace"), cursor, endPos, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
        editable.setSpan(ForegroundColorSpan(android.graphics.Color.parseColor("#1D4ED8")), cursor, endPos, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
        editable.setSpan(BackgroundColorSpan(android.graphics.Color.parseColor("#EFF6FF")), cursor, endPos, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
        et.setSelection(endPos)
        notifyContentChanged()
    }

    fun prefixCurrentLine(prefix: String) {
        val et = editTextRef ?: return
        val editable = et.text ?: return
        val cursor = et.selectionStart.coerceAtLeast(0)
        val (lineStart, _) = getLineRange(editable, cursor)
        editable.insert(lineStart, prefix)
        et.setSelection((cursor + prefix.length).coerceAtMost(editable.length))
        notifyContentChanged()
    }

    // Image Picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val et = editTextRef ?: return@rememberLauncherForActivityResult
            val cursor = et.selectionStart.coerceAtLeast(0)
            val imgText = "\n[📷 Gambar: $uri]\n"
            et.text?.insert(cursor, imgText)
            val endPos = cursor + imgText.length
            et.text?.setSpan(StyleSpan(Typeface.ITALIC), cursor, endPos, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
            et.text?.setSpan(ForegroundColorSpan(android.graphics.Color.parseColor("#475569")), cursor, endPos, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
            et.setSelection(endPos)
            notifyContentChanged()
            Toast.makeText(context, "Gambar berhasil disisipkan", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDarkTheme) Color(0xFF13141C) else Color(0xFFF1F5F9))
    ) {
        // ── 1. GOOGLE DOCS NATIVE WYSIWYG FORMATTING RIBBON ──────────────────
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
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // ── Group A: Undo / Redo / Print ──
                ToolbarIconButton(
                    icon = Icons.AutoMirrored.Filled.Undo,
                    tooltip = "Undo",
                    enabled = undoRedoManager.canUndo(),
                    onClick = {
                        val prevHtml = undoRedoManager.undo()
                        if (prevHtml != null) {
                            editTextRef?.let { et ->
                                isInternalUpdate = true
                                val spanned = RichDocumentConverter.fromHtml(prevHtml)
                                et.setText(spanned)
                                et.setSelection(spanned.length)
                                lastExportedHtml = prevHtml
                                onContentChange(prevHtml)
                                textSnapshot = et.text.toString()
                                isInternalUpdate = false
                            }
                        }
                    }
                )
                ToolbarIconButton(
                    icon = Icons.AutoMirrored.Filled.Redo,
                    tooltip = "Redo",
                    enabled = undoRedoManager.canRedo(),
                    onClick = {
                        val nextHtml = undoRedoManager.redo()
                        if (nextHtml != null) {
                            editTextRef?.let { et ->
                                isInternalUpdate = true
                                val spanned = RichDocumentConverter.fromHtml(nextHtml)
                                et.setText(spanned)
                                et.setSelection(spanned.length)
                                lastExportedHtml = nextHtml
                                onContentChange(nextHtml)
                                textSnapshot = et.text.toString()
                                isInternalUpdate = false
                            }
                        }
                    }
                )
                ToolbarIconButton(
                    icon = Icons.Filled.Print,
                    tooltip = "Print / Export PDF",
                    onClick = onExportPdf
                )

                ToolbarDivider()

                // ── Group B: Paragraph Style Dropdown ──
                Box {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Transparent,
                        modifier = Modifier
                            .height(32.dp)
                            .clickable { showStyleDropdown = true }
                            .padding(horizontal = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = selectedParagraphStyle.label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = LernspaceTextPrimary
                            )
                            Icon(Icons.Filled.ArrowDropDown, null, modifier = Modifier.size(16.dp), tint = LernspaceTextSecondary)
                        }
                    }

                    DropdownMenu(
                        expanded = showStyleDropdown,
                        onDismissRequest = { showStyleDropdown = false }
                    ) {
                        DocParagraphStyle.entries.forEach { style ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = style.label,
                                        fontWeight = if (style.isBold) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = if (style == DocParagraphStyle.TITLE) 15.sp else 13.sp
                                    )
                                },
                                onClick = {
                                    selectedParagraphStyle = style
                                    showStyleDropdown = false
                                    applyParagraphStyle(style)
                                }
                            )
                        }
                    }
                }

                ToolbarDivider()

                // ── Group C: Font Family Dropdown ──
                Box {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Transparent,
                        modifier = Modifier
                            .height(32.dp)
                            .clickable { showFontDropdown = true }
                            .padding(horizontal = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = selectedFontFamily.label.substringBefore(" /"),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = LernspaceTextPrimary
                            )
                            Icon(Icons.Filled.ArrowDropDown, null, modifier = Modifier.size(16.dp), tint = LernspaceTextSecondary)
                        }
                    }

                    DropdownMenu(
                        expanded = showFontDropdown,
                        onDismissRequest = { showFontDropdown = false }
                    ) {
                        DocFontFamily.entries.forEach { font ->
                            DropdownMenuItem(
                                text = { Text(font.label, fontSize = 13.sp) },
                                onClick = {
                                    selectedFontFamily = font
                                    showFontDropdown = false
                                    applyFontFamily(font.typefaceName)
                                }
                            )
                        }
                    }
                }

                ToolbarDivider()

                // ── Group D: Font Size Stepper [-] 15 [+] ──
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    ToolbarIconButton(
                        icon = Icons.Filled.Remove,
                        tooltip = "Perkecil ukuran font",
                        onClick = {
                            if (fontSizeSp > 10) {
                                fontSizeSp -= 1
                                applyFontSize(fontSizeSp)
                            }
                        }
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isDarkTheme) Color(0xFF2A2D3A) else Color.White,
                        border = BorderStroke(1.dp, LernspaceBorder),
                        modifier = Modifier.size(width = 32.dp, height = 26.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "$fontSizeSp",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = LernspaceTextPrimary
                            )
                        }
                    }
                    ToolbarIconButton(
                        icon = Icons.Filled.Add,
                        tooltip = "Perbesar ukuran font",
                        onClick = {
                            if (fontSizeSp < 42) {
                                fontSizeSp += 1
                                applyFontSize(fontSizeSp)
                            }
                        }
                    )
                }

                ToolbarDivider()

                // ── Group E: WYSIWYG Inline Styles (Bold, Italic, Underline, Strikethrough) ──
                ToolbarTextButton(
                    label = "B",
                    fontWeight = FontWeight.Bold,
                    isActive = isSelectionBold,
                    tooltip = "Tebal (Bold)",
                    onClick = { toggleBold() }
                )
                ToolbarTextButton(
                    label = "I",
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    isActive = isSelectionItalic,
                    tooltip = "Miring (Italic)",
                    onClick = { toggleItalic() }
                )
                ToolbarTextButton(
                    label = "U",
                    isActive = isSelectionUnderline,
                    tooltip = "Garis Bawah (Underline)",
                    onClick = { toggleUnderline() }
                )
                ToolbarTextButton(
                    label = "S",
                    isActive = isSelectionStrike,
                    tooltip = "Coret (Strikethrough)",
                    onClick = { toggleStrikethrough() }
                )

                // Text Color Palette Picker
                Box {
                    IconButton(
                        onClick = { showColorDropdown = true },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("A", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = LernspaceTextPrimary)
                            Box(
                                modifier = Modifier
                                    .size(width = 14.dp, height = 3.dp)
                                    .background(Color(0xFF2563EB))
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showColorDropdown,
                        onDismissRequest = { showColorDropdown = false }
                    ) {
                        val colors = listOf(
                            "Hitam" to "#1E293B",
                            "Biru" to "#2563EB",
                            "Merah" to "#DC2626",
                            "Hijau" to "#059669",
                            "Ungu" to "#7C3AED",
                            "Oranye" to "#EA580C"
                        )
                        colors.forEach { (name, hex) ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(Color(android.graphics.Color.parseColor(hex)))
                                        )
                                        Text(name, fontSize = 12.sp)
                                    }
                                },
                                onClick = {
                                    applyTextColor(hex)
                                    showColorDropdown = false
                                }
                            )
                        }
                    }
                }

                // Highlighter Marker Picker
                Box {
                    ToolbarIconButton(
                        icon = Icons.Filled.BorderColor,
                        tooltip = "Warna Sorotan (Highlighter)",
                        onClick = { showHighlightDropdown = true }
                    )

                    DropdownMenu(
                        expanded = showHighlightDropdown,
                        onDismissRequest = { showHighlightDropdown = false }
                    ) {
                        val highlights = listOf(
                            "Kuning" to "#FEF08A",
                            "Hijau Muda" to "#BBF7D0",
                            "Biru Muda" to "#BFDBFE",
                            "Pink Muda" to "#FBCFE8",
                            "Hapus Sorotan" to ""
                        )
                        highlights.forEach { (name, hex) ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (hex.isNotEmpty()) {
                                            Box(
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .background(Color(android.graphics.Color.parseColor(hex)))
                                            )
                                        }
                                        Text(name, fontSize = 12.sp)
                                    }
                                },
                                onClick = {
                                    applyHighlightColor(hex.ifEmpty { null })
                                    showHighlightDropdown = false
                                }
                            )
                        }
                    }
                }

                ToolbarDivider()

                // ── Group F: Insert Tools (Link, Visual Math Keyboard, Image, Table, Divider) ──
                ToolbarIconButton(
                    icon = Icons.Filled.Link,
                    tooltip = "Sisipkan Tautan (Link)",
                    onClick = {
                        val et = editTextRef
                        val sStart = et?.selectionStart ?: 0
                        val sEnd = et?.selectionEnd ?: 0
                        linkDialogInitialText = if (sStart != sEnd && et?.text != null) {
                            val min = minOf(sStart, sEnd)
                            val max = maxOf(sStart, sEnd)
                            et.text.subSequence(min, max).toString()
                        } else ""
                        showLinkDialog = true
                    }
                )
                ToolbarIconButton(
                    icon = Icons.Filled.Functions,
                    tooltip = "Keyboard Rumus Matematika (Visual)",
                    onClick = { showMathDialog = true }
                )
                ToolbarIconButton(
                    icon = Icons.Filled.Image,
                    tooltip = "Sisipkan Gambar",
                    onClick = { imagePickerLauncher.launch("image/*") }
                )
                ToolbarIconButton(
                    icon = Icons.Filled.HorizontalRule,
                    tooltip = "Garis Pembatas",
                    onClick = { prefixCurrentLine("\n────────────────────────────────────────\n") }
                )
                ToolbarIconButton(
                    icon = Icons.Filled.TableChart,
                    tooltip = "Sisipkan Tabel",
                    onClick = {
                        val table = "\n┌──────────────┬──────────────┬──────────────┐\n│ Kolom 1      │ Kolom 2      │ Kolom 3      │\n├──────────────┼──────────────┼──────────────┤\n│ Data 1       │ Data 2       │ Data 3       │\n│ Data 4       │ Data 5       │ Data 6       │\n└──────────────┴──────────────┴──────────────┘\n"
                        prefixCurrentLine(table)
                    }
                )

                ToolbarDivider()

                // ── Group G: Lists & Callouts (Bullets, Numbers, Checklist, Quotes) ──
                ToolbarIconButton(
                    icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                    tooltip = "Daftar Poin (Bulleted List)",
                    onClick = { prefixCurrentLine("• ") }
                )
                ToolbarIconButton(
                    icon = Icons.Filled.FormatListNumbered,
                    tooltip = "Daftar Angka (Numbered List)",
                    onClick = { prefixCurrentLine("1. ") }
                )
                ToolbarIconButton(
                    icon = Icons.Filled.Checklist,
                    tooltip = "Daftar Checklist / To-Do",
                    onClick = { prefixCurrentLine("☐ ") }
                )
                ToolbarIconButton(
                    icon = Icons.Filled.FormatQuote,
                    tooltip = "Kutipan / Callout Box",
                    onClick = { prefixCurrentLine("▌ ") }
                )

                ToolbarDivider()

                // ── Group H: Mimo AI Direct Actions ──
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF8B5CF6).copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.35f)),
                    modifier = Modifier
                        .height(30.dp)
                        .clickable(onClick = onTriggerCheatSheet)
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
                    modifier = Modifier
                        .height(30.dp)
                        .clickable(onClick = onTriggerSummary)
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

        // ── 2. DOCUMENT PAGE VIEWPORT (CENTERED A4-STYLE SHEET) ──────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 24.dp, horizontal = 16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 840.dp)
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 1100.dp)
                    .shadow(elevation = 3.dp, shape = RoundedCornerShape(4.dp))
                    .clickable {
                        // Tapping sheet focuses editor at end
                        editTextRef?.let { et ->
                            et.requestFocus()
                            et.setSelection(et.text?.length ?: 0)
                        }
                    },
                shape = RoundedCornerShape(4.dp),
                color = if (isDarkTheme) Color(0xFF1E202B) else Color.White,
                border = BorderStroke(1.dp, LernspaceBorder)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 56.dp, vertical = 48.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Document Header Meta
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
                                text = "📄 $subject · Dokumen Teks",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentBlue,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        // Word count & sync chip
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "$wordCount kata • $charCount karakter",
                                fontSize = 11.sp,
                                color = LernspaceTextSecondary
                            )
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                            )
                        }
                    }

                    // Document Editable Title
                    BasicTextField(
                        value = title,
                        onValueChange = onTitleChange,
                        textStyle = TextStyle(
                            fontFamily = selectedFontFamily.composeFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                            color = LernspaceTextPrimary
                        ),
                        cursorBrush = SolidColor(Color(0xFF2563EB)),
                        decorationBox = { innerTextField ->
                            if (title.isBlank()) {
                                Text(
                                    text = "Judul Dokumen Tanpa Nama...",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LernspaceTextSecondary.copy(alpha = 0.5f)
                                )
                            }
                            innerTextField()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(color = LernspaceBorder.copy(alpha = 0.6f), thickness = 1.dp)

                    // ── TRUE WYSIWYG RICH TEXT EDITOR ────────────────────────
                    AndroidView(
                        factory = { ctx ->
                            DocumentRichEditText(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT
                                )
                                background = null
                                setPadding(0, 0, 0, 0)
                                textSize = 15f
                                setTextColor(if (isDarkTheme) 0xFFF1F5F9.toInt() else 0xFF1E293B.toInt())
                                setHintTextColor(if (isDarkTheme) 0xFF64748B.toInt() else 0xFF94A3B8.toInt())
                                hint = "Mulai mengetik dokumen di sini..."
                                setLineSpacing(8f, 1.35f)
                                gravity = Gravity.TOP or Gravity.START
                                isFocusable = true
                                isFocusableInTouchMode = true
                                isNestedScrollingEnabled = false

                                // Initial content load
                                val initialSpanned = RichDocumentConverter.fromHtml(content)
                                setText(initialSpanned)
                                setSelection(initialSpanned.length)
                                textSnapshot = initialSpanned.toString()
                                lastExportedHtml = content
                                undoRedoManager.pushState(content)

                                // Listen to selection changes to update toolbar active indicators
                                onSelectionChangedListener = { start, end ->
                                    val ed = text
                                    if (ed != null && start >= 0 && end >= 0) {
                                        val sMin = minOf(start, end)
                                        val sMax = maxOf(start, end)
                                        if (sMin < sMax) {
                                            val styleSpans = ed.getSpans(sMin, sMax, StyleSpan::class.java)
                                            isSelectionBold = styleSpans.any { it.style == Typeface.BOLD || it.style == Typeface.BOLD_ITALIC }
                                            isSelectionItalic = styleSpans.any { it.style == Typeface.ITALIC || it.style == Typeface.BOLD_ITALIC }
                                            isSelectionUnderline = ed.getSpans(sMin, sMax, UnderlineSpan::class.java).isNotEmpty()
                                            isSelectionStrike = ed.getSpans(sMin, sMax, StrikethroughSpan::class.java).isNotEmpty()
                                        }
                                    }
                                }

                                // Listen to text modifications
                                addTextChangedListener(object : TextWatcher {
                                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                                    override fun afterTextChanged(s: Editable?) {
                                        if (!isInternalUpdate && s != null) {
                                            val html = RichDocumentConverter.toHtml(s)
                                            lastExportedHtml = html
                                            undoRedoManager.pushState(html)
                                            onContentChange(html)
                                            textSnapshot = s.toString()
                                        }
                                    }
                                })

                                editTextRef = this
                            }
                        },
                        update = { editText ->
                            // Update only if content changed externally (e.g. Mimo AI insertion)
                            if (content != lastExportedHtml) {
                                isInternalUpdate = true
                                val spanned = RichDocumentConverter.fromHtml(content)
                                editText.setText(spanned)
                                editText.setSelection(spanned.length)
                                lastExportedHtml = content
                                textSnapshot = spanned.toString()
                                undoRedoManager.pushState(content)
                                isInternalUpdate = false
                            }
                            editText.setTextColor(if (isDarkTheme) 0xFFF1F5F9.toInt() else 0xFF1E293B.toInt())
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                    )
                }
            }
        }
    }

    // ── MODAL DIALOGS ────────────────────────────────────────────────────────
    if (showLinkDialog) {
        InsertLinkDialog(
            initialText = linkDialogInitialText,
            onDismiss = { showLinkDialog = false },
            onApplyLink = { text, url ->
                applyLink(text, url)
                showLinkDialog = false
            }
        )
    }

    if (showMathDialog) {
        VisualMathEquationDialog(
            initialEquation = "",
            onDismiss = { showMathDialog = false },
            onInsertEquation = { displayEquation ->
                insertMathEquation(displayEquation)
                showMathDialog = false
            }
        )
    }
}

// ── REUSABLE TOOLBAR SUB-COMPONENTS ──────────────────────────────────────────
@Composable
private fun ToolbarIconButton(
    icon: ImageVector,
    tooltip: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(30.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = tooltip,
            modifier = Modifier.size(17.dp),
            tint = if (enabled) LernspaceTextPrimary else LernspaceTextSecondary.copy(alpha = 0.4f)
        )
    }
}

@Composable
private fun ToolbarTextButton(
    label: String,
    fontWeight: FontWeight = FontWeight.Normal,
    fontStyle: androidx.compose.ui.text.font.FontStyle = androidx.compose.ui.text.font.FontStyle.Normal,
    isActive: Boolean = false,
    tooltip: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = if (isActive) AccentBlue.copy(alpha = 0.15f) else Color.Transparent,
        border = if (isActive) BorderStroke(1.dp, AccentBlue.copy(alpha = 0.4f)) else null,
        modifier = Modifier
            .size(30.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = fontWeight,
                fontStyle = fontStyle,
                color = if (isActive) AccentBlue else LernspaceTextPrimary
            )
        }
    }
}

@Composable
private fun ToolbarDivider() {
    Box(
        modifier = Modifier
            .height(18.dp)
            .width(1.dp)
            .background(LernspaceBorder.copy(alpha = 0.6f))
            .padding(horizontal = 2.dp)
    )
}
