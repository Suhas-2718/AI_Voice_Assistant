package com.example.ai_voice_assistant

data class Message(
    val sender: String,   // "User" or "Bot"
    val text: String
)
