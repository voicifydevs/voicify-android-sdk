package com.voicify.voicify_assistant_sdk.assistantDrawerUITypes

import java.io.Serializable

data class HeaderProps(
    val fontSize: Float? = null,
    val backgroundColor: String? = null,
    val assistantImage: String? = null,
    val assistantImageColor: String? = null,
    val assistantImageBackgroundColor: String? = null,
    val assistantImagePadding: Int? = null,
    val assistantImageWidth: Int? = null,
    val assistantImageHeight: Int? = null,
    val assistantName: String? = null,
    val assistantNameTextColor: String? = null,
    val assistantImageBorderRadius: Float? = null,
    val assistantImageBorderColor: String? = null,
    val assistantImageBorderWidth: Int? = null,
    val closeAssistantButtonImage: String? = null,
    val closeAssistantColor: String? = null,
    val closeAssistantButtonBorderRadius: Float? = null,
    val closeAssistantButtonBackgroundColor: String? = null,
    val closeAssistantButtonBorderWidth: Int? = null,
    val closeAssistantButtonBorderColor: String? = null,
    val paddingLeft: Int? = null,
    val paddingRight: Int? = null,
    val paddingTop: Int? = null,
    val paddingBottom: Int? = null,
    val fontFamily: String? = null
) : Serializable
