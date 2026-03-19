package org.darchacheron.pantrypal.authentication

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun RegistrationView(registrationViewModel: RegistrationViewModel) {
    val uiState by registrationViewModel.registrationState.collectAsStateWithLifecycle()


}