package com.voicify.assistantsample

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.voicify.assistantsample.databinding.ActivityMainBinding
import com.voicify.voicify_assistant_sdk.assistantDrawerUITypes.ToolBarProps
import com.voicify.voicify_assistant_sdk.assistant.AssistantDrawerUI
import com.voicify.voicify_assistant_sdk.assistantDrawerUITypes.AssistantSettingsProps
import com.voicify.voicify_assistant_sdk.assistantDrawerUITypes.BodyProps
import com.voicify.voicify_assistant_sdk.assistantDrawerUITypes.HeaderProps

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.assistantMic.setOnClickListener {
            if (savedInstanceState == null) {
                val voiceAssistant = AssistantDrawerUI.newInstance(
                    AssistantSettingsProps(
                        serverRootUrl = "https://dev-assistant.voicify.com",
                        appId = "bc9fa6bf-6cea-4fad-af12-d388b64dbdb9",
                        appKey = "ZjcyNmNkNjEtNmY5My00NTg3LWI5ZmQtMjJkNzE3NGMwYTI4",
                        locale = "en-US",
                        channel = "My App",
                        device = "My device",
                        textToSpeechVoice = "",
                        autoRunConversation = false,
                        initializeWithWelcomeMessage = false,
                        textToSpeechProvider = "Google",
                        useVoiceInput = true,
                        useOutputSpeech = true,
                        useDraftContent = false,
                        noTracking = true,
                        initializeWithText = false,
                    ),
                    HeaderProps(
                    ),
                    BodyProps(
                    ),
                    ToolBarProps(
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
                    }
                }
                val sessionAttributes = mapOf("sessionData" to ExampleSessionData(id="conifguredId", user = "user"))
                voiceAssistant.addSessionAttributes(sessionAttributes)
                voiceAssistant.onEffect(onEffect)
                voiceAssistant.onAssistantDismiss(onAssistantDismiss)
                voiceAssistant.show(supportFragmentManager, "assistantDrawerUI")
            }
        }
    }
}