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
        AppLog.Storage.i("ensureFolderExists -> $RELATIVE_FOLDER")
        if (directFolderReady()) {
            AppLog.Storage.i("Folder ready: ${getPublicFolderFile().absolutePath}")
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
            AppLog.Storage.i("ensureFolderExists success — visible in Files app under Documents/$FOLDER_NAME")
            return true
        }
        RecordingPreferences.setFolderReady(context, false)
        AppLog.Storage.e("ensureFolderExists FAILED — check storage permission / Android version")
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
                AppLog.Storage.w("mkdirs failed: ${dir.absolutePath}")
                return false
            }
            val marker = File(dir, FOLDER_MARKER_FILE)
            if (!marker.exists()) {
                marker.writeText(
                    "Call recordings folder for Mes Validation.\n" +
                        "Helper app and Mes Validation (com.mesvalidation) use this location.\n"
                )
            }
            AppLog.Storage.i("Direct folder created: ${dir.absolutePath}")
            true
        } catch (e: Exception) {
            AppLog.Storage.e("ensureDirectDocumentsFolder failed", e)
            false
        }
    }

    private fun ensureFolderViaMediaStore(context: Context): Boolean {
        if (ensureDirectDocumentsFolder()) return true
        if (folderMarkerExistsInMediaStore(context)) {
            AppLog.Storage.d("MediaStore marker already exists")
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
            AppLog.Storage.e("MediaStore insert returned null for folder marker")
            return ensureDirectDocumentsFolder()
        }
        return try {
            resolver.openOutputStream(uri)?.use { stream ->
                stream.write(
                    "Call recordings folder for Mes Validation.\n".toByteArray()
                )
            } ?: run {
                AppLog.Storage.e("openOutputStream null for folder marker")
                return ensureDirectDocumentsFolder()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val done = ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }
                resolver.update(uri, done, null, null)
            }
            AppLog.Storage.i("MediaStore folder marker created: $uri")
            ensureDirectDocumentsFolder() || folderMarkerExistsInMediaStore(context)
        } catch (e: Exception) {
            AppLog.Storage.e("ensureFolderViaMediaStore failed", e)
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
        AppLog.Storage.detail(
            "save_start",
            "fileName" to fileName,
            "tempBytes" to tempFile.length(),
            "tempPath" to tempFile.absolutePath,
            "targetFolder" to RELATIVE_FOLDER,
            "sdk" to Build.VERSION.SDK_INT,
            "folderReadyOnDisk" to directFolderReady()
        )
        if (!tempFile.exists() || tempFile.length() == 0L) {
            AppLog.Storage.w("save skipped: empty or missing temp ($fileName)")
            tempFile.delete()
            return null
        }
        ensureFolderExists(context)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (directFolderReady()) {
                AppLog.Storage.d("Save strategy: direct file first, MediaStore.Files fallback")
                saveViaDirectFile(tempFile, fileName, "primary")
                    ?: saveViaMediaStoreDocuments(context, tempFile, fileName, "fallback")
            } else {
                AppLog.Storage.d("Save strategy: MediaStore.Files first, direct file fallback")
                saveViaMediaStoreDocuments(context, tempFile, fileName, "primary")
                    ?: saveViaDirectFile(tempFile, fileName, "fallback")
            }
        } else {
            AppLog.Storage.d("Save strategy: legacy direct file (API < 29)")
            saveViaLegacyPath(tempFile, fileName)
        }
    }

    private fun saveViaDirectFile(tempFile: File, fileName: String, role: String = "direct"): Uri? {
        return try {
            val dir = getPublicFolderFile()
            if (!dir.exists()) dir.mkdirs()
            val dest = File(dir, fileName)
            tempFile.copyTo(dest, overwrite = true)
            tempFile.delete()
            AppLog.Storage.detail(
                "save_direct_ok",
                "role" to role,
                "fileName" to dest.name,
                "bytes" to dest.length(),
                "absolutePath" to dest.absolutePath,
                "fileManager" to "Internal storage/Documents/$FOLDER_NAME/${dest.name}"
            )
            Uri.fromFile(dest)
        } catch (e: Exception) {
            AppLog.Storage.e("saveViaDirectFile failed (role=$role)", e)
            null
        }
    }

    /**
     * Saves into Documents/MesValidationCallRecorder via MediaStore.Files (not Audio —
     * Audio only allows Music/Recordings/etc. and rejects Documents/).
     */
    private fun saveViaMediaStoreDocuments(
        context: Context,
        tempFile: File,
        fileName: String,
        role: String = "mediastore"
    ): Uri? {
        val resolver = context.contentResolver
        val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        return try {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeTypeForFile(fileName))
                put(MediaStore.MediaColumns.RELATIVE_PATH, "$RELATIVE_FOLDER/")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = resolver.insert(collection, values)
            if (uri == null) {
                AppLog.Storage.w("MediaStore.Files insert null (role=$role) fileName=$fileName")
                return null
            }
            resolver.openOutputStream(uri)?.use { out ->
                FileInputStream(tempFile).use { it.copyTo(out) }
            } ?: run {
                AppLog.Storage.e("openOutputStream null for $fileName")
                resolver.delete(uri, null, null)
                return null
            }
            val done = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }
            resolver.update(uri, done, null, null)
            tempFile.delete()
            AppLog.Storage.detail(
                "save_mediastore_ok",
                "role" to role,
                "fileName" to fileName,
                "uri" to uri.toString(),
                "mime" to mimeTypeForFile(fileName),
                "fileManager" to "Internal storage/Documents/$FOLDER_NAME/$fileName"
            )
            uri
        } catch (e: Exception) {
            AppLog.Storage.e("saveViaMediaStoreDocuments failed (role=$role) fileName=$fileName", e)
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
        return buildFileName(direction, phoneNumber, "amr")
    }

    fun buildFileName(direction: CallDirection, phoneNumber: String?, extension: String): String {
        val ext = extension.lowercase(java.util.Locale.US)
        if (ext == "amr") {
            return buildCubeStyleFileName(phoneNumber)
        }
        val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US)
            .format(java.util.Date())
        val dir = when (direction) {
            CallDirection.INCOMING -> "IN"
            CallDirection.OUTGOING -> "OUT"
        }
        val number = phoneNumber?.filter { it.isDigit() || it == '+' }?.take(20) ?: "unknown"
        return "${timestamp}_${dir}_${number}.$ext"
    }

    /** Cube ACR Helper: phone_20260527-132812__918347112610.amr */
    fun buildCubeStyleFileName(phoneNumber: String?): String {
        val timestamp = java.text.SimpleDateFormat("yyyyMMdd-HHmmss", java.util.Locale.US)
            .format(java.util.Date())
        val number = phoneNumber?.filter { it.isDigit() }?.take(20) ?: "unknown"
        return "phone_${timestamp}__${number}.amr"
    }

    private fun mimeTypeForFile(fileName: String): String {
        return when (fileName.substringAfterLast('.', "").lowercase(java.util.Locale.US)) {
            "wav" -> "audio/wav"
            "amr" -> "audio/amr"
            "m4a", "mp4" -> "audio/mp4"
            else -> "audio/*"
        }
    }
}

enum class CallDirection {
    INCOMING,
    OUTGOING
}
