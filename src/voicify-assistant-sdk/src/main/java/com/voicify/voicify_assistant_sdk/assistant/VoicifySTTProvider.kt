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
//requireContext()
//requireActivity()
class VoicifySTTProvider (private val context: Context, private val activity: Activity) : VoicifySpeechToTextProvider{
    private var speechStartHandlers: Array<() -> Unit>? = emptyArray()
    private var speechPartialHandlers: Array<(partialResult: String?) -> Unit>? = emptyArray()
    private var speechEndHandlers: Array<() -> Unit>? = emptyArray()
    private var speechResultsHandlers: Array<(fullResult: String?) -> Unit>? = emptyArray()
    private var speechErrorHandlers: Array<(error: String) -> Unit>? = emptyArray()
    //private var speechRecognizedHandlers: Array<() -> Unit>? = emptyArray()
    private var speechVolumeHandlers: Array<(volume: Float) -> Unit>? = emptyArray()
    private var speechRecognizer: SpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
    private val speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

    override fun initialize (locale: String) {
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
        {
            checkPermission();
        }

        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d("JAMES", "Ready")
            }

            override fun onBeginningOfSpeech() {
                Log.d("JAMES", "Speech Began")
                speechStartHandlers?.forEach { handle -> handle() }
            }

            override fun onRmsChanged(rmsdB: Float) {
                Log.d("JAMES", rmsdB.toString())
                speechVolumeHandlers?.forEach { handle -> handle(rmsdB) }
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                Log.d("JAMES", "The buffer was received")
            }

            override fun onEndOfSpeech() {
                Log.d("JAMES", "Speech ended")
                speechEndHandlers?.forEach { handle -> handle() }
            }

            override fun onError(error: Int) {
                //list of errors found here: https://developer.android.com/reference/android/speech/SpeechRecognizer
                //most important code is 7 ...means no match
                Log.d("JAMES", "There was an error")
                speechErrorHandlers?.forEach { handle -> handle(error.toString()) }
            }

            override fun onResults(results: Bundle?) {
                Log.d("JAMES", "The results are in")
                speechResultsHandlers?.forEach { handle -> handle(results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.get(0).toString()) }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                speechPartialHandlers?.forEach {handle -> handle(partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.get(0)).toString() }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                Log.d("JAMES", "An event happend")
            }

        })
    }

    override fun startListening(){
        speechRecognizer.startListening(speechRecognizerIntent);
    }

    override fun stopListening() {
        speechRecognizer.stopListening()
    }

    fun cancelListening() {
        speechRecognizer.cancel();
    }

    fun destoryInstance(){
        speechRecognizer.destroy()
    }

    fun addStartListener (callback: () -> Unit) {
        speechStartHandlers = speechStartHandlers?.plus(callback)
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