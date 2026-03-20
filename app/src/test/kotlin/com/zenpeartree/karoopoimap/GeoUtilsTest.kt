package com.zenpeartree.karoopoimap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoUtilsTest {

    @Test
    fun `haversineMeters returns zero for identical coordinates`() {
        assertEquals(0.0, haversineMeters(38.7223, -9.1393, 38.7223, -9.1393), 0.001)
    }

    @Test
    fun `haversineMeters returns expected short distance`() {
        val distance = haversineMeters(38.7223, -9.1393, 38.7228, -9.1393)
        assertTrue(distance in 50.0..60.0)
    }

    @Test
    fun `filterPoisByRadius keeps only pois inside radius`() {
        val centerLat = 38.7223
        val centerLng = -9.1393
        val pois = listOf(
            Poi(id = "near", lat = 38.7225, lng = -9.1393, type = PoiType.WATER),
            Poi(id = "far", lat = 38.8123, lng = -9.1393, type = PoiType.CAFE),
        )

        val filtered = filterPoisByRadius(pois, centerLat, centerLng, radiusKm = 2.0)

        assertEquals(listOf("near"), filtered.map { it.id })
    }
}
