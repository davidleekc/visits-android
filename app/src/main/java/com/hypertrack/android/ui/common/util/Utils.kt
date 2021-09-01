package com.hypertrack.android.ui.common.util

import android.app.Activity
import android.util.Patterns
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import com.hypertrack.android.utils.MyApplication

object Utils {
    fun hideKeyboard(activity: Activity) {
        val inputMethodManager =
            activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        activity.currentFocus?.let {
            inputMethodManager.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }

    fun showKeyboard(activity: Activity, view: View? = null) {
        view?.requestFocus()
        val inputMethodManager =
            MyApplication.context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showSoftInput(activity.currentFocus, 0)
    }

    fun isDoneAction(actionId: Int, event: KeyEvent?): Boolean {
        return actionId == EditorInfo.IME_ACTION_DONE
                || actionId == EditorInfo.IME_ACTION_SEARCH
                || actionId == EditorInfo.IME_ACTION_SEND
                || actionId == EditorInfo.IME_ACTION_NEXT
                || (event?.action == KeyEvent.ACTION_UP && event.keyCode == KeyEvent.KEYCODE_ENTER)
    }

}

fun String?.nullIfEmpty(): String? {
    return if (this?.isEmpty() == true) null else this
}

fun String?.nullIfBlank(): String? {
    return if (this?.isBlank() == true) null else this
}

fun <T, R> List<T>.toMap(keyFunction: (T) -> R): Map<R, T> {
    val map = mutableMapOf<R, T>()
    forEach { map.put(keyFunction.invoke(it), it) }
    return map
}

fun <T, R> Set<T>.toMap(keyFunction: (T) -> R): Map<R, T> {
    val map = mutableMapOf<R, T>()
    forEach { map.put(keyFunction.invoke(it), it) }
    return map
}

fun String?.isEmail(): Boolean {
    return if (this != null) {
        return Patterns.EMAIL_ADDRESS.matcher(this).matches()
    } else false
}

