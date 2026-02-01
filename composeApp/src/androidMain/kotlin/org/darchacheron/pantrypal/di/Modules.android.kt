package org.darchacheron.pantrypal.di

import org.darchacheron.pantrypal.database.DatabaseFactory
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

actual val platformModule = module {
    single { DatabaseFactory(androidApplication()) }
}