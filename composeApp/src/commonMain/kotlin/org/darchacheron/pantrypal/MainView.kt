package org.darchacheron.pantrypal

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun MainView(
    navigator: Navigator = koinInject()
) {
    var selectedTab: BottomNavRoute by remember { mutableStateOf(BottomNavRoute.FoodList) }
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()

    Scaffold(
        bottomBar = {
            NavigationBar {
                BottomNavRoute.items.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                painter = painterResource(tab.icon),
                                contentDescription = stringResource(tab.label)
                            )
                        },
                        label = { Text(stringResource(tab.label)) }
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
            val backstack = when (selectedTab) {
                BottomNavRoute.FoodList -> navigator.foodBackStack
                BottomNavRoute.InventoryList -> navigator.inventoryBackStack
                BottomNavRoute.Profile -> navigator.profileBackStack
            }

            backstack?.let {
                NavDisplay(
                    backStack = it,
                    onBack = {
                        when (selectedTab) {
                            BottomNavRoute.FoodList -> navigator.goBackFood()
                            BottomNavRoute.InventoryList -> navigator.goBackInventory()
                            BottomNavRoute.Profile -> {} // Profile usually doesn't have deep backstack here
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
