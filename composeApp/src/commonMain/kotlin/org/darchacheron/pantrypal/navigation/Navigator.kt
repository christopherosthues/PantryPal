package org.darchacheron.pantrypal.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack

class Navigator {
    private var _mainBackStack: NavBackStack<NavKey>? = null
    private var _bottomBackStack: NavBackStack<NavKey>? = null
    private var _foodBackStack: NavBackStack<NavKey>? = null


    val backStack get() = _mainBackStack // TODO: return correct backstack

    private var simpleCameraCallback: ((String) -> Unit)? = null
    private var ocrCameraCallback: ((String) -> Unit)? = null

    @Composable
    fun Initialize() {
        _mainBackStack = rememberNavBackStack(navConfig, NavRoute.Login)
        _bottomBackStack = rememberNavBackStack(bottomNavConfig, BottomNavRoute.FoodList)
        _foodBackStack = rememberNavBackStack(foodNavConfig, FoodNavRoute.FoodDetail())
    }

    // TODO: Handle multiple backstacks

    fun goToMain() {
        _mainBackStack?.clear()
        _mainBackStack?.add(NavRoute.Main)
    }

    fun goToFoodList() {
        _mainBackStack?.removeAll { it is BottomNavRoute.FoodList }
        _mainBackStack?.add(BottomNavRoute.FoodList)
    }

    fun goToFoodDetail(foodId: String? = null) {
        _mainBackStack?.removeAll { it is FoodNavRoute.FoodDetail }
        _mainBackStack?.add(FoodNavRoute.FoodDetail(foodId))
    }

    fun goToInventoryDetail(itemId: String? = null) {
        _mainBackStack?.removeAll { it is InventoryNavRoute.InventoryDetail }
        _mainBackStack?.add(InventoryNavRoute.InventoryDetail(itemId))
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

    fun goToProfile() {
        _mainBackStack?.removeAll { it is BottomNavRoute.Profile }
        _mainBackStack?.add(BottomNavRoute.Profile)
    }

    fun goBack() {
        _mainBackStack?.removeLastOrNull()
    }
}
