package org.darthacheron.pantrypal.navigation

import kotlinx.serialization.Serializable

interface InventoryNavRoute {
    @Serializable
    data class InventoryDetail(val itemId: String? = null) : NavRoute
}
