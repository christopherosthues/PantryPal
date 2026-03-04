package org.darchacheron.pantrypal.food

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
import coil3.compose.AsyncImage
import com.kashif.cameraK.compose.CameraKScreen
import com.kashif.cameraK.compose.rememberCameraKState
import com.kashif.cameraK.enums.*
import com.kashif.cameraK.permissions.Permissions
import com.kashif.cameraK.permissions.providePermissions
import com.kashif.cameraK.state.CameraConfiguration
import com.kashif.cameraK.state.CameraKState
import com.kashif.imagesaverplugin.ImageSaverConfig
import com.kashif.imagesaverplugin.ImageSaverPlugin
import com.kashif.imagesaverplugin.rememberImageSaverPlugin
import com.kashif.ocrPlugin.OcrPlugin
import com.kashif.ocrPlugin.rememberOcrPlugin
import kotlinx.coroutines.delay
import org.darchacheron.pantrypal.navigation.OcrType
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import pantrypal.composeapp.generated.resources.*

private const val ocrCameraLoggerTag = "OcrCamera"

@Composable
fun OcrCameraView(
    viewModel: OcrCameraViewModel,
    onRecognized: (String) -> Unit,
) {
    val permissions: Permissions = providePermissions()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars),
    ) {
        val cameraPermissionState = remember { mutableStateOf(permissions.hasCameraPermission()) }
        val storagePermissionState = remember { mutableStateOf(permissions.hasStoragePermission()) }

        val imageSaverPlugin = rememberImageSaverPlugin(
            config = ImageSaverConfig(
                isAutoSave = true,
                prefix = "PantryPal",
                directory = Directory.PICTURES,
                customFolderName = "PantryPal",
                imageFormat = ImageFormat.JPEG
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
                viewModel = viewModel,
                onRecognized = onRecognized,
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
                Logger.withTag(ocrCameraLoggerTag).w { "Camera Permission Denied" }
            }
        )
    }

    RequestStoragePermission(storagePermissionState, permissions)
}

@Composable
private fun RequestStoragePermission(
    storagePermissionState: MutableState<Boolean>,
    permissions: Permissions
) {
    if (!storagePermissionState.value) {
        permissions.RequestStoragePermission(
            onGranted = { storagePermissionState.value = true },
            onDenied = {
                Logger.withTag(ocrCameraLoggerTag).w { "Storage Permission Denied" }
            }
        )
    }
}

@Composable
private fun CameraContent(
    viewModel: OcrCameraViewModel,
    onRecognized: (String) -> Unit,
    imageSaverPlugin: ImageSaverPlugin,
    ocrPlugin: OcrPlugin
) {
    val cameraState by rememberCameraKState(
        config = CameraConfiguration(
            cameraLens = CameraLens.BACK,
            flashMode = FlashMode.OFF,
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
            viewModel = viewModel,
            onRecognized = onRecognized,
            cameraState = state,
            ocrPlugin = ocrPlugin
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnhancedCameraScreen(
    viewModel: OcrCameraViewModel,
    onRecognized: (String) -> Unit,
    cameraState: CameraKState.Ready,
    ocrPlugin: OcrPlugin
) {
    val uiState by viewModel.uiState.collectAsState()
    val cameraController = cameraState.controller

    LaunchedEffect(cameraController) {
        var tries = 0
        var maxZoom = 1f
        while (maxZoom <= 1f && tries < 10) {
            maxZoom = cameraController.getMaxZoom()
            if (maxZoom <= 1f) {
                delay(100)
            }
            tries++
        }
        viewModel.updateMaxZoom(maxZoom)
    }

    val zoomLevel = uiState.zoomLevel

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoomChange, _ ->
                    if (zoomChange != 1f) {
                        viewModel.onZoomChanged(cameraController, zoomLevel * zoomChange)
                    }
                }
            },
    ) {
        if (uiState.capturedImageFilePath == null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = when (viewModel.route.type) {
                        OcrType.NAME -> "Scan product name"
                        OcrType.AMOUNT -> "Scan weight or volume (e.g. 500g, 1L)"
                        OcrType.NUTRIENTS -> "Scan nutrition table"
                        OcrType.DATE -> "Scan expiration date"
                    },
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            QuickControlsOverlay(
                modifier = Modifier.align(Alignment.TopEnd),
                flashMode = uiState.flashMode,
                torchMode = uiState.torchMode,
                onFlashToggle = { viewModel.onFlashToggle(cameraController) },
                onTorchToggle = { viewModel.onTorchToggle(cameraController) },
                onLensSwitch = { viewModel.onLensSwitch(cameraController) }
            )

            if (uiState.maxZoom > 1f) {
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
                            text = "${(uiState.maxZoom * 10).toInt() / 10f}x",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Slider(
                            value = uiState.zoomLevel,
                            onValueChange = { viewModel.onZoomChanged(cameraController, it) },
                            valueRange = 1f..uiState.maxZoom,
                            modifier = Modifier
                                .graphicsLayer {
                                    rotationZ = 270f
                                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin.Center
                                }
                                .width(16.dp)
                                .height(200.dp)
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

            CaptureButton(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 60.dp),
                isCapturing = uiState.isCapturing,
                onCapture = { viewModel.capture(cameraController, ocrPlugin) },
            )

            IconButton(
                onClick = { viewModel.goBack() },
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
        } else {
            OcrResultPreview(
                viewModel = viewModel,
                imageFilePath = uiState.capturedImageFilePath!!,
                onAccept = { viewModel.accept(onRecognized) },
                onRetry = { viewModel.retry() },
                onClose = { viewModel.goBack() }
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
private fun OcrResultPreview(
    viewModel: OcrCameraViewModel,
    imageFilePath: String,
    onAccept: () -> Unit,
    onRetry: () -> Unit,
    onClose: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .weight(0.3f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = imageFilePath,
                    contentDescription = stringResource(Res.string.simple_camera_content_description_captured_image_preview),
                    modifier = Modifier.fillMaxSize(),
                )

                IconButton(
                    onClick = onClose,
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

                IconButton(
                    onClick = { viewModel.resetLines() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_reset),
                        contentDescription = "Reset OCR",
                        tint = Color.White
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .weight(0.7f)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .windowInsetsPadding(WindowInsets.systemBars),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.ocr_camera_extraction_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    if (viewModel.route.type == OcrType.NUTRIENTS) {
                        NutrientQuickTags(onTagSelected = { tag ->
                            uiState.selectedLineIndex?.let { viewModel.tagLine(it, tag) }
                        })
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(8.dp)
                    ) {
                        if (uiState.capturedLines.isEmpty()) {
                            Text(
                                text = stringResource(Res.string.ocr_camera_no_text_detected),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().align(Alignment.Center)
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(uiState.capturedLines) { index, line ->
                                    OcrLineItem(
                                        line = line,
                                        isSelected = uiState.selectedLineIndex == index,
                                        onLineClicked = { viewModel.toggleLineSelection(index) },
                                        onLineChanged = { viewModel.updateLine(index, it) },
                                        onMoveUp = if (index > 0) { { viewModel.moveLineUp(index) } } else null,
                                        onMoveDown = if (index < uiState.capturedLines.size - 1) { { viewModel.moveLineDown(index) } } else null,
                                        onMerge = if (index < uiState.capturedLines.size - 1) { { viewModel.mergeWithNext(index) } } else null,
                                        onDelete = { viewModel.deleteLine(index) }
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilledTonalButton(
                            onClick = onRetry,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(text = stringResource(Res.string.ocr_camera_retry))
                        }
                        Button(
                            onClick = onAccept,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(text = stringResource(Res.string.ocr_camera_accept))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NutrientQuickTags(onTagSelected: (String) -> Unit) {
    val tags = listOf("kcal", "kJ", "Fat:", "Sat.Fat:", "Carbs:", "Sugar:", "Fiber:", "Protein:", "Salt:")

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 8.dp)
    ) {
        itemsIndexed(tags) { _, tag ->
            AssistChip(
                onClick = { onTagSelected(tag) },
                label = { Text(tag) }
            )
        }
    }
}

@Composable
private fun OcrLineItem(
    line: String,
    isSelected: Boolean,
    onLineClicked: () -> Unit,
    onLineChanged: (String) -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onMerge: (() -> Unit)?,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onLineClicked() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(8.dp),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                TextField(
                    value = line,
                    onValueChange = onLineChanged,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    singleLine = true
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onMerge != null) {
                    IconButton(onClick = onMerge, modifier = Modifier.size(32.dp)) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_add),
                            contentDescription = stringResource(Res.string.ocr_camera_merge),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_delete),
                        contentDescription = stringResource(Res.string.ocr_camera_delete),
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { onMoveUp?.invoke() },
                        enabled = onMoveUp != null,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_arrow_upward),
                            contentDescription = stringResource(Res.string.ocr_camera_move_up),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    IconButton(
                        onClick = { onMoveDown?.invoke() },
                        enabled = onMoveDown != null,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_arrow_downward),
                            contentDescription = stringResource(Res.string.ocr_camera_move_down),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
