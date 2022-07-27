package com.voicify.voicify_assistant_sdk.assistant

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import androidx.lifecycle.Lifecycle
import com.google.gson.Gson
import com.voicify.voicify_assistant_sdk.models.SsmlRequest
import com.voicify.voicify_assistant_sdk.models.TTSData
import com.voicify.voicify_assistant_sdk.models.TTSRequest
import com.voicify.voicify_assistant_sdk.models.VoicifyUserData
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class VoicifyTTSProvider(val settings: VoicifyTextToSpeechSettings)  : VoicifyTextToSpeechProvider{
    private var speechEndHandlers: Array<() -> Unit>? = emptyArray()
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
        var ttsResponse: Array<TTSData>? = null
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
                Log.d("JAMES", "FAILED!")
            }
            override fun onResponse(call: Call, response: Response) {
                Log.d("JAMES", "5")
                val ttsResult = response.body?.string()
                ttsResponse = gson.fromJson(ttsResult, Array<TTSData>::class.java)
//                ttsResponse?.forEach { response ->
//                    GlobalScope.launch {
//                        var ssmlUri = response.url
//                        mediaPlayer.reset()
//                        mediaPlayer.setDataSource(ssmlUri)
//                        mediaPlayer.prepare()
//                        mediaPlayer.setOnCompletionListener {
//                            speechEndHandlers?.forEach { handle -> handle() }
//                        }
//                        mediaPlayer.start()
//                    }
//                }
                var ssmlUri = ttsResponse?.get(0)?.url
                        mediaPlayer.reset()
                        mediaPlayer.setDataSource(ssmlUri)
                        mediaPlayer.prepare()
                        mediaPlayer.setOnCompletionListener {
                            speechEndHandlers?.forEach { handle -> handle() }
                        }
                        mediaPlayer.start()
            }
        })
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

//    private fun ssmlToSpeech (ssml: String): Array<TTSData>?
//    {
//        Log.d("JAMES", "SsmlToSpeech")
//        Log.d("JAMES", ssml)
//        var ttsResponse: Array<TTSData>? = null
//        val ttsRequestModel = generateTTSRequest(ssml)
//        val gson = Gson()
//        Log.d("JAMES", "0")
//        val ttsRequestJson = gson.toJson(ttsRequestModel)
//        Log.d("JAMES", "1")
//        val ttsRequestBody = ttsRequestJson.toRequestBody("application/json;charset=utf-8".toMediaTypeOrNull())
//        Log.d("JAMES", "2")
//        val ttsRequest = Request.Builder()
//            .url("${settings.serverRootUrl}/api/Ssml/toSpeech/${settings.provider}")
//            .method("POST", ttsRequestBody)
//            .build()
//        Log.d("JAMES", ttsRequestJson)
//        Log.d("JAMES", "4")
//
//        client.newCall(ttsRequest).enqueue(object : Callback {
//            override fun onFailure(call: Call, e: IOException) {
//                Log.d("JAMES", "FAILED!")
//            }
//            override fun onResponse(call: Call, response: Response) {
//                Log.d("JAMES", "5")
//                val ttsResult = response.body?.string()
////                Log.d("JAMES", ttsResult.toString())
//                ttsResponse = gson.fromJson(ttsResult, Array<TTSData>::class.java)
////                Log.d("JAMES", ttsResponse?.get(0)?.url.toString())
////                Log.d("JAMES", ssml)
//            }
//        })
//    }

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