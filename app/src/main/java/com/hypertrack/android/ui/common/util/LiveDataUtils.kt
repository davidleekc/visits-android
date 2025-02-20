package com.hypertrack.android.ui.common.util

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

fun <T> LiveData<T>.toHotTransformation(): HotLiveDataTransformation<T> {
    return HotLiveDataTransformation(this)
}

class HotLiveDataTransformation<T>(val liveData: LiveData<T>) {
    init {
        liveData.observeForever {}
    }
}

fun <T> LiveData<T>.requireValue(): T {
    return this.value!!
}

fun <T> MutableLiveData<T>.updateValue(value: T) {
    this.value = value
    postValue(value)
}