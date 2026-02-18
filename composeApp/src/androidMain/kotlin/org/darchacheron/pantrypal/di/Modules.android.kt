package org.darchacheron.pantrypal.di

import org.darchacheron.pantrypal.database.PantryPalDatabaseFactory
import org.darchacheron.pantrypal.settings.AndroidSettingsRepository
import org.darchacheron.pantrypal.settings.SettingsRepository
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

actual val platformModule = module {
    single { PantryPalDatabaseFactory(androidApplication()) }
    single<SettingsRepository> { AndroidSettingsRepository(androidApplication()) }
}