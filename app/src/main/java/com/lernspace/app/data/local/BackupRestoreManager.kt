package com.lernspace.app.data.local

import android.content.Context
import android.net.Uri
import com.lernspace.app.core.ai.GeminiApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class BackupMetadata(
    val appVersion: String,
    val backupTimestamp: Long,
    val noteCount: Int,
    val databaseVersion: Int
)

data class BackupResult(
    val success: Boolean,
    val message: String,
    val filePath: String? = null,
    val sizeBytes: Long = 0L
)

data class RestoreResult(
    val success: Boolean,
    val message: String,
    val restoredNotesCount: Int = 0
)

/**
 * Robust Backup & Restore Engine for Lernspace.
 * Packages the SQLite Room database, all note strokes/JSON, images, and imported PDFs
 * into a single compressed `.lernspace` (ZIP) archive.
 */
object BackupRestoreManager {

    private const val BACKUP_EXTENSION = ".lernspace"
    private const val METADATA_FILENAME = "backup_metadata.json"
    private const val DB_DIR_NAME = "database"
    private const val NOTES_DIR_NAME = "notes"
    private const val PREFS_DIR_NAME = "preferences"

    fun generateDefaultBackupFileName(): String {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        return "Lernspace_Backup_${sdf.format(Date())}$BACKUP_EXTENSION"
    }

    /**
     * Exports entire workspace to a ZIP stream (written directly to the user-selected Uri).
     */
    suspend fun createBackup(
        context: Context,
        outputUri: Uri
    ): BackupResult = withContext(Dispatchers.IO) {
        try {
            // 1. Flush any pending SQLite WAL checkpoint
            val db = LernspaceDatabase.getInstance(context)
            try {
                val dbHelper = db.openHelper.writableDatabase
                dbHelper.query("PRAGMA wal_checkpoint(FULL)").close()
            } catch (_: Exception) {
                // Ignore if in-memory or already checkpointed
            }

            val appCtx = context.applicationContext
            val contentResolver = appCtx.contentResolver

            val outputStream = contentResolver.openOutputStream(outputUri)
                ?: return@withContext BackupResult(false, "Tidak dapat membuka output storage.")

            var writtenBytes = 0L

            ZipOutputStream(BufferedOutputStream(outputStream)).use { zipOut ->
                // 1. Write Metadata
                val notesDir = File(appCtx.filesDir, "notes")
                val noteJsonFiles = notesDir.listFiles { f -> f.extension == "json" } ?: emptyArray()
                val metaJson = JSONObject().apply {
                    put("app", "Lernspace")
                    put("schema_version", 1)
                    put("database_version", 3)
                    put("created_at", System.currentTimeMillis())
                    put("note_count", noteJsonFiles.size)
                }.toString()

                zipOut.putNextEntry(ZipEntry(METADATA_FILENAME))
                val metaBytes = metaJson.toByteArray(Charsets.UTF_8)
                zipOut.write(metaBytes)
                zipOut.closeEntry()
                writtenBytes += metaBytes.size

                // 2. Backup Database files (lernspace_database, -wal, -shm)
                val dbFile = appCtx.getDatabasePath("lernspace_database")
                if (dbFile.exists()) {
                    addFileToZip(zipOut, dbFile, "$DB_DIR_NAME/${dbFile.name}")
                    writtenBytes += dbFile.length()

                    val walFile = File(dbFile.parentFile, "lernspace_database-wal")
                    if (walFile.exists()) {
                        addFileToZip(zipOut, walFile, "$DB_DIR_NAME/${walFile.name}")
                        writtenBytes += walFile.length()
                    }

                    val shmFile = File(dbFile.parentFile, "lernspace_database-shm")
                    if (shmFile.exists()) {
                        addFileToZip(zipOut, shmFile, "$DB_DIR_NAME/${shmFile.name}")
                        writtenBytes += shmFile.length()
                    }
                }

                // 3. Backup Notes Folder (strokes JSON, images/, pdfs/)
                if (notesDir.exists()) {
                    writtenBytes += addDirectoryToZip(zipOut, notesDir, NOTES_DIR_NAME)
                }

                // 4. Backup Preferences
                val prefsFile = File(appCtx.applicationInfo.dataDir, "shared_prefs/lernspace_settings.xml")
                if (prefsFile.exists()) {
                    addFileToZip(zipOut, prefsFile, "$PREFS_DIR_NAME/${prefsFile.name}")
                    writtenBytes += prefsFile.length()
                }
            }

            BackupResult(
                success = true,
                message = "Cadangan berhasil disimpan (${writtenBytes / 1024} KB).",
                sizeBytes = writtenBytes
            )
        } catch (e: Exception) {
            BackupResult(false, "Gagal membuat cadangan: ${e.localizedMessage ?: e.message}")
        }
    }

    /**
     * Restores entire workspace from a user-selected `.lernspace` (ZIP) Uri.
     */
    suspend fun restoreBackup(
        context: Context,
        inputUri: Uri
    ): RestoreResult = withContext(Dispatchers.IO) {
        try {
            val appCtx = context.applicationContext
            val contentResolver = appCtx.contentResolver

            val inputStream = contentResolver.openInputStream(inputUri)
                ?: return@withContext RestoreResult(false, "Tidak dapat membaca file cadangan.")

            val tempExtractDir = File(appCtx.cacheDir, "temp_restore_${System.currentTimeMillis()}")
            if (!tempExtractDir.mkdirs()) {
                return@withContext RestoreResult(false, "Gagal menyiapkan direktori restorasi.")
            }

            try {
                // 1. Unzip everything to tempExtractDir
                ZipInputStream(BufferedInputStream(inputStream)).use { zipIn ->
                    var entry: ZipEntry? = zipIn.nextEntry
                    val buffer = ByteArray(8192)

                    while (entry != null) {
                        val newFile = File(tempExtractDir, entry.name)
                        // Security check: prevent zip slip
                        if (!newFile.canonicalPath.startsWith(tempExtractDir.canonicalPath)) {
                            throw SecurityException("File cadangan mengandung path yang tidak aman: ${entry.name}")
                        }

                        if (entry.isDirectory) {
                            newFile.mkdirs()
                        } else {
                            newFile.parentFile?.mkdirs()
                            FileOutputStream(newFile).use { fos ->
                                var len: Int
                                while (zipIn.read(buffer).also { len = it } > 0) {
                                    fos.write(buffer, 0, len)
                                }
                            }
                        }
                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                    }
                }

                // 2. Validate metadata
                val metaFile = File(tempExtractDir, METADATA_FILENAME)
                if (!metaFile.exists()) {
                    return@withContext RestoreResult(false, "File cadangan tidak valid (metadata hilang).")
                }

                // 3. Close active Room DB connection before file replacement
                val currentDb = LernspaceDatabase.getInstance(appCtx)
                try {
                    currentDb.close()
                } catch (_: Exception) {}

                // 4. Replace Database files
                val restoredDbDir = File(tempExtractDir, DB_DIR_NAME)
                if (restoredDbDir.exists()) {
                    val targetDbFile = appCtx.getDatabasePath("lernspace_database")
                    targetDbFile.parentFile?.mkdirs()

                    File(restoredDbDir, "lernspace_database").let {
                        if (it.exists()) it.copyTo(targetDbFile, overwrite = true)
                    }
                    File(restoredDbDir, "lernspace_database-wal").let {
                        val targetWal = File(targetDbFile.parentFile, "lernspace_database-wal")
                        if (it.exists()) it.copyTo(targetWal, overwrite = true) else targetWal.delete()
                    }
                    File(restoredDbDir, "lernspace_database-shm").let {
                        val targetShm = File(targetDbFile.parentFile, "lernspace_database-shm")
                        if (it.exists()) it.copyTo(targetShm, overwrite = true) else targetShm.delete()
                    }
                }

                // 5. Replace Notes directory
                val restoredNotesDir = File(tempExtractDir, NOTES_DIR_NAME)
                val targetNotesDir = File(appCtx.filesDir, "notes")
                if (restoredNotesDir.exists()) {
                    targetNotesDir.mkdirs()
                    restoredNotesDir.copyRecursively(targetNotesDir, overwrite = true)
                }

                // 6. Replace Preferences (if present)
                val restoredPrefsDir = File(tempExtractDir, PREFS_DIR_NAME)
                if (restoredPrefsDir.exists()) {
                    val targetPrefsDir = File(appCtx.applicationInfo.dataDir, "shared_prefs")
                    targetPrefsDir.mkdirs()
                    File(restoredPrefsDir, "lernspace_settings.xml").let {
                        if (it.exists()) it.copyTo(File(targetPrefsDir, "lernspace_settings.xml"), overwrite = true)
                    }
                }

                // Count restored notes
                val noteCount = targetNotesDir.listFiles { f -> f.extension == "json" }?.size ?: 0

                // 7. Reload in-memory SQLite and repository state
                val freshDb = LernspaceDatabase.getInstance(appCtx)
                AcademicDatabaseManager.init(freshDb, appCtx)
                AppSettingsManager.init(appCtx)

                RestoreResult(
                    success = true,
                    message = "Restorasi berhasil! $noteCount catatan telah dipulihkan.",
                    restoredNotesCount = noteCount
                )
            } finally {
                tempExtractDir.deleteRecursively()
            }
        } catch (e: Exception) {
            RestoreResult(false, "Gagal memulihkan data: ${e.localizedMessage ?: e.message}")
        }
    }

    private fun addFileToZip(zipOut: ZipOutputStream, file: File, entryPath: String) {
        zipOut.putNextEntry(ZipEntry(entryPath))
        FileInputStream(file).use { fis ->
            fis.copyTo(zipOut)
        }
        zipOut.closeEntry()
    }

    private fun addDirectoryToZip(zipOut: ZipOutputStream, folder: File, baseName: String): Long {
        var totalBytes = 0L
        val files = folder.listFiles() ?: return 0L
        for (f in files) {
            val entryPath = "$baseName/${f.name}"
            if (f.isDirectory) {
                totalBytes += addDirectoryToZip(zipOut, f, entryPath)
            } else {
                addFileToZip(zipOut, f, entryPath)
                totalBytes += f.length()
            }
        }
        return totalBytes
    }
}
