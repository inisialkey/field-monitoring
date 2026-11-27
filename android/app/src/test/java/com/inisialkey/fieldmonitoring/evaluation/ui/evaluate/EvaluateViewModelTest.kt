package com.inisialkey.fieldmonitoring.evaluation.ui.evaluate

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.inisialkey.fieldmonitoring.Evaluate
import com.inisialkey.fieldmonitoring.core.UserMessages
import com.inisialkey.fieldmonitoring.evaluation.data.EvaluationRepository
import com.inisialkey.fieldmonitoring.evaluation.data.QueuePage
import com.inisialkey.fieldmonitoring.report.domain.Category
import com.inisialkey.fieldmonitoring.report.domain.Evaluation
import com.inisialkey.fieldmonitoring.report.domain.EvaluationResult
import com.inisialkey.fieldmonitoring.report.domain.Report
import com.inisialkey.fieldmonitoring.report.domain.SyncState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EvaluateViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<EvaluationRepository>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        mockkStatic("androidx.navigation.SavedStateHandleKt")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun report(id: String, category: Category) = Report(
        id = id,
        ownerId = "owner-1",
        title = "Laporan $id",
        category = category,
        description = "Deskripsi",
        incidentAt = Instant.EPOCH,
        incidentOffsetMinutes = 420,
        location = null,
        locationNote = "Blok B",
        syncState = SyncState.SYNCED,
        lastError = null,
        createdAt = Instant.EPOCH,
        receivedAt = Instant.EPOCH,
        photos = emptyList(),
        evaluation = null,
    )

    private fun viewModel(category: String?): EvaluateViewModel {
        val current = report("m1", Category.MANGROVE)
        coEvery { repository.report("m1") } returns current
        coEvery { repository.evaluate("m1", EvaluationResult.VALID, any()) } returns
            Evaluation(EvaluationResult.VALID, null, "Andi", Instant.EPOCH)
        val handle = SavedStateHandle()
        every { handle.toRoute(Evaluate::class, any()) } returns Evaluate("m1", category)
        return EvaluateViewModel(handle, repository, UserMessages())
    }

    private fun EvaluateViewModel.submitValid() {
        onResultSelected(EvaluationResult.VALID)
        onSubmitClick()
        onConfirmed()
    }

    @Test
    fun `the next report comes from the category the queue was filtered by`() = runTest(dispatcher) {
        val nextMangrove = report("m2", Category.MANGROVE)
        coEvery { repository.queue(null, Category.MANGROVE) } returns
            QueuePage(listOf(report("m1", Category.MANGROVE), nextMangrove), null)
        val viewModel = viewModel(category = "MANGROVE")
        advanceUntilIdle()

        viewModel.submitValid()
        advanceUntilIdle()

        assertEquals(EvaluateOutcome.OpenNext("m2"), viewModel.state.value.outcome)
        coVerify(exactly = 0) { repository.queue(any(), null) }
    }

    @Test
    fun `the next report comes from the whole queue when no filter is active`() = runTest(dispatcher) {
        coEvery { repository.queue(null, null) } returns QueuePage(listOf(report("o1", Category.OTHER)), null)
        val viewModel = viewModel(category = null)
        advanceUntilIdle()

        viewModel.submitValid()
        advanceUntilIdle()

        assertEquals(EvaluateOutcome.OpenNext("o1"), viewModel.state.value.outcome)
    }
}
