package com.lernspace.app.core.document

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.provider.OpenableColumns
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

data class DocumentConversionResult(
    val pdfFile: File,
    val pageCount: Int,
    val extractedText: String,
    val title: String,
    val originalFileName: String
)

sealed class ParsedDocumentBlock {
    data class Title(val text: String) : ParsedDocumentBlock()
    data class Heading(val level: Int, val text: String) : ParsedDocumentBlock()
    data class Paragraph(val text: String) : ParsedDocumentBlock()
    data class BulletItem(val text: String, val indentLevel: Int = 1) : ParsedDocumentBlock()
    data class CodeBlock(val text: String) : ParsedDocumentBlock()
    data class SlideCard(val slideNumber: Int, val title: String, val bullets: List<String>) : ParsedDocumentBlock()
}

/**
 * Universal document converter & text extractor for Lernspace.
 * Converts Microsoft Word (.docx), PowerPoint (.pptx), Markdown (.md), and Plain Text (.txt)
 * into high-resolution, vector-crisp PDF documents using Android's native PdfDocument engine.
 * 
 * Also extracts clean structured text suitable for Gemini & On-Device SLM Quiz/Flashcard generation.
 */
object DocumentToPdfConverter {

    private const val TAG = "DocToPdfConverter"

    // Standard high-DPI A4 Portrait Dimensions (matching Lernspace Canvas aspect ratio)
    const val A4_WIDTH = 1400
    const val A4_HEIGHT = 1980

    // Standard 16:9 Landscape Dimensions for Slide Presentations (PPTX)
    const val SLIDE_WIDTH = 1920
    const val SLIDE_HEIGHT = 1080

    fun isSupportedNonPdfDocument(fileName: String, mimeType: String? = null): Boolean {
        val lowerName = fileName.lowercase(Locale.ROOT)
        val lowerMime = mimeType?.lowercase(Locale.ROOT).orEmpty()
        return lowerName.endsWith(".docx") ||
                lowerName.endsWith(".doc") ||
                lowerName.endsWith(".pptx") ||
                lowerName.endsWith(".ppt") ||
                lowerName.endsWith(".txt") ||
                lowerName.endsWith(".md") ||
                lowerName.endsWith(".csv") ||
                lowerName.endsWith(".rtf") ||
                lowerMime.contains("wordprocessingml") ||
                lowerMime.contains("msword") ||
                lowerMime.contains("presentationml") ||
                lowerMime.contains("ms-powerpoint") ||
                lowerMime.startsWith("text/")
    }

    fun isPdf(fileName: String, mimeType: String? = null): Boolean {
        val lowerName = fileName.lowercase(Locale.ROOT)
        val lowerMime = mimeType?.lowercase(Locale.ROOT).orEmpty()
        return lowerName.endsWith(".pdf") || lowerMime.contains("pdf")
    }

    fun isDocx(fileName: String, mimeType: String? = null): Boolean {
        val lowerName = fileName.lowercase(Locale.ROOT)
        val lowerMime = mimeType?.lowercase(Locale.ROOT).orEmpty()
        return lowerName.endsWith(".docx") ||
                lowerName.endsWith(".doc") ||
                lowerMime.contains("wordprocessingml.document") ||
                lowerMime.contains("msword")
    }

    fun isPptx(fileName: String, mimeType: String? = null): Boolean {
        val lowerName = fileName.lowercase(Locale.ROOT)
        val lowerMime = mimeType?.lowercase(Locale.ROOT).orEmpty()
        return lowerName.endsWith(".pptx") ||
                lowerName.endsWith(".ppt") ||
                lowerMime.contains("presentationml.presentation") ||
                lowerMime.contains("ms-powerpoint")
    }

    fun isTextDoc(fileName: String, mimeType: String? = null): Boolean {
        val lowerName = fileName.lowercase(Locale.ROOT)
        val lowerMime = mimeType?.lowercase(Locale.ROOT).orEmpty()
        return lowerName.endsWith(".txt") ||
                lowerName.endsWith(".md") ||
                lowerName.endsWith(".csv") ||
                lowerName.endsWith(".rtf") ||
                lowerMime.startsWith("text/")
    }

    /**
     * Converts a non-PDF document (DOCX, PPTX, TXT, MD) to a PDF file at [targetFile].
     * Returns metadata including page count and full extracted text for AI.
     */
    suspend fun convertDocumentToPdf(
        context: Context,
        uri: Uri,
        targetFile: File
    ): Result<DocumentConversionResult> = withContext(Dispatchers.IO) {
        try {
            val fileName = getFileName(context, uri)
            val mimeType = context.contentResolver.getType(uri)
            val baseTitle = fileName.substringBeforeLast(".")

            targetFile.parentFile?.mkdirs()

            when {
                // Microsoft Word (.docx)
                isDocx(fileName, mimeType) -> {
                    convertDocxToPdf(context, uri, targetFile, baseTitle, fileName)
                }

                // Microsoft PowerPoint (.pptx)
                isPptx(fileName, mimeType) -> {
                    convertPptxToPdf(context, uri, targetFile, baseTitle, fileName)
                }

                // Plain Text / Markdown / CSV
                isTextDoc(fileName, mimeType) -> {
                    convertTextToPdf(context, uri, targetFile, baseTitle, fileName)
                }

                // Legacy .doc / .ppt / other fallback
                else -> {
                    convertFallbackToPdf(context, uri, targetFile, baseTitle, fileName)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal mengonversi dokumen ke PDF: ${e.message}", e)
            Result.failure(e)
        }
    }

    // =========================================================================
    // DOCX CONVERSION & EXTRACTION
    // =========================================================================

    suspend fun extractTextFromDocx(context: Context, uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val blocks = parseDocxBlocks(context, uri)
            if (blocks.isNotEmpty()) {
                val sb = StringBuilder()
                blocks.forEach { block ->
                    when (block) {
                        is ParsedDocumentBlock.Title -> sb.append(block.text).append("\n\n")
                        is ParsedDocumentBlock.Heading -> sb.append("\n").append("#".repeat(block.level)).append(" ").append(block.text).append("\n\n")
                        is ParsedDocumentBlock.Paragraph -> sb.append(block.text).append("\n\n")
                        is ParsedDocumentBlock.BulletItem -> sb.append("• ").append(block.text).append("\n")
                        is ParsedDocumentBlock.CodeBlock -> sb.append("```\n").append(block.text).append("\n```\n\n")
                        is ParsedDocumentBlock.SlideCard -> {}
                    }
                }
                val text = sb.toString().trim()
                if (text.isNotBlank()) return@withContext Result.success(text)
            }

            // Fallback: legacy .doc or raw stream extraction
            val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (rawBytes != null) {
                val fallbackText = extractPrintableText(rawBytes)
                if (fallbackText.isNotBlank()) {
                    return@withContext Result.success(fallbackText)
                }
            }

            Result.failure(IllegalStateException("Dokumen Word tidak memiliki teks yang terbaca."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun convertDocxToPdf(
        context: Context,
        uri: Uri,
        targetFile: File,
        baseTitle: String,
        fileName: String
    ): Result<DocumentConversionResult> {
        var blocks = parseDocxBlocks(context, uri)
        if (blocks.isEmpty()) {
            // Check if fallback raw extraction works (e.g. for .doc binary format)
            val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (rawBytes != null) {
                val fallbackText = extractPrintableText(rawBytes)
                if (fallbackText.isNotBlank()) {
                    blocks = parseTextToBlocks(fallbackText, baseTitle)
                }
            }
        }

        if (blocks.isEmpty()) {
            return Result.failure(IllegalStateException("Dokumen Word kosong atau tidak dapat dibaca."))
        }

        val extractedText = buildString {
            blocks.forEach { b ->
                when (b) {
                    is ParsedDocumentBlock.Title -> appendLine(b.text).appendLine()
                    is ParsedDocumentBlock.Heading -> appendLine("\n${b.text}").appendLine()
                    is ParsedDocumentBlock.Paragraph -> appendLine(b.text).appendLine()
                    is ParsedDocumentBlock.BulletItem -> appendLine("• ${b.text}")
                    is ParsedDocumentBlock.CodeBlock -> appendLine(b.text).appendLine()
                    else -> {}
                }
            }
        }.trim()

        val pageCount = renderDocumentBlocksToPdf(
            blocks = blocks,
            docTitle = baseTitle,
            targetFile = targetFile,
            pageWidth = A4_WIDTH,
            pageHeight = A4_HEIGHT
        )

        return Result.success(
            DocumentConversionResult(
                pdfFile = targetFile,
                pageCount = maxOf(1, pageCount),
                extractedText = extractedText,
                title = baseTitle,
                originalFileName = fileName
            )
        )
    }

    private fun parseDocxBlocks(context: Context, uri: Uri): List<ParsedDocumentBlock> {
        val blocks = mutableListOf<ParsedDocumentBlock>()
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return emptyList()
            ZipInputStream(inputStream).use { zip ->
                var entry: ZipEntry? = zip.nextEntry
                while (entry != null) {
                    val entryName = entry.name.replace('\\', '/').removePrefix("/")
                    if (entryName.equals("word/document.xml", ignoreCase = true) || entryName.endsWith("/document.xml", ignoreCase = true)) {
                        val bos = ByteArrayOutputStream()
                        val buf = ByteArray(8192)
                        var r: Int
                        while (zip.read(buf).also { r = it } != -1) {
                            bos.write(buf, 0, r)
                        }
                        val xmlBytes = bos.toByteArray()
                        parseWordDocumentXml(xmlBytes, blocks)
                        break
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Gagal membuka zip DOCX", e)
        }
        return blocks
    }

    private fun parseWordDocumentXml(xmlBytes: ByteArray, outBlocks: MutableList<ParsedDocumentBlock>) {
        try {
            val parser = Xml.newPullParser()
            parser.setInput(xmlBytes.inputStream(), "UTF-8")

            var eventType = parser.eventType
            var inParagraph = false
            var inT = false
            var currentStyle: String? = null
            var currentNumPr = false
            val currentParagraphText = StringBuilder()

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val rawName = parser.name ?: ""
                val name = rawName.substringAfter(':')

                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (name) {
                            "p" -> {
                                inParagraph = true
                                currentStyle = null
                                currentNumPr = false
                                currentParagraphText.setLength(0)
                            }
                            "pStyle" -> {
                                currentStyle = parser.getAttributeValue(null, "val")
                                    ?: parser.getAttributeValue("http://schemas.openxmlformats.org/wordprocessingml/2006/main", "val")
                            }
                            "numPr" -> {
                                currentNumPr = true
                            }
                            "tab" -> {
                                if (inParagraph) currentParagraphText.append("    ")
                            }
                            "br" -> {
                                if (inParagraph) currentParagraphText.append("\n")
                            }
                            "t" -> {
                                inT = true
                            }
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inT && inParagraph) {
                            currentParagraphText.append(parser.text)
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        when (name) {
                            "t" -> inT = false
                            "p" -> {
                                if (inParagraph) {
                                    val text = currentParagraphText.toString().trim()
                                    if (text.isNotEmpty()) {
                                        val styleLower = currentStyle?.lowercase(Locale.ROOT).orEmpty()
                                        when {
                                            styleLower.contains("title") -> {
                                                outBlocks.add(ParsedDocumentBlock.Title(text))
                                            }
                                            styleLower.contains("heading1") || styleLower.contains("heading 1") -> {
                                                outBlocks.add(ParsedDocumentBlock.Heading(1, text))
                                            }
                                            styleLower.contains("heading2") || styleLower.contains("heading 2") -> {
                                                outBlocks.add(ParsedDocumentBlock.Heading(2, text))
                                            }
                                            styleLower.contains("heading3") || styleLower.contains("heading 3") -> {
                                                outBlocks.add(ParsedDocumentBlock.Heading(3, text))
                                            }
                                            currentNumPr || styleLower.contains("list") || text.startsWith("•") || text.startsWith("- ") -> {
                                                val cleaned = text.removePrefix("•").removePrefix("-").trim()
                                                outBlocks.add(ParsedDocumentBlock.BulletItem(cleaned))
                                            }
                                            else -> {
                                                outBlocks.add(ParsedDocumentBlock.Paragraph(text))
                                            }
                                        }
                                    }
                                    inParagraph = false
                                    currentParagraphText.setLength(0)
                                }
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing word/document.xml", e)
        }

        // Robust regex fallback in case of unconventional XML namespaces or malformed tags
        if (outBlocks.isEmpty()) {
            try {
                val xmlString = String(xmlBytes, Charsets.UTF_8)
                val pRegex = Regex("<(?:\\w+:)?p(?:\\s[^>]*)?>(.*?)</(?:\\w+:)?p>", RegexOption.DOT_MATCHES_ALL)
                val tRegex = Regex("<(?:\\w+:)?t(?:\\s[^>]*)?>(.*?)</(?:\\w+:)?t>", RegexOption.DOT_MATCHES_ALL)
                for (pMatch in pRegex.findAll(xmlString)) {
                    val pContent = pMatch.groupValues[1]
                    val text = tRegex.findAll(pContent).map { unescapeXml(it.groupValues[1]) }.joinToString("").trim()
                    if (text.isNotEmpty()) {
                        val isHeading = pContent.contains("Heading", ignoreCase = true) || pContent.contains("Title", ignoreCase = true)
                        if (isHeading) {
                            outBlocks.add(ParsedDocumentBlock.Heading(1, text))
                        } else {
                            outBlocks.add(ParsedDocumentBlock.Paragraph(text))
                        }
                    }
                }
                if (outBlocks.isEmpty()) {
                    val allText = tRegex.findAll(xmlString).map { unescapeXml(it.groupValues[1]) }.joinToString(" ").trim()
                    if (allText.isNotEmpty()) {
                        allText.split("\n\n").forEach { p ->
                            if (p.isNotBlank()) outBlocks.add(ParsedDocumentBlock.Paragraph(p.trim()))
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    // =========================================================================
    // PPTX CONVERSION & EXTRACTION
    // =========================================================================

    suspend fun extractTextFromPptx(context: Context, uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val slides = parsePptxSlides(context, uri)
            val sb = StringBuilder()
            slides.forEach { slide ->
                sb.append("--- Slide ").append(slide.slideNumber)
                if (slide.title.isNotBlank()) {
                    sb.append(": ").append(slide.title)
                }
                sb.append(" ---\n")
                slide.bullets.forEach { bullet ->
                    sb.append("• ").append(bullet).append("\n")
                }
                sb.append("\n")
            }
            Result.success(sb.toString().trim())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun convertPptxToPdf(
        context: Context,
        uri: Uri,
        targetFile: File,
        baseTitle: String,
        fileName: String
    ): Result<DocumentConversionResult> {
        val slides = parsePptxSlides(context, uri)
        if (slides.isEmpty()) {
            return Result.failure(IllegalStateException("Presentasi PowerPoint kosong atau tidak memiliki slide yang terbaca."))
        }

        val extractedText = buildString {
            slides.forEach { s ->
                appendLine("--- Slide ${s.slideNumber}: ${s.title} ---")
                s.bullets.forEach { b -> appendLine("• $b") }
                appendLine()
            }
        }.trim()

        val pdfDoc = PdfDocument()
        val totalSlides = slides.size

        for ((idx, slide) in slides.withIndex()) {
            val pageInfo = PdfDocument.PageInfo.Builder(SLIDE_WIDTH, SLIDE_HEIGHT, idx + 1).create()
            val page = pdfDoc.startPage(pageInfo)
            drawSlidePage(page.canvas, slide, totalSlides, baseTitle)
            pdfDoc.finishPage(page)
        }

        FileOutputStream(targetFile).use { out ->
            pdfDoc.writeTo(out)
            out.flush()
        }
        pdfDoc.close()

        return Result.success(
            DocumentConversionResult(
                pdfFile = targetFile,
                pageCount = totalSlides,
                extractedText = extractedText,
                title = baseTitle,
                originalFileName = fileName
            )
        )
    }

    private fun parsePptxSlides(context: Context, uri: Uri): List<ParsedDocumentBlock.SlideCard> {
        val slideMap = sortedMapOf<Int, ByteArray>()
        val inputStream = context.contentResolver.openInputStream(uri) ?: return emptyList()

        try {
            ZipInputStream(inputStream).use { zip ->
                var entry: ZipEntry? = zip.nextEntry
                while (entry != null) {
                    val name = entry.name.replace('\\', '/').removePrefix("/")
                    // Matching ppt/slides/slide1.xml, ppt/slides/slide2.xml, etc.
                    if (name.startsWith("ppt/slides/slide", ignoreCase = true) && name.endsWith(".xml", ignoreCase = true)) {
                        val slideNumStr = name.substringAfter("ppt/slides/slide").substringBefore(".xml")
                        val slideNum = slideNumStr.toIntOrNull()
                        if (slideNum != null) {
                            val bos = ByteArrayOutputStream()
                            val buf = ByteArray(8192)
                            var r: Int
                            while (zip.read(buf).also { r = it } != -1) {
                                bos.write(buf, 0, r)
                            }
                            slideMap[slideNum] = bos.toByteArray()
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Gagal membuka zip PPTX", e)
        }

        val resultSlides = mutableListOf<ParsedDocumentBlock.SlideCard>()
        slideMap.forEach { (slideNum, xmlBytes) ->
            val (title, bullets) = parseSingleSlideXml(xmlBytes)
            resultSlides.add(
                ParsedDocumentBlock.SlideCard(
                    slideNumber = slideNum,
                    title = title.ifBlank { "Slide $slideNum" },
                    bullets = bullets
                )
            )
        }
        return resultSlides
    }

    private fun parseSingleSlideXml(xmlBytes: ByteArray): Pair<String, List<String>> {
        val paragraphs = mutableListOf<String>()
        try {
            val parser = Xml.newPullParser()
            parser.setInput(xmlBytes.inputStream(), "UTF-8")

            var eventType = parser.eventType
            var inParagraph = false
            var inT = false
            val currentP = StringBuilder()

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val rawName = parser.name ?: ""
                val name = rawName.substringAfter(':')
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (name == "p") {
                            inParagraph = true
                            currentP.setLength(0)
                        } else if (name == "t") {
                            inT = true
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inT && inParagraph) {
                            currentP.append(parser.text)
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (name == "t") {
                            inT = false
                        } else if (name == "p" && inParagraph) {
                            val line = currentP.toString().trim()
                            if (line.isNotEmpty()) {
                                paragraphs.add(line)
                            }
                            inParagraph = false
                            currentP.setLength(0)
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing slide XML", e)
        }

        if (paragraphs.isEmpty()) {
            try {
                val xmlString = String(xmlBytes, Charsets.UTF_8)
                val pRegex = Regex("<(?:\\w+:)?p(?:\\s[^>]*)?>(.*?)</(?:\\w+:)?p>", RegexOption.DOT_MATCHES_ALL)
                val tRegex = Regex("<(?:\\w+:)?t(?:\\s[^>]*)?>(.*?)</(?:\\w+:)?t>", RegexOption.DOT_MATCHES_ALL)
                for (pMatch in pRegex.findAll(xmlString)) {
                    val pContent = pMatch.groupValues[1]
                    val line = tRegex.findAll(pContent).map { unescapeXml(it.groupValues[1]) }.joinToString("").trim()
                    if (line.isNotEmpty()) {
                        paragraphs.add(line)
                    }
                }
                if (paragraphs.isEmpty()) {
                    val allText = tRegex.findAll(xmlString).map { unescapeXml(it.groupValues[1]) }.joinToString(" ").trim()
                    if (allText.isNotEmpty()) {
                        paragraphs.addAll(allText.split("\n\n").map { it.trim() }.filter { it.isNotBlank() })
                    }
                }
            } catch (_: Exception) {}
        }

        return if (paragraphs.isNotEmpty()) {
            val title = paragraphs.first()
            val bullets = paragraphs.drop(1)
            Pair(title, bullets)
        } else {
            Pair("", emptyList())
        }
    }

    private fun drawSlidePage(
        canvas: Canvas,
        slide: ParsedDocumentBlock.SlideCard,
        totalSlides: Int,
        deckTitle: String
    ) {
        // 1. Slide Canvas Background
        val bgPaint = Paint().apply {
            color = Color.rgb(248, 250, 252) // Soft slate 50
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, SLIDE_WIDTH.toFloat(), SLIDE_HEIGHT.toFloat(), bgPaint)

        // 2. Inner Slide Card
        val cardMargin = 50f
        val cardPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val cardStroke = Paint().apply {
            color = Color.rgb(226, 232, 240) // Slate 200
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }
        canvas.drawRoundRect(
            cardMargin, cardMargin,
            SLIDE_WIDTH - cardMargin, SLIDE_HEIGHT - cardMargin,
            24f, 24f, cardPaint
        )
        canvas.drawRoundRect(
            cardMargin, cardMargin,
            SLIDE_WIDTH - cardMargin, SLIDE_HEIGHT - cardMargin,
            24f, 24f, cardStroke
        )

        // Top Accent Strip
        val accentPaint = Paint().apply {
            color = Color.rgb(37, 99, 235) // Royal Blue 600
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(
            cardMargin + 20f, cardMargin + 20f,
            cardMargin + 180f, cardMargin + 28f,
            4f, 4f, accentPaint
        )

        // Header Title
        val titlePaint = TextPaint().apply {
            color = Color.rgb(15, 23, 42) // Slate 900
            textSize = 52f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val contentWidth = (SLIDE_WIDTH - (cardMargin * 2) - 120f).toInt()
        val titleLayout = StaticLayout.Builder.obtain(
            slide.title, 0, slide.title.length, titlePaint, contentWidth
        ).setAlignment(Layout.Alignment.ALIGN_NORMAL).build()

        val titleY = cardMargin + 50f
        canvas.save()
        canvas.translate(cardMargin + 60f, titleY)
        titleLayout.draw(canvas)
        canvas.restore()

        // Separator Line
        val sepY = titleY + titleLayout.height + 24f
        val sepPaint = Paint().apply {
            color = Color.rgb(241, 245, 249)
            strokeWidth = 3f
        }
        canvas.drawLine(cardMargin + 60f, sepY, SLIDE_WIDTH - cardMargin - 60f, sepY, sepPaint)

        // Slide Content / Bullet Items
        var currentY = sepY + 40f
        val bodyPaint = TextPaint().apply {
            color = Color.rgb(51, 65, 85) // Slate 700
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        val bulletIconPaint = Paint().apply {
            color = Color.rgb(37, 99, 235)
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val maxBodyY = SLIDE_HEIGHT - cardMargin - 100f
        for (bullet in slide.bullets) {
            if (currentY + 60f > maxBodyY) break

            // Draw bullet dot
            canvas.drawCircle(cardMargin + 72f, currentY + 22f, 7f, bulletIconPaint)

            val bulletLayout = StaticLayout.Builder.obtain(
                bullet, 0, bullet.length, bodyPaint, contentWidth - 40
            ).setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(12f, 1.25f)
                .build()

            canvas.save()
            canvas.translate(cardMargin + 100f, currentY)
            bulletLayout.draw(canvas)
            canvas.restore()

            currentY += bulletLayout.height + 32f
        }

        // Slide Footer (Number & Deck Title)
        val footerPaint = TextPaint().apply {
            color = Color.rgb(148, 163, 184) // Slate 400
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        val footerLeft = "$deckTitle • Slide ${slide.slideNumber} of $totalSlides"
        canvas.drawText(footerLeft, cardMargin + 60f, SLIDE_HEIGHT - cardMargin - 36f, footerPaint)

        val footerRight = "Lernspace Document"
        val rightWidth = footerPaint.measureText(footerRight)
        canvas.drawText(footerRight, SLIDE_WIDTH - cardMargin - 60f - rightWidth, SLIDE_HEIGHT - cardMargin - 36f, footerPaint)
    }

    // =========================================================================
    // TEXT & MARKDOWN CONVERSION
    // =========================================================================

    private fun convertTextToPdf(
        context: Context,
        uri: Uri,
        targetFile: File,
        baseTitle: String,
        fileName: String
    ): Result<DocumentConversionResult> {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: return Result.failure(IllegalStateException("Gagal membuka file teks."))

        val rawText = inputStream.use { stream ->
            BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).readText()
        }

        val blocks = parseTextToBlocks(rawText, baseTitle)
        val pageCount = renderDocumentBlocksToPdf(
            blocks = blocks,
            docTitle = baseTitle,
            targetFile = targetFile,
            pageWidth = A4_WIDTH,
            pageHeight = A4_HEIGHT
        )

        return Result.success(
            DocumentConversionResult(
                pdfFile = targetFile,
                pageCount = maxOf(1, pageCount),
                extractedText = rawText.trim(),
                title = baseTitle,
                originalFileName = fileName
            )
        )
    }

    private fun parseTextToBlocks(rawText: String, defaultTitle: String): List<ParsedDocumentBlock> {
        val blocks = mutableListOf<ParsedDocumentBlock>()
        var foundTitle = false

        val lines = rawText.lines()
        val currentParagraph = StringBuilder()

        fun flushParagraph() {
            val text = currentParagraph.toString().trim()
            if (text.isNotEmpty()) {
                blocks.add(ParsedDocumentBlock.Paragraph(text))
                currentParagraph.setLength(0)
            }
        }

        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.isEmpty() -> {
                    flushParagraph()
                }
                trimmed.startsWith("# ") -> {
                    flushParagraph()
                    val titleText = trimmed.removePrefix("# ").trim()
                    if (!foundTitle) {
                        blocks.add(ParsedDocumentBlock.Title(titleText))
                        foundTitle = true
                    } else {
                        blocks.add(ParsedDocumentBlock.Heading(1, titleText))
                    }
                }
                trimmed.startsWith("## ") -> {
                    flushParagraph()
                    blocks.add(ParsedDocumentBlock.Heading(2, trimmed.removePrefix("## ").trim()))
                }
                trimmed.startsWith("### ") -> {
                    flushParagraph()
                    blocks.add(ParsedDocumentBlock.Heading(3, trimmed.removePrefix("### ").trim()))
                }
                trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ") -> {
                    flushParagraph()
                    val itemText = trimmed.substring(2).trim()
                    blocks.add(ParsedDocumentBlock.BulletItem(itemText))
                }
                else -> {
                    if (currentParagraph.isNotEmpty()) currentParagraph.append(" ")
                    currentParagraph.append(trimmed)
                }
            }
        }
        flushParagraph()

        if (!foundTitle && blocks.isNotEmpty()) {
            blocks.add(0, ParsedDocumentBlock.Title(defaultTitle))
        }

        return blocks
    }

    // =========================================================================
    // FALLBACK FOR OTHER TEXT/LEGACY DOC FORMATS
    // =========================================================================

    private fun convertFallbackToPdf(
        context: Context,
        uri: Uri,
        targetFile: File,
        baseTitle: String,
        fileName: String
    ): Result<DocumentConversionResult> {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: return Result.failure(IllegalStateException("Gagal membaca file."))

        // Try extracting printable text sequences (handles ASCII/UTF-8 streams inside binary)
        val rawBytes = inputStream.use { it.readBytes() }
        val extractedText = extractPrintableText(rawBytes)

        if (extractedText.isBlank()) {
            return Result.failure(
                IllegalArgumentException("Format file '$fileName' tidak dapat dibaca. Harap gunakan format .docx, .pptx, .pdf, atau .txt/.md.")
            )
        }

        val blocks = parseTextToBlocks(extractedText, baseTitle)
        val pageCount = renderDocumentBlocksToPdf(
            blocks = blocks,
            docTitle = baseTitle,
            targetFile = targetFile,
            pageWidth = A4_WIDTH,
            pageHeight = A4_HEIGHT
        )

        return Result.success(
            DocumentConversionResult(
                pdfFile = targetFile,
                pageCount = maxOf(1, pageCount),
                extractedText = extractedText,
                title = baseTitle,
                originalFileName = fileName
            )
        )
    }

    fun unescapeXml(input: String): String {
        return input.replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
            .replace("&#x27;", "'")
    }

    fun extractPrintableText(bytes: ByteArray): String {
        val sb = StringBuilder()

        // 1. Try decoding UTF-16LE runs (standard for Word 97-2003 .doc binary format)
        val utf16Sb = StringBuilder()
        var run16 = 0
        var i = 0
        while (i + 1 < bytes.size) {
            val b0 = bytes[i].toInt() and 0xFF
            val b1 = bytes[i + 1].toInt() and 0xFF
            val charCode = b0 or (b1 shl 8)
            val c = charCode.toChar()
            if ((c in ' '..'~' || c in '\u00A0'..'\u00FF' || c == '\n' || c == '\r' || c == '\t') && b1 == 0) {
                utf16Sb.append(c)
                run16++
            } else {
                if (run16 >= 5) {
                    val line = utf16Sb.toString().trim()
                    if (line.isNotEmpty()) sb.append(line).append("\n")
                }
                utf16Sb.setLength(0)
                run16 = 0
            }
            i += 2
        }
        if (run16 >= 5) {
            val line = utf16Sb.toString().trim()
            if (line.isNotEmpty()) sb.append(line).append("\n")
        }

        // 2. Also decode ASCII/UTF-8 runs
        val asciiSb = StringBuilder()
        var runAscii = 0
        for (b in bytes) {
            val c = b.toInt().toChar()
            if (c in ' '..'~' || c == '\n' || c == '\r' || c == '\t') {
                asciiSb.append(c)
                runAscii++
            } else {
                if (runAscii >= 6) {
                    val line = asciiSb.toString().trim()
                    if (line.isNotEmpty() && !sb.contains(line)) {
                        sb.append(line).append("\n")
                    }
                }
                asciiSb.setLength(0)
                runAscii = 0
            }
        }
        if (runAscii >= 6) {
            val line = asciiSb.toString().trim()
            if (line.isNotEmpty() && !sb.contains(line)) {
                sb.append(line)
            }
        }

        return sb.toString().trim()
    }

    // =========================================================================
    // A4 MULTI-PAGE PDF RENDERER (WORD & TEXT)
    // =========================================================================

    private fun renderDocumentBlocksToPdf(
        blocks: List<ParsedDocumentBlock>,
        docTitle: String,
        targetFile: File,
        pageWidth: Int,
        pageHeight: Int
    ): Int {
        val pdfDoc = PdfDocument()

        val marginX = 90f
        val marginTop = 100f
        val marginBottom = 100f
        val contentWidth = (pageWidth - (marginX * 2)).toInt()
        val contentBottom = pageHeight - marginBottom

        var currentPageIndex = 0
        var currentPdfPage: PdfDocument.Page? = null
        var currentCanvas: Canvas? = null
        var currentY = marginTop

        fun startNewPage() {
            currentPdfPage?.let {
                drawPageFooter(currentCanvas!!, currentPageIndex, pageWidth, pageHeight, marginX, docTitle)
                pdfDoc.finishPage(it)
            }
            currentPageIndex++
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageIndex).create()
            val newPage = pdfDoc.startPage(pageInfo)
            currentPdfPage = newPage
            currentCanvas = newPage.canvas
            currentY = marginTop

            // Subtle top line on subsequent pages
            if (currentPageIndex > 1) {
                val headerPaint = TextPaint().apply {
                    color = Color.rgb(148, 163, 184)
                    textSize = 20f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    isAntiAlias = true
                }
                currentCanvas?.drawText(docTitle, marginX, 60f, headerPaint)
                val linePaint = Paint().apply {
                    color = Color.rgb(226, 232, 240)
                    strokeWidth = 2f
                }
                currentCanvas?.drawLine(marginX, 75f, pageWidth - marginX, 75f, linePaint)
            }
        }

        // Start first page
        startNewPage()

        val titlePaint = TextPaint().apply {
            color = Color.rgb(15, 23, 42) // Slate 900
            textSize = 46f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val h1Paint = TextPaint().apply {
            color = Color.rgb(30, 41, 59) // Slate 800
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val h2Paint = TextPaint().apply {
            color = Color.rgb(37, 99, 235) // Royal Blue 600
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val h3Paint = TextPaint().apply {
            color = Color.rgb(51, 65, 85) // Slate 700
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val bodyPaint = TextPaint().apply {
            color = Color.rgb(31, 41, 55) // Slate 800
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val bulletDotPaint = Paint().apply {
            color = Color.rgb(37, 99, 235)
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        for (block in blocks) {
            when (block) {
                is ParsedDocumentBlock.Title -> {
                    val layout = StaticLayout.Builder.obtain(
                        block.text, 0, block.text.length, titlePaint, contentWidth
                    ).setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setLineSpacing(10f, 1.2f)
                        .build()

                    if (currentY + layout.height + 40f > contentBottom) {
                        startNewPage()
                    }

                    currentCanvas?.save()
                    currentCanvas?.translate(marginX, currentY)
                    layout.draw(currentCanvas!!)
                    currentCanvas?.restore()

                    currentY += layout.height + 24f

                    // Draw separator under main title
                    val sepPaint = Paint().apply {
                        color = Color.rgb(203, 213, 225)
                        strokeWidth = 3f
                    }
                    currentCanvas?.drawLine(marginX, currentY, marginX + 240f, currentY, sepPaint)
                    currentY += 32f
                }

                is ParsedDocumentBlock.Heading -> {
                    val paint = when (block.level) {
                        1 -> h1Paint
                        2 -> h2Paint
                        else -> h3Paint
                    }
                    val layout = StaticLayout.Builder.obtain(
                        block.text, 0, block.text.length, paint, contentWidth
                    ).setAlignment(Layout.Alignment.ALIGN_NORMAL).build()

                    val topSpacing = if (block.level == 1) 40f else 28f
                    if (currentY + topSpacing + layout.height > contentBottom) {
                        startNewPage()
                    } else {
                        currentY += topSpacing
                    }

                    currentCanvas?.save()
                    currentCanvas?.translate(marginX, currentY)
                    layout.draw(currentCanvas!!)
                    currentCanvas?.restore()

                    currentY += layout.height + 16f
                }

                is ParsedDocumentBlock.Paragraph -> {
                    val layout = StaticLayout.Builder.obtain(
                        block.text, 0, block.text.length, bodyPaint, contentWidth
                    ).setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setLineSpacing(8f, 1.35f)
                        .build()

                    // If paragraph doesn't fit on remaining page, check if we need a new page
                    if (currentY + layout.height > contentBottom && layout.height < (contentBottom - marginTop)) {
                        startNewPage()
                    }

                    currentCanvas?.save()
                    currentCanvas?.translate(marginX, currentY)
                    layout.draw(currentCanvas!!)
                    currentCanvas?.restore()

                    currentY += layout.height + 22f
                }

                is ParsedDocumentBlock.BulletItem -> {
                    val bulletTextWidth = contentWidth - 40
                    val layout = StaticLayout.Builder.obtain(
                        block.text, 0, block.text.length, bodyPaint, bulletTextWidth
                    ).setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setLineSpacing(6f, 1.25f)
                        .build()

                    if (currentY + layout.height > contentBottom) {
                        startNewPage()
                    }

                    // Bullet circle
                    currentCanvas?.drawCircle(marginX + 14f, currentY + 14f, 5f, bulletDotPaint)

                    currentCanvas?.save()
                    currentCanvas?.translate(marginX + 36f, currentY)
                    layout.draw(currentCanvas!!)
                    currentCanvas?.restore()

                    currentY += layout.height + 16f
                }

                is ParsedDocumentBlock.CodeBlock -> {
                    val layout = StaticLayout.Builder.obtain(
                        block.text, 0, block.text.length, bodyPaint, contentWidth - 40
                    ).setAlignment(Layout.Alignment.ALIGN_NORMAL).build()

                    if (currentY + layout.height + 30f > contentBottom) {
                        startNewPage()
                    }

                    // Code block background
                    val boxPaint = Paint().apply {
                        color = Color.rgb(241, 245, 249)
                        style = Paint.Style.FILL
                    }
                    currentCanvas?.drawRoundRect(
                        marginX, currentY,
                        marginX + contentWidth, currentY + layout.height + 24f,
                        12f, 12f, boxPaint
                    )

                    currentCanvas?.save()
                    currentCanvas?.translate(marginX + 20f, currentY + 12f)
                    layout.draw(currentCanvas!!)
                    currentCanvas?.restore()

                    currentY += layout.height + 40f
                }

                is ParsedDocumentBlock.SlideCard -> {}
            }
        }

        // Finish last page
        currentPdfPage?.let {
            drawPageFooter(currentCanvas!!, currentPageIndex, pageWidth, pageHeight, marginX, docTitle)
            pdfDoc.finishPage(it)
        }

        FileOutputStream(targetFile).use { out ->
            pdfDoc.writeTo(out)
            out.flush()
        }
        pdfDoc.close()

        return currentPageIndex
    }

    private fun drawPageFooter(
        canvas: Canvas,
        pageIndex: Int,
        pageWidth: Int,
        pageHeight: Int,
        marginX: Float,
        docTitle: String
    ) {
        val footerPaint = TextPaint().apply {
            color = Color.rgb(148, 163, 184) // Slate 400
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val footerText = "Halaman $pageIndex"
        val textWidth = footerPaint.measureText(footerText)
        val y = pageHeight - 45f

        canvas.drawText(footerText, pageWidth - marginX - textWidth, y, footerPaint)
        canvas.drawText("Lernspace • Dokumen Belajar", marginX, y, footerPaint)
    }

    fun getFileName(context: Context, uri: Uri): String {
        var name = "Dokumen.docx"
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (index >= 0) {
                            name = cursor.getString(index) ?: "Dokumen.docx"
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Gagal mendapatkan nama file dari ContentResolver", e)
            }
        } else if (uri.scheme == "file") {
            name = uri.lastPathSegment ?: "Dokumen.docx"
        }
        return name
    }
}
