package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.ui.AppContent
import com.example.ui.ChatViewModel
import com.example.ui.theme.MyApplicationTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val startupResult = runCatching {
            ViewModelProvider(this)[ChatViewModel::class.java]
        }

        setContent {
            MyApplicationTheme {
                val viewModel = startupResult.getOrNull()
                if (viewModel != null) {
                    AppContent(viewModel = viewModel)
                } else {
                    StartupErrorScreen(
                        message = startupResult.exceptionOrNull()?.message
                            ?: "The app could not initialize its backend."
                    )
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun StartupErrorScreen(message: String) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "AI Messenger",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "Startup configuration is missing.",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 12.dp)
            )
            Text(
                text = "Firebase is not configured in this build, so the messenger cannot start its account service. The app is kept open instead of crashing.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 12.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 12.dp)
            )
            Button(
                onClick = { /* informational screen */ },
                modifier = Modifier.padding(top = 20.dp)
            ) {
                Text("Configuration Required")
            }
        }
    }
}
