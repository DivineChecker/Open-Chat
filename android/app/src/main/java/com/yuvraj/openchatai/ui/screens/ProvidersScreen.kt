package com.yuvraj.openchatai.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yuvraj.openchatai.data.model.Provider
import com.yuvraj.openchatai.ui.AppViewModel
import java.util.UUID

private data class ProviderPreset(val name: String, val baseUrl: String)

private val presets = listOf(
    ProviderPreset("OpenAI", "https://api.openai.com/v1"),
    ProviderPreset("OpenRouter", "https://openrouter.ai/api/v1"),
    ProviderPreset("Groq", "https://api.groq.com/openai/v1"),
    ProviderPreset("Together", "https://api.together.xyz/v1"),
    ProviderPreset("Mistral", "https://api.mistral.ai/v1"),
    ProviderPreset("DeepSeek", "https://api.deepseek.com/v1"),
    ProviderPreset("xAI", "https://api.x.ai/v1"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProvidersScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
) {
    val providers by viewModel.providers.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var showSheet by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Provider?>(null) }
    var deleting by remember { mutableStateOf<Provider?>(null) }

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
                title = { Text("Providers", fontWeight = FontWeight.Bold) },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editing = null
                    showSheet = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("Add provider") },
            )
        },
    ) { padding ->
        if (providers.isEmpty()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 32.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Dns,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(40.dp),
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "No providers yet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Add any OpenAI-compatible API endpoint and your key. Models will be fetched automatically.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding() + 4.dp,
                    bottom = padding.calculateBottomPadding() + 96.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(providers, key = { it.id }) { provider ->
                    val isActive = provider.id == settings.activeProviderId
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(18.dp),
                        onClick = { viewModel.setActiveProvider(provider.id) },
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 6.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
                        ) {
                            RadioButton(
                                selected = isActive,
                                onClick = { viewModel.setActiveProvider(provider.id) },
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = provider.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    if (isActive) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = "Active provider",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp),
                                        )
                                    }
                                }
                                Text(
                                    text = provider.baseUrl,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "${provider.models.size} models" +
                                        (provider.selectedModel?.let { " • $it" } ?: ""),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            IconButton(onClick = {
                                editing = provider
                                showSheet = true
                            }) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = "Edit provider",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            IconButton(onClick = { deleting = provider }) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = "Delete provider",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSheet) {
        ProviderEditorSheet(
            viewModel = viewModel,
            existing = editing,
            onDismiss = { showSheet = false },
        )
    }

    deleting?.let { provider ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete ${provider.name}?") },
            text = { Text("The API key and model list for this provider will be removed.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteProvider(provider.id)
                    deleting = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text("Cancel") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderEditorSheet(
    viewModel: AppViewModel,
    existing: Provider?,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember { mutableStateOf(existing?.name ?: "") }
    var baseUrl by remember { mutableStateOf(existing?.baseUrl ?: "") }
    var apiKey by remember { mutableStateOf(existing?.apiKey ?: "") }
    var showKey by remember { mutableStateOf(false) }
    var fetchedModels by remember { mutableStateOf(existing?.models ?: emptyList()) }
    var isFetching by remember { mutableStateOf(false) }
    var fetchMessage by remember { mutableStateOf<String?>(null) }
    var fetchFailed by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (existing == null) "Add provider" else "Edit provider",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                presets.forEach { preset ->
                    Surface(
                        color = if (baseUrl == preset.baseUrl) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = RoundedCornerShape(50),
                        onClick = {
                            name = preset.name
                            baseUrl = preset.baseUrl
                            fetchMessage = null
                        },
                    ) {
                        Text(
                            text = preset.name,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                    }
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = baseUrl,
                onValueChange = {
                    baseUrl = it
                    fetchMessage = null
                },
                label = { Text("Base URL") },
                placeholder = { Text("https://api.example.com/v1") },
                supportingText = {
                    Text(
                        "http:// and https:// both work. URLs with the key in the path " +
                            "(e.g. http://192.168.1.5:8000/v1/your-key) are supported — leave the API key field empty then.",
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("API key (optional for local servers)") },
                singleLine = true,
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showKey = !showKey }) {
                        Icon(
                            imageVector = if (showKey) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (showKey) "Hide key" else "Show key",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedButton(
                onClick = {
                    isFetching = true
                    fetchMessage = null
                    fetchFailed = false
                    viewModel.fetchModels(baseUrl, apiKey) { result ->
                        isFetching = false
                        result.fold(
                            onSuccess = { fetched ->
                                fetchedModels = fetched.models
                                baseUrl = fetched.baseUrl
                                fetchMessage = "${fetched.models.size} models found"
                                fetchFailed = false
                            },
                            onFailure = { error ->
                                fetchMessage = error.message ?: "Could not fetch models"
                                fetchFailed = true
                            },
                        )
                    }
                },
                enabled = baseUrl.isNotBlank() && !isFetching,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isFetching) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Fetching models…")
                } else {
                    Text("Fetch models")
                }
            }

            fetchMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (fetchFailed) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.tertiary,
                )
            }

            Button(
                onClick = {
                    val provider = Provider(
                        id = existing?.id ?: UUID.randomUUID().toString(),
                        name = name.trim().ifBlank { "Provider" },
                        baseUrl = baseUrl.trim(),
                        apiKey = apiKey.trim(),
                        models = fetchedModels,
                        selectedModel = existing?.selectedModel?.takeIf { fetchedModels.contains(it) }
                            ?: fetchedModels.firstOrNull(),
                    )
                    viewModel.saveProvider(provider)
                    onDismiss()
                },
                enabled = name.isNotBlank() && baseUrl.isNotBlank(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save provider")
            }
        }
    }
}
