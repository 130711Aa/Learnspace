package com.lernspace.app.core.updater

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class AppReleaseInfo(
    val tagName: String,         // e.g. "v1.0.0"
    val versionName: String,     // e.g. "1.0.0"
    val title: String,           // e.g. "Learnspace v1.0.0 - Initial Production Release"
    val changelog: String,       // description body
    val apkDownloadUrl: String,  // direct download url
    val releasePageUrl: String,  // browser html url
    val publishedAt: String,     // timestamp
    val hasUpdate: Boolean       // true if remote > local
)

object UpdateManager {
    private const val GITHUB_REPO = "130711Aa/Learnspace"
    private const val API_URL = "https://api.github.com/repos/$GITHUB_REPO/releases?per_page=5"
    const val RELEASES_PAGE_URL = "https://github.com/$GITHUB_REPO/releases"

    /**
     * Checks GitHub Releases API for the latest release (including stable and pre-release)
     * and compares with current app version.
     */
    suspend fun checkForUpdates(currentVersion: String = "1.2.1"): Result<AppReleaseInfo> = withContext(Dispatchers.IO) {
        try {
            val url = URL(API_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "Learnspace-Android-App")
            }

            if (connection.responseCode != 200) {
                return@withContext Result.failure(Exception("GitHub API HTTP Error: ${connection.responseCode}"))
            }

            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val response = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                response.append(line)
            }
            reader.close()

            val releasesArray = org.json.JSONArray(response.toString())
            if (releasesArray.length() == 0) {
                return@withContext Result.failure(Exception("Belum ada rilis di GitHub."))
            }

            // Find the latest non-draft release (supports both stable and pre-releases)
            var targetRelease: JSONObject? = null
            for (i in 0 until releasesArray.length()) {
                val item = releasesArray.getJSONObject(i)
                if (!item.optBoolean("draft", false)) {
                    targetRelease = item
                    break
                }
            }

            if (targetRelease == null) {
                return@withContext Result.failure(Exception("Tidak ditemukan rilis yang aktif."))
            }

            val json = targetRelease
            val tagName = json.optString("tag_name", "")
            val title = json.optString("name", "Learnspace Release")
            val changelog = json.optString("body", "Tidak ada catatan rilis.")
            val htmlUrl = json.optString("html_url", RELEASES_PAGE_URL)
            val publishedAt = json.optString("published_at", "")

            // Find APK asset
            val assets = json.optJSONArray("assets")
            var apkUrl = ""
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val assetName = asset.optString("name", "")
                    if (assetName.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url", "")
                        break
                    }
                }
            }

            val cleanRemoteVersion = tagName.removePrefix("v").removePrefix("V").trim()
            val cleanLocalVersion = currentVersion.removePrefix("v").removePrefix("V").trim()
            val isNewer = compareSemVer(cleanRemoteVersion, cleanLocalVersion) > 0

            val releaseInfo = AppReleaseInfo(
                tagName = tagName,
                versionName = cleanRemoteVersion,
                title = title,
                changelog = changelog,
                apkDownloadUrl = apkUrl,
                releasePageUrl = htmlUrl,
                publishedAt = publishedAt,
                hasUpdate = isNewer
            )

            Result.success(releaseInfo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Compares two semantic version strings (e.g. "1.0.1" vs "1.0.0").
     * Returns > 0 if v1 > v2, < 0 if v1 < v2, 0 if equal.
     */
    fun compareSemVer(v1: String, v2: String): Int {
        val parts1 = v1.split(".").mapNotNull { it.takeWhile { c -> c.isDigit() }.toIntOrNull() }
        val parts2 = v2.split(".").mapNotNull { it.takeWhile { c -> c.isDigit() }.toIntOrNull() }
        val maxLen = maxOf(parts1.size, parts2.size)

        for (i in 0 until maxLen) {
            val p1 = parts1.getOrElse(i) { 0 }
            val p2 = parts2.getOrElse(i) { 0 }
            if (p1 != p2) return p1.compareTo(p2)
        }
        return 0
    }

    /**
     * Downloads APK using Android DownloadManager and triggers package install upon completion.
     */
    fun startApkDownload(context: Context, downloadUrl: String, versionName: String) {
        if (downloadUrl.isBlank()) {
            openInBrowser(context, RELEASES_PAGE_URL)
            return
        }

        try {
            val fileName = "Learnspace-v$versionName.apk"
            val destinationFile = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                fileName
            )

            if (destinationFile.exists()) {
                destinationFile.delete()
            }

            val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
                setTitle("Mengunduh Learnspace v$versionName")
                setDescription("Mengunduh paket pembaruan aplikasi...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationUri(Uri.fromFile(destinationFile))
                setMimeType("application/vnd.android.package-archive")
            }

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = downloadManager.enqueue(request)

            Toast.makeText(context, "Mulai mengunduh Learnspace v$versionName...", Toast.LENGTH_SHORT).show()

            // Register broadcast receiver to automatically prompt install when download finishes
            val onCompleteReceiver = object : BroadcastReceiver() {
                override fun onReceive(ctxt: Context, intent: Intent) {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                    if (id == downloadId) {
                        try {
                            ctxt.unregisterReceiver(this)
                        } catch (_: Exception) {}

                        installApk(ctxt, destinationFile)
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    onCompleteReceiver,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                    Context.RECEIVER_NOT_EXPORTED
                )
            } else {
                context.registerReceiver(
                    onCompleteReceiver,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
                )
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal mengunduh: ${e.message}. Membuka browser...", Toast.LENGTH_LONG).show()
            openInBrowser(context, downloadUrl)
        }
    }

    /**
     * Prompts the Android Package Installer for the downloaded APK file.
     */
    fun installApk(context: Context, apkFile: File) {
        if (!apkFile.exists()) {
            Toast.makeText(context, "File installer tidak ditemukan.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(installIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membuka installer: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Opens a URL in the user's default web browser.
     */
    fun openInBrowser(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membuka tautan: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

