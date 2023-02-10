package com.voicify.assistantsample

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.voicify.assistantsample.databinding.ActivityMainBinding
import com.voicify.voicify_assistant_sdk.assistantDrawerUITypes.ToolbarProps
import com.voicify.voicify_assistant_sdk.assistant.AssistantDrawerUI
import com.voicify.voicify_assistant_sdk.assistantDrawerUITypes.AssistantSettingsProps
import com.voicify.voicify_assistant_sdk.assistantDrawerUITypes.BodyProps
import com.voicify.voicify_assistant_sdk.assistantDrawerUITypes.HeaderProps
import com.voicify.voicify_assistant_sdk.models.CustomAssistantRequest

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //config needed for UI tests
        val voiceAssistant = AssistantDrawerUI.newInstance(
            AssistantSettingsProps(
                serverRootUrl = "https://assistant.voicify.com",
                appId = "99a803b7-5b37-426c-a02e-63c8215c71eb",
                appKey = "MTAzM2RjNDEtMzkyMC00NWNhLThhOTYtMjljMDc3NWM5NmE3",
                effects = arrayOf("Play")
            )
        )

//        val voiceAssistant = AssistantDrawerUI.newInstance(
//            AssistantSettingsProps(
//                configurationId = "b8ee863c-6e8e-4aef-8e59-a10082430d50",
//                serverRootUrl = "https://dev-assistant.voicify.com",
//                appId = "c7681d20-b19e-407a-a475-320c681880e8",
//                appKey = "MzA4ZTQ5MWQtMzQzNy00N2Q0LTg5OWEtMzQzMGYwMTk5Y2Ex"
//            )
//        )

        binding.assistantMic.setOnClickListener {
            if (savedInstanceState == null) {
                binding.assistantMic.isClickable = false
                val onEffect: (String, Any) -> Unit = { effectName, data ->
                    if(effectName == "Dismiss")

                    {
                        Log.d("JAMES","CLOSING")
                        voiceAssistant.dismiss()
                    }
                    if(effectName == "Navigate")
                    {
                        Log.d("JAMES", "TIME TO NAVIGATE")
                        val effectData = voiceAssistant.deserializeEffectData(data, NavigateEffectData::class.java)
                        this.runOnUiThread {
                            voiceAssistant.dismiss()
                            binding.nowPlayingTextView.text = "Now playing ${effectData.page}"
                        }
                    }
                    if(effectName == "Play")
                    {
                        val effectData = voiceAssistant.deserializeEffectData(data, PlayEffectData::class.java)
                        this.runOnUiThread {
                            Log.d("JAMES", effectData.title.toString())
                            voiceAssistant.dismiss()
                            binding.nowPlayingTextView.text = "Now playing ${effectData.title}"
                        }
                    }
                }
                val onAssistantDismiss: () -> Unit = {
                    this.runOnUiThread{
                        binding.nowPlayingTextView.text = "Now playing"
                        binding.assistantMic.isClickable = true
                    }
                }

                val onAssistantError: (String, CustomAssistantRequest) -> Unit = {errorMessage, request ->
                    this.runOnUiThread {
                        voiceAssistant.dismiss()
                        val alertDialogBuilder = AlertDialog.Builder(this)
                        alertDialogBuilder.setTitle("ERROR")
                        alertDialogBuilder.setMessage("ASSISTANT UNAVAILABLE")
                        alertDialogBuilder.show()
                    }
                }
//                val sessionAttributes = mapOf("sessionData" to ExampleSessionData(id="conifguredId", user = "user"))
//              voiceAssistant.addSessionAttributes(sessionAttributes)
                voiceAssistant.onEffect(onEffect)
                voiceAssistant.onAssistantDismiss(onAssistantDismiss)
                //voiceAssistant.onAssistantError(onAssistantError)
                voiceAssistant.show(supportFragmentManager, "assistantDrawerUI")
            }
        }
    }
}