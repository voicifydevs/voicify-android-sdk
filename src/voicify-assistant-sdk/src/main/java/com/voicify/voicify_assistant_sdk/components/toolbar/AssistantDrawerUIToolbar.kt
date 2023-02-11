package com.voicify.voicify_assistant_sdk.components.toolbar

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.voicify.voicify_assistant_sdk.R
import com.voicify.voicify_assistant_sdk.assistantDrawerUITypes.*
import com.voicify.voicify_assistant_sdk.models.CustomAssistantConfigurationResponse
import kotlinx.android.synthetic.main.fragment_assistant_drawer_u_i.*

class AssistantDrawerUIToolbar(
    private var context: Context,
    private var toolbarProps: ToolbarProps?,
    private var configurationToolbarProps: ToolbarProps?,
    private var assistantSettingProps: AssistantSettingsProps?,
    private var configuration: CustomAssistantConfigurationResponse?,
) {
    private val scale = context.resources.displayMetrics.density

    fun initializeToolbar(
        micImageView: ImageView,
        sendMessageImageView: ImageView,
        speakTextView: TextView,
        typeTextView: TextView,
        drawerHelpTextView: TextView,
        assistantStateTextView: TextView,
        spokenTextView: TextView,
        inputeMessageEditText: EditText,
        drawerLayout: LinearLayout,
    ) {
        initializeMicButton(micImageView)
        initializeSendMessageButton(sendMessageImageView)
        initializeSpeakTextView(speakTextView)
        initializeTypeTextView(typeTextView)
        initializeDrawerHelpTextView(drawerHelpTextView)
        initializeAssistantStateTextView(assistantStateTextView)
        initializeSpokenTextView(spokenTextView)
        initializeInputMessageEditTextView(inputeMessageEditText)
        initializeDrawerLayout(drawerLayout)
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

    private fun initializeDrawerHelpTextView(drawerHelpTextView: TextView){
        drawerHelpTextView.text = toolbarProps?.helpText ?: configurationToolbarProps?.helpText ?: context.getString(R.string.drawer_welcome_text)
        drawerHelpTextView.setTextColor(Color.parseColor(toolbarProps?.helpTextFontColor ?: configurationToolbarProps?.helpTextFontColor ?: context.getString(R.string.dark_gray)))
        drawerHelpTextView.textSize = toolbarProps?.helpTextFontSize ?: configurationToolbarProps?.helpTextFontSize ?: 18f
        drawerHelpTextView.typeface = Typeface.create(
            toolbarProps?.helpTextFontFamily ?: configurationToolbarProps?.helpTextFontFamily ?: context.getString(R.string.default_font),
            Typeface.NORMAL
        )
    }

    private fun initializeAssistantStateTextView(assistantStateTextView: TextView){
        assistantStateTextView.setTextColor(Color.parseColor(toolbarProps?.assistantStateTextColor ?: configurationToolbarProps?.assistantStateTextColor ?: context.getString(R.string.dark_gray)))
        assistantStateTextView.textSize = toolbarProps?.assistantStateFontSize ?: configurationToolbarProps?.assistantStateFontSize ?: 16f
        assistantStateTextView.typeface = Typeface.create(
            toolbarProps?.assistantStateFontFamily ?: configurationToolbarProps?.assistantStateFontFamily ?: context.getString(R.string.default_font),
            Typeface.NORMAL
        )

    }

    private fun initializeSpokenTextView(spokenTextView: TextView){
        spokenTextView.textSize = 16f
        spokenTextView.typeface = Typeface.create(
            toolbarProps?.partialSpeechResultFontFamily ?: configurationToolbarProps?.partialSpeechResultFontFamily ?: context.getString(R.string.default_font),
            Typeface.NORMAL
        )
        val spokenTextViewStyle = GradientDrawable()
        spokenTextViewStyle.cornerRadius = 24f
        spokenTextViewStyle.setColor(Color.parseColor(toolbarProps?.speechResultBoxBackgroundColor ?: configurationToolbarProps?.speechResultBoxBackgroundColor ?: context.getString(R.string.black_50_percent)))
        spokenTextView.background = spokenTextViewStyle
    }

    private fun initializeInputMessageEditTextView(inputeMessageEditText: EditText){
        var inputMessageLineColor: Int
        if((assistantSettingProps?.initializeWithText ?: configuration?.activeInput == context.getString(R.string.textbox)) != true &&
            (assistantSettingProps?.useVoiceInput ?: configuration?.useVoiceInput) != false) {
            inputMessageLineColor = Color.parseColor(toolbarProps?.textInputLineColor ?: configurationToolbarProps?.textInputLineColor ?: context.getString(R.string.silver))
        }
        else {
            inputMessageLineColor = Color.parseColor(toolbarProps?.textInputActiveLineColor ?: configurationToolbarProps?.textInputLineColor ?: context.getString(R.string.silver))
        }
        inputeMessageEditText.hint = toolbarProps?.placeholder ?: configurationToolbarProps?.placeholder ?: context.getString(R.string.textbox_placeholder_text)
        inputeMessageEditText.typeface = Typeface.create(toolbarProps?.textboxFontFamily ?: configurationToolbarProps?.textboxFontFamily ?: context.getString(R.string.default_font), Typeface.NORMAL)
        inputeMessageEditText.setCursorDrawableColor(Color.parseColor(toolbarProps?.textInputCursorColor ?: configurationToolbarProps?.textInputCursorColor ?: context.getString(R.string.dark_light_gray)))
        inputeMessageEditText.setTextColor(Color.parseColor(toolbarProps?.textInputTextColor ?: configurationToolbarProps?.textInputTextColor ?: context.getString(R.string.black)))
        val colorStateList = ColorStateList.valueOf(inputMessageLineColor)
        ViewCompat.setBackgroundTintList(inputeMessageEditText,colorStateList)
        val inputTextMessageEditTextViewStyle = GradientDrawable()
        inputTextMessageEditTextViewStyle.setColor(Color.parseColor(context.getString(R.string.blue_12_percent)))
        inputeMessageEditText.textSize = toolbarProps?.textboxFontSize ?: configurationToolbarProps?.textboxFontSize ?: 18f
    }

    private fun initializeDrawerLayout(draweLayout: LinearLayout){
        if(!(toolbarProps?.backgroundColor ?: configurationToolbarProps?.backgroundColor).isNullOrEmpty()){
            draweLayout.setBackgroundColor(Color.parseColor(toolbarProps?.backgroundColor ?: configurationToolbarProps?.backgroundColor))
        }
        else if ((assistantSettingProps?.backgroundColor ?: configuration?.styles?.assistant?.backgroundColor).isNullOrEmpty())
        {
            draweLayout.setBackgroundColor(Color.parseColor(context.getString(R.string.white)))
        }

        draweLayout.setPadding(
            toolbarProps?.paddingLeft ?: configurationToolbarProps?.paddingLeft ?: HelperMethods.getPixelsFromDp(16, scale),
            toolbarProps?.paddingTop ?: configurationToolbarProps?.paddingTop ?: HelperMethods.getPixelsFromDp(16, scale),
            toolbarProps?.paddingRight ?: configurationToolbarProps?.paddingRight ?: HelperMethods.getPixelsFromDp(16, scale),
            toolbarProps?.paddingBottom ?: configurationToolbarProps?.paddingBottom ?: HelperMethods.getPixelsFromDp(16, scale)
        )
    }
}