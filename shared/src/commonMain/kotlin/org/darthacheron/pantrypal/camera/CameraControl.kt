package org.darthacheron.pantrypal.camera

import com.kashif.cameraK.controller.CameraController
import com.kashif.cameraK.enums.FlashMode
import com.kashif.cameraK.enums.TorchMode
import com.kashif.cameraK.result.ImageCaptureResult

interface CameraControl {
    fun toggleFlashMode()
    fun getFlashMode(): FlashMode?
    fun toggleTorchMode()
    fun getTorchMode(): TorchMode?
    fun toggleCameraLens()
    fun getMaxZoom(): Float
    fun setZoom(zoomLevel: Float)
    suspend fun takePictureToFile(): ImageCaptureResult
    suspend fun takePicture(): ImageCaptureResult
}

class CameraControllerWrapper(private val controller: CameraController) : CameraControl {
    override fun toggleFlashMode() = controller.toggleFlashMode()
    override fun getFlashMode(): FlashMode? = controller.getFlashMode()
    override fun toggleTorchMode() = controller.toggleTorchMode()
    override fun getTorchMode(): TorchMode? = controller.getTorchMode()
    override fun toggleCameraLens() = controller.toggleCameraLens()
    override fun getMaxZoom(): Float = controller.getMaxZoom()
    override fun setZoom(zoomLevel: Float) = controller.setZoom(zoomLevel)
    override suspend fun takePictureToFile(): ImageCaptureResult = controller.takePictureToFile()
    override suspend fun takePicture(): ImageCaptureResult = controller.takePicture()
}
