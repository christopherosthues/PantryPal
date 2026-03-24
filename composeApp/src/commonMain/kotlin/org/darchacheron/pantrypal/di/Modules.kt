@file:OptIn(ExperimentalMaterial3AdaptiveApi::class)

package org.darchacheron.pantrypal.di

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation3.ui.NavDisplay
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import okio.FileSystem
import okio.SYSTEM
import org.darchacheron.pantrypal.MainView
import org.darchacheron.pantrypal.authentication.AuthenticationPreferencesRepository
import org.darchacheron.pantrypal.authentication.AuthenticationService
import org.darchacheron.pantrypal.authentication.LoginView
import org.darchacheron.pantrypal.authentication.LoginViewModel
import org.darchacheron.pantrypal.authentication.RegistrationView
import org.darchacheron.pantrypal.authentication.RegistrationViewModel
import org.darchacheron.pantrypal.database.PantryPalDatabase
import org.darchacheron.pantrypal.database.PantryPalDatabaseFactory
import org.darchacheron.pantrypal.food.*
import org.darchacheron.pantrypal.camera.OcrCameraView
import org.darchacheron.pantrypal.camera.OcrCameraViewModel
import org.darchacheron.pantrypal.camera.SimpleCameraView
import org.darchacheron.pantrypal.camera.SimpleCameraViewModel
import org.darchacheron.pantrypal.inventory.*
import org.darchacheron.pantrypal.navigation.*
import org.darchacheron.pantrypal.profile.ProfileRepository
import org.darchacheron.pantrypal.profile.ProfileView
import org.darchacheron.pantrypal.profile.ProfileViewModel
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

    navigation<NavRoute.Main> {
        MainView()
    }

    navigation<BottomNavRoute.FoodList>(
        metadata = ListDetailSceneStrategy.listPane(
            detailPlaceholder = {
                Surface {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(Res.string.food_list_empty_selection),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        )
    ) {
        FoodListView(
            foodListViewModel = koinViewModel(),
        )
    }

    navigation<BottomNavRoute.InventoryList> {
        InventoryListView()
    }

    navigation<InventoryNavRoute.InventoryDetail> { route ->
        InventoryDetailView(
            viewModel = koinViewModel { parametersOf(route.itemId) }
        )
    }

    navigation<BottomNavRoute.Profile> {
        val navigator = get<Navigator>()
        ProfileView(
            viewModel = koinViewModel(),
            onGoToSettings = { navigator.goToSettings() }
        )
    }

    navigation<FoodNavRoute.FoodDetail>(
        metadata = ListDetailSceneStrategy.detailPane()
    ) { route ->
        FoodDetailView(
            viewModel = koinViewModel { parametersOf(route) },
        )
    }

    navigation<FoodNavRoute.SimpleCamera>(
        metadata = NavDisplay.transitionSpec {
            slideInVertically(initialOffsetY = { it }) togetherWith ExitTransition.KeepUntilTransitionsFinished
        } + NavDisplay.popTransitionSpec {
            EnterTransition.None togetherWith slideOutVertically(targetOffsetY = { it })
        }
    ) {
        val navigator = get<Navigator>()
        SimpleCameraView(
            onCapture = {
                navigator.onSimpleCameraResult(it)
            },
            onBack = {
                navigator.goBack()
            }
        )
    }

    navigation<FoodNavRoute.OcrCamera>(
        metadata = NavDisplay.transitionSpec {
            slideInVertically(initialOffsetY = { it }) togetherWith ExitTransition.KeepUntilTransitionsFinished
        } + NavDisplay.popTransitionSpec {
            EnterTransition.None togetherWith slideOutVertically(targetOffsetY = { it })
        }
    ) { route ->
        val navigator = get<Navigator>()
        OcrCameraView(
            viewModel = koinViewModel<OcrCameraViewModel> { parametersOf(route) },
            onRecognized = {
                navigator.onOcrCameraResult(it)
            }
        )
    }

    navigation<NavRoute.Settings>(
        metadata = NavDisplay.transitionSpec {
            slideInVertically(initialOffsetY = { it }) togetherWith ExitTransition.KeepUntilTransitionsFinished
        } + NavDisplay.popTransitionSpec {
            EnterTransition.None togetherWith slideOutVertically(targetOffsetY = { it })
        }
    ) {
        val navigator = get<Navigator>()
        SettingsView(
            onBack = {
                navigator.goBack()
            }
        )
    }

    navigation<NavRoute.Login>(
        metadata = NavDisplay.transitionSpec {
            slideInVertically(initialOffsetY = { it }) togetherWith ExitTransition.KeepUntilTransitionsFinished
        } + NavDisplay.popTransitionSpec {
            EnterTransition.None togetherWith slideOutVertically(targetOffsetY = { it })
        }
    ) {
        LoginView(loginViewModel = koinViewModel())
    }

    navigation<NavRoute.Register>(
        metadata = NavDisplay.transitionSpec {
            slideInVertically(initialOffsetY = { it }) togetherWith ExitTransition.KeepUntilTransitionsFinished
        } + NavDisplay.popTransitionSpec {
            EnterTransition.None togetherWith slideOutVertically(targetOffsetY = { it })
        }
    ) {
        RegistrationView(registrationViewModel = koinViewModel())
    }
}

val sharedModule =
    module {
        includes(navigationModule)
        single { FileSystem.SYSTEM }
        factoryOf(::FoodNetworkService)
        factoryOf(::FoodRepository)
        factoryOf(::InventoryNetworkService)
        factoryOf(::InventoryRepository)
        factoryOf(::ProfileRepository)
        factoryOf(::AuthenticationPreferencesRepository)

        single {
            get<PantryPalDatabaseFactory>()
                .create()
                .addMigrations(
                    PantryPalDatabase.MIGRATION_1_2,
                    PantryPalDatabase.MIGRATION_2_3,
                    PantryPalDatabase.MIGRATION_3_4,
                    PantryPalDatabase.MIGRATION_4_5,
                )
                .setDriver(BundledSQLiteDriver())
                .build()
        }

        single { get<PantryPalDatabase>().foodDao }
        single { get<PantryPalDatabase>().profileDao }
        single { get<PantryPalDatabase>().inventoryItemDao }

        viewModelOf(::SettingsViewModel)
        viewModelOf(::FoodListViewModel)
        viewModelOf(::FoodDetailViewModel)
        viewModelOf(::OcrCameraViewModel)
        viewModelOf(::SimpleCameraViewModel)
        viewModelOf(::LoginViewModel)
        viewModelOf(::RegistrationViewModel)
        viewModelOf(::ProfileViewModel)
        viewModelOf(::InventoryListViewModel)
        viewModelOf(::InventoryDetailViewModel)

        factoryOf(::AuthenticationService)
    }
