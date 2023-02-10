package com.voicify.voicify_assistant_sdk.components

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.voicify.voicify_assistant_sdk.R
import com.voicify.voicify_assistant_sdk.assistantDrawerUITypes.AssistantSettingsProps
import com.voicify.voicify_assistant_sdk.assistantDrawerUITypes.HelperMethods
import com.voicify.voicify_assistant_sdk.assistantDrawerUITypes.ToolbarProps
import com.voicify.voicify_assistant_sdk.models.CustomAssistantConfigurationResponse

class AssistantDrawerUIToolbar(
    private var context: Context,
    private var toolbarProps: ToolbarProps?,
    private var configurationToolbarProps: ToolbarProps?,
    private var assistantSettingProps: AssistantSettingsProps?,
    private var configuration: CustomAssistantConfigurationResponse?
) {
    private val scale = context.resources.displayMetrics.density

    fun initializeToolbar(
        micImageView: ImageView,
        sendMessageImageView: ImageView,
        speakTextView: TextView,
        typeTextView: TextView
    ) {
        initializeMicButton(micImageView)
        initializeSendMessageButton(sendMessageImageView)
        initializeSpeakTextView(speakTextView)
        initializeTypeTextView(typeTextView)
    }

    private fun initializeMicButton(micImageView: ImageView) {
        var micImageUrl = ""
        val micImageColor: String?

        if((assistantSettingProps?.initializeWithText ?: configuration?.activeInput == context.getString(R.string.textbox)) != true) {
            micImageUrl = toolbarProps?.micActiveImage ?: configurationToolbarProps?.micActiveImage ?: context.getString(R.string.mic_active_image)
        }
        else{
            micImageUrl = toolbarProps?.micInactiveImage ?: configurationToolbarProps?.micInactiveImage ?: context.getString(R.string.mic_inactive_image)
        }

        if(!(assistantSettingProps?.initializeWithText ?: configuration?.activeInput == context.getString(R.string.textbox)))
        {
            micImageColor =  toolbarProps?.micActiveColor ?: configurationToolbarProps?.micActiveColor
        }
        else{
            micImageColor =  toolbarProps?.micInactiveColor ?: configurationToolbarProps?.micInactiveColor
        }

        if((assistantSettingProps?.useVoiceInput ?: configuration?.useVoiceInput) == false) {
            micImageView.visibility = View.GONE
        }
        else{
            HelperMethods.loadImageFromUrl(
                url =  micImageUrl,
                view = micImageView,
                imageColor =  micImageColor
            )
        }

        val micImageLayoutParams = LinearLayout.LayoutParams(
            toolbarProps?.micImageWidth ?: configurationToolbarProps?.micImageWidth ?: HelperMethods.getPixelsFromDp(48, scale),
            toolbarProps?.micImageHeight ?: configurationToolbarProps?.micImageHeight ?: HelperMethods.getPixelsFromDp(48, scale)
        )
        micImageLayoutParams.setMargins(0,HelperMethods.getPixelsFromDp(12,scale),0,0)
        micImageView.layoutParams = micImageLayoutParams
        micImageView.setPadding(
            toolbarProps?.micImagePadding ?: configurationToolbarProps?.micImagePadding ?: HelperMethods.getPixelsFromDp(4, scale),
            toolbarProps?.micImagePadding ?: configurationToolbarProps?.micImagePadding ?: HelperMethods.getPixelsFromDp(4, scale),
            toolbarProps?.micImagePadding ?: configurationToolbarProps?.micImagePadding ?: HelperMethods.getPixelsFromDp(4, scale),
            toolbarProps?.micImagePadding ?: configurationToolbarProps?.micImagePadding ?: HelperMethods.getPixelsFromDp(4, scale)
        )
    }

    private fun initializeSendMessageButton(sendMessageImageView: ImageView){
        var sendImageUrl: String
        val sendImageColor: String?
        if(!(assistantSettingProps?.initializeWithText ?: configuration?.activeInput == context.getString(R.string.textbox))
            && assistantSettingProps?.useVoiceInput ?: configuration?.useVoiceInput != false)
        {
            sendImageUrl = toolbarProps?.sendInactiveImage ?: configurationToolbarProps?.sendInactiveImage ?: context.getString(R.string.send_inactive_image)
        }
        else{
            sendImageUrl = toolbarProps?.sendActiveImage ?: configurationToolbarProps?.sendActiveImage ?: context.getString(R.string.send_active_image)
        }

        if(!(assistantSettingProps?.initializeWithText ?: configuration?.activeInput == context.getString(R.string.textbox))
            && (assistantSettingProps?.useVoiceInput ?: configuration?.useVoiceInput) == false)
        {
            sendImageColor = toolbarProps?.sendInactiveColor ?: configurationToolbarProps?.sendInactiveColor
        }
        else{
            sendImageColor = toolbarProps?.sendActiveColor ?: configurationToolbarProps?.sendActiveColor
        }

        HelperMethods.loadImageFromUrl(
            url = sendImageUrl,
            view = sendMessageImageView,
            imageColor = sendImageColor
        )
        val sendImageLayoutParams = LinearLayout.LayoutParams(
            toolbarProps?.sendImageWidth ?: configurationToolbarProps?.sendImageWidth ?: HelperMethods.getPixelsFromDp(25, scale),
            toolbarProps?.sendImageHeight ?: configurationToolbarProps?.sendImageHeight ?: HelperMethods.getPixelsFromDp(25, scale))
        sendImageLayoutParams.gravity = Gravity.CENTER
        sendMessageImageView.layoutParams = sendImageLayoutParams
    }

    private fun initializeSpeakTextView(speakTextView: TextView){
        var speakTextColor: Int
        if((assistantSettingProps?.useVoiceInput ?: configuration?.useVoiceInput) == false)
        {

            speakTextView.visibility = View.GONE
        }
        else{
            if((assistantSettingProps?.initializeWithText ?: configuration?.activeInput == context.getString(R.string.textbox)) != true &&
                (assistantSettingProps?.useVoiceInput ?: configuration?.useVoiceInput) != false) {
                speakTextColor = Color.parseColor(
                    toolbarProps?.speakActiveTitleColor
                        ?: configurationToolbarProps?.speakActiveTitleColor
                        ?: context.getString(R.string.dark_blue)
                )
            }
            else {
                speakTextColor = Color.parseColor(
                    toolbarProps?.speakInactiveTitleColor
                        ?: configurationToolbarProps?.speakInactiveTitleColor
                        ?: context.getString(R.string.dark_gray)
                )
            }

            speakTextView.setTextColor(speakTextColor)
            speakTextView.textSize = toolbarProps?.speakFontSize ?: configurationToolbarProps?.speakFontSize ?: 12f
            speakTextView.typeface = Typeface.create(
                toolbarProps?.speakFontFamily ?: configurationToolbarProps?.speakFontFamily ?: context.getString(R.string.default_font),
                Typeface.NORMAL
            )
        }
    }

    private fun initializeTypeTextView(typeTextView: TextView){
        var typeTextColor: Int
        if((assistantSettingProps?.initializeWithText ?: configuration?.activeInput == context.getString(R.string.textbox)) != true &&
            (assistantSettingProps?.useVoiceInput ?: configuration?.useVoiceInput) != false) {
            typeTextColor = Color.parseColor(
                toolbarProps?.typeInactiveTitleColor
                    ?: configurationToolbarProps?.typeInactiveTitleColor
                    ?: context.getString(R.string.dark_gray)
            )
        }
        else {
            typeTextColor = Color.parseColor(
                toolbarProps?.typeActiveTitleColor
                    ?: configurationToolbarProps?.typeActiveTitleColor
                    ?: context.getString(R.string.dark_blue)
            )
        }
        typeTextView.setTextColor(typeTextColor)
        typeTextView.textSize = toolbarProps?.typeFontSize ?: configurationToolbarProps?.typeFontSize ?: 12f
        typeTextView.typeface = Typeface.create(toolbarProps?.typeFontFamily ?: configurationToolbarProps?.typeFontFamily ?: context.getString(R.string.default_font), Typeface.NORMAL)
    }
}