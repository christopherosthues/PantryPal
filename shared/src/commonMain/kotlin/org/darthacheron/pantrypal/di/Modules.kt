@file:OptIn(ExperimentalMaterial3AdaptiveApi::class)

package org.darthacheron.pantrypal.di

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
import io.ktor.client.engine.cio.CIO
import org.darthacheron.pantrypal.MainView
import org.darthacheron.pantrypal.MainViewModel
import org.darthacheron.pantrypal.authentication.AuthenticationPreferencesRepository
import org.darthacheron.pantrypal.authentication.AuthenticationPreferencesRepositoryImpl
import org.darthacheron.pantrypal.authentication.AuthenticationService
import org.darthacheron.pantrypal.authentication.AuthenticationServiceImpl
import org.darthacheron.pantrypal.authentication.LoginView
import org.darthacheron.pantrypal.authentication.LoginViewModel
import org.darthacheron.pantrypal.authentication.RegistrationView
import org.darthacheron.pantrypal.authentication.RegistrationViewModel
import org.darthacheron.pantrypal.authentication.RemoteAccountLinkViewModel
import org.darthacheron.pantrypal.authentication.RemoteLoginViewModel
import org.darthacheron.pantrypal.camera.OcrCameraView
import org.darthacheron.pantrypal.camera.OcrCameraViewModel
import org.darthacheron.pantrypal.camera.SimpleCameraView
import org.darthacheron.pantrypal.camera.SimpleCameraViewModel
import org.darthacheron.pantrypal.database.PantryPalDatabase
import org.darthacheron.pantrypal.database.PantryPalDatabaseFactory
import org.darthacheron.pantrypal.food.FoodDetailView
import org.darthacheron.pantrypal.food.FoodDetailViewModel
import org.darthacheron.pantrypal.food.FoodListView
import org.darthacheron.pantrypal.food.FoodListViewModel
import org.darthacheron.pantrypal.food.FoodNetworkService
import org.darthacheron.pantrypal.food.FoodNetworkServiceImpl
import org.darthacheron.pantrypal.food.FoodRepository
import org.darthacheron.pantrypal.food.FoodRepositoryImpl
import org.darthacheron.pantrypal.inventory.InventoryDetailView
import org.darthacheron.pantrypal.inventory.InventoryDetailViewModel
import org.darthacheron.pantrypal.inventory.InventoryListView
import org.darthacheron.pantrypal.inventory.InventoryListViewModel
import org.darthacheron.pantrypal.inventory.InventoryNetworkService
import org.darthacheron.pantrypal.inventory.InventoryNetworkServiceImpl
import org.darthacheron.pantrypal.inventory.InventoryRepository
import org.darthacheron.pantrypal.inventory.InventoryRepositoryImpl
import org.darthacheron.pantrypal.navigation.BottomNavRoute
import org.darthacheron.pantrypal.navigation.FoodNavRoute
import org.darthacheron.pantrypal.navigation.InventoryNavRoute
import org.darthacheron.pantrypal.navigation.NavRoute
import org.darthacheron.pantrypal.navigation.Navigator
import org.darthacheron.pantrypal.navigation.NavigatorImpl
import org.darthacheron.pantrypal.networking.ConnectionNetworkService
import org.darthacheron.pantrypal.networking.ConnectionNetworkServiceImpl
import org.darthacheron.pantrypal.networking.ImageNetworkService
import org.darthacheron.pantrypal.networking.ImageNetworkServiceImpl
import org.darthacheron.pantrypal.profile.AdminNetworkService
import org.darthacheron.pantrypal.profile.AdminNetworkServiceImpl
import org.darthacheron.pantrypal.profile.AdminView
import org.darthacheron.pantrypal.profile.AdminViewModel
import org.darthacheron.pantrypal.profile.EditRemoteProfileViewModel
import org.darthacheron.pantrypal.profile.ProfileNetworkService
import org.darthacheron.pantrypal.profile.ProfileNetworkServiceImpl
import org.darthacheron.pantrypal.profile.ProfileRepository
import org.darthacheron.pantrypal.profile.ProfileRepositoryImpl
import org.darthacheron.pantrypal.profile.ProfileView
import org.darthacheron.pantrypal.profile.ProfileViewModel
import org.darthacheron.pantrypal.settings.SettingsView
import org.darthacheron.pantrypal.settings.SettingsViewModel
import org.darthacheron.pantrypal.utils.HttpClientFactory
import org.darthacheron.pantrypal.utils.HttpClientFactoryImpl
import org.darthacheron.pantrypal.utils.createHttpClient
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation
import pantrypal.shared.generated.resources.Res
import pantrypal.shared.generated.resources.food_list_empty_selection
import pantrypal.shared.generated.resources.inventory_list_empty_selection

expect val platformModule: Module

val navigationModule = module {
    single<Navigator> { NavigatorImpl() }

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

    navigation<BottomNavRoute.InventoryList>(
        metadata = ListDetailSceneStrategy.listPane(
            detailPlaceholder = {
                Surface {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(Res.string.inventory_list_empty_selection),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        )
    ) {
        InventoryListView()
    }

    navigation<InventoryNavRoute.InventoryDetail>(
        metadata = ListDetailSceneStrategy.detailPane()
    ) { route ->
        InventoryDetailView(
            viewModel = koinViewModel { parametersOf(route) }
        )
    }

    navigation<BottomNavRoute.Profile> {
        val navigator = get<Navigator>()
        ProfileView(
            profileViewModel = koinViewModel(),
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

    navigation<NavRoute.Admin>(
        metadata = NavDisplay.transitionSpec {
            slideInVertically(initialOffsetY = { it }) togetherWith ExitTransition.KeepUntilTransitionsFinished
        } + NavDisplay.popTransitionSpec {
            EnterTransition.None togetherWith slideOutVertically(targetOffsetY = { it })
        }
    ) {
        val navigator = get<Navigator>()
        AdminView(onBack = { navigator.goBack() })
    }
}

val sharedModule =
    module {
        includes(navigationModule)
        single { FileSystem.SYSTEM }
        single { CIO.create() }
        single<HttpClientFactory> { HttpClientFactoryImpl(get()) }
        factory<AuthenticationService> { AuthenticationServiceImpl(get(), get()) }
        factory<FoodNetworkService> { FoodNetworkServiceImpl(get(), get()) }
        factory<InventoryNetworkService> { InventoryNetworkServiceImpl(get(), get()) }
        factory<ProfileNetworkService> { ProfileNetworkServiceImpl(get(), get()) }
        factory<AdminNetworkService> { AdminNetworkServiceImpl(get(), get()) }
        factory<ConnectionNetworkService> { ConnectionNetworkServiceImpl(get()) }
        factory<ImageNetworkService> { ImageNetworkServiceImpl(get(), get()) }
        factory<FoodRepository> { FoodRepositoryImpl(get(), get(), get(), get(), get(), get(), get(), get()) }
        factory<InventoryRepository> { InventoryRepositoryImpl(get(), get(), get(), get(), get(), get(), get(), get()) }
        factory<ProfileRepository> { ProfileRepositoryImpl(get(), get(), get(), get()) }
        factory<AuthenticationPreferencesRepository> { AuthenticationPreferencesRepositoryImpl(get()) }

        single {
            get<PantryPalDatabaseFactory>()
                .create()
                .addMigrations(
                )
                .setDriver(BundledSQLiteDriver())
                .build()
        }

        single { get<PantryPalDatabase>().foodDao }
        single { get<PantryPalDatabase>().remoteFoodDao }
        single { get<PantryPalDatabase>().profileDao }
        single { get<PantryPalDatabase>().remoteProfileDao }
        single { get<PantryPalDatabase>().inventoryItemDao }
        single { get<PantryPalDatabase>().remoteInventoryItemDao }
        single { get<PantryPalDatabase>().remoteImageDao }

        viewModelOf(::SettingsViewModel)
        viewModelOf(::FoodListViewModel)
        viewModelOf(::FoodDetailViewModel)
        viewModelOf(::OcrCameraViewModel)
        viewModelOf(::SimpleCameraViewModel)
        viewModelOf(::LoginViewModel)
        viewModelOf(::RemoteAccountLinkViewModel)
        viewModelOf(::RemoteLoginViewModel)
        viewModelOf(::RegistrationViewModel)
        viewModelOf(::ProfileViewModel)
        viewModelOf(::AdminViewModel)
        viewModelOf(::EditRemoteProfileViewModel)
        viewModelOf(::InventoryListViewModel)
        viewModelOf(::InventoryDetailViewModel)
        viewModelOf(::MainViewModel)
    }
