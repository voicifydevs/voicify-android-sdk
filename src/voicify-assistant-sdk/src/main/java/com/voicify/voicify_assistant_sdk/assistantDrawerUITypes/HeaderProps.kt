package com.voicify.voicify_assistant_sdk.assistantDrawerUITypes

import java.io.Serializable

data class HeaderProps(
    val fontSize: Float? = null,
    val backgroundColor: String? = null,
    val assistantImage: String? = null,
    val assistantImageBackgroundColor: String? = null,
    val assistantName: String? = null,
    val assistantNameTextColor: String? = null,
    val assistantImageBorderRadius: Float? = null,
    val assistantImageBorderColor: String? = null,
    val assistantImageBorderWidth: Int? = null,
    //val assistantImageBorderStyle: String? = null,
    val closeAssistantButtonImage: String? = null,
    val closeAssistantButtonBorderRadius: Float? = null,
    val closeAssistantButtonBackgroundColor: String? = null,
    val closeAssistantButtonBorderWidth: Int? = null,
    //val closeAssistantButtonBorderStyle: String? = null,
    val closeAssistantButtonBorderColor: String? = null,
    val paddingLeft: Int? = null,
    val paddingRight: Int? = null,
    val paddingTop: Int? = null,
    val paddingBottom: Int? = null,
) : Serializable
