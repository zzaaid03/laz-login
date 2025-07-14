package com.laz.viewmodels

import androidx.lifecycle.ViewModel
import com.laz.api.ApiClient
import com.laz.api.ChatRequest
import com.laz.api.ChatResponse
import com.laz.api.Message
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ChatViewModel : ViewModel() {

    private val _chatReply = MutableStateFlow("Hi! How can I help you?")
    val chatReply: StateFlow<String> = _chatReply

    fun sendMessageToBot(userInput: String) {
        val messages = listOf(Message("user", userInput))
        val request = ChatRequest(messages = messages)

        ApiClient.retrofit.sendMessage(request).enqueue(object : Callback<ChatResponse> {
            override fun onResponse(call: Call<ChatResponse>, response: Response<ChatResponse>) {
                val reply = response.body()?.choices?.firstOrNull()?.message?.content ?: "No reply"
                _chatReply.value = reply
            }

            override fun onFailure(call: Call<ChatResponse>, t: Throwable) {
                _chatReply.value = "Error: ${t.localizedMessage}"
            }
        })
    }
}
