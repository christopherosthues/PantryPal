package org.darchacheron.pantrypal.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.darchacheron.pantrypal.authentication.RemoteAccountLinkDialog
import org.darchacheron.pantrypal.authentication.RemoteAccountLinkViewModel
import org.darchacheron.pantrypal.authentication.RemoteLoginDialog
import org.darchacheron.pantrypal.authentication.RemoteLoginViewModel
import org.darchacheron.pantrypal.settings.DataSynchronization
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import pantrypal.composeapp.generated.resources.Res
import pantrypal.composeapp.generated.resources.arrow_drop_down
import pantrypal.composeapp.generated.resources.ic_check_circle_outline
import pantrypal.composeapp.generated.resources.ic_delete
import pantrypal.composeapp.generated.resources.ic_error_outline
import pantrypal.composeapp.generated.resources.ic_logout
import pantrypal.composeapp.generated.resources.ic_profile
import pantrypal.composeapp.generated.resources.ic_save
import pantrypal.composeapp.generated.resources.ic_settings
import pantrypal.composeapp.generated.resources.ic_sync
import pantrypal.composeapp.generated.resources.ic_sync_disabled
import pantrypal.composeapp.generated.resources.profile_change_password_title
import pantrypal.composeapp.generated.resources.profile_content_description_delete
import pantrypal.composeapp.generated.resources.profile_content_description_logout
import pantrypal.composeapp.generated.resources.profile_content_description_save
import pantrypal.composeapp.generated.resources.profile_current_password_label
import pantrypal.composeapp.generated.resources.profile_delete_cancel
import pantrypal.composeapp.generated.resources.profile_delete_confirm
import pantrypal.composeapp.generated.resources.profile_delete_dialog_message
import pantrypal.composeapp.generated.resources.profile_delete_dialog_title
import pantrypal.composeapp.generated.resources.profile_delete_remote_button
import pantrypal.composeapp.generated.resources.profile_delete_remote_confirm
import pantrypal.composeapp.generated.resources.profile_delete_remote_credentials_message
import pantrypal.composeapp.generated.resources.profile_delete_remote_dialog_message
import pantrypal.composeapp.generated.resources.profile_delete_remote_dialog_title
import pantrypal.composeapp.generated.resources.profile_edit_remote_button
import pantrypal.composeapp.generated.resources.profile_email_label
import pantrypal.composeapp.generated.resources.profile_enable_sync_button
import pantrypal.composeapp.generated.resources.profile_local_login_only
import pantrypal.composeapp.generated.resources.profile_local_only_no_server
import pantrypal.composeapp.generated.resources.profile_new_password_label
import pantrypal.composeapp.generated.resources.profile_pending_sync
import pantrypal.composeapp.generated.resources.profile_personal_information
import pantrypal.composeapp.generated.resources.profile_remote_logged_in
import pantrypal.composeapp.generated.resources.profile_remote_login_button
import pantrypal.composeapp.generated.resources.profile_remote_logout_button
import pantrypal.composeapp.generated.resources.profile_repeat_new_password_label
import pantrypal.composeapp.generated.resources.profile_server_url_label
import pantrypal.composeapp.generated.resources.profile_synchronization_title
import pantrypal.composeapp.generated.resources.profile_test_connection_button
import pantrypal.composeapp.generated.resources.profile_testing_connection
import pantrypal.composeapp.generated.resources.profile_connection_success
import pantrypal.composeapp.generated.resources.profile_connection_error
import pantrypal.composeapp.generated.resources.profile_title
import pantrypal.composeapp.generated.resources.profile_username_label
import pantrypal.composeapp.generated.resources.remote_login_password_label
import pantrypal.composeapp.generated.resources.remote_login_username_label
import pantrypal.composeapp.generated.resources.profile_description_sync
import pantrypal.composeapp.generated.resources.profile_default_data_synchronization
import pantrypal.composeapp.generated.resources.profile_select_data_synchronization
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)
@Composable
fun ProfileView(
    profileViewModel: ProfileViewModel = koinInject(),
    remoteLoginViewModel: RemoteLoginViewModel = koinInject(),
    remoteAccountLinkViewModel: RemoteAccountLinkViewModel = koinInject(),
    onGoToSettings: () -> Unit = {}
) {
    val uiState by profileViewModel.uiState.collectAsState()
    val profileValidationState by profileViewModel.profileValidationState.collectAsState()
    val passwordChangeState by profileViewModel.passwordChangeState.collectAsState()
    val isLoggedInRemotely by profileViewModel.isLoggedInRemotely.collectAsState()
    val isSyncing by profileViewModel.isSyncing.collectAsState()
    val showLoginDialog by profileViewModel.showLoginDialog.collectAsState()
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

    if (showLoginDialog) {
        LaunchedEffect(showLoginDialog) {
            val profile = uiState.data
            if (profile != null) {
                remoteLoginViewModel.onUsernameChanged(profile.username)
                profile.serverUrl?.let { remoteLoginViewModel.setServerUrl(it) }
            }
        }

        RemoteLoginDialog(
            viewModel = remoteLoginViewModel,
            onDismiss = { profileViewModel.onDismissLoginDialog() },
            onLoginSuccess = { profileViewModel.onLoginSuccess() }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.profile_title)) },
                actions = {
                    val profile = uiState.data
                    if (profile != null && profile.dataSynchronization != DataSynchronization.NO_SYNCHRONIZATION) {
                        IconButton(
                            onClick = { profileViewModel.triggerSync() },
                            enabled = !isSyncing
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                val syncIcon = when {
                                    isLoggedInRemotely -> Res.drawable.ic_sync
                                    else -> Res.drawable.ic_sync_disabled
                                }
                                Icon(
                                    painter = painterResource(syncIcon),
                                    contentDescription = stringResource(Res.string.profile_description_sync)
                                )
                            }
                        }
                    }

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
                    onClick = { profileViewModel.showDeleteLocalDialog() },
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
                            .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 80.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_profile),
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        PersonalInformationSection(profile, profileViewModel, profileValidationState)

                        RemoteProfileSection(profileViewModel, remoteAccountLinkViewModel, profile)

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

    val showDeleteLocalDialog by profileViewModel.showDeleteLocalDialog.collectAsState()
    if (showDeleteLocalDialog) {
        DeleteLocalProfileDialog(profileViewModel)
    }

    val showDeleteRemoteDialog by profileViewModel.showDeleteRemoteDialog.collectAsState()
    if (showDeleteRemoteDialog) {
        DeleteRemoteProfileDialog(profileViewModel)
    }

    val showRemoteProfileDialog by profileViewModel.showRemoteProfileDialog.collectAsState()
    if (showRemoteProfileDialog) {
        val profile = uiState.data
        if (profile != null) {
            EditRemoteProfileDialog(
                profile = profile,
                onDismiss = { profileViewModel.dismissRemoteProfileDialog() }
            )
        }
    }
}

@Composable
private fun DeleteLocalProfileDialog(profileViewModel: ProfileViewModel) {
    AlertDialog(
        onDismissRequest = { profileViewModel.dismissDeleteLocalDialog() },
        title = { Text(stringResource(Res.string.profile_delete_dialog_title)) },
        text = { Text(stringResource(Res.string.profile_delete_dialog_message)) },
        confirmButton = {
            TextButton(onClick = { profileViewModel.deleteLocalProfile() }) {
                Text(stringResource(Res.string.profile_delete_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = { profileViewModel.dismissDeleteLocalDialog() }) {
                Text(stringResource(Res.string.profile_delete_cancel))
            }
        }
    )
}

@Composable
private fun DeleteRemoteProfileDialog(profileViewModel: ProfileViewModel) {
    val isLoggedInRemotely by profileViewModel.isLoggedInRemotely.collectAsState()
    val username by profileViewModel.remoteDeleteUsername.collectAsState()
    val password by profileViewModel.remoteDeletePassword.collectAsState()
    val error by profileViewModel.remoteDeleteError.collectAsState()

    AlertDialog(
        onDismissRequest = { profileViewModel.dismissDeleteRemoteDialog() },
        title = { Text(stringResource(Res.string.profile_delete_remote_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(Res.string.profile_delete_remote_dialog_message))
                
                if (!isLoggedInRemotely) {
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = stringResource(Res.string.profile_delete_remote_credentials_message),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    OutlinedTextField(
                        value = username,
                        onValueChange = { profileViewModel.onRemoteDeleteUsernameChanged(it) },
                        label = { Text(stringResource(Res.string.remote_login_username_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { profileViewModel.onRemoteDeletePasswordChanged(it) },
                        label = { Text(stringResource(Res.string.remote_login_password_label)) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { profileViewModel.deleteRemoteProfile() },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(Res.string.profile_delete_remote_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = { profileViewModel.dismissDeleteRemoteDialog() }) {
                Text(stringResource(Res.string.profile_delete_cancel))
            }
        }
    )
}

@Composable
private fun PersonalInformationSection(
    profile: Profile,
    profileViewModel: ProfileViewModel,
    profileValidationState: ProfileValidationState
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(Res.string.profile_personal_information),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

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
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
@Composable
private fun RemoteProfileSection(
    profileViewModel: ProfileViewModel,
    remoteAccountLinkViewModel: RemoteAccountLinkViewModel,
    profile: Profile
) {
    val isLoggedInRemotely by profileViewModel.isLoggedInRemotely.collectAsState()
    val persistedServerUrl by profileViewModel.persistedServerUrl.collectAsState()
    val profileValidationState by profileViewModel.profileValidationState.collectAsState()
    var showEnableSyncDialog by remember { mutableStateOf(false) }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(Res.string.profile_synchronization_title),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
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

            val isTestingConnection by profileViewModel.isTestingConnection.collectAsState()
            val connectionTestSuccess by profileViewModel.connectionTestSuccess.collectAsState()

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { profileViewModel.testConnection() },
                    modifier = Modifier.weight(1f),
                    enabled = !isTestingConnection && !profile.serverUrl.isNullOrBlank() && profileValidationState.serverUrlError == null
                ) {
                    if (isTestingConnection) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(Res.string.profile_testing_connection))
                    } else {
                        Text(stringResource(Res.string.profile_test_connection_button))
                    }
                }

                connectionTestSuccess?.let { success ->
                    val color =
                        if (success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    val text =
                        if (success) Res.string.profile_connection_success else Res.string.profile_connection_error
                    val icon =
                        if (success) Res.drawable.ic_check_circle_outline else Res.drawable.ic_error_outline
                    Icon(painter = painterResource(icon), contentDescription = null, tint = color)
                    Text(
                        text = stringResource(text),
                        color = color,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            HorizontalDivider()

            DataSynchronizationDropdown(
                selectedDataSynchronization = profile.dataSynchronization,
                onDataSynchronizationSelected = { profileViewModel.updateDataSynchronization(it) }
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

                TextButton(
                    onClick = { profileViewModel.showDeleteRemoteDialog() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(Res.string.profile_delete_remote_button))
                }
            } else if (profile.serverId != null) {
                Text(
                    text = stringResource(Res.string.profile_local_login_only),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )

                Button(
                    onClick = { profileViewModel.showLoginDialog() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(Res.string.profile_remote_login_button))
                }
                // TODO delete not working
                // TODO edit not working
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
        }
    }

    if (showEnableSyncDialog) {
        LaunchedEffect(showEnableSyncDialog) {
            remoteAccountLinkViewModel.onUsernameChanged(profile.username)
            remoteAccountLinkViewModel.onEmailChanged(profile.email)
            profile.serverUrl?.let { remoteAccountLinkViewModel.onServerUrlChanged(it) }
        }

        RemoteAccountLinkDialog(
            viewModel = remoteAccountLinkViewModel,
            onDismiss = { showEnableSyncDialog = false },
            onLoginSuccess = {
                showEnableSyncDialog = false
                profileViewModel.loadProfile()
            }
        )
    }
}

@Composable
private fun DataSynchronizationDropdown(
    selectedDataSynchronization: DataSynchronization,
    onDataSynchronizationSelected: (DataSynchronization) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedTextField(
            value = stringResource(selectedDataSynchronization.toStringResource()),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(Res.string.profile_default_data_synchronization)) },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = { expanded = true }) {
                    Icon(
                        painter = painterResource(Res.drawable.arrow_drop_down),
                        contentDescription = stringResource(Res.string.profile_select_data_synchronization)
                    )
                }
            }
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { expanded = true }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            DataSynchronization.entries.forEach { unit ->
                DropdownMenuItem(
                    text = { Text(stringResource(unit.toStringResource())) },
                    onClick = {
                        onDataSynchronizationSelected(unit)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ChangeLocalPasswordSection(profileViewModel: ProfileViewModel) {
    val passwordValidationState by profileViewModel.passwordValidationState.collectAsState()
    val passwordChangeState by profileViewModel.passwordChangeState.collectAsState()

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(Res.string.profile_change_password_title),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
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
    }
}
