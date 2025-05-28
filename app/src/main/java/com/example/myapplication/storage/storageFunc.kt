package com.example.myapplication.storage

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.example.myapplication.discovery.Discovery
import com.example.myapplication.friend.Friend
import com.example.myapplication.group.Group
import com.example.myapplication.travel.Travel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.osmdroid.util.GeoPoint
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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
// Fonction pour supprimer un ami par son nom
fun removeFriendByName(context: Context, friendName: String) {
    val prefs = getPrefs(context)
    val json = prefs.getString("friends", null) ?: return
    val type = object : TypeToken<List<Friend>>() {}.type
    val friends: MutableList<Friend> = Gson().fromJson(json, type)

    // Filtrer la liste pour enlever l'ami correspondant au nom
    val updatedFriends = friends.filterNot {
        it.title == friendName
    }

    // Sauvegarder la liste mise à jour
    prefs.edit { putString("friends", Gson().toJson(updatedFriends)) }
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

    for (discovery in discoveries) {
        Log.d("PingDebug", "Saving discovery with UUID: ${discovery.uuid}")

        val pingData = hashMapOf(
            "userId" to userId,
            "uuid" to discovery.uuid,
            "title" to discovery.title,
            "description" to discovery.description,
            "latitude" to discovery.latitude,
            "longitude" to discovery.longitude,
            "date" to discovery.date,
            "uri" to discovery.imageUri
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
// Fonction pour supprimer un voyage par son titre
fun removeTravelByTitle(context: Context, travelTitle: String) {
    val prefs = getPrefs(context)
    val json = prefs.getString("travels", null) ?: return
    val type = object : TypeToken<List<Travel>>() {}.type
    val travels: MutableList<Travel> = Gson().fromJson(json, type)

    // Filtrer la liste pour enlever le voyage correspondant au titre
    val updatedTravels = travels.filterNot {
        it.title == travelTitle
    }

    // Sauvegarder la liste mise à jour
    prefs.edit { putString("travels", Gson().toJson(updatedTravels)) }
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
    val json = prefs.getString("groups", null) ?: return
    val type = object : TypeToken<List<Group>>() {}.type
    val groups: MutableList<Group> = Gson().fromJson(json, type)

    // Filtrer la liste pour enlever le groupe correspondant au nom
    val updatedGroups = groups.filterNot {
        it.title == groupName
    }

    // Sauvegarder la liste mise à jour
    prefs.edit { putString("groups", Gson().toJson(updatedGroups)) }
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