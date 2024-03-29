package com.voicify.voicify_assistant_sdk.assistantDrawerUITypes

import java.io.Serializable

data class ToolbarProps(
    val backgroundColor: String? = null,
    val micBorderRadius: Float? = null,
    val micImagePadding: Int? = null,
    val micImageBorderWidth: Int? = null,
    val micImageBorderColor: String? = null,
    val micImageHeight: Int? = null,
    val micImageWidth: Int? = null,
    val micActiveImage: String? = null,
    val micActiveColor: String? = null,
    val micInactiveImage: String? = null,
    val micInactiveColor: String? = null,
    val micActiveHighlightColor: String? = null,
    val micInactiveHighlightColor: String? = null,
    val sendActiveImage: String? = null,
    val sendActiveColor: String? = null,
    val sendImageWidth: Int? = null,
    val sendImageHeight: Int? = null,
    val sendInactiveImage: String? = null,
    val sendInactiveColor: String? = null,
    val speakFontSize: Float? = null,
    val speakActiveTitleColor: String? = null,
    val speakInactiveTitleColor: String? = null,
    val typeFontSize: Float? = null,
    val typeActiveTitleColor: String? = null,
    val typeInactiveTitleColor: String? = null,
    val textboxFontSize: Float? = null,
    val textboxActiveHighlightColor: String? = null,
    val textboxInactiveHighlightColor: String? = null,
    val partialSpeechResultTextColor: String? = null,
    val fullSpeechResultTextColor: String? = null,
    val speechResultBoxBackgroundColor: String? = null,
    val textInputActiveLineColor: String? = null,
    val textInputLineColor: String? = null,
    val textInputCursorColor: String? = null,
    val textInputTextColor: String? = null,
    val paddingLeft: Int? = null,
    val paddingRight: Int? = null,
    val paddingTop: Int? = null,
    val paddingBottom: Int? = null,
    val placeholder: String? = null,
    val helpText: String? = null,
    val helpTextFontSize: Float? = null,
    val helpTextFontColor: String? = null,
    val assistantStateTextColor: String? = null,
    val assistantStateFontSize: Float? = null,
    val equalizerColor: String? = null,
    val partialSpeechResultFontFamily: String? = null,
    val assistantStateFontFamily: String? = null,
    val helpTextFontFamily: String? = null,
    val speakFontFamily: String? = null,
    val typeFontFamily: String? = null,
    val textboxFontFamily: String? = null,
) : Serializable
