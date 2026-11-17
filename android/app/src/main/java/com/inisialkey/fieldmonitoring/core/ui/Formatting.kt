package com.inisialkey.fieldmonitoring.core.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.inisialkey.fieldmonitoring.R
import com.inisialkey.fieldmonitoring.report.domain.Category
import com.inisialkey.fieldmonitoring.report.domain.GeoPoint
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Indonesian: Locale = Locale.forLanguageTag("id-ID")
private val DateTimeFormat = DateTimeFormatter.ofPattern("d MMM yyyy, HH.mm", Indonesian)
private val DateFormat = DateTimeFormatter.ofPattern("d MMM yyyy", Indonesian)
private val TimeFormat = DateTimeFormatter.ofPattern("HH.mm", Indonesian)

fun deviceOffsetMinutes(instant: Instant = Instant.now()): Int =
    ZoneId.systemDefault().rules.getOffset(instant).totalSeconds / 60

fun formatDateTime(instant: Instant, offsetMinutes: Int = deviceOffsetMinutes(instant)): String =
    "${DateTimeFormat.format(instant.atOffset(offset(offsetMinutes)))} ${zoneLabel(offsetMinutes)}"

fun formatDate(instant: Instant, offsetMinutes: Int = deviceOffsetMinutes(instant)): String =
    DateFormat.format(instant.atOffset(offset(offsetMinutes)))

fun formatTime(instant: Instant, offsetMinutes: Int = deviceOffsetMinutes(instant)): String =
    "${TimeFormat.format(instant.atOffset(offset(offsetMinutes)))} ${zoneLabel(offsetMinutes)}"

fun zoneLabel(offsetMinutes: Int): String = when (offsetMinutes) {
    420 -> "WIB"
    480 -> "WITA"
    540 -> "WIT"
    0 -> "UTC"
    else -> "UTC${offset(offsetMinutes).id}"
}

fun GeoPoint.coordinates(): String = String.format(Locale.ROOT, "%.5f, %.5f", latitude, longitude)

@StringRes
fun Category.labelRes(): Int = when (this) {
    Category.ROAD -> R.string.category_road
    Category.BRIDGE -> R.string.category_bridge
    Category.DRAINAGE -> R.string.category_drainage
    Category.STREET_LIGHT -> R.string.category_street_light
    Category.TRAFFIC_SIGN -> R.string.category_traffic_sign
    Category.PUBLIC_FACILITY -> R.string.category_public_facility
    Category.OTHER -> R.string.category_other
}

@DrawableRes
fun Category.iconRes(): Int = when (this) {
    Category.ROAD -> R.drawable.ic_road
    Category.BRIDGE -> R.drawable.ic_foundation
    Category.DRAINAGE -> R.drawable.ic_water_drop
    Category.STREET_LIGHT -> R.drawable.ic_light
    Category.TRAFFIC_SIGN -> R.drawable.ic_signpost
    Category.PUBLIC_FACILITY -> R.drawable.ic_apartment
    Category.OTHER -> R.drawable.ic_category
}

private fun offset(minutes: Int): ZoneOffset = ZoneOffset.ofTotalSeconds(minutes * 60)
