package org.darthacheron.pantrypal.navigation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

internal val navConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(NavRoute.Main::class, NavRoute.Main.serializer())
            subclass(NavRoute.Settings::class, NavRoute.Settings.serializer())
            subclass(NavRoute.Login::class, NavRoute.Login.serializer())
            subclass(NavRoute.Register::class, NavRoute.Register.serializer())
            subclass(BottomNavRoute.FoodList::class, BottomNavRoute.FoodList.serializer())
            subclass(BottomNavRoute.InventoryList::class, BottomNavRoute.InventoryList.serializer())
            subclass(BottomNavRoute.Profile::class, BottomNavRoute.Profile.serializer())
            subclass(FoodNavRoute.FoodDetail::class, FoodNavRoute.FoodDetail.serializer())
            subclass(FoodNavRoute.SimpleCamera::class, FoodNavRoute.SimpleCamera.serializer())
            subclass(FoodNavRoute.OcrCamera::class, FoodNavRoute.OcrCamera.serializer())
            subclass(InventoryNavRoute.InventoryDetail::class, InventoryNavRoute.InventoryDetail.serializer())
        }
    }
}

internal val bottomNavConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(BottomNavRoute.FoodList::class, BottomNavRoute.FoodList.serializer())
            subclass(BottomNavRoute.InventoryList::class, BottomNavRoute.InventoryList.serializer())
            subclass(BottomNavRoute.Profile::class, BottomNavRoute.Profile.serializer())
        }
    }
}

internal val foodNavConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(FoodNavRoute.FoodDetail::class, FoodNavRoute.FoodDetail.serializer())
            subclass(FoodNavRoute.SimpleCamera::class, FoodNavRoute.SimpleCamera.serializer())
            subclass(FoodNavRoute.OcrCamera::class, FoodNavRoute.OcrCamera.serializer())
        }
    }
}
