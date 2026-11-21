package com.lernspace.app

import com.lernspace.app.ui.workspace.document.DocPageSetup
import com.lernspace.app.ui.workspace.document.DocxDocumentCodec
import com.lernspace.app.ui.workspace.document.LatexMathCodec
import com.lernspace.app.ui.workspace.model.NoteCanvasType
import com.lernspace.app.ui.workspace.model.NoteUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

class DocxExportTest {
    @Test
    fun testExportConsecutiveBlankParagraphsCollapsesToPageBreak() {
        val sb = StringBuilder()
        sb.append("<p>Test</p>")
        for (i in 0 until 55) {
            sb.append("<p><br></p>")
        }
        sb.append("<p>Test</p>")

        val baos = ByteArrayOutputStream()
        DocxDocumentCodec.exportToDocx(sb.toString(), baos)

        val zis = ZipInputStream(ByteArrayInputStream(baos.toByteArray()))
        var docXml: String? = null
        var stylesXml: String? = null
        var entry = zis.nextEntry
        while (entry != null) {
            if (entry.name == "word/document.xml") {
                docXml = String(zis.readBytes(), Charsets.UTF_8)
            } else if (entry.name == "word/styles.xml") {
                stylesXml = String(zis.readBytes(), Charsets.UTF_8)
            }
            entry = zis.nextEntry
        }

        assertTrue("document.xml should exist", docXml != null)
        assertTrue("styles.xml should exist", stylesXml != null)

        // Verify page break is emitted instead of 55 blank paragraphs
        assertTrue("Should contain page break", docXml!!.contains("""<w:br w:type="page"/>"""))

        // Count occurrences of <w:p>
        val pCount = Regex("<w:p[ >]").findAll(docXml).count()
        // There should only be ~3 paragraphs: paragraph 1 ("Test"), page break paragraph, and paragraph 2 ("Test")
        assertTrue("pCount should be very small (~3) instead of 57, but was $pCount", pCount <= 5)

        // Verify styles.xml has w:after="0"
        assertTrue("styles.xml should have w:after=\"0\" for Normal style", stylesXml!!.contains("""w:after="0""""))
        assertFalse("styles.xml should not have w:after=\"120\"", stylesXml.contains("""w:after="120""""))
    }

    @Test
    fun testLeadingBlankParagraphsArePreservedForVerticalSpacing() {
        val sb = StringBuilder()
        for (i in 0 until 4) {
            sb.append("<p><br></p>")
        }
        sb.append("<p style=\"text-align: right\">Jakarta, 20 November 2026</p>")

        val baos = ByteArrayOutputStream()
        DocxDocumentCodec.exportToDocx(sb.toString(), baos)

        val zis = ZipInputStream(ByteArrayInputStream(baos.toByteArray()))
        var docXml: String? = null
        var entry = zis.nextEntry
        while (entry != null) {
            if (entry.name == "word/document.xml") {
                docXml = String(zis.readBytes(), Charsets.UTF_8)
                break
            }
            entry = zis.nextEntry
        }

        assertTrue("document.xml should exist", docXml != null)
        assertFalse("Should not contain page break", docXml!!.contains("""<w:br w:type="page"/>"""))
        val pCount = Regex("<w:p[ >]").findAll(docXml).count()
        assertTrue("Should preserve all 4 blank paragraphs (pCount == 5), but was $pCount", pCount == 5)
        assertTrue("Should contain right alignment", docXml.contains("""<w:jc w:val="right"/>"""))
        assertTrue("Should contain text", docXml.contains("Jakarta, 20 November 2026"))
    }

    @Test
    fun testTableFullWidthAndGridExport() {
        val html = """
            <table>
                <tr><th>Nama</th><th>Nilai</th><th>Keterangan</th></tr>
                <tr><td>Budi</td><td>90</td><td>Lulus</td></tr>
            </table>
        """.trimIndent()

        val baos = ByteArrayOutputStream()
        DocxDocumentCodec.exportToDocx(html, baos)

        val zis = ZipInputStream(ByteArrayInputStream(baos.toByteArray()))
        var docXml: String? = null
        var entry = zis.nextEntry
        while (entry != null) {
            if (entry.name == "word/document.xml") {
                docXml = String(zis.readBytes(), Charsets.UTF_8)
                break
            }
            entry = zis.nextEntry
        }

        assertTrue("document.xml should exist", docXml != null)
        assertTrue("Table should have w:tbl", docXml!!.contains("<w:tbl>"))
        assertTrue("Table should have w:tblGrid", docXml.contains("<w:tblGrid>"))
        assertTrue("Table should have grid columns", docXml.contains("<w:gridCol"))
        assertTrue("Cells should have w:tcW with dxa type", docXml.contains("""w:type="dxa""""))
        assertTrue("Table should be centered", docXml.contains("""<w:jc w:val="center"/>"""))
    }

    @Test
    fun testTableColspanAndCellColorExport() {
        val html = """
            <table>
                <tr><th colspan="2" style="background-color: #2563EB;">Judul Gabungan</th></tr>
                <tr><td style="background-color: #FEF08A;">Kiri</td><td>Kanan</td></tr>
            </table>
        """.trimIndent()

        val baos = ByteArrayOutputStream()
        DocxDocumentCodec.exportToDocx(html, baos)

        val zis = ZipInputStream(ByteArrayInputStream(baos.toByteArray()))
        var docXml: String? = null
        var entry = zis.nextEntry
        while (entry != null) {
            if (entry.name == "word/document.xml") {
                docXml = String(zis.readBytes(), Charsets.UTF_8)
                break
            }
            entry = zis.nextEntry
        }

        assertTrue("document.xml should exist", docXml != null)
        assertTrue("Table should have 2 grid columns calculated from colspan", docXml!!.contains("<w:gridCol"))
        // Check grid col count
        val colCount = Regex("<w:gridCol").findAll(docXml).count()
        assertTrue("Should have exactly 2 columns, but was $colCount", colCount == 2)
        assertTrue("Should contain gridSpan 2", docXml.contains("""<w:gridSpan w:val="2"/>"""))
        assertTrue("Should contain custom cell background color 2563EB", docXml.contains("""w:fill="2563EB""""))
        assertTrue("Should contain custom cell background color FEF08A", docXml.contains("""w:fill="FEF08A""""))
    }

    @Test
    fun testMarkdownToHtmlConversion() {
        val md = """
            # Bab 1: Pendahuluan
            ## 1.1 Latar Belakang
            Berikut adalah poin penting:
            - Poin pertama
            - Poin kedua
            
            1. Langkah satu
            2. Langkah dua
            
            Teks **tebal** dan *miring* serta [tautan](https://example.com).
        """.trimIndent()

        val html = DocxDocumentCodec.markdownToHtml(md)
        assertTrue("Should contain h1", html.contains("<h1>Bab 1: Pendahuluan</h1>"))
        assertTrue("Should contain h3 for ##", html.contains("<h3>1.1 Latar Belakang</h3>"))
        assertTrue("Should contain ul and li", html.contains("<ul><li>Poin pertama</li><li>Poin kedua</li></ul>"))
        assertTrue("Should contain ol and li", html.contains("<ol><li>Langkah satu</li><li>Langkah dua</li></ol>"))
        assertTrue("Should contain strong", html.contains("<strong>tebal</strong>"))
        assertTrue("Should contain em", html.contains("<em>miring</em>"))
        assertTrue("Should contain link", html.contains("<a href=\"https://example.com\">tautan</a>"))
    }

    @Test
    fun testNoteUiModelDisplaySnippetStripsHtml() {
        val note = NoteUiModel(
            title = "Makalah Skripsi",
            subject = "Metodologi",
            date = "Nov 20",
            snippet = "",
            canvasType = NoteCanvasType.DOC_DOCUMENT,
            documentContent = """<!--LSPAGE:{"size":"A4","orient":"PORTRAIT"}--><p>Ini adalah <strong>pendahuluan</strong> penting tentang <em>penelitian</em> kami.</p>"""
        )

        val snippet = note.displaySnippet
        assertFalse("Snippet should not contain LSPAGE", snippet.contains("LSPAGE"))
        assertFalse("Snippet should not contain <p>", snippet.contains("<p>"))
        assertFalse("Snippet should not contain <strong>", snippet.contains("<strong>"))
        assertFalse("Snippet should not contain <em>", snippet.contains("<em>"))
        assertTrue("Snippet should contain clean text", snippet.contains("Ini adalah pendahuluan penting tentang penelitian kami."))
    }

    @Test
    fun testLatexMathCodecToOmmlAndMathML() {
        val integral = "\\int_{0}^{1} x^2 \\, dx"
        val omml = LatexMathCodec.toOmml(integral)
        assertTrue("OMML should contain m:oMath", omml.contains("<m:oMath"))
        assertTrue("OMML should contain m:nary for integral", omml.contains("<m:nary>"))
        assertTrue("OMML should contain integral chr", omml.contains("""<m:chr m:val="∫"/>"""))
        assertTrue("OMML should contain lower bound 0", omml.contains("<m:t>0</m:t>"))
        assertTrue("OMML should contain upper bound 1", omml.contains("<m:t>1</m:t>"))

        val mathMl = LatexMathCodec.toMathML(integral)
        assertTrue("MathML should start with <math", mathMl.contains("<math"))
        assertTrue("MathML should contain mo ∫", mathMl.contains("<mo>∫</mo>"))

        val frac = "\\frac{a + b}{c - d}"
        val fracOmml = LatexMathCodec.toOmml(frac)
        assertTrue("Fraction OMML should contain m:f", fracOmml.contains("<m:f>"))
        assertTrue("Fraction OMML should contain m:num", fracOmml.contains("<m:num>"))
        assertTrue("Fraction OMML should contain m:den", fracOmml.contains("<m:den>"))

        val fracMl = LatexMathCodec.toMathML(frac)
        assertTrue("Fraction MathML should contain mfrac", fracMl.contains("<mfrac>"))
    }

    @Test
    fun testDocxExportWithMathEquationEmitsOmml() {
        val html = """
            <p>Hitunglah integral tertentu berikut ini:</p>
            <p><span class="math-inline" data-latex="\int_{0}^{\pi} \sin(x) dx"><math><mi>test</mi></math></span></p>
            <p>Hasilnya adalah bilangan bulat.</p>
        """.trimIndent()

        val baos = ByteArrayOutputStream()
        DocxDocumentCodec.exportToDocx(html, baos)

        val zis = ZipInputStream(ByteArrayInputStream(baos.toByteArray()))
        var docXml: String? = null
        var entry = zis.nextEntry
        while (entry != null) {
            if (entry.name == "word/document.xml") {
                docXml = String(zis.readBytes(), Charsets.UTF_8)
                break
            }
            entry = zis.nextEntry
        }

        assertTrue("document.xml should exist", docXml != null)
        assertTrue("document.xml should have xmlns:m math declaration", docXml!!.contains("""xmlns:m="http://schemas.openxmlformats.org/officeDocument/2006/math""""))
        assertTrue("document.xml should contain <m:oMath", docXml.contains("<m:oMath"))
        assertTrue("document.xml should contain <m:nary", docXml.contains("<m:nary"))
        assertTrue("document.xml should contain integral symbol", docXml.contains("""<m:chr m:val="∫"/>"""))
        assertTrue("document.xml should contain prefix text", docXml.contains("Hitunglah integral tertentu berikut ini:"))
        assertTrue("document.xml should contain suffix text", docXml.contains("Hasilnya adalah bilangan bulat."))
    }

    @Test
    fun testDocxExportWithInlineDollarFormulaEmitsOmml() {
        val html = "<p>Rumus energi adalah \$\$E = mc^2\$\$ yang dirumuskan oleh Einstein.</p>"

        val baos = ByteArrayOutputStream()
        DocxDocumentCodec.exportToDocx(html, baos)

        val zis = ZipInputStream(ByteArrayInputStream(baos.toByteArray()))
        var docXml: String? = null
        var entry = zis.nextEntry
        while (entry != null) {
            if (entry.name == "word/document.xml") {
                docXml = String(zis.readBytes(), Charsets.UTF_8)
                break
            }
            entry = zis.nextEntry
        }

        assertTrue("document.xml should exist", docXml != null)
        assertTrue("document.xml should contain <m:oMath", docXml!!.contains("<m:oMath"))
        assertTrue("document.xml should contain m:sSup for mc^2", docXml.contains("<m:sSup>"))
        assertTrue("document.xml should contain surrounding text", docXml.contains("Rumus energi adalah"))
        assertTrue("document.xml should contain trailing text", docXml.contains("yang dirumuskan oleh Einstein."))
    }

    @Test
    fun testLatexMathCodecPlainTextConversion() {
        val plainInt = LatexMathCodec.toPlainText("\\int_{0}^{1} x^2 dx")
        assertTrue("Plain text should contain integral symbol", plainInt.contains("∫"))

        val plainFrac = LatexMathCodec.toPlainText("\\frac{1}{2}")
        assertEquals("(1)/(2)", plainFrac)
    }

    @Test
    fun testMathMLSemanticTypography() {
        val linear = LatexMathCodec.toMathML("2x + 1")
        assertTrue("Linear MathML should contain number 2 in mn", linear.contains("<mn>2</mn>"))
        assertTrue("Linear MathML should contain variable x in mi", linear.contains("<mi>x</mi>"))
        assertTrue("Linear MathML should contain operator + in mo", linear.contains("<mo>+</mo>"))
        assertTrue("Linear MathML should contain number 1 in mn", linear.contains("<mn>1</mn>"))

        val integral = LatexMathCodec.toMathML("\\int 2x dx")
        assertTrue("Integral MathML should contain integral symbol", integral.contains("<mo>∫</mo>"))
        assertTrue("Integral MathML should contain differential dx with normal d", integral.contains("""<mi mathvariant="normal">d</mi><mi>x</mi>"""))

        val poly = LatexMathCodec.toMathML("2x^2")
        assertTrue("Polynomial MathML should contain number 2 in mn", poly.contains("<mn>2</mn>"))
        assertTrue("Polynomial MathML should contain msup for x^2", poly.contains("<msup><mrow><mi>x</mi></mrow><mrow><mn>2</mn></mrow></msup>"))
    }
}
