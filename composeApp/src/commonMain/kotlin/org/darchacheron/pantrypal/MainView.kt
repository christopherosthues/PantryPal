package org.darchacheron.pantrypal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import org.darchacheron.pantrypal.navigation.BottomNavRoute
import org.darchacheron.pantrypal.navigation.Navigator
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.navigation3.koinEntryProvider
import org.koin.compose.viewmodel.koinViewModel
import pantrypal.composeapp.generated.resources.Res
import pantrypal.composeapp.generated.resources.profile_remote_deleted_delete_local
import pantrypal.composeapp.generated.resources.profile_remote_deleted_keep_local
import pantrypal.composeapp.generated.resources.profile_remote_deleted_message
import pantrypal.composeapp.generated.resources.profile_remote_deleted_title

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun MainView(
    navigator: Navigator = koinInject(),
    viewModel: MainViewModel = koinViewModel()
) {
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()
    val showRemoteDeletedDialog by viewModel.showRemoteDeletedDialog.collectAsState()

    if (showRemoteDeletedDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissRemoteDeletedDialog() },
            title = { Text(stringResource(Res.string.profile_remote_deleted_title)) },
            text = { Text(stringResource(Res.string.profile_remote_deleted_message)) },
            confirmButton = {
                Button(onClick = { viewModel.deleteLocalAccount() }) {
                    Text(stringResource(Res.string.profile_remote_deleted_delete_local))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.keepLocalAccountOnly() }) {
                    Text(stringResource(Res.string.profile_remote_deleted_keep_local))
                }
            }
        )
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                BottomNavRoute.items.forEach { tab ->
                    NavigationBarItem(
                        selected = navigator.selectedBottomTab == tab,
                        onClick = { navigator.selectedBottomTab = tab },
                        icon = {
                            Icon(
                                painter = painterResource(tab.icon),
                                contentDescription = stringResource(tab.label)
                            )
                        },
                        label = { Text(text = stringResource(tab.label)) }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
        ) {
            val backstack = when (navigator.selectedBottomTab) {
                BottomNavRoute.FoodList -> navigator.foodBackStack
                BottomNavRoute.InventoryList -> navigator.inventoryBackStack
                BottomNavRoute.Profile -> navigator.profileBackStack
            }

            backstack?.let {
                NavDisplay(
                    backStack = it,
                    onBack = {
                        when (navigator.selectedBottomTab) {
                            BottomNavRoute.FoodList -> navigator.goBackFood()
                            BottomNavRoute.InventoryList -> navigator.goBackInventory()
                            BottomNavRoute.Profile -> {}
                        }
                    },
                    sceneStrategies = listOf(listDetailStrategy),
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator()
                    ),
                    entryProvider = koinEntryProvider()
                )
            }
        }
    }
}
