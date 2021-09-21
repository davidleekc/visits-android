package com.hypertrack.android.ui.base

import androidx.annotation.StringRes
import androidx.lifecycle.*
import androidx.navigation.NavDirections
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.android.utils.format
import com.hypertrack.logistics.android.github.R

@Suppress("LeakingThis")
open class BaseViewModel(
    //todo inject everywhere
    private val osUtilsProvider: OsUtilsProvider? = null
) : ViewModel() {

    val destination = SingleLiveEvent<NavDirections>()
    val popBackStack = SingleLiveEvent<Boolean>()

    //todo migrate to this
    open val errorHandler = ErrorHandler(osUtilsProvider)

    //todo remove loadingState from children and rename to loadingState
    open val loadingStateBase = MutableLiveData<Boolean>()

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

class ErrorHandler(
    private val osUtilsProvider: OsUtilsProvider?,
    private val exceptionSource: LiveData<Consumable<Exception>>? = null,
    private val errorTextSource: LiveData<Consumable<String>>? = null
) {

    val exception: LiveData<Consumable<Exception>>
        get() = _exception
    private val _exception = MediatorLiveData<Consumable<Exception>>().apply {
        exceptionSource?.let {
            addSource(exceptionSource) {
                postValue(it)
            }
        }
    }

    val errorText: LiveData<Consumable<String>>
        get() = _errorText
    private val _errorText = MediatorLiveData<Consumable<String>>().apply {
        errorTextSource?.let {
            addSource(errorTextSource) {
                postValue(it)
            }
        }
        addSource(_exception) {
            postValue(it.map {
                osUtilsProvider?.getErrorMessage(it) ?: it.format()
            })
        }
    }

    fun postConsumable(e: Consumable<Exception>) {
        _exception.postValue(e)
    }

    fun postException(e: Exception) {
        _exception.postValue(Consumable(e))
    }

    fun postText(e: String) {
        _errorText.postValue(Consumable(e))
    }

    fun postText(@StringRes res: Int) {
        _errorText.postValue(Consumable(osUtilsProvider!!.stringFromResource(res)))
    }
}