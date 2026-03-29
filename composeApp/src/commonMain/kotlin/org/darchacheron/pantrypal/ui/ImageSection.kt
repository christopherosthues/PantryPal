package org.darchacheron.pantrypal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.darchacheron.pantrypal.camera.Image
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import pantrypal.composeapp.generated.resources.*

@Composable
fun ImageSection(
    primaryImage: Image?,
    additionalImages: List<Image>,
    onAddPrimaryImage: () -> Unit,
    onRemovePrimaryImage: () -> Unit,
    onAddAdditionalImage: () -> Unit,
    onRemoveAdditionalImage: (Image) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section Header for Primary Image
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(Res.string.food_detail_add_image), // Or a more generic "Main Image" if available
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            
            if (primaryImage?.localPath != null) {
                TextButton(onClick = onAddPrimaryImage) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_camera),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(Res.string.food_list_sort_name_asc)) // Placeholder for "Change" or "Replace" if exists, or just use "Add"
                }
            }
        }

        // Primary Image Display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { onAddPrimaryImage() },
            contentAlignment = Alignment.Center
        ) {
            if (primaryImage?.localPath != null) {
                AsyncImage(
                    model = primaryImage.localPath,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                // Remove button for primary image
                IconButton(
                    onClick = { onRemovePrimaryImage() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(32.dp)
                        .background(
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                            CircleShape
                        )
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_cancel),
                        contentDescription = stringResource(Res.string.food_detail_content_description_remove_image),
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_camera),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(Res.string.food_detail_add_image),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Additional Images Section
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(Res.string.food_detail_additional_images),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                
                if (additionalImages.isNotEmpty()) {
                    TextButton(onClick = onAddAdditionalImage) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_camera),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(Res.string.food_detail_add_image))
                    }
                }
            }

            if (additionalImages.isEmpty()) {
                OutlinedCard(
                    onClick = onAddAdditionalImage,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_camera),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = stringResource(Res.string.food_detail_add_image),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            } else {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(additionalImages) { image ->
                        if (image.localPath != null) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            ) {
                                AsyncImage(
                                    model = image.localPath,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                IconButton(
                                    onClick = { onRemoveAdditionalImage(image) },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(24.dp)
                                        .background(
                                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                                            CircleShape
                                        )
                                ) {
                                    Icon(
                                        painter = painterResource(Res.drawable.ic_cancel),
                                        contentDescription = stringResource(Res.string.food_detail_content_description_remove_image),
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
