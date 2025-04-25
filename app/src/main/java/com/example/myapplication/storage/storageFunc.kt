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

fun saveAmis(context: Context, amis: List<Friend>) {
    val json = gson.toJson(amis)
    getPrefs(context).edit { putString("amis", json) }
}

fun getAmis(context: Context): List<Friend> {
    val json = getPrefs(context).getString("amis", null) ?: return emptyList()
    val type = object : TypeToken<List<Friend>>() {}.type
    return gson.fromJson(json, type)
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

fun saveVoyages(context: Context, voyages: List<Travel>) {
    val json = gson.toJson(voyages)
    getPrefs(context).edit { putString("voyages", json) }
}

fun getVoyages(context: Context): List<Travel> {
    val json = getPrefs(context).getString("voyages", null) ?: return emptyList()
    val type = object : TypeToken<List<Travel>>() {}.type
    return gson.fromJson(json, type)
}

// ---------------------- GROUPES D'AMIS ----------------------

fun saveGroupes(context: Context, groupes: List<Group>) {
    val json = gson.toJson(groupes)
    getPrefs(context).edit { putString("groupes", json) }
}

fun getGroupes(context: Context): List<Group> {
    val json = getPrefs(context).getString("groupes", null) ?: return emptyList()
    val type = object : TypeToken<List<Group>>() {}.type
    return gson.fromJson(json, type)
}
