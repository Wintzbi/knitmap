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

    fun show(
        activity: Activity,
        title: String,
        onCancel: () -> Unit
    ) {

        if (dialog?.isShowing == true) return

        isCancelled = false

        val view: View = LayoutInflater.from(activity).inflate(R.layout.dialog_progress, null)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)
        val progressTextOverlay = view.findViewById<TextView>(R.id.progressTextOverlay)
        val cancelButton = view.findViewById<Button>(R.id.cancelButton)

        val builder = AlertDialog.Builder(activity)
            .setTitle(null)  // Le titre est dans le layout, pas la boîte de dialogue
            .setView(view)
            .setCancelable(false)

        dialog = builder.create()
        dialog?.show()

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
    }

    fun update(current: Int, max: Int) {
        updateCallback?.invoke(current, max)
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
