package com.voicify.voicify_assistant_sdk.components

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.constraintlayout.solver.widgets.Helper
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

    fun initializeToolbar(micImageView: ImageView, sendMessageImageView: ImageView, ) {
        initializeMicButton(micImageView)
        initializeSendMessageButton(sendMessageImageView)
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
        var sendImageUrl = ""
        val sendImageColor: String?
        if(!(assistantSettingProps?.initializeWithText ?: configuration?.activeInput == context.getString(R.string.textbox)) && assistantSettingProps?.useVoiceInput ?: configuration?.useVoiceInput != true)
        {
            sendImageUrl = toolbarProps?.sendInactiveImage ?: configurationToolbarProps?.sendInactiveImage ?: context.getString(R.string.send_inactive_image)
        }
        else{
            sendImageUrl = toolbarProps?.sendActiveImage ?: configurationToolbarProps?.sendActiveImage ?: context.getString(R.string.send_active_image)
        }

        if(!(assistantSettingProps?.initializeWithText ?: configuration?.activeInput == context.getString(R.string.textbox)) && (assistantSettingProps?.useVoiceInput ?: configuration?.useVoiceInput) == false){
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
}