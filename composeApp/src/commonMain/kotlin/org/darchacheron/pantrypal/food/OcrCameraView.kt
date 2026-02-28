package org.darchacheron.pantrypal.food

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
import com.kashif.cameraK.compose.CameraKScreen
import com.kashif.cameraK.compose.rememberCameraKState
import com.kashif.cameraK.controller.CameraController
import com.kashif.cameraK.enums.AspectRatio
import com.kashif.cameraK.enums.CameraDeviceType
import com.kashif.cameraK.enums.CameraLens
import com.kashif.cameraK.enums.Directory
import com.kashif.cameraK.enums.FlashMode
import com.kashif.cameraK.enums.ImageFormat
import com.kashif.cameraK.enums.QualityPrioritization
import com.kashif.cameraK.enums.TorchMode
import com.kashif.cameraK.permissions.Permissions
import com.kashif.cameraK.permissions.providePermissions
import com.kashif.cameraK.result.ImageCaptureResult
import com.kashif.cameraK.state.CameraConfiguration
import com.kashif.cameraK.state.CameraKState
import com.kashif.imagesaverplugin.ImageSaverConfig
import com.kashif.imagesaverplugin.ImageSaverPlugin
import com.kashif.imagesaverplugin.rememberImageSaverPlugin
import com.kashif.ocrPlugin.OcrPlugin
import com.kashif.ocrPlugin.extractTextFromBitmapImpl
import com.kashif.ocrPlugin.rememberOcrPlugin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import org.darchacheron.pantrypal.navigation.OcrType
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import pantrypal.composeapp.generated.resources.Res
import pantrypal.composeapp.generated.resources.appName
import pantrypal.composeapp.generated.resources.ic_camera_lens
import pantrypal.composeapp.generated.resources.ic_cameraswitch
import pantrypal.composeapp.generated.resources.ic_flash_off
import pantrypal.composeapp.generated.resources.ic_flash_on
import pantrypal.composeapp.generated.resources.ic_flashlight_off
import pantrypal.composeapp.generated.resources.ic_flashlight_on
import pantrypal.composeapp.generated.resources.ic_x
import pantrypal.composeapp.generated.resources.simple_camera_content_description_capture_button
import pantrypal.composeapp.generated.resources.simple_camera_content_description_captured_image_preview
import pantrypal.composeapp.generated.resources.simple_camera_content_description_close_preview
import pantrypal.composeapp.generated.resources.simple_camera_content_description_error_icon
import pantrypal.composeapp.generated.resources.simple_camera_content_description_flash_toggle
import pantrypal.composeapp.generated.resources.simple_camera_content_description_switch_lens
import pantrypal.composeapp.generated.resources.simple_camera_content_description_torch_toggle
import pantrypal.composeapp.generated.resources.simple_camera_error_title
import pantrypal.composeapp.generated.resources.simple_camera_initializing

private const val simpleCameraLoggerTag = "SimpleCamera"

@Composable
fun OcrCameraView(
    ocrType: OcrType,
    onRecognized: (String) -> Unit,
    onBack: () -> Unit
) {
    val permissions: Permissions = providePermissions()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars),
    ) {
        val cameraPermissionState = remember { mutableStateOf(permissions.hasCameraPermission()) }
        val storagePermissionState = remember { mutableStateOf(permissions.hasStoragePermission()) }

        // Create all plugin instances
        val imageSaverPlugin = rememberImageSaverPlugin(
            config = ImageSaverConfig(
                isAutoSave = true,
                prefix = stringResource(Res.string.appName),
                directory = Directory.PICTURES,
                customFolderName = stringResource(Res.string.appName),
            ),
        )

        val ocrPlugin = rememberOcrPlugin()

        PermissionsHandler(
            permissions = permissions,
            cameraPermissionState = cameraPermissionState,
            storagePermissionState = storagePermissionState,
        )

        if (cameraPermissionState.value && storagePermissionState.value) {
            CameraContent(
                ocrType = ocrType,
                onRecognized = onRecognized,
                onBack = onBack,
                imageSaverPlugin = imageSaverPlugin,
                ocrPlugin = ocrPlugin
            )
        }
    }
}

@Composable
private fun PermissionsHandler(
    permissions: Permissions,
    cameraPermissionState: MutableState<Boolean>,
    storagePermissionState: MutableState<Boolean>,
) {
    if (!cameraPermissionState.value) {
        permissions.RequestCameraPermission(
            onGranted = { cameraPermissionState.value = true },
            onDenied = {
                Logger.withTag(simpleCameraLoggerTag).w { "Camera Permission Denied" }
            }
        )
    }

    if (!storagePermissionState.value) {
        permissions.RequestStoragePermission(
            onGranted = { storagePermissionState.value = true },
            onDenied = {
                Logger.withTag(simpleCameraLoggerTag).w { "Storage Permission Denied" }
            }
        )
    }
}

@Composable
private fun CameraContent(
    ocrType: OcrType,
    onRecognized: (String) -> Unit,
    onBack: () -> Unit,
    imageSaverPlugin: ImageSaverPlugin,
    ocrPlugin: OcrPlugin
) {
    val cameraState by rememberCameraKState(
        config = CameraConfiguration(
            cameraLens = CameraLens.BACK,
            flashMode = FlashMode.OFF,
            imageFormat = ImageFormat.JPEG,
            directory = Directory.PICTURES,
            torchMode = TorchMode.OFF,
            qualityPrioritization = QualityPrioritization.QUALITY,
            cameraDeviceType = CameraDeviceType.WIDE_ANGLE,
            aspectRatio = AspectRatio.RATIO_16_9,
            targetResolution = 1920 to 1080,
            returnFilePath = true
        ),
        setupPlugins = { stateHolder ->
            stateHolder.attachPlugin(imageSaverPlugin)
            stateHolder.attachPlugin(ocrPlugin)
        },
    )

    CameraKScreen(
        cameraState = cameraState,
        showPreview = true,
        loadingContent = {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        stringResource(Res.string.simple_camera_initializing),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        },
        errorContent = { error ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(32.dp),
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_x),
                        contentDescription = stringResource(Res.string.simple_camera_content_description_error_icon),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp),
                    )
                    Text(
                        stringResource(Res.string.simple_camera_error_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        error.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        },
    ) { state ->
        EnhancedCameraScreen(
            ocrType = ocrType,
            onRecognized = onRecognized,
            onBack = onBack,
            cameraState = state,
            ocrPlugin = ocrPlugin
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnhancedCameraScreen(
    ocrType: OcrType,
    onRecognized: (String) -> Unit,
    onBack: () -> Unit,
    cameraState: CameraKState.Ready,
    ocrPlugin: OcrPlugin
) {
    val scope = rememberCoroutineScope()
    val cameraController = cameraState.controller
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var isCapturing by remember { mutableStateOf(false) }

    // Camera settings state
    var flashMode by remember { mutableStateOf(FlashMode.OFF) }
    var torchMode by remember { mutableStateOf(TorchMode.OFF) }
    var zoomLevel by remember { mutableFloatStateOf(1f) }
    var maxZoom by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(cameraController) {
        // Poll for max zoom as it might not be ready immediately after state is Ready
        var tries = 0
        while (maxZoom <= 1f && tries < 10) {
            maxZoom = cameraController.getMaxZoom()
            if (maxZoom <= 1f) {
                delay(100)
            }
            tries++
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // OCR Hint
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = when (ocrType) {
                    OcrType.NAME -> "Scan product name"
                    OcrType.AMOUNT -> "Scan weight or volume (e.g. 500g, 1L)"
                    OcrType.NUTRIENTS -> "Scan nutrition table"
                },
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        }

        // Quick controls overlay (Flash, Torch, Switch)
        QuickControlsOverlay(
            modifier = Modifier.align(Alignment.TopEnd),
            flashMode = flashMode,
            torchMode = torchMode,
            onFlashToggle = {
                cameraController.toggleFlashMode()
                flashMode = cameraController.getFlashMode() ?: FlashMode.OFF
            },
            onTorchToggle = {
                cameraController.toggleTorchMode()
                torchMode = cameraController.getTorchMode() ?: TorchMode.OFF
            },
            onLensSwitch = {
                cameraController.toggleCameraLens()
                maxZoom = cameraController.getMaxZoom()
                zoomLevel = 1f
            }
        )

        // Zoom Slider (Left side)
        if (maxZoom > 1f) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp)
                    .fillMaxHeight(0.5f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "${(maxZoom * 10).toInt() / 10f}x",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Slider(
                        value = zoomLevel,
                        onValueChange = {
                            zoomLevel = it
                            cameraController.setZoom(it)
                        },
                        valueRange = 1f..maxZoom,
                        modifier = Modifier
                            .graphicsLayer {
                                rotationZ = 270f
                                transformOrigin = androidx.compose.ui.graphics.TransformOrigin.Center
                            }
                            .width(16.dp) // Maintain width for sliding distance
                            .height(200.dp) // Provide enough height for the rotated slider to not overlap labels
                            .layout { measurable, constraints ->
                                val placeable = measurable.measure(
                                    Constraints(
                                        minWidth = constraints.minHeight,
                                        maxWidth = constraints.maxHeight,
                                        minHeight = constraints.minWidth,
                                        maxHeight = constraints.maxWidth
                                    )
                                )
                                layout(placeable.height, placeable.width) {
                                    placeable.place(
                                        -((placeable.width - placeable.height) / 2),
                                        -((placeable.height - placeable.width) / 2)
                                    )
                                }
                            },
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        )
                    )

                    Text(
                        text = "1x",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        // Capture button
        CaptureButton(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 60.dp),
            isCapturing = isCapturing,
            onCapture = {
                if (!isCapturing) {
                    isCapturing = true
                    scope.launch {
                        handleOcrCapture(
                            cameraController = cameraController,
                            ocrPlugin = ocrPlugin,
                            onRecognized = onRecognized,
                        )
                        isCapturing = false
                        onBack()
                    }
                }
            },
        )

        // Captured image preview
        CapturedImagePreview(imageBitmap = imageBitmap) {
            imageBitmap = null
        }

        // Back button
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_x),
                contentDescription = stringResource(Res.string.simple_camera_content_description_close_preview),
                tint = Color.White
            )
        }
    }
}

@Composable
private fun QuickControlsOverlay(
    modifier: Modifier = Modifier,
    flashMode: FlashMode,
    torchMode: TorchMode,
    onFlashToggle: () -> Unit,
    onTorchToggle: () -> Unit,
    onLensSwitch: () -> Unit,
) {
    Surface(
        modifier = modifier.padding(16.dp),
        color = Color.Black.copy(alpha = 0.6f),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(onClick = onFlashToggle) {
                Icon(
                    painter = painterResource(
                        if (flashMode == FlashMode.OFF) Res.drawable.ic_flash_off else Res.drawable.ic_flash_on
                    ),
                    contentDescription = stringResource(Res.string.simple_camera_content_description_flash_toggle),
                    tint = Color.White,
                )
            }
            IconButton(onClick = onTorchToggle) {
                Icon(
                    painter = painterResource(
                        if (torchMode == TorchMode.OFF) Res.drawable.ic_flashlight_off else Res.drawable.ic_flashlight_on
                    ),
                    contentDescription = stringResource(Res.string.simple_camera_content_description_torch_toggle),
                    tint = Color.White,
                )
            }
            IconButton(onClick = onLensSwitch) {
                Icon(
                    painter = painterResource(Res.drawable.ic_cameraswitch),
                    contentDescription = stringResource(Res.string.simple_camera_content_description_switch_lens),
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
private fun CaptureButton(modifier: Modifier = Modifier, isCapturing: Boolean, onCapture: () -> Unit) {
    FilledTonalButton(
        onClick = onCapture,
        enabled = !isCapturing,
        modifier = modifier.size(80.dp).clip(CircleShape),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
        ),
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_camera_lens),
            contentDescription = stringResource(Res.string.simple_camera_content_description_capture_button),
            tint = if (isCapturing) Color.White.copy(alpha = 0.5f) else Color.White,
            modifier = Modifier.size(32.dp),
        )
    }
}

@Composable
private fun CapturedImagePreview(imageBitmap: ImageBitmap?, onDismiss: () -> Unit) {
    imageBitmap?.let { bitmap ->
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black.copy(alpha = 0.9f),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    bitmap = bitmap,
                    contentDescription = stringResource(Res.string.simple_camera_content_description_captured_image_preview),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentScale = ContentScale.Fit,
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.6f),
                            CircleShape,
                        ),
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_x),
                        contentDescription = stringResource(Res.string.simple_camera_content_description_close_preview),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.rotate(120f),
                    )
                }
            }
        }

        LaunchedEffect(bitmap) {
            delay(3000)
            onDismiss()
        }
    }
}

private suspend fun handleOcrCapture(
    cameraController: CameraController,
    ocrPlugin: OcrPlugin,
    onRecognized: (String) -> Unit,
) {
    when (val result = cameraController.takePictureToFile()) {
        is ImageCaptureResult.SuccessWithFile -> {
            Logger.withTag(simpleCameraLoggerTag).i { "Image captured for OCR: ${result.filePath}" }
            try {
                val path = result.filePath.toPath()
                val bytes = FileSystem.SYSTEM.read(path) {
                    readByteArray()
                }
                val bitmap = bytes.decodeToImageBitmap()
                val recognizedText = extractTextFromBitmapImpl(bitmap)
                onRecognized(recognizedText)
            } catch (e: Exception) {
                Logger.withTag(simpleCameraLoggerTag).e(e) { "OCR failed" }
            }
        }

        is ImageCaptureResult.Error -> {
            Logger.withTag(simpleCameraLoggerTag).e { "Image Capture Error: ${result.exception.message}" }
        }

        else -> {}
    }
}

//    scope.launch {
//        when (val result = controller.takePictureToFile()) {
//            is ImageCaptureResult.SuccessWithFile -> {
//                val file = File(result.filePath)
//                val byteArray = file.readBytes()
//                val text = ocrPlugin.recognizeText(byteArray)
//            }
//        }
//    }