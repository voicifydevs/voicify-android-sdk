package com.voicify.voicify_assistant_sdk.models

data class CustomAssistantUser(
    val id: String,
    val name: String? = null,
    val accessToken: String? = null,
    val additionalUserAttributes: Map<String, Any>? = null,
    val additionalUserFlags: Array<String>? = null
)
