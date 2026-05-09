package com.opencode.notifier

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLog {
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val fileDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private var logDir: File? = null
    private var currentLogFile: File? = null
    private var fileWriter: PrintWriter? = null

    private const val MAX_FILES = 10
    private const val MAX_BYTES = 1_000_000L

    fun init(context: Context) {
        logDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "logs").apply {
            if (!exists()) mkdirs()
        }
        openCurrentFile()
    }

    private fun openCurrentFile() {
        fileWriter?.close()
        val dir = logDir ?: return

        val today = fileDateFormat.format(Date())
        val base = File(dir, "opencode-notifier-$today.log")

        // find next available index
        var index = 0
        var file = base
        while (file.exists() && file.length() >= MAX_BYTES) {
            index++
            file = File(dir, "opencode-notifier-$today.$index.log")
        }

        if (file.exists() && file.length() >= MAX_BYTES) {
            index++
            file = File(dir, "opencode-notifier-$today.$index.log")
        }

        currentLogFile = file
        fileWriter = PrintWriter(FileWriter(file, true), true)
        rotateIfNeeded()
    }

    private fun rotateIfNeeded() {
        currentLogFile?.let { file ->
            if (file.length() > MAX_BYTES) {
                openCurrentFile()
            }
        }
        logDir?.let { dir ->
            val logFiles = dir.listFiles { f -> f.name.endsWith(".log") }
                ?.sortedByDescending { it.lastModified() } ?: return
            if (logFiles.size > MAX_FILES) {
                logFiles.drop(MAX_FILES).forEach { it.delete() }
            }
        }
    }

    private fun add(tag: String, message: String) {
        val timestamp = dateFormat.format(Date())
        val line = "$timestamp [$tag] $message"
        Log.d("OpencodeNotifier", line)
        _logs.value = (_logs.value + line).takeLast(200)

        try {
            fileWriter?.let { writer ->
                writer.println(line)
                rotateIfNeeded()
            }
        } catch (_: Exception) {}
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
