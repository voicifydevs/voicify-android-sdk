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
                        backgroundColor = "#4C753F",
                        assistantImage = "https://1000logos.net/wp-content/uploads/2021/12/Panera-Bread-logo-768x432.png",
                        assistantImageBackgroundColor = "#ffffff",
                        assistantName = "Panera Bread Assistant",
                        assistantNameTextColor = "#ffffff",
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
                        assistantImage = "https://1000logos.net/wp-content/uploads/2021/12/Panera-Bread-logo-768x432.png",
                        messageSentTextColor = "#ffffff",
                        messageSentBackgroundColor = "#4C753F",
                        messageReceivedFontSize = 14f,
                        messageReceivedTextColor = "#131313",
                        messageReceivedBackgroundColor =  "#10000000",
                        messageSentFontSize = 14f,
                        //messageSentBorderWidth = 0,
                        //messageSentBorderColor = "#00ffffff",
                        messageReceivedBorderWidth = 4,
                        messageReceivedBorderColor = "#CCCCCC",
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
//                        hintsBackgroundColor = "",
//                        hintsBorderColor = "",
                        hintsBorderRadius = 60f,
//                        hintsBorderWidth = 0,
//                        hintsFontSize = 14f,
                        hintsPaddingBottom = 20,
                        hintsPaddingLeft = 40,
                        hintsPaddingRight = 40,
                        hintsPaddingTop = 20,
                        hintsTextColor = "#000000",
                    ),
                    ToolBarProps(
                        backgroundColor = "#ffffff",
                        micBorderRadius  = 100f,
//                        micImagePadding = 4,
//                        micImageBorderWidth = 4,
//                        micImageBorderColor = "#000000",
//                        micImageHeight = 60,
//                        micImageWidth = 60,
                        //micActiveImage = "https://voicify-prod-files.s3.amazonaws.com/12f5d424-b669-4e17-8967-4b483238fb90/e5d47174-feee-423a-9b3e-16be754a32f5/Frame-581.png",
//                        micInactiveImage = "",
//                        sendActiveImage = "",
//                        sendInactiveImage = "",
                        micActiveHighlightColor = "#EED484",
                        micInactiveHighlightColor = "#00ffffff",
                        //sendActiveImage = "https://voicify-prod-files.s3.amazonaws.com/12f5d424-b669-4e17-8967-4b483238fb90/640cc81b-2efa-4591-a261-5936a63136ad/send.png",
//                        sendInactiveImage = "",
                        speakFontSize = 12f,
                        speakActiveTitleColor = "#4C753F",
                        speakInactiveTitleColor = "#131313",
                        typeFontSize = 12f,
                        typeActiveTitleColor = "#4C753F",
                        typeInactiveTitleColor = "#131313",
                        textBoxFontSize = 18f,
                        textBoxActiveHighlightColor = "#EED484",
                        textBoxInactiveHighlightColor = "#00ffffff",
                        partialSpeechResultTextColor = "#33ffffff",
                        fullSpeechResultTextColor = "#ffffff",
                        speechResultBoxBackgroundColor = "#4C753F",
//                        paddingRight = 0,
//                        paddingLeft = 0,
//                        paddingTop = 0,
//                        paddingBottom = 0,
                        placeholder = "",
                        helpText = "How can i help?",
                        helpTextFontSize = 18f,
                        helpTextFontColor = "#000000",
                        assistantStateTextColor = "#000000",
                        assistantStateFontSize = 16f,
                        equalizerColor = "#4C753F"
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