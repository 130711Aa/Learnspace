package com.lernspace.app.core.math3d

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class GeoElementType(val displayName: String) {
    FUNCTION_2D("Fungsi 2D y = f(x)"),
    SOLID_OF_REVOLUTION("Benda Putar"),
    SURFACE_3D("Permukaan 3D z = f(x, y)"),
    VECTOR_3D("Vektor R³"),
    POINT_3D("Titik 3D"),
    PLANE_3D("Bidang Datar 3D"),
    POLYHEDRON("Bangun Ruang"),
    VARIABLE_SLIDER("Slider Variabel Dinamis")
}

data class GeoElementModel(
    val id: String = UUID.randomUUID().toString(),
    val type: GeoElementType,
    val name: String,
    val definition: String,
    val color: String = "#00E5FF",
    val visible: Boolean = true,
    // Numerical / function parameters
    val expr: String = "",
    val a: Float = 0f,
    val b: Float = 4f,
    val axis: String = "x",
    val angle: Float = 360f,
    val range: Float = 5f,
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
    val coeffA: Float = 1f,
    val coeffB: Float = 1f,
    val coeffC: Float = 1f,
    val coeffD: Float = 2f,
    val subType: String = "CUBE",
    val size: Float = 4f,
    val height: Float = 5f,
    val radius: Float = 2.5f,
    // Slider parameters
    val varName: String = "a",
    val sliderValue: Float = 2f,
    val sliderMin: Float = -5f,
    val sliderMax: Float = 5f,
    val sliderStep: Float = 0.5f,
    // Definite Integral (2D Graph)
    val hasIntegral: Boolean = false,
    val integralRiemannType: String = "SMOOTH",
    val integralPartitions: Int = 12,
    // Computed value / preview label
    val computedValue: String = ""
)

data class Math3DMetrics(
    val volume: Double = 0.0,
    val surfaceArea: Double = 0.0,
    val dotProduct: Double = 0.0,
    val angleDeg: Double = 0.0
)

fun geoElementToJson(elem: GeoElementModel): JSONObject {
    return JSONObject().apply {
        put("id", elem.id)
        put("type", elem.type.name)
        put("name", elem.name)
        put("definition", elem.definition)
        put("color", elem.color)
        put("visible", elem.visible)
        put("expr", elem.expr)
        put("a", elem.a.toDouble())
        put("b", elem.b.toDouble())
        put("axis", elem.axis)
        put("angle", elem.angle.toDouble())
        put("range", elem.range.toDouble())
        put("x", elem.x.toDouble())
        put("y", elem.y.toDouble())
        put("z", elem.z.toDouble())
        put("coeffA", elem.coeffA.toDouble())
        put("coeffB", elem.coeffB.toDouble())
        put("coeffC", elem.coeffC.toDouble())
        put("coeffD", elem.coeffD.toDouble())
        put("subType", elem.subType)
        put("size", elem.size.toDouble())
        put("height", elem.height.toDouble())
        put("radius", elem.radius.toDouble())
        put("varName", elem.varName)
        put("sliderValue", elem.sliderValue.toDouble())
        put("sliderMin", elem.sliderMin.toDouble())
        put("sliderMax", elem.sliderMax.toDouble())
        put("sliderStep", elem.sliderStep.toDouble())
        put("hasIntegral", elem.hasIntegral)
        put("integralRiemannType", elem.integralRiemannType)
        put("integralPartitions", elem.integralPartitions)
        put("computedValue", elem.computedValue)
    }
}

fun jsonToGeoElement(json: JSONObject): GeoElementModel {
    val typeStr = json.optString("type", GeoElementType.FUNCTION_2D.name)
    val type = try { GeoElementType.valueOf(typeStr) } catch (_: Exception) { GeoElementType.FUNCTION_2D }
    return GeoElementModel(
        id = json.optString("id", UUID.randomUUID().toString()),
        type = type,
        name = json.optString("name", "f"),
        definition = json.optString("definition", ""),
        color = json.optString("color", "#00E5FF"),
        visible = json.optBoolean("visible", true),
        expr = json.optString("expr", ""),
        a = json.optDouble("a", 0.0).toFloat(),
        b = json.optDouble("b", 4.0).toFloat(),
        axis = json.optString("axis", "x"),
        angle = json.optDouble("angle", 360.0).toFloat(),
        range = json.optDouble("range", 5.0).toFloat(),
        x = json.optDouble("x", 0.0).toFloat(),
        y = json.optDouble("y", 0.0).toFloat(),
        z = json.optDouble("z", 0.0).toFloat(),
        coeffA = json.optDouble("coeffA", 1.0).toFloat(),
        coeffB = json.optDouble("coeffB", 1.0).toFloat(),
        coeffC = json.optDouble("coeffC", 1.0).toFloat(),
        coeffD = json.optDouble("coeffD", 2.0).toFloat(),
        subType = json.optString("subType", "CUBE"),
        size = json.optDouble("size", 4.0).toFloat(),
        height = json.optDouble("height", 5.0).toFloat(),
        radius = json.optDouble("radius", 2.5).toFloat(),
        varName = json.optString("varName", "a"),
        sliderValue = json.optDouble("sliderValue", 2.0).toFloat(),
        sliderMin = json.optDouble("sliderMin", -5.0).toFloat(),
        sliderMax = json.optDouble("sliderMax", 5.0).toFloat(),
        sliderStep = json.optDouble("sliderStep", 0.5).toFloat(),
        hasIntegral = json.optBoolean("hasIntegral", false),
        integralRiemannType = json.optString("integralRiemannType", "SMOOTH"),
        integralPartitions = json.optInt("integralPartitions", 12),
        computedValue = json.optString("computedValue", "")
    )
}

fun saveElementsToPrefs(context: Context, elements: List<GeoElementModel>) {
    try {
        val prefs = context.getSharedPreferences("math3d_prefs", Context.MODE_PRIVATE)
        val arr = JSONArray()
        elements.forEach { arr.put(geoElementToJson(it)) }
        prefs.edit().putString("saved_elements", arr.toString()).apply()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun loadElementsFromPrefs(context: Context): List<GeoElementModel> {
    try {
        val prefs = context.getSharedPreferences("math3d_prefs", Context.MODE_PRIVATE)
        val raw = prefs.getString("saved_elements", null) ?: return emptyList()
        val arr = JSONArray(raw)
        val list = mutableListOf<GeoElementModel>()
        for (i in 0 until arr.length()) {
            list.add(jsonToGeoElement(arr.getJSONObject(i)))
        }
        return list
    } catch (e: Exception) {
        e.printStackTrace()
        return emptyList()
    }
}
