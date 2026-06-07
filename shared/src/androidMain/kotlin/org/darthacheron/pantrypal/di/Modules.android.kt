package org.darthacheron.pantrypal.di

import org.darthacheron.pantrypal.authentication.createAuthenticationDataStore
import org.darthacheron.pantrypal.database.PantryPalDatabaseFactory
import org.darthacheron.pantrypal.settings.AndroidSettingsRepository
import org.darthacheron.pantrypal.settings.SettingsRepository
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

actual val platformModule = module {
    single { PantryPalDatabaseFactory(androidApplication()) }
    single<SettingsRepository> { AndroidSettingsRepository(androidApplication()) }
    single { createAuthenticationDataStore(androidApplication())  }
}