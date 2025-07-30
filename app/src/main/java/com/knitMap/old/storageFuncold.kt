package com.knitMap.old

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.core.net.toUri
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
// 1. removeFriendByName avec clé "amis"
fun removeFriendByName(context: Context, friendName: String) {
    val prefs = getPrefs(context)
    val json = prefs.getString("amis", null) ?: return
    val type = object : TypeToken<List<Friend>>() {}.type
    val friends: MutableList<Friend> = Gson().fromJson(json, type)

    val updatedFriends = friends.filterNot { it.title == friendName }

    prefs.edit { putString("amis", Gson().toJson(updatedFriends)) }
}
// ---------------------- PINGS ----------------------

fun saveDiscoveries(context: Context, discoveries: List<Discovery>) {
    val json = gson.toJson(discoveries)
    getPrefs(context).edit { putString("discoveries", json) }

    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid ?: return
    val db = FirebaseFirestore.getInstance()

    val pingCollection = db.collection("pings")

    Log.d("PingDebug", "Nombre de discoveries à sauvegarder : ${discoveries.size}")
    if (!isOnline) return
    for (discovery in discoveries) {
        Log.d("PingDebug", "Saving discovery with UUID: ${discovery.uuid}")

        // Utilisez imageUri pour sauvegarder l'URI complet de l'image
        val pingData = hashMapOf(
            "userId" to userId,
            "uuid" to discovery.uuid,
            "title" to discovery.title,
            "description" to discovery.description,
            "latitude" to discovery.latitude,
            "longitude" to discovery.longitude,
            "date" to discovery.date,
            "imageUri" to discovery.imageUri, // Sauvegarder l'URI complet de l'image
            "locationName" to discovery.locationName
        )

        pingCollection.document(discovery.uuid).set(pingData)
            .addOnSuccessListener {
                Log.d("PingDebug", "Saved discovery ${discovery.uuid}")
            }
            .addOnFailureListener { e ->
                Log.e("PingDebug", "Failed to save discovery ${discovery.uuid}", e)
            }
    }
}

fun updateDiscovery(context: Context, updatedDiscovery: Discovery) {
    val prefs = getPrefs(context)
    val json = prefs.getString("discoveries", null) ?: return
    val type = object : TypeToken<List<Discovery>>() {}.type
    val discoveries: MutableList<Discovery> = gson.fromJson(json, type)

    Log.d("UpdateDebug", "Updated URI: ${updatedDiscovery.imageUri}")

    val updatedList = discoveries.map {
        if (it.uuid == updatedDiscovery.uuid) updatedDiscovery else it
    }

    prefs.edit { putString("discoveries", gson.toJson(updatedList)) }
    if (!isOnline) return
    // Mise à jour dans Firebase
    val db = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val pingData = hashMapOf(
        "userId" to userId,
        "uuid" to updatedDiscovery.uuid,
        "title" to updatedDiscovery.title,
        "description" to updatedDiscovery.description,
        "latitude" to updatedDiscovery.latitude,
        "longitude" to updatedDiscovery.longitude,
        "date" to updatedDiscovery.date,
        "imageUri" to updatedDiscovery.imageUri ,// Assurez-vous d'inclure imageUri ici
        "locationName" to updatedDiscovery.locationName
    )

    db.collection("pings").document(updatedDiscovery.uuid).set(pingData)
        .addOnSuccessListener {
            Log.d("UpdateDebug", "Updated discovery ${updatedDiscovery.uuid} in Firebase.")
        }
        .addOnFailureListener { e ->
            Log.e("UpdateDebug", "Failed to update discovery ${updatedDiscovery.uuid} in Firebase", e)
        }
}

fun getDiscoveries(context: Context): List<Discovery> {
    val json = getPrefs(context).getString("discoveries", "[]")
    val type = object : TypeToken<List<Discovery>>() {}.type
    return gson.fromJson(json, type)
}

fun removeDiscoveryByUuid(context: Context, uuid: String) {
    val prefs = getPrefs(context)
    val json = prefs.getString("discoveries", null)
    if (json == null) {
        Log.e("PingDebug", "Aucune découverte locale à supprimer")
        return
    }
    val type = object : TypeToken<List<Discovery>>() {}.type
    val discoveries: MutableList<Discovery> = gson.fromJson(json, type)

    val updatedDiscoveries = discoveries.filterNot { it.uuid == uuid }
    Log.d("PingDebug", "Suppression locale : avant=${discoveries.size} après=${updatedDiscoveries.size}")

    prefs.edit { putString("discoveries", gson.toJson(updatedDiscoveries)) }
    if (!isOnline) return
    val db = FirebaseFirestore.getInstance()
    db.collection("pings").document(uuid)
        .delete()
        .addOnSuccessListener {
            Log.d("PingDebug", "Discovery supprimé dans Firestore : $uuid")
        }
        .addOnFailureListener { e ->
            Log.e("PingDebug", "Erreur suppression Firestore : $uuid", e)
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
                    val imageUri = doc.getString("imageUri") // Récupérer imageUri directement
                    Discovery(
                        uuid = doc.getString("uuid") ?: return@mapNotNull null,
                        title = doc.getString("title") ?: "",
                        description = doc.getString("description") ?: "",
                        latitude = doc.getDouble("latitude") ?: 0.0,
                        longitude = doc.getDouble("longitude") ?: 0.0,
                        date = doc.getString("date") ?: "",
                        imageUri = imageUri, // Charger imageUri
                        locationName = doc.getString("locationName") ?: ""
                    )
                } catch (e: Exception) {
                    Log.e("SyncDebug", "Erreur parsing Discovery", e)
                    null
                }
            }

            val localDiscoveries = getDiscoveries(context).toMutableList()
            val localUuids = localDiscoveries.map { it.uuid }.toSet()
            val remoteUuids = remoteDiscoveries.map { it.uuid }.toSet()

            val newFromRemote = remoteDiscoveries.filter { it.uuid !in localUuids }
            if (newFromRemote.isNotEmpty()) {
                localDiscoveries.addAll(newFromRemote)
                Log.d("SyncDebug", "Importé ${newFromRemote.size} découvertes depuis Firebase.")
            }

            val missingInRemote = localDiscoveries.filter { it.uuid !in remoteUuids }
            for (discovery in missingInRemote) {
                discovery.imageUri?.toUri()?.lastPathSegment

                val pingData = hashMapOf(
                    "userId" to userId,
                    "uuid" to discovery.uuid,
                    "title" to discovery.title,
                    "description" to discovery.description,
                    "latitude" to discovery.latitude,
                    "longitude" to discovery.longitude,
                    "date" to discovery.date,
                    "imageUri" to discovery.imageUri,  // Sauvegarder imageUri
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

            saveDiscoveries(context, localDiscoveries)
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
// 1. removeGroupByName avec clé "groupes"
fun removeGroupByName(context: Context, groupName: String) {
    val prefs = getPrefs(context)
    val json = prefs.getString("groupes", null) ?: return
    val type = object : TypeToken<List<Group>>() {}.type
    val groups: MutableList<Group> = Gson().fromJson(json, type)

    val updatedGroups = groups.filterNot { it.title == groupName }

    prefs.edit { putString("groupes", Gson().toJson(updatedGroups)) }
}

// ---------------------- Scratch ----------------------

fun saveScratchedPoints(context: Context, scratchedPoints: List<GeoPoint>) {
    val json = gson.toJson(scratchedPoints)
    getPrefs(context).edit { putString("scratchedPoints", json) }
}

fun getScratchedPoints(context: Context): List<GeoPoint> {
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

            val localPoints = getScratchedPoints(context).toMutableList()

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
            saveScratchedPoints(context, localPoints)
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

//---------------------- PendingAction ----------------------