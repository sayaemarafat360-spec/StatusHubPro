package com.statushub.app.data.local.database

import androidx.room.*
import com.statushub.app.data.model.SavedStatus
import com.statushub.app.data.model.StatusType
import kotlinx.coroutines.flow.Flow

/**
 * Room database entity for saved statuses
 */
@Entity(tableName = "saved_statuses")
data class SavedStatusEntity(
    @PrimaryKey
    val id: String,
    val originalPath: String,
    val savedPath: String,
    val type: String,
    val timestamp: Long,
    val isFavorite: Boolean = false,
    val isHidden: Boolean = false,
    val thumbnailPath: String? = null
)

/**
 * DAO for saved statuses operations
 */
@Dao
interface SavedStatusDao {

    @Query("SELECT * FROM saved_statuses WHERE isHidden = 0 ORDER BY timestamp DESC")
    fun getAllSavedStatuses(): Flow<List<SavedStatusEntity>>

    @Query("SELECT * FROM saved_statuses WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteStatuses(): Flow<List<SavedStatusEntity>>

    @Query("SELECT * FROM saved_statuses WHERE isHidden = 1 ORDER BY timestamp DESC")
    fun getHiddenStatuses(): Flow<List<SavedStatusEntity>>

    @Query("SELECT * FROM saved_statuses WHERE id = :id")
    suspend fun getStatusById(id: String): SavedStatusEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatus(status: SavedStatusEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatuses(statuses: List<SavedStatusEntity>)

    @Update
    suspend fun updateStatus(status: SavedStatusEntity)

    @Delete
    suspend fun deleteStatus(status: SavedStatusEntity)

    @Query("DELETE FROM saved_statuses WHERE id = :id")
    suspend fun deleteStatusById(id: String)

    @Query("UPDATE saved_statuses SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: String, isFavorite: Boolean)

    @Query("UPDATE saved_statuses SET isHidden = :isHidden WHERE id = :id")
    suspend fun updateHidden(id: String, isHidden: Boolean)

    @Query("DELETE FROM saved_statuses")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM saved_statuses")
    suspend fun getCount(): Int
}

/**
 * Room database class
 */
@Database(
    entities = [SavedStatusEntity::class],
    version = 1,
    exportSchema = false
)
abstract class StatusHubDatabase : RoomDatabase() {
    abstract fun savedStatusDao(): SavedStatusDao
}

// Extension functions for mapping
fun SavedStatusEntity.toSavedStatus(): SavedStatus {
    return SavedStatus(
        id = id,
        originalPath = originalPath,
        savedPath = savedPath,
        type = when (type) {
            "IMAGE" -> StatusType.IMAGE
            "VIDEO" -> StatusType.VIDEO
            else -> StatusType.UNKNOWN
        },
        timestamp = timestamp,
        isFavorite = isFavorite,
        isHidden = isHidden,
        thumbnailPath = thumbnailPath
    )
}

fun SavedStatus.toEntity(): SavedStatusEntity {
    return SavedStatusEntity(
        id = id,
        originalPath = originalPath,
        savedPath = savedPath,
        type = when (type) {
            StatusType.IMAGE -> "IMAGE"
            StatusType.VIDEO -> "VIDEO"
            else -> "UNKNOWN"
        },
        timestamp = timestamp,
        isFavorite = isFavorite,
        isHidden = isHidden,
        thumbnailPath = thumbnailPath
    )
}
