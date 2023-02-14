package com.voicify.voicify_assistant_sdk.models

import com.voicify.voicify_assistant_sdk.assistantDrawerUITypes.BodyProps
import com.voicify.voicify_assistant_sdk.assistantDrawerUITypes.HeaderProps
import com.voicify.voicify_assistant_sdk.assistantDrawerUITypes.ToolbarProps

data class ConfigurationStyle(
    val assistant: ConfigurationAssistantStyle,
    val body: BodyProps,
    val toolbar: ToolbarProps,
    val header: HeaderProps
)