package com.inisialkey.fieldmonitoring.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldLabelPosition
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.inisialkey.fieldmonitoring.R
import com.inisialkey.fieldmonitoring.auth.domain.Role
import com.inisialkey.fieldmonitoring.core.ui.components.BackButton
import com.inisialkey.fieldmonitoring.core.ui.components.FieldError
import com.inisialkey.fieldmonitoring.core.ui.components.contentWidth

@Composable
fun LoginScreen(onSignedIn: (Role) -> Unit, onBack: () -> Unit, viewModel: LoginViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val online by viewModel.isOnline.collectAsStateWithLifecycle()
    val reauth = state.reauthUsername != null

    LaunchedEffect(state.signedInRole) { state.signedInRole?.let(onSignedIn) }

    Scaffold(
        topBar = {
            if (reauth) TopAppBar(title = {}, navigationIcon = { BackButton(onBack) })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .contentWidth()
                .padding(horizontal = 16.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                painterResource(R.drawable.ic_location_on),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp),
            )
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall)
            Text(
                stringResource(if (reauth) R.string.login_reauth_subtitle else R.string.login_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                state = viewModel.username,
                readOnly = reauth,
                labelPosition = TextFieldLabelPosition.Above(),
                label = { Text(stringResource(R.string.login_username)) },
                lineLimits = TextFieldLineLimits.SingleLine,
                keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Next),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentType = ContentType.Username },
            )
            OutlinedSecureTextField(
                state = viewModel.password,
                labelPosition = TextFieldLabelPosition.Above(),
                label = { Text(stringResource(R.string.login_password)) },
                textObfuscationMode = if (state.passwordVisible) TextObfuscationMode.Visible else TextObfuscationMode.RevealLastTyped,
                trailingIcon = {
                    IconButton(onClick = viewModel::togglePasswordVisibility) {
                        Icon(
                            painterResource(if (state.passwordVisible) R.drawable.ic_visibility_off else R.drawable.ic_visibility),
                            contentDescription = stringResource(
                                if (state.passwordVisible) R.string.login_hide_password else R.string.login_show_password,
                            ),
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                onKeyboardAction = { viewModel.signIn() },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentType = ContentType.Password },
            )
            state.error?.let { FieldError(stringResource(it, state.errorArg)) }
            Button(
                onClick = viewModel::signIn,
                enabled = online && !state.loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                if (state.loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.action_sign_in))
                }
            }
            Text(
                stringResource(if (online) R.string.login_needs_internet else R.string.login_offline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
