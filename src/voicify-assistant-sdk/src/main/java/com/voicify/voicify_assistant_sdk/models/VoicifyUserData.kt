package com.voicify.voicify_assistant_sdk.models

data class VoicifyUserData(
    val id: String? = null,
    val userFlags: Array<String>? = emptyArray(),
    val sessionAttributes: Map<String, Any>? = emptyMap(),
)
