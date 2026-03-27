package com.statushub.app.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.statushub.app.data.model.SaveLocation
import com.statushub.app.data.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private object PreferencesKeys {
        val THEME_MODE = intPreferencesKey("theme_mode")
        val SAVE_LOCATION = intPreferencesKey("save_location")
        val IS_PREMIUM = booleanPreferencesKey("is_premium")
        val HAS_SHOWN_ONBOARDING = booleanPreferencesKey("has_shown_onboarding")
        val LAST_VIEWED_TIMESTAMP = longPreferencesKey("last_viewed_timestamp")
        val APP_OPEN_AD_COUNT = intPreferencesKey("app_open_ad_count")
        val LAST_APP_OPEN_AD_DATE = stringPreferencesKey("last_app_open_ad_date")
        val SAVES_SINCE_LAST_AD = intPreferencesKey("saves_since_last_ad")
        val VAULT_PIN = stringPreferencesKey("vault_pin")
        val USE_BIOMETRIC = booleanPreferencesKey("use_biometric")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            when (preferences[PreferencesKeys.THEME_MODE] ?: 2) {
                0 -> ThemeMode.LIGHT
                1 -> ThemeMode.DARK
                else -> ThemeMode.SYSTEM
            }
        }

    val saveLocation: Flow<SaveLocation> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            when (preferences[PreferencesKeys.SAVE_LOCATION] ?: 0) {
                0 -> SaveLocation.APP_PRIVATE
                else -> SaveLocation.PUBLIC_GALLERY
            }
        }

    val isPremium: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.IS_PREMIUM] ?: false
        }

    val hasShownOnboarding: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.HAS_SHOWN_ONBOARDING] ?: false
        }

    val lastViewedTimestamp: Flow<Long> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.LAST_VIEWED_TIMESTAMP] ?: 0L
        }

    val vaultPin: Flow<String?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.VAULT_PIN]
        }

    val useBiometric: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.USE_BIOMETRIC] ?: false
        }

    val notificationsEnabled: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = when (mode) {
                ThemeMode.LIGHT -> 0
                ThemeMode.DARK -> 1
                ThemeMode.SYSTEM -> 2
            }
        }
    }

    suspend fun setSaveLocation(location: SaveLocation) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SAVE_LOCATION] = when (location) {
                SaveLocation.APP_PRIVATE -> 0
                SaveLocation.PUBLIC_GALLERY -> 1
            }
        }
    }

    suspend fun setPremium(isPremium: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_PREMIUM] = isPremium
        }
    }

    suspend fun setOnboardingShown(shown: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAS_SHOWN_ONBOARDING] = shown
        }
    }

    suspend fun setLastViewedTimestamp(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_VIEWED_TIMESTAMP] = timestamp
        }
    }

    suspend fun setVaultPin(pin: String?) {
        context.dataStore.edit { preferences ->
            if (pin != null) {
                preferences[PreferencesKeys.VAULT_PIN] = pin
            } else {
                preferences.remove(PreferencesKeys.VAULT_PIN)
            }
        }
    }

    suspend fun setUseBiometric(use: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USE_BIOMETRIC] = use
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    // Ad tracking methods
    suspend fun shouldShowAppOpenAd(): Boolean {
        val today = java.time.LocalDate.now().toString()
        var shouldShow = false
        
        context.dataStore.edit { preferences ->
            val lastAdDate = preferences[PreferencesKeys.LAST_APP_OPEN_AD_DATE] ?: ""
            val count = preferences[PreferencesKeys.APP_OPEN_AD_COUNT] ?: 0
            
            if (lastAdDate != today) {
                // New day, reset counter
                preferences[PreferencesKeys.LAST_APP_OPEN_AD_DATE] = today
                preferences[PreferencesKeys.APP_OPEN_AD_COUNT] = 1
                shouldShow = true
            } else if (count < 2) {
                // Same day, but under limit
                preferences[PreferencesKeys.APP_OPEN_AD_COUNT] = count + 1
                shouldShow = true
            }
        }
        
        return shouldShow
    }

    suspend fun shouldShowRewardedAd(): Boolean {
        var shouldShow = false
        
        context.dataStore.edit { preferences ->
            val savesSinceLastAd = preferences[PreferencesKeys.SAVES_SINCE_LAST_AD] ?: 0
            
            if (savesSinceLastAd >= 3) {
                preferences[PreferencesKeys.SAVES_SINCE_LAST_AD] = 0
                shouldShow = true
            } else {
                preferences[PreferencesKeys.SAVES_SINCE_LAST_AD] = savesSinceLastAd + 1
            }
        }
        
        return shouldShow
    }

    suspend fun incrementSaveCount() {
        context.dataStore.edit { preferences ->
            val count = preferences[PreferencesKeys.SAVES_SINCE_LAST_AD] ?: 0
            preferences[PreferencesKeys.SAVES_SINCE_LAST_AD] = count + 1
        }
    }
}
