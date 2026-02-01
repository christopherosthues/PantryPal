package org.darchacheron.pantrypal

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
//import org.darchacheron.pantrypal.di.initKoin

fun main() {
//    initKoin()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "pantrypal",
        ) {
            App()
        }
    }
}