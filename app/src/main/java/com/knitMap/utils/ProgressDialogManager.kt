package com.knitMap.utils

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.knitMap.R

object ProgressDialogManager {

    private var dialog: AlertDialog? = null
    private var updateCallback: ((Int, Int) -> Unit)? = null
    var isCancelled = false
        private set

    // Affiche la ProgressDialog
    fun show(
        activity: Activity,
        title: String,
        onCancel: () -> Unit
    ) {
        if (dialog != null) {
            dismiss()  // Ferme et nettoie l'ancienne si elle est encore en mémoire
        }

        isCancelled = false

        val view: View = LayoutInflater.from(activity).inflate(R.layout.dialog_progress, null)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)
        val progressTextOverlay = view.findViewById<TextView>(R.id.progressTextOverlay)
        val cancelButton = view.findViewById<Button>(R.id.cancelButton)
        val titleTextView = view.findViewById<TextView>(R.id.title)

        val builder = AlertDialog.Builder(activity)
            .setTitle(null)
            .setView(view)
            .setCancelable(false)

        dialog = builder.create()
        dialog?.show()

        titleTextView?.text = title

        cancelButton.setOnClickListener {
            isCancelled = true
            dialog?.dismiss()
            onCancel()
        }

        updateCallback = { current, max ->
            progressBar.max = max
            progressBar.progress = current

            val percent = if (max > 0) (current * 100 / max) else 0
            progressTextOverlay.text = "$percent%"
        }
        updateCallback?.invoke(0, 1)
    }

    // Met à jour la ProgressDialog
    fun update(current: Int, max: Int) {
        updateCallback?.invoke(current, max)
    }

    // Met à jour le titre de la ProgressDialog
    fun updateTitle(title: String) {
        val titleTextView = dialog?.findViewById<TextView>(R.id.title)
        titleTextView?.text = title
    }

    fun dismiss() {
        dialog?.dismiss()
        dialog = null
        isCancelled = false
        updateCallback = null
    }

    fun isVisible(): Boolean {
        return dialog?.isShowing == true
    }
}

