package com.inisialkey.fieldmonitoring.evaluation.ui.evaluate

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldLabelPosition
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.inisialkey.fieldmonitoring.R
import com.inisialkey.fieldmonitoring.core.ui.components.BackButton
import com.inisialkey.fieldmonitoring.core.ui.components.EmptyState
import com.inisialkey.fieldmonitoring.core.ui.components.FieldError
import com.inisialkey.fieldmonitoring.core.ui.components.FullScreenPhoto
import com.inisialkey.fieldmonitoring.core.ui.components.SectionCard
import com.inisialkey.fieldmonitoring.core.ui.components.UserMessageEffect
import com.inisialkey.fieldmonitoring.core.ui.components.contentWidth
import com.inisialkey.fieldmonitoring.core.ui.theme.LocalAppColors
import com.inisialkey.fieldmonitoring.evaluation.domain.EvaluationField
import com.inisialkey.fieldmonitoring.evaluation.domain.EvaluationRules
import com.inisialkey.fieldmonitoring.evaluation.ui.queue.LoadError
import com.inisialkey.fieldmonitoring.report.domain.EvaluationResult
import com.inisialkey.fieldmonitoring.report.domain.FieldError as ReportFieldError
import com.inisialkey.fieldmonitoring.report.domain.GeoPoint
import com.inisialkey.fieldmonitoring.report.domain.Photo
import com.inisialkey.fieldmonitoring.report.ui.detail.ReportDetailSections

@Composable
fun EvaluateScreen(
    onBack: () -> Unit,
    onOpenNext: (String) -> Unit,
    onSignIn: () -> Unit,
    viewModel: EvaluateViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    var openPhoto by remember { mutableStateOf<Photo?>(null) }
    val report = state.report
    val canEvaluate = report != null && report.evaluation == null

    UserMessageEffect(viewModel.messages, snackbarHostState)
    LaunchedEffect(state.outcome) {
        when (val outcome = state.outcome) {
            is EvaluateOutcome.OpenNext -> onOpenNext(outcome.reportId)
            EvaluateOutcome.BackToQueue -> onBack()
            null -> Unit
        }
    }
    LaunchedEffect(state.submitFailed) {
        if (state.submitFailed) {
            snackbarHostState.showSnackbar(resources.getString(R.string.evaluation_failed))
            viewModel.onSubmitFailedShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (canEvaluate || report == null) R.string.evaluate_title else R.string.detail_title)) },
                navigationIcon = { BackButton(onBack) },
            )
        },
        bottomBar = {
            if (canEvaluate) {
                Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest, shadowElevation = 8.dp) {
                    Button(
                        onClick = viewModel::onSubmitClick,
                        enabled = !state.submitting,
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                            .contentWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .height(56.dp),
                    ) {
                        if (state.submitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Icon(painterResource(R.drawable.ic_send), contentDescription = null, modifier = Modifier.size(20.dp))
                            Text(
                                stringResource(R.string.submit_evaluation),
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(start = 10.dp),
                            )
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            when {
                state.loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.notFound -> EmptyState(
                    icon = R.drawable.ic_error,
                    title = stringResource(R.string.report_not_found),
                    message = "",
                    modifier = Modifier.align(Alignment.Center),
                )
                state.error == LoadError.SESSION_EXPIRED -> EmptyState(
                    icon = R.drawable.ic_lock,
                    title = stringResource(R.string.session_expired_title),
                    message = stringResource(R.string.session_expired_message),
                    actionLabel = stringResource(R.string.action_sign_in),
                    onAction = onSignIn,
                    modifier = Modifier.align(Alignment.Center),
                )
                state.error != null || report == null -> EmptyState(
                    icon = R.drawable.ic_cloud_off,
                    title = stringResource(R.string.offline_title),
                    message = stringResource(R.string.offline_message),
                    actionLabel = stringResource(R.string.action_retry),
                    onAction = viewModel::load,
                    modifier = Modifier.align(Alignment.Center),
                )
                else -> Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .contentWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    ReportDetailSections(report, forOwner = false, onOpenPhoto = { openPhoto = it })
                    report.location?.let { OpenInMapsButton(it) }
                    if (canEvaluate) {
                        SectionCard(title = stringResource(R.string.field_result), icon = R.drawable.ic_task_alt) {
                            EvaluationForm(state, viewModel)
                        }
                    }
                }
            }
        }
    }

    openPhoto?.let { FullScreenPhoto(it, onDismiss = { openPhoto = null }) }

    if (state.confirming) {
        AlertDialog(
            onDismissRequest = viewModel::onConfirmDismissed,
            title = { Text(stringResource(R.string.dialog_evaluation_title)) },
            text = { Text(stringResource(R.string.dialog_evaluation_message)) },
            confirmButton = { TextButton(onClick = viewModel::onConfirmed) { Text(stringResource(R.string.dialog_send)) } },
            dismissButton = { TextButton(onClick = viewModel::onConfirmDismissed) { Text(stringResource(R.string.action_cancel)) } },
        )
    }

    if (state.alreadyEvaluated) {
        AlertDialog(
            onDismissRequest = viewModel::onViewExistingResult,
            title = { Text(stringResource(R.string.dialog_already_evaluated_title)) },
            text = { Text(stringResource(R.string.dialog_already_evaluated_message)) },
            confirmButton = {
                TextButton(onClick = viewModel::onViewExistingResult) { Text(stringResource(R.string.action_view_result)) }
            },
        )
    }
}

@Composable
private fun EvaluationForm(state: EvaluateUiState, viewModel: EvaluateViewModel) {
    val app = LocalAppColors.current
    val colors = MaterialTheme.colorScheme
    val notesError = state.errors[EvaluationField.NOTES]

    Row(modifier = Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        DecisionCard(
            selected = state.result == EvaluationResult.VALID,
            title = stringResource(R.string.evaluation_valid),
            hint = stringResource(R.string.decision_valid_hint),
            icon = R.drawable.ic_check_circle,
            container = app.successContainer,
            content = app.onSuccessContainer,
            accent = app.success,
            onClick = { viewModel.onResultSelected(EvaluationResult.VALID) },
            modifier = Modifier.weight(1f),
        )
        DecisionCard(
            selected = state.result == EvaluationResult.INVALID,
            title = stringResource(R.string.evaluation_invalid),
            hint = stringResource(R.string.decision_invalid_hint),
            icon = R.drawable.ic_cancel,
            container = colors.errorContainer,
            content = colors.onErrorContainer,
            accent = colors.error,
            onClick = { viewModel.onResultSelected(EvaluationResult.INVALID) },
            modifier = Modifier.weight(1f),
        )
    }
    if (state.errors[EvaluationField.RESULT] != null) FieldError(stringResource(R.string.error_result_required))

    val notesErrorText = when (notesError) {
        ReportFieldError.REQUIRED -> stringResource(R.string.error_required)
        ReportFieldError.TOO_LONG -> stringResource(R.string.error_too_long, EvaluationRules.NOTES_MAX)
        else -> null
    }
    OutlinedTextField(
        state = viewModel.notes,
        labelPosition = TextFieldLabelPosition.Above(),
        label = {
            Text(stringResource(if (state.result == EvaluationResult.INVALID) R.string.field_notes_required else R.string.field_notes))
        },
        isError = notesErrorText != null,
        supportingText = {
            Column {
                if (notesErrorText != null) FieldError(notesErrorText)
                Text(stringResource(R.string.char_counter, viewModel.notes.text.length, EvaluationRules.NOTES_MAX))
            }
        },
        inputTransformation = InputTransformation.maxLength(EvaluationRules.NOTES_MAX),
        lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = 3),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { if (notesErrorText != null) error(notesErrorText) },
    )
}

@Composable
private fun DecisionCard(
    selected: Boolean,
    title: String,
    hint: String,
    @DrawableRes icon: Int,
    container: Color,
    content: Color,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        selected = selected,
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = if (selected) container else MaterialTheme.colorScheme.surfaceContainerLowest,
        contentColor = if (selected) content else MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) accent else MaterialTheme.colorScheme.outline),
        modifier = modifier.semantics { role = Role.RadioButton },
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(painterResource(icon), contentDescription = null, tint = accent, modifier = Modifier.size(28.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(hint, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun OpenInMapsButton(location: GeoPoint) {
    val context = LocalContext.current
    OutlinedButton(onClick = { context.openMaps(location) }) {
        Icon(painterResource(R.drawable.ic_map), contentDescription = null)
        Text(stringResource(R.string.open_in_maps), modifier = Modifier.padding(start = 8.dp))
    }
}

private fun Context.openMaps(location: GeoPoint) {
    val uri = "https://www.google.com/maps/search/?api=1&query=${location.latitude},${location.longitude}".toUri()
    try {
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (e: ActivityNotFoundException) {
        return
    }
}
