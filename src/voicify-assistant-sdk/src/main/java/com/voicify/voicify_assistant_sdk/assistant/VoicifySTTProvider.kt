package com.voicify.voicify_assistant_sdk.assistant

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.voicify.voicify_assistant_sdk.models.CustomAssistantRequest
import com.voicify.voicify_assistant_sdk.models.CustomAssistantResponse
import java.util.*

class VoicifySTTProvider (private val context: Context, private val activity: Activity) : VoicifySpeechToTextProvider{
    private var speechStartHandlers: Array<() -> Unit>? = emptyArray()
    private var speechReadyHandlers: Array<() -> Unit?> = emptyArray()
    private var speechPartialHandlers: Array<(partialResult: String?) -> Unit>? = emptyArray()
    private var speechEndHandlers: Array<() -> Unit>? = emptyArray()
    private var speechResultsHandlers: Array<(fullResult: String?) -> Unit>? = emptyArray()
    private var speechErrorHandlers: Array<(error: String) -> Unit>? = emptyArray()
    private var speechVolumeHandlers: Array<(volume: Float) -> Unit>? = emptyArray()
    private var locale: String = ""
    private var speechRecognizer: SpeechRecognizer? = SpeechRecognizer.createSpeechRecognizer(context);
    private val speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
    var cancel: Boolean = false

    override fun initialize (locale: String) {
        this.locale = locale
    }

    override fun startListening(){
        if(!cancel) {
            if (speechRecognizer != null)  //have to do this because if the speech recognizer ever gets destroyed we need to recreate each time to account for that
            {
                speechRecognizer?.destroy()
            }
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                checkPermission();
            }
            speechRecognizerIntent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            );
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale);
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    speechReadyHandlers?.forEach { handle -> handle() }
                }

                override fun onBeginningOfSpeech() {
                    speechStartHandlers?.forEach { handle -> handle() }
                }

                override fun onRmsChanged(rmsdB: Float) {

                    //bug on android where volume reports as negative
                    speechVolumeHandlers?.forEach { handle -> handle(if (rmsdB < 0) .1f else rmsdB) }

                }

                override fun onBufferReceived(buffer: ByteArray?) {
                }

                override fun onEndOfSpeech() {
                    speechEndHandlers?.forEach { handle -> handle() }
                }

                override fun onError(error: Int) {
                    //list of errors found here: https://developer.android.com/reference/android/speech/SpeechRecognizer
                    //most important code is 7 ...means no match
                    speechErrorHandlers?.forEach { handle -> handle(error.toString()) }
                }

                override fun onResults(results: Bundle?) {
                    speechResultsHandlers?.forEach { handle ->
                        handle(
                            results?.getStringArrayList(
                                SpeechRecognizer.RESULTS_RECOGNITION
                            )?.get(0).toString()
                        )
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    speechPartialHandlers?.forEach { handle ->
                        handle(
                            partialResults?.getStringArrayList(
                                SpeechRecognizer.RESULTS_RECOGNITION
                            )?.get(0)
                        ).toString()
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {
                }

            })
            speechRecognizer?.startListening(speechRecognizerIntent);
        }
    }

    override fun stopListening() {
        speechRecognizer?.stopListening()
    }

    fun cancelListening() {
        speechRecognizer?.cancel();
    }

    fun destoryInstance(){
        speechRecognizer?.destroy()
    }

    fun addStartListener (callback: () -> Unit) {
        speechStartHandlers = speechStartHandlers?.plus(callback)
    }

    fun addSpeechReadyListener (callback: () -> Unit) {
        speechReadyHandlers = speechReadyHandlers?.plus(callback)
    }
    override  fun addPartialListener (callback: (partialResult: String?) -> Unit) {
        speechPartialHandlers = speechPartialHandlers?.plus(callback)
    }

    fun addEndListener (callback: () -> Unit) {
        speechEndHandlers = speechEndHandlers?.plus(callback)
    }

    override fun addFinalResultListener (callback: (fullResult: String?) -> Unit) {
        speechResultsHandlers = speechResultsHandlers?.plus(callback)
    }

    fun addVolumeListener (callback: (volume: Float) -> Unit) {
        speechVolumeHandlers = speechVolumeHandlers?.plus(callback)
    }

    override fun addErrorListener (callback: (error: String) -> Unit) {
        speechErrorHandlers = speechErrorHandlers?.plus(callback)
    }

    fun clearHandlers (){
        speechStartHandlers = emptyArray()
        speechPartialHandlers = emptyArray()
        speechEndHandlers = emptyArray()
        speechResultsHandlers = emptyArray()
        speechErrorHandlers = emptyArray()
        speechVolumeHandlers = emptyArray()
        speechReadyHandlers = emptyArray()
    }
    private fun checkPermission(){
         var permissions: Array<String> = emptyArray()
        permissions = permissions.plus(Manifest.permission.RECORD_AUDIO)
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            ActivityCompat.requestPermissions(activity, permissions , 1 )
        }
    }
}