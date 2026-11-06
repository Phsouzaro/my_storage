package com.mystorage.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.mystorage.domain.model.AppSettings
import com.mystorage.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class DataStoreSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    private object Keys {
        val WARNING_DAYS = intPreferencesKey("warning_days")
        val NOTIFY_HOUR = intPreferencesKey("notify_hour")
        val LAST_EXPIRY_CHECK = longPreferencesKey("last_expiry_check_epoch_day")
    }

    override fun observe(): Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            warningDays = prefs[Keys.WARNING_DAYS] ?: AppSettings().warningDays,
            notifyHour = prefs[Keys.NOTIFY_HOUR] ?: AppSettings.DEFAULT_NOTIFY_HOUR,
        )
    }

    override suspend fun get(): AppSettings = observe().first()

    override suspend fun setWarningDays(days: Int) {
        require(days >= 7) { "Prazo mínimo de 7 dias" }
        dataStore.edit { it[Keys.WARNING_DAYS] = days }
    }

    override suspend fun setNotifyHour(hour: Int) {
        require(hour in 0..23)
        dataStore.edit { it[Keys.NOTIFY_HOUR] = hour }
    }

    override suspend fun getLastExpiryCheck(): LocalDate? =
        dataStore.data.first()[Keys.LAST_EXPIRY_CHECK]?.let(LocalDate::ofEpochDay)

    override suspend fun setLastExpiryCheck(date: LocalDate) {
        dataStore.edit { it[Keys.LAST_EXPIRY_CHECK] = date.toEpochDay() }
    }
}
