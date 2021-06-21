package com.hypertrack.android.ui.common

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

class ManagedObserver {

    private val observers = mutableListOf<Pair<LiveData<*>, Observer<*>>>()

    fun <T> observeManaged(liveData: LiveData<T>, observer: Observer<T>) {
        liveData.observeForever(observer)
        observers.add(Pair(liveData, observer))
    }

    @Suppress("UNCHECKED_CAST")
    fun onCleared() {
        observers.forEach { it.first.removeObserver(it.second as Observer<Any>) }
    }

}