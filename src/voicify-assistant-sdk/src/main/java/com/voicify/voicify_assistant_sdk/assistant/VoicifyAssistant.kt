package com.voicify.voicify_assistant_sdk.assistant

import com.voicify.voicify_assistant_sdk.models.*

class VoicifyAssistant (
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
) {
}