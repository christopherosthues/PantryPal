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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import pantrypal.composeapp.generated.resources.*

@Composable
fun RemoteLoginDialog(
    viewModel: RemoteLoginViewModel,
    onDismiss: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    val uiState by viewModel.loginState.collectAsStateWithLifecycle()
    val data = uiState.data ?: return

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

                TabRow(selectedTabIndex = if (data.isCreatingNew) 1 else 0) {
                    Tab(
                        selected = !data.isCreatingNew,
                        onClick = { viewModel.setIsCreatingNew(false) },
                        text = { Text(stringResource(Res.string.profile_link_account_title)) }
                    )
                    Tab(
                        selected = data.isCreatingNew,
                        onClick = { viewModel.setIsCreatingNew(true) },
                        text = { Text(stringResource(Res.string.profile_create_account_title)) }
                    )
                }

                if (uiState.hasError) {
                    val errorMessage = stringResource(uiState.error!!)
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                OutlinedTextField(
                    value = data.serverUrl,
                    onValueChange = { },
                    label = { Text(stringResource(Res.string.profile_server_url_label)) },
                    readOnly = true,
                    enabled = false,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = data.username,
                    onValueChange = { viewModel.onUsernameChanged(it) },
                    label = { Text(stringResource(Res.string.registration_username)) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    isError = data.usernameError != null,
                    supportingText = { data.usernameError?.let { Text(stringResource(it)) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (data.isCreatingNew) {
                    OutlinedTextField(
                        value = data.email,
                        onValueChange = { viewModel.onEmailChanged(it) },
                        label = { Text(stringResource(Res.string.registration_email)) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        isError = data.emailError != null,
                        supportingText = { data.emailError?.let { Text(stringResource(it)) } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = data.password,
                    onValueChange = { viewModel.onPasswordChanged(it) },
                    label = { Text(stringResource(Res.string.registration_password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = if (data.isCreatingNew) ImeAction.Next else ImeAction.Done
                    ),
                    isError = data.passwordError != null,
                    supportingText = { data.passwordError?.let { Text(stringResource(it)) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (data.isCreatingNew) {
                    OutlinedTextField(
                        value = data.repeatedPassword,
                        onValueChange = { viewModel.onRepeatedPasswordChanged(it) },
                        label = { Text(stringResource(Res.string.registration_repeat_password)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        isError = data.repeatedPasswordError != null,
                        supportingText = { data.repeatedPasswordError?.let { Text(stringResource(it)) } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(Res.string.profile_delete_cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { viewModel.submit(onSuccess = onLoginSuccess) },
                        enabled = !uiState.isLoading && data.canSubmit
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(
                                if (data.isCreatingNew) stringResource(Res.string.profile_create_account_button)
                                else stringResource(Res.string.profile_link_account_button)
                            )
                        }
                    }
                }
            }
        }
    }
}
