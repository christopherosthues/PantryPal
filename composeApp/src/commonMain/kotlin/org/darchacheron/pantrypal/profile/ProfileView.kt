package org.darchacheron.pantrypal.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import pantrypal.composeapp.generated.resources.ic_save
import pantrypal.composeapp.generated.resources.ic_settings
import pantrypal.composeapp.generated.resources.profile_delete_button
import pantrypal.composeapp.generated.resources.profile_email_label
import pantrypal.composeapp.generated.resources.profile_logout_button
import pantrypal.composeapp.generated.resources.profile_save_button
import pantrypal.composeapp.generated.resources.profile_title
import pantrypal.composeapp.generated.resources.profile_username_label
import pantrypal.composeapp.generated.resources.profile_password_label

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileView(
    viewModel: ProfileViewModel = koinInject(),
    onGoToSettings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
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
                    val profile = state.data!!
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

                        Button(
                            onClick = { viewModel.saveProfile(password.takeIf { it.isNotBlank() }) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(painterResource(Res.drawable.ic_save), contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(Res.string.profile_save_button))
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        OutlinedButton(
                            onClick = { viewModel.logout() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            // Using ic_delete as a placeholder if ic_logout is missing
                            Icon(painterResource(Res.drawable.ic_delete), contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(Res.string.profile_logout_button))
                        }

                        OutlinedButton(
                            onClick = { viewModel.deleteProfile() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(painterResource(Res.drawable.ic_delete), contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(Res.string.profile_delete_button))
                        }
                    }
                }
            }
        }
    }
}
