package org.darchacheron.pantrypal.food

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.kashif.cameraK.controller.CameraController
import com.kashif.cameraK.enums.FlashMode
import com.kashif.cameraK.enums.TorchMode
import com.kashif.cameraK.result.ImageCaptureResult
import com.kashif.ocrPlugin.OcrPlugin
import com.kashif.ocrPlugin.extractTextFromBitmapImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import org.darchacheron.pantrypal.navigation.NavRoute
import org.darchacheron.pantrypal.navigation.Navigator
import org.jetbrains.compose.resources.decodeToImageBitmap

data class OcrCameraUiState(
    val capturedImageFilePath: String? = null,
    val capturedText: String? = null,
    val isCapturing: Boolean = false,
    val flashMode: FlashMode = FlashMode.OFF,
    val torchMode: TorchMode = TorchMode.OFF,
    val zoomLevel: Float = 1f,
    val maxZoom: Float = 1f,
)

class OcrCameraViewModel(
    val route: NavRoute.OcrCamera,
    private val navigator: Navigator,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OcrCameraUiState())
    val uiState: StateFlow<OcrCameraUiState> = _uiState.asStateFlow()

    private val sessionFilePaths = mutableListOf<String>()
    private val loggerTag = "OcrCameraViewModel"

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

    fun onRecognizedTextChanged(recognizedText: String) {
        _uiState.update { it.copy(capturedText = recognizedText) }
    }

    fun updateMaxZoom(maxZoom: Float) {
        _uiState.update { it.copy(maxZoom = maxZoom) }
    }

    fun capture(cameraController: CameraController, ocrPlugin: OcrPlugin) {
        if (_uiState.value.isCapturing) return

        _uiState.update { it.copy(isCapturing = true) }

        viewModelScope.launch {
            try {
                when (val result = cameraController.takePictureToFile()) {
                    is ImageCaptureResult.SuccessWithFile -> {
                        val path = result.filePath
                        sessionFilePaths.add(path)
                        Logger.withTag(loggerTag).i { "Image captured for OCR: $path" }
                        
                        val bytes = FileSystem.SYSTEM.read(path.toPath()) {
                            readByteArray()
                        }
                        val bitmap = bytes.decodeToImageBitmap()
                        val recognizedText = extractTextFromBitmapImpl(bitmap)
                        
                        _uiState.update { 
                            it.copy(
                                capturedImageFilePath = result.filePath,
                                capturedText = recognizedText,
                                isCapturing = false
                            )
                        }
                    }
                    is ImageCaptureResult.Error -> {
                        Logger.withTag(loggerTag).e { "Image Capture Error: ${result.exception.message}" }
                        _uiState.update { it.copy(isCapturing = false) }
                    }
                    else -> {
                        _uiState.update { it.copy(isCapturing = false) }
                    }
                }
            } catch (e: Exception) {
                Logger.withTag(loggerTag).e(e) { "OCR failed" }
                _uiState.update { it.copy(isCapturing = false) }
            }
        }
    }

    fun retry() {
        _uiState.update { it.copy(capturedImageFilePath = null, capturedText = null) }
    }

    fun accept(onRecognized: (String) -> Unit) {
        onRecognized(_uiState.value.capturedText ?: "")
        navigator.goBack()
    }

    fun goBack() {
        navigator.goBack()
    }

    override fun onCleared() {
        super.onCleared()
        sessionFilePaths.forEach { path ->
            try {
                FileSystem.SYSTEM.delete(path.toPath())
                Logger.withTag(loggerTag).i { "Deleted session file: $path" }
            } catch (e: Exception) {
                Logger.withTag(loggerTag).e(e) { "Failed to delete session file: $path" }
            }
        }
    }
}
