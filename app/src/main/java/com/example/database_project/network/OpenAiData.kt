package com.example.database_project.network

import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val role: String,
    val content: String
)

@Serializable
data class OpenAiRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Double? = null
)

@Serializable
data class Choice(
    val index: Int,
    val message: Message
)

@Serializable
data class OpenAiResponse(
    val id: String,
    val choices: List<Choice>
)
