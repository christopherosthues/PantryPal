package org.darchacheron.pantrypal.di

import org.darchacheron.pantrypal.database.PantryPalDatabase
import org.darchacheron.pantrypal.food.FoodDao
import org.darchacheron.pantrypal.food.FoodDetailViewModel
import org.darchacheron.pantrypal.food.FoodListViewModel
import org.darchacheron.pantrypal.food.FoodRepository
import org.darchacheron.pantrypal.settings.SettingsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

expect val platformModule: Module

val sharedModule =
    module {
        factoryOf(::FoodRepository)

        single { get<PantryPalDatabase>().foodDao }

        viewModelOf(::SettingsViewModel)
        viewModelOf(::FoodListViewModel)
        viewModelOf(::FoodDetailViewModel)
    }
