package com.voicify.voicify_assistant_sdk.assistantDrawerUITypes

import java.io.Serializable


data class AssistantSettingsProps(
    val serverRootUrl: String,
    val appId: String,
    val appKey: String,
    val locale: String,
    val channel: String,
    val device: String,
    val autoRunConversation: Boolean,
    val initializeWithWelcomeMessage: Boolean,
    val textToSpeechProvider: String,
    val useVoiceInput: Boolean,
    val useOutputSpeech: Boolean,
    val initializeWithText: Boolean,
    val effects: Array<String>? = emptyArray() ): Serializable