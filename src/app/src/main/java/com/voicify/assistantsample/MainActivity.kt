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
        val voiceAssistant = AssistantDrawerUI.newInstance(
            AssistantSettingsProps(
                serverRootUrl = "https://dev-assistant.voicify.com",
                appId = "52dfe3a1-b44e-4ff1-ac02-04f0a139cd51",
                appKey = "NmYxNjM3ZDAtYzdiOC00NGVjLWE1OGMtZGNmMjJlOWYxMDAx",
                locale = "en-US",
                channel = "My App",
                device = "My device",
                textToSpeechVoice = "",
                autoRunConversation = false,
                initializeWithWelcomeMessage = true,
                textToSpeechProvider = "Google",
                useVoiceInput = true,
                useOutputSpeech = true,
                useDraftContent = false,
                noTracking = true,
                initializeWithText = false,
                backgroundColor = "#202C36,#3E77A5"
            ),
            HeaderProps(
                backgroundColor = "#00000000",
                assistantName = "pink",
                assistantNameTextColor = "#FFFFFF",
                assistantImage = "https://voicify-dev-files.s3.amazonaws.com/52dfe3a1-b44e-4ff1-ac02-04f0a139cd51/d23550ef-0b21-4a2d-886a-19c831689e98/mary-headshot.png",
                fontFamily = "Helvetica",
                closeAssistantColor = "#FFFFFF"
            ),
            BodyProps(
                backgroundColor = "#00000000",
                messageSentTextColor = "#FFFFFF",
                messageSentBackgroundColor = "#69275f",
                messageReceivedTextColor = "#FFFFFF",
                messageReceivedBackgroundColor = null,
                messageSentFontFamily = "Helvetica",
                messageReceivedFontFamily = "Helvetica",
                hintsTextColor = "#FFFFFF",
                hintsBackgroundColor = "#00000000",
                hintsFontFamily = "Helvetica",
                assistantImage = "https://voicify-dev-files.s3.amazonaws.com/52dfe3a1-b44e-4ff1-ac02-04f0a139cd51/d23550ef-0b21-4a2d-886a-19c831689e98/mary-headshot.png"
            ),
            ToolbarProps(
                backgroundColor = "#00000000",
                speakActiveTitleColor = "#FFFFFF",
                speakInactiveTitleColor = "#FFFFFF",
                typeActiveTitleColor = "#FFFFFF",
                typeInactiveTitleColor = "#FFFFFF",
                partialSpeechResultTextColor = "#FFFFFF",
                fullSpeechResultTextColor = "#FFFFFF",
                speechResultBoxBackgroundColor = "#69275f",
                textInputTextColor = "#FFFFFF",
                helpTextFontColor = "#FFFFFF",
                partialSpeechResultFontFamily = "Helvetica",
                assistantStateFontFamily = "Helvetica",
                helpTextFontFamily = "Helvetica",
                speakFontFamily = "Helvetica",
                typeFontFamily = "Helvetica",
                textboxFontFamily = "Helvetica",
                equalizerColor = "#b3ffffff,#b3ffffff",
                micActiveColor = "#FFFFFF",
                sendActiveColor = "#FFFFFF",
                sendInactiveColor = "#FFFFFF",
                assistantStateTextColor = "#FFFFFF"
            )
        )
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
                voiceAssistant.onAssistantError(onAssistantError)
                voiceAssistant.show(supportFragmentManager, "assistantDrawerUI")
            }
        }
    }
}