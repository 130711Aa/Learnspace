package com.lernspace.app.core.supabase

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.lernspace.app.core.mimolab.MimoSimulation
import com.lernspace.app.core.mimolab.MimoSubject
import com.lernspace.app.core.mimolab.MimoEnginePresets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Supabase client for Mimo Lab (Interactive STEM Simulations Community Commons).
 * Enables students to fetch public simulations, search community experiments,
 * and publish their own AI-generated simulations with offline cache fallback.
 */
object SupabaseMimoLabClient {

    private const val TAG = "SupabaseMimoLab"
    private const val CACHE_FILE_NAME = "mimo_simulations_offline_cache.json"

    const val SUPABASE_URL = "https://bcmpfokmuegoomcnpxis.supabase.co"
    const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImJjbXBmb2ttdWVnb29tY25weGlzIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTQ4OTczNTIsImV4cCI6MjExMDQ3MzM1Mn0.wKvG5K90z3l7JY02u5AvAnx_SENfEjuBQk69pkU7HOA"

    private var appContext: Context? = null

    var isOperatingOffline by mutableStateOf(false)
        private set

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /**
     * Fetch community simulations from Supabase with optional subject and search filtering.
     * Automatically falls back to offline disk cache if network is unavailable.
     */
    suspend fun fetchSimulations(
        subject: MimoSubject? = null,
        searchQuery: String = "",
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<MimoSimulation>> = withContext(Dispatchers.IO) {
        try {
            val queryParams = mutableListOf(
                "select=*",
                "order=likes_count.desc,created_at.desc",
                "limit=$limit"
            )

            if (offset > 0) {
                queryParams.add("offset=$offset")
            }

            if (subject != null && subject != MimoSubject.UMUM) {
                queryParams.add("subject=eq.${URLEncoder.encode(subject.name, "UTF-8")}")
            }

            if (searchQuery.isNotBlank()) {
                val q = URLEncoder.encode("*${searchQuery.trim()}*", "UTF-8")
                queryParams.add("or=(title.ilike.$q,description.ilike.$q)")
            }

            val urlString = "$SUPABASE_URL/rest/v1/mimo_simulations?${queryParams.joinToString("&")}"
            val url = URL(urlString)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("apikey", SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $SUPABASE_ANON_KEY")
                setRequestProperty("Accept", "application/json")
                connectTimeout = 12000
                readTimeout = 15000
            }

            val responseCode = conn.responseCode
            if (responseCode !in 200..299) {
                val errorStream = conn.errorStream?.let { InputStreamReader(it).readText() } ?: "HTTP $responseCode"
                throw Exception("Supabase HTTP $responseCode: $errorStream")
            }

            val responseBody = conn.inputStream.bufferedReader().use(BufferedReader::readText)
            val remoteSimulations = parseSimulationsJson(responseBody)
            val presets = MimoEnginePresets.getPresetSimulations()
            val presetIds = presets.map { it.id }.toSet()
            val validRemote = remoteSimulations.filter { rem ->
                !presetIds.contains(rem.id) &&
                (rem.htmlContent.contains("ResizeObserver") || rem.htmlContent.contains("lernspace-mimo-viewport-guard"))
            }
            val combined = (presets + validRemote).filter { sim ->
                val matchSubject = subject == null || subject == MimoSubject.UMUM || sim.subject == subject
                val matchQuery = searchQuery.isBlank() ||
                        sim.title.contains(searchQuery, ignoreCase = true) ||
                        sim.description.contains(searchQuery, ignoreCase = true) ||
                        sim.conceptTopic.contains(searchQuery, ignoreCase = true)
                matchSubject && matchQuery
            }

            if (combined.isNotEmpty()) {
                saveSimulationsToCache(combined)
            }
            isOperatingOffline = false

            Result.success(combined)
        } catch (e: Exception) {
            Log.w(TAG, "fetchSimulations network error: ${e.message}. Falling back to cache.")
            val cached = loadSimulationsFromCache()
            if (cached.isNotEmpty()) {
                isOperatingOffline = true
                var filtered = cached
                if (subject != null && subject != MimoSubject.UMUM) {
                    filtered = filtered.filter { it.subject == subject }
                }
                if (searchQuery.isNotBlank()) {
                    val q = searchQuery.trim().lowercase()
                    filtered = filtered.filter {
                        it.title.lowercase().contains(q) || it.description.lowercase().contains(q)
                    }
                }
                Result.success(filtered)
            } else {
                // If offline cache is empty, provide starter simulations
                val presets = MimoEnginePresets.getPresetSimulations().filter { sim ->
                    val matchSubject = subject == null || subject == MimoSubject.UMUM || sim.subject == subject
                    val matchQuery = searchQuery.isBlank() ||
                            sim.title.contains(searchQuery, ignoreCase = true) ||
                            sim.description.contains(searchQuery, ignoreCase = true) ||
                            sim.conceptTopic.contains(searchQuery, ignoreCase = true)
                    matchSubject && matchQuery
                }
                Result.success(presets)
            }
        }
    }

    /**
     * Publish a newly generated simulation to Supabase Community Commons.
     */
    suspend fun publishSimulation(simulation: MimoSimulation): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val simObj = JSONObject().apply {
                put("id", simulation.id)
                put("title", simulation.title)
                put("description", simulation.description)
                put("subject", simulation.subject.name)
                put("html_code", simulation.htmlContent)
                put("author_name", simulation.authorName)
                put("likes_count", simulation.likesCount)
            }

            val body = JSONArray().put(simObj).toString()
            val success = postJson("$SUPABASE_URL/rest/v1/mimo_simulations", body)
            if (success) {
                // Also add to local cache
                val current = loadSimulationsFromCache().toMutableList()
                if (current.none { it.id == simulation.id }) {
                    current.add(0, simulation.copy(isCommunityOrigin = true))
                    saveSimulationsToCache(current)
                }
                Result.success(true)
            } else {
                Result.failure(Exception("Gagal mengunggah simulasi ke Supabase."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "publishSimulation error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Increment like count for a simulation.
     */
    suspend fun incrementLike(simulationId: String, newCount: Int): Result<Int> = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val urlString = "$SUPABASE_URL/rest/v1/mimo_simulations?id=eq.$simulationId"
            val url = URL(urlString)
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "PATCH"
                setRequestProperty("apikey", SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $SUPABASE_ANON_KEY")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Prefer", "return=representation")
                connectTimeout = 12000
                readTimeout = 15000
                doOutput = true
            }

            val body = JSONObject().apply {
                put("likes_count", newCount)
            }.toString()

            OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
                writer.write(body)
                writer.flush()
            }

            val code = conn.responseCode
            if (code in 200..299) {
                // Update local cache
                val current = loadSimulationsFromCache().map {
                    if (it.id == simulationId) it.copy(likesCount = newCount) else it
                }
                saveSimulationsToCache(current)
                Result.success(newCount)
            } else {
                Result.failure(Exception("HTTP $code updating likes"))
            }
        } catch (e: Exception) {
            Log.w(TAG, "incrementLike error: ${e.message}")
            Result.failure(e)
        } finally {
            conn?.disconnect()
        }
    }

    private fun postJson(urlString: String, jsonBody: String): Boolean {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("apikey", SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $SUPABASE_ANON_KEY")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Prefer", "resolution=merge-duplicates,return=minimal")
                connectTimeout = 15000
                readTimeout = 20000
                doOutput = true
            }

            OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
                writer.write(jsonBody)
                writer.flush()
            }

            val code = conn.responseCode
            code in 200..299
        } catch (e: Exception) {
            Log.e(TAG, "postJson exception: ${e.message}")
            false
        } finally {
            conn?.disconnect()
        }
    }

    private fun parseSimulationsJson(jsonString: String): List<MimoSimulation> {
        val jsonArray = JSONArray(jsonString)
        val list = mutableListOf<MimoSimulation>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val id = obj.optString("id")
            val title = obj.optString("title", "Simulasi Sains")
            val desc = obj.optString("description", "")
            val subjectStr = obj.optString("subject", "UMUM")
            val html = obj.optString("html_code", "")
            val author = obj.optString("author_name", "Mimo AI")
            val likes = obj.optInt("likes_count", 0)

            list.add(
                MimoSimulation(
                    id = id,
                    title = title,
                    subject = MimoSubject.fromString(subjectStr),
                    conceptTopic = title,
                    description = desc,
                    htmlContent = html,
                    isCommunityOrigin = true,
                    authorName = author,
                    likesCount = likes,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
        return list
    }

    private fun saveSimulationsToCache(simulations: List<MimoSimulation>) {
        val ctx = appContext ?: return
        try {
            val file = File(ctx.filesDir, CACHE_FILE_NAME)
            val jsonArray = JSONArray()
            simulations.forEach { sim ->
                jsonArray.put(JSONObject().apply {
                    put("id", sim.id)
                    put("title", sim.title)
                    put("subject", sim.subject.name)
                    put("description", sim.description)
                    put("conceptTopic", sim.conceptTopic)
                    put("htmlContent", sim.htmlContent)
                    put("authorName", sim.authorName)
                    put("likesCount", sim.likesCount)
                    put("viewsCount", sim.viewsCount)
                    put("createdAt", sim.createdAt)
                })
            }
            file.writeText(jsonArray.toString())
        } catch (e: Exception) {
            Log.w(TAG, "Error saving simulations to cache: ${e.message}")
        }
    }

    private fun loadSimulationsFromCache(): List<MimoSimulation> {
        val ctx = appContext ?: return emptyList()
        try {
            val file = File(ctx.filesDir, CACHE_FILE_NAME)
            if (!file.exists()) return emptyList()
            val text = file.readText()
            val jsonArray = JSONArray(text)
            val list = mutableListOf<MimoSimulation>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    MimoSimulation(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        subject = MimoSubject.fromString(obj.optString("subject", "UMUM")),
                        conceptTopic = obj.optString("conceptTopic", ""),
                        description = obj.optString("description", ""),
                        htmlContent = obj.optString("htmlContent", ""),
                        authorName = obj.optString("authorName", "Mimo AI"),
                        likesCount = obj.optInt("likesCount", 0),
                        viewsCount = obj.optInt("viewsCount", 0),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        isCommunityOrigin = true
                    )
                )
            }
            return list
        } catch (e: Exception) {
            Log.w(TAG, "Error loading simulations from cache: ${e.message}")
            return emptyList()
        }
    }

    /**
     * Curated built-in STEM simulations available offline out-of-the-box.
     * Powered by MimoEnginePresets (reverse-engineered Clean White simulation engine).
     */
    fun getBuiltInStarterSimulations(): List<MimoSimulation> {
        return MimoEnginePresets.getPresetSimulations()
    }
}
