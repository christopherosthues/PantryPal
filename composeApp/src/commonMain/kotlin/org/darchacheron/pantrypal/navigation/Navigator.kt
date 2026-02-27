package org.darchacheron.pantrypal.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack

class Navigator {
    private var _backStack: NavBackStack<NavKey>? = null

    val backStack get() = _backStack

    @Composable
    fun Initialize() {
        _backStack = rememberNavBackStack(navConfig, NavRoute.FoodList)
    }

    fun goToFoodDetail(foodId: String? = null) {
        _backStack?.removeAll { it is NavRoute.FoodDetail }
        _backStack?.add(NavRoute.FoodDetail(foodId))
    }

    fun goToSimpleCamera(onSuccess: (String) -> Unit) {
        _backStack?.add(NavRoute.SimpleCamera(onSuccess))
    }

    fun goToSettings() {
        _backStack?.add(NavRoute.Settings)
    }

    fun goBack() {
        _backStack?.removeLastOrNull()
    }
}