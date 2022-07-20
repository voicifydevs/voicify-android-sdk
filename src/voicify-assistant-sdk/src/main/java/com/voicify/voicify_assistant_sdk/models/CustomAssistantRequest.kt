package com.voicify.voicify_assistant_sdk.models

data class CustomAssistantRequest(
    val requestId: String,
    val context: CustomAssistantRequestContext,
    val device: CustomAssistantDevice,
    val user: CustomAssistantUser
)
