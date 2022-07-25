package com.voicify.assistantsample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.voicify.assistantsample.databinding.ActivityMainBinding
import com.voicify.voicify_assistant_sdk.assistantDrawerUITypes.ToolBar
import com.voicify.voicify_assistant_sdk.assistant.AssistantDrawerUI

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isAssitantOpen: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.assistantMic.setOnClickListener {
            if (savedInstanceState == null) {
                if(!isAssitantOpen)
                {
                    val modalBottomSheet = AssistantDrawerUI.newInstance("HELLO TO THE WORLD", "LEGGO", ToolBar(backgroundColor = "#ffffff"))
                    modalBottomSheet.show(supportFragmentManager, "")
//                    var transaction = supportFragmentManager.beginTransaction()
//                    transaction.add(R.id.relative_container, assistantDrawer)
//                    transaction.commit()
//                    isAssitantOpen = true
                }
            }
        }
    }
}