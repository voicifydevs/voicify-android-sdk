package com.voicify.voicify_assistant_sdk.assistant

import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.voicify.voicify_assistant_sdk.models.*
import okhttp3.*
import com.google.gson.Gson
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
    var effects: Array<VoicifySessionEffect>? = null
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
    var fireBeforeSpeechEffects: Array<VoicifySessionEffect> = emptyArray()
    var fireAfterSpeechEffects: Array<VoicifySessionEffect> = emptyArray()

    fun initializeAndStart() {
        textToSpeechProvider?.initialize(settings.locale)
        speechToTextProvider?.initialize(settings.locale)
    }

    fun startNewSession(sessionId: String? = null, userId: String? = null, sessionAttributes: Map<String, Any>? = null, userAttributes: Map<String, Any>? = null) {
        this.sessionId = sessionId ?: UUID.randomUUID().toString()
        this.userId = userId ?: UUID.randomUUID().toString()
        this.sessionAttributes = sessionAttributes
        this.userAttributes = userAttributes
        this.currentSessionInfo = null
        this.currentUserInfo = null
        if(settings.initializeWithWelcomeMessage)
        {
            makeWelcomeMessage(null)
        }
        addDefaultEffects()
    }

    fun onEffect(effectName: String, callback: (data: Any) -> Unit){
        effectHandlers = effectHandlers?.plus(EffectModel(effectName, callback))
    }

    fun onError(callback: (error: String) -> Unit)
    {
        errorHandlers = errorHandlers?.plus(callback)
    }

    fun onRequestStarted(callback: (request: CustomAssistantRequest) -> Unit)
    {
        requestStartedHandlers = requestStartedHandlers?.plus(callback)
    }

    fun onResponseReceived(callback: (response: CustomAssistantResponse) -> Unit)
    {
        responseHandlers = responseHandlers?.plus(callback)
    }

    fun onSessionEnded(callback: (response: CustomAssistantResponse) -> Unit)
    {
        endSessionHandlers = endSessionHandlers?.plus(callback)
    }

    fun onPlayVideo(callback: (mediaItem: MediaItemModel) -> Unit)
    {
        videoHandlers = videoHandlers?.plus(callback)
    }

    fun onPlayAudio(callback: (mediaItem: MediaItemModel) -> Unit)
    {
        audioHandlers = audioHandlers?.plus(callback)
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
        this.addDefaultEffects()
    }

    fun makeRequest (request: CustomAssistantRequest, inputType: String)
    {
        textToSpeechProvider?.stop()
        requestStartedHandlers?.forEach {handle ->  handle.invoke(request)}
        val gson = Gson()
        val assistantRequestJsonString = gson.toJson(request)
        val assistantRequestBody = assistantRequestJsonString.toRequestBody("application/json;charset=utf-8".toMediaTypeOrNull())
        val useDraftContent = if (settings.useDraftContent) "&useDraftContent=true" else ""
        val assistantRequest = Request.Builder()
            .url("${settings.serverRootUrl}/api/customAssistant/handlerequest?applicationId=${settings.appId}&applicationSecret=${settings.appKey}${useDraftContent}&includeSessionAttributes=true")
            .method("POST", assistantRequestBody)
            .build()
        client.newCall(assistantRequest).enqueue(object : Callback{
            override fun onFailure(call: Call, e: IOException) {

            }
            override fun onResponse(call: Call, response: Response) {
                fireAfterSpeechEffects = emptyArray()
                fireBeforeSpeechEffects = emptyArray()
                effects = emptyArray()
                val assistantResult = response.body?.string()
                val assistantResponse: CustomAssistantResponse =
                    gson.fromJson(assistantResult, CustomAssistantResponse::class.java)
                if(assistantResponse.effects != null && assistantResponse.effects.isNotEmpty()){
                    effects = assistantResponse.effects
                }
                else{
                    val effectsString = gson.toJson(assistantResponse.sessionAttributes?.get("effects"))
                    if (effectsString.isNotEmpty())
                    {
                        effects =  gson.fromJson(effectsString, Array<VoicifySessionEffect>::class.java)
                    }
                }
                if(!effects.isNullOrEmpty())
                {
                    effects!!.forEach { effect ->
                        var effectData = deserializeEffectData(effect.data, DefaultEffectModel::class.java)
                        if(effectData.fireBeforeTextToSpeech == true)
                        {
                            fireBeforeSpeechEffects = fireBeforeSpeechEffects.plus(effect)
                        }
                        else{
                            fireAfterSpeechEffects = fireAfterSpeechEffects.plus(effect)
                        }
                    }
                }
                if(!fireBeforeSpeechEffects.isNullOrEmpty())
                {
                    runBlocking {
                        launch { fireEffects(fireBeforeSpeechEffects, request) }
                    }
                }

                textToSpeechProvider?.clearHandlers()
                if(textToSpeechProvider != null && settings.useOutputSpeech)
                {
                    textToSpeechProvider?.addFinishListener {
                        if(!fireAfterSpeechEffects.isNullOrEmpty()){
                            runBlocking {
                                launch { fireEffects(fireAfterSpeechEffects, request) }
                            }
                        }
                        else if ((settings.autoRunConversation && settings.useVoiceInput
                                    && inputType == "Speech" && settings.useOutputSpeech &&
                                    speechToTextProvider != null) && !assistantResponse.endSession
                        ) {
                            speechToTextProvider?.startListening()
                        }
                    }
                }
                else{
                    if(!fireAfterSpeechEffects.isNullOrEmpty()){
                        runBlocking {
                            launch {fireEffects(fireAfterSpeechEffects, request)}
                        }
                    }
                }

                if(textToSpeechProvider!= null && settings.useOutputSpeech)
                {
                    if(assistantResponse.ssml != null)
                    {
                        textToSpeechProvider?.speakSsml(assistantResponse.ssml)
                    }
                    else if(assistantResponse.outputSpeech != null)
                    {
                        textToSpeechProvider?.speakSsml("<speak>${assistantResponse.outputSpeech}</speak>")
                    }
                }

                currentSessionInfo = VoicifySessionData(sessionFlags = assistantResponse.sessionFlags, sessionAttributes = assistantResponse.sessionAttributes)

                val userDataRequest = Request.Builder()
                    .url("${settings.serverRootUrl}/api/UserProfile/${userId}?applicationId=${settings.appId}&applicationSecret=${settings.appKey}")
                    .addHeader("Content-Type","application/json")
                    .get()
                    .build()
                client.newCall(userDataRequest).enqueue(object : Callback{
                    override fun onFailure(call: Call, e: IOException) {

                    }
                    override fun onResponse(call: Call, response: Response) {
                        if(response.code == 200 || response.code == 204)
                        {
                            val userDataResult = response.body?.string()
                            if(!userDataResult.isNullOrEmpty())
                            {
                                val userDataResponse: VoicifyUserData =
                                    gson.fromJson(userDataResult, VoicifyUserData::class.java)
                                currentUserInfo = userDataResponse
                            }
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
                                speechToTextProvider?.startListening()
                            }
                        }
                    }
                })
            }
        })
    }

    fun makeTextRequest(text: String, requestAttributes: Map<String, Any>?, inputType: String)
    {
        val request = generateTextRequest(text, requestAttributes)
        makeRequest(request, inputType)
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
                noTracking = this.settings.noTracking,
                sessionId = this.sessionId as String,
                requestType = "IntentRequest",
                originalInput = text,
                requiresLanguageUnderstanding = true,
                additionalSessionAttributes = this.sessionAttributes,
                additionalRequestAttributes = requestAttributes
            )
        )
    }

    fun makeWelcomeMessage(requestAttributes: Map<String, Any>?)
    {
        val request = generateWelcomeRequest(requestAttributes)
        if(settings.autoRunConversation && !settings.initializeWithText)
        {
            makeRequest(request, "Speech")
        }
        else
        {
            makeRequest(request, "Text")
        }

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
                noTracking = this.settings.noTracking,
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
        this.sessionAttributes = this.sessionAttributes?.plus(Pair(key, value))
    }

    fun addUserAttributes(key: String, value: Any)
    {
        this.userAttributes = this.userAttributes?.plus(Pair(key,value))
    }

    fun addAccessToken(value: String)
    {
        this.accessToken = value
    }

    private fun addDefaultEffects() {
        this.effectHandlers = this.effectHandlers?.plus(EffectModel("closeAssistant", closeAssistantCallback))
        this.effectHandlers = this.effectHandlers?.plus(EffectModel("scrollTo", scrollToCallback))
        this.effectHandlers = this.effectHandlers?.plus(EffectModel("clickTap", clickTapCallback))
    }

    fun <T> deserializeEffectData(data: Any, type: Class<T>): T {
        val gson = Gson()
        val dataString = gson.toJson(data)
        val newData =  gson.fromJson(dataString, type)
        return newData as T
    }

    private fun fireEffects(effectsToFire: Array<VoicifySessionEffect>, request: CustomAssistantRequest)
    {
        Handler(Looper.getMainLooper()).post{
        if(!effectsToFire.isNullOrEmpty()) {
            if (!effectsToFire[0].name.isNullOrEmpty()) {
                effectsToFire.forEach { effect ->
                    effectHandlers?.filter { e -> e.effect == effect.name }?.forEach { handle ->
                        val effectData =
                            deserializeEffectData(effect.data, DefaultEffectModel::class.java)
                        if (!effectData.fireAfterMilliseconds.isNullOrEmpty()) {
                            var milliSeconds = effectData.fireAfterMilliseconds?.toLongOrNull()
                            val timer = object : CountDownTimer(milliSeconds ?: 0, 1000) {
                                override fun onTick(millisUntilFinished: Long) {

                                }

                                override fun onFinish() {
                                    handle.callback(effect.data)
                                }
                            }
                            timer.start()
                        } else {
                            handle.callback(effect.data)
                        }
                    }
                }
            } else if (!effects!![0].effectName.isNullOrEmpty()) {
                effectsToFire.filter { e -> e.requestId == request.requestId }.forEach { effect ->
                    effectHandlers?.filter { e -> e.effect == effect.name }?.forEach { handle ->
                        val effectData =
                            deserializeEffectData(effect.data, DefaultEffectModel::class.java)
                        if (!effectData.fireAfterMilliseconds.isNullOrEmpty()) {
                            var milliSeconds = effectData.fireAfterMilliseconds.toLongOrNull()
                            val timer = object : CountDownTimer(milliSeconds ?: 0, 1000) {
                                override fun onTick(millisUntilFinished: Long) {

                                }

                                override fun onFinish() {
                                    handle.callback(effect.data)
                                }
                            }
                            timer.start()
                        } else {
                            handle.callback(effect.data)
                        }
                    }
                }
            }
        }
        }
    }
}
