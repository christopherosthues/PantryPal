package org.darchacheron.pantrypal.di

import org.darchacheron.pantrypal.database.DatabaseFactory
import org.darchacheron.pantrypal.settings.NativeSettingsRepository
import org.darchacheron.pantrypal.settings.SettingsRepository
import org.koin.dsl.module

actual val platformModule =
    module {
        single { DatabaseFactory() }
        single<SettingsRepository> { NativeSettingsRepository() }
    }