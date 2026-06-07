package org.darthacheron.pantrypal.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.kashif.cameraK.controller.CameraController
import com.kashif.cameraK.enums.FlashMode
import com.kashif.cameraK.enums.TorchMode
import com.kashif.cameraK.result.ImageCaptureResult
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.filesDir
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import kotlin.time.Clock

data class SimpleCameraUiState(
    val isCapturing: Boolean = false,
    val flashMode: FlashMode = FlashMode.OFF,
    val torchMode: TorchMode = TorchMode.OFF,
    val zoomLevel: Float = 1f,
    val maxZoom: Float = 1f,
)

class SimpleCameraViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SimpleCameraUiState())
    val uiState: StateFlow<SimpleCameraUiState> = _uiState.asStateFlow()

    private val loggerTag = "SimpleCameraViewModel"

    fun onFlashToggle(cameraController: CameraController) {
        cameraController.toggleFlashMode()
        _uiState.update { it.copy(flashMode = cameraController.getFlashMode() ?: FlashMode.OFF) }
    }

    fun onTorchToggle(cameraController: CameraController) {
        cameraController.toggleTorchMode()
        _uiState.update { it.copy(torchMode = cameraController.getTorchMode() ?: TorchMode.OFF) }
    }

    fun onLensSwitch(cameraController: CameraController) {
        cameraController.toggleCameraLens()
        _uiState.update {
            it.copy(
                maxZoom = cameraController.getMaxZoom(),
                zoomLevel = 1f
            )
        }
    }

    fun onZoomChanged(cameraController: CameraController, zoomLevel: Float) {
        cameraController.setZoom(zoomLevel)
        _uiState.update { it.copy(zoomLevel = zoomLevel) }
    }

    fun updateMaxZoom(maxZoom: Float) {
        _uiState.update { it.copy(maxZoom = maxZoom) }
    }

    fun capture(cameraController: CameraController, onCapture: (String) -> Unit) {
        if (_uiState.value.isCapturing) return

        _uiState.update { it.copy(isCapturing = true) }

        viewModelScope.launch {
            try {
                var result = cameraController.takePictureToFile()
                if (result is ImageCaptureResult.Error) {
                    Logger.withTag(loggerTag).w { "takePictureToFile failed, retrying with takePicture: ${result.exception.message}" }
                    result = cameraController.takePicture()
                }

                val timestamp = Clock.System.now().toEpochMilliseconds()
                val fileName = "PantryPal_$timestamp.jpg"
                val targetDir = FileKit.filesDir.path.toPath() / "PantryPal"
                val targetFile = targetDir / fileName

                if (!FileSystem.SYSTEM.exists(targetDir)) {
                    FileSystem.SYSTEM.createDirectories(targetDir)
                }

                when (result) {
                    is ImageCaptureResult.SuccessWithFile -> {
                        val sourcePath = result.filePath.toPath()
                        FileSystem.SYSTEM.copy(sourcePath, targetFile)
                        FileSystem.SYSTEM.delete(sourcePath)
                        Logger.withTag(loggerTag).i { "Image captured and moved to: $targetFile" }
                        onCapture(targetFile.toString())
                    }
                    is ImageCaptureResult.Success -> {
                        FileSystem.SYSTEM.write(targetFile) {
                            write(result.byteArray)
                        }
                        Logger.withTag(loggerTag).i { "Image captured and saved to: $targetFile" }
                        onCapture(targetFile.toString())
                    }
                    is ImageCaptureResult.Error -> {
                        Logger.withTag(loggerTag).e { "Image Capture Error: ${result.exception.message}" }
                    }
                }
            } catch (e: Exception) {
                Logger.withTag(loggerTag).e(e) { "Capture failed" }
            } finally {
                _uiState.update { it.copy(isCapturing = false) }
            }
        }
    }
}
