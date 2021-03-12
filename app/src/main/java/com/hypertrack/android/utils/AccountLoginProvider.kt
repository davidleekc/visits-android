package com.hypertrack.android.utils

import android.content.Context
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobile.client.Callback
import com.amazonaws.mobile.client.UserStateDetails
import com.amazonaws.mobile.client.results.SignInResult
import com.amazonaws.mobile.client.results.SignInState
import com.amazonaws.mobile.client.results.SignUpResult
import com.amazonaws.mobile.client.results.Tokens
import com.amazonaws.services.cognitoidentityprovider.model.NotAuthorizedException
import com.amazonaws.services.cognitoidentityprovider.model.UserNotConfirmedException
import com.amazonaws.services.cognitoidentityprovider.model.UserNotFoundException
import com.hypertrack.android.interactors.LoginResult
import com.hypertrack.android.interactors.RegisterResult
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header

interface CognitoAccountLoginProvider {
    suspend fun awsInitCallWrapper(): AwsInitResult
    suspend fun awsTokenCallWrapper(): AwsTokenResult
    suspend fun awsSignUpCallWrapper(
        login: String,
        password: String,
        userAttributes: Map<String, String>
    ): AwsSignUpResult

    suspend fun awsLoginCallWrapper(login: String, password: String): AwsSignInResult
    fun signOut()
}

sealed class AwsSignInResult
object AwsSignInSuccess : AwsSignInResult()
object AwsSignInConfirmationRequired : AwsSignInResult()
class AwsSignInError(val exception: Exception) : AwsSignInResult()

sealed class AwsSignUpResult
object AwsSignUpConfirmationRequired : AwsSignUpResult()
object AwsSignUpSuccess : AwsSignUpResult()
class AwsSignUpError(val exception: Exception) : AwsSignUpResult()

sealed class AwsInitResult
object AwsSuccess : AwsInitResult()
class AwsError(val exception: Exception) : AwsInitResult()

sealed class AwsTokenResult
class CognitoToken(val token: String) : AwsTokenResult()
class CognitoTokenError(val exception: Exception) : AwsTokenResult()

@ExperimentalCoroutinesApi
class CognitoAccountLoginProviderImpl(private val ctx: Context, private val baseApiUrl: String) :
    CognitoAccountLoginProvider {

    override suspend fun awsInitCallWrapper(): AwsInitResult {
        return suspendCancellableCoroutine {
            AWSMobileClient.getInstance().initialize(ctx, object : Callback<UserStateDetails> {
                override fun onResult(result: UserStateDetails?) = it.resume(AwsSuccess) {}
                override fun onError(e: Exception) = it.resume(AwsError(e)) {}
            })
        }
    }


    override suspend fun awsLoginCallWrapper(login: String, password: String): AwsSignInResult {
        return suspendCancellableCoroutine {
            AWSMobileClient.getInstance().signIn(
                login,
                password,
                emptyMap(),
                object : Callback<SignInResult> {
                    override fun onResult(result: SignInResult) {
                        when (result.signInState) {
                            SignInState.DONE -> {
                                it.resume(AwsSignInSuccess) {}
                            }
                            else -> {
                                it.resume(AwsSignInError(Exception(result.signInState.toString()))) {}
                            }
                        }
                    }

                    override fun onError(e: Exception) {
                        if (e is UserNotConfirmedException) {
                            it.resume(AwsSignInConfirmationRequired) {}
                        } else {
                            it.resume(AwsSignInError(e)) {}
                        }
                    }
                })
        }
    }

    override suspend fun awsSignUpCallWrapper(
        login: String,
        password: String,
        userAttributes: Map<String, String>
    ): AwsSignUpResult {
        return suspendCancellableCoroutine {
            AWSMobileClient.getInstance().signUp(
                login,
                password,
                userAttributes,
                emptyMap(),
                object : Callback<SignUpResult> {
                    override fun onResult(result: SignUpResult) {
                        //true - user is confirmed, no further action is necessary
                        if (!result.confirmationState) {
                            it.resume(AwsSignUpConfirmationRequired) {}
                        } else {
                            it.resume(AwsSignUpSuccess) {}
                        }
                    }

                    override fun onError(e: Exception) {
                        it.resume(AwsSignUpError(e)) {}
                    }
                })
        }
    }

    override suspend fun awsTokenCallWrapper(): AwsTokenResult {
        return suspendCancellableCoroutine {
            AWSMobileClient.getInstance().getTokens(object : Callback<Tokens> {
                override fun onResult(result: Tokens) {
                    it.resume(CognitoToken(result.idToken.tokenString)) {}
                }

                override fun onError(e: Exception) = it.resume(CognitoTokenError(e)) {}
            })
        }
    }

    override fun signOut() {
        AWSMobileClient.getInstance().signOut()
    }

    companion object {
        const val TAG = "CognitoAccountProvider"
    }
}

interface TokenForPublishableKeyExchangeService {
    @GET("api-key")
    suspend fun getPublishableKey(@Header("Authorization") token: String): Response<PublishableKeyContainer>
}

@JsonClass(generateAdapter = true)
data class PublishableKeyContainer(@field:Json(name = "key") val publishableKey: String?)