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
        val onEffect: (String, Any) -> Unit = { effectName, data ->
            if(effectName == "Play")
            {
                Log.d("JAMES", "here comes the data CLAP $data")
            }
        }
        binding.assistantMic.setOnClickListener {
            if (savedInstanceState == null) {
                val voiceAssistant = AssistantDrawerUI.newInstance(
                    HeaderProps(
                        fontSize = 18f,
                        backgroundColor = "#ffffff",
                        assistantImage = "https://voicify-prod-files.s3.amazonaws.com/99a803b7-5b37-426c-a02e-63c8215c71eb/eb7d2538-a3dc-4304-b58c-06fdb34e9432/Mark-Color-3-.png",
                        assistantImageBackgroundColor = "#ffffff",
                        assistantName = "Voicify Assistant",
                        assistantNameTextColor = "#000000",
                        assistantImageBorderRadius = 48f,
                        assistantImageBorderColor = "#CBCCD2",
                        assistantImageBorderWidth = 4,
                        closeAssistantButtonImage = "https://voicify-prod-files.s3.amazonaws.com/99a803b7-5b37-426c-a02e-63c8215c71eb/a6de04bb-e572-4a55-8cd9-1a7628285829/delete-2.png",
                        //closeAssistantButtonBorderRadius = 0f,
                        //closeAssistantButtonBackgroundColor = "blue",
                        //closeAssistantButtonBorderWidth = 0,
                        //closeAssistantButtonBorderColor = "red",
                        paddingLeft = 16,
                        paddingTop = 16,
                        paddingRight = 16,
                        paddingBottom = 16
                    ),
                    BodyProps(
                        backgroundColor = "#F4F4F6",
                        assistantImageBorderColor = "#CBCCD2",
                        assistantImageBorderWidth = 4,
                        assistantImageBorderRadius = 38f,
                        assistantImage = "https://voicify-prod-files.s3.amazonaws.com/99a803b7-5b37-426c-a02e-63c8215c71eb/eb7d2538-a3dc-4304-b58c-06fdb34e9432/Mark-Color-3-.png",
                        messageSentTextColor = "#ffffff",
                        messageSentBackgroundColor = "#80000000",
                        messageReceivedFontSize = 14f,
                        messageReceivedTextColor = "#000000",
                        messageReceivedBackgroundColor =  "#0d000000",
                        messageSentFontSize = 14f,
                        //messageSentBorderWidth = 0,
                        //messageSentBorderColor = "#00ffffff",
                        messageReceivedBorderWidth = 4,
                        messageReceivedBorderColor = "#CBCCD2",
                        messageSentBorderTopLeftRadius = 18f,
                        messageSentBorderTopRightRadius = 0f,
                        messageSentBorderBottomLeftRadius = 18f,
                        messageSentBorderBottomRightRadius = 18f,
                        messageReceivedBorderTopLeftRadius = 0f,
                        messageReceivedBorderTopRightRadius = 18f,
                        messageReceivedBorderBottomLeftRadius = 18f,
                        messageReceivedBorderBottomRightRadius = 18f,
                        paddingLeft = 20,
                        paddingRight = 20,
                        paddingTop = 0,
                        paddingBottom = 0,
//                        borderTopColor = "#CBCCD2",
//                        borderBottomColor = "#CBCCD2",
                        borderColor = "#CBCCD2",
                        hintsBackgroundColor = "",
                        hintsBorderColor = "",
                        hintsBorderRadius = 0f,
                        hintsBorderWidth = 0,
                        hintsFontSize = 14f,
                        hintsPaddingBottom = 0,
                        hintsPaddingLeft = 0,
                        hintsPaddingRight = 0,
                        hintsPaddingTop = 0,
                        hintsTextColor = "",
                    ),
                    ToolBarProps(
                        backgroundColor = "#ffffff",
                        micBorderRadius  = 100f,
//                        micImagePadding = 4,
//                        micImageBorderWidth = 4,
//                        micImageBorderColor = "#000000",
//                        micImageHeight = 60,
//                        micImageWidth = 60,
//                        micActiveImage = "",
//                        micInactiveImage = "",
//                        sendActiveImage = "",
//                        sendInactiveImage = "",
                        micActiveHighlightColor = "#1f1e7eb9",
                        micInactiveHighlightColor = "#ffffff",
//                        sendActiveImage = "",
//                        sendInactiveImage = "",
                        speakFontSize = 12f,
                        speakActiveTitleColor = "#3E77A5",
                        speakInactiveTitleColor = "#8F97A1",
                        typeFontSize = 12f,
                        typeActiveTitleColor = "#3E77A5",
                        typeInactiveTitleColor = "#8F97A1",
                        textBoxFontSize = 18f,
                        textBoxActiveHighlightColor = "#1f1e7eb9",
                        textBoxInactiveHighlightColor = "#00ffffff",
                        partialSpeechResultTextColor = "#33ffffff",
                        fullSpeechResultTextColor = "#ffffff",
                        speechResultBoxBackgroundColor = "#80000000",
//                        paddingRight = 0,
//                        paddingLeft = 0,
//                        paddingTop = 0,
//                        paddingBottom = 0,
                        placeholder = "",
                        helpText = "How can i help?",
                        helpTextFontSize = 18f,
                        helpTextFontColor = "#8F97A1",
                        assistantStateTextColor = "#8F97A1",
                        assistantStateFontSize = 16f,
                        equalizerColor = "#80000000"
                    ),
                    AssistantSettingsProps(
                        appId = "99a803b7-5b37-426c-a02e-63c8215c71eb",
                        appKey = "MTAzM2RjNDEtMzkyMC00NWNhLThhOTYtMjljMDc3NWM5NmE3",
                        serverRootUrl = "https://assistant.voicify.com",
                        locale = "en-US",
                        channel = "My App",
                        device = "My Device",
                        autoRunConversation = true,
                        initializeWithWelcomeMessage = false,
                        useVoiceInput = true,
                        useOutputSpeech = true,
                        initializeWithText = false,
                        effects = arrayOf("Play"),
                        onEffect = onEffect
                        )
                )
                voiceAssistant.show(supportFragmentManager, "assistantDrawerUI")
                //modalBottomSheet.dismiss()
            }
        }
    }
}