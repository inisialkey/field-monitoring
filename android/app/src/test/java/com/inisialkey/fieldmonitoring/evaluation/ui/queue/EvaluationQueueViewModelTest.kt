package com.inisialkey.fieldmonitoring.evaluation.ui.queue

import com.inisialkey.fieldmonitoring.core.UserMessages
import com.inisialkey.fieldmonitoring.evaluation.data.EvaluationRepository
import com.inisialkey.fieldmonitoring.evaluation.data.QueuePage
import com.inisialkey.fieldmonitoring.report.domain.Category
import com.inisialkey.fieldmonitoring.report.domain.Report
import com.inisialkey.fieldmonitoring.report.domain.SyncState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EvaluationQueueViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<EvaluationRepository>()
    private lateinit var viewModel: EvaluationQueueViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = EvaluationQueueViewModel(repository, UserMessages())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
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

    @Test
    fun `selecting a category loads its first page`() = runTest(dispatcher) {
        val mangrove = report("m1", Category.MANGROVE)
        coEvery { repository.queue(null, Category.MANGROVE) } returns QueuePage(listOf(mangrove), "c1")

        viewModel.selectCategory(Category.MANGROVE)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(Category.MANGROVE, state.category)
        assertEquals(listOf(mangrove), state.reports)
        assertEquals("c1", state.nextCursor)
        assertEquals(false, state.loading)
    }

    @Test
    fun `loading more keeps the active category`() = runTest(dispatcher) {
        val first = report("m1", Category.MANGROVE)
        val second = report("m2", Category.MANGROVE)
        coEvery { repository.queue(null, Category.MANGROVE) } returns QueuePage(listOf(first), "c1")
        coEvery { repository.queue("c1", Category.MANGROVE) } returns QueuePage(listOf(second), null)

        viewModel.selectCategory(Category.MANGROVE)
        advanceUntilIdle()
        viewModel.loadMore()
        advanceUntilIdle()

        assertEquals(listOf(first, second), viewModel.state.value.reports)
        assertNull(viewModel.state.value.nextCursor)
        coVerify(exactly = 1) { repository.queue("c1", Category.MANGROVE) }
    }

    @Test
    fun `changing the category drops the old cursor and reports`() = runTest(dispatcher) {
        val unfiltered = report("a1", Category.OTHER)
        val late = report("a2", Category.OTHER)
        val biodiversity = report("b1", Category.BIODIVERSITY)
        coEvery { repository.queue(null, null) } returns QueuePage(listOf(unfiltered), "all-cursor")
        coEvery { repository.queue("all-cursor", null) } coAnswers {
            delay(1_000)
            QueuePage(listOf(late), null)
        }
        coEvery { repository.queue(null, Category.BIODIVERSITY) } returns QueuePage(listOf(biodiversity), null)

        viewModel.refresh()
        advanceUntilIdle()
        viewModel.loadMore()
        runCurrent()
        viewModel.selectCategory(Category.BIODIVERSITY)

        val switching = viewModel.state.value
        assertTrue(switching.reports.isEmpty())
        assertNull(switching.nextCursor)
        assertTrue(switching.loading)

        advanceUntilIdle()

        assertEquals(listOf(biodiversity), viewModel.state.value.reports)
        coVerify(exactly = 1) { repository.queue("all-cursor", null) }
        coVerify(exactly = 0) { repository.queue("all-cursor", Category.BIODIVERSITY) }
    }

    @Test
    fun `selecting the same category again does not reload`() = runTest(dispatcher) {
        coEvery { repository.queue(null, Category.MANGROVE) } returns QueuePage(emptyList(), null)

        viewModel.selectCategory(Category.MANGROVE)
        advanceUntilIdle()
        viewModel.selectCategory(Category.MANGROVE)
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.queue(null, Category.MANGROVE) }
    }

    @Test
    fun `selecting all loads the unfiltered queue`() = runTest(dispatcher) {
        val unfiltered = listOf(report("m1", Category.MANGROVE), report("o1", Category.OTHER))
        coEvery { repository.queue(null, Category.MANGROVE) } returns QueuePage(unfiltered.take(1), null)
        coEvery { repository.queue(null, null) } returns QueuePage(unfiltered, null)

        viewModel.selectCategory(Category.MANGROVE)
        advanceUntilIdle()
        viewModel.selectCategory(null)
        advanceUntilIdle()

        assertNull(viewModel.state.value.category)
        assertEquals(unfiltered, viewModel.state.value.reports)
        coVerify(exactly = 1) { repository.queue(null, null) }
    }
}
