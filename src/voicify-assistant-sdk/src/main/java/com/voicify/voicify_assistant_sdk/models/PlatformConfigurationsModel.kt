package com.voicify.voicify_assistant_sdk.models

data class PlatformConfigurationsModel(
    val JavaScript: CustomAssistantConfigurationResponse? = null,
    val Kotlin: CustomAssistantConfigurationResponse? = null,
    val Swift: CustomAssistantConfigurationResponse? = null
)
