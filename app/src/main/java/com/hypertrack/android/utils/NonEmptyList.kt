package com.hypertrack.android.utils

import com.hypertrack.logistics.android.github.R

class NonEmptyList<T>(first: T, rest: List<T> = listOf()) {

    val elements: List<T> = mutableListOf(first).apply {
        addAll(rest)
    }

    override fun toString(): String {
        return elements.toString()
    }

    companion object {
        fun <T> fromList(list: List<T>): NonEmptyList<T> {
            return NonEmptyList(list[0], list.subList(1, list.size))
        }
    }
}

fun <E> List<E>.asNonEmpty(): NonEmptyList<E> {
    return NonEmptyList.fromList(this)
}