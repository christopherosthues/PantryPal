package org.darchacheron.pantrypal.di

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import org.darchacheron.pantrypal.database.PantryPalDatabaseFactory
import org.darchacheron.pantrypal.database.PantryPalDatabase
import org.darchacheron.pantrypal.food.FoodDetailViewModel
import org.darchacheron.pantrypal.food.FoodListViewModel
import org.darchacheron.pantrypal.food.FoodRepository
import org.darchacheron.pantrypal.settings.SettingsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

expect val platformModule: Module

val sharedModule =
    module {
        factoryOf(::FoodRepository)

        single {
            get<PantryPalDatabaseFactory>()
                .create()
//                .addCallback(
//                    PrepopulateCallback(
//                        { get<ExerciseDao>() },
//                        { get<EquipmentDao>() },
//                        { get<WorkoutTemplateDao>() }
//                    )
//                )
            .setDriver(BundledSQLiteDriver())
            .build()
        }

        single { get<PantryPalDatabase>().foodDao }

        viewModelOf(::SettingsViewModel)
        viewModelOf(::FoodListViewModel)
        viewModelOf(::FoodDetailViewModel)
    }
