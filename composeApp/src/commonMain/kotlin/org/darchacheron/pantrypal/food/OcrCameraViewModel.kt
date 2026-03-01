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
    val capturedImageBytes: ByteArray? = null,
    val capturedText: String? = null,
    val isCapturing: Boolean = false,
    val flashMode: FlashMode = FlashMode.OFF,
    val torchMode: TorchMode = TorchMode.OFF,
    val zoomLevel: Float = 1f,
    val maxZoom: Float = 1f,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as OcrCameraUiState

        if (capturedImageFilePath != other.capturedImageFilePath) return false
        if (capturedImageBytes != null) {
            if (other.capturedImageBytes == null) return false
            if (!capturedImageBytes.contentEquals(other.capturedImageBytes)) return false
        } else if (other.capturedImageBytes != null) return false
        if (capturedText != other.capturedText) return false
        if (isCapturing != other.isCapturing) return false
        if (flashMode != other.flashMode) return false
        if (torchMode != other.torchMode) return false
        if (zoomLevel != other.zoomLevel) return false
        if (maxZoom != other.maxZoom) return false

        return true
    }

    override fun hashCode(): Int {
        var result = capturedImageFilePath?.hashCode() ?: 0
        result = 31 * result + (capturedImageBytes?.contentHashCode() ?: 0)
        result = 31 * result + (capturedText?.hashCode() ?: 0)
        result = 31 * result + isCapturing.hashCode()
        result = 31 * result + flashMode.hashCode()
        result = 31 * result + torchMode.hashCode()
        result = 31 * result + zoomLevel.hashCode()
        result = 31 * result + maxZoom.hashCode()
        return result
    }
}

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
                var result = cameraController.takePictureToFile()
                if (result is ImageCaptureResult.Error) {
                    Logger.withTag(loggerTag).w { "takePictureToFile failed, retrying with takePicture: ${result.exception.message}" }
                    result = cameraController.takePicture()
                }

                when (result) {
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
                                capturedImageBytes = null,
                                capturedText = recognizedText,
                                isCapturing = false
                            )
                        }
                    }
                    is ImageCaptureResult.Success -> {
                        // Fallback for platforms that don't support direct file capture
                        Logger.withTag(loggerTag).i { "Image captured successfully (${result.byteArray.size} bytes)" }
                        val bitmap = result.byteArray.decodeToImageBitmap()
                        val recognizedText = extractTextFromBitmapImpl(bitmap)
                        _uiState.update {
                            it.copy(
                                capturedImageFilePath = null,
                                capturedImageBytes = result.byteArray,
                                capturedText = recognizedText,
                                isCapturing = false
                            )
                        }
                    }
                    is ImageCaptureResult.Error -> {
                        Logger.withTag(loggerTag).e { "Image Capture Error: ${result.exception.message}" }
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
        _uiState.update { it.copy(capturedImageFilePath = null, capturedImageBytes = null, capturedText = null) }
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
