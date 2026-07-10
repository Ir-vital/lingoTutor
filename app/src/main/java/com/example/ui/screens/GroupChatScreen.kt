package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.ChatEntity
import com.example.ui.viewmodel.TutorViewModel

@Composable
fun GroupChatScreen(
    viewModel: TutorViewModel,
    modifier: Modifier = Modifier
) {
    val groupMessages by viewModel.groupMessages.collectAsStateWithLifecycle()
    val selectedTopic by viewModel.selectedTopic.collectAsStateWithLifecycle()
    val groupIsThinking by viewModel.groupIsThinking.collectAsStateWithLifecycle()
    val chatInputText by viewModel.chatInputText.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()

    val topics = listOf(
        "Ordering Food & Coffee",
        "Healthy Habits",
        "Artificial Intelligence",
        "My Dream Destination",
        "Sports & Activities",
        "Career Plans"
    )

    // Trigger greeting once if empty
    LaunchedEffect(selectedTopic, groupMessages.size) {
        if (groupMessages.isEmpty()) {
            viewModel.setGroupTopic(selectedTopic)
        }
    }

    // Scroll to bottom automatically
    LaunchedEffect(groupMessages.size) {
        if (groupMessages.isNotEmpty()) {
            listState.animateScrollToItem(groupMessages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- Persona Members Row ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "GROUP DISCUSSION PARTICIPANTS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ParticipantAvatar(name = "Sarah", flag = "🇺🇸", desc = "Fluent, Casual", color = Color(0xFFE91E63))
                    ParticipantAvatar(name = "Emily", flag = "🇬🇧", desc = "Fluent, Academic", color = Color(0xFF00BCD4))
                    ParticipantAvatar(name = "Jean", flag = "🇫🇷", desc = "Beginner Peer", color = Color(0xFFFF9800))
                    ParticipantAvatar(name = "Me", flag = "👤", desc = "English Student", color = MaterialTheme.colorScheme.primary, isUser = true)
                }
            }
        }

        // --- Horizontal Topic Selector Carousel ---
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(topics) { topic ->
                val isSelected = selectedTopic == topic
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                        .clickable { viewModel.setGroupTopic(topic) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("topic_tab_${topic.lowercase().replace(" ", "_")}")
                ) {
                    Text(
                        text = topic,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // --- Conversation Chat Logs ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("group_chat_lazy_column"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(groupMessages) { msg ->
                    GroupChatBubbleItem(message = msg)
                }

                if (groupIsThinking) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Sarah, Emily, and Jean are writing replies",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        strokeWidth = 1.5.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Chat Input Footer ---
        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = { viewModel.clearGroupChat() },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Restart Chat",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedTextField(
                    value = chatInputText,
                    onValueChange = { viewModel.updateChatInput(it) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("group_chat_input_field"),
                    placeholder = { Text("Contribute to the discussion...") },
                    maxLines = 3,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )

                IconButton(
                    onClick = { viewModel.sendGroupChatMessage() },
                    enabled = chatInputText.isNotBlank(),
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (chatInputText.isNotBlank()) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .testTag("group_chat_send_button")
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Send",
                        tint = if (chatInputText.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// --- Dynamic Person Avatars ---
@Composable
fun ParticipantAvatar(
    name: String,
    flag: String,
    desc: String,
    color: Color,
    isUser: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.width(64.dp)
    ) {
        Box(
            modifier = Modifier.size(44.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            // Main avatar circle
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = color
                )
            }

            // Flag badge
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .shadow(1.dp, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = flag, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = name,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = desc,
            fontSize = 8.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

// --- Group Bubble Item ---
@Composable
fun GroupChatBubbleItem(
    message: ChatEntity
) {
    val senderName = message.sender.lowercase().replaceFirstChar { it.uppercase() }
    val isUser = message.sender == "USER"

    // Custom colors per personality
    val bubbleColor = when (message.sender) {
        "USER" -> MaterialTheme.colorScheme.primary
        "SARAH" -> Color(0xFFFCE4EC) // Warm blush pink
        "EMILY" -> Color(0xFFE0F7FA) // Cool pale cyan
        "JEAN" -> Color(0xFFFFF3E0)  // Peach/orange
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = when (message.sender) {
        "USER" -> Color.White
        "SARAH" -> Color(0xFF880E4F)
        "EMILY" -> Color(0xFF006064)
        "JEAN" -> Color(0xFFE65100)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val flag = when (message.sender) {
        "SARAH" -> "🇺🇸"
        "EMILY" -> "🇬🇧"
        "JEAN" -> "🇫🇷"
        else -> "👤"
    }

    var expandedTranslation by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Participant Name label
        if (!isUser) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            ) {
                Text(text = flag, fontSize = 11.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = senderName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(0.85f),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 2.dp,
                            bottomEnd = if (isUser) 2.dp else 16.dp
                        )
                    )
                    .background(bubbleColor)
                    .clickable {
                        if (!message.frenchTranslation.isNullOrBlank() || !message.educationalNote.isNullOrBlank()) {
                            expandedTranslation = !expandedTranslation
                        }
                    }
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = message.text,
                        color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // Indicators that interactive translations are available
                    if (!isUser && (!message.frenchTranslation.isNullOrBlank() || !message.educationalNote.isNullOrBlank())) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = if (expandedTranslation) Icons.Default.ExpandLess else Icons.Default.Translate,
                                contentDescription = "Translate icon",
                                tint = textColor.copy(alpha = 0.7f),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (expandedTranslation) "Tap to collapse" else "Tap to translate / tips",
                                fontSize = 9.sp,
                                color = textColor.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Expanded translated box
        if (expandedTranslation) {
            AnimatedVisibility(
                visible = true,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(top = 4.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        if (!message.frenchTranslation.isNullOrBlank()) {
                            Text(
                                text = "Traduction:",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = message.frenchTranslation,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (!message.educationalNote.isNullOrBlank()) {
                            if (!message.frenchTranslation.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            Text(
                                text = "Note Éducative:",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = message.educationalNote,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
