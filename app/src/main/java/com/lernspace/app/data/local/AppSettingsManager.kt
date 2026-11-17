package com.lernspace.app.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.lernspace.app.ui.workspace.model.GeometricShapeType
import com.lernspace.app.ui.workspace.model.NoteTool
import com.lernspace.app.ui.workspace.model.PaperStyle

enum class Handedness(val title: String, val description: String) {
    RIGHT_HANDED("Tangan Kanan (Right-Handed)", "Navigasi rail di sisi kiri, area layar bebas untuk tangan kanan"),
    LEFT_HANDED("Kidal (Left-Handed)", "Navigasi rail dipindah ke sisi kanan agar pergelangan tangan kiri tidak menyenggol menu")
}

enum class ShapeSnapSensitivity(val title: String, val thresholdMs: Long, val description: String) {
    FAST("Cepat (180ms)", 180L, "Sangat responsif, langsung mengunci bentuk geometri sesaat setelah berhenti menggores"),
    NORMAL("Standar (260ms)", 260L, "Seimbang dan nyaman untuk menulis serta menggambar sehari-hari (Rekomendasi)"),
    RELAXED("Rileks (400ms)", 400L, "Memerlukan jeda tahan lebih lama, mencegah salah deteksi saat berpikir di atas kanvas"),
    OFF("Nonaktif (Mati)", Long.MAX_VALUE, "Coretan bebas murni tanpa auto-shape locking")
}

enum class AppThemeMode(val title: String, val description: String) {
    SYSTEM("Mengikuti Sistem", "Menyesuaikan otomatis dengan tema gelap/terang sistem tablet Anda"),
    LIGHT("Mode Terang", "Tampilan bersih cerah bernuansa kertas akademik"),
    DARK("Mode Gelap", "Tampilan gelap elegan yang nyaman untuk belajar di ruangan redup")
}

enum class TouchInputMode(val title: String, val description: String) {
    STYLUS_ONLY(
        "Hanya Stylus (Rekomendasi)",
        "Goresan hanya direspon oleh pena stylus. Sentuhan 1 & 2 jari untuk menggeser & zoom (Palm Rejection aktif penuh)"
    ),
    FINGER_AND_STYLUS(
        "Sentuhan Tangan & Stylus",
        "1 sentuhan jari atau stylus untuk coret-coret catatan, 2 jari untuk menggeser (pan) dan zoom kanvas"
    )
}

enum class EraserMode(val title: String, val description: String) {
    PIXEL("Pixel / Bagian", "Menghapus hanya bagian goresan di dalam radius lingkaran penghapus"),
    STROKE("Goresan Utuh", "Menghapus seluruh goresan yang tersentuh lingkaran penghapus")
}

/**
 * Single source of truth for user preferences on the tablet.
 * Persists instantaneously to Android SharedPreferences and provides reactive Compose states.
 */
object AppSettingsManager {
    private const val PREFS_NAME = "lernspace_settings"
    private const val KEY_HANDEDNESS = "handedness"
    private const val KEY_SHAPE_SENSITIVITY = "shape_sensitivity"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_DEFAULT_PAPER = "default_paper"
    private const val KEY_TOUCH_INPUT_MODE = "touch_input_mode"
    private const val KEY_ERASER_RADIUS = "eraser_radius"
    private const val KEY_ERASER_MODE = "eraser_mode"
    private const val KEY_SCRIBBLE_TO_ERASE = "scribble_to_erase"

    // Note Tool Presets persistence
    private const val KEY_ACTIVE_TOOL = "active_note_tool"
    private const val KEY_PEN_COLOR = "pen_color"
    private const val KEY_PEN_THICKNESS = "pen_thickness"
    private const val KEY_HIGHLIGHTER_COLOR = "highlighter_color"
    private const val KEY_HIGHLIGHTER_THICKNESS = "highlighter_thickness"
    private const val KEY_SELECTED_SHAPE_TYPE = "selected_shape_type"

    val DEFAULT_PEN_COLOR = Color(0xFF0F172A)
    val DEFAULT_HIGHLIGHTER_COLOR = Color(0xFFFDE047)

    var handedness by mutableStateOf(Handedness.RIGHT_HANDED)
    var shapeSensitivity by mutableStateOf(ShapeSnapSensitivity.NORMAL)
    var themeMode by mutableStateOf(AppThemeMode.SYSTEM)
    var defaultPaperStyle by mutableStateOf(PaperStyle.RULED)
    var touchInputMode by mutableStateOf(TouchInputMode.STYLUS_ONLY)
    var eraserRadius by mutableFloatStateOf(30f)
    var eraserMode by mutableStateOf(EraserMode.PIXEL)
    var isScribbleToEraseEnabled by mutableStateOf(true)

    // Reactive states for Note Tool presets
    var activeTool by mutableStateOf(NoteTool.PEN)
    var penColor by mutableStateOf(DEFAULT_PEN_COLOR)
    var penThickness by mutableFloatStateOf(4f)
    var highlighterColor by mutableStateOf(DEFAULT_HIGHLIGHTER_COLOR)
    var highlighterThickness by mutableFloatStateOf(24f)
    var selectedShapeType by mutableStateOf(GeometricShapeType.RECTANGLE)

    val isFingerDrawingEnabled: Boolean
        get() = touchInputMode == TouchInputMode.FINGER_AND_STYLUS

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = sp
        handedness = try {
            Handedness.valueOf(sp.getString(KEY_HANDEDNESS, Handedness.RIGHT_HANDED.name) ?: Handedness.RIGHT_HANDED.name)
        } catch (e: Exception) {
            Handedness.RIGHT_HANDED
        }

        shapeSensitivity = try {
            ShapeSnapSensitivity.valueOf(sp.getString(KEY_SHAPE_SENSITIVITY, ShapeSnapSensitivity.NORMAL.name) ?: ShapeSnapSensitivity.NORMAL.name)
        } catch (e: Exception) {
            ShapeSnapSensitivity.NORMAL
        }

        themeMode = try {
            AppThemeMode.valueOf(sp.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.name) ?: AppThemeMode.SYSTEM.name)
        } catch (e: Exception) {
            AppThemeMode.SYSTEM
        }

        defaultPaperStyle = try {
            PaperStyle.valueOf(sp.getString(KEY_DEFAULT_PAPER, PaperStyle.RULED.name) ?: PaperStyle.RULED.name)
        } catch (e: Exception) {
            PaperStyle.RULED
        }

        touchInputMode = try {
            TouchInputMode.valueOf(sp.getString(KEY_TOUCH_INPUT_MODE, TouchInputMode.STYLUS_ONLY.name) ?: TouchInputMode.STYLUS_ONLY.name)
        } catch (e: Exception) {
            TouchInputMode.STYLUS_ONLY
        }

        eraserRadius = sp.getFloat(KEY_ERASER_RADIUS, 30f)

        eraserMode = try {
            EraserMode.valueOf(sp.getString(KEY_ERASER_MODE, EraserMode.PIXEL.name) ?: EraserMode.PIXEL.name)
        } catch (e: Exception) {
            EraserMode.PIXEL
        }

        isScribbleToEraseEnabled = sp.getBoolean(KEY_SCRIBBLE_TO_ERASE, true)

        activeTool = try {
            val toolName = sp.getString(KEY_ACTIVE_TOOL, NoteTool.PEN.name) ?: NoteTool.PEN.name
            val tool = NoteTool.valueOf(toolName)
            if (tool == NoteTool.IMAGE || tool == NoteTool.TEXT) NoteTool.PEN else tool
        } catch (e: Exception) {
            NoteTool.PEN
        }

        penColor = try {
            val argb = sp.getInt(KEY_PEN_COLOR, DEFAULT_PEN_COLOR.toArgb())
            Color(argb)
        } catch (e: Exception) {
            DEFAULT_PEN_COLOR
        }

        penThickness = sp.getFloat(KEY_PEN_THICKNESS, 4f)

        highlighterColor = try {
            val argb = sp.getInt(KEY_HIGHLIGHTER_COLOR, DEFAULT_HIGHLIGHTER_COLOR.toArgb())
            Color(argb)
        } catch (e: Exception) {
            DEFAULT_HIGHLIGHTER_COLOR
        }

        highlighterThickness = sp.getFloat(KEY_HIGHLIGHTER_THICKNESS, 24f)

        selectedShapeType = try {
            val shapeName = sp.getString(KEY_SELECTED_SHAPE_TYPE, GeometricShapeType.RECTANGLE.name) ?: GeometricShapeType.RECTANGLE.name
            GeometricShapeType.valueOf(shapeName)
        } catch (e: Exception) {
            GeometricShapeType.RECTANGLE
        }
    }

    fun ensureSafeActiveTool() {
        if (activeTool == NoteTool.IMAGE || activeTool == NoteTool.TEXT) {
            activeTool = NoteTool.PEN
        }
    }

    fun updateActiveTool(value: NoteTool) {
        activeTool = value
        if (value != NoteTool.IMAGE && value != NoteTool.TEXT) {
            prefs?.edit()?.putString(KEY_ACTIVE_TOOL, value.name)?.apply()
        }
    }

    fun updatePenColor(value: Color) {
        penColor = value
        prefs?.edit()?.putInt(KEY_PEN_COLOR, value.toArgb())?.apply()
    }

    fun updatePenThickness(value: Float) {
        penThickness = value
        prefs?.edit()?.putFloat(KEY_PEN_THICKNESS, value)?.apply()
    }

    fun updateHighlighterColor(value: Color) {
        highlighterColor = value
        prefs?.edit()?.putInt(KEY_HIGHLIGHTER_COLOR, value.toArgb())?.apply()
    }

    fun updateHighlighterThickness(value: Float) {
        highlighterThickness = value
        prefs?.edit()?.putFloat(KEY_HIGHLIGHTER_THICKNESS, value)?.apply()
    }

    fun updateSelectedShapeType(value: GeometricShapeType) {
        selectedShapeType = value
        prefs?.edit()?.putString(KEY_SELECTED_SHAPE_TYPE, value.name)?.apply()
    }

    fun updateHandedness(value: Handedness) {
        handedness = value
        prefs?.edit()?.putString(KEY_HANDEDNESS, value.name)?.apply()
    }

    fun updateShapeSensitivity(value: ShapeSnapSensitivity) {
        shapeSensitivity = value
        prefs?.edit()?.putString(KEY_SHAPE_SENSITIVITY, value.name)?.apply()
    }

    fun updateThemeMode(value: AppThemeMode) {
        themeMode = value
        prefs?.edit()?.putString(KEY_THEME_MODE, value.name)?.apply()
    }

    fun updateDefaultPaperStyle(value: PaperStyle) {
        defaultPaperStyle = value
        prefs?.edit()?.putString(KEY_DEFAULT_PAPER, value.name)?.apply()
    }

    fun updateTouchInputMode(value: TouchInputMode) {
        touchInputMode = value
        prefs?.edit()?.putString(KEY_TOUCH_INPUT_MODE, value.name)?.apply()
    }

    fun updateEraserRadius(value: Float) {
        eraserRadius = value
        prefs?.edit()?.putFloat(KEY_ERASER_RADIUS, value)?.apply()
    }

    fun updateEraserMode(value: EraserMode) {
        eraserMode = value
        prefs?.edit()?.putString(KEY_ERASER_MODE, value.name)?.apply()
    }

    fun updateScribbleToErase(enabled: Boolean) {
        isScribbleToEraseEnabled = enabled
        prefs?.edit()?.putBoolean(KEY_SCRIBBLE_TO_ERASE, enabled)?.apply()
    }
}

