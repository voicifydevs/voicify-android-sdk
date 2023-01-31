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

        binding.assistantMic.setOnClickListener {
            if (savedInstanceState == null) {
                binding.assistantMic.isClickable = false
                val voiceAssistant = AssistantDrawerUI.newInstance(
                    AssistantSettingsProps(
                        configurationId = "a0542ef6-f4cd-40b7-8653-ca4a2b949fe3",
                        serverRootUrl ="https://dev-assistant.voicify.com",
                        appId = "bc9fa6bf-6cea-4fad-af12-d388b64dbdb9",
                        appKey = "ZjcyNmNkNjEtNmY5My00NTg3LWI5ZmQtMjJkNzE3NGMwYTI4",
                        initializeWithWelcomeMessage = true
                    )
                )
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
                voiceAssistant.onAssistantError(onAssistantError)
                voiceAssistant.show(supportFragmentManager, "assistantDrawerUI")
            }
        }
    }
}