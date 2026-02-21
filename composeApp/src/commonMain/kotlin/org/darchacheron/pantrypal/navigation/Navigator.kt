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
        _backStack?.add(NavRoute.FoodDetail(foodId))
    }

    fun goToSimpleCamera(foodId: String) {
        _backStack?.add(NavRoute.Camera(foodId))
    }

    fun goToSettings() {
        _backStack?.add(NavRoute.Settings)
    }

    fun goBack() {
        _backStack?.removeLastOrNull()
    }
}