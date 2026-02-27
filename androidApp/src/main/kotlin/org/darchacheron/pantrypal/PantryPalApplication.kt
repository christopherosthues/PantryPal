package org.darchacheron.pantrypal

import android.app.Application
import org.darchacheron.pantrypal.di.initKoin
import org.koin.android.ext.koin.androidContext

class PantryPalApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@PantryPalApplication)
        }
    }
}