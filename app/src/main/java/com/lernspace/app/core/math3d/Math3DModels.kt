package com.lernspace.app.core.math3d

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
    // Computed value / preview label
    val computedValue: String = ""
)

data class Math3DMetrics(
    val volume: Double = 0.0,
    val surfaceArea: Double = 0.0,
    val dotProduct: Double = 0.0,
    val angleDeg: Double = 0.0
)
