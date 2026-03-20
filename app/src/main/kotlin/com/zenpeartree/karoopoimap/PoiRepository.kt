package com.zenpeartree.karoopoimap

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import ch.hsr.geohash.GeoHash
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import org.json.JSONArray
import org.json.JSONObject

class PoiRepository(context: Context) {

    companion object {
        private const val TAG = "PoiRepository"
        private const val PREFS_NAME = "poi_cache"
        private const val KEY_POIS = "cached_pois"
        private const val COLLECTION = "pois"
        private const val GEOHASH_PRECISION = 5 // ~5km cells
    }

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private var cachedPois = mutableListOf<Poi>()

    init {
        loadCache()
        ensureAuth()
    }

    private fun ensureAuth() {
        if (auth.currentUser == null) {
            auth.signInAnonymously()
                .addOnSuccessListener { Log.i(TAG, "Anonymous auth: ${it.user?.uid}") }
                .addOnFailureListener { Log.e(TAG, "Auth failed", it) }
        }
    }

    private fun withAuthenticatedUser(onResult: (Boolean) -> Unit, block: (String) -> Unit) {
        val currentUid = auth.currentUser?.uid
        if (!currentUid.isNullOrBlank()) {
            block(currentUid)
            return
        }

        auth.signInAnonymously()
            .addOnSuccessListener { result ->
                val uid = result.user?.uid
                if (uid.isNullOrBlank()) {
                    Log.e(TAG, "Anonymous auth returned no uid")
                    onResult(false)
                    return@addOnSuccessListener
                }
                Log.i(TAG, "Anonymous auth ready for action: $uid")
                block(uid)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Auth failed before action", e)
                onResult(false)
            }
    }

    fun getCurrentUserId(): String = auth.currentUser?.uid ?: ""

    fun getCachedPois(): List<Poi> = cachedPois.filter { it.isVisible }

    fun fetchNearby(lat: Double, lng: Double, radiusKm: Double, onResult: (List<Poi>) -> Unit) {
        val center = GeoHash.withCharacterPrecision(lat, lng, GEOHASH_PRECISION)
        val neighbors = center.adjacent.toMutableList()
        neighbors.add(center)

        val hashes = neighbors.map { it.toBase32() }
        val minHash = hashes.min()
        val maxHash = hashes.max() + "~"

        db.collection(COLLECTION)
            .whereGreaterThanOrEqualTo("geoHash", minHash)
            .whereLessThanOrEqualTo("geoHash", maxHash)
            .get()
            .addOnSuccessListener { snapshot ->
                val pois = snapshot.documents.mapNotNull { doc ->
                    Poi.fromFirestore(doc.id, doc.data ?: emptyMap())
                }
                val nearbyPois = filterPoisByRadius(pois, lat, lng, radiusKm)
                cachedPois.clear()
                cachedPois.addAll(nearbyPois)
                saveCache()
                Log.i(TAG, "Fetched ${nearbyPois.size} POIs within ${radiusKm}km of ($lat, $lng)")
                onResult(nearbyPois.filter { it.isVisible })
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Fetch failed, using cache", e)
                onResult(getCachedPois())
            }
    }

    fun addPoi(poi: Poi, onResult: (Boolean) -> Unit) {
        withAuthenticatedUser(onResult) { uid ->
            val geoHash = GeoHash.withCharacterPrecision(poi.lat, poi.lng, GEOHASH_PRECISION).toBase32()
            val poiWithHash = poi.copy(
                geoHash = geoHash,
                createdBy = uid,
                createdAt = System.currentTimeMillis(),
            )

            db.collection(COLLECTION)
                .add(poiWithHash.toFirestoreMap())
                .addOnSuccessListener { ref ->
                    val saved = poiWithHash.copy(id = ref.id)
                    cachedPois.removeAll { it.id == saved.id }
                    cachedPois.add(saved)
                    saveCache()
                    Log.i(TAG, "Added POI ${ref.id}: ${poi.type.label} at (${poi.lat}, ${poi.lng})")
                    onResult(true)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to add POI", e)
                    onResult(false)
                }
        }
    }

    private fun persistVote(poiRefId: String, uid: String, upvote: Boolean, onResult: (Boolean) -> Unit) {
        val voteRef = db.collection(COLLECTION).document(poiRefId)
            .collection("votes").document(uid)

        voteRef.get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                Log.i(TAG, "Already voted on $poiRefId")
                onResult(false)
                return@addOnSuccessListener
            }

            val field = if (upvote) "upvotes" else "downvotes"
            val poiRef = db.collection(COLLECTION).document(poiRefId)

            db.runTransaction { tx ->
                val snapshot = tx.get(poiRef)
                val current = (snapshot.getLong(field) ?: 0) + 1
                tx.update(poiRef, field, current)
                tx.set(
                    voteRef,
                    mapOf("vote" to upvote, "ts" to System.currentTimeMillis()),
                    SetOptions.merge(),
                )
            }
                .addOnSuccessListener {
                    cachedPois.replaceAll { poi ->
                        if (poi.id == poiRefId) {
                            if (upvote) poi.copy(upvotes = poi.upvotes + 1)
                            else poi.copy(downvotes = poi.downvotes + 1)
                        } else poi
                    }
                    saveCache()
                    onResult(true)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Vote failed", e)
                    onResult(false)
                }
        }
    }

    fun vote(poiId: String, upvote: Boolean, onResult: (Boolean) -> Unit) {
        withAuthenticatedUser(onResult) { uid ->
            persistVote(poiId, uid, upvote, onResult)
        }
    }

    private fun saveCache() {
        val arr = JSONArray()
        cachedPois.forEach { poi ->
            arr.put(JSONObject().apply {
                put("id", poi.id)
                put("lat", poi.lat)
                put("lng", poi.lng)
                put("type", poi.type.id)
                put("name", poi.name)
                put("createdBy", poi.createdBy)
                put("createdAt", poi.createdAt)
                put("geoHash", poi.geoHash)
                put("upvotes", poi.upvotes)
                put("downvotes", poi.downvotes)
            })
        }
        prefs.edit().putString(KEY_POIS, arr.toString()).apply()
    }

    private fun loadCache() {
        val json = prefs.getString(KEY_POIS, null) ?: return
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val type = PoiType.fromId(obj.getString("type")) ?: continue
                cachedPois.add(Poi(
                    id = obj.getString("id"),
                    lat = obj.getDouble("lat"),
                    lng = obj.getDouble("lng"),
                    type = type,
                    name = obj.optString("name", ""),
                    createdBy = obj.optString("createdBy", ""),
                    createdAt = obj.optLong("createdAt", 0L),
                    geoHash = obj.optString("geoHash", ""),
                    upvotes = obj.optInt("upvotes", 0),
                    downvotes = obj.optInt("downvotes", 0),
                ))
            }
            Log.i(TAG, "Loaded ${cachedPois.size} POIs from cache")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load cache", e)
        }
    }
}
