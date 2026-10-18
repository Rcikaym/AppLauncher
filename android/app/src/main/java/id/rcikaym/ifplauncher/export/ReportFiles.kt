package id.rcikaym.ifplauncher.export

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File

sealed interface SaveResult {
    data class Ok(val location: String) : SaveResult
    data object Failed : SaveResult
}

class ReportFiles(private val context: Context) {
    fun needsLegacyWrite(): Boolean = Build.VERSION.SDK_INT <= 28

    fun hasLegacyWrite(): Boolean {
        if (!needsLegacyWrite()) return true
        return context.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED
    }

    fun shareText(text: String, title: String): Boolean {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, title)
        }
        return launch(send, title)
    }

    fun shareFile(name: String, mime: String, bytes: ByteArray, title: String): Boolean {
        val file = writeCache(name, bytes)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri(name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return launch(send, title)
    }

    fun saveDownloads(name: String, mime: String, bytes: ByteArray): SaveResult {
        return if (Build.VERSION.SDK_INT >= 29) saveMediaStore(name, mime, bytes) else saveLegacy(name, bytes)
    }

    private fun launch(send: Intent, title: String): Boolean {
        val chooser = Intent.createChooser(send, title)
        if (context !is Activity) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(chooser)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }

    private fun writeCache(name: String, bytes: ByteArray): File {
        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        return File(dir, name).also { it.writeBytes(bytes) }
    }

    private fun saveMediaStore(name: String, mime: String, bytes: ByteArray): SaveResult {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: return SaveResult.Failed
        return try {
            context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                ?: return SaveResult.Failed
            SaveResult.Ok(name)
        } catch (_: Exception) {
            context.contentResolver.delete(uri, null, null)
            SaveResult.Failed
        }
    }

    private fun saveLegacy(name: String, bytes: ByteArray): SaveResult {
        return try {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!dir.exists() && !dir.mkdirs()) return SaveResult.Failed
            val file = File(dir, name)
            file.writeBytes(bytes)
            SaveResult.Ok(file.absolutePath)
        } catch (_: Exception) {
            SaveResult.Failed
        }
    }
}
