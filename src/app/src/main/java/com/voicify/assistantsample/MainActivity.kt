package com.voicify.assistantsample

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
                    HeaderProps(
                        fontFamily = "cursive"
                    ),
                    BodyProps(
                        hintsFontFamily = "cursive",
                        messageReceivedFontFamily = "cursive",
                        messageSentFontFamily = "cursive"
                    ),
                    ToolBarProps(
                        partialSpeechResultFontFamily = "cursive",
                        assistantStateFontFamily = "cursive",
                        helpTextFontFamily = "cursive",
                        speakFontFamily = "cursive",
                        typeFontFamily = "cursive",
                        textboxFontFamily = "cursive"
                    ),
                    AssistantSettingsProps(
                        appId = "99a803b7-5b37-426c-a02e-63c8215c71eb",
                        appKey = "MTAzM2RjNDEtMzkyMC00NWNhLThhOTYtMjljMDc3NWM5NmE3",
                        serverRootUrl = "https://assistant.voicify.com",
                        locale = "en-US",
                        channel = "My App",
                        textToSpeechVoice = "male|ar-XA-Standard-C",
                        device = "My Device",
                        autoRunConversation = true,
                        initializeWithWelcomeMessage = true,
                        textToSpeechProvider = "Google",
                        useVoiceInput = false,
                        useOutputSpeech = true,
                        initializeWithText = false,
                        useDraftContent = true,
                        noTracking = false,
                        effects = arrayOf("Play", "Dismiss", "Navigate"),
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