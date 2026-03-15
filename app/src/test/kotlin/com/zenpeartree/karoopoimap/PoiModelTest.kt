package com.zenpeartree.karoopoimap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PoiModelTest {

    @Test
    fun `PoiType fromId returns correct type`() {
        assertEquals(PoiType.WATER, PoiType.fromId("water"))
        assertEquals(PoiType.DOG_HAZARD, PoiType.fromId("dog_hazard"))
        assertEquals(PoiType.ROAD_HAZARD, PoiType.fromId("road_hazard"))
        assertEquals(PoiType.BIKE_SHOP, PoiType.fromId("bike_shop"))
        assertEquals(PoiType.CAFE, PoiType.fromId("cafe"))
        assertEquals(PoiType.RESTROOM, PoiType.fromId("restroom"))
        assertEquals(PoiType.FIRST_AID, PoiType.fromId("first_aid"))
    }

    @Test
    fun `PoiType fromId returns null for unknown type`() {
        assertNull(PoiType.fromId("unknown"))
        assertNull(PoiType.fromId(""))
    }

    @Test
    fun `all PoiType entries have unique ids`() {
        val ids = PoiType.entries.map { it.id }
        assertEquals(ids.size, ids.distinct().size)
    }

    @Test
    fun `all PoiType entries have non-empty labels`() {
        PoiType.entries.forEach {
            assertTrue("${it.name} should have a non-empty label", it.label.isNotBlank())
        }
    }

    @Test
    fun `Poi toSymbol uses type label when name is blank`() {
        val poi = Poi(id = "1", lat = 40.0, lng = -8.0, type = PoiType.WATER, name = "")
        val symbol = poi.toSymbol()
        assertEquals("Water", symbol.name)
    }

    @Test
    fun `Poi toSymbol uses name when provided`() {
        val poi = Poi(id = "1", lat = 40.0, lng = -8.0, type = PoiType.WATER, name = "Park fountain")
        val symbol = poi.toSymbol()
        assertEquals("Park fountain", symbol.name)
    }

    @Test
    fun `Poi toSymbol maps coordinates correctly`() {
        val poi = Poi(id = "abc", lat = 38.7223, lng = -9.1393, type = PoiType.CAFE)
        val symbol = poi.toSymbol()
        assertEquals("abc", symbol.id)
        assertEquals(38.7223, symbol.lat, 0.0001)
        assertEquals(-9.1393, symbol.lng, 0.0001)
    }

    @Test
    fun `Poi toSymbol maps karoo type correctly`() {
        assertEquals("water", Poi(id = "1", type = PoiType.WATER).toSymbol().type)
        assertEquals("caution", Poi(id = "2", type = PoiType.DOG_HAZARD).toSymbol().type)
        assertEquals("caution", Poi(id = "3", type = PoiType.ROAD_HAZARD).toSymbol().type)
        assertEquals("bike_shop", Poi(id = "4", type = PoiType.BIKE_SHOP).toSymbol().type)
        assertEquals("coffee", Poi(id = "5", type = PoiType.CAFE).toSymbol().type)
        assertEquals("restroom", Poi(id = "6", type = PoiType.RESTROOM).toSymbol().type)
        assertEquals("first_aid", Poi(id = "7", type = PoiType.FIRST_AID).toSymbol().type)
    }

    @Test
    fun `Poi isVisible returns true when not heavily downvoted`() {
        assertTrue(Poi(upvotes = 0, downvotes = 0).isVisible)
        assertTrue(Poi(upvotes = 3, downvotes = 5).isVisible)
        assertTrue(Poi(upvotes = 0, downvotes = 5).isVisible)
        assertTrue(Poi(upvotes = 10, downvotes = 15).isVisible)
    }

    @Test
    fun `Poi isVisible returns false when downvotes exceed upvotes by more than 5`() {
        assertFalse(Poi(upvotes = 0, downvotes = 6).isVisible)
        assertFalse(Poi(upvotes = 1, downvotes = 7).isVisible)
        assertFalse(Poi(upvotes = 0, downvotes = 100).isVisible)
    }

    @Test
    fun `Poi toFirestoreMap contains all fields`() {
        val poi = Poi(
            lat = 40.0, lng = -8.0,
            type = PoiType.WATER, name = "Test",
            createdBy = "uid1", createdAt = 1000L,
            geoHash = "abc12", upvotes = 3, downvotes = 1,
        )
        val map = poi.toFirestoreMap()

        assertEquals(40.0, map["lat"])
        assertEquals(-8.0, map["lng"])
        assertEquals("water", map["type"])
        assertEquals("Test", map["name"])
        assertEquals("uid1", map["createdBy"])
        assertEquals(1000L, map["createdAt"])
        assertEquals("abc12", map["geoHash"])
        assertEquals(3, map["upvotes"])
        assertEquals(1, map["downvotes"])
    }

    @Test
    fun `Poi fromFirestore parses valid data`() {
        val data = mapOf<String, Any?>(
            "lat" to 40.0,
            "lng" to -8.0,
            "type" to "water",
            "name" to "Fountain",
            "createdBy" to "uid1",
            "createdAt" to 1000L,
            "geoHash" to "abc12",
            "upvotes" to 5L,
            "downvotes" to 1L,
        )
        val poi = Poi.fromFirestore("doc1", data)

        assertNotNull(poi)
        assertEquals("doc1", poi!!.id)
        assertEquals(40.0, poi.lat, 0.01)
        assertEquals(-8.0, poi.lng, 0.01)
        assertEquals(PoiType.WATER, poi.type)
        assertEquals("Fountain", poi.name)
        assertEquals(5, poi.upvotes)
        assertEquals(1, poi.downvotes)
    }

    @Test
    fun `Poi fromFirestore returns null for missing lat`() {
        val data = mapOf<String, Any?>("lng" to -8.0, "type" to "water")
        assertNull(Poi.fromFirestore("id", data))
    }

    @Test
    fun `Poi fromFirestore returns null for missing lng`() {
        val data = mapOf<String, Any?>("lat" to 40.0, "type" to "water")
        assertNull(Poi.fromFirestore("id", data))
    }

    @Test
    fun `Poi fromFirestore returns null for unknown type`() {
        val data = mapOf<String, Any?>("lat" to 40.0, "lng" to -8.0, "type" to "unknown")
        assertNull(Poi.fromFirestore("id", data))
    }

    @Test
    fun `Poi fromFirestore returns null for missing type`() {
        val data = mapOf<String, Any?>("lat" to 40.0, "lng" to -8.0)
        assertNull(Poi.fromFirestore("id", data))
    }

    @Test
    fun `Poi fromFirestore handles missing optional fields`() {
        val data = mapOf<String, Any?>("lat" to 40.0, "lng" to -8.0, "type" to "cafe")
        val poi = Poi.fromFirestore("id", data)

        assertNotNull(poi)
        assertEquals("", poi!!.name)
        assertEquals("", poi.createdBy)
        assertEquals(0, poi.upvotes)
        assertEquals(0, poi.downvotes)
    }

    @Test
    fun `default Poi has sensible defaults`() {
        val poi = Poi()
        assertEquals("", poi.id)
        assertEquals(0.0, poi.lat, 0.01)
        assertEquals(0.0, poi.lng, 0.01)
        assertEquals(PoiType.WATER, poi.type)
        assertEquals("", poi.name)
        assertEquals(0, poi.upvotes)
        assertEquals(0, poi.downvotes)
        assertTrue(poi.isVisible)
    }
}
