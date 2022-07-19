package com.voicify.voicify_assistant_sdk.models

data class CustomAssistantDevice(
    val id: String,
    val name: String,
    val supportsVideo: Boolean? = false,
    val supportsForegroundImage: Boolean? = false,
    val supportsBackgroundImage: Boolean? = false,
    val supportsAudio: Boolean? = false,
    val supportsSsml: Boolean? = false,
    val supportsDisplayText: Boolean? = false,
    val supportsVoiceInput: Boolean? = false,
    val supportsTextInput: Boolean? = false

)
