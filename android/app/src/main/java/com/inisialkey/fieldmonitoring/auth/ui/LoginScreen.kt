package com.inisialkey.fieldmonitoring.auth.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldLabelPosition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.inisialkey.fieldmonitoring.R
import com.inisialkey.fieldmonitoring.auth.domain.Role
import com.inisialkey.fieldmonitoring.core.ui.components.AppCard
import com.inisialkey.fieldmonitoring.core.ui.components.FieldError
import com.inisialkey.fieldmonitoring.core.ui.components.HeroHeader
import com.inisialkey.fieldmonitoring.core.ui.components.HeroText
import com.inisialkey.fieldmonitoring.core.ui.components.contentWidth

@Composable
fun LoginScreen(onSignedIn: (Role) -> Unit, onBack: () -> Unit, viewModel: LoginViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val online by viewModel.isOnline.collectAsStateWithLifecycle()
    val reauth = state.reauthUsername != null

    LaunchedEffect(state.signedInRole) { state.signedInRole?.let(onSignedIn) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .verticalScroll(rememberScrollState()),
    ) {
        HeroHeader {
            if (reauth) {
                IconButton(onClick = onBack, modifier = Modifier.offset(x = (-12).dp)) {
                    Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.action_back), tint = Color.White)
                }
            }
            Surface(
                shape = MaterialTheme.shapes.large,
                color = Color.White,
                modifier = Modifier.padding(top = if (reauth) 8.dp else 32.dp),
            ) {
                Image(
                    painterResource(R.drawable.logo),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                        .height(40.dp),
                )
            }
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(top = 28.dp),
            )
            HeroText(
                stringResource(if (reauth) R.string.login_reauth_subtitle else R.string.login_subtitle),
                modifier = Modifier.padding(top = 6.dp, bottom = 40.dp),
            )
        }

        AppCard(
            modifier = Modifier
                .offset(y = (-28).dp)
                .contentWidth()
                .padding(horizontal = 16.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.login_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    state = viewModel.username,
                    readOnly = reauth,
                    labelPosition = TextFieldLabelPosition.Above(),
                    label = { Text(stringResource(R.string.login_username)) },
                    leadingIcon = { Icon(painterResource(R.drawable.ic_person), contentDescription = null) },
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
                    leadingIcon = { Icon(painterResource(R.drawable.ic_lock), contentDescription = null) },
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
                        .height(56.dp),
                ) {
                    if (state.loading) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text(stringResource(R.string.action_sign_in), style = MaterialTheme.typography.titleSmall)
                    }
                }
                if (!online) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            Modifier
                                .size(8.dp)
                                .background(MaterialTheme.colorScheme.error, CircleShape),
                        )
                        Text(
                            stringResource(R.string.login_offline),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        Box(Modifier.navigationBarsPadding())
    }
}
