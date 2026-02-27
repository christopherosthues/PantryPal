package org.darchacheron.pantrypal.navigation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

internal val navConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(NavRoute.FoodList::class, NavRoute.FoodList.serializer())
            subclass(NavRoute.FoodDetail::class, NavRoute.FoodDetail.serializer())
            subclass(NavRoute.SimpleCamera::class, NavRoute.SimpleCamera.serializer())
            subclass(NavRoute.Settings::class, NavRoute.Settings.serializer())
        }
    }
}