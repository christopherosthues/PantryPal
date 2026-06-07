package org.darthacheron.pantrypal.di

import org.darthacheron.pantrypal.authentication.createAuthenticationDataStore
import org.darthacheron.pantrypal.database.PantryPalDatabaseFactory
import org.darthacheron.pantrypal.settings.NativeSettingsRepository
import org.darthacheron.pantrypal.settings.SettingsRepository
import org.koin.dsl.module

actual val platformModule =
    module {
        single { PantryPalDatabaseFactory() }
        single<SettingsRepository> { NativeSettingsRepository() }
        single { createAuthenticationDataStore() }
    }