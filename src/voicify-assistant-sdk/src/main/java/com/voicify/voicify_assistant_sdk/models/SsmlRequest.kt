package com.voicify.voicify_assistant_sdk.models

data class SsmlRequest(
    val ssml: String,
    val locale: String,
    val voice: String,
)
