package org.darthacheron.pantrypal.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.darthacheron.pantrypal.authentication.AuthenticationPreferencesRepository

class NavigatorImpl : Navigator {
    private var _mainBackStack: NavBackStack<NavKey>? = null
    private var _foodBackStack: NavBackStack<NavKey>? = null
    private var _inventoryBackStack: NavBackStack<NavKey>? = null
    private var _profileBackStack: NavBackStack<NavKey>? = null

    override var selectedBottomTab by mutableStateOf<BottomNavRoute>(BottomNavRoute.FoodList)

    override val backStack get() = _mainBackStack
    override val foodBackStack get() = _foodBackStack
    override val inventoryBackStack get() = _inventoryBackStack
    override val profileBackStack get() = _profileBackStack

    private var simpleCameraCallback: ((String) -> Unit)? = null
    private var ocrCameraCallback: ((String) -> Unit)? = null

    @Composable
    override fun Initialize(
        authenticationPreferencesRepository: AuthenticationPreferencesRepository
    ) {
        val startRoute = remember {
            runBlocking {
                val prefs = authenticationPreferencesRepository.authenticationPreferencesFlow.first()
                if (prefs.stayLoggedIn && prefs.localProfileId.isNotEmpty()) {
                    NavRoute.Main
                } else {
                    NavRoute.Login
                }
            }
        }

        _mainBackStack = rememberNavBackStack(navConfig, startRoute)
        _foodBackStack = rememberNavBackStack(navConfig, BottomNavRoute.FoodList)
        _inventoryBackStack = rememberNavBackStack(navConfig, BottomNavRoute.InventoryList)
        _profileBackStack = rememberNavBackStack(navConfig, BottomNavRoute.Profile)
    }

    override fun goToMain() {
        _mainBackStack?.clear()
        _mainBackStack?.add(NavRoute.Main)
    }

    override fun goToFoodDetail(foodId: String?) {
        _foodBackStack?.removeAll { it is FoodNavRoute.FoodDetail }
        _foodBackStack?.add(FoodNavRoute.FoodDetail(foodId))
    }

    override fun goToInventoryDetail(itemId: String?) {
        _inventoryBackStack?.removeAll { it is InventoryNavRoute.InventoryDetail }
        _inventoryBackStack?.add(InventoryNavRoute.InventoryDetail(itemId))
    }

    override fun goToSimpleCamera(onSuccess: (String) -> Unit) {
        simpleCameraCallback = onSuccess
        _mainBackStack?.add(FoodNavRoute.SimpleCamera)
    }

    override fun goToOcrCamera(type: OcrType, onRecognized: (String) -> Unit) {
        ocrCameraCallback = onRecognized
        _mainBackStack?.add(FoodNavRoute.OcrCamera(type))
    }

    override fun onSimpleCameraResult(result: String) {
        simpleCameraCallback?.invoke(result)
        simpleCameraCallback = null
    }

    override fun onOcrCameraResult(result: String) {
        ocrCameraCallback?.invoke(result)
        ocrCameraCallback = null
    }

    override fun goToSettings() {
        _mainBackStack?.add(NavRoute.Settings)
    }

    override fun goToAdmin() {
        _mainBackStack?.add(NavRoute.Admin)
    }

    override fun goToLogin() {
        clearBackStacks()
        _mainBackStack?.add(NavRoute.Login)
    }

    override fun goToRegister() {
        clearBackStacks()
        _mainBackStack?.add(NavRoute.Register)
    }

    private fun clearBackStacks() {
        _mainBackStack?.clear()
        _foodBackStack?.clear()
        _foodBackStack?.add(BottomNavRoute.FoodList)
        _inventoryBackStack?.clear()
        _inventoryBackStack?.add(BottomNavRoute.InventoryList)
        _profileBackStack?.clear()
        _profileBackStack?.add(BottomNavRoute.Profile)
        selectedBottomTab = BottomNavRoute.FoodList
    }

    override fun goBack() {
        _mainBackStack?.removeLastOrNull()
    }

    override fun goBackFood() {
        if ((_foodBackStack?.size ?: 0) > 1) {
            _foodBackStack?.removeLastOrNull()
        }
    }

    override fun goBackInventory() {
        if ((_inventoryBackStack?.size ?: 0) > 1) {
            _inventoryBackStack?.removeLastOrNull()
        }
    }
}