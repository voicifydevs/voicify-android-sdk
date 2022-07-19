package com.voicify.voicify_assistant_sdk.models

data class CustomAssistantRequestContext(
    val sessionId: String,
    val noTracking: Boolean? = false,
    val requestType: String,
    val requestName: String? = null,
    val slots: Map<String, String>? = null,
    val originalInput: String,
    val channel: String,
    val requiresLanguageUnderstanding: Boolean,
    val locale: String,
    val additionalRequestAttributes: Map<String, Any>? = null,
    val additionalSessionAttributes: Map<String, Any>? = null,
    val additionalSessionFlags: List<String>? = null,
)
