package com.voicify.voicify_assistant_sdk.assistant

data class VoicifyTextToSpeechProvider (
    val initialize: (locale: String) -> Unit,
    val speakSsml: (ssml: String) -> Unit,
    val addFinishListener: (callback: (fullResult: String) -> Unit) -> Unit,
    val clearHandlers: () -> Unit,
    val stop: () -> Unit,
)