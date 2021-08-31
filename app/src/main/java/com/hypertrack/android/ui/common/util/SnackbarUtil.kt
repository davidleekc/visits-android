package com.hypertrack.android.ui.common.util

import android.view.View
import android.widget.TextView
import com.google.android.material.snackbar.Snackbar
import com.hypertrack.android.ui.base.Consumable
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.logistics.android.github.R


object SnackbarUtil {

    fun showErrorSnackbar(view: View, errorText: String?) {
        errorText.toString().let { text ->
            val snackbar = Snackbar.make(view, text, Snackbar.LENGTH_INDEFINITE).apply {
                setAction(MyApplication.context.getString(R.string.close)) {
                    dismiss()
                }
                setActionTextColor(MyApplication.context.getColor(R.color.colorRed))
            }
            try {
                val snackbarView = snackbar.view
                snackbarView.setOnClickListener {
                    ClipboardUtil.copyToClipboard(text)
                }
                val snackTextView = snackbarView.findViewById<View>(
                    com.google.android.material.R.id.snackbar_text
                ) as TextView
                snackTextView.maxLines = 20
            } catch (_: Exception) {
            }
            snackbar.show()
        }
    }

    fun showErrorSnackbar(view: View, errorTextConsumable: Consumable<String>) {
        errorTextConsumable.consume {
            showErrorSnackbar(view, it)
        }
    }

}