@file:OptIn(ExperimentalMaterial3AdaptiveApi::class)

package org.darchacheron.pantrypal.di

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import org.darchacheron.pantrypal.database.PantryPalDatabase
import org.darchacheron.pantrypal.database.PantryPalDatabaseFactory
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
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation
import pantrypal.composeapp.generated.resources.Res
import pantrypal.composeapp.generated.resources.food_list_empty_selection

expect val platformModule: Module

val navigationModule = module {
    single { Navigator() }

    navigation<NavRoute.FoodList>(
        metadata = ListDetailSceneStrategy.listPane(
            detailPlaceholder = {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(Res.string.food_list_empty_selection))
                }
            }
        )
    ) {
        FoodListView(
            foodListViewModel = koinViewModel(),
        )
    }

    navigation<NavRoute.FoodDetail>(
        metadata = ListDetailSceneStrategy.detailPane()
    ) { route ->
        FoodDetailView(
            viewModel = koinViewModel { parametersOf(route) },
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
