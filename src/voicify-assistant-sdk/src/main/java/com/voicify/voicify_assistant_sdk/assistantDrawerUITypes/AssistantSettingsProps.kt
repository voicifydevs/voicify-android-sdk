package com.voicify.voicify_assistant_sdk.assistantDrawerUITypes

import com.voicify.voicify_assistant_sdk.models.CustomAssistantResponse
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
    val useVoiceInput: Boolean,
    val useOutputSpeech: Boolean,
    val initializeWithText: Boolean? = false,
    val effects: Array<String>? = emptyArray(),
    val onEffect: ((effect: String, data: Any) -> Unit)? = null): Serializable
