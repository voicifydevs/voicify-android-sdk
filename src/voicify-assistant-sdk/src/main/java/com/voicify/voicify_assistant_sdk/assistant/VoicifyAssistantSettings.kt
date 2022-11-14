package com.voicify.voicify_assistant_sdk.assistant

data class VoicifyAssistantSettings(
    val serverRootUrl: String,
    val appId: String,
    val appKey: String,
    val locale: String,
    val channel: String,
    val device: String,
    val noTracking: Boolean,
    val autoRunConversation: Boolean,
    val initializeWithWelcomeMessage: Boolean,
    val initializeWithText: Boolean,
    val useVoiceInput: Boolean,
    val useDraftContent: Boolean,
    val useOutputSpeech: Boolean
)
