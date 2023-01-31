package com.voicify.voicify_assistant_sdk.assistantDrawerUITypes

import java.io.Serializable


data class AssistantSettingsProps(
    val configurationId: String? = null,
    val serverRootUrl: String,
    val appId: String,
    val appKey: String,
    val locale: String? = null,
    val textToSpeechVoice: String? = null,
    val channel: String? = null,
    val device: String? = null,
    val autoRunConversation: Boolean? = null,
    val initializeWithWelcomeMessage: Boolean? = null,
    val textToSpeechProvider: String? = null,
    val useVoiceInput: Boolean? = null,
    val useOutputSpeech: Boolean? = null,
    val initializeWithText: Boolean? = null,
    val useDraftContent: Boolean? = null,
    val noTracking: Boolean? = null,
    val backgroundColor: String? = null,
    val effects: Array<String>? = emptyArray() ): Serializable