package org.darchacheron.pantrypal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.darchacheron.pantrypal.food.FoodListView
import org.darchacheron.pantrypal.navigation.BottomNavRoute
import org.darchacheron.pantrypal.navigation.Navigator
import org.darchacheron.pantrypal.profile.ProfileView
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import pantrypal.composeapp.generated.resources.Res
import pantrypal.composeapp.generated.resources.ic_food
import pantrypal.composeapp.generated.resources.ic_settings
import pantrypal.composeapp.generated.resources.main_tab_food
import pantrypal.composeapp.generated.resources.main_tab_profile

@Composable
fun MainView(
    navigator: Navigator = koinInject()
) {
    var selectedTab: BottomNavRoute by remember { mutableStateOf(BottomNavRoute.FoodList) }

    Scaffold(
        topBar = {},
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
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            when (selectedTab) {
                BottomNavRoute.FoodList -> FoodListView()
                BottomNavRoute.InventoryList -> {
                    Text(text = "Placeholder Inventory", modifier = Modifier.fillMaxSize(), color = Color.White)
                }
                BottomNavRoute.Profile -> ProfileView(
                    onGoToSettings = { navigator.goToSettings() }
                )
            }
        }
    }
}
