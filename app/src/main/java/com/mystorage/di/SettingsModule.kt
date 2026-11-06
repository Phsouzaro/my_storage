package com.mystorage.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.mystorage.data.settings.DataStoreSettingsRepository
import com.mystorage.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Arquivo: files/datastore/settings.preferences_pb (incluído no Auto Backup). */
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Module
@InstallIn(SingletonComponent::class)
object SettingsProvidesModule {
    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> = context.settingsDataStore
}

@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsBindsModule {
    @Binds abstract fun bindSettingsRepository(impl: DataStoreSettingsRepository): SettingsRepository
}
