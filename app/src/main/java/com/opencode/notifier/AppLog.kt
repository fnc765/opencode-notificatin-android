package com.opencode.notifier

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLog {
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    private fun add(tag: String, message: String) {
        val timestamp = dateFormat.format(Date())
        val line = "$timestamp [$tag] $message"
        Log.d("OpencodeNotifier", line)
        _logs.value = (_logs.value + line).takeLast(200)
    }

    fun i(tag: String, message: String) = add(tag, message)
    fun e(tag: String, message: String, error: Throwable? = null) {
        val msg = if (error != null) "$message: ${error.message}" else message
        add(tag, "ERROR: $msg")
        Log.e("OpencodeNotifier", msg, error)
    }
    fun w(tag: String, message: String) = add(tag, "WARN: $message")
    fun clear() { _logs.value = emptyList() }
}
