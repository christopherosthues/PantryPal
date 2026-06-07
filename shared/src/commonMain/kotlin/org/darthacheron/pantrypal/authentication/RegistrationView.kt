package org.darthacheron.pantrypal.authentication

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.darthacheron.pantrypal.ui.PantryPalTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import pantrypal.shared.generated.resources.Res
import pantrypal.shared.generated.resources.ic_fridge
import pantrypal.shared.generated.resources.login_stay_logged_in
import pantrypal.shared.generated.resources.registration_email
import pantrypal.shared.generated.resources.registration_login
import pantrypal.shared.generated.resources.registration_password
import pantrypal.shared.generated.resources.registration_register
import pantrypal.shared.generated.resources.registration_register_or_login
import pantrypal.shared.generated.resources.registration_repeat_password
import pantrypal.shared.generated.resources.registration_title
import pantrypal.shared.generated.resources.registration_username

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationView(registrationViewModel: RegistrationViewModel) {
    val uiState by registrationViewModel.registrationState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    if (!uiState.isLoading) {
                        Text(text = stringResource(Res.string.registration_title))
                    }
                }
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
            modifier = Modifier.fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.Center
        ) {
            val data = uiState.data ?: return@Scaffold

            Image(
                painter = painterResource(Res.drawable.ic_fridge),
                contentDescription = null,
                colorFilter = ColorFilter.tint(PantryPalTheme.extraColors.leftCardBackgroundTo),
                modifier = Modifier.size(128.dp).align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(64.dp))

            OutlinedTextField(
                value = data.userName,
                onValueChange = { registrationViewModel.onUserNameChanged(it) },
                label = { Text(text = stringResource(Res.string.registration_username)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                isError = data.userNameError != null,
                supportingText = data.userNameError?.let { { Text(text = stringResource(it)) } },
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = data.email,
                onValueChange = { registrationViewModel.onEmailChanged(it) },
                label = { Text(text = stringResource(Res.string.registration_email)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                isError = data.emailError != null,
                supportingText = data.emailError?.let { { Text(text = stringResource(it)) } },
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = data.password,
                onValueChange = { registrationViewModel.onPasswordChanged(it) },
                label = { Text(text = stringResource(Res.string.registration_password)) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                isError = data.passwordError != null,
                supportingText = data.passwordError?.let { { Text(text = stringResource(it)) } },
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = data.repeatedPassword,
                onValueChange = { registrationViewModel.onRepeatPasswordChanged(it) },
                label = { Text(text = stringResource(Res.string.registration_repeat_password)) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                isError = data.repeatedPasswordError != null,
                supportingText = data.repeatedPasswordError?.let { { Text(text = stringResource(it)) } },
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = data.stayLoggedIn,
                    onCheckedChange = { registrationViewModel.onStayLoggedInChanged(it) }
                )
                Text(
                    text = stringResource(Res.string.login_stay_logged_in),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { registrationViewModel.register() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading && data.canRegister
            ) {
                Text(text = stringResource(Res.string.registration_register))
            }

            Text(
                text = stringResource(Res.string.registration_register_or_login),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            OutlinedButton(
                onClick = { registrationViewModel.goToLogin() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                Text(text = stringResource(Res.string.registration_login))
            }
        }
    }
}
