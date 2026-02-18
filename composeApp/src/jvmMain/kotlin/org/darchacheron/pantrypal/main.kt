package org.darchacheron.pantrypal

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import pantrypal.composeapp.generated.resources.Res
import pantrypal.composeapp.generated.resources.appName
import pantrypal.composeapp.generated.resources.ic_app_launcher
import org.darchacheron.pantrypal.di.initKoin

fun main() {
    initKoin()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = stringResource(Res.string.appName),
            icon = painterResource(Res.drawable.ic_app_launcher)
        ) {
            App()
        }
    }
}