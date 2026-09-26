package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CallLog
import java.util.UUID
import kotlinx.coroutines.launch
import com.example.data.Conversation
import com.example.data.Message
import com.example.network.AudioCallManager.CallState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppContent(viewModel: ChatViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val callState by viewModel.callManager.callState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        when (currentScreen) {
            "MAIN" -> HomeScreen(viewModel)
            "CHAT_CONVERSATION" -> ChatScreen(viewModel)
            "ACTIVE_CALL" -> CallScreen(viewModel)
            else -> HomeScreen(viewModel)
        }

        // Keep call overlay visible if a call is active but we navigated away
        if (callState !is CallState.Idle && currentScreen != "ACTIVE_CALL") {
            Card(
                onClick = { viewModel.navigateToScreen("ACTIVE_CALL") },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Call, contentDescription = "Active Call", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ongoing Audio Call...",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    TextButton(onClick = { viewModel.callManager.endCall() }) {
                        Text("Hang Up", color = Color.Red)
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(viewModel: ChatViewModel) {
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = activeTab == "CHATS",
                    onClick = { viewModel.setActiveTab("CHATS") },
                    icon = { Icon(Icons.Filled.Chat, contentDescription = "Chats") },
                    label = { Text("Chats") }
                )
                NavigationBarItem(
                    selected = activeTab == "CALLS",
                    onClick = { viewModel.setActiveTab("CALLS") },
                    icon = { Icon(Icons.Filled.Call, contentDescription = "Calls") },
                    label = { Text("Calls") }
                )
                NavigationBarItem(
                    selected = activeTab == "AI",
                    onClick = { viewModel.setActiveTab("AI") },
                    icon = { Icon(Icons.Filled.AutoAwesome, contentDescription = "AI") },
                    label = { Text("AI Assist") }
                )
                NavigationBarItem(
                    selected = activeTab == "SETTINGS",
                    onClick = { viewModel.setActiveTab("SETTINGS") },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                "CHATS" -> ConversationsTab(viewModel)
                "CALLS" -> CallsTab(viewModel)
                "AI" -> AIScreen(viewModel)
                "SETTINGS" -> SettingsScreen(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsTab(viewModel: ChatViewModel) {
    val conversations by viewModel.allConversations.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var showNewChatDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Messenger", fontWeight = FontWeight.Black) },
                actions = {
                    IconButton(onClick = { showNewChatDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "New Chat")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search conversations...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("search_bar"),
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            val filteredList = conversations.filter {
                it.title.contains(searchQuery, ignoreCase = true)
            }

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.ChatBubbleOutline,
                            contentDescription = "No chats",
                            modifier = Modifier.size(72.dp),
                            tint = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No conversations found", color = Color.Gray)
                    }
                }
            } else {
                LazyColumn {
                    items(filteredList) { conversation ->
                        ListItem(
                            headlineContent = { Text(conversation.title, fontWeight = FontWeight.Bold) },
                            supportingContent = {
                                Text(
                                    conversation.lastMessageText ?: "No messages yet",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            trailingContent = {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = android.text.format.DateFormat.format("hh:mm a", conversation.lastMessageTime).toString(),
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                    if (conversation.unreadCount > 0) {
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 4.dp)
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = conversation.unreadCount.toString(),
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            },
                            leadingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = conversation.title.take(1).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            },
                            modifier = Modifier
                                .clickable { viewModel.openConversation(conversation) }
                                .animateItem()
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (showNewChatDialog) {
        var newChatName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewChatDialog = false },
            title = { Text("Start New Chat") },
            text = {
                OutlinedTextField(
                    value = newChatName,
                    onValueChange = { newChatName = it },
                    placeholder = { Text("Enter recipient name...") },
                    modifier = Modifier.fillMaxWidth().testTag("new_chat_name")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newChatName.isNotEmpty()) {
                            val newConv = Conversation(
                                id = "conv_${UUID.randomUUID()}",
                                title = newChatName,
                                isGroup = false,
                                participantsJson = "user_me,peer_new"
                            )
                            scope.launch {
                                viewModel.repository.conversationDao.insertConversation(newConv)
                            }
                            showNewChatDialog = false
                        }
                    }
                ) {
                    Text("Start")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewChatDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallsTab(viewModel: ChatViewModel) {
    val callLogs by viewModel.callLogs.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Call Logs", fontWeight = FontWeight.Black) })
        }
    ) { padding ->
        if (callLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.Call,
                        contentDescription = "No calls",
                        modifier = Modifier.size(72.dp),
                        tint = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No calls recorded yet", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(callLogs) { log ->
                    ListItem(
                        headlineContent = { Text(log.userName, fontWeight = FontWeight.Bold) },
                        supportingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (log.isIncoming) Icons.Filled.CallReceived else Icons.Filled.CallMade,
                                    contentDescription = if (log.isIncoming) "Incoming" else "Outgoing",
                                    tint = if (log.status == "MISSED") Color.Red else Color.Green,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${log.status} • ${log.durationSeconds}s",
                                    color = Color.Gray
                                )
                            }
                        },
                        trailingContent = {
                            IconButton(onClick = { viewModel.initiateCall(log.userName) }) {
                                Icon(Icons.Filled.Call, contentDescription = "Call Back", tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.LightGray),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Person, contentDescription = "User", tint = Color.White)
                            }
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    val conversation by viewModel.activeConversation.collectAsStateWithLifecycle()
    val messages by viewModel.activeMessages.collectAsStateWithLifecycle()
    var inputText by remember { mutableStateOf("") }
    val isRecording by viewModel.isRecordingVoice.collectAsStateWithLifecycle()

    var showAIContextDialog by remember { mutableStateOf(false) }
    var selectedMessageForAI by remember { mutableStateOf<Message?>(null) }
    var aiResultText by remember { mutableStateOf("") }
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()

    BackHandler {
        viewModel.closeConversation()
    }

    if (conversation == null) return

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(conversation!!.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Online • Secured", fontSize = 12.sp, color = Color.Gray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.closeConversation() }, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.initiateCall(conversation!!.title) }) {
                        Icon(Icons.Filled.Call, contentDescription = "Voice Call")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Message List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                reverseLayout = false
            ) {
                items(messages) { message ->
                    val isMe = !message.isIncoming
                    val bubbleColor = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    val textColor = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = bubbleColor),
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isMe) 16.dp else 0.dp,
                                bottomEnd = if (isMe) 0.dp else 16.dp
                            ),
                            modifier = Modifier
                                .widthIn(max = 280.dp)
                                .combinedClickable(
                                    onClick = {},
                                    onLongClick = {
                                        selectedMessageForAI = message
                                        showAIContextDialog = true
                                    }
                                )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                if (message.attachmentType == "VOICE") {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.PlayArrow, contentDescription = "Play voice message", tint = textColor)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Voice Message (0:04)", color = textColor, fontWeight = FontWeight.Medium)
                                    }
                                } else {
                                    Text(message.text, color = textColor)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text(
                                        text = android.text.format.DateFormat.format("hh:mm a", message.timestamp).toString(),
                                        fontSize = 10.sp,
                                        color = textColor.copy(alpha = 0.7f)
                                    )
                                    if (isMe) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = when (message.status) {
                                                "SENT" -> Icons.Filled.Check
                                                "READ" -> Icons.Filled.DoneAll
                                                else -> Icons.Filled.Schedule
                                            },
                                            contentDescription = "Status",
                                            tint = textColor.copy(alpha = 0.7f),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = message.transportUsed,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textColor.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Input Composer
            Surface(
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .windowInsetsPadding(WindowInsets.navigationBars),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (isRecording) {
                                viewModel.stopAndSendVoiceRecording()
                            } else {
                                viewModel.startVoiceRecording()
                            }
                        },
                        modifier = Modifier.testTag("voice_record_button")
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                            contentDescription = "Record Voice",
                            tint = if (isRecording) Color.Red else MaterialTheme.colorScheme.primary
                        )
                    }

                    OutlinedTextField(
                        value = if (isRecording) "Recording voice message..." else inputText,
                        onValueChange = { if (!isRecording) inputText = it },
                        placeholder = { Text("Message...") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input"),
                        shape = RoundedCornerShape(24.dp),
                        readOnly = isRecording
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (inputText.trim().isNotEmpty()) {
                                viewModel.sendMessage(inputText)
                                inputText = ""
                            }
                        },
                        modifier = Modifier.testTag("send_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }

    if (showAIContextDialog && selectedMessageForAI != null) {
        AlertDialog(
            onDismissRequest = {
                showAIContextDialog = false
                aiResultText = ""
            },
            title = { Text("Ask AI Assistant") },
            text = {
                Column {
                    Text(
                        text = "Selected text: \"${selectedMessageForAI!!.text}\"",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (isAiLoading) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (aiResultText.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp).verticalScroll(rememberScrollState())
                        ) {
                            Text(aiResultText, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    } else {
                        // Quick Actions
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Column {
                                TextButton(onClick = {
                                    viewModel.executeMessageAIContextAction(selectedMessageForAI!!, "SUMMARIZE") { aiResultText = it }
                                }) { Text("Summarize") }
                                TextButton(onClick = {
                                    viewModel.executeMessageAIContextAction(selectedMessageForAI!!, "TRANSLATE") { aiResultText = it }
                                }) { Text("Translate") }
                            }
                            Column {
                                TextButton(onClick = {
                                    viewModel.executeMessageAIContextAction(selectedMessageForAI!!, "REWRITE") { aiResultText = it }
                                }) { Text("Rewrite") }
                                TextButton(onClick = {
                                    viewModel.executeMessageAIContextAction(selectedMessageForAI!!, "EXPLAIN") { aiResultText = it }
                                }) { Text("Explain") }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (aiResultText.isNotEmpty()) {
                    Button(
                        onClick = {
                            viewModel.sendMessage(aiResultText, replyToId = selectedMessageForAI!!.id, replyToText = selectedMessageForAI!!.text)
                            showAIContextDialog = false
                            aiResultText = ""
                        }
                    ) {
                        Text("Send Reply")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAIContextDialog = false
                        aiResultText = ""
                    }
                ) {
                    Text("Close")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIScreen(viewModel: ChatViewModel) {
    val aiMessages by viewModel.aiMessages.collectAsStateWithLifecycle()
    val aiMode by viewModel.aiWorkspaceMode.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()
    var aiInputText by remember { mutableStateOf("") }

    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Assist Workspace", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Mode Selectors
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "GENERAL" to "General Chat",
                    "CODING" to "Coding Helper",
                    "DEBUGGING" to "Debugger",
                    "WRITING" to "Writer",
                    "RESEARCH" to "Research",
                    "STUDY" to "Study Coach",
                    "DATA" to "Data Sage",
                    "APP_BUILDING" to "App Builder",
                    "LANGUAGE" to "Language translation"
                ).forEach { (modeKey, modeName) ->
                    FilterChip(
                        selected = aiMode == modeKey,
                        onClick = { viewModel.setAIWorkspaceMode(modeKey) },
                        label = { Text(modeName) }
                    )
                }
            }

            // Chat Viewport
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                items(aiMessages) { msg ->
                    val isAi = msg.role == "assistant"
                    val bubbleColor = if (isAi) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer
                    val textColor = if (isAi) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        contentAlignment = if (isAi) Alignment.CenterStart else Alignment.CenterEnd
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = bubbleColor),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.widthIn(max = 300.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = if (isAi) "AI Assist (${aiMode})" else "You",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = textColor.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(msg.text, color = textColor)
                            }
                        }
                    }
                }

                if (isAiLoading) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("AI is thinking...", color = Color.Gray)
                        }
                    }
                }
            }

            // Input Row
            Surface(
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .windowInsetsPadding(WindowInsets.navigationBars),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = aiInputText,
                        onValueChange = { aiInputText = it },
                        placeholder = { Text("Ask anything...") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ai_input"),
                        shape = RoundedCornerShape(24.dp),
                        trailingIcon = {
                            IconButton(onClick = {
                                // Simulate Voice Input to AI
                                aiInputText = "Summarize local mesh architecture"
                            }) {
                                Icon(Icons.Filled.Mic, contentDescription = "Voice Input")
                            }
                        }
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (aiInputText.trim().isNotEmpty()) {
                                viewModel.askAiWorkspace(aiInputText)
                                aiInputText = ""
                            }
                        },
                        modifier = Modifier.testTag("ask_ai_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Submit", tint = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
    }
}

@Composable
fun CallScreen(viewModel: ChatViewModel) {
    val callState by viewModel.callManager.callState.collectAsStateWithLifecycle()
    val isMuted by viewModel.callManager.isMuted.collectAsStateWithLifecycle()
    val isSpeakerOn by viewModel.callManager.isSpeakerOn.collectAsStateWithLifecycle()
    val durationSeconds by viewModel.callManager.callDurationSeconds.collectAsStateWithLifecycle()

    val peerName = when (val state = callState) {
        is CallState.Outgoing -> state.peerName
        is CallState.Incoming -> state.peerName
        is CallState.Connected -> state.peerName
        is CallState.Disconnected -> "Call Ended"
        else -> "Contact"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 64.dp)
        ) {
            // Header Info
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = when (callState) {
                        is CallState.Outgoing -> "OUTGOING CALL"
                        is CallState.Incoming -> "INCOMING CALL"
                        is CallState.Connected -> "CONNECTED"
                        else -> "CALL ENDED"
                    },
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = peerName,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (callState is CallState.Connected) {
                    val minutes = durationSeconds / 60
                    val seconds = durationSeconds % 60
                    Text(
                        text = String.format("%02d:%02d", minutes, seconds),
                        color = Color.Green,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(text = "Securing link...", color = Color.Gray, fontSize = 14.sp)
                }
            }

            // Big Avatar
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = "Peer Avatar",
                    modifier = Modifier.size(96.dp),
                    tint = Color.Gray
                )
            }

            // Controls
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (callState is CallState.Incoming) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(32.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Accept Call
                        Button(
                            onClick = { viewModel.callManager.acceptIncomingCall() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                            modifier = Modifier.size(64.dp),
                            shape = CircleShape
                        ) {
                            Icon(Icons.Filled.Call, contentDescription = "Accept", tint = Color.White)
                        }

                        // Decline Call
                        Button(
                            onClick = { viewModel.callManager.endCall() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            modifier = Modifier.size(64.dp),
                            shape = CircleShape
                        ) {
                            Icon(Icons.Filled.CallEnd, contentDescription = "Decline", tint = Color.White)
                        }
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mute button
                        IconButton(
                            onClick = { viewModel.callManager.toggleMute() },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(if (isMuted) Color.White else Color(0xFF1E293B))
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                                contentDescription = "Mute",
                                tint = if (isMuted) Color.Black else Color.White
                            )
                        }

                        // End Call
                        Button(
                            onClick = { viewModel.callManager.endCall() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            modifier = Modifier.size(72.dp),
                            shape = CircleShape
                        ) {
                            Icon(Icons.Filled.CallEnd, contentDescription = "End Call", tint = Color.White)
                        }

                        // Speaker button
                        IconButton(
                            onClick = { viewModel.callManager.toggleSpeaker() },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(if (isSpeakerOn) Color.White else Color(0xFF1E293B))
                        ) {
                            Icon(
                                imageVector = Icons.Filled.VolumeUp,
                                contentDescription = "Speaker",
                                tint = if (isSpeakerOn) Color.Black else Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: ChatViewModel) {
    val meshEnabled by viewModel.isMeshEnabled.collectAsStateWithLifecycle()
    val peers by viewModel.discoveredPeers.collectAsStateWithLifecycle()
    val connectionStatus by viewModel.meshConnectionStatus.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("AI Settings & Mesh", fontWeight = FontWeight.Black) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Profile Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("ME", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Me (My Account)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Handle: @me_handle", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            }

            Text("RESILIENT COMMUNICATION MESH", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            // Mesh settings control
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("P2P Nearby Mesh Mode", fontWeight = FontWeight.Bold)
                            Text("Enables secure local socket fallback when internet fails.", fontSize = 12.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = meshEnabled,
                            onCheckedChange = { viewModel.toggleMeshNetwork(it) },
                            modifier = Modifier.testTag("mesh_toggle")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Mesh Link Status:")
                        Text(
                            text = connectionStatus,
                            color = if (meshEnabled) Color.Green else Color.Red,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (meshEnabled) {
                Text("DISCOVERED NEARBY NODES", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (peers.isEmpty()) {
                            Text("Scanning subnet for nearby nodes...", color = Color.Gray, fontSize = 14.sp)
                        } else {
                            peers.forEach { peer ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(peer.name, fontWeight = FontWeight.Bold)
                                        Text("IP: ${peer.ipAddress}", fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (peer.isOnline) Color.Green else Color.LightGray)
                                    )
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }

            // Legal & Data Safety Section
            Text("DATA SAFETY & PRIVACY DISCLOSURE", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("• Local Persistence", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Your conversations and credentials remain securely encrypted in our local Room database.", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("• Self-Hosted AI Backend", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("AI prompts are transmitted securely via HTTPS to your self-hosted AI backend. No commercial API keys or cloud credentials are required.", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("• Secured Edge Cryptography", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Plaintext data never leaves the device. Transmissions are encrypted using standard AES-GCM.", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }
}
