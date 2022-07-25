package com.voicify.voicify_assistant_sdk.assistant

interface VoicifySpeechToTextProvider {
    fun initialize(locale: String)
    fun addPartialListener(callback: (partialResult: String?) -> Unit)
    fun addFinalResultListener (callback: (fullResult: String?) -> Unit)
    fun addErrorListener (callback: (partialResult: String) -> Unit)
    fun startListening ()
    fun stopListening ()
}