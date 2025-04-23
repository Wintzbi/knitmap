package com.example.myapplication.storage

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

import com.example.myapplication.friend.Friend
import com.example.myapplication.travel.Travel
import com.example.myapplication.Ping
import com.example.myapplication.group.Group
import androidx.core.content.edit

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

fun savePings(context: Context, pings: List<Ping>) {
    val json = gson.toJson(pings)
    getPrefs(context).edit { putString("pings", json) }
}

fun getPings(context: Context): List<Ping> {
    val json = getPrefs(context).getString("pings", null) ?: return emptyList()
    val type = object : TypeToken<List<Ping>>() {}.type
    return gson.fromJson(json, type)
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
