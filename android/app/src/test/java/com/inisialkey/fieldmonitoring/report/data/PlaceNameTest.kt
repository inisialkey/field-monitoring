package com.inisialkey.fieldmonitoring.report.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaceNameTest {

    @Test
    fun `joins the available address parts from village to province`() {
        assertEquals(
            "Pantai Bahagia, Muara Gembong, Kabupaten Bekasi, Jawa Barat",
            placeName(listOf("Pantai Bahagia", "Muara Gembong", "Kabupaten Bekasi", "Jawa Barat")),
        )
    }

    @Test
    fun `skips blank and repeated parts`() {
        assertEquals("Muara Gembong, Jawa Barat", placeName(listOf(null, " Muara Gembong ", "Muara Gembong", "", "Jawa Barat")))
    }

    @Test
    fun `returns nothing when no part is known`() {
        assertNull(placeName(listOf(null, " ", null)))
    }
}
