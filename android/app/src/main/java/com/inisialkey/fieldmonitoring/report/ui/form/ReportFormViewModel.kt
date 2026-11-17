package com.inisialkey.fieldmonitoring.report.ui.form

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshotFlow
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.inisialkey.fieldmonitoring.R
import com.inisialkey.fieldmonitoring.ReportForm
import com.inisialkey.fieldmonitoring.auth.data.SessionStore
import com.inisialkey.fieldmonitoring.core.ApplicationScope
import com.inisialkey.fieldmonitoring.core.UserMessage
import com.inisialkey.fieldmonitoring.core.UserMessages
import com.inisialkey.fieldmonitoring.core.ui.deviceOffsetMinutes
import com.inisialkey.fieldmonitoring.report.data.LocationProvider
import com.inisialkey.fieldmonitoring.report.data.PhotoStore
import com.inisialkey.fieldmonitoring.report.data.ReportRepository
import com.inisialkey.fieldmonitoring.report.domain.Category
import com.inisialkey.fieldmonitoring.report.domain.FieldError
import com.inisialkey.fieldmonitoring.report.domain.Photo
import com.inisialkey.fieldmonitoring.report.domain.Report
import com.inisialkey.fieldmonitoring.report.domain.ReportField
import com.inisialkey.fieldmonitoring.report.domain.ReportRules
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface LocationStatus {
    data object Idle : LocationStatus
    data object Searching : LocationStatus
    data object Acquired : LocationStatus
    data object Approximate : LocationStatus
    data class Unavailable(val needsSettings: Boolean) : LocationStatus
}

data class ReportFormUiState(
    val report: Report,
    val loading: Boolean = true,
    val isExistingDraft: Boolean = false,
    val saved: Boolean = false,
    val errors: Map<ReportField, FieldError> = emptyMap(),
    val submitAttempt: Int = 0,
    val location: LocationStatus = LocationStatus.Idle,
    val processingPhoto: Boolean = false,
    val confirmingSubmit: Boolean = false,
    val submitted: Boolean = false,
    val confirmingDelete: Boolean = false,
    val deleted: Boolean = false,
    @StringRes val message: Int? = null,
)

@HiltViewModel
class ReportFormViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    sessionStore: SessionStore,
    private val reports: ReportRepository,
    private val photos: PhotoStore,
    private val locationProvider: LocationProvider,
    private val userMessages: UserMessages,
    @ApplicationScope private val appScope: CoroutineScope,
) : ViewModel() {

    private val reportId = savedStateHandle.toRoute<ReportForm>().reportId
    private val ownerId = sessionStore.session.value?.userId.orEmpty()

    val title = TextFieldState()
    val description = TextFieldState()
    val locationNote = TextFieldState()

    private val _state = MutableStateFlow(
        ReportFormUiState(report = Report.newDraft(reportId, ownerId, Instant.now(), deviceOffsetMinutes())),
    )
    val state: StateFlow<ReportFormUiState> = _state.asStateFlow()

    private var dirty = false
    private var saveJob: Job? = null

    init {
        viewModelScope.launch {
            val existing = reports.getReport(reportId, ownerId)
            if (existing != null) {
                title.setTextAndPlaceCursorAtEnd(existing.title)
                description.setTextAndPlaceCursorAtEnd(existing.description)
                locationNote.setTextAndPlaceCursorAtEnd(existing.locationNote)
            }
            _state.update {
                it.copy(
                    report = existing ?: it.report,
                    loading = false,
                    isExistingDraft = existing != null,
                    saved = existing != null,
                    location = if (existing?.location != null) LocationStatus.Acquired else LocationStatus.Idle,
                )
            }
            snapshotFlow { listOf(title.text.toString(), description.text.toString(), locationNote.text.toString()) }
                .drop(1)
                .collect { onEdited() }
        }
    }

    fun onCategorySelected(category: Category) = updateReport { copy(category = category) }

    fun onIncidentTimeChange(instant: Instant) =
        updateReport { copy(incidentAt = instant, incidentOffsetMinutes = deviceOffsetMinutes(instant)) }

    fun onLocationPermissionResult(granted: Boolean, approximateOnly: Boolean, permanentlyDenied: Boolean) {
        when {
            granted -> captureLocation()
            approximateOnly -> _state.update { it.copy(location = LocationStatus.Approximate) }
            else -> _state.update { it.copy(location = LocationStatus.Unavailable(needsSettings = permanentlyDenied)) }
        }
    }

    fun captureLocation() {
        if (_state.value.location == LocationStatus.Searching) return
        _state.update { it.copy(location = LocationStatus.Searching) }
        viewModelScope.launch {
            val point = locationProvider.currentLocation()
            _state.update {
                when {
                    point != null -> it.copy(report = it.report.copy(location = point), location = LocationStatus.Acquired)
                    it.report.location != null -> it.copy(location = LocationStatus.Acquired)
                    else -> it.copy(location = LocationStatus.Unavailable(needsSettings = false))
                }
            }
            if (point != null && (dirty || _state.value.saved)) onEdited()
        }
    }

    fun cameraTarget(): Uri = photos.newCameraTarget().also { savedStateHandle[CAMERA_URI] = it.toString() }

    fun onCameraResult(success: Boolean) {
        val uri = savedStateHandle.remove<String>(CAMERA_URI)?.toUri() ?: return
        if (success) addPhoto(uri) else photos.discard(uri)
    }

    fun onPhotoPicked(uri: Uri?) {
        if (uri != null) addPhoto(uri)
    }

    fun onRemovePhoto(photo: Photo) {
        _state.update { it.copy(report = it.report.copy(photos = it.report.photos - photo)).revalidated() }
        appScope.launch { reports.removePhoto(photo, ownerId) }
    }

    fun onMessageShown() {
        _state.update { it.copy(message = null) }
    }

    fun onSubmitClick() {
        val errors = ReportRules.validate(_state.value.currentReport(), Instant.now())
        _state.update {
            it.copy(
                errors = errors,
                confirmingSubmit = errors.isEmpty(),
                submitAttempt = if (errors.isEmpty()) it.submitAttempt else it.submitAttempt + 1,
            )
        }
    }

    fun onSubmitDismissed() {
        _state.update { it.copy(confirmingSubmit = false) }
    }

    fun onSubmitConfirmed() {
        _state.update { it.copy(confirmingSubmit = false) }
        saveJob?.cancel()
        val report = _state.value.currentReport()
        viewModelScope.launch {
            if (reports.submit(report)) {
                _state.update { it.copy(submitted = true) }
                userMessages.show(UserMessage(R.string.message_submitted))
            }
        }
    }

    fun onDeleteClick() {
        _state.update { it.copy(confirmingDelete = true) }
    }

    fun onDeleteDismissed() {
        _state.update { it.copy(confirmingDelete = false) }
    }

    fun onDeleteConfirmed() {
        _state.update { it.copy(confirmingDelete = false) }
        saveJob?.cancel()
        val report = _state.value.currentReport()
        viewModelScope.launch {
            if (reports.deleteUnsent(report)) {
                _state.update { it.copy(deleted = true) }
                userMessages.show(UserMessage(R.string.message_draft_deleted))
            }
        }
    }

    override fun onCleared() {
        val state = _state.value
        if (state.loading || state.submitted || state.deleted || !(dirty || state.saved)) return
        val report = state.currentReport()
        appScope.launch {
            if (reports.saveDraft(report)) userMessages.show(UserMessage(R.string.message_draft_saved))
        }
    }

    private fun addPhoto(source: Uri) {
        val report = _state.value.currentReport()
        if (report.photos.size >= ReportRules.MAX_PHOTOS) return
        _state.update { it.copy(processingPhoto = true) }
        appScope.launch {
            val photo = try {
                reports.addPhoto(report, source)
            } catch (e: IOException) {
                null
            }
            _state.update {
                if (photo == null) {
                    it.copy(processingPhoto = false, message = R.string.photo_failed)
                } else {
                    it.copy(processingPhoto = false, saved = true, report = it.report.copy(photos = it.report.photos + photo))
                        .revalidated()
                }
            }
        }
    }

    private fun updateReport(transform: Report.() -> Report) {
        _state.update { it.copy(report = it.report.transform()) }
        onEdited()
    }

    private fun onEdited() {
        if (_state.value.deleted) return
        dirty = true
        _state.update { it.revalidated() }
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(AUTOSAVE_DELAY_MS)
            if (reports.saveDraft(_state.value.currentReport())) {
                dirty = false
                _state.update { it.copy(saved = true) }
            }
        }
    }

    private fun ReportFormUiState.revalidated(): ReportFormUiState =
        if (errors.isEmpty()) this else copy(errors = ReportRules.validate(currentReport(), Instant.now()))

    private fun ReportFormUiState.currentReport(): Report = report.copy(
        title = title.text.toString(),
        description = description.text.toString(),
        locationNote = locationNote.text.toString(),
    )

    private companion object {
        const val CAMERA_URI = "camera_uri"
        const val AUTOSAVE_DELAY_MS = 500L
    }
}
