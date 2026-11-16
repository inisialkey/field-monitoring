package com.inisialkey.fieldmonitoring.report.data

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.inisialkey.fieldmonitoring.report.domain.GeoPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

@Singleton
class LocationProvider @Inject constructor(@ApplicationContext context: Context) {

    private val client = LocationServices.getFusedLocationProviderClient(context)

    @OptIn(ExperimentalCoroutinesApi::class)
    @SuppressLint("MissingPermission")
    suspend fun currentLocation(): GeoPoint? = try {
        withTimeoutOrNull(TIMEOUT_MS) {
            val cancellation = CancellationTokenSource()
            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellation.token).await(cancellation)
        }?.let { GeoPoint(it.latitude, it.longitude, it.accuracy, Instant.ofEpochMilli(it.time)) }
    } catch (e: SecurityException) {
        null
    } catch (e: ApiException) {
        null
    }

    private companion object {
        const val TIMEOUT_MS = 30_000L
    }
}
