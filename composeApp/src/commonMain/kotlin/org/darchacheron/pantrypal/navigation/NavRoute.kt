package org.darchacheron.pantrypal.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface NavRoute : NavKey {
    @Serializable
    data object FoodList : NavRoute

    @Serializable
    data class FoodDetail(val foodId: String? = null) : NavRoute

    @Serializable
    data class SimpleCamera(val onCapture: (String) -> Unit) : NavRoute

    @Serializable
    data class OcrCamera(val type: OcrType, val onRecognized: (String) -> Unit) : NavRoute

    @Serializable
    data object Settings : NavRoute
}

@Serializable
enum class OcrType {
    NAME, AMOUNT, NUTRIENTS
}
