package com.voicify.voicify_assistant_sdk.models

data class VoicifySessionData(
    val id: String? = null,
    val sessionFlags: Array<String>? = null,
    val sessionAttributes: Map<String, Any>? = null,
)
