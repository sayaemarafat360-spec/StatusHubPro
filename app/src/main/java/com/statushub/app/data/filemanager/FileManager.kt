package com.statushub.app.data.filemanager

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.statushub.app.BuildConfig
import com.statushub.app.R
import com.statushub.app.data.model.SaveLocation
import com.statushub.app.data.model.SavedStatus
import com.statushub.app.data.model.Status
import com.statushub.app.data.model.StatusType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        const val WHATSAPP_STATUSES_PATH = "WhatsApp/Media/.Statuses"
        const val WHATSAPP_BUSINESS_STATUSES_PATH = "WhatsApp Business/Media/.Statuses"
        const val WHATSAPP_DUAL_STATUSES_PATH = "Android/data/com.clone.whatsapp/WhatsApp/Media/.Statuses"
        const val SAVED_FOLDER_NAME = "StatusHub"
        const val VAULT_FOLDER_NAME = "Vault"
        const val THUMBNAIL_FOLDER_NAME = "thumbnails"

        // Supported file extensions
        val IMAGE_EXTENSIONS = listOf("jpg", "jpeg", "png", "webp", "gif")
        val VIDEO_EXTENSIONS = listOf("mp4", "3gp", "mkv", "webm")
    }

    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    private val appFolder: File
        get() = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), SAVED_FOLDER_NAME)

    private val vaultFolder: File
        get() = File(context.filesDir, VAULT_FOLDER_NAME)

    private val thumbnailFolder: File
        get() = File(context.cacheDir, THUMBNAIL_FOLDER_NAME)

    init {
        // Create necessary folders
        appFolder.mkdirs()
        vaultFolder.mkdirs()
        thumbnailFolder.mkdirs()
        createNoMediaFile()
    }

    /**
     * Create .nomedia file to hide media from gallery
     */
    private fun createNoMediaFile() {
        val noMediaFile = File(appFolder, ".nomedia")
        if (!noMediaFile.exists()) {
            noMediaFile.createNewFile()
        }
    }

    /**
     * Get all WhatsApp status files from all sources
     */
    suspend fun getStatusFiles(): List<Status> = withContext(Dispatchers.IO) {
        val statuses = mutableListOf<Status>()
        
        // Primary WhatsApp
        val primaryPath = File(Environment.getExternalStorageDirectory(), WHATSAPP_STATUSES_PATH)
        if (primaryPath.exists()) {
            statuses.addAll(scanFolder(primaryPath, isFromBusiness = false, isFromDual = false))
        }
        
        // WhatsApp Business
        val businessPath = File(Environment.getExternalStorageDirectory(), WHATSAPP_BUSINESS_STATUSES_PATH)
        if (businessPath.exists()) {
            statuses.addAll(scanFolder(businessPath, isFromBusiness = true, isFromDual = false))
        }
        
        // Dual WhatsApp (clone apps)
        val dualPath = File(Environment.getExternalStorageDirectory(), WHATSAPP_DUAL_STATUSES_PATH)
        if (dualPath.exists()) {
            statuses.addAll(scanFolder(dualPath, isFromBusiness = false, isFromDual = true))
        }
        
        // Sort by timestamp (newest first)
        statuses.sortedByDescending { it.timestamp }
    }

    /**
     * Scan a folder for status files
     */
    private fun scanFolder(folder: File, isFromBusiness: Boolean, isFromDual: Boolean): List<Status> {
        return folder.listFiles()
            ?.filter { file ->
                val ext = file.extension.lowercase()
                IMAGE_EXTENSIONS.contains(ext) || VIDEO_EXTENSIONS.contains(ext)
            }
            ?.map { file ->
                Status.fromFile(file, isFromBusiness, isFromDual)
            }
            ?: emptyList()
    }

    /**
     * Save a status to the app folder or public gallery
     */
    suspend fun saveStatus(
        status: Status,
        saveLocation: SaveLocation
    ): Result<SavedStatus> = withContext(Dispatchers.IO) {
        try {
            val timestamp = System.currentTimeMillis()
            val dateStr = dateFormat.format(Date(timestamp))
            val extension = status.file.extension
            val fileName = "STATUS_${dateStr}.${extension}"

            val destinationFile = when (saveLocation) {
                SaveLocation.APP_PRIVATE -> File(appFolder, fileName)
                SaveLocation.PUBLIC_GALLERY -> saveToPublicGallery(status, fileName)
            }

            // Copy the file
            status.file.copyTo(destinationFile, overwrite = true)

            // Generate thumbnail for videos
            val thumbnailPath = if (status.isVideo) {
                generateVideoThumbnail(destinationFile)
            } else null

            SavedStatus(
                id = status.id,
                originalPath = status.path,
                savedPath = destinationFile.absolutePath,
                type = status.type,
                timestamp = timestamp,
                thumbnailPath = thumbnailPath
            ).let { Result.success(it) }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Save to public gallery using MediaStore
     */
    private fun saveToPublicGallery(status: Status, fileName: String): File {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, getMimeType(status.file))
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$SAVED_FOLDER_NAME")
        }

        val resolver = context.contentResolver
        val uri = if (status.isVideo) {
            resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
        } else {
            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        }

        uri?.let { 
            resolver.openOutputStream(it)?.use { outputStream ->
                FileInputStream(status.file).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }

        return File("${Environment.getExternalStorageDirectory()}/${Environment.DIRECTORY_PICTURES}/$SAVED_FOLDER_NAME/$fileName")
    }

    /**
     * Bulk save statuses
     */
    suspend fun saveStatuses(
        statuses: List<Status>,
        saveLocation: SaveLocation
    ): Result<List<SavedStatus>> = withContext(Dispatchers.IO) {
        val results = mutableListOf<SavedStatus>()
        val errors = mutableListOf<Exception>()

        statuses.forEach { status ->
            val result = saveStatus(status, saveLocation)
            result.fold(
                onSuccess = { results.add(it) },
                onFailure = { errors.add(it as Exception) }
            )
        }

        if (errors.isEmpty()) {
            Result.success(results)
        } else {
            Result.failure(Exception("Some files failed to save: ${errors.size} errors"))
        }
    }

    /**
     * Move a saved status to the vault
     */
    suspend fun moveToVault(savedStatus: SavedStatus): Result<SavedStatus> = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(savedStatus.savedPath)
            val destinationFile = File(vaultFolder, sourceFile.name)
            
            sourceFile.copyTo(destinationFile, overwrite = true)
            sourceFile.delete()
            
            Result.success(savedStatus.copy(savedPath = destinationFile.absolutePath, isHidden = true))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Move from vault back to regular saved folder
     */
    suspend fun moveFromVault(savedStatus: SavedStatus): Result<SavedStatus> = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(savedStatus.savedPath)
            val destinationFile = File(appFolder, sourceFile.name)
            
            sourceFile.copyTo(destinationFile, overwrite = true)
            sourceFile.delete()
            
            Result.success(savedStatus.copy(savedPath = destinationFile.absolutePath, isHidden = false))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete a saved status
     */
    suspend fun deleteStatus(savedStatus: SavedStatus): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = File(savedStatus.savedPath)
            if (file.exists()) {
                file.delete()
            }
            
            // Delete thumbnail if exists
            savedStatus.thumbnailPath?.let {
                File(it).delete()
            }
            
            // Remove from MediaStore if public
            if (savedStatus.savedPath.contains(Environment.DIRECTORY_PICTURES)) {
                removeFromMediaStore(savedStatus.savedPath)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Remove file from MediaStore
     */
    private fun removeFromMediaStore(path: String) {
        val resolver = context.contentResolver
        val uri = if (path.contains("video", ignoreCase = true)) {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        resolver.delete(uri, "${MediaStore.Images.Media.DATA} = ?", arrayOf(path))
    }

    /**
     * Generate thumbnail for video
     */
    private fun generateVideoThumbnail(videoFile: File): String {
        val thumbnailFile = File(thumbnailFolder, "${videoFile.nameWithoutExtension}.jpg")
        
        if (!thumbnailFile.exists()) {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ThumbnailUtils.createVideoThumbnail(videoFile, android.util.Size(480, 480), null)
            } else {
                @Suppress("DEPRECATION")
                ThumbnailUtils.createVideoThumbnail(videoFile.absolutePath, MediaStore.Images.Thumbnails.MINI_KIND)
            }
            
            bitmap?.let {
                FileOutputStream(thumbnailFile).use { fos ->
                    it.compress(Bitmap.CompressFormat.JPEG, 80, fos)
                }
                it.recycle()
            }
        }
        
        return thumbnailFile.absolutePath
    }

    /**
     * Get cached thumbnail or generate one
     */
    fun getThumbnail(status: Status): File? {
        return if (status.isVideo) {
            val thumbnailFile = File(thumbnailFolder, "${status.id}.jpg")
            if (thumbnailFile.exists()) {
                thumbnailFile
            } else {
                generateVideoThumbnailAsync(status.file, thumbnailFile)
                null
            }
        } else {
            null
        }
    }

    private fun generateVideoThumbnailAsync(videoFile: File, thumbnailFile: File) {
        Thread {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ThumbnailUtils.createVideoThumbnail(videoFile, android.util.Size(480, 480), null)
                } else {
                    @Suppress("DEPRECATION")
                    ThumbnailUtils.createVideoThumbnail(videoFile.absolutePath, MediaStore.Images.Thumbnails.MINI_KIND)
                }
                
                bitmap?.let {
                    FileOutputStream(thumbnailFile).use { fos ->
                        it.compress(Bitmap.CompressFormat.JPEG, 80, fos)
                    }
                    it.recycle()
                }
            } catch (e: Exception) {
                // Ignore thumbnail generation errors
            }
        }.start()
    }

    /**
     * Share a saved status
     */
    fun shareStatus(savedStatus: SavedStatus) {
        val file = File(savedStatus.savedPath)
        val uri = FileProvider.getUriForFile(
            context,
            "${BuildConfig.APPLICATION_ID}.fileprovider",
            file
        )

        val mimeType = getMimeType(file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooserIntent = Intent.createChooser(shareIntent, context.getString(R.string.preview_share))
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooserIntent)
    }

    /**
     * Share original status
     */
    fun shareStatus(status: Status) {
        val uri = FileProvider.getUriForFile(
            context,
            "${BuildConfig.APPLICATION_ID}.fileprovider",
            status.file
        )

        val mimeType = getMimeType(status.file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooserIntent = Intent.createChooser(shareIntent, context.getString(R.string.preview_share))
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooserIntent)
    }

    /**
     * Get MIME type for a file
     */
    private fun getMimeType(file: File): String {
        val extension = file.extension.lowercase()
        return when {
            IMAGE_EXTENSIONS.contains(extension) -> "image/$extension"
            VIDEO_EXTENSIONS.contains(extension) -> "video/$extension"
            else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"
        }
    }

    /**
     * Check if WhatsApp is installed
     */
    fun isWhatsAppInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo("com.whatsapp", 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if WhatsApp Business is installed
     */
    fun isWhatsAppBusinessInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo("com.whatsapp.w4b", 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Open WhatsApp
     */
    fun openWhatsApp(): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage("com.whatsapp")
            intent?.let {
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(it)
                true
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Clear cache
     */
    suspend fun clearCache(): Long = withContext(Dispatchers.IO) {
        var deletedSize = 0L
        
        thumbnailFolder.listFiles()?.forEach { file ->
            deletedSize += file.length()
            file.delete()
        }
        
        deletedSize
    }

    /**
     * Get cache size
     */
    fun getCacheSize(): Long {
        var size = 0L
        
        thumbnailFolder.listFiles()?.forEach { file ->
            size += file.length()
        }
        
        return size
    }

    /**
     * Format file size to human readable string
     */
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
            else -> String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024))
        }
    }
}
