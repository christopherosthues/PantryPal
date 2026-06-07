package org.darthacheron.pantrypal.navigation

import kotlinx.serialization.Serializable

interface FoodNavRoute {
    @Serializable
    data class FoodDetail(val foodId: String? = null) : NavRoute

    @Serializable
    data object SimpleCamera : NavRoute

    @Serializable
    data class OcrCamera(val type: OcrType) : NavRoute
}

@Serializable
enum class OcrType {
    NAME, AMOUNT, NUTRIENTS, DATE
}