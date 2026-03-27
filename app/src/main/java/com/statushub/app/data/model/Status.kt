package com.statushub.app.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.File

/**
 * Represents a WhatsApp status (image or video)
 */
@Parcelize
data class Status(
    val id: String,
    val file: File,
    val type: StatusType,
    val timestamp: Long,
    val path: String,
    val isFromBusiness: Boolean = false,
    val isFromDual: Boolean = false,
    val thumbnailPath: String? = null
) : Parcelable {

    val isVideo: Boolean
        get() = type == StatusType.VIDEO

    val isImage: Boolean
        get() = type == StatusType.IMAGE

    val fileName: String
        get() = file.name

    val fileSize: Long
        get() = file.length()

    companion object {
        fun fromFile(file: File, isFromBusiness: Boolean = false, isFromDual: Boolean = false): Status {
            val type = when (file.extension.lowercase()) {
                "jpg", "jpeg", "png", "webp", "gif" -> StatusType.IMAGE
                "mp4", "3gp", "mkv", "webm" -> StatusType.VIDEO
                else -> StatusType.UNKNOWN
            }
            
            return Status(
                id = file.nameWithoutExtension,
                file = file,
                type = type,
                timestamp = file.lastModified(),
                path = file.absolutePath,
                isFromBusiness = isFromBusiness,
                isFromDual = isFromDual
            )
        }
    }
}

enum class StatusType {
    IMAGE,
    VIDEO,
    UNKNOWN
}

/**
 * Represents a saved status item
 */
@Parcelize
data class SavedStatus(
    val id: String,
    val originalPath: String,
    val savedPath: String,
    val type: StatusType,
    val timestamp: Long,
    val isFavorite: Boolean = false,
    val isHidden: Boolean = false,
    val thumbnailPath: String? = null
) : Parcelable {

    val isVideo: Boolean
        get() = type == StatusType.VIDEO

    val isImage: Boolean
        get() = type == StatusType.IMAGE

    val file: File
        get() = File(savedPath)
}

/**
 * UI State for status lists
 */
data class StatusUiState(
    val isLoading: Boolean = false,
    val statuses: List<Status> = emptyList(),
    val savedStatuses: List<SavedStatus> = emptyList(),
    val favoriteStatuses: List<SavedStatus> = emptyList(),
    val hiddenStatuses: List<SavedStatus> = emptyList(),
    val error: String? = null,
    val hasPermission: Boolean = false,
    val selectedItems: Set<String> = emptySet(),
    val isNewStatusAvailable: Boolean = false,
    val lastViewedTimestamp: Long = 0L
)

/**
 * Theme mode for the app
 */
enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM;

    fun isDarkMode(): Boolean = this == DARK
}

/**
 * Save location preference
 */
enum class SaveLocation {
    APP_PRIVATE,
    PUBLIC_GALLERY
}
