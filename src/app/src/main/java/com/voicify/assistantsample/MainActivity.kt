package com.voicify.assistantsample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.voicify.assistantsample.databinding.ActivityMainBinding
import com.voicify.voicify_assistant_sdk.assistantDrawerUITypes.ToolBarProps
import com.voicify.voicify_assistant_sdk.assistant.AssistantDrawerUI
import com.voicify.voicify_assistant_sdk.assistantDrawerUITypes.AssistantSettingsProps
import com.voicify.voicify_assistant_sdk.assistantDrawerUITypes.BodyProps
import com.voicify.voicify_assistant_sdk.assistantDrawerUITypes.HeaderProps

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isAssitantOpen: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.assistantMic.setOnClickListener {
            if (savedInstanceState == null) {
                    val modalBottomSheet = AssistantDrawerUI.newInstance(HeaderProps(backgroundColor = "#ffffff"),
                        BodyProps(backgroundColor = "#ffffff"),
                        ToolBarProps(backgroundColor = "#ffffff"),
                        AssistantSettingsProps(appId = "99a803b7-5b37-426c-a02e-63c8215c71eb",
                            appKey = "MTAzM2RjNDEtMzkyMC00NWNhLThhOTYtMjljMDc3NWM5NmE3",
                            serverRootUrl = "https://assistant.voicify.com",
                            locale = "en-US",
                            channel = "My App",
                            device = "My Device",
                            autoRunConversation = true,
                            initializeWithWelcomeMessage = false,
                            useVoiceInput = true,
                            useOutputSpeech = true,
                            initializeWithText = false)
                    )
                    modalBottomSheet.show(supportFragmentManager, "assistantDrawerUI")
                    //modalBottomSheet.dismiss()
            }
        }
    }
}