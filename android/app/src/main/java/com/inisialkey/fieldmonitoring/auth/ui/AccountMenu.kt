package com.inisialkey.fieldmonitoring.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.inisialkey.fieldmonitoring.R
import com.inisialkey.fieldmonitoring.auth.data.AuthRepository
import com.inisialkey.fieldmonitoring.auth.data.SessionStore
import com.inisialkey.fieldmonitoring.auth.domain.Role
import com.inisialkey.fieldmonitoring.core.ui.components.Avatar
import com.inisialkey.fieldmonitoring.core.ui.components.HeroText
import com.inisialkey.fieldmonitoring.core.ui.components.greeting
import com.inisialkey.fieldmonitoring.core.ui.components.initials
import com.inisialkey.fieldmonitoring.report.data.PendingCounts
import com.inisialkey.fieldmonitoring.report.data.ReportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val sessionStore: SessionStore,
    private val reports: ReportRepository,
    private val auth: AuthRepository,
) : ViewModel() {

    val displayName: String = sessionStore.session.value?.displayName.orEmpty()
    val role: Role? = sessionStore.session.value?.role

    private val _logoutPrompt = MutableStateFlow<PendingCounts?>(null)
    val logoutPrompt: StateFlow<PendingCounts?> = _logoutPrompt.asStateFlow()

    private val _signedOut = MutableStateFlow(false)
    val signedOut: StateFlow<Boolean> = _signedOut.asStateFlow()

    fun requestLogout() {
        val ownerId = sessionStore.session.value?.userId ?: return
        viewModelScope.launch { _logoutPrompt.value = reports.pendingCounts(ownerId) }
    }

    fun dismissLogout() {
        _logoutPrompt.value = null
    }

    fun confirmLogout() {
        _logoutPrompt.value = null
        viewModelScope.launch {
            auth.logout()
            _signedOut.value = true
        }
    }
}

@Composable
fun AccountHeader(onSignedOut: () -> Unit, modifier: Modifier = Modifier, viewModel: AccountViewModel = hiltViewModel()) {
    var expanded by remember { mutableStateOf(false) }
    val prompt by viewModel.logoutPrompt.collectAsStateWithLifecycle()
    val signedOut by viewModel.signedOut.collectAsStateWithLifecycle()
    val menuLabel = stringResource(R.string.account_menu)
    val roleLabel = stringResource(if (viewModel.role == Role.EVALUATOR) R.string.role_evaluator else R.string.role_field_officer)

    LaunchedEffect(signedOut) { if (signedOut) onSignedOut() }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            HeroText(greeting())
            Text(
                viewModel.displayName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box {
            Surface(
                onClick = { expanded = true },
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.18f),
                contentColor = Color.White,
                modifier = Modifier
                    .size(48.dp)
                    .semantics { contentDescription = menuLabel },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(initials(viewModel.displayName), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Avatar(viewModel.displayName)
                    Column {
                        Text(viewModel.displayName, style = MaterialTheme.typography.titleSmall)
                        Text(roleLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.logout)) },
                    leadingIcon = { Icon(painterResource(R.drawable.ic_logout), contentDescription = null) },
                    onClick = {
                        expanded = false
                        viewModel.requestLogout()
                    },
                )
            }
        }
    }

    prompt?.let { counts ->
        AlertDialog(
            onDismissRequest = viewModel::dismissLogout,
            title = { Text(stringResource(R.string.logout_title)) },
            text = if (counts.unsent + counts.drafts > 0) {
                { Text(stringResource(R.string.logout_pending, counts.unsent, counts.drafts)) }
            } else {
                null
            },
            confirmButton = { TextButton(onClick = viewModel::confirmLogout) { Text(stringResource(R.string.logout)) } },
            dismissButton = { TextButton(onClick = viewModel::dismissLogout) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}
