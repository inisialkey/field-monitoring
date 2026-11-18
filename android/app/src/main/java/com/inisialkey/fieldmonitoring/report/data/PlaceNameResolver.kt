package com.inisialkey.fieldmonitoring.report.data

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

@Singleton
class PlaceNameResolver @Inject constructor(@ApplicationContext private val context: Context) {

    suspend fun resolve(latitude: Double, longitude: Double): String? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context, Locale.forLanguageTag("id-ID"))
        val address = withTimeoutOrNull(TIMEOUT_MS) { lookup(geocoder, latitude, longitude) } ?: return null
        val name = placeName(listOf(address.subLocality, address.locality, address.subAdminArea, address.adminArea))
            ?: address.getAddressLine(0)
        return name?.take(MAX_LENGTH)
    }

    private suspend fun lookup(geocoder: Geocoder, latitude: Double, longitude: Double): Address? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { continuation ->
                geocoder.getFromLocation(
                    latitude,
                    longitude,
                    1,
                    object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            continuation.resume(addresses.firstOrNull())
                        }

                        override fun onError(errorMessage: String?) {
                            continuation.resume(null)
                        }
                    },
                )
            }
        } else {
            withContext(Dispatchers.IO) {
                try {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
                } catch (e: IOException) {
                    null
                } catch (e: IllegalArgumentException) {
                    null
                }
            }
        }

    private companion object {
        const val TIMEOUT_MS = 10_000L
        const val MAX_LENGTH = 200
    }
}

fun placeName(parts: List<String?>): String? = parts.mapNotNull { part -> part?.trim()?.takeIf { it.isNotEmpty() } }
    .distinct()
    .takeIf { it.isNotEmpty() }
    ?.joinToString(", ")
