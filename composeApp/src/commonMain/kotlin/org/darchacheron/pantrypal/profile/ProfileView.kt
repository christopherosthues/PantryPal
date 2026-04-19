package org.darchacheron.pantrypal.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
    profileViewModel: ProfileViewModel = koinInject(),
    remoteLoginViewModel: RemoteLoginViewModel = koinInject(),
    onGoToSettings: () -> Unit = {}
) {
    val uiState by profileViewModel.uiState.collectAsState()
    val profileValidationState by profileViewModel.profileValidationState.collectAsState()
    val passwordChangeState by profileViewModel.passwordChangeState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

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
                    onClick = { profileViewModel.showDeleteDialog() },
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
                    onClick = { profileViewModel.logout() },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_logout),
                        contentDescription = stringResource(Res.string.profile_content_description_logout)
                    )
                }

                FloatingActionButton(
                    onClick = { profileViewModel.saveProfile() },
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
                            onValueChange = { profileViewModel.updateUsername(it) },
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
                            onValueChange = { profileViewModel.updateEmail(it) },
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

                        RemoteProfileSection(profileViewModel, remoteLoginViewModel, profile)

                        ChangeLocalPasswordSection(profileViewModel)

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

    val showDeleteDialog by profileViewModel.showDeleteDialog.collectAsState()
    if (showDeleteDialog) {
        val canDeleteRemote = profileViewModel.canDeleteRemote()
        var deleteRemote by remember { mutableStateOf(canDeleteRemote) }
        AlertDialog(
            onDismissRequest = { profileViewModel.dismissDeleteDialog() },
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
                TextButton(onClick = { profileViewModel.deleteProfile(deleteRemote) }) {
                    Text(stringResource(Res.string.profile_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { profileViewModel.dismissDeleteDialog() }) {
                    Text(stringResource(Res.string.profile_delete_cancel))
                }
            }
        )
    }

    val showRemoteProfileDialog by profileViewModel.showRemoteProfileDialog.collectAsState()
    if (showRemoteProfileDialog) {
        val remoteProfileEditState by profileViewModel.remoteProfileEditState.collectAsState()
        val profile = uiState.data
        if (profile != null) {
            var username by remember { mutableStateOf(profile.username) }
            var email by remember { mutableStateOf(profile.email) }
            var oldPassword by remember { mutableStateOf("") }
            var newPassword by remember { mutableStateOf("") }
            var repeatNewPassword by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { profileViewModel.dismissRemoteProfileDialog() },
                title = { Text(stringResource(Res.string.profile_edit_remote_dialog_title)) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text(stringResource(Res.string.profile_username_label)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text(stringResource(Res.string.profile_email_label)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        OutlinedTextField(
                            value = oldPassword,
                            onValueChange = { oldPassword = it },
                            label = { Text(stringResource(Res.string.profile_remote_old_password_label)) },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = { Text(stringResource(Res.string.profile_remote_new_password_label)) },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = repeatNewPassword,
                            onValueChange = { repeatNewPassword = it },
                            label = { Text(stringResource(Res.string.profile_remote_repeat_new_password_label)) },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            isError = newPassword != repeatNewPassword && repeatNewPassword.isNotEmpty()
                        )
                        
                        if (remoteProfileEditState.error != null) {
                            Text(
                                text = stringResource(remoteProfileEditState.error!!),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            profileViewModel.updateRemoteProfile(
                                username = username,
                                email = email,
                                newPassword = newPassword.takeIf { it.isNotBlank() },
                                currentPassword = oldPassword
                            )
                        },
                        enabled = !remoteProfileEditState.isLoading && 
                                 username.isNotBlank() && 
                                 email.isNotBlank() && 
                                 oldPassword.isNotBlank() &&
                                 (newPassword.isEmpty() || newPassword == repeatNewPassword)
                    ) {
                        if (remoteProfileEditState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(stringResource(Res.string.profile_save_button))
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { profileViewModel.dismissRemoteProfileDialog() }) {
                        Text(stringResource(Res.string.profile_delete_cancel))
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
@Composable
private fun RemoteProfileSection(
    profileViewModel: ProfileViewModel,
    remoteLoginViewModel: RemoteLoginViewModel,
    profile: Profile) {
    val isLoggedInRemotely by profileViewModel.isLoggedInRemotely.collectAsState()
    val persistedServerUrl by profileViewModel.persistedServerUrl.collectAsState()
    val profileValidationState by profileViewModel.profileValidationState.collectAsState()
    var showEnableSyncDialog by remember { mutableStateOf(false) }

    Text(
        text = stringResource(Res.string.profile_synchronization_title),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 8.dp)
    )

    OutlinedTextField(
        value = profile.serverUrl ?: "",
        onValueChange = { profileViewModel.updateServerUrl(it) },
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(Res.string.profile_remote_logged_in),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            TextButton(onClick = { profileViewModel.logoutRemote() }) {
                Text(stringResource(Res.string.profile_remote_logout_button))
            }
        }
        TextButton(
            onClick = { profileViewModel.showRemoteProfileDialog() },
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Text(stringResource(Res.string.profile_edit_remote_button))
        }
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

    if (showEnableSyncDialog) {
        LaunchedEffect(showEnableSyncDialog) {
            remoteLoginViewModel.onUsernameChanged(profile.username)
            remoteLoginViewModel.onEmailChanged(profile.email)
            profile.serverUrl?.let { remoteLoginViewModel.onServerUrlChanged(it) }
        }

        RemoteLoginDialog(
            viewModel = remoteLoginViewModel,
            onDismiss = { showEnableSyncDialog = false },
            onLoginSuccess = {
                showEnableSyncDialog = false
                profileViewModel.loadProfile()
            }
        )
    }
}


@Composable
private fun ChangeLocalPasswordSection(profileViewModel: ProfileViewModel) {
    val passwordValidationState by profileViewModel.passwordValidationState.collectAsState()
    val passwordChangeState by profileViewModel.passwordChangeState.collectAsState()

    Text(
        text = stringResource(Res.string.profile_change_password_title),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 8.dp)
    )

    OutlinedTextField(
        value = passwordChangeState.data?.current ?: "",
        onValueChange = { profileViewModel.onCurrentPasswordChanged(it) },
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
        onValueChange = { profileViewModel.onNewPasswordChanged(it) },
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
        onValueChange = { profileViewModel.onRepeatPasswordChanged(it) },
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
            profileViewModel.changePassword()
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(stringResource(Res.string.profile_change_password_title))
    }
}
