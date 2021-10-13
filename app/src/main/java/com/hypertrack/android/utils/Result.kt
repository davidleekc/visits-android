package com.hypertrack.android.utils

sealed class SimpleResult
object JustSuccess : SimpleResult()
class JustFailure(val exception: Exception) : SimpleResult()

sealed class Result<T>
class Success<T>(val result: T) : Result<T>()
class Failure<T>(val exception: Exception) : Result<T>()