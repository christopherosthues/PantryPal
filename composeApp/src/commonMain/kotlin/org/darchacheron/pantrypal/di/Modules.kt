package org.darchacheron.pantrypal.di

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import org.darchacheron.pantrypal.database.PantryPalDatabase
import org.darchacheron.pantrypal.database.PantryPalDatabaseFactory
import org.darchacheron.pantrypal.food.CameraView
import org.darchacheron.pantrypal.food.FoodDetailView
import org.darchacheron.pantrypal.food.FoodDetailViewModel
import org.darchacheron.pantrypal.food.FoodListView
import org.darchacheron.pantrypal.food.FoodListViewModel
import org.darchacheron.pantrypal.food.FoodRepository
import org.darchacheron.pantrypal.food.SimpleCameraView
import org.darchacheron.pantrypal.navigation.NavRoute
import org.darchacheron.pantrypal.navigation.Navigator
import org.darchacheron.pantrypal.settings.SettingsView
import org.darchacheron.pantrypal.settings.SettingsViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

expect val platformModule: Module

val navigationModule = module {
    single { Navigator() }

    navigation<NavRoute.FoodList> {
        val navigator = get<Navigator>()
        FoodListView(
            foodListViewModel = koinViewModel(),
        )
    }

    navigation<NavRoute.FoodDetail> { route ->
        FoodDetailView(
            viewModel = koinViewModel { parametersOf(route) },
        )
    }

    navigation<NavRoute.Camera> { route ->
        val navigator = get<Navigator>()
        CameraView(
            foodId = route.foodId,
            onBack = {
                navigator.goBack()
            }
        )
    }

    navigation<NavRoute.SimpleCamera> { route ->
        val navigator = get<Navigator>()
        SimpleCameraView(
            onCapture = {
                route.onCapture(it)
            },
            onBack = {
                navigator.goBack()
            }
        )
    }

    navigation<NavRoute.Settings> {
        val navigator = get<Navigator>()
        SettingsView(
            onBack = {
                navigator.goBack()
            }
        )
    }
}

val sharedModule =
    module {
        includes(navigationModule)
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
