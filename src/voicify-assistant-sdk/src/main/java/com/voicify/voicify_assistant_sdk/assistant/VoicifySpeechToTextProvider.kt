package com.voicify.voicify_assistant_sdk.assistant

data class VoicifySpeechToTextProvider (
    val initialize: (locale: String) -> Unit,
    val addPartialListener: (callback: (partialResult: String) -> Unit) -> Unit,
    val addFinishListener: (callback: (fullResult: String) -> Unit) -> Unit,
    val addErrorListener: (callback: (partialResult: String) -> Unit) -> Unit,
    val startListening: () -> Unit,
    val stopListening: () -> Unit,
)