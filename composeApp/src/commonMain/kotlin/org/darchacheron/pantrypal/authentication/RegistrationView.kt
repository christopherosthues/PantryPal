package org.darchacheron.pantrypal.authentication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import pantrypal.composeapp.generated.resources.Res
import pantrypal.composeapp.generated.resources.login_password
import pantrypal.composeapp.generated.resources.login_register
import pantrypal.composeapp.generated.resources.login_title
import pantrypal.composeapp.generated.resources.login_username_or_email
import pantrypal.composeapp.generated.resources.registration_email
import pantrypal.composeapp.generated.resources.registration_error_email_exists
import pantrypal.composeapp.generated.resources.registration_error_empty_email
import pantrypal.composeapp.generated.resources.registration_error_empty_password
import pantrypal.composeapp.generated.resources.registration_error_empty_username
import pantrypal.composeapp.generated.resources.registration_error_password_mismatch
import pantrypal.composeapp.generated.resources.registration_error_username_exists
import pantrypal.composeapp.generated.resources.registration_password
import pantrypal.composeapp.generated.resources.registration_remote_checkbox
import pantrypal.composeapp.generated.resources.registration_repeat_password
import pantrypal.composeapp.generated.resources.registration_server_url
import pantrypal.composeapp.generated.resources.registration_title
import pantrypal.composeapp.generated.resources.registration_username

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationView(registrationViewModel: RegistrationViewModel) {
    val uiState by registrationViewModel.registrationState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(Res.string.registration_title)) }
            )
        }
    ) { padding ->
        if (uiState.hasError) {
            val errorMessage = stringResource(uiState.error!!)
            LaunchedEffect(errorMessage) {
                snackbarHostState.showSnackbar(errorMessage)
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            val data = uiState.data ?: return@Scaffold

            val userNameError = data.userNameError?.let { stringResource(it) }

            OutlinedTextField(
                value = data.userName,
                onValueChange = { registrationViewModel.onUserNameChanged(it) },
                label = { Text(text = stringResource(Res.string.registration_username)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                isError = userNameError != null,
                supportingText = userNameError?.let { { Text(text = it) } },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            val emailError = data.emailError?.let { stringResource(it) }

            OutlinedTextField(
                value = data.email,
                onValueChange = { registrationViewModel.onEmailChanged(it) },
                label = { Text(text = stringResource(Res.string.registration_email)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                isError = emailError != null,
                supportingText = emailError?.let { { Text(text = it) } },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            val passwordError = data.passwordError?.let { stringResource(it) }

            OutlinedTextField(
                value = data.password,
                onValueChange = { registrationViewModel.onPasswordChanged(it) },
                label = { Text(text = stringResource(Res.string.registration_password)) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                isError = passwordError != null,
                supportingText = passwordError?.let { { Text(text = it) } },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            val repeatedPasswordError = data.repeatedPasswordError?.let { stringResource(it) }

            OutlinedTextField(
                value = data.repeatedPassword,
                onValueChange = { registrationViewModel.onRepeatPasswordChanged(it) },
                label = { Text(text = stringResource(Res.string.registration_repeat_password)) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                isError = repeatedPasswordError != null,
                supportingText = repeatedPasswordError?.let { { Text(text = it) } },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = data.registerRemotely,
                    onCheckedChange = { registrationViewModel.onRegisterRemotelyChanged(it) }
                )
                Text(text = stringResource(Res.string.registration_remote_checkbox))
            }

            if (data.registerRemotely) {
                Spacer(modifier = Modifier.height(16.dp))

                val serverUrlError = data.serverUrlError?.let { stringResource(it) }

                OutlinedTextField(
                    value = data.serverUrl,
                    onValueChange = { registrationViewModel.onServerUrlChanged(it) },
                    label = { Text(text = stringResource(Res.string.registration_server_url)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                    isError = serverUrlError != null,
                    supportingText = serverUrlError?.let { { Text(text = it) } },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { registrationViewModel.register() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading && data.canRegister
            ) {
                Text(text = stringResource(Res.string.login_register))
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = { registrationViewModel.goToLogin() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                Text(text = stringResource(Res.string.login_title))
            }
        }
    }
}
