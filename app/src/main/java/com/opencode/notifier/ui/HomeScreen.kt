package com.opencode.notifier.ui

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.getSystemService
import com.opencode.notifier.AppLog
import kotlinx.coroutines.flow.first

@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    isServiceConnected: Boolean,
    settingsRepository: com.opencode.notifier.data.SettingsRepository
) {
    var settings by remember {
        mutableStateOf(com.opencode.notifier.data.SettingsRepository.Settings())
    }
    val logs by AppLog.logs.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        settings = settingsRepository.settings.first()
    }

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    fun exportLog() {
        val text = AppLog.logs.value.joinToString("\n")
        val clipboard = context.getSystemService<ClipboardManager>()
        clipboard?.setPrimaryClip(ClipData.newPlainText("OpenCode Notifier Log", text))
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "OpenCode Notifier",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (isServiceConnected) "● Connected" else "○ Disconnected",
                style = MaterialTheme.typography.titleMedium,
                color = if (isServiceConnected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error
            )

            if (settings.isConfigured) {
                Text(
                    text = settings.serverUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onNavigateToSettings) {
                    Text("Settings")
                }
                OutlinedButton(onClick = { AppLog.clear() }) {
                    Text("Clear")
                }
                OutlinedButton(onClick = { exportLog() }) {
                    Text("Copy Log")
                }
            }
        }

        HorizontalDivider()

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No logs yet.\nSet server URL in Settings.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(logs) { log ->
                    val color = when {
                        "ERROR" in log -> MaterialTheme.colorScheme.error
                        "WARN" in log -> MaterialTheme.colorScheme.tertiary
                        "→" in log || "sent" in log -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                    Text(
                        text = log,
                        fontSize = 11.sp,
                        color = color,
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
            }
        }
    }
}
