package com.voicify.voicify_assistant_sdk.models

data class CustomAssistantRequest(
    val requestId: String,
    val context: CustomAssistantRequestContext,
    val device: CustomAssistantUser,
    val user: CustomAssistantUser
)
