package com.knitMap.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.io.InputStreamReader
import java.io.BufferedReader

// Fonction de géocodage avec Nominatim en utilisant Kotlin Coroutines
suspend fun getLocationFromCoordinates(
    latitude: Double,
    longitude: Double,
    onResult: (String) -> Unit
) {
    val url = "https://nominatim.openstreetmap.org/reverse?format=json&lat=$latitude&lon=$longitude&addressdetails=1&zoom=18"

    try {
        withContext(Dispatchers.IO) {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connect()

            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val response = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                response.append(line)
            }
            reader.close()

            val jsonResponse = JSONObject(response.toString())
            val address = jsonResponse.getJSONObject("address")

            val city = address.optString("city", "")
            val town = address.optString("town", "")
            val village = address.optString("village", "")
            val state = address.optString("state", "")

            val place = when {
                city.isNotBlank() -> city
                town.isNotBlank() -> town
                village.isNotBlank() -> village
                else -> ""
            }

            val result = when {
                place.isBlank() && state.isBlank() -> "Lieu inconnu"
                place.isBlank() -> state
                state.isBlank() -> place
                else -> "$place, $state"
            }

            withContext(Dispatchers.Main) {
                onResult(result)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        withContext(Dispatchers.Main) {
            onResult("Lieu inconnu")
        }
    }
}


fun isConnectedToInternet(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetwork ?: return false
    val capabilities = cm.getNetworkCapabilities(network) ?: return false

    if (!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) return false

    // Test HTTP réel pour valider la connexion (latence, accès sortant)
    return try {
        val url = URL("https://clients3.google.com/generate_204") // réponse 204 très rapide
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 1500  // maximum 1,5 seconde
        connection.readTimeout = 1500
        connection.connect()
        connection.responseCode == 204
    } catch (e: Exception) {
        false
    }
}

fun isConnectedToInternetFast(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetwork ?: return false
    val capabilities = cm.getNetworkCapabilities(network) ?: return false

    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}



