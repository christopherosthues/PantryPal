package org.darthacheron.pantrypal.camera

import com.kashif.cameraK.enums.FlashMode
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import dev.mokkery.verify
import org.darthacheron.pantrypal.navigation.FoodNavRoute
import org.darthacheron.pantrypal.navigation.Navigator
import org.darthacheron.pantrypal.navigation.OcrType
import kotlin.test.*

class OcrCameraViewModelTest {

    private lateinit var viewModel: OcrCameraViewModel
    private lateinit var navigator: Navigator

    @BeforeTest
    fun setup() {
        navigator = mock<Navigator>()
        viewModel = OcrCameraViewModel(FoodNavRoute.OcrCamera(OcrType.NUTRIENTS), navigator)
    }

    @Test
    fun testInitialState() {
        val state = viewModel.uiState.value
        assertNull(state.capturedText)
        assertTrue(state.capturedLines.isEmpty())
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
    fun testRetryResetsState() {
        viewModel.retry()
        val state = viewModel.uiState.value
        assertNull(state.capturedImageFilePath)
        assertNull(state.capturedText)
        assertTrue(state.capturedLines.isEmpty())
    }
}
