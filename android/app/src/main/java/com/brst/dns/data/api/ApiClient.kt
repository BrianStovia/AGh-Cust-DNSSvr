package com.brst.dns.data.api

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class ApiClient {

    private val gson: Gson = GsonBuilder().setLenient().create()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val okHttpClient: OkHttpClient by lazy {
        createUnsafeOkHttpClient()
    }

    private fun createUnsafeOkHttpClient(): OkHttpClient {
        val trustAllCerts = arrayOf<TrustManager>(
            object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            }
        )

        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, SecureRandom())

        return OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    suspend fun <T> get(baseUrl: String, path: String, user: String, pass: String, clazz: Class<T>): Result<T> {
        return withContext(Dispatchers.IO) {
            try {
                val fullUrl = "${baseUrl.trimEnd('/')}/$path"
                val requestBuilder = Request.Builder().url(fullUrl).get()

                if (user.isNotBlank() && pass.isNotBlank()) {
                    requestBuilder.addHeader("Authorization", Credentials.basic(user, pass))
                }

                val response = okHttpClient.newCall(requestBuilder.build()).execute()
                val bodyString = response.body?.string()

                if (response.isSuccessful && bodyString != null) {
                    val data = gson.fromJson(bodyString, clazz)
                    Result.success(data)
                } else {
                    Result.failure(Exception("HTTP ${response.code}: ${response.message} - $bodyString"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun <T> post(baseUrl: String, path: String, user: String, pass: String, body: Any?, clazz: Class<T>): Result<T> {
        return withContext(Dispatchers.IO) {
            try {
                val fullUrl = "${baseUrl.trimEnd('/')}/$path"
                val requestBody = if (body != null) {
                    val json = gson.toJson(body)
                    json.toRequestBody(jsonMediaType)
                } else {
                    "".toRequestBody(jsonMediaType)
                }

                val requestBuilder = Request.Builder().url(fullUrl).post(requestBody)

                if (user.isNotBlank() && pass.isNotBlank()) {
                    requestBuilder.addHeader("Authorization", Credentials.basic(user, pass))
                }

                val response = okHttpClient.newCall(requestBuilder.build()).execute()
                val bodyString = response.body?.string()

                if (response.isSuccessful && bodyString != null) {
                    val data = gson.fromJson(bodyString, clazz)
                    Result.success(data)
                } else {
                    Result.failure(Exception("HTTP ${response.code}: ${response.message} - $bodyString"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun postRaw(baseUrl: String, path: String, user: String, pass: String, body: Any?): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val fullUrl = "${baseUrl.trimEnd('/')}/$path"
                val requestBody = if (body != null) {
                    val json = gson.toJson(body)
                    json.toRequestBody(jsonMediaType)
                } else {
                    "".toRequestBody(jsonMediaType)
                }

                val requestBuilder = Request.Builder().url(fullUrl).post(requestBody)

                if (user.isNotBlank() && pass.isNotBlank()) {
                    requestBuilder.addHeader("Authorization", Credentials.basic(user, pass))
                }

                val response = okHttpClient.newCall(requestBuilder.build()).execute()
                val bodyString = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    Result.success(bodyString)
                } else {
                    Result.failure(Exception("HTTP ${response.code}: ${response.message} - $bodyString"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
