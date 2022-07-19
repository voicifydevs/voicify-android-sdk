package com.voicify.voicify_assistant_sdk.models

data class TTSRequest(
    val applicationId: String,
    val applicationSecret: String,
    val ssmlRequest: SsmlRequest
)
