package com.zenpeartree.karoopoimap

import io.hammerhead.karooext.models.Symbol

enum class PoiType(val id: String, val label: String, val karooType: String) {
    WATER("water", "Water", Symbol.POI.Types.WATER),
    DOG_HAZARD("dog_hazard", "Dogs", Symbol.POI.Types.CAUTION),
    ROAD_HAZARD("road_hazard", "Road Hazard", Symbol.POI.Types.CAUTION),
    BIKE_SHOP("bike_shop", "Bike Shop", Symbol.POI.Types.BIKE_SHOP),
    CAFE("cafe", "Cafe", Symbol.POI.Types.COFFEE),
    RESTROOM("restroom", "Restroom", Symbol.POI.Types.RESTROOM),
    FIRST_AID("first_aid", "First Aid", Symbol.POI.Types.FIRST_AID);

    companion object {
        fun fromId(id: String): PoiType? = entries.find { it.id == id }
    }
}

data class Poi(
    val id: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val type: PoiType = PoiType.WATER,
    val name: String = "",
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val geoHash: String = "",
    val upvotes: Int = 0,
    val downvotes: Int = 0,
) {
    val displayName: String get() = if (name.isNotBlank()) name else type.label

    fun toSymbol(): Symbol.POI {
        return Symbol.POI(
            id = id,
            lat = lat,
            lng = lng,
            type = type.karooType,
            name = displayName,
        )
    }

    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        "lat" to lat,
        "lng" to lng,
        "type" to type.id,
        "name" to name,
        "createdBy" to createdBy,
        "createdAt" to createdAt,
        "geoHash" to geoHash,
        "upvotes" to upvotes,
        "downvotes" to downvotes,
    )

    val isVisible: Boolean get() = downvotes - upvotes <= 5

    companion object {
        fun fromFirestore(id: String, data: Map<String, Any?>): Poi? {
            val lat = (data["lat"] as? Number)?.toDouble() ?: return null
            val lng = (data["lng"] as? Number)?.toDouble() ?: return null
            val typeId = data["type"] as? String ?: return null
            val type = PoiType.fromId(typeId) ?: return null
            return Poi(
                id = id,
                lat = lat,
                lng = lng,
                type = type,
                name = data["name"] as? String ?: "",
                createdBy = data["createdBy"] as? String ?: "",
                createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0L,
                geoHash = data["geoHash"] as? String ?: "",
                upvotes = (data["upvotes"] as? Number)?.toInt() ?: 0,
                downvotes = (data["downvotes"] as? Number)?.toInt() ?: 0,
            )
        }
    }
}
