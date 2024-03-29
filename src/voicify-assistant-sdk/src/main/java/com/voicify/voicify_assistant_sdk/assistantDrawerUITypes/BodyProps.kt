package com.voicify.voicify_assistant_sdk.assistantDrawerUITypes

import java.io.Serializable

data class BodyProps(
    val backgroundColor: String? = null,
    val assistantImageBorderColor: String? = null,
    val assistantImageBorderWidth: Int? = null,
    val assistantImageBorderRadius: Float? = null,
    val assistantImage: String? = null,
    val assistantImageColor: String? = null,
    val assistantImageBackgroundColor: String? = null,
    val assistantImagePadding: Int? = null,
    val assistantImageWidth: Int? = null,
    val assistantImageHeight: Int? = null,
    val messageSentTextColor: String? = null,
    val messageSentBackgroundColor: String? = null,
    val messageReceivedFontSize: Float? = null,
    val messageReceivedTextColor: String? = null,
    val messageReceivedBackgroundColor: String? = null,
    val messageSentFontSize: Float? = null,
    val messageSentBorderWidth: Int? = null,
    val messageSentBorderColor: String? = null,
    val messageReceivedBorderWidth: Int? = null,
    val messageReceivedBorderColor: String? = null,
    val messageSentBorderTopLeftRadius: Float? = null,
    val messageSentBorderTopRightRadius: Float? = null,
    val messageSentBorderBottomLeftRadius: Float? = null,
    val messageSentBorderBottomRightRadius: Float? = null,
    val messageReceivedBorderTopLeftRadius: Float? = null,
    val messageReceivedBorderTopRightRadius: Float? = null,
    val messageReceivedBorderBottomLeftRadius: Float? = null,
    val messageReceivedBorderBottomRightRadius: Float? = null,
    val paddingLeft: Int? = null,
    val paddingRight: Int? = null,
    val paddingTop: Int? = null,
    val paddingBottom: Int? = null,
    val borderTopColor: String? = null,
    val borderTopWidth: Int? = null,
    val borderBottomColor: String? = null,
    val borderBottomWidth: Int? = null,
    val hintsTextColor: String? = null,
    val hintsFontSize: Float? = null,
    val hintsPaddingTop: Int? = null,
    val hintsPaddingBottom: Int? = null,
    val hintsPaddingRight: Int? = null,
    val hintsPaddingLeft: Int? = null,
    val hintsBackgroundColor: String? = null,
    val hintsBorderWidth: Int? = null,
    val hintsBorderColor: String? = null,
    val hintsBorderRadius: Float? = null,
    val messageSentFontFamily: String? = null,
    val messageReceivedFontFamily: String? = null,
    val hintsFontFamily: String? = null
) : Serializable
