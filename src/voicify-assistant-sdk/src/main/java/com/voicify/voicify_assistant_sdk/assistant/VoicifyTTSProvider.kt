package com.voicify.voicify_assistant_sdk.assistant

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaPlayer.OnPreparedListener
import android.util.Log
import com.google.gson.Gson
import com.voicify.voicify_assistant_sdk.models.SsmlRequest
import com.voicify.voicify_assistant_sdk.models.TTSData
import com.voicify.voicify_assistant_sdk.models.TTSRequest
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class VoicifyTTSProvider(val settings: VoicifyTextToSpeechSettings)  : VoicifyTextToSpeechProvider{
    private var speechEndHandlers: Array<() -> Unit>? = emptyArray()
    private var ttsResponse: Array<TTSData>? = null
    private var currentPlayingIndex = 0
    private var mediaPlayer:  MediaPlayer = MediaPlayer().apply { setAudioAttributes(
        AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .build()
    ) }
    private var locale: String = ""
    private val client: OkHttpClient = OkHttpClient()

    override fun initialize(locale: String) {
        this.locale = locale
    }

    override fun speakSsml(ssml: String) {

        val ttsRequestModel = generateTTSRequest(ssml)
        val gson = Gson()
        val ttsRequestJson = gson.toJson(ttsRequestModel)
        val ttsRequestBody = ttsRequestJson.toRequestBody("application/json;charset=utf-8".toMediaTypeOrNull())
        val ttsRequest = Request.Builder()
            .url("${settings.serverRootUrl}/api/Ssml/toSpeech/${settings.provider}")
            .method("POST", ttsRequestBody)
            .build()

        client.newCall(ttsRequest).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
            }
            override fun onResponse(call: Call, response: Response) {
                val ttsResult = response.body?.string()

                ttsResponse = gson.fromJson(ttsResult, Array<TTSData>::class.java)
                if(ttsResponse?.size == 1)
                {
                    var ssmlUri = ttsResponse?.get(0)?.url
                    mediaPlayer.reset()
                    mediaPlayer.setDataSource(ssmlUri)
                    mediaPlayer.prepareAsync()
                    mediaPlayer.setOnCompletionListener {
                        speechEndHandlers?.forEach { handle -> handle() }
                    }
                    mediaPlayer.setOnPreparedListener { mp ->
                        mp.start()
                    }
                }
                else{
                    var ssmlUri = ttsResponse?.get(0)?.url
                    mediaPlayer.reset()
                    mediaPlayer.setDataSource(ssmlUri)
                    mediaPlayer.prepareAsync()
                    mediaPlayer.setOnPreparedListener { mp ->
                        mp.start()
                    }
                    mediaPlayer.setOnCompletionListener {
                        if(currentPlayingIndex < ttsResponse?.size as Int - 1)
                        {
                            currentPlayingIndex++
                            playNext()
                        }
                        else
                        {
                            speechEndHandlers?.forEach { handle -> handle() }
                        }
                    }
                }
            }
        })
    }

    private fun playNext () {
        var ssmlUri = ttsResponse?.get(currentPlayingIndex)?.url
        mediaPlayer.reset()
        mediaPlayer.setDataSource(ssmlUri)
        mediaPlayer.prepareAsync()
        mediaPlayer.setOnPreparedListener { mp ->
            mp.start()
        }
    }

    override fun addFinishListener(callback: () -> Unit) {
        speechEndHandlers = speechEndHandlers?.plus(callback)
    }

    override fun clearHandlers() {
        speechEndHandlers = emptyArray()
    }

    override fun stop() {
        mediaPlayer.stop()
    }

    private fun generateTTSRequest(ssml: String): TTSRequest {
        return TTSRequest(
            applicationId = settings.appId,
            applicationSecret = settings.appKey,
            ssmlRequest = SsmlRequest(
                ssml = ssml,
                locale = locale,
                voice = settings.voice
            )
        )
    }
}