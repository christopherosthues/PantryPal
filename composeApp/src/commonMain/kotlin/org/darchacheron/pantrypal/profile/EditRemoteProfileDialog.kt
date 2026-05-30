package org.darchacheron.pantrypal.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import pantrypal.composeapp.generated.resources.*

@Composable
fun EditRemoteProfileDialog(
    profile: Profile,
    onDismiss: () -> Unit,
    viewModel: EditRemoteProfileViewModel = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()
    val username by viewModel.username.collectAsState()
    val email by viewModel.email.collectAsState()
    val currentPassword by viewModel.currentPassword.collectAsState()
    val newPassword by viewModel.newPassword.collectAsState()
    val repeatNewPassword by viewModel.repeatNewPassword.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.onUsernameChanged(profile.username)
        viewModel.onEmailChanged(profile.email)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.profile_edit_remote_dialog_title)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { viewModel.onUsernameChanged(it) },
                    label = { Text(stringResource(Res.string.profile_username_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { viewModel.onEmailChanged(it) },
                    label = { Text(stringResource(Res.string.profile_email_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                if (newPassword.isNotBlank() || repeatNewPassword.isNotBlank()) {
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { viewModel.onCurrentPasswordChanged(it) },
                        label = { Text(stringResource(Res.string.profile_remote_old_password_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { viewModel.onNewPasswordChanged(it) },
                    label = { Text(stringResource(Res.string.profile_remote_new_password_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = repeatNewPassword,
                    onValueChange = { viewModel.onRepeatNewPasswordChanged(it) },
                    label = { Text(stringResource(Res.string.profile_remote_repeat_new_password_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    isError = newPassword != repeatNewPassword && repeatNewPassword.isNotEmpty()
                )

                if (uiState.error != null) {
                    Text(
                        text = stringResource(uiState.error!!),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            val isUsernameChanged = username != profile.username && username.isNotBlank()
            val isEmailChanged = email != profile.email && email.isNotBlank()
            val isPasswordChanged = newPassword.isNotBlank()
            
            val isDataChanged = isUsernameChanged || isEmailChanged || isPasswordChanged
            val isPasswordChangeValid = !isPasswordChanged || (currentPassword.isNotBlank() && newPassword == repeatNewPassword)
            
            Button(
                onClick = {
                    profile.serverUrl?.let {
                        viewModel.updateRemoteProfile(
                            serverUrl = it,
                            currentProfile = profile,
                            onDismiss = onDismiss
                        )
                    }
                },
                enabled = !uiState.isLoading && isDataChanged && isPasswordChangeValid
            ) {
                if (uiState.isLoading) {
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
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.profile_delete_cancel))
            }
        }
    )
}
