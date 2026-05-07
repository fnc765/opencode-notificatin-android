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
        const val CHANNEL_SERVICE = "opencode_service"

        const val PERMISSION_NOTIFICATION_BASE_ID = 1000
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
                CHANNEL_SERVICE,
                "Service Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background service indicator"
            }
        ).forEach { manager.createNotificationChannel(it) }
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
        password: String
    ) {
        val sessionUrl = if (webUiUrl.isNotBlank()) {
            "$webUiUrl/session/$sessionId"
        } else {
            serverUrl
        }

        val contentIntent = Intent(Intent.ACTION_VIEW, Uri.parse(sessionUrl))
        val contentPendingIntent = PendingIntent.getActivity(
            context, sessionId.hashCode(), contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

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

        val notification = NotificationCompat.Builder(context, CHANNEL_PERMISSION)
            .setContentTitle("Approval needed: $toolType")
            .setContentText(title)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(contentPendingIntent)
            .addAction(android.R.drawable.ic_input_add, "Approve", approvePendingIntent)
            .addAction(android.R.drawable.ic_delete, "Deny", denyPendingIntent)
            .build()

        manager.notify(
            PERMISSION_NOTIFICATION_BASE_ID + permissionId.hashCode(),
            notification
        )
    }

    fun showCompletionNotification(sessionId: String, webUiUrl: String) {
        val sessionUrl = if (webUiUrl.isNotBlank()) {
            "$webUiUrl/session/$sessionId"
        } else {
            null
        }

        val contentIntent = if (sessionUrl != null) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(sessionUrl))
            PendingIntent.getActivity(
                context, sessionId.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else null

        val builder = NotificationCompat.Builder(context, CHANNEL_COMPLETION)
            .setContentTitle("Task Completed")
            .setContentText("OpenCode session finished")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        if (contentIntent != null) {
            builder.setContentIntent(contentIntent)
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

    fun cancelPermissionNotification(permissionId: String) {
        manager.cancel(PERMISSION_NOTIFICATION_BASE_ID + permissionId.hashCode())
    }
}
