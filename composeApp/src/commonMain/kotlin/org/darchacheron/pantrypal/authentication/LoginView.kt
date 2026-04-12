package org.darchacheron.pantrypal.authentication

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.darchacheron.pantrypal.ui.PantryPalTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import pantrypal.composeapp.generated.resources.Res
import pantrypal.composeapp.generated.resources.ic_fridge
import pantrypal.composeapp.generated.resources.login_login
import pantrypal.composeapp.generated.resources.login_login_or_register
import pantrypal.composeapp.generated.resources.login_password
import pantrypal.composeapp.generated.resources.login_register
import pantrypal.composeapp.generated.resources.login_title
import pantrypal.composeapp.generated.resources.login_username_or_email

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LoginView(loginViewModel: LoginViewModel) {
    val uiState by loginViewModel.loginState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    if (!uiState.isLoading) {
                        Text(text = stringResource(Res.string.login_title))
                    }
                },
            )
        },
        floatingActionButton = {
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

            Image(
                painter = painterResource(Res.drawable.ic_fridge),
                contentDescription = null,
                colorFilter = ColorFilter.tint(PantryPalTheme.extraColors.leftCardBackgroundTo),
                modifier = Modifier.size(128.dp).align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(64.dp))

            OutlinedTextField(
                value = data.username,
                onValueChange = { loginViewModel.onUsernameChanged(it) },
                label = { Text(text = stringResource(Res.string.login_username_or_email)) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                isError = data.usernameError != null,
                supportingText = {
                    data.usernameError?.let { Text(stringResource(it)) }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = data.password,
                onValueChange = { loginViewModel.onPasswordChanged(it) },
                label = { Text(text = stringResource(Res.string.login_password)) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = if (data.loginRemotely) ImeAction.Next else ImeAction.Done
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                isError = data.passwordError != null,
                supportingText = {
                    data.passwordError?.let { Text(stringResource(it)) }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    coroutineScope.launch { loginViewModel.login() }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(Res.string.login_login))
            }

            Text(
                text = stringResource(Res.string.login_login_or_register),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            OutlinedButton(
                onClick = { loginViewModel.openRegister() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(Res.string.login_register))
            }
        }
    }
}
