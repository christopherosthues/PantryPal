package org.darchacheron.pantrypal.di

import org.darchacheron.pantrypal.database.PantryPalDatabaseFactory
import org.darchacheron.pantrypal.settings.JvmSettingsRepository
import org.darchacheron.pantrypal.settings.SettingsRepository
import org.koin.dsl.module

actual val platformModule =
    module {
        single { PantryPalDatabaseFactory() }
        single<SettingsRepository> { JvmSettingsRepository() }
    }