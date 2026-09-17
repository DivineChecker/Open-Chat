package com.yuvraj.openchatai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.InputChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yuvraj.openchatai.data.model.builtInPromptPresets
import com.yuvraj.openchatai.ui.AppViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val memories by viewModel.memories.collectAsStateWithLifecycle()
    val customPresets by viewModel.customPresets.collectAsStateWithLifecycle()
    var showClearDialog by remember { mutableStateOf(false) }
    var showClearMemoriesDialog by remember { mutableStateOf(false) }
    var newMemoryText by remember { mutableStateOf("") }
    var showSavePresetDialog by remember { mutableStateOf(false) }
    var newPresetName by remember { mutableStateOf("") }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionHeader("Capabilities")
            SettingsCard {
                ToggleRow(
                    title = "Tool calling",
                    subtitle = "Turn off if your API doesn't support function calling",
                    checked = settings.toolCallingEnabled,
                    onCheckedChange = { checked ->
                        viewModel.updateSettings { it.copy(toolCallingEnabled = checked) }
                    },
                )
                ToggleRow(
                    title = "Web search",
                    subtitle = "Let the AI search the web for current, up-to-date information",
                    checked = settings.webSearchEnabled && settings.toolCallingEnabled,
                    enabled = settings.toolCallingEnabled,
                    onCheckedChange = { checked ->
                        viewModel.updateSettings { it.copy(webSearchEnabled = checked) }
                    },
                )
                ToggleRow(
                    title = "Stream responses",
                    subtitle = "Show tokens as they arrive. Turn off for APIs without streaming",
                    checked = settings.streamingEnabled,
                    onCheckedChange = { checked ->
                        viewModel.updateSettings { it.copy(streamingEnabled = checked) }
                    },
                )
            }

            SectionHeader("Memory")
            SettingsCard {
                ToggleRow(
                    title = "Long-term memory",
                    subtitle = "The AI remembers facts about you across all chats, stored only on this device",
                    checked = settings.memoryEnabled,
                    onCheckedChange = { checked ->
                        viewModel.updateSettings { it.copy(memoryEnabled = checked) }
                    },
                )
                if (settings.memoryEnabled) {
                    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = newMemoryText,
                                onValueChange = { newMemoryText = it },
                                placeholder = { Text("Add a memory, e.g. \"I'm a Kotlin developer\"") },
                                maxLines = 3,
                                shape = RoundedCornerShape(14.dp),
                                textStyle = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(
                                onClick = {
                                    if (viewModel.addMemory(newMemoryText)) newMemoryText = ""
                                },
                                enabled = newMemoryText.isNotBlank(),
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = "Save memory")
                            }
                        }
                        if (memories.isEmpty()) {
                            Text(
                                text = "No memories yet. The AI saves important facts automatically when tool calling is on, or add one above.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 10.dp),
                            )
                        } else {
                            Spacer(modifier = Modifier.height(6.dp))
                            memories.asReversed().forEach { memory ->
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = memory.content,
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                        Text(
                                            text = if (memory.source == "user") "Added by you" else "Saved by AI",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteMemory(memory.id) },
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Delete memory",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                            }
                            TextButton(onClick = { showClearMemoriesDialog = true }) {
                                Text("Clear all memories", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            SectionHeader("Model behavior")
            SettingsCard {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Text(
                        text = "System prompt",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = settings.activePresetId == null && settings.systemPrompt.isBlank(),
                            onClick = { viewModel.clearSystemPrompt() },
                            label = { Text("None") },
                        )
                        builtInPromptPresets.forEach { preset ->
                            FilterChip(
                                selected = settings.activePresetId == preset.id,
                                onClick = { viewModel.applyPreset(preset) },
                                label = { Text(preset.name) },
                            )
                        }
                        customPresets.forEach { preset ->
                            InputChip(
                                selected = settings.activePresetId == preset.id,
                                onClick = { viewModel.applyPreset(preset) },
                                label = { Text(preset.name) },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Delete preset ${preset.name}",
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { viewModel.deletePreset(preset.id) },
                                    )
                                },
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = settings.systemPrompt,
                        onValueChange = { value ->
                            viewModel.updateSettings { it.copy(systemPrompt = value, activePresetId = null) }
                        },
                        placeholder = { Text("e.g. You are a concise, helpful assistant.") },
                        minLines = 3,
                        maxLines = 6,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextButton(
                        onClick = {
                            newPresetName = ""
                            showSavePresetDialog = true
                        },
                        enabled = settings.systemPrompt.isNotBlank(),
                    ) {
                        Text("Save current prompt as preset")
                    }
                }
                ToggleRow(
                    title = "Custom temperature",
                    subtitle = "Override the provider's default creativity",
                    checked = settings.useCustomTemperature,
                    onCheckedChange = { checked ->
                        viewModel.updateSettings { it.copy(useCustomTemperature = checked) }
                    },
                )
                if (settings.useCustomTemperature) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                    ) {
                        Slider(
                            value = settings.temperature,
                            onValueChange = { value ->
                                viewModel.updateSettings { it.copy(temperature = value) }
                            },
                            valueRange = 0f..2f,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = String.format(Locale.US, "%.1f", settings.temperature),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            SectionHeader("Data")
            SettingsCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Clear all chats",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Text(
                            text = "Deletes every conversation permanently",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { showClearDialog = true }) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteSweep,
                            contentDescription = "Clear all chats",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            Text(
                text = "Everything — chats, memories, API keys — is stored only on this device. Keys are sent directly to your chosen provider.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                lineHeight = 16.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showSavePresetDialog) {
        AlertDialog(
            onDismissRequest = { showSavePresetDialog = false },
            title = { Text("Save preset") },
            text = {
                OutlinedTextField(
                    value = newPresetName,
                    onValueChange = { newPresetName = it },
                    placeholder = { Text("Preset name, e.g. \"Interview coach\"") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (viewModel.saveCurrentPromptAsPreset(newPresetName)) {
                            showSavePresetDialog = false
                        }
                    },
                    enabled = newPresetName.isNotBlank(),
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSavePresetDialog = false }) { Text("Cancel") }
            },
        )
    }

    if (showClearMemoriesDialog) {
        AlertDialog(
            onDismissRequest = { showClearMemoriesDialog = false },
            title = { Text("Clear all memories?") },
            text = { Text("The AI will forget everything it has remembered about you. Chats are kept.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllMemories()
                    showClearMemoriesDialog = false
                }) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearMemoriesDialog = false }) { Text("Cancel") }
            },
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear all chats?") },
            text = { Text("This permanently deletes all conversations. Providers and settings are kept.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllConversations()
                    showClearDialog = false
                }) {
                    Text("Clear all", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(Locale.US),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 1.3.sp,
        modifier = Modifier.padding(start = 6.dp, top = 8.dp),
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column { content() }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) MaterialTheme.colorScheme.onBackground
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}
