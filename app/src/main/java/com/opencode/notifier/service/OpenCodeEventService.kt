package com.opencode.notifier.service

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.opencode.notifier.AppLog
import com.opencode.notifier.MainActivity
import com.opencode.notifier.api.OpenCodeApiClient
import com.opencode.notifier.api.PermissionInfo
import com.opencode.notifier.api.QuestionInfo
import com.opencode.notifier.api.SessionIdleProps
import com.opencode.notifier.api.SseEvent
import com.opencode.notifier.data.SettingsRepository
import com.opencode.notifier.notification.NotificationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicBoolean

class OpenCodeEventService : LifecycleService() {

    private lateinit var notificationHelper: NotificationHelper
    private lateinit var settingsRepository: SettingsRepository
    private var eventJob: Job? = null
    private val json = Json { ignoreUnknownKeys = true }

    private val isRunning = AtomicBoolean(false)

    companion object {
        const val ACTION_START = "com.opencode.notifier.action.START"
        const val ACTION_STOP = "com.opencode.notifier.action.STOP"
    }

    override fun onCreate() {
        super.onCreate()
        notificationHelper = NotificationHelper(this)
        settingsRepository = SettingsRepository(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stop()
                return START_NOT_STICKY
            }
        }
        start()
        return START_STICKY
    }

    private fun start() {
        if (isRunning.getAndSet(true)) return
        AppLog.i("SVC", "Service starting...")

        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, OpenCodeEventService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(
            this, NotificationHelper.CHANNEL_SERVICE
        )
            .setContentTitle("OpenCode Notifier")
            .setContentText("Connected and listening")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(openIntent)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopIntent)
            .setOngoing(true)
            .build()

        startForeground(1, notification)

        eventJob = lifecycleScope.launch {
            connectToServer()
        }
    }

    private fun stop() {
        AppLog.i("SVC", "Service stopping")
        isRunning.set(false)
        eventJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private suspend fun connectToServer() {
        val settings = settingsRepository.settings.first()
        if (!settings.isConfigured) return

        val client = OpenCodeApiClient(
            serverUrl = settings.serverUrl.trimEnd('/'),
            username = settings.username,
            password = settings.password
        )
        AppLog.i("SVC", "SSE stream started, server=${settings.serverUrl} auth=${settings.password.isNotBlank()}")

        try {
            client.eventStream().collect { event ->
                if (event.properties == null) return@collect
                when (event.type) {
                    "session.idle" -> {
                        AppLog.i("SVC", "→ session.idle")
                        handleSessionIdle(event, settings.webUiUrl.trimEnd('/'), settings.serverUrl.trimEnd('/'), settings.uiType)
                    }
                    "session.error" -> {
                        AppLog.i("SVC", "→ session.error")
                        handleSessionError(event)
                    }
                    "permission.asked" -> {
                        AppLog.i("SVC", "→ permission.asked")
                        handlePermissionAsked(event, settings)
                    }
                    "question.asked" -> {
                        AppLog.i("SVC", "→ question.asked")
                        handleQuestionAsked(event, settings)
                    }
                }
            }
        } catch (_: Exception) {
            // disconnected
        }
    }

    private fun handleSessionIdle(event: SseEvent, webUiUrl: String, serverUrl: String, uiType: String) {
        try {
            val props = json.decodeFromString<SessionIdleProps>(
                event.properties!!.toString()
            )
            notificationHelper.showCompletionNotification(props.sessionID, webUiUrl, serverUrl, uiType)
            AppLog.i("SVC", "  → completion notification sent for ${props.sessionID}")
        } catch (_: Exception) {}
    }

    private fun handleSessionError(event: SseEvent) {
        try {
            val sessionId = event.properties?.get("sessionID")?.let {
                json.decodeFromString<String>(it.toString())
            } ?: ""
            notificationHelper.showErrorNotification(sessionId, null)
            AppLog.i("SVC", "  → error notification sent for $sessionId")
        } catch (_: Exception) {}
    }

    private fun handlePermissionAsked(
        event: SseEvent,
        settings: SettingsRepository.Settings
    ) {
        try {
            val raw = event.properties!!.toString()
            AppLog.i("SVC", "  permission raw[0..200]: ${raw.take(200)}")
            val permission = json.decodeFromString<PermissionInfo>(raw)
            val title = buildPermissionTitle(permission)

            notificationHelper.showPermissionNotification(
                sessionId = permission.sessionID,
                permissionId = permission.id,
                title = title,
                toolType = permission.permission,
                webUiUrl = settings.webUiUrl.trimEnd('/'),
                serverUrl = settings.serverUrl.trimEnd('/'),
                username = settings.username,
                password = settings.password,
                uiType = settings.uiType,
                directory = event.directory
            )
            AppLog.i("SVC", "  → permission notification sent: ${permission.permission} - $title")
        } catch (e: Exception) {
            AppLog.e("SVC", "Permission parse/handle error", e)
        }
    }

    private fun handleQuestionAsked(
        event: SseEvent,
        settings: SettingsRepository.Settings
    ) {
        try {
            val raw = event.properties!!.toString()
            AppLog.i("SVC", "  question raw[0..200]: ${raw.take(200)}")
            val question = json.decodeFromString<QuestionInfo>(raw)
            val text = question.questions.firstOrNull()?.question ?: "OpenCode has a question"

            notificationHelper.showQuestionNotification(
                sessionId = question.sessionID,
                permissionId = question.id,
                title = text,
                webUiUrl = settings.webUiUrl.trimEnd('/'),
                serverUrl = settings.serverUrl.trimEnd('/'),
                username = settings.username,
                password = settings.password,
                uiType = settings.uiType
            )
            AppLog.i("SVC", "  → question notification sent: $text")
        } catch (e: Exception) {
            AppLog.e("SVC", "Question parse/handle error", e)
        }
    }

    private fun buildPermissionTitle(permission: PermissionInfo): String {
        val patterns = permission.patterns?.joinToString(", ") ?: ""
        return when {
            patterns.isNotBlank() -> "${permission.permission}: $patterns"
            permission.permission.isNotBlank() -> permission.permission
            else -> "Unknown permission"
        }
    }

}
