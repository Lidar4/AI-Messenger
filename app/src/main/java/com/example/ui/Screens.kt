package com.example.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Conversation
import com.example.data.Message
import com.example.data.User
import com.example.data.StatusUpdate
import com.example.network.AudioCallManager.CallState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppContent(viewModel: ChatViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val callState by viewModel.callManager.callState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        when (currentScreen) {
            "AUTH" -> AuthScreen(viewModel)
            "MAIN" -> HomeScreen(viewModel)
            "CHAT_CONVERSATION" -> ChatScreen(viewModel)
            "ACTIVE_CALL" -> CallScreen(viewModel)
            else -> AuthScreen(viewModel)
        }

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
                            text = "Ongoing Audio/Video Call...",
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
fun AuthScreen(viewModel: ChatViewModel) {
    var isLogin by remember { mutableStateOf(true) }
    var showForgotPassword by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }

    val authError by viewModel.authError.collectAsStateWithLifecycle()
    val authSuccess by viewModel.authSuccessMessage.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Chat, contentDescription = "Logo", tint = Color.White, modifier = Modifier.size(40.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "AI Messenger",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Secure real-time messaging",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (!isLogin) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name") },
                    modifier = Modifier.fillMaxWidth().testTag("display_name_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Unique Username (e.g. @mf_fardus)") },
                    modifier = Modifier.fillMaxWidth().testTag("username_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                modifier = Modifier.fillMaxWidth().testTag("email_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (isLogin) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().testTag("password_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            if (authError != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = authError!!, color = Color.Red, fontSize = 14.sp)
            }

            if (authSuccess != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = authSuccess!!, color = Color.Green, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (isLogin) {
                        viewModel.login(email, password)
                    } else {
                        viewModel.register(email, password, username, displayName)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("auth_submit_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(text = if (isLogin) "Sign In" else "Create Account", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLogin) {
                TextButton(onClick = { showForgotPassword = true }) {
                    Text("Forgot Password?")
                }
            }

            TextButton(onClick = { isLogin = !isLogin }) {
                Text(text = if (isLogin) "Don't have an account? Sign Up" else "Already have an account? Sign In")
            }
        }
    }

    if (showForgotPassword) {
        var resetEmail by remember { mutableStateOf(email) }
        AlertDialog(
            onDismissRequest = { showForgotPassword = false },
            title = { Text("Reset Password") },
            text = {
                Column {
                    Text("Enter your email to receive a password reset link.")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.sendPasswordReset(resetEmail)
                    showForgotPassword = false
                }) {
                    Text("Send Link")
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPassword = false }) {
                    Text("Cancel")
                }
            }
        )
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
                    selected = activeTab == "UPDATES",
                    onClick = { viewModel.setActiveTab("UPDATES") },
                    icon = { Icon(Icons.Filled.Update, contentDescription = "Updates") },
                    label = { Text("Updates") }
                )
                NavigationBarItem(
                    selected = activeTab == "CALLS",
                    onClick = { viewModel.setActiveTab("CALLS") },
                    icon = { Icon(Icons.Filled.Call, contentDescription = "Calls") },
                    label = { Text("Calls") }
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
                "UPDATES" -> UpdatesTab(viewModel)
                "CALLS" -> CallsTab(viewModel)
                "SETTINGS" -> SettingsScreen(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsTab(viewModel: ChatViewModel) {
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var showNewChatDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Messenger", fontWeight = FontWeight.Black) },
                actions = {
                    IconButton(onClick = { showNewChatDialog = true }, modifier = Modifier.testTag("new_chat_button")) {
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
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search conversations...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("search_bar"),
                shape = RoundedCornerShape(24.dp)
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
                        Text("No conversations yet. Tap + to search @username", color = Color.Gray, textAlign = TextAlign.Center)
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
                                        Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                            Text(conversation.unreadCount.toString(), color = Color.White)
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
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (showNewChatDialog) {
        var usernameQuery by remember { mutableStateOf("") }
        val searchedUser by viewModel.searchedUser.collectAsStateWithLifecycle()

        AlertDialog(
            onDismissRequest = { showNewChatDialog = false },
            title = { Text("Start New Chat") },
            text = {
                Column {
                    OutlinedTextField(
                        value = usernameQuery,
                        onValueChange = {
                            usernameQuery = it
                            viewModel.searchUserByUsername(it)
                        },
                        placeholder = { Text("Enter @username...") },
                        modifier = Modifier.fillMaxWidth().testTag("username_search_input"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (searchedUser != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.startConversationWith(searchedUser!!) {
                                        showNewChatDialog = false
                                    }
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(searchedUser!!.displayName.take(1), color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(searchedUser!!.displayName, fontWeight = FontWeight.Bold)
                                    Text("@${searchedUser!!.username}", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }
                    } else if (usernameQuery.isNotBlank()) {
                        Text("No user found with this username", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {},
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
fun UpdatesTab(viewModel: ChatViewModel) {
    val statuses by viewModel.statuses.collectAsStateWithLifecycle()
    var showPostDialog by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Updates & Status", fontWeight = FontWeight.Black) })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showPostDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Post Status")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Text(
                text = "Recent Updates",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.primary
            )

            if (statuses.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No status updates from contacts yet", color = Color.Gray)
                }
            } else {
                LazyColumn {
                    items(statuses) { status ->
                        ListItem(
                            headlineContent = { Text(status.userName, fontWeight = FontWeight.Bold) },
                            supportingContent = { Text(status.text ?: "") },
                            trailingContent = {
                                Text(
                                    text = android.text.format.DateFormat.format("hh:mm a", status.timestamp).toString(),
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            },
                            leadingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = status.userName.take(1).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (showPostDialog) {
        AlertDialog(
            onDismissRequest = { showPostDialog = false },
            title = { Text("Post Status Update") },
            text = {
                OutlinedTextField(
                    value = statusText,
                    onValueChange = { statusText = it },
                    placeholder = { Text("What's on your mind?") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (statusText.isNotBlank()) {
                        viewModel.postStatus(statusText)
                        statusText = ""
                        showPostDialog = false
                    }
                }) {
                    Text("Post")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPostDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallsTab(viewModel: ChatViewModel) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Calls", fontWeight = FontWeight.Black) })
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Filled.Call,
                    contentDescription = "Calls",
                    modifier = Modifier.size(72.dp),
                    tint = Color.LightGray
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("WebRTC audio/video calls ready", color = Color.Gray)
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
    var inConversationSearch by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var selectedMessage by remember { mutableStateOf<Message?>(null) }
    var showMessageMenu by remember { mutableStateOf(false) }
    var replyMessage by remember { mutableStateOf<Message?>(null) }

    val clipboardManager: ClipboardManager = LocalClipboardManager.current

    BackHandler {
        viewModel.closeConversation()
    }

    if (conversation == null) return

    val displayedMessages = if (inConversationSearch.isNotBlank()) {
        messages.filter { it.text.contains(inConversationSearch, ignoreCase = true) }
    } else {
        messages
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (showSearch) {
                        OutlinedTextField(
                            value = inConversationSearch,
                            onValueChange = { inConversationSearch = it },
                            placeholder = { Text("Search chat...") },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            singleLine = true
                        )
                    } else {
                        Column {
                            Text(conversation!!.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Online • Encrypted", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.closeConversation() }, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        showSearch = !showSearch
                        if (!showSearch) inConversationSearch = ""
                    }) {
                        Icon(Icons.Filled.Search, contentDescription = "Search in chat")
                    }
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
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                reverseLayout = false
            ) {
                items(displayedMessages) { message ->
                    val isMe = !message.isIncoming
                    val bubbleColor = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    val textColor = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .combinedClickable(
                                onClick = {},
                                onLongClick = {
                                    selectedMessage = message
                                    showMessageMenu = true
                                }
                            ),
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
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                if (message.replyToText != null) {
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                                    ) {
                                        Text(
                                            text = message.replyToText,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(6.dp),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Text(message.text, color = textColor)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    if (message.isStarred) {
                                        Icon(
                                            Icons.Filled.Star,
                                            contentDescription = "Starred",
                                            tint = Color.Yellow,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = android.text.format.DateFormat.format("hh:mm a", message.timestamp).toString(),
                                        fontSize = 10.sp,
                                        color = textColor.copy(alpha = 0.7f)
                                    )
                                    if (isMe) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Filled.DoneAll,
                                            contentDescription = "Status",
                                            tint = textColor.copy(alpha = 0.7f),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (replyMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Replying to ${replyMessage!!.senderName}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(replyMessage!!.text, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = { replyMessage = null }) {
                            Icon(Icons.Filled.Close, contentDescription = "Cancel Reply")
                        }
                    }
                }
            }

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
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Message...") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input"),
                        shape = RoundedCornerShape(24.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (inputText.trim().isNotEmpty()) {
                                viewModel.sendMessage(
                                    text = inputText,
                                    replyToId = replyMessage?.id,
                                    replyToText = replyMessage?.text
                                )
                                inputText = ""
                                replyMessage = null
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

    if (showMessageMenu && selectedMessage != null) {
        AlertDialog(
            onDismissRequest = { showMessageMenu = false },
            title = { Text("Message Options") },
            text = {
                Column {
                    TextButton(onClick = {
                        clipboardManager.setText(AnnotatedString(selectedMessage!!.text))
                        showMessageMenu = false
                    }) {
                        Text("Copy Text")
                    }
                    TextButton(onClick = {
                        replyMessage = selectedMessage
                        showMessageMenu = false
                    }) {
                        Text("Reply")
                    }
                    TextButton(onClick = {
                        viewModel.toggleStarMessage(selectedMessage!!.id, selectedMessage!!.isStarred)
                        showMessageMenu = false
                    }) {
                        Text(if (selectedMessage!!.isStarred) "Unstar Message" else "Star Message")
                    }
                    TextButton(onClick = {
                        viewModel.deleteMessage(selectedMessage!!.id)
                        showMessageMenu = false
                    }) {
                        Text("Delete Message", color = Color.Red)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showMessageMenu = false }) {
                    Text("Cancel")
                }
            }
        )
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
                    Text(text = "Establishing WebRTC link...", color = Color.Gray, fontSize = 14.sp)
                }
            }

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

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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

                    Button(
                        onClick = { viewModel.callManager.endCall() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        modifier = Modifier.size(72.dp),
                        shape = CircleShape
                    ) {
                        Icon(Icons.Filled.CallEnd, contentDescription = "End Call", tint = Color.White)
                    }

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: ChatViewModel) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val darkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val notifications by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val readReceipts by viewModel.readReceiptsEnabled.collectAsStateWithLifecycle()

    var showEditProfile by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(userProfile?.displayName ?: "") }
    var editBio by remember { mutableStateOf(userProfile?.bio ?: "") }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings", fontWeight = FontWeight.Black) })
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
                    .clickable { showEditProfile = true },
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
                        Text(
                            text = userProfile?.displayName?.take(1)?.uppercase() ?: "U",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userProfile?.displayName ?: "User",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "@${userProfile?.username ?: "username"}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = userProfile?.bio ?: "",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(Icons.Filled.Edit, contentDescription = "Edit Profile")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Preferences", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            ListItem(
                headlineContent = { Text("Dark Theme") },
                trailingContent = {
                    Switch(checked = darkMode, onCheckedChange = { viewModel.toggleDarkMode(it) })
                },
                leadingContent = { Icon(Icons.Filled.DarkMode, contentDescription = "Theme") }
            )
            HorizontalDivider()

            ListItem(
                headlineContent = { Text("Notifications") },
                trailingContent = {
                    Switch(checked = notifications, onCheckedChange = { viewModel.toggleNotifications(it) })
                },
                leadingContent = { Icon(Icons.Filled.Notifications, contentDescription = "Notifications") }
            )
            HorizontalDivider()

            ListItem(
                headlineContent = { Text("Read Receipts") },
                trailingContent = {
                    Switch(checked = readReceipts, onCheckedChange = { viewModel.toggleReadReceipts(it) })
                },
                leadingContent = { Icon(Icons.Filled.DoneAll, contentDescription = "Privacy") }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text("Storage & Data", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            ListItem(
                headlineContent = { Text("Network Usage & Media Cache") },
                supportingContent = { Text("Firestore Realtime Database & Cloud Storage") },
                leadingContent = { Icon(Icons.Filled.Storage, contentDescription = "Storage") }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text("About", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            ListItem(
                headlineContent = { Text("App Version") },
                supportingContent = { Text("1.0.0 (Production)") },
                leadingContent = { Icon(Icons.Filled.Info, contentDescription = "About") }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { viewModel.logout() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Sign Out", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }

    if (showEditProfile) {
        AlertDialog(
            onDismissRequest = { showEditProfile = false },
            title = { Text("Edit Profile") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Display Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editBio,
                        onValueChange = { editBio = it },
                        label = { Text("About / Bio") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateProfile(editName, editBio)
                    showEditProfile = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfile = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
