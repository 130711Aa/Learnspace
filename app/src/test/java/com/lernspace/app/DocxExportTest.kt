package com.lernspace.app

import com.lernspace.app.ui.workspace.document.DocPageSetup
import com.lernspace.app.ui.workspace.document.DocxDocumentCodec
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
}
