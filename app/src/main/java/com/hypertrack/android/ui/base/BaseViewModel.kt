package com.hypertrack.android.ui.base

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.navigation.NavDirections
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.logistics.android.github.R

open class BaseViewModel : ViewModel() {

    val destination = SingleLiveEvent<NavDirections>()
    val popBackStack = SingleLiveEvent<Boolean>()

    //todo remove loadingState form children and rename to loadingState
    open val loadingStateBase = MutableLiveData<Boolean>()
    open val errorBase = MutableLiveData<Consumable<String>>()

    protected val observers = mutableListOf<Pair<LiveData<*>, Observer<*>>>()

    protected fun <T> LiveData<T>.observeManaged(observer: Observer<T>) {
        observeForever(observer)
        observers.add(Pair(this, observer))
    }

    @Suppress("UNCHECKED_CAST")
    override fun onCleared() {
        observers.forEach { it.first.removeObserver(it.second as Observer<Any>) }
    }
}