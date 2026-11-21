package com.lernspace.app.ui.workspace.document

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualMathEquationModelTest {

    @Test
    fun testSequenceToLatex_quadraticEquation() {
        val seq = MathExpr.Sequence(
            listOf(
                MathExpr.Power(base = MathExpr.Atom("x"), exp = MathExpr.Sequence(listOf(MathExpr.Atom("2")))),
                MathExpr.Atom(" "),
                MathExpr.Atom("+"),
                MathExpr.Atom(" "),
                MathExpr.Atom("4"),
                MathExpr.Atom("x"),
                MathExpr.Atom(" "),
                MathExpr.Atom("+"),
                MathExpr.Atom(" "),
                MathExpr.Atom("4")
            )
        )
        val latex = seq.toLatex()
        assertEquals("x^{2} + 4x + 4", latex)
    }

    @Test
    fun testFindContainerInfo_powerInRoot() {
        val exp = MathExpr.Sequence(listOf(MathExpr.Atom("2")))
        val power = MathExpr.Power(base = MathExpr.Atom("x"), exp = exp)
        val root = MathExpr.Sequence(listOf(power))

        val info = findContainerInfo(root, exp.id)
        assertNotNull(info)
        assertEquals(power.id, info?.container?.id)
        assertEquals(root.id, info?.parentSeq?.id)
        assertEquals(0, info?.indexInParentSeq)
    }

    @Test
    fun testFindContainerInfo_fractionInPower() {
        val num = MathExpr.Sequence(listOf(MathExpr.Atom("1")))
        val den = MathExpr.Sequence(listOf(MathExpr.Atom("2")))
        val frac = MathExpr.Fraction(num = num, den = den)
        val exp = MathExpr.Sequence(listOf(frac))
        val power = MathExpr.Power(base = MathExpr.Atom("x"), exp = exp)
        val root = MathExpr.Sequence(listOf(power))

        val info = findContainerInfo(root, num.id)
        assertNotNull(info)
        assertEquals(frac.id, info?.container?.id)
        assertEquals(exp.id, info?.parentSeq?.id)
        assertEquals(0, info?.indexInParentSeq)
    }

    @Test
    fun testReplaceSequence() {
        val num = MathExpr.Sequence(listOf(MathExpr.Atom("1")))
        val den = MathExpr.Sequence(listOf(MathExpr.Atom("2")))
        val frac = MathExpr.Fraction(num = num, den = den)
        val root = MathExpr.Sequence(listOf(frac))

        val newNum = MathExpr.Sequence(listOf(MathExpr.Atom("5")))
        val updated = replaceSequence(root, num.id, newNum) as MathExpr.Sequence

        val updatedFrac = updated.items[0] as MathExpr.Fraction
        assertEquals("5", (updatedFrac.num.items[0] as MathExpr.Atom).text)
    }

    @Test
    fun testLatexToMathExpr_quadraticRoundTrip() {
        val latex = "x^{2}+4x+4"
        val parsed = latexToMathExpr(latex)
        // Root items should contain Power(Atom("x"), Sequence(Atom("2"))), Atom("+"), Atom("4"), etc.
        val firstItem = parsed.items[0]
        assertTrue(firstItem is MathExpr.Power)
        val power = firstItem as MathExpr.Power
        assertTrue("Base should be unwrapSingleAtom to Atom(x)", power.base is MathExpr.Atom)
        assertEquals("x", (power.base as MathExpr.Atom).text)
        assertEquals("2", (power.exp.items[0] as MathExpr.Atom).text)
    }

    @Test
    fun testLatexToMathExpr_fractionRoundTrip() {
        val latex = "\\frac{x+1}{x-1}"
        val parsed = latexToMathExpr(latex)
        assertTrue(parsed.items[0] is MathExpr.Fraction)
        val frac = parsed.items[0] as MathExpr.Fraction
        assertEquals(3, frac.num.items.size)
        assertEquals(3, frac.den.items.size)
        assertEquals("\\frac{x+1}{x-1}", frac.toLatex())
    }

    @Test
    fun testLatexToMathExpr_integralNoDoubleDiff() {
        val latex = "\\int_{0}^{1} x \\, dx"
        val parsed = latexToMathExpr(latex)
        assertTrue(parsed.items[0] is MathExpr.Integral)
        val integral = parsed.items[0] as MathExpr.Integral
        assertEquals("0", integral.lower.toLatex())
        assertEquals("1", integral.upper.toLatex())
        assertEquals("x", integral.integrand.toLatex())
        assertEquals("x", integral.variable.toLatex())
        assertEquals("\\int_{0}^{1} x \\, dx", integral.toLatex())
    }

    @Test
    fun testLatexToMathExpr_functionParensRoundTrip() {
        val latex = "\\sin \\left( x \\right)"
        val parsed = latexToMathExpr(latex)
        assertEquals(2, parsed.items.size)
        assertTrue(parsed.items[0] is MathExpr.Atom)
        assertEquals("sin", (parsed.items[0] as MathExpr.Atom).text)
        assertTrue(parsed.items[1] is MathExpr.Parens)
        val parens = parsed.items[1] as MathExpr.Parens
        assertEquals("x", parens.body.toLatex())
        assertEquals("\\sin \\left( x \\right)", parsed.toLatex())
    }
}
