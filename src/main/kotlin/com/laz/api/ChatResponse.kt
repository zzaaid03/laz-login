package com.laz.api

data class ChatResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: Message
)
