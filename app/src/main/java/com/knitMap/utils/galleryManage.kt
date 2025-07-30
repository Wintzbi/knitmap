package com.knitMap.utils

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun saveGalleryImageToAppFolder(context: Context, sourceUri: Uri): String? {
    return try {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "IMG_$timestamp.jpg"
        val picturesDir = context.getExternalFilesDir(null)
        val appDir = File(picturesDir, "KnitMapPictures")
        if (!appDir.exists()) appDir.mkdirs()
        val destFile = File(appDir, fileName)

        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
        destFile.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}