package com.voicify.voicify_assistant_sdk.assistantDrawerUITypes

import java.io.Serializable


data class AssistantSettingsProps(
    val configurationId: String? = null,
    val serverRootUrl: String,
    val appId: String,
    val appKey: String,
    val locale: String? = "en-US",
    val textToSpeechVoice: String? = "",
    val channel: String? = "Android",
    val device: String? = "Mobile",
    val autoRunConversation: Boolean? = false,
    val initializeWithWelcomeMessage: Boolean? = false,
    val textToSpeechProvider: String? = "Google",
    val useVoiceInput: Boolean? = true,
    val useOutputSpeech: Boolean? = true,
    val initializeWithText: Boolean? = false,
    val useDraftContent: Boolean? = false,
    val noTracking: Boolean? = false,
    val backgroundColor: String? = "",
    val effects: Array<String>? = emptyArray() ): Serializable