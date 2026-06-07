package org.darthacheron.pantrypal

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.github.vinceglb.filekit.FileKit
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import pantrypal.shared.generated.resources.Res
import pantrypal.shared.generated.resources.appName
import pantrypal.shared.generated.resources.ic_app_launcher
import org.darthacheron.pantrypal.di.initKoin

fun main() {
    initKoin()
    application {
        FileKit.init(appId = "PantryPal")

        Window(
            onCloseRequest = ::exitApplication,
            title = stringResource(Res.string.appName),
            icon = painterResource(Res.drawable.ic_app_launcher)
        ) {
            App()
        }
    }
}