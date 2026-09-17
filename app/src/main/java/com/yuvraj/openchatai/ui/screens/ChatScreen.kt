package com.yuvraj.openchatai.ui.screens

import android.net.Uri
import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.DrawerValue
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yuvraj.openchatai.data.model.Attachment
import com.yuvraj.openchatai.data.model.Conversation
import com.yuvraj.openchatai.data.model.Provider
import com.yuvraj.openchatai.data.model.StoredMessage
import com.yuvraj.openchatai.ui.AppViewModel
import com.yuvraj.openchatai.ui.components.EditMessageDialog
import com.yuvraj.openchatai.ui.components.MessageActionsSheet
import com.yuvraj.openchatai.ui.components.MessageBubble
import com.yuvraj.openchatai.ui.components.PendingAttachmentChip
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: AppViewModel,
    onOpenProviders: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val providers by viewModel.providers.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val currentId by viewModel.currentConversationId.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val status by viewModel.generationStatus.collectAsStateWithLifecycle()
    val pendingAttachments by viewModel.pendingAttachments.collectAsStateWithLifecycle()
    val isProcessingAttachments by viewModel.isProcessingAttachments.collectAsStateWithLifecycle()
    val attachmentError by viewModel.attachmentError.collectAsStateWithLifecycle()

    val conversation = conversations.find { it.id == currentId }
    val messages = conversation?.messages ?: emptyList()

    val activeProvider = providers.find { it.id == settings.activeProviderId } ?: providers.firstOrNull()
    val activeModel = activeProvider?.selectedModel ?: activeProvider?.models?.firstOrNull()
    val canSend = activeProvider != null && activeModel != null

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var input by rememberSaveable { mutableStateOf("") }
    var showModelSheet by remember { mutableStateOf(false) }
    var actionMessage by remember { mutableStateOf<StoredMessage?>(null) }
    var editingMessage by remember { mutableStateOf<StoredMessage?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 6),
    ) { uris: List<Uri> -> viewModel.addAttachments(uris) }
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris: List<Uri> -> viewModel.addAttachments(uris) }

    val isAtBottom by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull() ?: return@derivedStateOf true
            lastVisible.index == info.totalItemsCount - 1 &&
                lastVisible.offset + lastVisible.size <= info.viewportEndOffset + 48
        }
    }

    // Follow mode: the chat sticks to fresh lines while streaming. It only detaches
    // when the user actively drags upward, and re-attaches when they return to the bottom.
    var autoFollow by remember { mutableStateOf(true) }

    val followBreakConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // A user-initiated upward drag (finger moving down the list) detaches follow mode.
                if (source == NestedScrollSource.UserInput && available.y > 0f) {
                    autoFollow = false
                }
                return Offset.Zero
            }
        }
    }

    // Reaching the bottom again (by hand or via the arrow button) re-attaches follow mode.
    LaunchedEffect(isAtBottom) {
        if (isAtBottom) autoFollow = true
    }

    // A new message (user sent, or a fresh reply started) always re-pins to the bottom.
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            autoFollow = true
            listState.scrollToItem(messages.lastIndex, Int.MAX_VALUE)
        }
    }

    // While streaming, keep gliding with the fresh lines as long as follow mode is on.
    val streamedLength = if (isGenerating) messages.lastOrNull()?.content?.length ?: 0 else 0
    LaunchedEffect(streamedLength) {
        if (streamedLength > 0 && messages.isNotEmpty() && autoFollow) {
            listState.scrollToItem(messages.lastIndex, Int.MAX_VALUE)
        }
    }

    LaunchedEffect(attachmentError) {
        attachmentError?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.dismissAttachmentError()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ConversationDrawer(
                conversations = conversations.sortedByDescending { it.updatedAt },
                currentId = currentId,
                onNewChat = {
                    viewModel.newChat()
                    scope.launch { drawerState.close() }
                },
                onSelect = { id ->
                    viewModel.selectConversation(id)
                    scope.launch { drawerState.close() }
                },
                onDelete = { viewModel.deleteConversation(it) },
                onOpenProviders = {
                    scope.launch { drawerState.close() }
                    onOpenProviders()
                },
                onOpenSettings = {
                    scope.launch { drawerState.close() }
                    onOpenSettings()
                },
            )
        },
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Rounded.Menu, contentDescription = "Open menu")
                        }
                    },
                    title = {
                        ModelSelectorChip(
                            providerName = activeProvider?.name,
                            modelName = activeModel,
                            onClick = {
                                if (activeProvider == null) onOpenProviders() else showModelSheet = true
                            },
                        )
                    },
                    actions = {
                        IconButton(onClick = { viewModel.newChat() }) {
                            Icon(Icons.Rounded.EditNote, contentDescription = "New chat")
                        }
                    },
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .imePadding(),
            ) {
                if (messages.isEmpty()) {
                    EmptyChatState(
                        hasProvider = activeProvider != null,
                        canSend = canSend,
                        webSearchOn = settings.toolCallingEnabled && settings.webSearchEnabled,
                        onSuggestion = { viewModel.sendMessage(it) },
                        onOpenProviders = onOpenProviders,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    ) {
                        LazyColumn(
                            state = listState,
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 16.dp,
                                vertical = 14.dp,
                            ),
                            modifier = Modifier
                                .fillMaxSize()
                                .nestedScroll(followBreakConnection),
                        ) {
                            val lastAssistantId = messages.lastOrNull { it.role == "assistant" }?.id
                            items(messages, key = { it.id }) { message ->
                                MessageBubble(
                                    message = message,
                                    isActiveStream = isGenerating && message.id == lastAssistantId,
                                    status = status,
                                    canRegenerate = !isGenerating && message.id == lastAssistantId,
                                    onRegenerate = { viewModel.regenerateLastResponse() },
                                    onLongPress = { actionMessage = message },
                                )
                            }
                        }

                        androidx.compose.animation.AnimatedVisibility(
                            visible = !isAtBottom,
                            enter = fadeIn() + scaleIn(initialScale = 0.8f),
                            exit = fadeOut() + scaleOut(targetScale = 0.8f),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 10.dp),
                        ) {
                            Surface(
                                onClick = {
                                    autoFollow = true
                                    scope.launch {
                                        if (messages.isNotEmpty()) {
                                            listState.animateScrollToItem(messages.lastIndex, Int.MAX_VALUE)
                                        }
                                    }
                                },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                shadowElevation = 4.dp,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ArrowDownward,
                                    contentDescription = "Scroll to latest response",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .size(20.dp),
                                )
                            }
                        }
                    }
                }

                ChatInputBar(
                    value = input,
                    onValueChange = { input = it },
                    enabled = canSend,
                    isGenerating = isGenerating,
                    toolsOn = settings.toolCallingEnabled,
                    webSearchOn = settings.webSearchEnabled,
                    attachments = pendingAttachments,
                    isProcessingAttachments = isProcessingAttachments,
                    onAttachPhotos = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                    onAttachFiles = { filePicker.launch(arrayOf("*/*")) },
                    onRemoveAttachment = { viewModel.removeAttachment(it) },
                    onToggleWebSearch = {
                        viewModel.updateSettings { it.copy(webSearchEnabled = !it.webSearchEnabled) }
                    },
                    onSend = {
                        val text = input
                        input = ""
                        viewModel.sendMessage(text)
                    },
                    onStop = { viewModel.stopGeneration() },
                )
            }
        }
    }

    if (showModelSheet) {
        ModelPickerSheet(
            providers = providers,
            activeProvider = activeProvider,
            onSelectProvider = { viewModel.setActiveProvider(it) },
            onSelectModel = {
                viewModel.selectModel(it)
                showModelSheet = false
            },
            onDismiss = { showModelSheet = false },
        )
    }

    actionMessage?.let { selected ->
        MessageActionsSheet(
            message = selected,
            canModify = !isGenerating,
            onEditResend = {
                actionMessage = null
                editingMessage = selected
            },
            onDelete = {
                actionMessage = null
                viewModel.deleteMessage(selected.id)
            },
            onDismiss = { actionMessage = null },
        )
    }

    editingMessage?.let { editing ->
        EditMessageDialog(
            initialText = editing.content,
            onConfirm = { newText ->
                editingMessage = null
                viewModel.editAndResend(editing.id, newText)
            },
            onDismiss = { editingMessage = null },
        )
    }
}

@Composable
private fun ModelSelectorChip(
    providerName: String?,
    modelName: String?,
    onClick: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(50),
        onClick = onClick,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 14.dp, end = 8.dp, top = 7.dp, bottom = 7.dp),
        ) {
            Column {
                Text(
                    text = modelName ?: (if (providerName == null) "Add a provider" else "Pick a model"),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 180.dp),
                )
                providerName?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
            Icon(
                imageVector = Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmptyChatState(
    hasProvider: Boolean,
    canSend: Boolean,
    webSearchOn: Boolean,
    onSuggestion: (String) -> Unit,
    onOpenProviders: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(72.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(34.dp),
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = if (hasProvider) "What can I help with?" else "Welcome to OpenChat",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (!hasProvider) {
            Text(
                text = "Connect any OpenAI-compatible API — OpenAI, OpenRouter, Groq, Mistral, DeepSeek and more. Models are fetched automatically.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(20.dp))
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(50),
                onClick = onOpenProviders,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Add a provider",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        } else {
            Text(
                text = if (webSearchOn) "Web search is on — ask about anything current." else "Ask anything, or turn on web search for live info.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(28.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    "What's the latest AI news this week?",
                    "Explain quantum computing simply",
                    "Write a Kotlin coroutine example",
                    "Plan a 3-day trip to Tokyo",
                ).forEach { suggestion ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(16.dp),
                        onClick = { if (canSend) onSuggestion(suggestion) },
                    ) {
                        Text(
                            text = suggestion,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    isGenerating: Boolean,
    toolsOn: Boolean,
    webSearchOn: Boolean,
    attachments: List<Attachment>,
    isProcessingAttachments: Boolean,
    onAttachPhotos: () -> Unit,
    onAttachFiles: () -> Unit,
    onRemoveAttachment: (String) -> Unit,
    onToggleWebSearch: () -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
) {
    var showAttachMenu by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Column {
            if (attachments.isNotEmpty() || isProcessingAttachments) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(start = 14.dp, end = 14.dp, top = 12.dp),
                ) {
                    attachments.forEach { attachment ->
                        PendingAttachmentChip(
                            attachment = attachment,
                            onRemove = { onRemoveAttachment(attachment.id) },
                        )
                    }
                    if (isProcessingAttachments) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .size(56.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainerHighest,
                                    RoundedCornerShape(12.dp),
                                ),
                        ) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.padding(start = 6.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        ) {
            Box {
                IconButton(
                    onClick = { showAttachMenu = true },
                    enabled = enabled,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Attach files",
                        tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(22.dp),
                    )
                }
                DropdownMenu(
                    expanded = showAttachMenu,
                    onDismissRequest = { showAttachMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Photos") },
                        leadingIcon = {
                            Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(20.dp))
                        },
                        onClick = {
                            showAttachMenu = false
                            onAttachPhotos()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Files (PDF, docs…)") },
                        leadingIcon = {
                            Icon(Icons.Rounded.Description, contentDescription = null, modifier = Modifier.size(20.dp))
                        },
                        onClick = {
                            showAttachMenu = false
                            onAttachFiles()
                        },
                    )
                }
            }
            if (toolsOn) {
                IconButton(
                    onClick = onToggleWebSearch,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Language,
                        contentDescription = if (webSearchOn) "Web search on" else "Web search off",
                        tint = if (webSearchOn) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(21.dp),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Default,
                    ),
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (value.isEmpty()) {
                    Text(
                        text = if (enabled) "Message…" else "Add a provider to start",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 16.sp,
                    )
                }
            }
            FilledIconButton(
                onClick = { if (isGenerating) onStop() else onSend() },
                enabled = isGenerating ||
                    (enabled && !isProcessingAttachments && (value.isNotBlank() || attachments.isNotEmpty())),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                ),
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = if (isGenerating) Icons.Rounded.Stop else Icons.Rounded.ArrowUpward,
                    contentDescription = if (isGenerating) "Stop generating" else "Send message",
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        }
    }
}

@Composable
private fun ConversationDrawer(
    conversations: List<Conversation>,
    currentId: String?,
    onNewChat: () -> Unit,
    onSelect: (String) -> Unit,
    onDelete: (String) -> Unit,
    onOpenProviders: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val filteredConversations = if (searchQuery.isBlank()) {
        conversations
    } else {
        conversations.filter { conv ->
            conv.title.contains(searchQuery, ignoreCase = true) ||
                conv.messages.any { it.content.contains(searchQuery, ignoreCase = true) }
        }
    }

    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.width(300.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 12.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "OpenChat",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(14.dp),
            onClick = onNewChat,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "New chat",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search chats", style = MaterialTheme.typography.bodyMedium) },
            leadingIcon = {
                Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp))
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Clear search",
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, top = 10.dp),
        )

        if (searchQuery.isNotBlank() && filteredConversations.isEmpty()) {
            Text(
                text = "No chats match \"$searchQuery\"",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(top = 10.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp),
        ) {
            items(filteredConversations, key = { it.id }) { conversation ->
                val selected = conversation.id == currentId
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .background(
                            color = if (selected) MaterialTheme.colorScheme.surfaceContainerHighest
                            else androidx.compose.ui.graphics.Color.Transparent,
                            shape = RoundedCornerShape(12.dp),
                        )
                        .clickable { onSelect(conversation.id) }
                        .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = conversation.title,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val subtitle = if (searchQuery.isNotBlank()) {
                            matchSnippet(conversation, searchQuery)
                        } else {
                            null
                        }
                        Text(
                            text = subtitle
                                ?: DateUtils.getRelativeTimeSpanString(conversation.updatedAt).toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(
                        onClick = { onDelete(conversation.id) },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Delete conversation",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        DrawerFooterItem(icon = Icons.Outlined.Dns, label = "Providers", onClick = onOpenProviders)
        DrawerFooterItem(icon = Icons.Outlined.Settings, label = "Settings", onClick = onOpenSettings)
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

/** Short excerpt around the first message that matches the search query. */
private fun matchSnippet(conversation: Conversation, query: String): String? {
    val message = conversation.messages.firstOrNull { it.content.contains(query, ignoreCase = true) }
        ?: return null
    val index = message.content.indexOf(query, ignoreCase = true)
    val start = (index - 20).coerceAtLeast(0)
    val end = (index + query.length + 40).coerceAtMost(message.content.length)
    val prefix = if (start > 0) "…" else ""
    val suffix = if (end < message.content.length) "…" else ""
    return prefix + message.content.substring(start, end).replace('\n', ' ').trim() + suffix
}

@Composable
private fun DrawerFooterItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelPickerSheet(
    providers: List<Provider>,
    activeProvider: Provider?,
    onSelectProvider: (String) -> Unit,
    onSelectModel: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }
    var customModel by remember { mutableStateOf("") }

    val models = activeProvider?.models ?: emptyList()
    val filtered = if (query.isBlank()) models else models.filter { it.contains(query, ignoreCase = true) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                text = "Choose a model",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (providers.size > 1) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                ) {
                    providers.forEach { provider ->
                        FilterChip(
                            selected = provider.id == activeProvider?.id,
                            onClick = { onSelectProvider(provider.id) },
                            label = { Text(provider.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        )
                    }
                }
            }
            if (models.size > 10) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search models") },
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.heightIn(max = 380.dp)) {
                items(filtered, key = { it }) { model ->
                    val selected = model == activeProvider?.selectedModel
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectModel(model) }
                            .padding(vertical = 12.dp),
                    ) {
                        Text(
                            text = model,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (selected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = customModel,
                    onValueChange = { customModel = it },
                    placeholder = { Text("Custom model ID") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(10.dp))
                FilledIconButton(
                    onClick = { if (customModel.isNotBlank()) onSelectModel(customModel.trim()) },
                    enabled = customModel.isNotBlank(),
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = "Use custom model")
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
