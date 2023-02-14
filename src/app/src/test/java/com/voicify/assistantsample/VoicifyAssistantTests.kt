package com.voicify.assistantsample

import com.voicify.voicify_assistant_sdk.assistant.VoicifyAssistant
import com.voicify.voicify_assistant_sdk.assistant.VoicifyAssistantSettings
import com.voicify.voicify_assistant_sdk.models.CustomAssistantRequest
import com.voicify.voicify_assistant_sdk.models.CustomAssistantResponse
import com.voicify.voicify_assistant_sdk.models.MediaItemModel
import org.junit.Test

import org.junit.Assert.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class VoicifyAssistantTests {
    @Test
    fun testHandlers() {
        var lock1 =  CountDownLatch(1);
        val assistant = VoicifyAssistant(
null,
null,
            VoicifyAssistantSettings(
                appId = "99a803b7-5b37-426c-a02e-63c8215c71eb",
                appKey = "MTAzM2RjNDEtMzkyMC00NWNhLThhOTYtMjljMDc3NWM5NmE3",
                serverRootUrl = "https://assistant.voicify.com",
                locale = "en-US",
                channel = "android unit test",
                device = "android",
                initializeWithWelcomeMessage = false,
                autoRunConversation = false,
                initializeWithText = false,
                useVoiceInput = false,
                useOutputSpeech = false,
                useDraftContent = false,
                noTracking = false
            )
        )
        var tempRequest: CustomAssistantRequest? = null
        var tempResponse: CustomAssistantResponse? = null
        var tempEndSessionResponse: CustomAssistantResponse? = null
        var tempAudioResponse: MediaItemModel? = null
        var tempVideoResponse: MediaItemModel? = null

        assistant.startNewSession()

        assistant.onRequestStarted{request ->
            tempRequest = request
        }
        //ResponseReceivedHandlers
        assistant.onResponseReceived{response ->
            tempResponse = response
        }

        //AudioHandlers
        assistant.onPlayAudio{mediaItem ->
            System.out.println("audio fired")
            tempAudioResponse = mediaItem
        }
        //VideoHandlers
        assistant.onPlayVideo{videoItem ->
            System.out.println("video fired")
            tempVideoResponse = videoItem
        }
        //EndSessionHandlers
        assistant.onSessionEnded{response ->
            tempEndSessionResponse = response
            lock1.countDown();
        }

        assistant.makeTextRequest(text= "Test Response", requestAttributes = null, inputType= "text")
        lock1.await(10000, TimeUnit.MILLISECONDS)
        //assert request start handler
        assertEquals("android unit test",tempRequest?.context?.channel)
        assertEquals("android",tempRequest?.device?.name)
        assertEquals("en-US",tempRequest?.context?.locale)

        //assert response received handler
        assertEquals("here is the response",tempResponse?.outputSpeech)
        assertEquals("play",tempResponse?.effects?.get(0)?.name)
        assertEquals(true, tempResponse?.endSession)

        //assert end session handler
        assertEquals("here is the response", tempEndSessionResponse?.displayText)

        //assert audio handler
        assertEquals("https://voicify-prod-files.s3.amazonaws.com/665730ca-6687-442c-863d-7db30f22c0e6/ba125dbd-b0d5-4d99-a3c1-40560f9b1b85/173-Portland-St.mp3", tempAudioResponse?.url)

//        assert video handler
        assertEquals("https://voicify-prod-files.s3.amazonaws.com/665730ca-6687-442c-863d-7db30f22c0e6/ef85725b-a01b-4a45-86da-2ac652a6409f/Screen-Recording-20221114-123949-P-QARC.mp4", tempVideoResponse?.url)
    }

    @Test
    fun testDefaultSessionAttributes(){
        var lock1 =  CountDownLatch(1);
        val assistant = VoicifyAssistant(
            null,
            null,
            VoicifyAssistantSettings(
                appId = "99a803b7-5b37-426c-a02e-63c8215c71eb",
                appKey = "MTAzM2RjNDEtMzkyMC00NWNhLThhOTYtMjljMDc3NWM5NmE3",
                serverRootUrl = "https://assistant.voicify.com",
                locale = "en-US",
                channel = "android unit test",
                device = "android",
                initializeWithWelcomeMessage = false,
                autoRunConversation = false,
                initializeWithText = false,
                useVoiceInput = false,
                useOutputSpeech = false,
                useDraftContent = false,
                noTracking = false
            )
        )
        var tempResponse: CustomAssistantResponse? = null

        assistant.startNewSession(sessionAttributes = mapOf("sessionData" to "the data"))
        assistant.onResponseReceived{response ->
            tempResponse = response
            lock1.countDown();
        }

        assistant.makeTextRequest(text= "Test Response", requestAttributes = null, inputType= "text")
        lock1.await(10000, TimeUnit.MILLISECONDS)

        assertEquals("we have session",tempResponse?.outputSpeech)
    }
}