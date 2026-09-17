package com.yuvraj.openchatai.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yuvraj.openchatai.data.model.StoredMessage

/** Renders one chat message: user bubble or full-width assistant answer. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: StoredMessage,
    isActiveStream: Boolean,
    status: String?,
    canRegenerate: Boolean,
    onRegenerate: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val handleLongPress = {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        onLongPress()
    }
    if (message.role == "user") {
        Column(
            horizontalAlignment = Alignment.End,
            modifier = modifier
                .fillMaxWidth()
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                    onLongClick = handleLongPress,
                ),
        ) {
            if (message.attachments.isNotEmpty()) {
                MessageAttachments(attachments = message.attachments)
                if (message.content.isNotBlank()) Spacer(modifier = Modifier.height(6.dp))
            }
            if (message.content.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = 20.dp,
                        bottomEnd = 6.dp,
                    ),
                    modifier = Modifier.widthIn(max = 300.dp),
                ) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
                    )
                }
            }
        }
        return
    }

    val clipboard = LocalClipboardManager.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
                onLongClick = handleLongPress,
            ),
    ) {
        if (isActiveStream && status != null) {
            StatusPill(status = status)
            Spacer(modifier = Modifier.height(10.dp))
        }

        val reasoning = message.reasoning
        if (!reasoning.isNullOrBlank()) {
            ReasoningSection(
                reasoning = reasoning,
                isThinking = isActiveStream && message.content.isBlank(),
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        when {
            message.isError -> Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(14.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.size(10.dp))
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
            message.content.isBlank() && isActiveStream ->
                if (reasoning.isNullOrBlank()) TypingDots() else Unit
            else -> MarkdownText(markdown = message.content)
        }

        if (!isActiveStream && message.content.isNotBlank() && !message.isError) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp),
            ) {
                IconButton(
                    onClick = { clipboard.setText(AnnotatedString(message.content)) },
                    modifier = Modifier.size(34.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = "Copy response",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp),
                    )
                }
                if (canRegenerate) {
                    IconButton(onClick = onRegenerate, modifier = Modifier.size(34.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Regenerate response",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                message.model?.let { model ->
                    Text(
                        text = model,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }
        }
    }
}

/**
 * Collapsible "thinking" panel for reasoning-model output. Auto-expands while
 * the model is still reasoning and collapses once the final answer starts.
 */
@Composable
private fun ReasoningSection(
    reasoning: String,
    isThinking: Boolean,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    LaunchedEffect(isThinking) { expanded = isThinking }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "chevron",
    )

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Psychology,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = if (isThinking) "Thinking…" else "Reasoning",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (isThinking) {
                    CircularProgressIndicator(
                        strokeWidth = 1.5.dp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(12.dp),
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                }
                Icon(
                    imageVector = Icons.Rounded.ExpandMore,
                    contentDescription = if (expanded) "Collapse reasoning" else "Expand reasoning",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(18.dp)
                        .graphicsLayer { rotationZ = chevronRotation },
                )
            }
            AnimatedVisibility(visible = expanded) {
                Text(
                    text = remember(reasoning) { LatexMath.normalize(reasoning) },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun StatusPill(status: String) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(50),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
        ) {
            CircularProgressIndicator(
                strokeWidth = 1.5.dp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(12.dp),
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = status,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TypingDots(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "typing")
    Row(modifier = modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(3) { index ->
            val alpha by transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 700, delayMillis = index * 180, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "dot$index",
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .graphicsLayer { this.alpha = alpha }
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
            )
        }
    }
}
