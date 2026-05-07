package com.opencode.notifier.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.opencode.notifier.service.PermissionActionReceiver

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_PERMISSION = "opencode_permission"
        const val CHANNEL_COMPLETION = "opencode_completion"
        const val CHANNEL_ERROR = "opencode_error"
        const val CHANNEL_QUESTION = "opencode_question"
        const val CHANNEL_SERVICE = "opencode_service"

        const val PERMISSION_NOTIFICATION_BASE_ID = 1000
        const val QUESTION_NOTIFICATION_BASE_ID = 3000
        const val COMPLETION_NOTIFICATION_ID = 2000
        const val ERROR_NOTIFICATION_ID = 2001

        const val ACTION_APPROVE = "com.opencode.notifier.ACTION_APPROVE"
        const val ACTION_DENY = "com.opencode.notifier.ACTION_DENY"
    }

    private val manager = context.getSystemService(NotificationManager::class.java)

    init {
        createChannels()
    }

    private fun createChannels() {
        listOf(
            NotificationChannel(
                CHANNEL_PERMISSION,
                "Approval Requests",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications when OpenCode needs your approval"
            },
            NotificationChannel(
                CHANNEL_COMPLETION,
                "Task Completion",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications when OpenCode completes a task"
            },
            NotificationChannel(
                CHANNEL_ERROR,
                "Errors",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications when OpenCode encounters an error"
            },
            NotificationChannel(
                CHANNEL_QUESTION,
                "Questions",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications when OpenCode asks you a question"
            },
            NotificationChannel(
                CHANNEL_SERVICE,
                "Service Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background service indicator"
            }
        ).forEach { manager.createNotificationChannel(it) }
    }

    private fun buildSessionUrl(uiType: String, webUiUrl: String, serverUrl: String, sessionId: String): String {
        val base = webUiUrl.ifBlank { serverUrl }
        return if (uiType == "portal") "$base/session/$sessionId" else base
    }

    private fun buildContentIntent(url: String, requestCode: Int): PendingIntent? {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            PendingIntent.getActivity(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } catch (_: Exception) {
            null
        }
    }

    fun buildServiceNotification(): Notification {
        return NotificationCompat.Builder(context, CHANNEL_SERVICE)
            .setContentTitle("OpenCode Notifier")
            .setContentText("Connected and listening for events")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
    }

    fun showPermissionNotification(
        sessionId: String,
        permissionId: String,
        title: String,
        toolType: String,
        webUiUrl: String,
        serverUrl: String,
        username: String,
        password: String,
        uiType: String
    ) {
        val webUrl = buildSessionUrl(uiType, webUiUrl, serverUrl, sessionId)
        val contentPendingIntent = buildContentIntent(webUrl, sessionId.hashCode())

        val approveIntent = Intent(context, PermissionActionReceiver::class.java).apply {
            action = ACTION_APPROVE
            putExtra("session_id", sessionId)
            putExtra("permission_id", permissionId)
            putExtra("server_url", serverUrl)
            putExtra("username", username)
            putExtra("password", password)
            putExtra("notification_id", PERMISSION_NOTIFICATION_BASE_ID + permissionId.hashCode())
        }
        val approvePendingIntent = PendingIntent.getBroadcast(
            context, "approve_$permissionId".hashCode(), approveIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val denyIntent = Intent(context, PermissionActionReceiver::class.java).apply {
            action = ACTION_DENY
            putExtra("session_id", sessionId)
            putExtra("permission_id", permissionId)
            putExtra("server_url", serverUrl)
            putExtra("username", username)
            putExtra("password", password)
            putExtra("notification_id", PERMISSION_NOTIFICATION_BASE_ID + permissionId.hashCode())
        }
        val denyPendingIntent = PendingIntent.getBroadcast(
            context, "deny_$permissionId".hashCode(), denyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_PERMISSION)
            .setContentTitle("Approval needed: $toolType")
            .setContentText(title)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .addAction(android.R.drawable.ic_input_add, "Approve", approvePendingIntent)
            .addAction(android.R.drawable.ic_delete, "Deny", denyPendingIntent)

        if (contentPendingIntent != null) {
            builder.setContentIntent(contentPendingIntent)
        }

        manager.notify(
            PERMISSION_NOTIFICATION_BASE_ID + permissionId.hashCode(),
            builder.build()
        )
    }

    fun showCompletionNotification(sessionId: String, webUiUrl: String, serverUrl: String, uiType: String) {
        val webUrl = buildSessionUrl(uiType, webUiUrl, serverUrl, sessionId)
        val contentPendingIntent = buildContentIntent(webUrl, sessionId.hashCode())

        val builder = NotificationCompat.Builder(context, CHANNEL_COMPLETION)
            .setContentTitle("Task Completed")
            .setContentText("OpenCode session finished")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        if (contentPendingIntent != null) {
            builder.setContentIntent(contentPendingIntent)
        }

        manager.notify(COMPLETION_NOTIFICATION_ID, builder.build())
    }

    fun showErrorNotification(sessionId: String, error: String?) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ERROR)
            .setContentTitle("OpenCode Error")
            .setContentText(error ?: "An error occurred during execution")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify(ERROR_NOTIFICATION_ID, notification)
    }

    fun showQuestionNotification(
        sessionId: String,
        permissionId: String,
        title: String,
        webUiUrl: String,
        serverUrl: String,
        username: String,
        password: String,
        uiType: String
    ) {
        val webUrl = buildSessionUrl(uiType, webUiUrl, serverUrl, sessionId)
        val contentPendingIntent = buildContentIntent(webUrl, sessionId.hashCode())

        val notification = NotificationCompat.Builder(context, CHANNEL_QUESTION)
            .setContentTitle("OpenCode Question")
            .setContentText(title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(title))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        if (contentPendingIntent != null) {
            notification.contentIntent = contentPendingIntent
        }

        manager.notify(
            QUESTION_NOTIFICATION_BASE_ID + permissionId.hashCode(),
            notification
        )
    }

    fun cancelPermissionNotification(permissionId: String) {
        manager.cancel(PERMISSION_NOTIFICATION_BASE_ID + permissionId.hashCode())
    }

    fun cancelQuestionNotification(permissionId: String) {
        manager.cancel(QUESTION_NOTIFICATION_BASE_ID + permissionId.hashCode())
    }
}
