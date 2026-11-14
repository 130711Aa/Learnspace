package com.lernspace.app.core.math

import org.junit.Assert.*
import org.junit.Test

class MathRecognitionAndSolverTest {

    @Test
    fun testCleanLatexNormalizesArrowsToEquals() {
        val cases = listOf(
            "2 + 2 \\rightarrow" to "2 + 2 =",
            "2 + 2 ->" to "2 + 2 =",
            "2 + 2 \\to" to "2 + 2 =",
            "2 + 2 =>" to "2 + 2 =",
            "2 + 2 →" to "2 + 2 =",
            "2 + 2 ⇒" to "2 + 2 =",
            "2 + 2 >" to "2 + 2 =",
            "2 + 2 --" to "2 + 2 =",
            "2 + 2 ==" to "2 + 2 =",
            "2 + 2 \\rightarrow 4" to "2 + 2 = 4",
            "2 + 2 -> 4" to "2 + 2 = 4"
        )

        for ((input, expected) in cases) {
            val cleaned = MathLatexRecognizer.cleanLatex(input)
            assertEquals("Failed for input '$input'", expected, cleaned)
        }
    }

    @Test
    fun testCleanLatexPreservesLimits() {
        val limitExpr = "\\lim_{x \\to 0} \\frac{\\sin x}{x}"
        val cleaned = MathLatexRecognizer.cleanLatex(limitExpr)
        assertTrue(cleaned.contains("\\to"))
        assertFalse(cleaned.contains("\\lim_{x = 0}"))
    }

    @Test
    fun testMathSolverSolvesWithArrowOrEquals() {
        val inputs = listOf(
            "2 + 2 =",
            "2 + 2 \\rightarrow",
            "2 + 2 ->",
            "2 + 2 \\to",
            "2 + 2 =>",
            "2 + 2 →",
            "2 + 2"
        )

        for (input in inputs) {
            val solution = MathSolver.solve(input)
            assertNotNull("Solver returned null for '$input'", solution)
            assertTrue("Solver marked unsolved for '$input'", solution!!.isSolved)
            assertEquals("Result mismatch for '$input'", "4", solution.result)
        }
    }

    @Test
    fun testMathSolverSolvesFractionsAndArithmetic() {
        val sol1 = MathSolver.solve("\\frac{10}{2} =")
        assertNotNull(sol1)
        assertEquals("5", sol1!!.result)

        val sol2 = MathSolver.solve("3 * 5 + 10 / 2 =")
        assertNotNull(sol2)
        assertEquals("20", sol2!!.result)
    }

    @Test
    fun testQuadraticKeyPointsCalculation() {
        val expr = "x^2 - 4"
        val keys = MathPlotHelper.findKeyPoints(expr, -10f, 10f)

        // Y-intercept
        val yInt = keys.firstOrNull { it.type == KeyPointType.Y_INTERCEPT }
        assertNotNull("Y-intercept should be found", yInt)
        assertEquals(0f, yInt!!.x, 0.05f)
        assertEquals(-4f, yInt.y, 0.05f)

        // X-intercepts
        val xInts = keys.filter { it.type == KeyPointType.X_INTERCEPT }.sortedBy { it.x }
        assertEquals("Should have 2 roots", 2, xInts.size)
        assertEquals(-2f, xInts[0].x, 0.05f)
        assertEquals(2f, xInts[1].x, 0.05f)

        // Vertex (Min)
        val vertex = keys.firstOrNull { it.type == KeyPointType.VERTEX_MIN }
        assertNotNull("Vertex minimum should be found", vertex)
        assertEquals(0f, vertex!!.x, 0.08f)
        assertEquals(-4f, vertex.y, 0.08f)
    }

    @Test
    fun testInvertedQuadraticKeyPoints() {
        // -x^2 + 4x - 3 = -(x-1)(x-3), vertex at (2, 1)
        val expr = "-x^2 + 4*x - 3"
        val keys = MathPlotHelper.findKeyPoints(expr, -5f, 10f)

        val xInts = keys.filter { it.type == KeyPointType.X_INTERCEPT }.sortedBy { it.x }
        assertEquals("Should have 2 roots", 2, xInts.size)
        assertEquals(1f, xInts[0].x, 0.08f)
        assertEquals(3f, xInts[1].x, 0.08f)

        val vertex = keys.firstOrNull { it.type == KeyPointType.VERTEX_MAX }
        assertNotNull("Vertex maximum should be found", vertex)
        assertEquals(2f, vertex!!.x, 0.08f)
        assertEquals(1f, vertex.y, 0.08f)
    }

    @Test
    fun testCircleStandardEquationParsing() {
        // Standard circle: (x - 2)^2 + (y - 3)^2 = 16 -> center (2, 3), r = 4
        val circle1 = MathPlotHelper.parseCircle("(x - 2)^2 + (y - 3)^2 = 16")
        assertNotNull("Should recognize standard circle", circle1)
        assertEquals(2f, circle1!!.centerX, 0.01f)
        assertEquals(3f, circle1.centerY, 0.01f)
        assertEquals(4f, circle1.radius, 0.01f)
        assertEquals(8f, circle1.diameter, 0.01f)

        // Centered at origin: x^2 + y^2 = 25 -> center (0, 0), r = 5
        val circle2 = MathPlotHelper.parseCircle("x^2 + y^2 = 25")
        assertNotNull("Should recognize origin circle", circle2)
        assertEquals(0f, circle2!!.centerX, 0.01f)
        assertEquals(0f, circle2.centerY, 0.01f)
        assertEquals(5f, circle2.radius, 0.01f)

        // LaTeX formatting: \left(x+1\right)^{2} + \left(y-4\right)^{2} = 25
        val circle3 = MathPlotHelper.parseCircle("\\left(x+1\\right)^{2} + \\left(y-4\\right)^{2} = 25")
        assertNotNull("Should recognize LaTeX circle", circle3)
        assertEquals(-1f, circle3!!.centerX, 0.01f)
        assertEquals(4f, circle3.centerY, 0.01f)
        assertEquals(5f, circle3.radius, 0.01f)
    }

    @Test
    fun testCircleGeneralFormParsing() {
        // General form: x^2 + y^2 - 4x - 6y - 12 = 0 -> center (2, 3), r = 5
        val circle1 = MathPlotHelper.parseCircle("x^2 + y^2 - 4x - 6y - 12 = 0")
        assertNotNull("Should recognize general circle form", circle1)
        assertEquals(2f, circle1!!.centerX, 0.01f)
        assertEquals(3f, circle1.centerY, 0.01f)
        assertEquals(5f, circle1.radius, 0.01f)

        // General form with RHS: x^2 + y^2 + 2x - 8y = -8 -> center (-1, 4), r = 3
        val circle2 = MathPlotHelper.parseCircle("x^2 + y^2 + 2x - 8y = -8")
        assertNotNull("Should recognize general circle form with RHS", circle2)
        assertEquals(-1f, circle2!!.centerX, 0.01f)
        assertEquals(4f, circle2.centerY, 0.01f)
        assertEquals(3f, circle2.radius, 0.01f)
    }

    @Test
    fun testCircleKeyPoints() {
        // x^2 + y^2 = 25 -> center (0,0), radius 5
        val circle = MathPlotHelper.parseCircle("x^2 + y^2 = 25")!!
        val keys = MathPlotHelper.getCircleKeyPoints(circle)

        val center = keys.firstOrNull { it.type == KeyPointType.CIRCLE_CENTER }
        assertNotNull("Center should be present", center)
        assertEquals(0f, center!!.x, 0.01f)
        assertEquals(0f, center.y, 0.01f)

        val xInts = keys.filter { it.type == KeyPointType.X_INTERCEPT }.sortedBy { it.x }
        assertEquals(2, xInts.size)
        assertEquals(-5f, xInts[0].x, 0.01f)
        assertEquals(5f, xInts[1].x, 0.01f)

        val yInts = keys.filter { it.type == KeyPointType.Y_INTERCEPT }.sortedBy { it.y }
        assertEquals(2, yInts.size)
        assertEquals(-5f, yInts[0].y, 0.01f)
        assertEquals(5f, yInts[1].y, 0.01f)
    }

    @Test
    fun testFormatForCanvasNormalizesMathExpressions() {
        val cases = listOf(
            "x^2" to "\$\$\nx^2\n\$\$",
            "\$\$x^2\$\$" to "\$\$\nx^2\n\$\$",
            "\$x^2\$" to "\$\$\nx^2\n\$\$",
            "x^2 + y^2 = 25" to "\$\$\nx^2 + y^2 = 25\n\$\$",
            "(x - 2)^2 + (y - 3)^2 = 16" to "\$\$\n(x - 2)^2 + (y - 3)^2 = 16\n\$\$"
        )
        for ((input, expected) in cases) {
            val formatted = MathLatexRecognizer.formatForCanvas(input)
            assertEquals("Failed for input '$input'", expected, formatted)
        }
    }

    @Test
    fun testLinearEquationParsing() {
        // 2x + 3y = 6 -> a = 2, b = 3, c = -6
        val lin1 = MathPlotHelper.parseLinearEquation("2x + 3y = 6")
        assertNotNull(lin1)
        assertFalse(lin1!!.isVertical)
        assertEquals(-2f / 3f, lin1.slope, 0.01f)
        assertEquals(2f, lin1.yIntercept, 0.01f)

        // x - y = 0 -> slope = 1, yIntercept = 0
        val lin2 = MathPlotHelper.parseLinearEquation("x - y = 0")
        assertNotNull(lin2)
        assertEquals(1f, lin2!!.slope, 0.01f)
        assertEquals(0f, lin2.yIntercept, 0.01f)

        // y = 5 -> slope = 0, yIntercept = 5
        val lin3 = MathPlotHelper.parseLinearEquation("y = 5")
        assertNotNull(lin3)
        assertEquals(0f, lin3!!.slope, 0.01f)
        assertEquals(5f, lin3.yIntercept, 0.01f)

        // Quadratic equation must NOT be parsed as linear
        assertNull(MathPlotHelper.parseLinearEquation("y = x^2 - 4"))
    }

    @Test
    fun testVerticalLineParsing() {
        // x = 3
        val vert1 = MathPlotHelper.parseLinearEquation("x = 3")
        assertNotNull(vert1)
        assertTrue(vert1!!.isVertical)
        assertEquals(3f, vert1.verticalX, 0.01f)

        // 2x = 8 -> x = 4
        val vert2 = MathPlotHelper.parseLinearEquation("2x = 8")
        assertNotNull(vert2)
        assertTrue(vert2!!.isVertical)
        assertEquals(4f, vert2.verticalX, 0.01f)

        // x - 5 = 0 -> x = 5
        val vert3 = MathPlotHelper.parseLinearEquation("x - 5 = 0")
        assertNotNull(vert3)
        assertTrue(vert3!!.isVertical)
        assertEquals(5f, vert3.verticalX, 0.01f)
    }

    @Test
    fun testIntersectionsLineAndParabola() {
        // y = x and y = x^2 - 2 -> x^2 - x - 2 = 0 -> (x-2)(x+1) = 0 -> intersections (-1, -1) and (2, 2)
        val line = CurvePlotDefinition(id = "1", cleanExpr = "x")
        val parabola = CurvePlotDefinition(id = "2", cleanExpr = "x^2 - 2")

        val pts = MathPlotHelper.findIntersections(listOf(line, parabola), -5f, 5f)
        assertEquals(2, pts.size)
        assertEquals(-1f, pts[0].x, 0.05f)
        assertEquals(-1f, pts[0].y, 0.05f)
        assertEquals(2f, pts[1].x, 0.05f)
        assertEquals(2f, pts[1].y, 0.05f)
    }

    @Test
    fun testIntersectionsVerticalLineAndCircle() {
        // Circle x^2 + y^2 = 25 and vertical line x = 3 -> y = +-4
        val circleData = MathPlotHelper.parseCircle("x^2 + y^2 = 25")!!
        val circleDef = CurvePlotDefinition(id = "c", cleanExpr = circleData.standardEquation, isCircle = true, circleData = circleData)
        val vertDef = CurvePlotDefinition(id = "v", cleanExpr = "x = 3", isVerticalLine = true, verticalLineX = 3f)

        val pts = MathPlotHelper.findIntersections(listOf(circleDef, vertDef), -10f, 10f)
        assertEquals(2, pts.size)
        assertEquals(3f, pts[0].x, 0.01f)
        assertEquals(-4f, pts[0].y, 0.05f)
        assertEquals(3f, pts[1].x, 0.01f)
        assertEquals(4f, pts[1].y, 0.05f)
    }

    @Test
    fun testParameterExtractionAndSubstitution() {
        // Test extracting parameters
        val params1 = MathPlotHelper.extractParameters("y = a*x^2 + b*x + c")
        assertEquals(listOf('a', 'b', 'c'), params1)

        val params2 = MathPlotHelper.extractParameters("y = a*sin(b*x) + d")
        assertEquals(listOf('a', 'b', 'd'), params2)

        val paramsCircle = MathPlotHelper.extractParameters("(x - h)^2 + (y - k)^2 = r^2")
        assertEquals(listOf('h', 'k', 'r'), paramsCircle)

        // Test substituting parameters
        val substituted = MathPlotHelper.substituteParameters("a*x^2 + c", mapOf('a' to 2.5f, 'c' to -3f))
        assertEquals("(2.5)*x^2 + (-3)", substituted)
    }

    @Test
    fun testTrigonometricAndPiGridFormatting() {
        assertTrue(MathPlotHelper.isTrigonometric("sin(x)"))
        assertTrue(MathPlotHelper.isTrigonometric("y = 2*cos(x) + 1"))
        assertFalse(MathPlotHelper.isTrigonometric("y = x^2 + 4"))

        val pi = Math.PI.toFloat()
        assertEquals("0", MathPlotHelper.formatPiFraction(0f))
        assertEquals("π", MathPlotHelper.formatPiFraction(pi))
        assertEquals("-π", MathPlotHelper.formatPiFraction(-pi))
        assertEquals("π/2", MathPlotHelper.formatPiFraction(pi / 2f))
        assertEquals("-π/2", MathPlotHelper.formatPiFraction(-pi / 2f))
        assertEquals("3π/2", MathPlotHelper.formatPiFraction(3f * pi / 2f))
        assertEquals("2π", MathPlotHelper.formatPiFraction(2f * pi))
        assertEquals("π/4", MathPlotHelper.formatPiFraction(pi / 4f))

        // Step size calculation
        val step = MathPlotHelper.calculateNicePiStep(4f * pi)
        assertTrue(step > 0f)
    }

    @Test
    fun testParametricCircleEquations() {
        // Test x^2 + y^2 = r (without params -> default r=3)
        val circleDefault = MathPlotHelper.parseCircle("x^2 + y^2 = r")
        assertNotNull(circleDefault)
        assertEquals(0f, circleDefault!!.centerX, 0.001f)
        assertEquals(0f, circleDefault.centerY, 0.001f)
        assertEquals(3f, circleDefault.radius, 0.001f)

        // Test x^2 + y^2 = r with slider r = 5
        val circleWithR = MathPlotHelper.parseCircle("x^2 + y^2 = r", mapOf('r' to 5f))
        assertNotNull(circleWithR)
        assertEquals(0f, circleWithR!!.centerX, 0.001f)
        assertEquals(0f, circleWithR.centerY, 0.001f)
        assertEquals(5f, circleWithR.radius, 0.001f)

        // Test x^2 + y^2 = r^2 with slider r = 4
        val circleSq = MathPlotHelper.parseCircle("x^2 + y^2 = r^2", mapOf('r' to 4f))
        assertNotNull(circleSq)
        assertEquals(0f, circleSq!!.centerX, 0.001f)
        assertEquals(0f, circleSq.centerY, 0.001f)
        assertEquals(4f, circleSq.radius, 0.001f)

        // Test (x - h)^2 + (y - k)^2 = r with h=2, k=-3, r=6
        val circleHkr = MathPlotHelper.parseCircle("(x - h)^2 + (y - k)^2 = r", mapOf('h' to 2f, 'k' to -3f, 'r' to 6f))
        assertNotNull(circleHkr)
        assertEquals(2f, circleHkr!!.centerX, 0.01f)
        assertEquals(-3f, circleHkr.centerY, 0.01f)
        assertEquals(6f, circleHkr.radius, 0.01f)
    }

    @Test
    fun testImplicitMultiplicationAndLinearParameter() {
        // Test normalization of implicit multiplication
        assertEquals("a*x + 5", MathPlotHelper.normalizeMathExpression("ax + 5"))
        assertEquals("y = a*x + 5", MathPlotHelper.normalizeMathExpression("y = ax + 5"))
        assertEquals("m*x + c", MathPlotHelper.normalizeMathExpression("mx + c"))
        assertEquals("2*x + 3", MathPlotHelper.normalizeMathExpression("2x + 3"))
        assertEquals("a*x^2 + b*x + c", MathPlotHelper.normalizeMathExpression("ax^2 + bx + c"))
        assertEquals("a*sin(b*x)", MathPlotHelper.normalizeMathExpression("a sin(bx)"))

        // Test parameter extraction with implicit multiplication: y = ax + 5 -> ['a']
        val params = MathPlotHelper.extractParameters("y = ax + 5")
        assertEquals(listOf('a'), params)

        // Test substitution: ax + 5 with a=2 -> (2)*x + 5
        val substituted = MathPlotHelper.substituteParameters("ax + 5", mapOf('a' to 2f))
        assertEquals("(2)*x + 5", substituted)

        // Test dynamic sampling of substituted linear function
        val samples = MathPlotHelper.sampleFunctionDynamic(substituted, -5f, 5f)
        assertTrue(samples.isNotEmpty())
    }
}
