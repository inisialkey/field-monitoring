package com.inisialkey.fieldmonitoring.sync

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.await
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

@Singleton
class SyncScheduler @Inject constructor(private val workManager: WorkManager) {

    fun enqueue(ownerId: String) {
        enqueue(ownerId, ExistingWorkPolicy.APPEND_OR_REPLACE)
    }

    suspend fun syncNow(ownerId: String) {
        val running = observe(ownerId).first().any { it.state == WorkInfo.State.RUNNING }
        enqueue(ownerId, if (running) ExistingWorkPolicy.APPEND_OR_REPLACE else ExistingWorkPolicy.REPLACE).await()
    }

    fun cancel(ownerId: String) {
        workManager.cancelUniqueWork(workName(ownerId))
    }

    fun observe(ownerId: String): Flow<List<WorkInfo>> = workManager.getWorkInfosForUniqueWorkFlow(workName(ownerId))

    private fun enqueue(ownerId: String, policy: ExistingWorkPolicy) = workManager.enqueueUniqueWork(
        workName(ownerId),
        policy,
        OneTimeWorkRequestBuilder<SyncWorker>()
            .setInputData(workDataOf(SyncWorker.KEY_OWNER_ID to ownerId))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build(),
    )

    private fun workName(ownerId: String) = "sync-$ownerId"
}
