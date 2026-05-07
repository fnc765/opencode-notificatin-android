package com.opencode.notifier.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.opencode.notifier.data.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    settingsRepository: SettingsRepository
) {
    var settings by remember {
        mutableStateOf(SettingsRepository.Settings())
    }

    var serverUrl by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("opencode") }
    var password by remember { mutableStateOf("") }
    var webUiUrl by remember { mutableStateOf("") }

    var isSaved by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        settings = settingsRepository.settings.first()
        serverUrl = settings.serverUrl
        username = settings.username
        password = settings.password
        webUiUrl = settings.webUiUrl
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it },
                label = { Text("OpenCode Server URL") },
                placeholder = { Text("http://192.168.1.100:4096") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
            )

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "Web UI (optional)",
                style = MaterialTheme.typography.titleMedium
            )

            OutlinedTextField(
                value = webUiUrl,
                onValueChange = { webUiUrl = it },
                label = { Text("Web UI URL") },
                placeholder = { Text("http://192.168.1.100:3000") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                supportingText = {
                    Text("Tap a completion notification to open this URL")
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    scope.launch {
                        settingsRepository.saveSettings(
                            SettingsRepository.Settings(
                                serverUrl = serverUrl,
                                username = username,
                                password = password,
                                webUiUrl = webUiUrl
                            )
                        )
                        isSaved = true
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = serverUrl.isNotBlank()
            ) {
                Text("Save")
            }

            if (isSaved) {
                Text(
                    text = "Settings saved. Restart the service to apply.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider()

            Text(
                text = "How to use",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 8.dp)
            )

            Text(
                text = """
                    1. Run OpenCode on your server:
                       opencode web --hostname 0.0.0.0
                       
                    2. Set OPENCODE_SERVER_PASSWORD env var.
                    
                    3. Enter the server URL and credentials above.
                    
                    4. Go back to the home screen. The service will connect and send notifications when:
                       • OpenCode needs your approval (Approve/Deny in notification)
                       • A session completes (tap to open Web UI)
                       • An error occurs
                    
                    5. Use OpenCode Portal or Web UI in your browser for full interaction.
                """.trimIndent(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
