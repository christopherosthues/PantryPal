package org.darchacheron.pantrypal.food

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.ktor.client.request.invoke
import org.darchacheron.pantrypal.ui.PantryPalTheme
import org.jetbrains.compose.resources.painterResource
import kotlin.uuid.ExperimentalUuidApi
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import pantrypal.composeapp.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)
@Composable
fun FoodListView(
    foodListViewModel: FoodListViewModel = koinInject(),
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val uiState by foodListViewModel.uiState.collectAsState()
    val searchQuery by foodListViewModel.searchQuery.collectAsState()
    val sortOrder by foodListViewModel.sortOrder.collectAsState()
    val sortDirection by foodListViewModel.sortDirection.collectAsState()
    val filter by foodListViewModel.filter.collectAsState()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.food_list_title)) },
                actions = {
                    IconButton(onClick = { foodListViewModel.goToSettings() }) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_settings),
                            contentDescription = stringResource(Res.string.food_list_content_description_settings)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { foodListViewModel.goToFoodDetail(null) }) {
                Icon(
                    painter = painterResource(Res.drawable.ic_add),
                    contentDescription = stringResource(Res.string.food_list_add_food)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            FoodListControls(
                searchQuery = searchQuery,
                onSearchQueryChange = foodListViewModel::setSearchQuery,
                sortOrder = sortOrder,
                sortDirection = sortDirection,
                onSortChange = foodListViewModel::setSort,
                currentFilter = filter,
                onFilterChange = foodListViewModel::setFilter
            )

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val state = uiState
                when {
                    state.isLoading -> {
                        CircularProgressIndicator()
                    }
                    state.hasError -> {
                        val errorMessage = stringResource(state.error!!)
                        LaunchedEffect(errorMessage) {
                            snackbarHostState.showSnackbar(message = errorMessage)
                        }
                    }
                    state.data.isNullOrEmpty() && !state.isLoading -> {
                        Text(stringResource(Res.string.food_list_empty))
                    }
                    state.data != null -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 72.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.data, key = { it.id.toString() }) { food ->
                                FoodItem(
                                    food = food,
                                    onClick = { foodListViewModel.goToFoodDetail(food.id.toString()) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodListControls(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    sortOrder: FoodSortOrder,
    sortDirection: FoodSortDirection,
    onSortChange: (FoodSortOrder, FoodSortDirection) -> Unit,
    currentFilter: FoodFilter,
    onFilterChange: (FoodFilter) -> Unit
) {
    var showSortMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(Res.string.food_list_search_placeholder)) },
            leadingIcon = { Icon(painter = painterResource(Res.drawable.ic_search), contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(painter = painterResource(Res.drawable.ic_x), contentDescription = null)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SecondaryScrollableTabRow(
                selectedTabIndex = currentFilter.ordinal,
                edgePadding = 0.dp,
                divider = {},
                indicator = {}
            ) {
                FoodFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = currentFilter == filter,
                        onClick = { onFilterChange(filter) },
                        label = {
                            Text(
                                when (filter) {
                                    FoodFilter.All -> stringResource(Res.string.food_list_filter_all)
                                    FoodFilter.Opened -> stringResource(Res.string.food_list_filter_opened)
                                    FoodFilter.Unopened -> stringResource(Res.string.food_list_filter_unopened)
                                    FoodFilter.Overdue -> stringResource(Res.string.food_list_filter_overdue)
                                }
                            )
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
        }

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterEnd
        ) {
            AssistChip(
                onClick = { showSortMenu = true },
                label = {
                    Text(
                        when (sortOrder) {
                            FoodSortOrder.Name -> if (sortDirection == FoodSortDirection.Ascending) stringResource(Res.string.food_list_sort_name_asc) else stringResource(
                                Res.string.food_list_sort_name_desc
                            )

                            FoodSortOrder.Date -> if (sortDirection == FoodSortDirection.Ascending) stringResource(Res.string.food_list_sort_date_asc) else stringResource(
                                Res.string.food_list_sort_date_desc
                            )
                        }
                    )
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(Res.drawable.ic_sort),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
            DropdownMenu(
                expanded = showSortMenu,
                onDismissRequest = { showSortMenu = false }
            ) {
                val sortOptions = listOf(
                    Triple(FoodSortOrder.Name, FoodSortDirection.Ascending, Res.string.food_list_sort_name_asc),
                    Triple(FoodSortOrder.Name, FoodSortDirection.Descending, Res.string.food_list_sort_name_desc),
                    Triple(FoodSortOrder.Date, FoodSortDirection.Ascending, Res.string.food_list_sort_date_asc),
                    Triple(FoodSortOrder.Date, FoodSortDirection.Descending, Res.string.food_list_sort_date_desc)
                )

                sortOptions.forEach { (order, direction, labelRes) ->
                    DropdownMenuItem(
                        text = { Text(stringResource(labelRes)) },
                        onClick = {
                            onSortChange(order, direction)
                            showSortMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(if (direction == FoodSortDirection.Ascending) Res.drawable.ic_arrow_upward else Res.drawable.ic_arrow_downward),
                                contentDescription = null,
                            )
                        },
                        trailingIcon = {
                            if (sortOrder == order && sortDirection == direction) {
                                Icon(painter = painterResource(Res.drawable.ic_check), contentDescription = null)
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
@Composable
fun FoodItem(
    food: Food,
    onClick: () -> Unit
) {
    val extraColors = PantryPalTheme.extraColors
    val itemColor = if (food.isOverdue) {
        if (food.isUseBy) extraColors.overdueUseBy else extraColors.overdueBestBefore
    } else {
        extraColors.notOverdue
    }
    val containerColor = if (food.isOverdue) {
        if (food.isUseBy) extraColors.overdueUseByContainer else extraColors.overdueBestBeforeContainer
    } else {
        extraColors.notOverdueContainer
    }

    BadgedBox(
        badge = {
            if (food.isOverdue) {
                Icon(
                    painter = painterResource(if (food.isUseBy) Res.drawable.ic_stop_sign else Res.drawable.ic_warning),
                    tint = itemColor,
                    contentDescription = null,
                )
            }
        }
    ) {
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, itemColor),
            colors = CardDefaults.outlinedCardColors(
                containerColor = containerColor
            )
        ) {
            ListItem(
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                leadingContent = {
                    BadgedBox(
                        badge = {
                            if (food.isOpened) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_opened_can),
                                    contentDescription = null
                                )
                            }
                        }
                    ) {
                        if (food.imagePath != null) {
                            AsyncImage(
                                model = food.imagePath,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Surface(
                                modifier = Modifier.size(64.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(Res.drawable.ic_fridge),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                },
                headlineContent = {
                    Text(
                        text = food.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1
                    )
                },
                supportingContent = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val details = mutableListOf<String>()
                        food.kiloCalories?.let { details.add("$it kcal") }
                        food.kiloJoule?.let { details.add("$it kJ") }
                        food.weightInGrams?.let { details.add("${it}g") }
                        if (details.isNotEmpty()) {
                            Text(
                                text = details.joinToString(" • "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (food.openedAt != null) {
                            Text(
                                text = "${stringResource(Res.string.food_list_opened_at)}: ${food.openedAt}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                trailingContent = {
                    if (food.bestBeforeUsedByDate != null) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = stringResource(
                                    if (food.isUseBy) Res.string.food_list_use_by_label
                                    else Res.string.food_list_best_before_label
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = itemColor
                            )
                            Text(
                                text = food.bestBeforeUsedByDate.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = itemColor
                            )
                        }
                    }
                }
            )
        }
    }
}
