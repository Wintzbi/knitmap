package com.knitMap.storage

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.knitMap.MainActivity.Companion.isOnline
import com.knitMap.discovery.Discovery
import com.knitMap.friend.Friend
import com.knitMap.group.Group
import com.knitMap.travel.Travel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.osmdroid.util.GeoPoint
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions

private const val PREF_NAME = "my_complex_data"
private val gson = Gson()

private fun getPrefs(context: Context): SharedPreferences {
    return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
}

// ---------------------- AMIS ----------------------

fun saveFriends(context: Context, amis: List<Friend>) {
    val json = gson.toJson(amis)
    getPrefs(context).edit { putString("amis", json) }
}

fun getFriends(context: Context): List<Friend> {
    val json = getPrefs(context).getString("amis", null) ?: return emptyList()
    val type = object : TypeToken<List<Friend>>() {}.type
    return gson.fromJson(json, type)
}

fun removeFriendByName(context: Context, friendName: String) {
    val prefs = getPrefs(context)
    val json = prefs.getString("amis", null) ?: return
    val type = object : TypeToken<List<Friend>>() {}.type
    val friends: MutableList<Friend> = Gson().fromJson(json, type)

    val updatedFriends = friends.filterNot { it.title == friendName }

    prefs.edit { putString("amis", Gson().toJson(updatedFriends)) }
}

// ========== DISCOVERIES ==========

// ========== 🔒 Local ==========
fun saveDiscoveriesLocally(context: Context, discoveries: List<Discovery>) {
    val json = gson.toJson(discoveries)
    getPrefs(context).edit { putString("discoveries", json) }

    if (!isOnline) {
        for (discoverie in discoveries) {
            queuePendingActionDiscoverie(context, PendingActionDiscoverie("add", discoverie))
        }
        return
    }
    else{
        saveDiscoveriesToFirebase(discoveries)
    }
}

fun getDiscoveriesLocally(context: Context): List<Discovery> {
    val json = getPrefs(context).getString("discoveries", "[]")
    val type = object : TypeToken<List<Discovery>>() {}.type
    return gson.fromJson(json, type)
}

fun updateDiscoveryLocally(context: Context, updatedDiscovery: Discovery) {
    val json = getPrefs(context).getString("discoveries", "[]")
    val type = object : TypeToken<List<Discovery>>() {}.type
    val discoveries: MutableList<Discovery> = gson.fromJson(json, type)

    val updatedDiscoveries = discoveries.map {
        if (it.uuid == updatedDiscovery.uuid) updatedDiscovery else it
    }

    val updatedJson = gson.toJson(updatedDiscoveries)
    getPrefs(context).edit { putString("discoveries", updatedJson) }

    if (!isOnline) {
        queuePendingActionDiscoverie(context, PendingActionDiscoverie("update", updatedDiscovery))
    } else {
        updateDiscoveryInFirebase(updatedDiscovery)
    }
}

fun removeDiscoveryByUuidLocally(context: Context, uuid: String) {
    val json = getPrefs(context).getString("discoveries", "[]")
    val type = object : TypeToken<List<Discovery>>() {}.type
    val discoveries: MutableList<Discovery> = gson.fromJson(json, type)

    val updatedDiscoveries = discoveries.filterNot { it.uuid == uuid }

    val updatedJson = gson.toJson(updatedDiscoveries)
    getPrefs(context).edit { putString("discoveries", updatedJson) }

    if (!isOnline) {
        queuePendingActionDiscoverie(context, PendingActionDiscoverie("delete", uuid = uuid))
    } else {
        removeDiscoveryByUuidFromFirebase(uuid)
    }
}

// ========== ☁️ Firebase ==========

fun saveDiscoveriesToFirebase(discoveries: List<Discovery>) {
    if (!isOnline) return
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid ?: return
    val db = FirebaseFirestore.getInstance()

    for (discovery in discoveries) {
        val data = hashMapOf(
            "userId" to userId,
            "uuid" to discovery.uuid,
            "title" to discovery.title,
            "description" to discovery.description,
            "latitude" to discovery.latitude,
            "longitude" to discovery.longitude,
            "date" to discovery.date,
            "imageUri" to discovery.imageUri,
            "locationName" to discovery.locationName
        )

        db.collection("pings").document(discovery.uuid).set(data)
            .addOnSuccessListener {
                Log.d("PingDebug", "Saved discovery ${discovery.uuid} to Firebase")
            }
            .addOnFailureListener { e ->
                Log.e("PingDebug", "Failed to save discovery ${discovery.uuid}", e)
            }
    }
}

fun updateDiscoveryInFirebase(discovery: Discovery) {
    if (!isOnline) return
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid ?: return
    val db = FirebaseFirestore.getInstance()

    // Création d'une map avec tous les champs, y compris nulls
    val fullMap = mapOf(
        "title" to discovery.title,
        "description" to discovery.description,
        "latitude" to discovery.latitude,
        "longitude" to discovery.longitude,
        "date" to discovery.date,
        "imageUri" to discovery.imageUri,
        "locationName" to discovery.locationName
    )

    // Ne conserver que les champs non-nuls pour éviter les suppressions
    val safeMap = fullMap.filterValues { it != null }

    db.collection("pings")
        .document(discovery.uuid)
        .set(safeMap, SetOptions.merge())
        .addOnSuccessListener {
            Log.d("PingDebug", "Discovery ${discovery.uuid} updated safely (merge, no nulls)")
        }
        .addOnFailureListener { e ->
            Log.e("PingDebug", "Failed to safely update discovery ${discovery.uuid}", e)
        }
}

fun removeDiscoveryByUuidFromFirebase(uuid: String) {
    if (!isOnline) return
    val db = FirebaseFirestore.getInstance()

    db.collection("pings").document(uuid)
        .delete()
        .addOnSuccessListener {
            Log.d("PingDebug", "Deleted discovery $uuid from Firebase")
        }
        .addOnFailureListener { e ->
            Log.e("PingDebug", "Failed to delete discovery $uuid", e)
        }
}

fun checkDiscoveryExistsInFirebase(uuid: String, callback: (Boolean) -> Unit) {
    val db = FirebaseFirestore.getInstance()

    db.collection("pings").document(uuid)
        .get()
        .addOnSuccessListener { document ->
            callback(document.exists())
        }
        .addOnFailureListener { e ->
            Log.e("FirebaseCheck", "Erreur de vérification d'existence pour $uuid", e)
            callback(false) // On considère que ça n'existe pas en cas d'erreur
        }
}

fun fetchAndSyncDiscoveriesWithProgress(
    context: Context,
    onProgress: (current: Int, max: Int) -> Unit,
    isCancelled: () -> Boolean,
    onComplete: () -> Unit,
    onFailure: (Exception) -> Unit
) {
    if (!isOnline) return
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid ?: return
    val db = FirebaseFirestore.getInstance()

    db.collection("pings")
        .whereEqualTo("userId", userId)
        .get()
        .addOnSuccessListener { result ->
            val docs = result.documents
            val total = docs.size
            val remoteDiscoveries = mutableListOf<Discovery>()

            // Mise à jour de la progression globale
            for ((index, doc) in docs.withIndex()) {
                if (isCancelled()) return@addOnSuccessListener
                // Mise à jour de la progression à chaque itération
                onProgress(index + 1, total)

                try {
                    val discovery = Discovery(
                        uuid = doc.getString("uuid") ?: continue,
                        title = doc.getString("title") ?: "",
                        description = doc.getString("description") ?: "",
                        latitude = doc.getDouble("latitude") ?: 0.0,
                        longitude = doc.getDouble("longitude") ?: 0.0,
                        date = doc.getString("date") ?: "",
                        imageUri = doc.getString("imageUri"),
                        locationName = doc.getString("locationName") ?: ""
                    )
                    remoteDiscoveries.add(discovery)
                } catch (e: Exception) {
                    Log.e("SyncProgress", "Erreur parsing", e)
                    continue
                }
            }

            // Récupération des découvertes locales
            val localDiscoveries = getDiscoveriesLocally(context).toMutableList()
            val localUuids = localDiscoveries.map { it.uuid }.toSet()
            val remoteUuids = remoteDiscoveries.map { it.uuid }.toSet()

            // Ajout des découvertes manquantes depuis Firebase
            val newFromRemote = remoteDiscoveries.filter { it.uuid !in localUuids }
            localDiscoveries.addAll(newFromRemote)

            // Ajout des découvertes locales manquantes dans Firebase
            val missingInRemote = localDiscoveries.filter { it.uuid !in remoteUuids }

            // Mise à jour de la progression des découvertes manquantes à uploader
            val uploadTotal = missingInRemote.size
            for ((uploadIndex, discovery) in missingInRemote.withIndex()) {
                if (isCancelled()) return@addOnSuccessListener
                onProgress(uploadIndex + 1, uploadTotal)

                val pingData = hashMapOf(
                    "userId" to userId,
                    "uuid" to discovery.uuid,
                    "title" to discovery.title,
                    "description" to discovery.description,
                    "latitude" to discovery.latitude,
                    "longitude" to discovery.longitude,
                    "date" to discovery.date,
                    "imageUri" to discovery.imageUri,
                    "locationName" to discovery.locationName
                )

                // Sauvegarde de la découverte sur Firebase
                db.collection("pings").document(discovery.uuid).set(pingData)
            }

            // Sauvegarde des découvertes locales après ajout de celles manquantes depuis Firebase
            saveDiscoveriesLocally(context, localDiscoveries)

            // Appel de onComplete à la fin de la synchronisation
            onComplete()
        }
        .addOnFailureListener { e ->
            Log.e("SyncProgress", "Erreur Firebase", e)
            onFailure(e)
        }
}

fun fetchAndSyncDiscoveriesWithFirebase(
    context: Context,
    onComplete: (() -> Unit)? = null,
    onFailure: ((Exception) -> Unit)? = null
) {
    if (!isOnline) return
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid ?: return
    val db = FirebaseFirestore.getInstance()

    db.collection("pings")
        .whereEqualTo("userId", userId)
        .get()
        .addOnSuccessListener { result ->
            val remoteDiscoveries = result.documents.mapNotNull { doc ->
                try {
                    val imageUri = doc.getString("imageUri")
                    Discovery(
                        uuid = doc.getString("uuid") ?: return@mapNotNull null,
                        title = doc.getString("title") ?: "",
                        description = doc.getString("description") ?: "",
                        latitude = doc.getDouble("latitude") ?: 0.0,
                        longitude = doc.getDouble("longitude") ?: 0.0,
                        date = doc.getString("date") ?: "",
                        imageUri = imageUri,
                        locationName = doc.getString("locationName") ?: ""
                    )
                } catch (e: Exception) {
                    Log.e("SyncDebug", "Erreur parsing Discovery", e)
                    null
                }
            }

            val localDiscoveries = getDiscoveriesLocally(context).toMutableList()
            val localUuids = localDiscoveries.map { it.uuid }.toSet()
            val remoteUuids = remoteDiscoveries.map { it.uuid }.toSet()

            val newFromRemote = remoteDiscoveries.filter { it.uuid !in localUuids }
            if (newFromRemote.isNotEmpty()) {
                localDiscoveries.addAll(newFromRemote)
                Log.d("SyncDebug", "Importé ${newFromRemote.size} découvertes depuis Firebase.")
            }

            val missingInRemote = localDiscoveries.filter { it.uuid !in remoteUuids }
            for (discovery in missingInRemote) {
                val pingData = hashMapOf(
                    "userId" to userId,
                    "uuid" to discovery.uuid,
                    "title" to discovery.title,
                    "description" to discovery.description,
                    "latitude" to discovery.latitude,
                    "longitude" to discovery.longitude,
                    "date" to discovery.date,
                    "imageUri" to discovery.imageUri,
                    "locationName" to discovery.locationName
                )

                db.collection("pings").document(discovery.uuid).set(pingData)
                    .addOnSuccessListener {
                        Log.d("SyncDebug", "Exporté découverte ${discovery.uuid} vers Firebase.")
                    }
                    .addOnFailureListener { e ->
                        Log.e("SyncDebug", "Erreur export ${discovery.uuid}", e)
                    }
            }

            saveDiscoveriesLocally(context, localDiscoveries)

            // --- RESET des pending actions ici ---
            //flushPendingActionsDiscoverie(context)

            onComplete?.invoke()
        }
        .addOnFailureListener { e ->
            Log.e("SyncDebug", "Erreur récupération Firebase", e)
            onFailure?.invoke(e)
        }
}


// ---------------------- VOYAGES ----------------------

fun saveTravels(context: Context, voyages: List<Travel>) {
    val json = gson.toJson(voyages)
    getPrefs(context).edit { putString("voyages", json) }
}

fun getTravels(context: Context): List<Travel> {
    val json = getPrefs(context).getString("voyages", null) ?: return emptyList()
    val type = object : TypeToken<List<Travel>>() {}.type
    return gson.fromJson(json, type)
}

fun removeTravelByTitle(context: Context, travelTitle: String) {
    val prefs = getPrefs(context)
    val json = prefs.getString("voyages", null) ?: return
    val type = object : TypeToken<List<Travel>>() {}.type
    val travels: MutableList<Travel> = Gson().fromJson(json, type)

    val updatedTravels = travels.filterNot { it.title == travelTitle }

    prefs.edit { putString("voyages", Gson().toJson(updatedTravels)) }
}

// ---------------------- GROUPES D'AMIS ----------------------

fun saveGroups(context: Context, groupes: List<Group>) {
    val json = gson.toJson(groupes)
    getPrefs(context).edit { putString("groupes", json) }
}

fun getGroups(context: Context): List<Group> {
    val json = getPrefs(context).getString("groupes", null) ?: return emptyList()
    val type = object : TypeToken<List<Group>>() {}.type
    return gson.fromJson(json, type)
}

fun removeGroupByName(context: Context, groupName: String) {
    val prefs = getPrefs(context)
    val json = prefs.getString("groupes", null) ?: return
    val type = object : TypeToken<List<Group>>() {}.type
    val groups: MutableList<Group> = Gson().fromJson(json, type)

    val updatedGroups = groups.filterNot { it.title == groupName }

    prefs.edit { putString("groupes", Gson().toJson(updatedGroups)) }
}

// ---------------------- Scratch ----------------------

fun saveScratchedPointsLocally(context: Context, scratchedPoints: List<GeoPoint>) {
    val json = gson.toJson(scratchedPoints)
    getPrefs(context).edit { putString("scratchedPoints", json) }
}

fun getScratchedPointsLocally(context: Context): List<GeoPoint> {
    val json = getPrefs(context).getString("scratchedPoints", null) ?: return emptyList()
    val type = object : TypeToken<List<GeoPoint>>() {}.type
    return gson.fromJson(json, type)
}

fun saveScratchedPointsToFirebase(points: List<GeoPoint>) {
    if (!isOnline) return

    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid ?: return
    val db = FirebaseFirestore.getInstance()

    // Convertir chaque GeoPoint en Map<String, Double>
    val pointsMapList = points.map {
        mapOf("lat" to it.latitude, "lon" to it.longitude)
    }

    val data = hashMapOf(
        "userId" to userId,
        "points" to pointsMapList
    )

    db.collection("scratches").document(userId)
        .set(data)
        .addOnSuccessListener {
            Log.d("ScratchDebug", "Points grattés sauvegardés")
        }
        .addOnFailureListener { e ->
            Log.e("ScratchDebug", "Erreur sauvegarde points grattés", e)
        }
}

fun addScratchedPointToFirebase(point: GeoPoint) {
    if (!isOnline) return
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid ?: return
    val db = FirebaseFirestore.getInstance()

    // Convertir le GeoPoint en Map<String, Double>
    val pointMap = mapOf("lat" to point.latitude, "lon" to point.longitude)

    // Ajouter le point à la collection de l'utilisateur
    db.collection("scratches").document(userId)
        .update("points", FieldValue.arrayUnion(pointMap))
        .addOnSuccessListener {
            Log.d("ScratchDebug", "Point ajouté avec succès à Firebase")
        }
        .addOnFailureListener { e ->
            Log.e("ScratchDebug", "Erreur ajout du point à Firebase", e)
        }
}

fun cleanAndSortScratchedPoints(points: List<GeoPoint>): List<GeoPoint> {
    // Suppression des doublons en utilisant latitude et longitude comme critères
    val distinctPoints = points.distinctBy { "${it.latitude},${it.longitude}" }

    // Tri des points par latitude, puis longitude
    return distinctPoints.sortedWith(compareBy<GeoPoint> { it.latitude }.thenBy { it.longitude })
}

fun cleanAndSortScratchedPointsWithProgress(
    context: Context,
    onProgress: (current: Int, max: Int) -> Unit,
    isCancelled: () -> Boolean,
    onComplete: () -> Unit
) {
    val points = getScratchedPointsLocally(context)
    val seen = mutableSetOf<String>()
    val result = mutableListOf<GeoPoint>()
    val total = points.size

    for ((index, point) in points.withIndex()) {
        if (isCancelled()) return
        onProgress(index + 1, total)  // Mise à jour de la progression
        val key = "${point.latitude},${point.longitude}"
        if (key !in seen) {
            seen.add(key)
            result.add(point)
        }
    }

    val sorted = result.sortedWith(compareBy<GeoPoint> { it.latitude }.thenBy { it.longitude })
    saveScratchedPointsLocally(context, sorted)

    onComplete()
}

fun fetchAndSyncScratchesWithProgress(
    context: Context,
    onProgress: (current: Int, max: Int) -> Unit,
    isCancelled: () -> Boolean,
    onComplete: () -> Unit,
    onFailure: (Exception) -> Unit
) {
    if (!isOnline) return

    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid ?: return
    val db = FirebaseFirestore.getInstance()

    db.collection("scratches").document(userId)
        .get()
        .addOnSuccessListener { document ->
            val remotePoints = try {
                val pointsData = document.get("points") as? List<*>
                val total = pointsData?.size ?: 0
                pointsData?.mapIndexedNotNull { i, item ->
                    if (isCancelled()) return@addOnSuccessListener
                    onProgress(i + 1, total)  // Mise à jour de la progression
                    val map = item as? Map<*, *>
                    val lat = (map?.get("lat") as? Number)?.toDouble()
                    val lon = (map?.get("lon") as? Number)?.toDouble()
                    if (lat != null && lon != null) GeoPoint(lat, lon) else null
                } ?: emptyList()
            } catch (e: Exception) {
                Log.e("ScratchProgress", "Parsing error", e)
                emptyList()
            }

            val localPoints = getScratchedPointsLocally(context).toMutableList()

            val newFromRemote = remotePoints.filterNot { rp ->
                localPoints.any { lp -> lp.latitude == rp.latitude && lp.longitude == rp.longitude }
            }
            localPoints.addAll(newFromRemote)

            val newForRemote = localPoints.filterNot { lp ->
                remotePoints.any { rp -> lp.latitude == rp.latitude && lp.longitude == rp.longitude }
            }

            val allPoints = (remotePoints + newForRemote).distinctBy { "${it.latitude},${it.longitude}" }
            val pointsMapList = allPoints.map {
                mapOf("lat" to it.latitude, "lon" to it.longitude)
            }

            db.collection("scratches").document(userId)
                .set(mapOf("userId" to userId, "points" to pointsMapList))
                .addOnSuccessListener {
                    saveScratchedPointsLocally(context, localPoints)
                    onComplete()
                }
                .addOnFailureListener { onFailure(it) }
        }
        .addOnFailureListener { onFailure(it) }
}

fun fetchAndSyncScratchedPointsWithFirebase(
    context: Context,
    onComplete: (() -> Unit)? = null,
    onFailure: ((Exception) -> Unit)? = null
) {
    if (!isOnline) return
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid ?: return
    val db = FirebaseFirestore.getInstance()

    db.collection("scratches")
        .document(userId)
        .get()
        .addOnSuccessListener { document ->
            val remotePoints: List<GeoPoint> = try {
                val pointsDataRaw = document.get("points") as? List<*>
                pointsDataRaw?.mapNotNull { item ->
                    val map = item as? Map<*, *>
                    val lat = (map?.get("lat") as? Number)?.toDouble()
                    val lon = (map?.get("lon") as? Number)?.toDouble()
                    if (lat != null && lon != null) GeoPoint(lat, lon) else null
                } ?: emptyList()
            } catch (e: Exception) {
                Log.e("ScratchDebug", "Erreur parsing des points Firebase", e)
                emptyList()
            }

            val localPoints = getScratchedPointsLocally(context).toMutableList()

            // Comparaison par latitude/longitude
            fun GeoPoint.matches(other: GeoPoint) =
                this.latitude == other.latitude && this.longitude == other.longitude

            // Importer depuis Firebase
            val newFromRemote = remotePoints.filterNot { rp ->
                localPoints.any { lp -> lp.matches(rp) }
            }
            if (newFromRemote.isNotEmpty()) {
                localPoints.addAll(newFromRemote)
                Log.d("ScratchDebug", "Importé ${newFromRemote.size} points depuis Firebase.")
            }

            // Exporter vers Firebase
            val newForRemote = localPoints.filterNot { lp ->
                remotePoints.any { rp -> lp.matches(rp) }
            }

            if (newForRemote.isNotEmpty()) {
                // Fusion totale avant envoi
                val allPoints = (remotePoints + newForRemote).distinctBy { "${it.latitude},${it.longitude}" }

                val pointsMapList = allPoints.map {
                    mapOf("lat" to it.latitude, "lon" to it.longitude)
                }

                val data = hashMapOf(
                    "userId" to userId,
                    "points" to pointsMapList
                )

                db.collection("scratches").document(userId)
                    .set(data)
                    .addOnSuccessListener {
                        Log.d("ScratchDebug", "Exporté ${newForRemote.size} nouveaux points vers Firebase.")
                    }
                    .addOnFailureListener { e ->
                        Log.e("ScratchDebug", "Erreur export vers Firebase", e)
                        onFailure?.invoke(e)
                        return@addOnFailureListener
                    }
            } else {
                Log.d("ScratchDebug", "Aucun point local à exporter vers Firebase.")
            }

            // Sauvegarde fusionnée en local
            saveScratchedPointsLocally(context, localPoints)
            onComplete?.invoke()
        }
        .addOnFailureListener { e ->
            Log.e("ScratchDebug", "Erreur récupération Firebase", e)
            onFailure?.invoke(e)
        }
}

// ---------------------- LastknowPoint ----------------------
fun saveLastKnownPoint(context: Context, lastKnownPoint: GeoPoint) {
    val json = gson.toJson(lastKnownPoint)
    getPrefs(context).edit { putString("lastKnownPoint", json) }
}

fun getLastKnownPoint(context: Context): GeoPoint? {
    val json = getPrefs(context).getString("lastKnownPoint", null) ?: return null
    return gson.fromJson(json, GeoPoint::class.java)
}

fun removeLastKnownPoint(context: Context){
    getPrefs(context).edit { remove("lastKnownPoint")}
}

//---------------------- PendingAction Discoveries ----------------------

data class PendingActionDiscoverie(
    val type: String, // "add", "update", "delete"
    val discovery: Discovery? = null,
    val uuid: String? = null
)

fun queuePendingActionDiscoverie(context: Context, action: PendingActionDiscoverie) {
    val prefs = getPrefs(context)
    val json = prefs.getString("pending_discovery_actions", "[]")
    val type = object : TypeToken<MutableList<PendingActionDiscoverie>>() {}.type
    val actions: MutableList<PendingActionDiscoverie> = gson.fromJson(json, type)

    actions.add(action)
    prefs.edit { putString("pending_discovery_actions", gson.toJson(actions)) }
}

fun flushPendingActionsDiscoverie(context: Context) {
    if (!isOnline) return

    val prefs = getPrefs(context)
    val json = prefs.getString("pending_discovery_actions", "[]") ?: return
    val type = object : TypeToken<MutableList<PendingActionDiscoverie>>() {}.type
    val actions: MutableList<PendingActionDiscoverie> = gson.fromJson(json, type)

    for (action in actions) {
        when (action.type) {
            "add" -> action.discovery?.let {
                saveDiscoveriesToFirebase(listOf(it))
            }
            "update" -> action.discovery?.let { discovery ->
                checkDiscoveryExistsInFirebase(discovery.uuid) { exists ->
                    if (exists) {
                        updateDiscoveryInFirebase(discovery)
                    } else {
                        saveDiscoveriesToFirebase(listOf(discovery)) // fallback: créer si absent
                    }
                }
            }
            "delete" -> action.uuid?.let {
                removeDiscoveryByUuidFromFirebase(it)
            }
        }
    }


    prefs.edit { remove("pending_discovery_actions") }
}

fun flushPendingActionsDiscoverieWithProgress(
    context: Context,
    onProgress: (current: Int, max: Int) -> Unit,
    isCancelled: () -> Boolean,
    onComplete: () -> Unit
) {
    if (!isOnline) return

    val prefs = getPrefs(context)
    val json = prefs.getString("pending_discovery_actions", "[]") ?: return
    val type = object : TypeToken<MutableList<PendingActionDiscoverie>>() {}.type
    val actions: MutableList<PendingActionDiscoverie> = gson.fromJson(json, type)

    val total = actions.size
    for ((index, action) in actions.withIndex()) {
        if (isCancelled()) return
        onProgress(index + 1, total)  // Mise à jour de la progression

        when (action.type) {
            "add" -> action.discovery?.let {
                saveDiscoveriesToFirebase(listOf(it))
            }
            "update" -> action.discovery?.let { discovery ->
                checkDiscoveryExistsInFirebase(discovery.uuid) { exists ->
                    if (exists) {
                        updateDiscoveryInFirebase(discovery)
                    } else {
                        saveDiscoveriesToFirebase(listOf(discovery)) // fallback: créer si absent
                    }
                }
            }
            "delete" -> action.uuid?.let {
                removeDiscoveryByUuidFromFirebase(it)
            }
        }
    }

    prefs.edit { remove("pending_discovery_actions") }
    onComplete()
}

//---------------------- PendingAction Scratches ----------------------

data class PendingScratch(val latitude: Double, val longitude: Double)

fun queuePendingScratch(context: Context, point: GeoPoint) {
    val prefs = getPrefs(context)
    val json = prefs.getString("pending_scratches", "[]")
    val type = object : TypeToken<MutableList<PendingScratch>>() {}.type
    val queue: MutableList<PendingScratch> = gson.fromJson(json, type)

    queue.add(PendingScratch(point.latitude, point.longitude))
    prefs.edit { putString("pending_scratches", gson.toJson(queue)) }
}

fun flushPendingScratches(context: Context) {
    if (!isOnline) return

    val prefs = getPrefs(context)
    val json = prefs.getString("pending_scratches", "[]") ?: return
    val type = object : TypeToken<MutableList<PendingScratch>>() {}.type
    val queue: MutableList<PendingScratch> = gson.fromJson(json, type)

    val geoPoints = queue.map { GeoPoint(it.latitude, it.longitude) }
    if (geoPoints.isNotEmpty()) {
        saveScratchedPointsToFirebase(geoPoints)
        prefs.edit { remove("pending_scratches") }
    }
}

fun flushPendingScratchesWithProgress(
    context: Context,
    onProgress: (current: Int, max: Int) -> Unit,
    isCancelled: () -> Boolean,
    onComplete: () -> Unit
) {
    if (!isOnline) return

    val prefs = getPrefs(context)
    val json = prefs.getString("pending_scratches", "[]") ?: return
    val type = object : TypeToken<MutableList<PendingScratch>>() {}.type
    val queue: MutableList<PendingScratch> = gson.fromJson(json, type)

    val total = queue.size
    for ((index, item) in queue.withIndex()) {
        if (isCancelled()) return
        onProgress(index + 1, total)  // Mise à jour de la progression

        addScratchedPointToFirebase(GeoPoint(item.latitude, item.longitude))

        // Si c'est la dernière itération, on appelle onComplete
        if (index == total - 1) {
            onComplete()
        }
    }
}

//---------------------- Shader ----------------------
// Pour enregistrer la valeur
fun saveShaderState(context: Context, isOn: Boolean) {
    val prefs = getPrefs(context)
    prefs.edit { putBoolean("shader_is_on", isOn) }
}

// Pour la récupérer
fun loadShaderState(context: Context): Boolean {
    val prefs = getPrefs(context)
    return prefs.getBoolean("shader_is_on", false) // false = valeur par défaut
}