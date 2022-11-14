package com.voicify.voicify_assistant_sdk.assistantDrawerUITypes

import java.io.Serializable


data class AssistantSettingsProps(
    val serverRootUrl: String,
    val appId: String,
    val appKey: String,
    val locale: String,
    val textToSpeechVoice: String,
    val channel: String,
    val device: String,
    val autoRunConversation: Boolean,
    val initializeWithWelcomeMessage: Boolean,
    val textToSpeechProvider: String,
    val useVoiceInput: Boolean,
    val useOutputSpeech: Boolean,
    val initializeWithText: Boolean,
    val useDraftContent: Boolean,
    val noTracking: Boolean? = false,
    val effects: Array<String>? = emptyArray() ): Serializable