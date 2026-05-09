package com.opencode.notifier.api

import com.opencode.notifier.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class OpenCodeApiClient(
    private val serverUrl: String,
    private val username: String,
    private val password: String
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private val useAuth: Boolean get() = password.isNotBlank()

    private val credential: String
        get() = Credentials.basic(username, password)

    private fun Request.Builder.addAuth(): Request.Builder {
        if (useAuth) header("Authorization", credential)
        return this
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    fun eventStream(): Flow<SseEvent> = flow {
        var retryDelay = 1000L

        while (currentCoroutineContext().isActive) {
            try {
                AppLog.i("SSE", "Connecting to $serverUrl/global/event...")
                val request = Request.Builder()
                    .url("$serverUrl/global/event")
                    .addAuth()
                    .header("Accept", "text/event-stream")
                    .build()

                val response = client.newCall(request).execute()
                AppLog.i("SSE", "Connected! HTTP ${response.code} (retryDelay reset)")
                retryDelay = 1000L

                val reader = BufferedReader(
                    InputStreamReader(
                        response.body?.byteStream() ?: throw Exception("No body"),
                        Charsets.UTF_8
                    )
                )

                var currentData: String? = null

                reader.use { r ->
                    var line = r.readLine()
                    while (line != null) {
                        currentCoroutineContext().ensureActive()

                        when {
                            line.startsWith("data:") -> {
                                currentData = line.removePrefix("data:").trim()
                            }
                            line.isBlank() -> {
                                if (currentData != null) {
                                    try {
                                        val dataStr = currentData!!
                                        val envelope = json.decodeFromString<SseEnvelope>(dataStr)
                                        envelope.payload.directory = envelope.directory
                                        AppLog.i("SSE", "Event: ${envelope.payload.type} dir=${envelope.directory.takeLast(30)}")
                                        emit(envelope.payload)
                                    } catch (e1: Exception) {
                                        try {
                                            val parsed = json.decodeFromString<SseEvent>(currentData!!)
                                            AppLog.i("SSE", "Event: ${parsed.type}")
                                            emit(parsed)
                                        } catch (e2: Exception) {
                                            AppLog.w("SSE", "Parse failed [${e2::class.simpleName}]: ${currentData?.take(120)}")
                                        }
                                    }
                                    currentData = null
                                }
                            }
                        }
                        line = r.readLine()
                    }
                }
                AppLog.w("SSE", "Stream ended, reconnecting in ${retryDelay}ms...")
            } catch (e: Exception) {
                AppLog.e("SSE", "Connection failed", e)
                if (currentCoroutineContext().isActive) {
                    AppLog.i("SSE", "Retrying in ${retryDelay}ms...")
                    delay(retryDelay)
                    retryDelay = (retryDelay * 2).coerceAtMost(30_000L)
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun respondToPermission(
        sessionId: String,
        permissionId: String,
        response: String,
        directory: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            AppLog.i("API", "POST /session/$sessionId/permissions/$permissionId response=$response dir=$directory")
            val body = """{"response":"$response"}"""
            val req = Request.Builder()
                .url("$serverUrl/session/$sessionId/permissions/$permissionId")
            if (directory.isNotBlank()) {
                req.header("x-opencode-directory", directory)
            }
            req.addAuth()
                .post(body.toRequestBody("application/json".toMediaType()))

            client.newCall(req.build()).execute().use { resp ->
                val respBody = resp.body?.string() ?: ""
                AppLog.i("API", "Permission response: HTTP ${resp.code} body=${respBody.take(200)}")
                resp.isSuccessful
            }
        } catch (e: Exception) {
            AppLog.e("API", "Permission response failed", e)
            false
        }
    }
}
