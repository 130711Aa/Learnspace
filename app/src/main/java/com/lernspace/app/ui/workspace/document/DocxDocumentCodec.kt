package com.lernspace.app.ui.workspace.document

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class ImportedDocument(
    val title: String,
    val html: String,
    val formatLabel: String,
    val plainText: String
)

/**
 * Pure-Android OpenXML (.docx) + HTML + Markdown + TXT + RTF codec.
 * Zero external heavyweight dependencies; 100% compatible with Microsoft Word,
 * Google Docs, WPS Office, and LibreOffice Writer.
 */
object DocxDocumentCodec {

    const val MIME_DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    const val MIME_DOC = "application/msword"
    const val MIME_HTML = "text/html"
    const val MIME_TXT = "text/plain"
    const val MIME_MD = "text/markdown"
    const val MIME_RTF = "application/rtf"

    val SUPPORTED_IMPORT_MIMES = arrayOf(
        MIME_DOCX,
        MIME_DOC,
        MIME_HTML,
        MIME_TXT,
        MIME_MD,
        MIME_RTF,
        "text/rtf",
        "application/octet-stream"
    )

    fun isEditableDocument(fileName: String, mimeType: String?): Boolean {
        val ext = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        val mime = mimeType?.lowercase(Locale.ROOT).orEmpty()
        if (ext in setOf("docx", "doc", "html", "htm", "txt", "md", "markdown", "rtf")) return true
        return mime.contains("wordprocessingml") ||
            mime == MIME_DOC ||
            mime == MIME_HTML ||
            mime == MIME_TXT ||
            mime.contains("markdown") ||
            mime.contains("rtf")
    }

    fun getDisplayName(context: Context, uri: Uri): String {
        var name = "Dokumen"
        try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) {
                        val value = c.getString(idx)
                        if (!value.isNullOrBlank()) name = value
                    }
                }
            }
        } catch (_: Exception) {
            uri.lastPathSegment?.substringAfterLast('/')?.let { if (it.isNotBlank()) name = it }
        }
        return name
    }

    // =========================================================================
    //  IMPORT (DOCX / HTML / MD / TXT / RTF / LEGACY DOC -> HTML)
    // =========================================================================
    fun importFromUri(context: Context, uri: Uri): Result<ImportedDocument> = runCatching {
        val rawName = getDisplayName(context, uri)
        val cleanTitle = rawName.substringBeforeLast('.').ifBlank { "Dokumen Impor" }
        val ext = rawName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        val mime = context.contentResolver.getType(uri)?.lowercase(Locale.ROOT).orEmpty()

        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Tidak dapat membaca isi file")

        val isZipHeader = bytes.size >= 4 &&
            bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte() &&
            bytes[2] == 0x03.toByte() && bytes[3] == 0x04.toByte()

        val (html, formatLabel) = when {
            isZipHeader || ext == "docx" || mime.contains("wordprocessingml") -> {
                parseDocxToHtml(bytes) to "Dokumen Word (.docx)"
            }
            ext == "html" || ext == "htm" || mime == MIME_HTML -> {
                val text = bytes.toString(Charsets.UTF_8)
                extractSafeBodyHtml(text) to "Dokumen HTML"
            }
            ext == "md" || ext == "markdown" || mime.contains("markdown") -> {
                markdownToHtml(bytes.toString(Charsets.UTF_8)) to "Dokumen Markdown"
            }
            ext == "rtf" || mime.contains("rtf") || bytes.take(5).toByteArray().toString(Charsets.US_ASCII).startsWith("{\\rtf") -> {
                rtfToHtml(bytes.toString(Charsets.ISO_8859_1)) to "Dokumen RTF"
            }
            ext == "doc" || mime == MIME_DOC -> {
                val preview = bytes.take(512).toByteArray().toString(Charsets.UTF_8)
                when {
                    preview.contains("<html", ignoreCase = true) || preview.contains("<body", ignoreCase = true) ->
                        extractSafeBodyHtml(bytes.toString(Charsets.UTF_8)) to "Dokumen Word (.doc)"
                    preview.startsWith("{\\rtf") ->
                        rtfToHtml(bytes.toString(Charsets.ISO_8859_1)) to "Dokumen Word (.doc)"
                    else ->
                        extractLegacyDocToHtml(bytes) to "Dokumen Word (.doc)"
                }
            }
            else -> {
                plainTextToHtml(bytes.toString(Charsets.UTF_8)) to "Dokumen Teks"
            }
        }

        val plainText = stripHtmlToPlainText(html)
        ImportedDocument(
            title = cleanTitle,
            html = html.ifBlank { "<p><br></p>" },
            formatLabel = formatLabel,
            plainText = plainText
        )
    }

    // ── DOCX ZIP + XML PARSER ─────────────────────────────────────────────────
    private data class RelInfo(val target: String, val isExternal: Boolean)

    private fun parseDocxToHtml(docxBytes: ByteArray): String {
        var documentXml: ByteArray? = null
        var relsXml: ByteArray? = null
        var numberingXml: ByteArray? = null
        val mediaMap = mutableMapOf<String, ByteArray>()

        ZipInputStream(ByteArrayInputStream(docxBytes)).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                val name = entry.name.replace('\\', '/')
                if (!entry.isDirectory) {
                    val data = zis.readBytes()
                    when (name.lowercase(Locale.ROOT)) {
                        "word/document.xml" -> documentXml = data
                        "word/_rels/document.xml.rels" -> relsXml = data
                        "word/numbering.xml" -> numberingXml = data
                        else -> if (name.startsWith("word/media/", ignoreCase = true)) {
                            mediaMap[name.substringAfter("word/").lowercase(Locale.ROOT)] = data
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        val docBytes = documentXml ?: error("File .docx tidak memiliki word/document.xml")
        val rels = relsXml?.let { parseRelationships(it) } ?: emptyMap()
        val orderedNumIds = numberingXml?.let { parseOrderedNumberingIds(it) } ?: emptySet()

        // Pre-encode media to compressed base64 data URIs lazily
        val encodedMediaCache = mutableMapOf<String, String>()
        fun resolveImageDataUri(relId: String): String? {
            val rel = rels[relId] ?: return null
            val normalizedTarget = rel.target.removePrefix("/").removePrefix("word/").lowercase(Locale.ROOT)
            encodedMediaCache[normalizedTarget]?.let { return it }
            val raw = mediaMap[normalizedTarget] ?: return null
            val dataUri = compressImageBytesToDataUri(raw, normalizedTarget) ?: return null
            encodedMediaCache[normalizedTarget] = dataUri
            return dataUri
        }

        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            setInput(ByteArrayInputStream(docBytes), "UTF-8")
        }

        val out = StringBuilder()
        var activeListTag: String? = null // "ul" or "ol"
        var importedSetup = DocPageSetup()
        var hasImportedPageSetup = false

        fun closeListIfOpen() {
            if (activeListTag != null) {
                out.append("</").append(activeListTag).append(">")
                activeListTag = null
            }
        }

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                when (localName(parser.name)) {
                    "p" -> {
                        val pHtml = parseParagraph(parser, rels, orderedNumIds, ::resolveImageDataUri)
                        if (pHtml.listTag != null) {
                            if (activeListTag != pHtml.listTag) {
                                closeListIfOpen()
                                activeListTag = pHtml.listTag
                                out.append("<").append(activeListTag).append(">")
                            }
                            out.append("<li>").append(pHtml.innerHtml.ifBlank { "<br>" }).append("</li>")
                        } else {
                            closeListIfOpen()
                            out.append(pHtml.fullHtml)
                        }
                    }
                    "tbl" -> {
                        closeListIfOpen()
                        out.append(parseTable(parser, rels, orderedNumIds, ::resolveImageDataUri))
                    }
                    "pgSz" -> {
                        val wTw = attr(parser, "w").toIntOrNull()
                        val hTw = attr(parser, "h").toIntOrNull()
                        val orientAttr = attr(parser, "orient").lowercase(Locale.ROOT)
                        if (wTw != null && hTw != null && wTw > 0 && hTw > 0) {
                            val isLand = orientAttr == "landscape" || wTw > hTw
                            val longMm = (maxOf(wTw, hTw) * 25.4 / 1440.0)
                            val detectedSize = when {
                                longMm > 343.0 -> DocPaperSize.LEGAL
                                longMm > 312.0 -> DocPaperSize.F4
                                longMm < 288.0 -> DocPaperSize.LETTER
                                else -> DocPaperSize.A4
                            }
                            importedSetup = importedSetup.copy(
                                paperSize = detectedSize,
                                orientation = if (isLand) DocPageOrientation.LANDSCAPE else DocPageOrientation.PORTRAIT
                            )
                            hasImportedPageSetup = true
                        }
                    }
                    "pgMar" -> {
                        val t = attr(parser, "top").toIntOrNull()
                        val b = attr(parser, "bottom").toIntOrNull()
                        val l = attr(parser, "left").toIntOrNull()
                        val r = attr(parser, "right").toIntOrNull()
                        if (t != null && b != null && l != null && r != null) {
                            importedSetup = importedSetup.copy(
                                marginPreset = DocMarginPreset.CUSTOM,
                                marginTopCm = (t * 2.54f / 1440f).coerceIn(0.5f, 6.0f),
                                marginBottomCm = (b * 2.54f / 1440f).coerceIn(0.5f, 6.0f),
                                marginLeftCm = (l * 2.54f / 1440f).coerceIn(0.5f, 6.0f),
                                marginRightCm = (r * 2.54f / 1440f).coerceIn(0.5f, 6.0f)
                            )
                            hasImportedPageSetup = true
                        }
                    }
                }
            }
            event = parser.next()
        }
        closeListIfOpen()
        val bodyHtml = out.toString().ifBlank { "<p><br></p>" }
        return if (hasImportedPageSetup) DocPageSetup.packWithContent(importedSetup, bodyHtml) else bodyHtml
    }

    private data class ParsedParagraph(
        val fullHtml: String,
        val innerHtml: String,
        val listTag: String? // "ul", "ol", or null
    )

    private fun parseParagraph(
        parser: XmlPullParser,
        rels: Map<String, RelInfo>,
        orderedNumIds: Set<String>,
        resolveImage: (String) -> String?
    ): ParsedParagraph {
        var tag = "p"
        var align: String? = null
        var listTag: String? = null
        var isQuote = false
        val inner = StringBuilder()
        var depth = 1

        while (depth > 0) {
            val ev = parser.next()
            if (ev == XmlPullParser.END_DOCUMENT) break
            if (ev == XmlPullParser.START_TAG) {
                val name = localName(parser.name)
                depth++
                when (name) {
                    "pStyle" -> {
                        val styleVal = attr(parser, "val").lowercase(Locale.ROOT)
                        when {
                            styleVal == "title" -> tag = "h1"
                            styleVal.startsWith("heading1") || styleVal == "heading 1" || styleVal == "1" -> tag = "h2"
                            styleVal.startsWith("heading2") || styleVal == "heading 2" || styleVal == "2" -> tag = "h3"
                            styleVal.startsWith("heading3") || styleVal == "heading 3" || styleVal == "3" -> tag = "h4"
                            styleVal.startsWith("heading4") || styleVal == "heading 4" || styleVal == "4" -> tag = "h5"
                            styleVal.contains("quote") -> isQuote = true
                        }
                    }
                    "jc" -> {
                        val jcVal = attr(parser, "val").lowercase(Locale.ROOT)
                        align = when (jcVal) {
                            "center" -> "center"
                            "right", "end" -> "right"
                            "both", "distribute" -> "justify"
                            else -> null
                        }
                    }
                    "numId" -> {
                        val id = attr(parser, "val")
                        if (id.isNotBlank() && id != "0") {
                            listTag = if (orderedNumIds.contains(id)) "ol" else "ul"
                        }
                    }
                    "hyperlink" -> {
                        val rId = attr(parser, "id")
                        val url = rels[rId]?.target
                        val linkHtml = parseHyperlink(parser, resolveImage)
                        depth-- // parseHyperlink consumes up to </hyperlink>
                        if (!url.isNullOrBlank()) {
                            inner.append("<a href=\"").append(escapeHtmlAttr(url)).append("\">")
                                .append(linkHtml).append("</a>")
                        } else {
                            inner.append(linkHtml)
                        }
                    }
                    "r" -> {
                        inner.append(parseRun(parser, resolveImage))
                        depth-- // parseRun consumes up to </r>
                    }
                }
            } else if (ev == XmlPullParser.END_TAG) {
                depth--
            }
        }

        val innerStr = inner.toString()
        val alignStyle = if (align != null) " style=\"text-align:$align;\"" else ""
        val effectiveTag = if (isQuote && tag == "p") "blockquote" else tag
        val fullHtml = "<$effectiveTag$alignStyle>${innerStr.ifBlank { "<br>" }}</$effectiveTag>"
        return ParsedParagraph(fullHtml = fullHtml, innerHtml = innerStr, listTag = listTag)
    }

    private fun parseHyperlink(parser: XmlPullParser, resolveImage: (String) -> String?): String {
        val sb = StringBuilder()
        var depth = 1
        while (depth > 0) {
            val ev = parser.next()
            if (ev == XmlPullParser.END_DOCUMENT) break
            if (ev == XmlPullParser.START_TAG) {
                val name = localName(parser.name)
                depth++
                if (name == "r") {
                    sb.append(parseRun(parser, resolveImage))
                    depth--
                }
            } else if (ev == XmlPullParser.END_TAG) {
                depth--
            }
        }
        return sb.toString()
    }

    private fun parseRun(parser: XmlPullParser, resolveImage: (String) -> String?): String {
        var bold = false
        var italic = false
        var underline = false
        var strike = false
        var textColor: String? = null
        var bgColor: String? = null
        var fontSizePx: Int? = null
        var fontFamily: String? = null
        var drawingWidthPx: Int? = null
        val content = StringBuilder()
        var depth = 1

        while (depth > 0) {
            val ev = parser.next()
            if (ev == XmlPullParser.END_DOCUMENT) break
            if (ev == XmlPullParser.START_TAG) {
                val name = localName(parser.name)
                depth++
                when (name) {
                    "b", "bCs" -> {
                        val v = attr(parser, "val").lowercase(Locale.ROOT)
                        bold = v != "0" && v != "false"
                    }
                    "i", "iCs" -> {
                        val v = attr(parser, "val").lowercase(Locale.ROOT)
                        italic = v != "0" && v != "false"
                    }
                    "u" -> {
                        val v = attr(parser, "val").lowercase(Locale.ROOT)
                        underline = v != "none" && v != "0" && v != "false"
                    }
                    "strike", "dstrike" -> {
                        val v = attr(parser, "val").lowercase(Locale.ROOT)
                        strike = v != "0" && v != "false"
                    }
                    "color" -> {
                        val v = attr(parser, "val")
                        if (v.length == 6 && v.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
                            textColor = "#$v"
                        }
                    }
                    "highlight" -> {
                        bgColor = mapWordHighlightColor(attr(parser, "val"))
                    }
                    "shd" -> {
                        val fill = attr(parser, "fill")
                        if (bgColor == null && fill.length == 6 && !fill.equals("auto", true) && !fill.equals("ffffff", true)) {
                            bgColor = "#$fill"
                        }
                    }
                    "sz" -> {
                        val halfPts = attr(parser, "val").toIntOrNull()
                        if (halfPts != null && halfPts > 0) {
                            // 1 pt = 1.333 px, halfPts / 2 = pt
                            val px = ((halfPts / 2.0) * 1.333).toInt().coerceIn(10, 64)
                            if (px != 15 && px != 14) fontSizePx = px
                        }
                    }
                    "rFonts" -> {
                        val f = attr(parser, "ascii").ifBlank { attr(parser, "hAnsi") }
                        if (f.isNotBlank()) fontFamily = f
                    }
                    "t" -> {
                        val txt = readElementText(parser)
                        depth-- // readElementText consumes END_TAG
                        content.append(escapeHtml(txt))
                    }
                    "tab" -> content.append("&nbsp;&nbsp;&nbsp;&nbsp;")
                    "br", "cr" -> {
                        val brType = attr(parser, "type").lowercase(Locale.ROOT)
                        if (brType == "page") {
                            content.append("<div class=\"doc-page-break\" contenteditable=\"false\"></div>")
                        } else {
                            content.append("<br>")
                        }
                    }
                    "extent" -> {
                        val cx = attr(parser, "cx").toLongOrNull()
                        if (cx != null && cx > 0) {
                            drawingWidthPx = (cx / 9525L).toInt().coerceIn(64, 720)
                        }
                    }
                    "blip" -> {
                        val embedId = attr(parser, "embed").ifBlank { attr(parser, "link") }
                        if (embedId.isNotBlank()) {
                            val dataUri = resolveImage(embedId)
                            if (dataUri != null) {
                                val wStyle = drawingWidthPx?.let { " style=\"width:${it}px;\"" } ?: ""
                                content.append("<img src=\"").append(dataUri).append("\" data-wrap=\"break\"").append(wStyle).append(">")
                            }
                        }
                    }
                    "imagedata" -> {
                        val rId = attr(parser, "id")
                        if (rId.isNotBlank()) {
                            val dataUri = resolveImage(rId)
                            if (dataUri != null) {
                                content.append("<img src=\"").append(dataUri).append("\" data-wrap=\"break\">")
                            }
                        }
                    }
                }
            } else if (ev == XmlPullParser.END_TAG) {
                depth--
            }
        }

        val raw = content.toString()
        if (raw.isEmpty()) return ""

        val styles = mutableListOf<String>()
        if (textColor != null) styles.add("color:$textColor")
        if (bgColor != null) styles.add("background-color:$bgColor")
        if (fontSizePx != null) styles.add("font-size:${fontSizePx}px")
        if (fontFamily != null) styles.add("font-family:'${escapeHtmlAttr(fontFamily)}',sans-serif")

        var result = raw
        if (styles.isNotEmpty()) result = "<span style=\"${styles.joinToString(";")}\">$result</span>"
        if (bold) result = "<strong>$result</strong>"
        if (italic) result = "<em>$result</em>"
        if (underline) result = "<u>$result</u>"
        if (strike) result = "<s>$result</s>"
        return result
    }

    private fun parseTable(
        parser: XmlPullParser,
        rels: Map<String, RelInfo>,
        orderedNumIds: Set<String>,
        resolveImage: (String) -> String?
    ): String {
        val sb = StringBuilder("<table>")
        var rowIndex = 0
        var depth = 1

        while (depth > 0) {
            val ev = parser.next()
            if (ev == XmlPullParser.END_DOCUMENT) break
            if (ev == XmlPullParser.START_TAG) {
                val name = localName(parser.name)
                depth++
                if (name == "tr") {
                    sb.append(parseTableRow(parser, rowIndex == 0, rels, orderedNumIds, resolveImage))
                    rowIndex++
                    depth-- // consumed </tr >
                }
            } else if (ev == XmlPullParser.END_TAG) {
                depth--
            }
        }

        sb.append("</table>")
        return sb.toString()
    }

    private fun parseTableRow(
        parser: XmlPullParser,
        isFirstRow: Boolean,
        rels: Map<String, RelInfo>,
        orderedNumIds: Set<String>,
        resolveImage: (String) -> String?
    ): String {
        val sb = StringBuilder("<tr>")
        var depth = 1
        while (depth > 0) {
            val ev = parser.next()
            if (ev == XmlPullParser.END_DOCUMENT) break
            if (ev == XmlPullParser.START_TAG) {
                val name = localName(parser.name)
                depth++
                if (name == "tc") {
                    sb.append(parseTableCell(parser, isFirstRow, rels, orderedNumIds, resolveImage))
                    depth--
                }
            } else if (ev == XmlPullParser.END_TAG) {
                depth--
            }
        }
        sb.append("</tr>")
        return sb.toString()
    }

    private fun parseTableCell(
        parser: XmlPullParser,
        isHeaderRow: Boolean,
        rels: Map<String, RelInfo>,
        orderedNumIds: Set<String>,
        resolveImage: (String) -> String?
    ): String {
        var colSpan = 1
        val cellParts = mutableListOf<String>()
        var depth = 1
        while (depth > 0) {
            val ev = parser.next()
            if (ev == XmlPullParser.END_DOCUMENT) break
            if (ev == XmlPullParser.START_TAG) {
                val name = localName(parser.name)
                depth++
                when (name) {
                    "gridSpan" -> colSpan = attr(parser, "val").toIntOrNull() ?: 1
                    "p" -> {
                        val p = parseParagraph(parser, rels, orderedNumIds, resolveImage)
                        cellParts.add(p.innerHtml)
                        depth--
                    }
                }
            } else if (ev == XmlPullParser.END_TAG) {
                depth--
            }
        }
        val tag = if (isHeaderRow) "th" else "td"
        val spanAttr = if (colSpan > 1) " colspan=\"$colSpan\"" else ""
        val joined = cellParts.filter { it.isNotBlank() }.joinToString("<br>").ifBlank { "<br>" }
        return "<$tag$spanAttr>$joined</$tag>"
    }

    private fun parseRelationships(relsBytes: ByteArray): Map<String, RelInfo> {
        val map = mutableMapOf<String, RelInfo>()
        try {
            val parser = Xml.newPullParser().apply {
                setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                setInput(ByteArrayInputStream(relsBytes), "UTF-8")
            }
            var ev = parser.eventType
            while (ev != XmlPullParser.END_DOCUMENT) {
                if (ev == XmlPullParser.START_TAG && localName(parser.name) == "Relationship") {
                    val id = attr(parser, "Id")
                    val target = attr(parser, "Target")
                    val mode = attr(parser, "TargetMode")
                    if (id.isNotBlank() && target.isNotBlank()) {
                        map[id] = RelInfo(target, mode.equals("External", ignoreCase = true))
                    }
                }
                ev = parser.next()
            }
        } catch (_: Exception) {}
        return map
    }

    private fun parseOrderedNumberingIds(numBytes: ByteArray): Set<String> {
        val abstractOrdered = mutableSetOf<String>()
        val numIdsOrdered = mutableSetOf<String>()
        try {
            val parser = Xml.newPullParser().apply {
                setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                setInput(ByteArrayInputStream(numBytes), "UTF-8")
            }
            var currentAbstractId: String? = null
            var currentNumId: String? = null
            var ev = parser.eventType
            while (ev != XmlPullParser.END_DOCUMENT) {
                if (ev == XmlPullParser.START_TAG) {
                    when (localName(parser.name)) {
                        "abstractNum" -> currentAbstractId = attr(parser, "abstractNumId")
                        "numFmt" -> {
                            val fmt = attr(parser, "val").lowercase(Locale.ROOT)
                            if (currentAbstractId != null && fmt != "bullet" && fmt != "none") {
                                abstractOrdered.add(currentAbstractId!!)
                            }
                        }
                        "num" -> currentNumId = attr(parser, "numId")
                        "abstractNumId" -> {
                            val ref = attr(parser, "val")
                            if (currentNumId != null && abstractOrdered.contains(ref)) {
                                numIdsOrdered.add(currentNumId!!)
                            }
                        }
                    }
                } else if (ev == XmlPullParser.END_TAG) {
                    when (localName(parser.name)) {
                        "abstractNum" -> currentAbstractId = null
                        "num" -> currentNumId = null
                    }
                }
                ev = parser.next()
            }
        } catch (_: Exception) {}
        return numIdsOrdered
    }

    private fun mapWordHighlightColor(name: String): String? = when (name.lowercase(Locale.ROOT)) {
        "yellow" -> "#FEF08A"
        "green" -> "#BBF7D0"
        "cyan" -> "#A5F3FC"
        "magenta", "pink" -> "#FBCFE8"
        "blue" -> "#BFDBFE"
        "red" -> "#FECACA"
        "darkyellow" -> "#FDE047"
        "lightgray", "lightgrey" -> "#E2E8F0"
        else -> null
    }

    private fun compressImageBytesToDataUri(raw: ByteArray, path: String): String? {
        return try {
            val isPng = path.endsWith(".png")
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(raw, 0, raw.size, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            val maxDim = 1100
            var sample = 1
            while (bounds.outWidth / sample > maxDim * 2 || bounds.outHeight / sample > maxDim * 2) sample *= 2
            val bmp = BitmapFactory.decodeByteArray(raw, 0, raw.size, BitmapFactory.Options().apply { inSampleSize = sample })
                ?: return null
            val scaled = if (bmp.width > maxDim || bmp.height > maxDim) {
                val r = minOf(maxDim.toFloat() / bmp.width, maxDim.toFloat() / bmp.height)
                Bitmap.createScaledBitmap(bmp, (bmp.width * r).toInt().coerceAtLeast(1), (bmp.height * r).toInt().coerceAtLeast(1), true)
                    .also { if (it !== bmp) bmp.recycle() }
            } else bmp
            val out = ByteArrayOutputStream()
            if (isPng) scaled.compress(Bitmap.CompressFormat.PNG, 92, out)
            else scaled.compress(Bitmap.CompressFormat.JPEG, 84, out)
            scaled.recycle()
            val mime = if (isPng) "image/png" else "image/jpeg"
            "data:$mime;base64,${Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)}"
        } catch (_: Exception) {
            null
        }
    }

    // ── OTHER FORMAT IMPORTERS (HTML / MD / RTF / TXT / LEGACY DOC) ───────────
    private fun extractSafeBodyHtml(rawHtml: String): String {
        val bodyMatch = Regex("<body[^>]*>(.*?)</body>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            .find(rawHtml)
        val inner = bodyMatch?.groupValues?.get(1) ?: rawHtml
        return inner
            .replace(Regex("<script[^>]*>.*?</script>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")
            .replace(Regex("<style[^>]*>.*?</style>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")
            .trim()
            .ifBlank { "<p><br></p>" }
    }

    private fun markdownToHtml(md: String): String {
        val lines = md.replace("\r\n", "\n").split("\n")
        val sb = StringBuilder()
        var activeList: String? = null
        fun closeList() {
            if (activeList != null) {
                sb.append("</$activeList>")
                activeList = null
            }
        }
        for (rawLine in lines) {
            val line = rawLine.trimEnd()
            val trimmed = line.trim()
            when {
                trimmed.isEmpty() -> {
                    closeList()
                }
                trimmed == "---" || trimmed == "***" -> {
                    closeList()
                    sb.append("<hr>")
                }
                trimmed.startsWith("#### ") -> {
                    closeList(); sb.append("<h5>").append(inlineMarkdown(trimmed.drop(5))).append("</h5>")
                }
                trimmed.startsWith("### ") -> {
                    closeList(); sb.append("<h4>").append(inlineMarkdown(trimmed.drop(4))).append("</h4>")
                }
                trimmed.startsWith("## ") -> {
                    closeList(); sb.append("<h3>").append(inlineMarkdown(trimmed.drop(3))).append("</h3>")
                }
                trimmed.startsWith("# ") -> {
                    closeList(); sb.append("<h1>").append(inlineMarkdown(trimmed.drop(2))).append("</h1>")
                }
                trimmed.startsWith("> ") -> {
                    closeList(); sb.append("<blockquote>").append(inlineMarkdown(trimmed.drop(2))).append("</blockquote>")
                }
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    if (activeList != "ul") { closeList(); activeList = "ul"; sb.append("<ul>") }
                    sb.append("<li>").append(inlineMarkdown(trimmed.drop(2))).append("</li>")
                }
                Regex("^\\d+\\.\\s+.*").matches(trimmed) -> {
                    if (activeList != "ol") { closeList(); activeList = "ol"; sb.append("<ol>") }
                    val item = trimmed.replaceFirst(Regex("^\\d+\\.\\s+"), "")
                    sb.append("<li>").append(inlineMarkdown(item)).append("</li>")
                }
                else -> {
                    closeList()
                    sb.append("<p>").append(inlineMarkdown(trimmed)).append("</p>")
                }
            }
        }
        closeList()
        return sb.toString().ifBlank { "<p><br></p>" }
    }

    private fun inlineMarkdown(text: String): String {
        var s = escapeHtml(text)
        s = s.replace(Regex("\\[([^\\]]+)]\\((https?://[^)]+)\\)")) { m ->
            "<a href=\"${m.groupValues[2]}\">${m.groupValues[1]}</a>"
        }
        s = s.replace(Regex("\\*\\*(.+?)\\*\\*"), "<strong>$1</strong>")
        s = s.replace(Regex("__(.+?)__"), "<strong>$1</strong>")
        s = s.replace(Regex("~~(.+?)~~"), "<s>$1</s>")
        s = s.replace(Regex("`([^`]+)`"), "<code>$1</code>")
        s = s.replace(Regex("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)"), "<em>$1</em>")
        return s
    }

    private fun plainTextToHtml(text: String): String {
        return text.replace("\r\n", "\n").split("\n").joinToString("") { line ->
            if (line.isBlank()) "<p><br></p>" else "<p>${escapeHtml(line)}</p>"
        }.ifBlank { "<p><br></p>" }
    }

    private fun rtfToHtml(rtf: String): String {
        val cleaned = rtf
            .replace(Regex("\\{\\\\\\*[^{}]*\\}"), "")
            .replace(Regex("\\{\\\\(?:fonttbl|colortbl|stylesheet|info|pict)[^{}]*(?:\\{[^{}]*\\}[^{}]*)*\\}"), "")
            .replace("\\par", "\n")
            .replace("\\line", "\n")
            .replace("\\tab", "    ")
            .replace(Regex("\\\\'([0-9a-fA-F]{2})")) { m ->
                val code = m.groupValues[1].toIntOrNull(16) ?: 32
                code.toChar().toString()
            }
            .replace(Regex("\\\\[a-zA-Z]+-?\\d* ?"), "")
            .replace("{", "")
            .replace("}", "")
            .trim()
        return plainTextToHtml(cleaned)
    }

    private fun extractLegacyDocToHtml(bytes: ByteArray): String {
        // Fallback reader for binary OLE2 .doc: extract readable UTF-16LE / ASCII runs
        val utf16 = String(bytes, Charset.forName("UTF-16LE"))
        val chunks = Regex("[\\p{L}\\p{N}\\p{P} \\t]{12,}").findAll(utf16)
            .map { it.value.trim() }
            .filter { it.any(Char::isLetter) && !it.contains("Microsoft Word") && !it.contains("Root Entry") }
            .toList()
        if (chunks.isNotEmpty()) {
            return chunks.joinToString("") { "<p>${escapeHtml(it)}</p>" }
        }
        val ascii = String(bytes, Charsets.ISO_8859_1)
        val asciiChunks = Regex("[A-Za-z0-9 ,.;:!?()\\-'\"\\n]{16,}").findAll(ascii)
            .map { it.value.trim() }
            .filter { it.any(Char::isLetter) }
            .toList()
        return if (asciiChunks.isNotEmpty()) {
            asciiChunks.joinToString("") { "<p>${escapeHtml(it)}</p>" }
        } else {
            "<p>Format .doc biner lama terdeteksi. Simpan ulang sebagai .docx untuk mempertahankan seluruh format &amp; tabel.</p>"
        }
    }

    // =========================================================================
    //  EXPORT (HTML -> SPEC-COMPLIANT .DOCX / .HTML / .TXT)
    // =========================================================================
    fun exportToHtmlFile(title: String, rawBodyHtml: String): String {
        val (setup, bodyHtml) = DocPageSetup.extractFromContent(rawBodyHtml)
        val safeTitle = escapeHtml(title.ifBlank { "Dokumen" })
        return """<!DOCTYPE html>
<html lang="id">
<head>
<meta charset="UTF-8">
<title>$safeTitle</title>
<style>
  @page { size: ${setup.effectiveWidthMm}mm ${setup.effectiveHeightMm}mm; margin: ${setup.marginTopCm}cm ${setup.marginRightCm}cm ${setup.marginBottomCm}cm ${setup.marginLeftCm}cm; }
  body { font-family: Inter, Arial, sans-serif; max-width: ${setup.widthPx}px; margin: 40px auto; padding: 0 24px; color: #1E293B; line-height: 1.7; font-size: 15px; }
  table { border-collapse: collapse; width: 100%; margin: 12px 0; }
  td, th { border: 1px solid #CBD5E1; padding: 8px 12px; text-align: left; }
  th { background: #F8FAFC; font-weight: 600; }
  blockquote { border-left: 3px solid #2563EB; padding: 8px 16px; background: #EFF6FF; color: #1D4ED8; margin: 10px 0; }
  img { max-width: 100%; height: auto; }
  .doc-page-break { page-break-after: always; break-after: page; height: 0; border: none; }
</style>
</head>
<body>
$bodyHtml
</body>
</html>"""
    }

    fun exportToPlainText(rawBodyHtml: String): String {
        val (_, bodyHtml) = DocPageSetup.extractFromContent(rawBodyHtml)
        return stripHtmlToPlainText(bodyHtml)
    }

    private data class ExportImage(val relId: String, val fileName: String, val mime: String, val bytes: ByteArray, val widthEmu: Long, val heightEmu: Long)
    private data class ExportLink(val relId: String, val url: String)

    private data class RunStyle(
        val bold: Boolean = false,
        val italic: Boolean = false,
        val underline: Boolean = false,
        val strike: Boolean = false,
        val colorHex: String? = null,
        val bgHex: String? = null,
        val fontSizeHalfPts: Int? = null,
        val fontFamily: String? = null,
        val linkUrl: String? = null
    )

    private data class HtmlNode(
        val tag: String?, // null = text node
        val text: String = "",
        val attrs: Map<String, String> = emptyMap(),
        val children: MutableList<HtmlNode> = mutableListOf()
    )

    fun exportToDocx(rawBodyHtml: String, outputStream: OutputStream) {
        val (pageSetup, cleanBodyHtml) = DocPageSetup.extractFromContent(rawBodyHtml)
        val root = parseHtmlFragment(cleanBodyHtml)
        val images = mutableListOf<ExportImage>()
        val links = mutableListOf<ExportLink>()
        val hasHeader = !pageSetup.isPageless && pageSetup.headerText.isNotBlank()
        val hasFooter = !pageSetup.isPageless && (pageSetup.footerText.isNotBlank() || pageSetup.showPageNumbers)
        var nextRelNum = 3 // rId1=styles, rId2=numbering
        val headerRelId = if (hasHeader) "rId${nextRelNum++}" else null
        val footerRelId = if (hasFooter) "rId${nextRelNum++}" else null
        var nextDocPrId = 1

        fun registerLink(url: String): String {
            val id = "rId${nextRelNum++}"
            links.add(ExportLink(id, url))
            return id
        }

        fun registerImage(src: String, styleAttr: String): ExportImage? {
            if (src.isBlank()) return null
            var bytes: ByteArray? = null
            var isPng = false
            var isWebp = false
            var isGif = false

            if (src.startsWith("data:image/")) {
                val comma = src.indexOf(',')
                if (comma < 0) return null
                val header = src.substring(5, comma).lowercase(Locale.ROOT)
                isPng = header.contains("png")
                isWebp = header.contains("webp")
                isGif = header.contains("gif")
                val cleanB64 = src.substring(comma + 1).replace(Regex("\\s+"), "")
                bytes = try { Base64.decode(cleanB64, Base64.DEFAULT) } catch (_: Exception) { null }
            } else if (src.startsWith("file://") || src.startsWith("/")) {
                val filePath = if (src.startsWith("file://")) src.removePrefix("file://") else src
                val f = java.io.File(filePath)
                if (f.exists() && f.canRead()) {
                    bytes = try { f.readBytes() } catch (_: Exception) { null }
                    val lower = filePath.lowercase(Locale.ROOT)
                    isPng = lower.endsWith(".png")
                    isWebp = lower.endsWith(".webp")
                    isGif = lower.endsWith(".gif")
                }
            }

            if (bytes == null || bytes.isEmpty()) return null

            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
            val natW = opts.outWidth.coerceAtLeast(1)
            val natH = opts.outHeight.coerceAtLeast(1)

            val cssWidthPx = Regex("width\\s*:\\s*(\\d+)px").find(styleAttr)?.groupValues?.get(1)?.toIntOrNull()
            val targetPxW = (cssWidthPx ?: natW.coerceAtMost(520)).coerceIn(60, 580)
            val targetPxH = ((targetPxW.toDouble() / natW) * natH).toInt().coerceAtLeast(30)

            val ext = when {
                isPng -> "png"
                isWebp -> "png"
                isGif -> "gif"
                else -> "jpeg"
            }
            val mime = when {
                isPng -> "image/png"
                isGif -> "image/gif"
                else -> "image/jpeg"
            }
            val id = "rId${nextRelNum++}"
            val img = ExportImage(
                relId = id,
                fileName = "image${images.size + 1}.$ext",
                mime = mime,
                bytes = bytes,
                widthEmu = targetPxW * 9525L,
                heightEmu = targetPxH * 9525L
            )
            images.add(img)
            return img
        }

        val bodyXml = StringBuilder()

        fun buildRunXml(text: String, style: RunStyle): String {
            if (text.isEmpty()) return ""
            val rPr = StringBuilder()
            if (style.bold) rPr.append("<w:b/>")
            if (style.italic) rPr.append("<w:i/>")
            if (style.underline || style.linkUrl != null) rPr.append("<w:u w:val=\"single\"/>")
            if (style.strike) rPr.append("<w:strike/>")
            val effectiveColor = style.colorHex ?: if (style.linkUrl != null) "2563EB" else null
            if (effectiveColor != null) rPr.append("<w:color w:val=\"").append(effectiveColor).append("\"/>")
            if (style.bgHex != null) rPr.append("<w:shd w:val=\"clear\" w:color=\"auto\" w:fill=\"").append(style.bgHex).append("\"/>")
            if (style.fontSizeHalfPts != null) {
                rPr.append("<w:sz w:val=\"").append(style.fontSizeHalfPts).append("\"/>")
                rPr.append("<w:szCs w:val=\"").append(style.fontSizeHalfPts).append("\"/>")
            }
            if (!style.fontFamily.isNullOrBlank()) {
                val f = escapeXml(style.fontFamily)
                rPr.append("<w:rFonts w:ascii=\"$f\" w:hAnsi=\"$f\"/>")
            }
            val rPrBlock = if (rPr.isNotEmpty()) "<w:rPr>$rPr</w:rPr>" else ""
            val run = "<w:r>$rPrBlock<w:t xml:space=\"preserve\">${escapeXml(text)}</w:t></w:r>"
            return if (!style.linkUrl.isNullOrBlank()) {
                val rId = registerLink(style.linkUrl)
                "<w:hyperlink r:id=\"$rId\">$run</w:hyperlink>"
            } else run
        }

        fun buildDrawingXml(img: ExportImage): String {
            val docId = nextDocPrId++
            return """<w:r><w:drawing><wp:inline distT="0" distB="0" distL="0" distR="0"><wp:extent cx="${img.widthEmu}" cy="${img.heightEmu}"/><wp:effectExtent l="0" t="0" r="0" b="0"/><wp:docPr id="$docId" name="${img.fileName}"/><wp:cNvGraphicFramePr><a:graphicFrameLocks xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" noChangeAspect="1"/></wp:cNvGraphicFramePr><a:graphic xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"><a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture"><pic:pic xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture"><pic:nvPicPr><pic:cNvPr id="$docId" name="${img.fileName}"/><pic:cNvPicPr><a:picLocks xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" noChangeAspect="1" noChangeArrowheads="1"/></pic:cNvPicPr></pic:nvPicPr><pic:blipFill><a:blip xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" r:embed="${img.relId}"/><a:stretch><a:fillRect/></a:stretch></pic:blipFill><pic:spPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="${img.widthEmu}" cy="${img.heightEmu}"/></a:xfrm><a:prstGeom prst="rect"><a:avLst/></a:prstGeom></pic:spPr></pic:pic></a:graphicData></a:graphic></wp:inline></w:drawing></w:r>"""
        }

        fun collectInlineRuns(node: HtmlNode, inherited: RunStyle, outRuns: StringBuilder) {
            if (node.tag == null) {
                val decoded = unescapeHtml(node.text)
                if (decoded.isNotEmpty()) outRuns.append(buildRunXml(decoded, inherited))
                return
            }
            val tag = node.tag.lowercase(Locale.ROOT)
            val styleAttr = node.attrs["style"].orEmpty()
            var next = inherited
            when (tag) {
                "strong", "b" -> next = next.copy(bold = true)
                "em", "i" -> next = next.copy(italic = true)
                "u" -> next = next.copy(underline = true)
                "s", "strike", "del" -> next = next.copy(strike = true)
                "a" -> next = next.copy(linkUrl = node.attrs["href"]?.takeIf { it.isNotBlank() })
                "code" -> next = next.copy(fontFamily = "Courier New")
                "br" -> {
                    outRuns.append("<w:r><w:br/></w:r>")
                    return
                }
                "input" -> {
                    if (node.attrs["type"] == "checkbox") {
                        val mark = if (node.attrs.containsKey("checked")) "☑ " else "☐ "
                        outRuns.append(buildRunXml(mark, inherited))
                    }
                    return
                }
                "img" -> {
                    val src = node.attrs["src"].orEmpty()
                    val img = registerImage(src, styleAttr)
                    if (img != null) outRuns.append(buildDrawingXml(img))
                    return
                }
            }

            if (styleAttr.isNotBlank()) {
                extractCssColor(styleAttr, "color")?.let { next = next.copy(colorHex = it) }
                extractCssColor(styleAttr, "background-color")?.let { next = next.copy(bgHex = it) }
                Regex("font-size\\s*:\\s*(\\d+(?:\\.\\d+)?)px", RegexOption.IGNORE_CASE).find(styleAttr)?.let { m ->
                    val px = m.groupValues[1].toDoubleOrNull()
                    if (px != null) {
                        val halfPts = ((px / 1.333) * 2.0).toInt().coerceIn(16, 120)
                        next = next.copy(fontSizeHalfPts = halfPts)
                    }
                }
                Regex("font-family\\s*:\\s*([^;]+)", RegexOption.IGNORE_CASE).find(styleAttr)?.let { m ->
                    val primary = m.groupValues[1].split(',').firstOrNull()?.trim()?.trim('\'', '"')
                    if (!primary.isNullOrBlank()) next = next.copy(fontFamily = primary)
                }
            }
            if (tag == "font") {
                node.attrs["color"]?.let { parseHex6(it) }?.let { next = next.copy(colorHex = it) }
                node.attrs["face"]?.split(',')?.firstOrNull()?.trim()?.trim('\'', '"')
                    ?.takeIf { it.isNotBlank() }?.let { next = next.copy(fontFamily = it) }
            }

            for (child in node.children) {
                collectInlineRuns(child, next, outRuns)
            }
        }

        fun isNodeBlank(node: HtmlNode): Boolean {
            if (node.children.any { it.tag != null && it.tag in setOf("img", "table", "hr", "input") }) return false
            if (node.tag == "img" || node.tag == "table") return false
            val text = unescapeHtml(node.text).replace("\u200B", "").trim()
            if (text.isNotEmpty()) return false
            return node.children.all { isNodeBlank(it) }
        }

        fun emitParagraph(
            node: HtmlNode,
            pStyle: String? = null,
            numId: Int? = null,
            isQuote: Boolean = false,
            baseStyle: RunStyle = RunStyle()
        ) {
            if (isNodeBlank(node)) {
                bodyXml.append("<w:p><w:pPr><w:spacing w:after=\"0\" w:line=\"276\" w:lineRule=\"auto\"/></w:pPr></w:p>")
                return
            }

            val pPr = StringBuilder()
            if (pStyle != null) pPr.append("<w:pStyle w:val=\"$pStyle\"/>")
            if (numId != null) pPr.append("<w:numPr><w:ilvl w:val=\"0\"/><w:numId w:val=\"$numId\"/></w:numPr>")
            if (isQuote) {
                pPr.append("<w:ind w:left=\"480\"/><w:pBdr><w:left w:val=\"single\" w:sz=\"18\" w:space=\"8\" w:color=\"2563EB\"/></w:pBdr>")
            }
            val styleAttr = node.attrs["style"].orEmpty()
            val alignAttr = node.attrs["align"]?.lowercase(Locale.ROOT)
                ?: Regex("text-align\\s*:\\s*(left|center|right|justify)", RegexOption.IGNORE_CASE)
                    .find(styleAttr)?.groupValues?.get(1)?.lowercase(Locale.ROOT)
            when (alignAttr) {
                "center" -> pPr.append("<w:jc w:val=\"center\"/>")
                "right" -> pPr.append("<w:jc w:val=\"right\"/>")
                "justify" -> pPr.append("<w:jc w:val=\"both\"/>")
            }

            // Paragraph spacing (margin-top/margin-bottom & line-height)
            val mtPx = Regex("margin-top\\s*:\\s*(\\d+(?:\\.\\d+)?)px", RegexOption.IGNORE_CASE)
                .find(styleAttr)?.groupValues?.get(1)?.toDoubleOrNull()
            val mbPx = Regex("margin-bottom\\s*:\\s*(\\d+(?:\\.\\d+)?)px", RegexOption.IGNORE_CASE)
                .find(styleAttr)?.groupValues?.get(1)?.toDoubleOrNull()
            val spaceBeforeTw = mtPx?.let { (it * 15.0).toInt().coerceIn(0, 4000) }
            val spaceAfterTw = mbPx?.let { (it * 15.0).toInt().coerceIn(0, 4000) }

            val lineMult = Regex("line-height\\s*:\\s*(\\d+(?:\\.\\d+)?)", RegexOption.IGNORE_CASE)
                .find(styleAttr)?.groupValues?.get(1)?.toDoubleOrNull()
            val lineVal = if (lineMult != null && lineMult in 0.8..3.5) (lineMult * 240.0).toInt() else 276

            val spacingSb = StringBuilder()
            if (spaceBeforeTw != null && spaceBeforeTw > 0) spacingSb.append(" w:before=\"$spaceBeforeTw\"")
            if (spaceAfterTw != null) {
                spacingSb.append(" w:after=\"$spaceAfterTw\"")
            } else if (pStyle == null) {
                spacingSb.append(" w:after=\"0\"")
            }
            spacingSb.append(" w:line=\"$lineVal\" w:lineRule=\"auto\"")
            pPr.append("<w:spacing$spacingSb/>")

            // Indentation (margin-left px & text-indent cm/px -> OOXML twips)
            if (!isQuote) {
                val leftPx = Regex("margin-left\\s*:\\s*(\\d+(?:\\.\\d+)?)px", RegexOption.IGNORE_CASE)
                    .find(styleAttr)?.groupValues?.get(1)?.toDoubleOrNull()
                val firstIndentCm = Regex("text-indent\\s*:\\s*(\\d+(?:\\.\\d+)?)cm", RegexOption.IGNORE_CASE)
                    .find(styleAttr)?.groupValues?.get(1)?.toDoubleOrNull()
                val firstIndentPx = Regex("text-indent\\s*:\\s*(\\d+(?:\\.\\d+)?)px", RegexOption.IGNORE_CASE)
                    .find(styleAttr)?.groupValues?.get(1)?.toDoubleOrNull()
                val leftTwips = leftPx?.let { (it * 15.0).toInt().coerceIn(0, 6000) }
                val firstTwips = firstIndentCm?.let { (it * 566.93).toInt().coerceIn(0, 3000) }
                    ?: firstIndentPx?.let { (it * 15.0).toInt().coerceIn(0, 3000) }
                if (leftTwips != null || firstTwips != null) {
                    pPr.append("<w:ind")
                    if (leftTwips != null && leftTwips > 0) pPr.append(" w:left=\"$leftTwips\"")
                    if (firstTwips != null && firstTwips > 0) pPr.append(" w:firstLine=\"$firstTwips\"")
                    pPr.append("/>")
                }
            }

            val runs = StringBuilder()
            for (child in node.children) {
                collectInlineRuns(child, baseStyle, runs)
            }
            val pPrBlock = if (pPr.isNotEmpty()) "<w:pPr>$pPr</w:pPr>" else ""
            bodyXml.append("<w:p>").append(pPrBlock).append(runs).append("</w:p>")
        }

        fun emitBlocks(nodes: List<HtmlNode>) {
            // Trim trailing empty paragraphs/whitespace to prevent ghost blank pages
            val trimmedNodes = nodes.toMutableList()
            while (trimmedNodes.isNotEmpty() && isNodeBlank(trimmedNodes.last())) {
                trimmedNodes.removeAt(trimmedNodes.lastIndex)
            }

            var skipNextIfEmpty = false
            var idx = 0
            while (idx < trimmedNodes.size) {
                val node = trimmedNodes[idx]

                if (node.tag == null) {
                    val txt = unescapeHtml(node.text).replace("\u200B", "").trim()
                    if (txt.isNotEmpty()) {
                        bodyXml.append("<w:p>").append(buildRunXml(txt, RunStyle())).append("</w:p>")
                        skipNextIfEmpty = false
                    }
                    idx++
                    continue
                }

                val tag = node.tag.lowercase(Locale.ROOT)
                val isPb = (tag == "p" || tag == "div") && node.attrs["class"]?.contains("doc-page-break") == true

                if (isPb) {
                    bodyXml.append("<w:p><w:r><w:br w:type=\"page\"/></w:r></w:p>")
                    skipNextIfEmpty = true // Skip any placeholder <p><br></p> right after a page break
                    idx++
                    continue
                }

                if (skipNextIfEmpty) {
                    if (isNodeBlank(node)) {
                        // Skip placeholder paragraph so content starts directly at top of new page
                        idx++
                        continue
                    }
                    skipNextIfEmpty = false
                }

                // Check for a run of blank paragraphs (e.g. repeated Enters to reach the next page)
                if (isNodeBlank(node)) {
                    var blankCount = 0
                    var lookAhead = idx
                    while (lookAhead < trimmedNodes.size && isNodeBlank(trimmedNodes[lookAhead])) {
                        blankCount++
                        lookAhead++
                    }
                    if (idx == 0 && blankCount >= 4) {
                        // Leading run of blank paragraphs at start of document: skip them
                        idx = lookAhead
                        continue
                    }
                    if (blankCount >= 4) {
                        // 4 or more consecutive blank lines indicate a user manually pushing to the next page
                        bodyXml.append("<w:p><w:r><w:br w:type=\"page\"/></w:r></w:p>")
                        skipNextIfEmpty = true
                        idx = lookAhead
                        continue
                    } else {
                        for (b in 0 until blankCount) {
                            emitParagraph(trimmedNodes[idx + b])
                        }
                        idx = lookAhead
                        continue
                    }
                }

                when (tag) {
                    "h1" -> emitParagraph(node, pStyle = "Title")
                    "h2" -> emitParagraph(node, pStyle = "Heading1")
                    "h3" -> emitParagraph(node, pStyle = "Heading2")
                    "h4" -> emitParagraph(node, pStyle = "Heading3")
                    "h5", "h6" -> emitParagraph(node, pStyle = "Heading4")
                    "blockquote" -> emitParagraph(node, isQuote = true, baseStyle = RunStyle(italic = true, colorHex = "1D4ED8"))
                    "pre" -> emitParagraph(node, baseStyle = RunStyle(fontFamily = "Courier New", fontSizeHalfPts = 20))
                    "hr" -> bodyXml.append("<w:p><w:pPr><w:pBdr><w:bottom w:val=\"single\" w:sz=\"6\" w:space=\"1\" w:color=\"CBD5E1\"/></w:pBdr></w:pPr></w:p>")
                    "ul" -> {
                        val isChecklist = node.attrs["class"]?.contains("checklist") == true
                        for (li in node.children.filter { it.tag.equals("li", true) }) {
                            emitParagraph(li, numId = if (isChecklist) null else 1)
                        }
                    }
                    "ol" -> {
                        for (li in node.children.filter { it.tag.equals("li", true) }) {
                            emitParagraph(li, numId = 2)
                        }
                    }
                    "table" -> {
                        bodyXml.append("""<w:tbl><w:tblPr><w:tblW w:w="5000" w:type="pct"/><w:tblBorders><w:top w:val="single" w:sz="4" w:space="0" w:color="CBD5E1"/><w:left w:val="single" w:sz="4" w:space="0" w:color="CBD5E1"/><w:bottom w:val="single" w:sz="4" w:space="0" w:color="CBD5E1"/><w:right w:val="single" w:sz="4" w:space="0" w:color="CBD5E1"/><w:insideH w:val="single" w:sz="4" w:space="0" w:color="CBD5E1"/><w:insideV w:val="single" w:sz="4" w:space="0" w:color="CBD5E1"/></w:tblBorders></w:tblPr>""")
                        val rows = mutableListOf<HtmlNode>()
                        fun collectRows(n: HtmlNode) {
                            for (c in n.children) {
                                if (c.tag.equals("tr", true)) rows.add(c)
                                else if (c.tag.equals("tbody", true) || c.tag.equals("thead", true)) collectRows(c)
                            }
                        }
                        collectRows(node)
                        for (tr in rows) {
                            bodyXml.append("<w:tr>")
                            for (cell in tr.children.filter { it.tag.equals("td", true) || it.tag.equals("th", true) }) {
                                val isTh = cell.tag.equals("th", true)
                                val shd = if (isTh) """<w:shd w:val="clear" w:color="auto" w:fill="F1F5F9"/>""" else ""
                                bodyXml.append("<w:tc><w:tcPr>$shd</w:tcPr>")
                                emitParagraph(cell, baseStyle = RunStyle(bold = isTh))
                                bodyXml.append("</w:tc>")
                            }
                            bodyXml.append("</w:tr>")
                        }
                        bodyXml.append("</w:tbl>")
                    }
                    "img" -> {
                        val img = registerImage(node.attrs["src"].orEmpty(), node.attrs["style"].orEmpty())
                        if (img != null) {
                            bodyXml.append("<w:p><w:pPr><w:jc w:val=\"center\"/></w:pPr>").append(buildDrawingXml(img)).append("</w:p>")
                        }
                    }
                    "p", "div" -> {
                        val hasNestedBlocks = node.children.any { it.tag?.lowercase(Locale.ROOT) in setOf("p", "div", "table", "ul", "ol", "h1", "h2", "h3", "h4") }
                        if (hasNestedBlocks) emitBlocks(node.children)
                        else emitParagraph(node)
                    }
                    else -> {
                        val wrapper = HtmlNode("p", children = mutableListOf(node))
                        emitParagraph(wrapper)
                    }
                }
                idx++
            }
        }

        emitBlocks(root.children)
        if (bodyXml.isEmpty()) bodyXml.append("<w:p/>")

        // Build ZIP archive
        ZipOutputStream(outputStream).use { zos ->
            fun putXml(path: String, xml: String) {
                zos.putNextEntry(ZipEntry(path))
                zos.write(xml.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }

            // 1. [Content_Types].xml
            val extraOverrides = StringBuilder()
            if (hasHeader) extraOverrides.append("""  <Override PartName="/word/header1.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.header+xml"/>""" + "\n")
            if (hasFooter) extraOverrides.append("""  <Override PartName="/word/footer1.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.footer+xml"/>""" + "\n")
            putXml(
                "[Content_Types].xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Default Extension="png" ContentType="image/png"/>
  <Default Extension="jpeg" ContentType="image/jpeg"/>
  <Default Extension="jpg" ContentType="image/jpeg"/>
  <Default Extension="gif" ContentType="image/gif"/>
  <Default Extension="webp" ContentType="image/webp"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
  <Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
  <Override PartName="/word/numbering.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.numbering+xml"/>
$extraOverrides</Types>"""
            )

            // 2. _rels/.rels
            putXml(
                "_rels/.rels",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""
            )

            // 3. word/_rels/document.xml.rels
            val relsSb = StringBuilder()
            relsSb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""")
            relsSb.append("""<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>""")
            relsSb.append("""<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/numbering" Target="numbering.xml"/>""")
            if (headerRelId != null) {
                relsSb.append("""<Relationship Id="$headerRelId" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/header" Target="header1.xml"/>""")
            }
            if (footerRelId != null) {
                relsSb.append("""<Relationship Id="$footerRelId" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/footer" Target="footer1.xml"/>""")
            }
            for (img in images) {
                relsSb.append("""<Relationship Id="${img.relId}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="media/${img.fileName}"/>""")
            }
            for (lnk in links) {
                relsSb.append("""<Relationship Id="${lnk.relId}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/hyperlink" Target="${escapeXml(lnk.url)}" TargetMode="External"/>""")
            }
            relsSb.append("</Relationships>")
            putXml("word/_rels/document.xml.rels", relsSb.toString())

            // 4. word/styles.xml
            putXml(
                "word/styles.xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:style w:type="paragraph" w:default="1" w:styleId="Normal"><w:name w:val="Normal"/><w:pPr><w:spacing w:after="0" w:line="276" w:lineRule="auto"/></w:pPr><w:rPr><w:rFonts w:ascii="Calibri" w:hAnsi="Calibri"/><w:sz w:val="22"/><w:color w:val="1E293B"/></w:rPr></w:style>
  <w:style w:type="paragraph" w:styleId="Title"><w:name w:val="Title"/><w:basedOn w:val="Normal"/><w:pPr><w:spacing w:before="200" w:after="140"/></w:pPr><w:rPr><w:b/><w:sz w:val="44"/></w:rPr></w:style>
  <w:style w:type="paragraph" w:styleId="Heading1"><w:name w:val="heading 1"/><w:basedOn w:val="Normal"/><w:pPr><w:spacing w:before="220" w:after="100"/></w:pPr><w:rPr><w:b/><w:sz w:val="34"/></w:rPr></w:style>
  <w:style w:type="paragraph" w:styleId="Heading2"><w:name w:val="heading 2"/><w:basedOn w:val="Normal"/><w:pPr><w:spacing w:before="180" w:after="80"/></w:pPr><w:rPr><w:b/><w:sz w:val="28"/></w:rPr></w:style>
  <w:style w:type="paragraph" w:styleId="Heading3"><w:name w:val="heading 3"/><w:basedOn w:val="Normal"/><w:pPr><w:spacing w:before="140" w:after="60"/></w:pPr><w:rPr><w:b/><w:sz w:val="24"/></w:rPr></w:style>
  <w:style w:type="paragraph" w:styleId="Heading4"><w:name w:val="heading 4"/><w:basedOn w:val="Normal"/><w:pPr><w:spacing w:before="120" w:after="40"/></w:pPr><w:rPr><w:b/><w:sz w:val="22"/></w:rPr></w:style>
</w:styles>"""
            )

            // 5. word/numbering.xml (numId=1 bullet, numId=2 decimal)
            putXml(
                "word/numbering.xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:numbering xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:abstractNum w:abstractNumId="0"><w:lvl w:ilvl="0"><w:start w:val="1"/><w:numFmt w:val="bullet"/><w:lvlText w:val="•"/><w:lvlJc w:val="left"/><w:pPr><w:ind w:left="720" w:hanging="360"/></w:pPr></w:lvl></w:abstractNum>
  <w:abstractNum w:abstractNumId="1"><w:lvl w:ilvl="0"><w:start w:val="1"/><w:numFmt w:val="decimal"/><w:lvlText w:val="%1."/><w:lvlJc w:val="left"/><w:pPr><w:ind w:left="720" w:hanging="360"/></w:pPr></w:lvl></w:abstractNum>
  <w:num w:numId="1"><w:abstractNumId w:val="0"/></w:num>
  <w:num w:numId="2"><w:abstractNumId w:val="1"/></w:num>
</w:numbering>"""
            )

            // 5b. word/header1.xml & word/footer1.xml
            if (hasHeader) {
                putXml(
                    "word/header1.xml",
                    """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:hdr xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:p><w:pPr><w:jc w:val="right"/></w:pPr><w:r><w:rPr><w:sz w:val="18"/><w:color w:val="64748B"/></w:rPr><w:t>${escapeXml(pageSetup.headerText)}</w:t></w:r></w:p>
</w:hdr>"""
                )
            }
            if (hasFooter) {
                val ftrRuns = StringBuilder()
                if (pageSetup.footerText.isNotBlank()) {
                    ftrRuns.append("""<w:r><w:rPr><w:sz w:val="18"/><w:color w:val="64748B"/></w:rPr><w:t xml:space="preserve">${escapeXml(pageSetup.footerText)}</w:t></w:r>""")
                    if (pageSetup.showPageNumbers) {
                        ftrRuns.append("""<w:r><w:rPr><w:sz w:val="18"/><w:color w:val="64748B"/></w:rPr><w:t xml:space="preserve">  ·  Hal. </w:t></w:r>""")
                    }
                }
                if (pageSetup.showPageNumbers) {
                    ftrRuns.append("""<w:fldSimple w:instr="PAGE"><w:r><w:rPr><w:sz w:val="18"/><w:color w:val="64748B"/></w:rPr><w:t>1</w:t></w:r></w:fldSimple>""")
                }
                putXml(
                    "word/footer1.xml",
                    """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:ftr xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:p><w:pPr><w:jc w:val="center"/></w:pPr>$ftrRuns</w:p>
</w:ftr>"""
                )
            }

            // 6. word/media/*
            for (img in images) {
                zos.putNextEntry(ZipEntry("word/media/${img.fileName}"))
                zos.write(img.bytes)
                zos.closeEntry()
            }

            // 7. word/document.xml (with exact paper size, orientation, margins, header/footer refs)
            val orientXml = if (pageSetup.orientation == DocPageOrientation.LANDSCAPE) """ w:orient="landscape"""" else ""
            val hdrRefXml = if (headerRelId != null) """<w:headerReference w:type="default" r:id="$headerRelId"/>""" else ""
            val ftrRefXml = if (footerRelId != null) """<w:footerReference w:type="default" r:id="$footerRelId"/>""" else ""
            putXml(
                "word/document.xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing">
  <w:body>
    $bodyXml
    <w:sectPr>$hdrRefXml$ftrRefXml<w:pgSz w:w="${pageSetup.widthTwips}" w:h="${pageSetup.heightTwips}"$orientXml/><w:pgMar w:top="${pageSetup.marginTopTwips}" w:right="${pageSetup.marginRightTwips}" w:bottom="${pageSetup.marginBottomTwips}" w:left="${pageSetup.marginLeftTwips}"/></w:sectPr>
  </w:body>
</w:document>"""
            )
        }
    }

    // ── LIGHTWEIGHT HTML FRAGMENT PARSER ──────────────────────────────────────
    private val VOID_TAGS = setOf("br", "hr", "img", "input", "meta", "link")

    private fun parseHtmlFragment(html: String): HtmlNode {
        val cleanHtml = html.replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "")
        val root = HtmlNode("root")
        val stack = ArrayDeque<HtmlNode>().apply { addLast(root) }
        val tagRegex = Regex("<(/?)([a-zA-Z0-9]+)([^>]*)>")
        var pos = 0

        for (m in tagRegex.findAll(cleanHtml)) {
            if (m.range.first > pos) {
                val txt = cleanHtml.substring(pos, m.range.first)
                if (txt.isNotEmpty()) stack.last().children.add(HtmlNode(null, text = txt))
            }
            val isClosing = m.groupValues[1] == "/"
            val tagName = m.groupValues[2].lowercase(Locale.ROOT)
            val attrStr = m.groupValues[3]
            val selfClosing = attrStr.trimEnd().endsWith("/") || tagName in VOID_TAGS

            if (isClosing) {
                // Pop until matching tag if present in stack
                val matchIdx = stack.indexOfLast { it.tag == tagName }
                if (matchIdx > 0) {
                    while (stack.size > matchIdx) stack.removeLast()
                }
            } else {
                val attrs = parseHtmlAttrs(attrStr)
                val node = HtmlNode(tagName, attrs = attrs)
                stack.last().children.add(node)
                if (!selfClosing) stack.addLast(node)
            }
            pos = m.range.last + 1
        }
        if (pos < cleanHtml.length) {
            val tail = cleanHtml.substring(pos)
            if (tail.isNotEmpty()) stack.last().children.add(HtmlNode(null, text = tail))
        }
        return root
    }

    private fun parseHtmlAttrs(attrStr: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val regex = Regex("""([a-zA-Z0-9_-]+)(?:\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s"'=<>`]+)))?""")
        for (m in regex.findAll(attrStr)) {
            val k = m.groupValues[1].lowercase(Locale.ROOT)
            val v = m.groupValues[2].ifEmpty { m.groupValues[3].ifEmpty { m.groupValues[4] } }
            map[k] = v
        }
        return map
    }

    // ── COLOR & XML UTILS ─────────────────────────────────────────────────────
    private fun extractCssColor(styleAttr: String, prop: String): String? {
        val m = Regex("(?<![a-zA-Z-])$prop\\s*:\\s*([^;]+)", RegexOption.IGNORE_CASE).find(styleAttr) ?: return null
        val v = m.groupValues[1].trim()
        if (v.equals("transparent", true)) return null
        parseHex6(v)?.let { return it }
        val rgb = Regex("rgb\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*\\)", RegexOption.IGNORE_CASE).find(v)
        if (rgb != null) {
            val r = rgb.groupValues[1].toInt().coerceIn(0, 255)
            val g = rgb.groupValues[2].toInt().coerceIn(0, 255)
            val b = rgb.groupValues[3].toInt().coerceIn(0, 255)
            return String.format(Locale.ROOT, "%02X%02X%02X", r, g, b)
        }
        return null
    }

    private fun parseHex6(raw: String): String? {
        val h = raw.trim().removePrefix("#")
        if (h.length == 6 && h.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) return h.uppercase(Locale.ROOT)
        if (h.length == 3 && h.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
            return "${h[0]}${h[0]}${h[1]}${h[1]}${h[2]}${h[2]}".uppercase(Locale.ROOT)
        }
        return null
    }

    private fun localName(raw: String?): String = raw?.substringAfter(':') ?: ""

    private fun attr(parser: XmlPullParser, local: String): String {
        for (i in 0 until parser.attributeCount) {
            if (localName(parser.getAttributeName(i)) == local) {
                return parser.getAttributeValue(i) ?: ""
            }
        }
        return ""
    }

    private fun readElementText(parser: XmlPullParser): String {
        val sb = StringBuilder()
        var ev = parser.next()
        while (ev != XmlPullParser.END_TAG && ev != XmlPullParser.END_DOCUMENT) {
            if (ev == XmlPullParser.TEXT || ev == XmlPullParser.CDSECT) {
                sb.append(parser.text ?: "")
            }
            ev = parser.next()
        }
        return sb.toString()
    }

    private fun stripHtmlToPlainText(html: String): String {
        return unescapeHtml(
            html.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
                .replace(Regex("</(p|div|h[1-6]|li|tr|blockquote)>", RegexOption.IGNORE_CASE), "\n")
                .replace(Regex("<[^>]+>"), "")
        ).replace(Regex("\n{3,}"), "\n\n").trim()
    }

    private fun escapeHtml(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

    private fun escapeHtmlAttr(s: String): String = escapeHtml(s).replace("\"", "&quot;")

    private fun escapeXml(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    private fun unescapeHtml(s: String): String = s
        .replace("&nbsp;", " ")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&#39;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&amp;", "&")
}
