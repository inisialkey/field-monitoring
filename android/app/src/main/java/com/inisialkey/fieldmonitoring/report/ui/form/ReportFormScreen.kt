package com.inisialkey.fieldmonitoring.report.ui.form

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldLabelPosition
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.inisialkey.fieldmonitoring.R
import com.inisialkey.fieldmonitoring.core.ui.components.AppCard
import com.inisialkey.fieldmonitoring.core.ui.components.BackButton
import com.inisialkey.fieldmonitoring.core.ui.components.DeleteDialog
import com.inisialkey.fieldmonitoring.core.ui.components.FieldError
import com.inisialkey.fieldmonitoring.core.ui.components.PhotoGrid
import com.inisialkey.fieldmonitoring.core.ui.components.SectionCard
import com.inisialkey.fieldmonitoring.core.ui.components.contentWidth
import com.inisialkey.fieldmonitoring.core.ui.formatDate
import com.inisialkey.fieldmonitoring.core.ui.formatTime
import com.inisialkey.fieldmonitoring.core.ui.iconRes
import com.inisialkey.fieldmonitoring.core.ui.labelRes
import com.inisialkey.fieldmonitoring.report.domain.Category
import com.inisialkey.fieldmonitoring.report.domain.FieldError as ReportFieldError
import com.inisialkey.fieldmonitoring.report.domain.ReportField
import com.inisialkey.fieldmonitoring.report.domain.ReportRules
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

@Composable
fun ReportFormScreen(onBack: () -> Unit, viewModel: ReportFormViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current

    LaunchedEffect(state.submitted, state.deleted) { if (state.submitted || state.deleted) onBack() }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(resources.getString(it))
            viewModel.onMessageShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(if (state.isExistingDraft) R.string.form_title_draft else R.string.form_title_new))
                        if (state.saved) {
                            Text(
                                stringResource(R.string.form_autosaved),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = { BackButton(onBack) },
                actions = {
                    if (state.saved) {
                        IconButton(onClick = viewModel::onDeleteClick, enabled = !state.processingPhoto) {
                            Icon(painterResource(R.drawable.ic_delete), contentDescription = stringResource(R.string.delete_draft))
                        }
                    }
                },
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest, shadowElevation = 8.dp) {
                Button(
                    onClick = viewModel::onSubmitClick,
                    enabled = !state.loading,
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                        .contentWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .height(56.dp),
                ) {
                    Icon(painterResource(R.drawable.ic_send), contentDescription = null, modifier = Modifier.size(20.dp))
                    Text(
                        stringResource(R.string.submit_report),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(start = 10.dp),
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (!state.loading) {
            ReportFormContent(
                state = state,
                viewModel = viewModel,
                onMessage = { snackbarHostState.showSnackbar(resources.getString(it)) },
                modifier = Modifier.padding(padding),
            )
        }
    }

    if (state.confirmingDelete) {
        DeleteDialog(
            title = stringResource(R.string.dialog_delete_draft_title),
            message = stringResource(R.string.dialog_delete_draft_message),
            onConfirm = viewModel::onDeleteConfirmed,
            onDismiss = viewModel::onDeleteDismissed,
        )
    }

    if (state.confirmingSubmit) {
        AlertDialog(
            onDismissRequest = viewModel::onSubmitDismissed,
            title = { Text(stringResource(R.string.dialog_submit_title)) },
            text = { Text(stringResource(R.string.dialog_submit_message)) },
            confirmButton = { TextButton(onClick = viewModel::onSubmitConfirmed) { Text(stringResource(R.string.dialog_send)) } },
            dismissButton = { TextButton(onClick = viewModel::onSubmitDismissed) { Text(stringResource(R.string.dialog_review)) } },
        )
    }
}

@Composable
private fun ReportFormContent(
    state: ReportFormUiState,
    viewModel: ReportFormViewModel,
    onMessage: suspend (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val report = state.report
    var showPhotoSource by rememberSaveable { mutableStateOf(false) }
    var showLocationRationale by rememberSaveable { mutableStateOf(false) }
    var cameraUnavailable by remember { mutableStateOf(false) }
    val sections = remember { ReportField.entries.associateWith { BringIntoViewRequester() } }
    val focus = remember { ReportField.entries.associateWith { FocusRequester() } }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture(), viewModel::onCameraResult)
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia(), viewModel::onPhotoPicked)
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        val fine = result[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        val permanentlyDenied = !fine && activity != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)
        viewModel.onLocationPermissionResult(fine, approximateOnly = coarse && !fine, permanentlyDenied = permanentlyDenied)
    }
    val requestLocation = {
        if (context.hasFineLocation()) viewModel.captureLocation() else showLocationRationale = true
    }
    val openSettings = {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    LaunchedEffect(Unit) {
        if (!state.isExistingDraft && report.location == null && context.hasFineLocation()) viewModel.captureLocation()
    }
    LaunchedEffect(state.submitAttempt) {
        val first = state.errors.keys.firstOrNull() ?: return@LaunchedEffect
        if (first in TEXT_FIELDS) focus.getValue(first).requestFocus() else sections.getValue(first).bringIntoView()
    }
    LaunchedEffect(cameraUnavailable) {
        if (cameraUnavailable) {
            onMessage(R.string.camera_unavailable)
            cameraUnavailable = false
        }
    }

    val current = report.copy(
        title = viewModel.title.text.toString(),
        description = viewModel.description.text.toString(),
        locationNote = viewModel.locationNote.text.toString(),
    )
    val total = ReportField.entries.size
    val filled = total - current.validate(Instant.now()).size

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .contentWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CompletenessCard(filled = filled, total = total)

        SectionCard(
            number = 1,
            title = stringResource(R.string.field_photos),
            trailing = stringResource(R.string.photo_counter, report.photos.size, ReportRules.MAX_PHOTOS),
            error = state.errors[ReportField.PHOTOS]?.let { errorText(ReportField.PHOTOS, it) },
            modifier = Modifier.bringIntoViewRequester(sections.getValue(ReportField.PHOTOS)),
        ) {
            PhotoGrid(
                photos = report.photos,
                canAdd = report.photos.size < ReportRules.MAX_PHOTOS,
                processing = state.processingPhoto,
                onAdd = { showPhotoSource = true },
                onRemove = viewModel::onRemovePhoto,
            )
        }

        SectionCard(number = 2, title = stringResource(R.string.field_location)) {
            LocationField(
                status = state.location,
                location = report.location,
                resolvingPlace = state.resolvingPlace,
                online = state.online,
                onRequest = requestLocation,
                onOpenSettings = openSettings,
            )
            FormTextField(
                state = viewModel.locationNote,
                label = stringResource(
                    if (report.location == null) R.string.field_location_note_required else R.string.field_location_note,
                ),
                error = state.errors[ReportField.LOCATION_NOTE]?.let { errorText(ReportField.LOCATION_NOTE, it) },
                supporting = if (report.location == null) stringResource(R.string.location_note_required_supporting) else null,
                placeholder = stringResource(R.string.location_note_placeholder),
                maxLength = ReportRules.LOCATION_NOTE_MAX,
                focusRequester = focus.getValue(ReportField.LOCATION_NOTE),
            )
        }

        SectionCard(
            number = 3,
            title = stringResource(R.string.field_category),
            error = state.errors[ReportField.CATEGORY]?.let { errorText(ReportField.CATEGORY, it) },
            modifier = Modifier.bringIntoViewRequester(sections.getValue(ReportField.CATEGORY)),
        ) {
            FlowRow(modifier = Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Category.entries.forEach { category ->
                    val selected = report.category == category
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.onCategorySelected(category) },
                        label = { Text(stringResource(category.labelRes())) },
                        leadingIcon = {
                            Icon(
                                painterResource(if (selected) R.drawable.ic_check else category.iconRes()),
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize),
                            )
                        },
                    )
                }
            }
        }

        SectionCard(number = 4, title = stringResource(R.string.section_details)) {
            FormTextField(
                state = viewModel.title,
                label = stringResource(R.string.field_title),
                placeholder = stringResource(R.string.title_placeholder),
                error = state.errors[ReportField.TITLE]?.let { errorText(ReportField.TITLE, it) },
                maxLength = ReportRules.TITLE_MAX,
                showCounter = true,
                focusRequester = focus.getValue(ReportField.TITLE),
            )
            FormTextField(
                state = viewModel.description,
                label = stringResource(R.string.field_description),
                placeholder = stringResource(R.string.description_placeholder),
                error = state.errors[ReportField.DESCRIPTION]?.let { errorText(ReportField.DESCRIPTION, it) },
                maxLength = ReportRules.DESCRIPTION_MAX,
                showCounter = true,
                singleLine = false,
                focusRequester = focus.getValue(ReportField.DESCRIPTION),
            )
        }

        SectionCard(
            number = 5,
            title = stringResource(R.string.field_incident_time),
            error = state.errors[ReportField.INCIDENT_TIME]?.let { errorText(ReportField.INCIDENT_TIME, it) },
            modifier = Modifier.bringIntoViewRequester(sections.getValue(ReportField.INCIDENT_TIME)),
        ) {
            IncidentTimeField(report.incidentAt, report.incidentOffsetMinutes, viewModel::onIncidentTimeChange)
        }
    }

    if (showPhotoSource) {
        ModalBottomSheet(onDismissRequest = { showPhotoSource = false }) {
            SheetAction(R.drawable.ic_photo_camera, stringResource(R.string.take_photo)) {
                showPhotoSource = false
                try {
                    camera.launch(viewModel.cameraTarget())
                } catch (e: ActivityNotFoundException) {
                    viewModel.onCameraResult(false)
                    cameraUnavailable = true
                }
            }
            SheetAction(R.drawable.ic_photo_library, stringResource(R.string.pick_photo)) {
                showPhotoSource = false
                gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        }
    }

    if (showLocationRationale) {
        ModalBottomSheet(onDismissRequest = { showLocationRationale = false }) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(stringResource(R.string.rationale_location_title), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.rationale_location_message), style = MaterialTheme.typography.bodyLarge)
                Button(
                    onClick = {
                        showLocationRationale = false
                        locationPermission.launch(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.rationale_continue)) }
                OutlinedButton(
                    onClick = {
                        showLocationRationale = false
                        focus.getValue(ReportField.LOCATION_NOTE).requestFocus()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.rationale_manual)) }
            }
        }
    }
}

@Composable
private fun CompletenessCard(filled: Int, total: Int) {
    val progress by animateFloatAsState(filled / total.toFloat(), label = "completeness")
    AppCard(Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.form_progress_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "$filled/$total",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            LinearProgressIndicator(
                progress = { progress },
                strokeCap = StrokeCap.Round,
                trackColor = MaterialTheme.colorScheme.primaryContainer,
                gapSize = 0.dp,
                drawStopIndicator = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
            )
            Text(
                stringResource(if (filled == total) R.string.form_progress_ready else R.string.form_required_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FormTextField(
    state: TextFieldState,
    label: String,
    maxLength: Int,
    focusRequester: FocusRequester,
    error: String? = null,
    supporting: String? = null,
    placeholder: String? = null,
    showCounter: Boolean = false,
    singleLine: Boolean = true,
) {
    OutlinedTextField(
        state = state,
        labelPosition = TextFieldLabelPosition.Above(),
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        isError = error != null,
        supportingText = if (error != null || supporting != null || showCounter) {
            {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (error != null) FieldError(error) else if (supporting != null) Text(supporting)
                    }
                    if (showCounter) Text(stringResource(R.string.char_counter, state.text.length, maxLength))
                }
            }
        } else {
            null
        },
        inputTransformation = InputTransformation.maxLength(maxLength),
        lineLimits = if (singleLine) TextFieldLineLimits.SingleLine else TextFieldLineLimits.MultiLine(minHeightInLines = 4),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = if (singleLine) ImeAction.Next else ImeAction.Default,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .semantics { if (error != null) error(error) },
    )
}

@Composable
private fun IncidentTimeField(incidentAt: Instant, offsetMinutes: Int, onChange: (Instant) -> Unit) {
    var showDate by rememberSaveable { mutableStateOf(false) }
    var showTime by rememberSaveable { mutableStateOf(false) }
    val local = incidentAt.atZone(ZoneId.systemDefault())
    val pickDate = stringResource(R.string.pick_date)
    val pickTime = stringResource(R.string.pick_time)
    val date = formatDate(incidentAt, offsetMinutes)
    val time = formatTime(incidentAt, offsetMinutes)

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = { showDate = true },
            modifier = Modifier
                .weight(1f)
                .semantics { contentDescription = "$pickDate, $date" },
        ) {
            Icon(painterResource(R.drawable.ic_calendar_today), contentDescription = null)
            Text(date, modifier = Modifier.padding(start = 8.dp))
        }
        OutlinedButton(
            onClick = { showTime = true },
            modifier = Modifier
                .weight(1f)
                .semantics { contentDescription = "$pickTime, $time" },
        ) {
            Icon(painterResource(R.drawable.ic_schedule), contentDescription = null)
            Text(time, modifier = Modifier.padding(start = 8.dp))
        }
    }

    if (showDate) {
        val datePicker = rememberDatePickerState(
            initialSelectedDateMillis = local.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = PastDates,
        )
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePicker.selectedDateMillis?.let { millis ->
                            onChange(local.with(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()).toInstant())
                        }
                        showDate = false
                    },
                ) { Text(stringResource(R.string.dialog_ok)) }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text(stringResource(R.string.action_cancel)) } },
        ) {
            DatePicker(state = datePicker)
        }
    }

    if (showTime) {
        val timePicker = rememberTimePickerState(initialHour = local.hour, initialMinute = local.minute, is24Hour = true)
        TimePickerDialog(
            onDismissRequest = { showTime = false },
            title = { Text(pickTime) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onChange(local.withHour(timePicker.hour).withMinute(timePicker.minute).withSecond(0).withNano(0).toInstant())
                        showTime = false
                    },
                ) { Text(stringResource(R.string.dialog_ok)) }
            },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text(stringResource(R.string.action_cancel)) } },
        ) {
            TimeInput(state = timePicker)
        }
    }
}

@Composable
private fun SheetAction(@DrawableRes icon: Int, label: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        leadingContent = { Icon(painterResource(icon), contentDescription = null) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun errorText(field: ReportField, error: ReportFieldError): String = when (error) {
    ReportFieldError.REQUIRED -> when (field) {
        ReportField.PHOTOS -> stringResource(R.string.error_photos_required)
        else -> stringResource(R.string.error_required)
    }
    ReportFieldError.TOO_MANY -> stringResource(R.string.error_too_many_photos, ReportRules.MAX_PHOTOS)
    ReportFieldError.IN_FUTURE -> stringResource(R.string.error_future_time)
    ReportFieldError.TOO_LONG -> stringResource(
        R.string.error_too_long,
        when (field) {
            ReportField.TITLE -> ReportRules.TITLE_MAX
            ReportField.DESCRIPTION -> ReportRules.DESCRIPTION_MAX
            else -> ReportRules.LOCATION_NOTE_MAX
        },
    )
}

private val TEXT_FIELDS = setOf(ReportField.TITLE, ReportField.DESCRIPTION, ReportField.LOCATION_NOTE)

private object PastDates : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean =
        utcTimeMillis <= LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    override fun isSelectableYear(year: Int): Boolean = year <= LocalDate.now().year
}

private fun Context.hasFineLocation(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
