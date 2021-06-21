package com.hypertrack.android.ui.base

import androidx.lifecycle.*
import androidx.navigation.NavDirections
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.logistics.android.github.R

@Suppress("LeakingThis")
open class BaseViewModel(
    //todo inject everywhere
    private val osUtilsProvider: OsUtilsProvider? = null
) : ViewModel() {

    val destination = SingleLiveEvent<NavDirections>()
    val popBackStack = SingleLiveEvent<Boolean>()

    //todo remove loadingState from children and rename to loadingState
    open val loadingStateBase = MutableLiveData<Boolean>()

    open val errorBase = MutableLiveData<Consumable<String>>()

    //todo migrate to this scheme
    open val exception: MutableLiveData<Consumable<Exception>> =
        SingleLiveEvent<Consumable<Exception>>()
    open val errorText: MutableLiveData<Consumable<String>> by lazy {
        MediatorLiveData<Consumable<String>>().apply {
            addSource(exception) {
                postValue(it.map { e ->
                    osUtilsProvider?.getErrorMessage(e) ?: e.message ?: "Unknown error: No message"
                })
            }
        }
    }

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