package org.darchacheron.pantrypal.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import pantrypal.composeapp.generated.resources.Res
import pantrypal.composeapp.generated.resources.ic_delete
import pantrypal.composeapp.generated.resources.ic_logout
import pantrypal.composeapp.generated.resources.ic_save
import pantrypal.composeapp.generated.resources.ic_settings
import pantrypal.composeapp.generated.resources.profile_content_description_delete
import pantrypal.composeapp.generated.resources.profile_content_description_logout
import pantrypal.composeapp.generated.resources.profile_content_description_save
import pantrypal.composeapp.generated.resources.profile_delete_cancel
import pantrypal.composeapp.generated.resources.profile_delete_confirm
import pantrypal.composeapp.generated.resources.profile_delete_dialog_message
import pantrypal.composeapp.generated.resources.profile_delete_dialog_remote_checkbox
import pantrypal.composeapp.generated.resources.profile_delete_dialog_title
import pantrypal.composeapp.generated.resources.profile_email_label
import pantrypal.composeapp.generated.resources.profile_local_login_only
import pantrypal.composeapp.generated.resources.profile_local_only_no_server
import pantrypal.composeapp.generated.resources.profile_password_label
import pantrypal.composeapp.generated.resources.profile_pending_sync
import pantrypal.composeapp.generated.resources.profile_remote_logged_in
import pantrypal.composeapp.generated.resources.profile_title
import pantrypal.composeapp.generated.resources.profile_username_label
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)
@Composable
fun ProfileView(
    viewModel: ProfileViewModel = koinInject(),
    onGoToSettings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val isLoggedInRemotely by viewModel.isLoggedInRemotely.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var password by remember { mutableStateOf("") }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.profile_title)) },
                actions = {
                    IconButton(onClick = onGoToSettings) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_settings),
                            contentDescription = null
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FloatingActionButton(
                    onClick = { viewModel.showDeleteDialog() },
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_delete),
                        contentDescription = stringResource(Res.string.profile_content_description_delete)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                FloatingActionButton(
                    onClick = { viewModel.logout() },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_logout),
                        contentDescription = stringResource(Res.string.profile_content_description_logout)
                    )
                }

                FloatingActionButton(
                    onClick = { viewModel.saveProfile(password.takeIf { it.isNotBlank() }) },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_save),
                        contentDescription = stringResource(Res.string.profile_content_description_save)
                    )
                }
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
                state.isLoading -> CircularProgressIndicator()
                state.data != null -> {
                    val profile = state.data
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = profile.username,
                            onValueChange = { viewModel.updateUsername(it) },
                            label = { Text(stringResource(Res.string.profile_username_label)) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = profile.email,
                            onValueChange = { viewModel.updateEmail(it) },
                            label = { Text(stringResource(Res.string.profile_email_label)) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text(stringResource(Res.string.profile_password_label)) },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (isLoggedInRemotely) {
                            Text(
                                text = stringResource(Res.string.profile_remote_logged_in),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else if (profile.serverId != null) {
                            Text(
                                text = stringResource(Res.string.profile_local_login_only),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        } else {
                            Text(
                                text = stringResource(Res.string.profile_local_only_no_server),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (profile.serverId != null && (profile.lastSyncedAt == null || (profile.lastModifiedAt > profile.lastSyncedAt))) {
                            Text(
                                text = stringResource(Res.string.profile_pending_sync),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }

    val showDeleteDialog by viewModel.showDeleteDialog.collectAsState()
    if (showDeleteDialog) {
        val canDeleteRemote = viewModel.canDeleteRemote()
        var deleteRemote by remember { mutableStateOf(canDeleteRemote) }
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteDialog() },
            title = { Text(stringResource(Res.string.profile_delete_dialog_title)) },
            text = {
                Column {
                    Text(stringResource(Res.string.profile_delete_dialog_message))
                    if (canDeleteRemote) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = deleteRemote,
                                onCheckedChange = { deleteRemote = it }
                            )
                            Text(stringResource(Res.string.profile_delete_dialog_remote_checkbox))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteProfile(deleteRemote) }) {
                    Text(stringResource(Res.string.profile_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteDialog() }) {
                    Text(stringResource(Res.string.profile_delete_cancel))
                }
            }
        )
    }
}
