package org.darchacheron.pantrypal.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack

class Navigator {
    private var _backStack: NavBackStack<NavKey>? = null

    val backStack get() = _backStack

    private var simpleCameraCallback: ((String) -> Unit)? = null
    private var ocrCameraCallback: ((String) -> Unit)? = null

    @Composable
    fun Initialize() {
        _backStack = rememberNavBackStack(navConfig, NavRoute.Login)
    }

    fun goToMain() {
        _backStack?.clear()
        _backStack?.add(NavRoute.Main)
    }

    fun goToFoodList() {
        _backStack?.removeAll { it is NavRoute.FoodList }
        _backStack?.add(NavRoute.FoodList)
    }

    fun goToFoodDetail(foodId: String? = null) {
        _backStack?.removeAll { it is NavRoute.FoodDetail }
        _backStack?.add(NavRoute.FoodDetail(foodId))
    }

    fun goToSimpleCamera(onSuccess: (String) -> Unit) {
        simpleCameraCallback = onSuccess
        _backStack?.add(NavRoute.SimpleCamera)
    }

    fun goToOcrCamera(type: OcrType, onRecognized: (String) -> Unit) {
        ocrCameraCallback = onRecognized
        _backStack?.add(NavRoute.OcrCamera(type))
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
        _backStack?.add(NavRoute.Settings)
    }

    fun goToLogin() {
        _backStack?.clear()
        _backStack?.add(NavRoute.Login)
    }

    fun goToRegister() {
        _backStack?.clear()
        _backStack?.add(NavRoute.Register)
    }

    fun goToProfile() {
        _backStack?.removeAll { it is NavRoute.Profile }
        _backStack?.add(NavRoute.Profile)
    }

    fun goBack() {
        _backStack?.removeLastOrNull()
    }
}
