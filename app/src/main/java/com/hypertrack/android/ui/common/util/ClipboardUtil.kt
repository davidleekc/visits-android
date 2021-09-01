package com.hypertrack.android.ui.common.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.logistics.android.github.R

object ClipboardUtil {
    fun copyToClipboard(str: String) {
        val manager =
            MyApplication.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        manager.setPrimaryClip(ClipData.newPlainText(str, str))
        MyApplication.context.let {
            Toast.makeText(it, it.getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT)
                .show()
        }
    }
}