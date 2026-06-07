package org.darthacheron.pantrypal.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import pantrypal.shared.generated.resources.Res
import pantrypal.shared.generated.resources.ic_food
import pantrypal.shared.generated.resources.ic_inventory
import pantrypal.shared.generated.resources.ic_profile
import pantrypal.shared.generated.resources.main_tab_food
import pantrypal.shared.generated.resources.main_tab_inventory
import pantrypal.shared.generated.resources.main_tab_profile

sealed interface BottomNavRoute : NavKey {
    val icon: DrawableResource
    val label: StringResource

    @Serializable
    data object FoodList : BottomNavRoute {
        override val icon: DrawableResource
            get() = Res.drawable.ic_food

        override val label: StringResource
            get() = Res.string.main_tab_food
    }

    @Serializable
    data object InventoryList : BottomNavRoute {
        override val icon: DrawableResource
            get() = Res.drawable.ic_inventory

        override val label: StringResource
            get() = Res.string.main_tab_inventory
    }

    @Serializable
    data object Profile : BottomNavRoute {
        override val icon: DrawableResource
            get() = Res.drawable.ic_profile

        override val label: StringResource
            get() = Res.string.main_tab_profile
    }

    companion object {
        val items = listOf(FoodList, InventoryList, Profile)
    }
}