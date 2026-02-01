package org.darchacheron.pantrypal.di

import org.darchacheron.pantrypal.database.DatabaseFactory
import org.koin.dsl.module

actual val platformModule =
    module {
        single { DatabaseFactory() }
    }