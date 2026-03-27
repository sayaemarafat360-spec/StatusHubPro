package com.statushub.app.data.repository

import com.statushub.app.data.filemanager.FileManager
import com.statushub.app.data.local.database.SavedStatusDao
import com.statushub.app.data.local.database.toEntity
import com.statushub.app.data.local.database.toSavedStatus
import com.statushub.app.data.local.preferences.PreferencesManager
import com.statushub.app.data.model.SaveLocation
import com.statushub.app.data.model.SavedStatus
import com.statushub.app.data.model.Status
import com.statushub.app.data.model.StatusUiState
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatusRepository @Inject constructor(
    private val fileManager: FileManager,
    private val savedStatusDao: SavedStatusDao,
    private val preferencesManager: PreferencesManager
) {

    /**
     * Get WhatsApp statuses
     */
    suspend fun getStatuses(): Result<List<Status>> {
        return try {
            val statuses = fileManager.getStatusFiles()
            Result.success(statuses)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get saved statuses flow
     */
    fun getSavedStatuses(): Flow<List<SavedStatus>> {
        return savedStatusDao.getAllSavedStatuses()
            .map { entities -> entities.map { it.toSavedStatus() } }
    }

    /**
     * Get favorite statuses flow
     */
    fun getFavoriteStatuses(): Flow<List<SavedStatus>> {
        return savedStatusDao.getFavoriteStatuses()
            .map { entities -> entities.map { it.toSavedStatus() } }
    }

    /**
     * Get hidden statuses flow
     */
    fun getHiddenStatuses(): Flow<List<SavedStatus>> {
        return savedStatusDao.getHiddenStatuses()
            .map { entities -> entities.map { it.toSavedStatus() } }
    }

    /**
     * Save a single status
     */
    suspend fun saveStatus(status: Status): Result<SavedStatus> {
        val saveLocation = preferencesManager.saveLocation.first()
        return fileManager.saveStatus(status, saveLocation).also { result ->
            result.onSuccess { savedStatus ->
                savedStatusDao.insertStatus(savedStatus.toEntity())
                preferencesManager.incrementSaveCount()
            }
        }
    }

    /**
     * Save multiple statuses (bulk save)
     */
    suspend fun saveStatuses(statuses: List<Status>): Result<List<SavedStatus>> {
        val saveLocation = preferencesManager.saveLocation.first()
        return fileManager.saveStatuses(statuses, saveLocation).also { result ->
            result.onSuccess { savedStatuses ->
                savedStatusDao.insertStatuses(savedStatuses.map { it.toEntity() })
            }
        }
    }

    /**
     * Toggle favorite status
     */
    suspend fun toggleFavorite(savedStatus: SavedStatus) {
        savedStatusDao.updateFavorite(savedStatus.id, !savedStatus.isFavorite)
    }

    /**
     * Move to vault
     */
    suspend fun moveToVault(savedStatus: SavedStatus): Result<SavedStatus> {
        return fileManager.moveToVault(savedStatus).also { result ->
            result.onSuccess { updated ->
                savedStatusDao.updateStatus(updated.toEntity())
            }
        }
    }

    /**
     * Move from vault
     */
    suspend fun moveFromVault(savedStatus: SavedStatus): Result<SavedStatus> {
        return fileManager.moveFromVault(savedStatus).also { result ->
            result.onSuccess { updated ->
                savedStatusDao.updateStatus(updated.toEntity())
            }
        }
    }

    /**
     * Delete a saved status
     */
    suspend fun deleteStatus(savedStatus: SavedStatus): Result<Unit> {
        return fileManager.deleteStatus(savedStatus).also { result ->
            result.onSuccess {
                savedStatusDao.deleteStatusById(savedStatus.id)
            }
        }
    }

    /**
     * Share a status
     */
    fun shareStatus(status: Status) {
        fileManager.shareStatus(status)
    }

    /**
     * Share a saved status
     */
    fun shareSavedStatus(savedStatus: SavedStatus) {
        fileManager.shareStatus(savedStatus)
    }

    /**
     * Open WhatsApp
     */
    fun openWhatsApp(): Boolean {
        return fileManager.openWhatsApp()
    }

    /**
     * Check if WhatsApp is installed
     */
    fun isWhatsAppInstalled(): Boolean {
        return fileManager.isWhatsAppInstalled()
    }

    /**
     * Get thumbnail for status
     */
    fun getThumbnail(status: Status) = fileManager.getThumbnail(status)

    /**
     * Clear cache
     */
    suspend fun clearCache(): Long {
        return fileManager.clearCache()
    }

    /**
     * Get cache size
     */
    fun getCacheSize(): Long {
        return fileManager.getCacheSize()
    }

    /**
     * Format file size
     */
    fun formatFileSize(bytes: Long) = fileManager.formatFileSize(bytes)

    // Preferences delegation
    val themeMode = preferencesManager.themeMode
    val saveLocation = preferencesManager.saveLocation
    val isPremium = preferencesManager.isPremium
    val hasShownOnboarding = preferencesManager.hasShownOnboarding
    val lastViewedTimestamp = preferencesManager.lastViewedTimestamp
    val vaultPin = preferencesManager.vaultPin
    val useBiometric = preferencesManager.useBiometric

    suspend fun setThemeMode(mode: com.statushub.app.data.model.ThemeMode) {
        preferencesManager.setThemeMode(mode)
    }

    suspend fun setSaveLocation(location: SaveLocation) {
        preferencesManager.setSaveLocation(location)
    }

    suspend fun setPremium(isPremium: Boolean) {
        preferencesManager.setPremium(isPremium)
    }

    suspend fun setOnboardingShown(shown: Boolean) {
        preferencesManager.setOnboardingShown(shown)
    }

    suspend fun setLastViewedTimestamp(timestamp: Long) {
        preferencesManager.setLastViewedTimestamp(timestamp)
    }

    suspend fun setVaultPin(pin: String?) {
        preferencesManager.setVaultPin(pin)
    }

    suspend fun setUseBiometric(use: Boolean) {
        preferencesManager.setUseBiometric(use)
    }

    suspend fun shouldShowAppOpenAd() = preferencesManager.shouldShowAppOpenAd()
    suspend fun shouldShowRewardedAd() = preferencesManager.shouldShowRewardedAd()
}
