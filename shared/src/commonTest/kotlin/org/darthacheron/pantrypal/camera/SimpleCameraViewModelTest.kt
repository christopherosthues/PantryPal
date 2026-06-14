package org.darthacheron.pantrypal.camera

import com.kashif.cameraK.enums.FlashMode
import com.kashif.cameraK.enums.TorchMode
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import kotlin.test.*

class SimpleCameraViewModelTest {

    private lateinit var viewModel: SimpleCameraViewModel

    @BeforeTest
    fun setup() {
        viewModel = SimpleCameraViewModel()
    }

    @Test
    fun testInitialState() {
        val state = viewModel.uiState.value
        assertFalse(state.isCapturing)
        assertEquals(FlashMode.OFF, state.flashMode)
        assertEquals(TorchMode.OFF, state.torchMode)
        assertEquals(1f, state.zoomLevel)
    }

    @Test
    fun testOnFlashToggle() {
        val cameraController = mock<CameraControl> {
            every { toggleFlashMode() } returns Unit
            every { getFlashMode() } returns FlashMode.ON
        }

        viewModel.onFlashToggle(cameraController)

        verify { cameraController.toggleFlashMode() }
        assertEquals(FlashMode.ON, viewModel.uiState.value.flashMode)
    }

    @Test
    fun testOnTorchToggle() {
        val cameraController = mock<CameraControl> {
            every { toggleTorchMode() } returns Unit
            every { getTorchMode() } returns TorchMode.ON
        }

        viewModel.onTorchToggle(cameraController)

        verify { cameraController.toggleTorchMode() }
        assertEquals(TorchMode.ON, viewModel.uiState.value.torchMode)
    }

    @Test
    fun testOnLensSwitch() {
        val cameraController = mock<CameraControl> {
            every { toggleCameraLens() } returns Unit
            every { getMaxZoom() } returns 5f
        }

        viewModel.onLensSwitch(cameraController)

        verify { cameraController.toggleCameraLens() }
        assertEquals(5f, viewModel.uiState.value.maxZoom)
        assertEquals(1f, viewModel.uiState.value.zoomLevel)
    }

    @Test
    fun testOnZoomChanged() {
        val cameraController = mock<CameraControl> {
            every { setZoom(any()) } returns Unit
        }

        viewModel.onZoomChanged(cameraController, 2.5f)

        verify { cameraController.setZoom(2.5f) }
        assertEquals(2.5f, viewModel.uiState.value.zoomLevel)
    }
}
