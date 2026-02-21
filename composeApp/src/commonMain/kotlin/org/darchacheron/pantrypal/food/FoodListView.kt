package org.darchacheron.pantrypal.food

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.food_list_title)) },
                actions = {
                    IconButton(
                        onClick = { foodListViewModel.goToSettings() }
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_settings),
                            contentDescription = null//stringResource(Res.string.play_content_description_settings)
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
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
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.data) { food ->
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

@OptIn(ExperimentalUuidApi::class)
@Composable
fun FoodItem(
    food: Food,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(food.name) },
        supportingContent = {
            Text("${food.calories} kcal | ${food.weightInGrams}g")
        },
        modifier = Modifier.clickable { onClick() }
    )
}
