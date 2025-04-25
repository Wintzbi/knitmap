package com.example.myapplication.storage

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

import com.example.myapplication.friend.Friend
import com.example.myapplication.travel.Travel
import com.example.myapplication.group.Group
import androidx.core.content.edit
import kotlin.apply
import com.example.myapplication.discovery.Discovery

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
    prefs.edit().putString("friends", Gson().toJson(updatedFriends)).apply()
}

// ---------------------- PINGS ----------------------

fun saveDiscoveries(context: Context, discoveries: List<Discovery>) {
    val json = gson.toJson(discoveries)
    getPrefs(context).edit { putString("discoveries", json) }
}
fun getDiscoveries(context: Context): List<Discovery> {
    val json = getPrefs(context).getString("discoveries", "[]")
    val type = object : TypeToken<List<Discovery>>() {}.type
    return gson.fromJson(json, type)
}



fun removeDiscoveryByCoordinates(context: Context, latitude: Double, longitude: Double) {
    val prefs = getPrefs(context)
    val json = prefs.getString("discoveries", null) ?: return
    val type = object : TypeToken<List<Discovery>>() {}.type
    val discoveries: MutableList<Discovery> = gson.fromJson(json, type)

    // Filtrer la liste pour enlever le Discovery correspondant aux coordonnées
    val updatedDiscoveries = discoveries.filterNot {
        it.latitude == latitude && it.longitude == longitude
    }

    // Sauvegarder la liste mise à jour
    prefs.edit().putString("discoveries", gson.toJson(updatedDiscoveries)).apply()
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
    prefs.edit().putString("travels", Gson().toJson(updatedTravels)).apply()
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
    prefs.edit().putString("groups", Gson().toJson(updatedGroups)).apply()
}