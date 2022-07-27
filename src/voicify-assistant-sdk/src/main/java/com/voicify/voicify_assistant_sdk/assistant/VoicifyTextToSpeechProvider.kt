package com.voicify.voicify_assistant_sdk.assistant

interface VoicifyTextToSpeechProvider {
    fun initialize (locale: String)
    fun speakSsml (ssml: String)
    fun addFinishListener (callback: () -> Unit)
    fun clearHandlers ()
    fun stop ()
}