package com.laz.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier // Existing import
import androidx.compose.ui.Alignment // Add this import
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.laz.viewmodels.ChatViewModel

@Composable
fun ChatScreen(chatViewModel: ChatViewModel = viewModel()) {
    val chatReply by chatViewModel.chatReply.collectAsState()
    var userInput by remember { mutableStateOf("") }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {

        Text(text = "Bot: $chatReply", style = MaterialTheme.typography.bodyLarge)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = userInput,
            onValueChange = { userInput = it },
            label = { Text("Your Message") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                chatViewModel.sendMessageToBot(userInput)
                userInput = ""
            },
            modifier = Modifier.align(Alignment.End) // This line should now compile
        ) {
            Text("Send")
        }
    }
}
