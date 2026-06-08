package org.darthacheron.pantrypal.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import org.darthacheron.pantrypal.authentication.AuthenticationPreferencesRepository
import org.koin.compose.koinInject

interface Navigator {
    var selectedBottomTab: BottomNavRoute
    val backStack: NavBackStack<NavKey>?
    val foodBackStack: NavBackStack<NavKey>?
    val inventoryBackStack: NavBackStack<NavKey>?
    val profileBackStack: NavBackStack<NavKey>?

    @Composable
    fun Initialize(
        authenticationPreferencesRepository: AuthenticationPreferencesRepository = koinInject()
    )

    fun goToMain()
    fun goToFoodDetail(foodId: String? = null)
    fun goToInventoryDetail(itemId: String? = null)
    fun goToSimpleCamera(onSuccess: (String) -> Unit)
    fun goToOcrCamera(type: OcrType, onRecognized: (String) -> Unit)
    fun onSimpleCameraResult(result: String)
    fun onOcrCameraResult(result: String)
    fun goToSettings()
    fun goToAdmin()
    fun goToLogin()
    fun goToRegister()
    fun goBack()
    fun goBackFood()
    fun goBackInventory()
}

