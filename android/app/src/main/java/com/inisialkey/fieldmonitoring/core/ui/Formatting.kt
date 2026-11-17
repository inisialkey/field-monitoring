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
    Category.FOREST_REHABILITATION -> R.string.category_forest_rehabilitation
    Category.MANGROVE -> R.string.category_mangrove
    Category.PEATLAND_RESTORATION -> R.string.category_peatland_restoration
    Category.SOCIAL_FORESTRY -> R.string.category_social_forestry
    Category.RENEWABLE_ENERGY -> R.string.category_renewable_energy
    Category.WASTE_MANAGEMENT -> R.string.category_waste_management
    Category.BIODIVERSITY -> R.string.category_biodiversity
    Category.OTHER -> R.string.category_other
}

@DrawableRes
fun Category.iconRes(): Int = when (this) {
    Category.FOREST_REHABILITATION -> R.drawable.ic_forest
    Category.MANGROVE -> R.drawable.ic_waves
    Category.PEATLAND_RESTORATION -> R.drawable.ic_grass
    Category.SOCIAL_FORESTRY -> R.drawable.ic_groups
    Category.RENEWABLE_ENERGY -> R.drawable.ic_solar_power
    Category.WASTE_MANAGEMENT -> R.drawable.ic_recycling
    Category.BIODIVERSITY -> R.drawable.ic_pets
    Category.OTHER -> R.drawable.ic_category
}

private fun offset(minutes: Int): ZoneOffset = ZoneOffset.ofTotalSeconds(minutes * 60)
