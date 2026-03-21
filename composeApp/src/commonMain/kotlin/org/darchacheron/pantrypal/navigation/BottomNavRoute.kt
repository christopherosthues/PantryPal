package org.darchacheron.pantrypal.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface BottomNavRoute : NavKey {
    @Serializable
    data object FoodList : NavRoute

    @Serializable
    data object InventoryList : NavRoute

    @Serializable
    data object Profile : NavRoute
}