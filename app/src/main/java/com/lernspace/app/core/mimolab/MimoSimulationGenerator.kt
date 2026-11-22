package com.lernspace.app.core.mimolab

import com.lernspace.app.core.ai.GeminiApiClient
import org.json.JSONObject
import java.util.UUID

/**
 * Generator engine that instructs Gemini (Mimo AI) to architect complete,
 * zero-dependency, responsive HTML5 Canvas/SVG interactive STEM simulations.
 */
object MimoSimulationGenerator {

    private val SYSTEM_PROMPT = """
# LEARNSPACE LAB — INTERACTIVE STEM SIMULATION GENERATOR
## AI Educational Simulation Architect (Clean White Theme)

### 1. IDENTITY & PEDAGOGICAL ROLE
You are LearnSpace Lab AI, an advanced educational simulation architect integrated into LearnSpace.
Your goal is to transform scientific phenomena, mathematical formulas, physical laws, and STEM concepts into interactive, visually engaging, and mathematically precise simulations.
Students interact with these simulations on tablet touchscreens by manipulating variables and observing real-time feedback.

Priority principles:
- Scientific and mathematical rigor: Never invent physical laws or wrong equations.
- Educational clarity: Visual representation comes first. Help students understand abstract concepts through experimentation.
- Instant real-time feedback: Changing any slider immediately updates the visualization, calculations, and evaluated formulas.
- Zero external dependencies: Pure vanilla HTML5, Canvas 2D API or SVG, zero CDN or npm scripts. Must work 100% offline.

### 2. STRICT BRANDING: CLEAN WHITE LABORATORY (TEMA PUTIH BERSIH)
Simulations MUST strictly adhere to LearnSpace's clean, minimalist, high-contrast white aesthetic:
- Canvas & Body Background: PURE WHITE (`#FFFFFF`). Never use dark, black, or gloomy backgrounds.
- Control Panels & Docks: Soft Crisp Off-White (`#F8FAFC`) with subtle light borders (`1px solid #E2E8F0`).
- Typography: Deep Charcoal / Black (`#0F172A`) for primary text, Soft Slate (`#64748B`) for secondary labels.
- Color Accents (Harmonious, soft, modern tones — NOT neon or garish):
  * Primary Motion / Trajectory / Force: Soft Cerulean (`#0284C7`)
  * Math / Functions / Calculus: Soft Violet (`#7C3AED`)
  * Thermodynamics / Heat / Friction: Warm Amber (`#D97706`)
  * Gravity Vector: Soft Red (`#DC2626`)
  * Normal Force Vector: Emerald Green (`#059669`)
  * Incline / Ground Surface: Crisp Slate Gray (`#F1F5F9`, border `#CBD5E1`)

### 3. TEXT & FORMULA FORMATTING (USE UNICODE, NO UNESCAPED LATEX)
CRITICAL: To prevent JSON parsing errors and broken display on web views:
- DO NOT output unescaped LaTeX with backslashes like \theta or \mu which turns into broken tabs or raw dollar signs.
- ALWAYS use clean Unicode characters:
  * Angles & coefficients: θ, μ, α, ω, Δ, π, λ, ρ, σ
  * Units & superscripts: m/s², m², kg, N, J, W, Hz, K, °
  * Formula pill: a = g · (sin θ - μ · cos θ) or F_net = m · a

### 4. UNIVERSAL VIEWPORT ARCHITECTURE (ZERO SCROLL)
Every simulation MUST fit exactly in one screen (100% height, zero vertical scroll):
1. `#canvas-container` (Flex: 1, position: relative):
   - Pure White canvas with subtle dot grid: `rgba(0,0,0,0.04)` spaced every 24px.
   - `#glass-hud` (top-left, translucent white card `rgba(255,255,255,0.92)`, border `#E2E8F0`):
     Displays 3-5 real-time calculated values with bold blue badges.
2. `#control-dock` (flex-shrink: 0, docked at bottom):
   - Grid of 2 to 4 responsive `<input type="range">` sliders with numeric badges.
   - Action row with [Mulai/Jeda] and [Reset] buttons + Live Formula Pill.

### 5. PHYSICAL & COORDINATE INTEGRITY
- Canvas Origin: (0,0) is top-left, +Y is DOWN.
- World-to-Screen Mapping: Explicitly separate physical units from pixel coordinates:
  `w2sX(x) = originX + x * scale; w2sY(y) = originY - y * scale;`
- Canvas Setup: Always use `ResizeObserver` + `devicePixelRatio` scaling to prevent 0px collapsed canvas.
- Touch/Stylus Interaction: Set `canvas.style.touchAction = 'none'` so dragging handles works on tablet touchscreen without scrolling.
- String Formatting: Use standard JavaScript string concatenation (+) instead of template literals ($) in JS code to guarantee zero JSON escaping corruption.
- For inclined planes:
  * Ramp is a grounded right triangle:
    Base: `groundY = height - 40;`
    Ramp starts at bottom-left `(startX, groundY)` and rises to `(endX, groundY - rampHeight)` or vice-versa.
    Surface is the hypotenuse.
  * The sliding block is drawn ON the hypotenuse surface.
  * Vectors start at block center:
    - Gravity (W = m * g): points STRAIGHT DOWN (+Y).
    - Normal Force (N = m * g * cos θ): points PERPENDICULAR to ramp AWAY from surface.
    - Friction (f = μ * N): points PARALLEL to ramp resisting motion.

### 6. CANONICAL CODE TEMPLATE (FOLLOW THIS ARCHITECTURE)
```html
<!DOCTYPE html>
<html lang="id">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; -webkit-tap-highlight-color: transparent; }
  html, body { width: 100%; height: 100%; overflow: hidden; display: flex; flex-direction: column; background: #FFFFFF; color: #0F172A; user-select: none; }
  #canvas-container { flex: 1 1 0%; min-height: 0; position: relative; width: 100%; background: #FFFFFF; overflow: hidden; }
  canvas { position: absolute; top: 0; left: 0; width: 100%; height: 100%; display: block; touch-action: none; }
  #glass-hud { position: absolute; top: 12px; left: 12px; background: rgba(255, 255, 255, 0.94); backdrop-filter: blur(8px); border: 1px solid #E2E8F0; border-radius: 12px; padding: 10px 14px; box-shadow: 0 4px 14px rgba(0,0,0,0.06); font-size: 12px; line-height: 1.6; max-width: 320px; pointer-events: none; z-index: 5; }
  .hud-val { font-weight: 700; color: #0284C7; }
  #control-dock { flex-shrink: 0; background: #F8FAFC; border-top: 1px solid #E2E8F0; padding: 12px 18px; display: flex; flex-direction: column; gap: 10px; z-index: 10; }
  .sliders-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(170px, 1fr)); gap: 12px; }
  .control-group { display: flex; flex-direction: column; gap: 4px; }
  .control-label { display: flex; justify-content: space-between; font-size: 11px; font-weight: 600; color: #64748B; }
  .val-badge { font-weight: 700; color: #0284C7; }
  input[type="range"] { width: 100%; height: 26px; accent-color: #0284C7; cursor: pointer; }
  .actions-row { display: flex; align-items: center; justify-content: space-between; gap: 10px; border-top: 1px solid #E2E8F0; padding-top: 8px; }
  .btn-group { display: flex; gap: 8px; }
  button { padding: 8px 16px; font-size: 12px; font-weight: 600; border-radius: 8px; cursor: pointer; border: 1px solid #CBD5E1; background: #FFFFFF; color: #0F172A; }
  button.active { background: #0F172A; color: #FFFFFF; border-color: #0F172A; }
  .formula-box { font-family: ui-monospace, Menlo, Consolas, monospace; font-size: 11px; color: #334155; background: #FFFFFF; border: 1px solid #E2E8F0; border-radius: 6px; padding: 6px 12px; }
</style>
</head>
<body>
<div id="canvas-container">
  <canvas id="simCanvas"></canvas>
  <div id="glass-hud">
    <!-- Live metrics here -->
  </div>
</div>
<div id="control-dock">
  <div class="sliders-grid">
    <!-- Sliders here -->
  </div>
  <div class="actions-row">
    <div class="btn-group">
      <button id="btnPlay" class="active">Mulai</button>
      <button id="btnReset">Reset</button>
    </div>
    <div class="formula-box" id="formulaPill">
      <!-- Live evaluated formula -->
    </div>
  </div>
</div>
<script>
(function() {
  const container = document.getElementById('canvas-container');
  const canvas = document.getElementById('simCanvas');
  const ctx = canvas.getContext('2d');
  let width = 600, height = 400;
  function resize() {
    const w = container.clientWidth || 600;
    const h = container.clientHeight || 400;
    const dpr = window.devicePixelRatio || 1;
    canvas.width = Math.round(w * dpr);
    canvas.height = Math.round(h * dpr);
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    width = w; height = h;
  }
  window.addEventListener('resize', resize);
  if (window.ResizeObserver) new ResizeObserver(resize).observe(container);
  resize();

  // 60FPS animation loop with requestAnimationFrame
  // Responsive slider oninput listeners
})();
</script>
</body>
</html>
```

### 7. OUTPUT FORMAT (STRICT JSON ONLY)
Respond ONLY with a single valid JSON object, no introductory or trailing commentary:
{
  "title": "Judul Simulasi (Bahasa Indonesia)",
  "subject": "FISIKA / KALKULUS / TERMODINAMIKA / ELEKTRO / KIMIA / UMUM",
  "topic": "Topik spesifik",
  "description": "1-2 kalimat ringkas mengenai konsep dan interaksi yang dieksplorasi.",
  "html": "<!DOCTYPE html>..."
}
""".trimIndent()

    suspend fun generateSimulation(
        conceptQuery: String,
        subjectHint: MimoSubject? = null
    ): Result<MimoSimulation> {
        if (!GeminiApiClient.isConfigured()) {
            return Result.failure(
                IllegalStateException("Gemini API Key belum diatur. Silakan atur API Key di menu Settings untuk mengaktifkan Mimo Lab.")
            )
        }

        val subjectInstruction = if (subjectHint != null) {
            "Fokuskan pada bidang ${subjectHint.displayName}."
        } else ""

        val userPrompt = """
Buatkan simulasi interaktif visual untuk konsep berikut:
"$conceptQuery"
$subjectInstruction

Pastikan:
1. Tema kanvas dan latar belakang adalah PUTIH BERSIH (#FFFFFF) dengan kisi-kisi titik halus.
2. Gunakan simbol Unicode (θ, μ, α, m/s², N) BUKAN LaTeX dollar mentah.
3. Kontrol slider di bawah dapat digeser secara real-time dan hasilnya langsung terlihat pada animasi dan formula.
4. Tampilkan vektor gaya (jika fisika) atau kurva fungsi (jika matematika) dengan arah dan skala yang tepat.
Balas HANYA dengan objek JSON yang valid.
""".trimIndent()

        val responseRes = GeminiApiClient.generateText(
            systemPrompt = SYSTEM_PROMPT,
            userPrompt = userPrompt
        )

        return responseRes.mapCatching { rawText ->
            parseSimulationJson(rawText, conceptQuery, subjectHint)
        }
    }

    private fun parseSimulationJson(
        rawText: String,
        fallbackTopic: String,
        fallbackSubject: MimoSubject?
    ): MimoSimulation {
        val cleanJson = rawText
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        return try {
            val json = JSONObject(cleanJson)
            val title = json.optString("title", fallbackTopic.take(40))
            val rawSubject = json.optString("subject", fallbackSubject?.name ?: "UMUM")
            val subject = MimoSubject.fromString(rawSubject)
            val topic = json.optString("topic", fallbackTopic)
            val description = json.optString("description", "Simulasi interaktif konsep $title")
            var html = json.optString("html", "")

            if (html.isBlank()) {
                html = extractHtmlFallback(cleanJson)
            }

            html = postProcessHtml(html)

            MimoSimulation(
                id = UUID.randomUUID().toString(),
                title = title,
                subject = subject,
                conceptTopic = topic,
                description = description,
                htmlContent = html,
                isCommunityOrigin = false,
                authorName = "Mimo AI"
            )
        } catch (e: Exception) {
            // Fallback: If LLM returned raw HTML directly instead of JSON wrapper
            val directHtml = extractHtmlFallback(rawText)
            if (directHtml.isNotBlank()) {
                MimoSimulation(
                    id = UUID.randomUUID().toString(),
                    title = fallbackTopic.take(35),
                    subject = fallbackSubject ?: MimoSubject.fromString(fallbackTopic),
                    conceptTopic = fallbackTopic,
                    description = "Simulasi interaktif konsep $fallbackTopic",
                    htmlContent = postProcessHtml(directHtml),
                    isCommunityOrigin = false,
                    authorName = "Mimo AI"
                )
            } else {
                throw IllegalStateException("Gagal memproses kode simulasi dari Mimo: ${e.message}")
            }
        }
    }

    private fun extractHtmlFallback(text: String): String {
        return if (text.contains("<!DOCTYPE html", ignoreCase = true) || text.contains("<html", ignoreCase = true)) {
            val start = text.indexOf("<!DOCTYPE html", ignoreCase = true).takeIf { it != -1 }
                ?: text.indexOf("<html", ignoreCase = true)
            val end = text.lastIndexOf("</html>", ignoreCase = true)
            if (start != -1 && end != -1) {
                text.substring(start, end + 7)
            } else {
                text
            }
        } else {
            ""
        }
    }

    /**
     * Sanitizes unescaped characters, guarantees the zero-scroll layout,
     * and injects the self-healing canvas resize monitor.
     */
    private fun postProcessHtml(rawHtml: String): String {
        var processed = rawHtml

        // 1. Sanitize accidental unescaped JSON tabs or raw LaTeX symbols in labels
        val replacements = listOf(
            "\$\t" to "θ",
            "\$\\theta\$" to "θ",
            "\$theta\$" to "θ",
            "\$\\mu\$" to "μ",
            "\$mu\$" to "μ",
            "\$\\alpha\$" to "α",
            "\$\\omega\$" to "ω",
            "m/s^2" to "m/s²"
        )
        for ((target, rep) in replacements) {
            processed = processed.replace(target, rep)
        }

        // 2. Guaranteed Clean White & Responsive Viewport CSS
        val canonicalStyle = """
<style id="lernspace-mimo-viewport-guard">
  * { -webkit-tap-highlight-color: transparent; box-sizing: border-box; }
  html, body {
    position: fixed !important;
    top: 0 !important;
    left: 0 !important;
    right: 0 !important;
    bottom: 0 !important;
    width: 100vw !important;
    height: 100vh !important;
    margin: 0 !important;
    padding: 0 !important;
    overflow: hidden !important;
    display: flex !important;
    flex-direction: column !important;
    background: #FFFFFF !important;
    color: #0F172A !important;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif !important;
    user-select: none !important;
    -webkit-user-select: none !important;
  }
  #canvas-container {
    flex: 1 1 0% !important;
    min-height: 0 !important;
    position: relative !important;
    width: 100% !important;
    overflow: hidden !important;
    background: #FFFFFF !important;
  }
  canvas {
    position: absolute !important;
    top: 0 !important;
    left: 0 !important;
    width: 100% !important;
    height: 100% !important;
    display: block !important;
  }
  #glass-hud {
    position: absolute !important;
    top: 14px !important;
    left: 14px !important;
    background: rgba(255, 255, 255, 0.94) !important;
    backdrop-filter: blur(10px) !important;
    border: 1px solid #E2E8F0 !important;
    border-radius: 12px !important;
    padding: 10px 14px !important;
    box-shadow: 0 4px 14px rgba(0, 0, 0, 0.06) !important;
    font-size: 12px !important;
    line-height: 1.6 !important;
    max-width: 320px !important;
    pointer-events: none !important;
    z-index: 5 !important;
  }
  .hud-val { font-weight: 700 !important; color: #0284C7 !important; }
  #control-dock {
    flex-shrink: 0 !important;
    background: #F8FAFC !important;
    border-top: 1px solid #E2E8F0 !important;
    padding: 12px 18px !important;
    display: flex !important;
    flex-direction: column !important;
    gap: 10px !important;
    z-index: 10 !important;
  }
  .sliders-grid {
    display: grid !important;
    grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)) !important;
    gap: 12px !important;
  }
  .control-group { display: flex !important; flex-direction: column !important; gap: 4px !important; }
  .control-label { display: flex !important; justify-content: space-between !important; font-size: 11px !important; font-weight: 600 !important; color: #64748B !important; }
  .val-badge { font-weight: 700 !important; color: #0284C7 !important; }
  input[type="range"] { width: 100% !important; height: 26px !important; cursor: pointer !important; accent-color: #0284C7 !important; }
  .actions-row { display: flex !important; align-items: center !important; justify-content: space-between !important; gap: 10px !important; border-top: 1px solid #E2E8F0 !important; padding-top: 8px !important; }
  .btn-group { display: flex !important; gap: 8px !important; }
  button { padding: 8px 16px !important; font-size: 12px !important; font-weight: 600 !important; border-radius: 8px !important; cursor: pointer !important; border: 1px solid #CBD5E1 !important; background: #FFFFFF !important; color: #0F172A !important; }
  button.active { background: #0F172A !important; color: #FFFFFF !important; border-color: #0F172A !important; }
  .formula-box { font-family: ui-monospace, Menlo, Consolas, monospace !important; font-size: 11px !important; color: #334155 !important; background: #FFFFFF !important; border: 1px solid #E2E8F0 !important; border-radius: 6px !important; padding: 6px 12px !important; }
</style>
""".trimIndent()

        // 3. Self-healing canvas monitor script
        val selfHealingScript = """
<script id="lernspace-mimo-canvas-guard">
  (function() {
    function fixCanvas() {
      var c = document.querySelector('canvas');
      var container = document.getElementById('canvas-container');
      if (c && container) {
        var w = container.clientWidth;
        var h = container.clientHeight;
        if (w > 0 && h > 0) {
          var dpr = window.devicePixelRatio || 1;
          if (c.width !== Math.round(w * dpr) || c.height !== Math.round(h * dpr)) {
            c.width = Math.round(w * dpr);
            c.height = Math.round(h * dpr);
            var ctx = c.getContext('2d');
            if (ctx) ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
          }
        }
      }
    }
    window.addEventListener('load', fixCanvas);
    window.addEventListener('resize', fixCanvas);
    if (window.ResizeObserver) {
      var ro = new ResizeObserver(fixCanvas);
      var container = document.getElementById('canvas-container');
      if (container) ro.observe(container);
    }
    setTimeout(fixCanvas, 50);
    setTimeout(fixCanvas, 250);
    setTimeout(fixCanvas, 600);
  })();
</script>
""".trimIndent()

        // Inject Canonical Style in <head>
        processed = if (processed.contains("</head>", ignoreCase = true)) {
            processed.replace("</head>", "$canonicalStyle\n</head>", ignoreCase = true)
        } else {
            "$canonicalStyle\n$processed"
        }

        // Inject Self-Healing Script before </body>
        processed = if (processed.contains("</body>", ignoreCase = true)) {
            processed.replace("</body>", "$selfHealingScript\n</body>", ignoreCase = true)
        } else {
            "$processed\n$selfHealingScript"
        }

        return processed
    }
}
