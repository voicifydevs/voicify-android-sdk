package com.voicify.voicify_assistant_sdk.models

data class CustomAssistantResponse(
    val responseId: String,
    val ssml: String? = null,
    val outputSpeech: String? = null,
    val displayText: String? = null,
    val displayTitle: String? = null,
    val responseTemplate: String? = null,
    val foregroundImage: MediaItemModel? = null,
    val backgroundImage: MediaItemModel? = null,
    val audioFile: MediaItemModel? = null,
    val videoFile: MediaItemModel? = null,
    val sessionAttributes: Map<String, Any>? = null,
    val hints: Array<String>? = null,
    val listItems: Array<CustomAssistantListItem>? = null,
    val endSession: Boolean
)
