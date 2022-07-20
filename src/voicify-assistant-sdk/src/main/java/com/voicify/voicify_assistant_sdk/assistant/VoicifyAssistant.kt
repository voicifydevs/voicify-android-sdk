package com.voicify.voicify_assistant_sdk.assistant

import com.voicify.voicify_assistant_sdk.models.*
import java.util.UUID

class VoicifyAssistant(
    var speechToTextProvider: VoicifySpeechToTextProvider?,
    var textToSpeechProvider: VoicifyTextToSpeechProvider?,
    var settings: VoicifyAssistantSettings
) {
    var sessionId: String? = null
    var userId: String? = null
    var accessToken: String? = null
    var sessionAttributes: Map<String, Any>? = null
    var userAttributes: Map<String, Any>? = null
    var errorHandlers: Array<(e: String) -> Unit>? = null
    var effectHandlers: Array<EffectModel>? = null
    var requestStartedHandlers: Array<(req: CustomAssistantRequest) -> Unit>? = null
    var responseHandlers: Array<(res: CustomAssistantResponse) -> Unit>? = null
    var endSessionHandlers: Array<(res: CustomAssistantResponse) -> Unit>? = null
    var audioHandlers: Array<(media: MediaItemModel) -> Unit>? = null
    var videoHandlers: Array<(media: MediaItemModel) -> Unit>? = null
    var currentSessionInfo: VoicifySessionData? = null
    var currentUserInfo: VoicifyUserData? = null

    fun initializeAndStart() {
        textToSpeechProvider?.initialize?.invoke(settings.locale)
        speechToTextProvider?.initialize?.invoke(settings.locale)
    }

    fun startNewSession(sessionId: String?, userId: String?, sessionAttributes: Map<String, Any>?, userAttributes: Map<String, Any>?) {
        this.sessionId = sessionId ?: UUID.randomUUID().toString()
        this.userId = userId ?: UUID.randomUUID().toString()
        this.sessionAttributes = sessionAttributes
        this.userAttributes = userAttributes
        this.currentSessionInfo = null
        this.currentSessionInfo = null
        if(settings?.initializeWithWelcomeMessage)
        {
            makeWelcomeMessage(null)
        }
    }

    fun onEffect(effectName: String, callback: (data: Any) -> Unit){
        this.effectHandlers?.plus(EffectModel(effectName, callback))
    }

    fun onError(callback: (error: String) -> Unit)
    {
        this.errorHandlers?.plus(callback)
    }

    fun makeWelcomeMessage(requestAttributes: Map<String, Any>?)
    {
        val request = generateWelcomeRequest(requestAttributes)
    }

    fun generateWelcomeRequest (requestAttributes: Map<String, Any>?): CustomAssistantRequest {
        return CustomAssistantRequest(requestId = UUID.randomUUID().toString(),
            user = this.generateUser(),
            device = this.generateDevice(),
            context = CustomAssistantRequestContext(
                channel = this.settings.channel,
                locale = this.settings.locale,
                sessionId = this.sessionId as String,
                requestType = "IntentRequest",
                requestName = "VoicifyWelcome",
                originalInput = "[Automated]",
                requiresLanguageUnderstanding = false,
                additionalSessionAttributes = this.sessionAttributes,
                additionalRequestAttributes = requestAttributes
            )
        )
    }

    fun generateUser(): CustomAssistantUser{
        return CustomAssistantUser(id = this.userId as String,
            name = this.userId,
            additionalUserAttributes = this.userAttributes,
            accessToken = this?.accessToken
        )
    }

    fun generateDevice(): CustomAssistantDevice{
        return CustomAssistantDevice(
            id = this.settings.device,
            name = this.settings.device,
            supportsDisplayText = true,
            supportsTextInput = true,
            supportsSsml = this.settings.useOutputSpeech,
            supportsVoiceInput = this.settings.useVoiceInput
        )
    }
}


/*
val textToSpeechProvider: VoicifyTextToSpeechProvider,
val speechToTextProvider: VoicifySpeechToTextProvider,
val settings: VoicifyAssistantSettings,
val sessionId: String? = null,
val userId: String? = null,
val accessToken: String? = null,
val sessionAttributes: Map<String, Any>? = null,
val useAttributes: Map<String, Any>? = null,
val errorHandlers: Array<() -> Unit>,
val effectHandlers: Array<EffectModel>,
val requestStartedHandlers: Array<(req: CustomAssistantRequest) -> Unit>,
val responseHandlers: Array<(res: CustomAssistantResponse) -> Unit>,
val endSessionHandlers: Array<(res: CustomAssistantResponse) -> Unit>,
val audioHandlers: Array<(media: MediaItemModel) -> Unit>,
val videoHandlers: Array<(media: MediaItemModel) -> Unit>,
val currentSessionInfo: VoicifySessionData? = null,
val currentUserInfo: VoicifyUserData? = null
*/
