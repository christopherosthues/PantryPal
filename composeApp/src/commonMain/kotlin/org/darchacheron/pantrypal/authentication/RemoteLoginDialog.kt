package org.darchacheron.pantrypal.authentication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import pantrypal.composeapp.generated.resources.Res
import pantrypal.composeapp.generated.resources.login_remote_password_label
import pantrypal.composeapp.generated.resources.login_remote_username_label
import pantrypal.composeapp.generated.resources.login_remote_username_placeholder
import pantrypal.composeapp.generated.resources.login_remotely_checkbox
import pantrypal.composeapp.generated.resources.login_use_same_credentials_checkbox
import pantrypal.composeapp.generated.resources.profile_delete_cancel
import pantrypal.composeapp.generated.resources.remote_login_button
import pantrypal.composeapp.generated.resources.remote_login_password_label
import pantrypal.composeapp.generated.resources.remote_login_server_url_label
import pantrypal.composeapp.generated.resources.remote_login_title
import pantrypal.composeapp.generated.resources.remote_login_username_label

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteLoginDialog(
    viewModel: RemoteLoginViewModel,
    onDismiss: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    val uiState by viewModel.loginState.collectAsStateWithLifecycle()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(Res.string.remote_login_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (uiState.hasError) {
                    val errorMessage = stringResource(uiState.error!!)
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                val data = uiState.data ?: return@Column

                OutlinedTextField(
                    value = data.username,
                    onValueChange = { viewModel.onUsernameChanged(it) },
                    label = { Text(text = stringResource(Res.string.remote_login_username_label)) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = data.password,
                    onValueChange = { viewModel.onPasswordChanged(it) },
                    label = { Text(text = stringResource(Res.string.remote_login_password_label)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = if (data.loginRemotely) ImeAction.Next else ImeAction.Done
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = data.loginRemotely,
                        onCheckedChange = { viewModel.onLoginRemotelyChanged(it) }
                    )
                    Text(
                        text = stringResource(Res.string.login_remotely_checkbox),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (data.loginRemotely) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = data.useSameCredentials,
                            onCheckedChange = { viewModel.onUseSameCredentialsChanged(it) }
                        )
                        Text(
                            text = stringResource(Res.string.login_use_same_credentials_checkbox),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    OutlinedTextField(
                        value = data.serverUrl,
                        onValueChange = { viewModel.onServerUrlChanged(it) },
                        label = { Text(text = stringResource(Res.string.remote_login_server_url_label)) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (!data.useSameCredentials) {
                        OutlinedTextField(
                            value = data.remoteUsername,
                            onValueChange = { viewModel.onRemoteUsernameChanged(it) },
                            label = { Text(text = stringResource(Res.string.login_remote_username_label)) },
                            placeholder = { Text(text = stringResource(Res.string.login_remote_username_placeholder)) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = data.remotePassword,
                            onValueChange = { viewModel.onRemotePasswordChanged(it) },
                            label = { Text(text = stringResource(Res.string.login_remote_password_label)) },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(Res.string.profile_delete_cancel))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.login(onSuccess = onLoginSuccess)
                            },
                            enabled = !uiState.isLoading
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text(text = stringResource(Res.string.remote_login_button))
                            }
                        }
                    }
                }
            }
        }
    }
}
