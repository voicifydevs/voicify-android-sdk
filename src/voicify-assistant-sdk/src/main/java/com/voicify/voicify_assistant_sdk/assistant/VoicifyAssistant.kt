package com.voicify.voicify_assistant_sdk.assistant

import com.voicify.voicify_assistant_sdk.models.*
import okhttp3.*
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.UUID

@Suppress("unused")
class VoicifyAssistant(
    var speechToTextProvider: VoicifySpeechToTextProvider?,
    var textToSpeechProvider: VoicifyTextToSpeechProvider?,
    var settings: VoicifyAssistantSettings
) {
    private val client: OkHttpClient = OkHttpClient()
    var sessionId: String? = null
    var userId: String? = null
    var accessToken: String? = null
    var sessionAttributes: Map<String, Any>? = emptyMap()
    var userAttributes: Map<String, Any>? = emptyMap()
    var errorHandlers: Array<(e: String) -> Unit>? = emptyArray()
    var effectHandlers: Array<EffectModel>? = emptyArray()
    var requestStartedHandlers: Array<(req: CustomAssistantRequest) -> Unit>? = emptyArray()
    var responseHandlers: Array<(res: CustomAssistantResponse) -> Unit>? = emptyArray()
    var endSessionHandlers: Array<(res: CustomAssistantResponse) -> Unit>? = emptyArray()
    var audioHandlers: Array<(media: MediaItemModel) -> Unit>? = emptyArray()
    var videoHandlers: Array<(media: MediaItemModel) -> Unit>? = emptyArray()
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
        if(settings.initializeWithWelcomeMessage)
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

    fun onRequestStarted(callback: (request: CustomAssistantRequest) -> Unit)
    {
        this.requestStartedHandlers?.plus(callback)
    }

    fun onResponseReceived(callback: (response: CustomAssistantResponse) -> Unit)
    {
        this.responseHandlers?.plus(callback)
    }

    fun onSessionEnded(callback: (response: CustomAssistantResponse) -> Unit)
    {
        this.endSessionHandlers?.plus(callback)
    }

    fun onPlayVideo(callback: (mediaItem: MediaItemModel) -> Unit)
    {
        this.videoHandlers?.plus(callback)
    }

    fun onPlayAudio(callback: (mediaItem: MediaItemModel) -> Unit)
    {
        this.audioHandlers?.plus(callback)
    }

    fun clearHandlers()
    {
        this.audioHandlers = emptyArray()
        this.videoHandlers = emptyArray()
        this.endSessionHandlers = emptyArray()
        this.responseHandlers = emptyArray()
        this.requestStartedHandlers = emptyArray()
        this.effectHandlers = emptyArray()
        this.errorHandlers = emptyArray()
    }

    fun makeRequest (request: CustomAssistantRequest, inputType: String): CustomAssistantResponse?
    {
            var customAssistantResponse: CustomAssistantResponse? = null
            textToSpeechProvider?.stop?.invoke()
            requestStartedHandlers?.forEach {handle ->  handle.invoke(request)}
            val gson = Gson()
            val assistantRequestJsonString = gson.toJson(request)
            val assistantBody = assistantRequestJsonString.toRequestBody("application/json;charset=utf-8".toMediaTypeOrNull())
            val assistantRequest = Request.Builder()
                .url("${settings.serverRootUrl}/api/customAssistant/handlerequest?applicationId=${settings.appId}&applicationSecret=${settings.appKey}")
                .method("POST", assistantBody)
                .build()
            client.newCall(assistantRequest).enqueue(object : Callback{
                override fun onFailure(call: Call, e: IOException) {

                }
                override fun onResponse(call: Call, response: Response) {
                    val assistantResult = response.body?.string()
                    val assistantResponse: CustomAssistantResponse =
                        gson.fromJson(assistantResult, CustomAssistantResponse::class.java)
                    textToSpeechProvider?.clearHandlers?.invoke()
                    textToSpeechProvider?.addFinishListener?.invoke {
                        if ((settings.autoRunConversation && settings.useVoiceInput
                                    && inputType == "Speech" && settings.useOutputSpeech &&
                                    speechToTextProvider != null) && !assistantResponse.endSession
                        ) {
                            speechToTextProvider?.startListening?.invoke()
                        }
                    }
                    if(textToSpeechProvider!= null && settings.useOutputSpeech)
                    {
                        if(assistantResponse.ssml != null)
                        {
                            textToSpeechProvider?.speakSsml?.invoke(assistantResponse.ssml)
                        }
                        else if(assistantResponse.outputSpeech != null)
                        {
                            textToSpeechProvider?.speakSsml?.invoke("<speak>${assistantResponse.outputSpeech}</speak>")
                        }
                    }

                    val sessionDataRequest = Request.Builder()
                        .url("${settings.serverRootUrl}/api/UserProfile/${userId}?applicationId=$settings.appId}&applicationSecret=${settings.appKey}")
                        .addHeader("Content-Type","application/json")
                        .get()
                        .build()
                    client.newCall(sessionDataRequest).enqueue(object : Callback{
                        override fun onFailure(call: Call, e: IOException) {

                        }
                        @Suppress("unchecked_cast")
                        override fun onResponse(call: Call, response: Response) {
                            val sessionDataResult = response.body?.string()
                            val sessionDataResponse: VoicifySessionData =
                                gson.fromJson(sessionDataResult, VoicifySessionData::class.java)
                            currentSessionInfo = sessionDataResponse
                            val effects: Array<VoicifySessionEffect> = sessionDataResponse.sessionAttributes?.get("effects") as Array<VoicifySessionEffect>
                            effects.filter {e -> e.requestId == request.requestId}.forEach { effect ->
                                effectHandlers?.filter { e -> e.effect == effect.effectName }?.forEach { handle -> handle.callback(effect.data) }
                            }
                        }
                    })

                    val userDataRequest = Request.Builder()
                        .url("${settings.serverRootUrl}/api/UserProfile/${userId}?applicationId=${settings.appId}&applicationSecret=${settings.appKey}")
                        .addHeader("Content-Type","application/json")
                        .get()
                        .build()
                    client.newCall(userDataRequest).enqueue(object : Callback{
                        override fun onFailure(call: Call, e: IOException) {

                        }
                        override fun onResponse(call: Call, response: Response) {
                            val userDataResult = response.body?.string()
                            val userDataResponse: VoicifyUserData =
                                gson.fromJson(userDataResult, VoicifyUserData::class.java)
                            currentUserInfo = userDataResponse
                        }
                    })
                    responseHandlers?.forEach { handle -> handle(assistantResponse) }
                    if(assistantResponse.audioFile != null)
                    {
                        audioHandlers?.forEach { handle -> handle(assistantResponse.audioFile) }
                    }
                    if(assistantResponse.videoFile != null)
                    {
                        videoHandlers?.forEach { handle -> handle(assistantResponse.videoFile) }
                    }
                    if(assistantResponse.endSession)
                    {
                        endSessionHandlers?.forEach { handle -> handle(assistantResponse) }
                    }

                    if(settings.autoRunConversation && settings.useVoiceInput && !assistantResponse.endSession && inputType == "Speech" && (textToSpeechProvider == null || !settings.useOutputSpeech))
                    {
                        speechToTextProvider?.startListening?.invoke()
                    }
                    customAssistantResponse = assistantResponse
                    return
                }
            })
        return customAssistantResponse
    }

    fun makeTextRequest(text: String, requestAttributes: Map<String, Any>?, inputType: String): CustomAssistantResponse?
    {
        val request = generateTextRequest(text, requestAttributes)
        return makeRequest(request, inputType)
    }

    fun generateTextRequest(text: String, requestAttributes: Map<String, Any>?): CustomAssistantRequest
    {
        return CustomAssistantRequest(
            requestId = UUID.randomUUID().toString(),
            user = this.generateUser(),
            device = this.generateDevice(),
            context = CustomAssistantRequestContext(
                channel = this.settings.channel,
                locale = this.settings.locale,
                sessionId = this.sessionId as String,
                requestType = "IntentRequest",
                originalInput = text,
                requiresLanguageUnderstanding = true,
                additionalSessionAttributes = this.sessionAttributes,
                additionalRequestAttributes = requestAttributes
            )
        )
    }

    fun makeWelcomeMessage(requestAttributes: Map<String, Any>?): CustomAssistantResponse?
    {
        val request = generateWelcomeRequest(requestAttributes)
        return makeRequest(request, "")
    }

    fun generateWelcomeRequest (requestAttributes: Map<String, Any>?): CustomAssistantRequest {
        return CustomAssistantRequest(
            requestId = UUID.randomUUID().toString(),
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
        return CustomAssistantUser(
            id = this.userId as String,
            name = this.userId,
            additionalUserAttributes = this.userAttributes,
            accessToken = this.accessToken
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

    fun addSessionAttribute(key: String, value: Any)
    {
        this.sessionAttributes?.plus(Pair(key, value))
    }

    fun addUserAttributes(key: String, value: Any)
    {
        this.userAttributes?.plus(Pair(key,value))
    }

    fun addAccessToken(value: String)
    {
        this.accessToken = value
    }
}
