package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AppContent(viewModel: ChatViewModel) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val selected by viewModel.selectedConversation.collectAsStateWithLifecycle()
    if (selected != null && profile != null) ChatScreen(viewModel, selected!!)
    else if (profile == null) AuthScreen(viewModel)
    else HomeScreen(viewModel)
}

@Composable
private fun AuthScreen(viewModel: ChatViewModel) {
    var signUp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Messenger", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text(
                if (!viewModel.repository.isConfigured) "Firebase connection is required."
                else if (signUp) "Create your account" else "Sign in",
                color = Color.Gray
            )
            if (signUp) {
                OutlinedTextField(name, { name = it }, label = { Text("Display name") }, singleLine = true)
                OutlinedTextField(username, { username = it }, label = { Text("Unique username") }, singleLine = true)
            }
            OutlinedTextField(email, { email = it }, label = { Text("Email") }, singleLine = true)
            OutlinedTextField(password, { password = it }, label = { Text("Password") }, singleLine = true)
            Button(
                onClick = { if (signUp) viewModel.signUp(email, password, name, username) else viewModel.signIn(email, password) },
                enabled = !busy && viewModel.repository.isConfigured
            ) {
                if (busy) CircularProgressIndicator(Modifier.size(18.dp)) else Text(if (signUp) "Create account" else "Sign in")
            }
            Text(
                if (signUp) "Already have an account? Sign in" else "New here? Create an account",
                modifier = Modifier.clickable { signUp = !signUp },
                color = MaterialTheme.colorScheme.primary
            )
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun HomeScreen(viewModel: ChatViewModel) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val contact by viewModel.contactResult.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(profile?.displayName ?: "Messenger") },
            actions = { IconButton(onClick = viewModel::signOut) { Icon(Icons.Default.Logout, "Sign out") } }
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            OutlinedTextField(
                query, { query = it }, Modifier.fillMaxWidth(), singleLine = true,
                label = { Text("Find by @username") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = { IconButton(onClick = { viewModel.findContact(query) }) { Icon(Icons.Default.Chat, "Find") } }
            )
            contact?.let { person ->
                Card(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Avatar(person.displayName)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(person.displayName, fontWeight = FontWeight.Bold)
                            Text("@" + person.username, color = Color.Gray)
                        }
                        Button(onClick = { viewModel.startConversation(person) }) { Text("Chat") }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Chats", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            if (conversations.isEmpty()) {
                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Chat, null, Modifier.size(48.dp), tint = Color.Gray)
                    Text("No chats yet.")
                    Text("Search a username above to start one.", color = Color.Gray)
                }
            } else {
                LazyColumn {
                    items(conversations, key = { it.id }) { c ->
                        Row(
                            Modifier.fillMaxWidth().clickable { viewModel.openConversation(c) }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Avatar(c.otherDisplayName)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(c.otherDisplayName, fontWeight = FontWeight.Bold)
                                Text("@" + c.otherUsername, color = Color.Gray)
                                if (c.lastMessage.isNotBlank()) Text(c.lastMessage, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatScreen(viewModel: ChatViewModel, conversation: com.example.data.MessengerConversation) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    var text by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(conversation.otherDisplayName, fontWeight = FontWeight.Bold)
                        Text("@" + conversation.otherUsername, style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = { IconButton(onClick = viewModel::closeConversation) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = { IconButton(onClick = { }) { Icon(Icons.Default.Call, "Voice call") } }
            )
        },
        bottomBar = {
            Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    text, { text = it }, Modifier.weight(1f), placeholder = { Text("Message") }, singleLine = true
                )
                IconButton(
                    onClick = { viewModel.sendMessage(text); text = "" },
                    enabled = text.isNotBlank()
                ) { Icon(Icons.Default.Send, "Send") }
            }
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(messages, key = { it.id }) { message ->
                val mine = message.senderId == viewModel.profile.value?.uid
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
                    Box(
                        Modifier.clip(RoundedCornerShape(16.dp))
                            .background(if (mine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) { Text(message.text) }
                }
            }
        }
    }
}

@Composable
private fun Avatar(name: String) {
    Box(Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
        Text(name.firstOrNull()?.uppercase() ?: "?", color = Color.White, fontWeight = FontWeight.Bold)
    }
}
