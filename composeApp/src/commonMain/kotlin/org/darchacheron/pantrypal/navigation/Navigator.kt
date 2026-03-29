package org.darchacheron.pantrypal.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack

class Navigator {
    private var _mainBackStack: NavBackStack<NavKey>? = null
    private var _foodBackStack: NavBackStack<NavKey>? = null
    private var _inventoryBackStack: NavBackStack<NavKey>? = null
    private var _profileBackStack: NavBackStack<NavKey>? = null

    val backStack get() = _mainBackStack
    val foodBackStack get() = _foodBackStack
    val inventoryBackStack get() = _inventoryBackStack
    val profileBackStack get() = _profileBackStack

    private var simpleCameraCallback: ((String) -> Unit)? = null
    private var ocrCameraCallback: ((String) -> Unit)? = null

    @Composable
    fun Initialize() {
        _mainBackStack = rememberNavBackStack(navConfig, NavRoute.Login)
        _foodBackStack = rememberNavBackStack(navConfig, BottomNavRoute.FoodList)
        _inventoryBackStack = rememberNavBackStack(navConfig, BottomNavRoute.InventoryList)
        _profileBackStack = rememberNavBackStack(navConfig, BottomNavRoute.Profile)
    }

    fun goToMain() {
        _mainBackStack?.clear()
        _mainBackStack?.add(NavRoute.Main)
    }

    fun goToFoodDetail(foodId: String? = null) {
        _foodBackStack?.removeAll { it is FoodNavRoute.FoodDetail }
        _foodBackStack?.add(FoodNavRoute.FoodDetail(foodId))
    }

    fun goToInventoryDetail(itemId: String? = null) {
        _inventoryBackStack?.removeAll { it is InventoryNavRoute.InventoryDetail }
        _inventoryBackStack?.add(InventoryNavRoute.InventoryDetail(itemId))
    }

    fun goToSimpleCamera(onSuccess: (String) -> Unit) {
        simpleCameraCallback = onSuccess
        _mainBackStack?.add(FoodNavRoute.SimpleCamera)
    }

    fun goToOcrCamera(type: OcrType, onRecognized: (String) -> Unit) {
        ocrCameraCallback = onRecognized
        _mainBackStack?.add(FoodNavRoute.OcrCamera(type))
    }

    fun onSimpleCameraResult(result: String) {
        simpleCameraCallback?.invoke(result)
        simpleCameraCallback = null
    }

    fun onOcrCameraResult(result: String) {
        ocrCameraCallback?.invoke(result)
        ocrCameraCallback = null
    }

    fun goToSettings() {
        _mainBackStack?.add(NavRoute.Settings)
    }

    fun goToLogin() {
        _mainBackStack?.clear()
        _mainBackStack?.add(NavRoute.Login)
    }

    fun goToRegister() {
        _mainBackStack?.clear()
        _mainBackStack?.add(NavRoute.Register)
    }

    fun goBack() {
        _mainBackStack?.removeLastOrNull()
    }

    fun goBackFood() {
        if ((_foodBackStack?.size ?: 0) > 1) {
            _foodBackStack?.removeLastOrNull()
        }
    }

    fun goBackInventory() {
        if ((_inventoryBackStack?.size ?: 0) > 1) {
            _inventoryBackStack?.removeLastOrNull()
        }
    }
}
