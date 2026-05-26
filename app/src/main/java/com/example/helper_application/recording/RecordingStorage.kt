package com.example.helper_application.recording

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.helper_application.util.AppLog
import java.io.File
import java.io.FileInputStream

object RecordingStorage {

    /** Same style as Cube: Internal storage → Documents → MesValidationCallRecorder */
    const val FOLDER_NAME = "MesValidationCallRecorder"
    const val RELATIVE_FOLDER = "Documents/$FOLDER_NAME"
    const val FOLDER_MARKER_FILE = "README.txt"

    fun getPublicFolderFile(): File {
        return File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            FOLDER_NAME
        )
    }

    /**
     * Creates Documents/MesValidationCallRecorder (like Cube Documents/CubeCallRecorder).
     */
    fun ensureFolderExists(context: Context): Boolean {
        AppLog.i("ensureFolderExists start → $RELATIVE_FOLDER")
        if (directFolderReady()) {
            AppLog.i("Folder ready via direct path: ${getPublicFolderFile().absolutePath}")
            RecordingPreferences.setFolderReady(context, true)
            return true
        }
        val viaMediaStore = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ensureFolderViaMediaStore(context)
        } else {
            ensureFolderLegacy()
        }
        if (viaMediaStore || directFolderReady()) {
            RecordingPreferences.setFolderReady(context, true)
            AppLog.i("ensureFolderExists success")
            return true
        }
        RecordingPreferences.setFolderReady(context, false)
        AppLog.e("ensureFolderExists FAILED — check storage permission / Android version")
        return false
    }

    fun isFolderReadyOnDisk(): Boolean = directFolderReady()

    private fun directFolderReady(): Boolean {
        val dir = getPublicFolderFile()
        val marker = File(dir, FOLDER_MARKER_FILE)
        return dir.isDirectory && marker.isFile && marker.length() > 0L
    }

    @Suppress("DEPRECATION")
    private fun ensureDirectDocumentsFolder(): Boolean {
        return try {
            val dir = getPublicFolderFile()
            if (!dir.exists() && !dir.mkdirs()) {
                AppLog.w("mkdirs failed: ${dir.absolutePath}")
                return false
            }
            val marker = File(dir, FOLDER_MARKER_FILE)
            if (!marker.exists()) {
                marker.writeText(
                    "Call recordings folder for Mes Validation.\n" +
                        "Helper app and Mes Validation (com.mesvalidation) use this location.\n"
                )
            }
            AppLog.i("Direct folder: ${dir.absolutePath}, marker=${marker.length()} bytes")
            true
        } catch (e: Exception) {
            AppLog.e("ensureDirectDocumentsFolder failed", e)
            false
        }
    }

    private fun ensureFolderViaMediaStore(context: Context): Boolean {
        if (ensureDirectDocumentsFolder()) return true
        if (folderMarkerExistsInMediaStore(context)) {
            AppLog.d("MediaStore marker already exists")
            return true
        }
        val resolver = context.contentResolver
        val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, FOLDER_MARKER_FILE)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "$RELATIVE_FOLDER/")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(collection, values)
        if (uri == null) {
            AppLog.e("MediaStore insert returned null for folder marker")
            return ensureDirectDocumentsFolder()
        }
        return try {
            resolver.openOutputStream(uri)?.use { stream ->
                stream.write(
                    "Call recordings folder for Mes Validation.\n".toByteArray()
                )
            } ?: run {
                AppLog.e("openOutputStream null for folder marker")
                return ensureDirectDocumentsFolder()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val done = ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }
                resolver.update(uri, done, null, null)
            }
            AppLog.i("MediaStore folder marker created: $uri")
            ensureDirectDocumentsFolder() || folderMarkerExistsInMediaStore(context)
        } catch (e: Exception) {
            AppLog.e("ensureFolderViaMediaStore failed", e)
            resolver.delete(uri, null, null)
            ensureDirectDocumentsFolder()
        }
    }

    private fun folderMarkerExistsInMediaStore(context: Context): Boolean {
        val resolver = context.contentResolver
        val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val projection = arrayOf(MediaStore.MediaColumns._ID)
        val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ? AND " +
            "${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
        val args = arrayOf("%$FOLDER_NAME%", FOLDER_MARKER_FILE)
        return resolver.query(collection, projection, selection, args, null)?.use { cursor ->
            cursor.moveToFirst()
        } ?: false
    }

    fun saveRecording(context: Context, tempFile: File, fileName: String): Uri? {
        if (!tempFile.exists() || tempFile.length() == 0L) {
            AppLog.w("saveRecording skipped: empty or missing temp file ($fileName)")
            tempFile.delete()
            return null
        }
        ensureFolderExists(context)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveViaMediaStore(context, tempFile, fileName) ?: saveViaDirectFile(tempFile, fileName)
        } else {
            saveViaLegacyPath(tempFile, fileName)
        }
    }

    private fun saveViaDirectFile(tempFile: File, fileName: String): Uri? {
        return try {
            val dir = getPublicFolderFile()
            if (!dir.exists()) dir.mkdirs()
            val dest = File(dir, fileName)
            tempFile.copyTo(dest, overwrite = true)
            tempFile.delete()
            AppLog.i("Saved via direct file: ${dest.absolutePath}")
            Uri.fromFile(dest)
        } catch (e: Exception) {
            AppLog.e("saveViaDirectFile failed", e)
            null
        }
    }

    private fun saveViaMediaStore(context: Context, tempFile: File, fileName: String): Uri? {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
            put(MediaStore.Audio.Media.RELATIVE_PATH, "$RELATIVE_FOLDER/")
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        return try {
            resolver.openOutputStream(uri)?.use { out ->
                FileInputStream(tempFile).use { it.copyTo(out) }
            }
            val done = ContentValues().apply {
                put(MediaStore.Audio.Media.IS_PENDING, 0)
            }
            resolver.update(uri, done, null, null)
            tempFile.delete()
            uri
        } catch (e: Exception) {
            AppLog.e("saveViaMediaStore failed for $fileName", e)
            resolver.delete(uri, null, null)
            null
        }
    }

    @Suppress("DEPRECATION")
    private fun ensureFolderLegacy(): Boolean {
        return ensureDirectDocumentsFolder()
    }

    @Suppress("DEPRECATION")
    private fun saveViaLegacyPath(tempFile: File, fileName: String): Uri? {
        return saveViaDirectFile(tempFile, fileName)
    }

    fun buildFileName(direction: CallDirection, phoneNumber: String?): String {
        val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US)
            .format(java.util.Date())
        val dir = when (direction) {
            CallDirection.INCOMING -> "IN"
            CallDirection.OUTGOING -> "OUT"
        }
        val number = phoneNumber?.filter { it.isDigit() || it == '+' }?.take(20) ?: "unknown"
        return "${timestamp}_${dir}_${number}.m4a"
    }
}

enum class CallDirection {
    INCOMING,
    OUTGOING
}
