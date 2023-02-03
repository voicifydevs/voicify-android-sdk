package com.voicify.voicify_assistant_sdk.models

data class PlatformConfigurationsModel(
    val javaScript: CustomAssistantConfigurationResponse? = null,
    val kotlin: CustomAssistantConfigurationResponse? = null,
    val swift: CustomAssistantConfigurationResponse? = null
)
