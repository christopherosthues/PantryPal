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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import org.darchacheron.pantrypal.authentication.LoginViewModel
import org.darchacheron.pantrypal.authentication.RemoteLoginDialog
import org.darchacheron.pantrypal.authentication.RemoteLoginViewModel
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import pantrypal.composeapp.generated.resources.*
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)
@Composable
fun ProfileView(
    viewModel: ProfileViewModel = koinInject(),
    remoteLoginViewModel: RemoteLoginViewModel = koinInject(),
    onGoToSettings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val profileValidationState by viewModel.profileValidationState.collectAsState()
    val passwordChangeState by viewModel.passwordChangeState.collectAsState()
    val passwordValidationState by viewModel.passwordValidationState.collectAsState()
    val isLoggedInRemotely by viewModel.isLoggedInRemotely.collectAsState()
    val persistedServerUrl by viewModel.persistedServerUrl.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showEnableSyncDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { errorResource ->
            val message = getString(errorResource)
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(passwordChangeState.error) {
        passwordChangeState.error?.let { errorResource ->
            val message = getString(errorResource)
            snackbarHostState.showSnackbar(message)
        }
    }

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
                    onClick = { viewModel.saveProfile() },
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
                .padding(top = padding.calculateTopPadding()),
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
                            .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 72.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = profile.username,
                            onValueChange = { viewModel.updateUsername(it) },
                            label = { Text(stringResource(Res.string.profile_username_label)) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            isError = profileValidationState.usernameError != null,
                            supportingText = {
                                profileValidationState.usernameError?.let {
                                    Text(stringResource(it))
                                }
                            }
                        )

                        OutlinedTextField(
                            value = profile.email,
                            onValueChange = { viewModel.updateEmail(it) },
                            label = { Text(stringResource(Res.string.profile_email_label)) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            isError = profileValidationState.emailError != null,
                            supportingText = {
                                profileValidationState.emailError?.let {
                                    Text(stringResource(it))
                                }
                            }
                        )

                        Text(
                            text = stringResource(Res.string.profile_synchronization_title),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        OutlinedTextField(
                            value = profile.serverUrl ?: "",
                            onValueChange = { viewModel.updateServerUrl(it) },
                            label = { Text(stringResource(Res.string.profile_server_url_label)) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            isError = profileValidationState.serverUrlError != null,
                            supportingText = {
                                profileValidationState.serverUrlError?.let {
                                    Text(stringResource(it))
                                }
                            }
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

                            val isServerUrlPersisted = !profile.serverUrl.isNullOrBlank() &&
                                    profile.serverUrl == persistedServerUrl &&
                                    profileValidationState.serverUrlError == null

                            Button(
                                onClick = { showEnableSyncDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = isServerUrlPersisted
                            ) {
                                Text(stringResource(Res.string.profile_enable_sync_button))
                            }
                        }

                        Text(
                            text = stringResource(Res.string.profile_change_password_title),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        OutlinedTextField(
                            value = passwordChangeState.data?.current ?: "",
                            onValueChange = { viewModel.onCurrentPasswordChanged(it) },
                            label = { Text(stringResource(Res.string.profile_current_password_label)) },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            isError = passwordValidationState.currentPasswordError != null,
                            supportingText = {
                                passwordValidationState.currentPasswordError?.let {
                                    Text(stringResource(it))
                                }
                            }
                        )
                        OutlinedTextField(
                            value = passwordChangeState.data?.new ?: "",
                            onValueChange = { viewModel.onNewPasswordChanged(it) },
                            label = { Text(stringResource(Res.string.profile_new_password_label)) },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            isError = passwordValidationState.newPasswordError != null,
                            supportingText = {
                                passwordValidationState.newPasswordError?.let {
                                    Text(stringResource(it))
                                }
                            }
                        )
                        OutlinedTextField(
                            value = passwordChangeState.data?.repeat ?: "",
                            onValueChange = { viewModel.onRepeatPasswordChanged(it) },
                            label = { Text(stringResource(Res.string.profile_repeat_new_password_label)) },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            isError = passwordValidationState.repeatPasswordError != null,
                            supportingText = {
                                passwordValidationState.repeatPasswordError?.let {
                                    Text(stringResource(it))
                                }
                            }
                        )

                        Button(
                            onClick = {
                                viewModel.changePassword()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(Res.string.profile_change_password_title))
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

    if (showEnableSyncDialog) {
        val currentProfile = uiState.data
        LaunchedEffect(showEnableSyncDialog) {
            if (currentProfile != null) {
                remoteLoginViewModel.onUsernameChanged(currentProfile.username)
                remoteLoginViewModel.onEmailChanged(currentProfile.email)
                currentProfile.serverUrl?.let { remoteLoginViewModel.onServerUrlChanged(it) }
            }
        }

        RemoteLoginDialog(
            viewModel = remoteLoginViewModel,
            onDismiss = { showEnableSyncDialog = false },
            onLoginSuccess = {
                showEnableSyncDialog = false
                viewModel.loadProfile()
            }
        )
    }
}
