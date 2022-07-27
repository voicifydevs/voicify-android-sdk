package com.voicify.voicify_assistant_sdk.assistant

data class VoicifyTextToSpeechSettings(
    val appId: String,
    val appKey: String,
    val voice: String,
    val serverRootUrl: String,
    val provider: String
)

//TTSVoiceProviderSetting