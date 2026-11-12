package com.lernspace.app.ui.workspace.math

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MathExpressionEvaluatorTest {

    @Test
    fun variableDeclarationAndSubstitution() {
        val vars = LinkedHashMap<String, Double>()
        val xAssign = MathExpressionEvaluator.parseVariableAssignment("x = 5", vars)
        assertNotNull(xAssign)
        assertEquals("x", xAssign!!.first)
        assertEquals(5.0, xAssign.second, 1e-6)
        vars[xAssign.first] = xAssign.second

        val yAssign = MathExpressionEvaluator.parseVariableAssignment("y = 2x + 3", vars)
        assertNotNull(yAssign)
        assertEquals("y", yAssign!!.first)
        assertEquals(13.0, yAssign.second, 1e-6)
        vars[yAssign.first] = yAssign.second

        val res = MathExpressionEvaluator.solve("2x + y =", vars)
        assertNotNull(res)
        assertEquals("23", res!!.formattedAnswer)

        val prod = MathExpressionEvaluator.solve("xy =", vars)
        assertNotNull(prod)
        assertEquals("65", prod!!.formattedAnswer)
    }

    @Test
    fun powersAndUnicodeSuperscripts() {
        assertEquals("8", MathExpressionEvaluator.solve("2^3 =")?.formattedAnswer)
        assertEquals("27", MathExpressionEvaluator.solve("3^(1+2) =")?.formattedAnswer)
        assertEquals("17", MathExpressionEvaluator.solve("4² + 1 =")?.formattedAnswer)

        val vars = mapOf("x" to 5.0)
        assertEquals("26", MathExpressionEvaluator.solve("x² + 1 =", vars)?.formattedAnswer)
        assertEquals("50", MathExpressionEvaluator.solve("2x^2 =", vars)?.formattedAnswer)
    }

    @Test
    fun fractionsAndStackedFractionGrouping() {
        assertEquals("1", MathExpressionEvaluator.solve("((3)/(4)) + ((1)/(4)) =")?.formattedAnswer)
        assertEquals("2", MathExpressionEvaluator.solve("((2+4)/(3)) =")?.formattedAnswer)
        assertEquals("1.25", MathExpressionEvaluator.solve("½ + ¾ =")?.formattedAnswer)

        val vars = LinkedHashMap<String, Double>()
        val a = MathExpressionEvaluator.parseVariableAssignment("a = ((1)/(2))", vars)
        assertNotNull(a)
        vars[a!!.first] = a.second
        assertEquals("5", MathExpressionEvaluator.solve("a * 10 =", vars)?.formattedAnswer)
    }

    @Test
    fun linearQuadraticAndUnknownEquations() {
        assertEquals("x = 3", MathExpressionEvaluator.solve("2x + 4 = 10")?.formattedAnswer)
        assertEquals("x = -3; 3", MathExpressionEvaluator.solve("x^2 - 9 = 0")?.formattedAnswer)
        assertEquals("a = 4", MathExpressionEvaluator.solve("a^2 - 8a + 16 = 0")?.formattedAnswer)
        assertEquals("x = 3", MathExpressionEvaluator.solve("x^3 = 27")?.formattedAnswer)

        val knownX = mapOf("x" to 5.0)
        assertEquals("y = 7", MathExpressionEvaluator.solve("x + y = 12", knownX)?.formattedAnswer)
    }

    @Test
    fun rejectsNonMathWordsAndAllowsDeclaredVariableExprs() {
        assertFalse(MathExpressionEvaluator.isPlausibleMathExpression("hello"))
        assertFalse(MathExpressionEvaluator.isPlausibleMathExpression("cat = 5"))
        assertTrue(MathExpressionEvaluator.isPlausibleMathExpression("2x + 4 = 10"))
        assertTrue(MathExpressionEvaluator.isPlausibleMathExpression("x^2 - 9 = 0"))
        assertTrue(MathExpressionEvaluator.isPlausibleMathExpression("x + y", mapOf("x" to 2.0, "y" to 3.0)))
    }

    @Test
    fun handlesMlKitMultiplySymbolAsVariableXAndMultiStatement() {
        val vars = mapOf("x" to 5.0)
        assertTrue(MathExpressionEvaluator.isPlausibleMathExpression("2× + 3 =", vars))
        assertEquals("13", MathExpressionEvaluator.solve("2× + 3 =", vars)?.formattedAnswer)
        assertEquals("7", MathExpressionEvaluator.solve("× + 2 =", vars)?.formattedAnswer)
        assertEquals("10", MathExpressionEvaluator.solve("2 × x =", vars)?.formattedAnswer)

        val xFromTimes = MathExpressionEvaluator.parseVariableAssignment("× = 5")
        assertNotNull(xFromTimes)
        assertEquals("x", xFromTimes!!.first)
        assertEquals(5.0, xFromTimes.second, 1e-6)

        assertTrue(MathExpressionEvaluator.isPlausibleMathExpression("x = 5\n2x + 3 ="))
        assertEquals("13", MathExpressionEvaluator.solve("x = 5\n2x + 3 =")?.formattedAnswer)
        assertEquals("11", MathExpressionEvaluator.solve("x = 5, y = 3, x + 2y =")?.formattedAnswer)
        assertEquals("x = 7", MathExpressionEvaluator.solve("2x + 1 = 15", vars)?.formattedAnswer)
    }
}
