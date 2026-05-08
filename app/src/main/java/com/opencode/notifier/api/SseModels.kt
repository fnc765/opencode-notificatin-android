package com.opencode.notifier.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class SseEnvelope(
    val directory: String = "",
    val payload: SseEvent
)

@Serializable
data class SseEvent(
    val id: String? = null,
    val type: String,
    val properties: JsonObject
)

@Serializable
data class PermissionInfo(
    val id: String,
    val type: String,
    val sessionID: String,
    val messageID: String,
    val title: String,
    val metadata: JsonObject? = null
)

@Serializable
data class SessionIdleProps(
    val sessionID: String
)

@Serializable
data class PermissionRepliedProps(
    val sessionID: String,
    val permissionID: String,
    val response: String
)

@Serializable
data class PermissionResponseBody(
    val response: String
)
