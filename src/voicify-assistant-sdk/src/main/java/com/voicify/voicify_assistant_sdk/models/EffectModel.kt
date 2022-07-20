package com.voicify.voicify_assistant_sdk.models

data class EffectModel(
    val effect: String,
    val callback: (data: Any) -> Unit
)