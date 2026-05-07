package com.opencode.notifier.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.opencode.notifier.api.OpenCodeApiClient
import com.opencode.notifier.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PermissionActionReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val sessionId = intent.getStringExtra("session_id") ?: return
        val permissionId = intent.getStringExtra("permission_id") ?: return
        val serverUrl = intent.getStringExtra("server_url") ?: return
        val username = intent.getStringExtra("username") ?: "opencode"
        val password = intent.getStringExtra("password") ?: return
        val notificationId = intent.getIntExtra("notification_id", 0)

        val response = when (intent.action) {
            NotificationHelper.ACTION_APPROVE -> "approved"
            NotificationHelper.ACTION_DENY -> "denied"
            else -> return
        }

        val pendingResult = goAsync()

        scope.launch {
            try {
                val client = OpenCodeApiClient(serverUrl, username, password)
                client.respondToPermission(sessionId, permissionId, response)

                val manager = context.getSystemService(NotificationManager::class.java)
                manager.cancel(notificationId)
            } catch (_: Exception) {
            } finally {
                pendingResult.finish()
            }
        }
    }
}
