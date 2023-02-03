package com.voicify.voicify_assistant_sdk.models

data class CustomAssistantConfigurationResponse(
    val id: String? = null,
    val applicationId: String? = null,
    val applicationSecret: String? = null,
    val environmentId: String? = null,
    val textToSpeechProvider: String? = null,
    val locale: String? = null,
    val textToSpeechVoice : String? = null,
    val channel: String? = null,
    val device: String? = null,
    val name: String? = null,
    val autoRunConversation: Boolean? = null,
    val initializeWithWelcomeMessage: Boolean? = null,
    val useOutputSpeech: Boolean? = null,
    val useVoiceInput: Boolean? = null,
    val sessionTimeout: String? = null,
    val uiType: String? = null,
    val noTracking: Boolean? = null,
    val useDraftContent: Boolean? = null,
    val activeInput: String? = null,
    val avatarUrl: String? = null,
    val displayName: String? = null,
    val theme: String? = null,
    val font: String? = null,
    val primaryColor: String? = null,
    val platformConfigurationsModel: PlatformConfigurationsModel? = null,
    val styles: ConfigurationStyle? = null
)

