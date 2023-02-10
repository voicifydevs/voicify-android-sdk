package com.voicify.voicify_assistant_sdk.components

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.voicify.voicify_assistant_sdk.R
import com.voicify.voicify_assistant_sdk.assistantDrawerUITypes.HeaderProps
import com.voicify.voicify_assistant_sdk.assistantDrawerUITypes.HelperMethods

class AssistantDrawerUIHeader(
    private var context: Context,
    private var headerProps: HeaderProps?,
    private var configurationHeaderProps: HeaderProps?,
    private var resources: Resources
) {

    fun initializeHeader(
        closeAssistantImageView: ImageView,
        avatarImageView: ImageView,
        closeBackgroundLayout: LinearLayout,
        avatarBackgroundLayout: LinearLayout,
        assistantNameTextView: TextView
    ){
        initializeCloseAssistantButton(closeBackgroundLayout, closeAssistantImageView)
        initializeAssistantAvatar(avatarBackgroundLayout, avatarImageView)
        initializeAssistantName(assistantNameTextView)
    }

    private fun initializeCloseAssistantButton(closeBackgroundLayout: LinearLayout, closeAssistantImageView: ImageView){
        val closeAssistantImageBackgroundStyle = GradientDrawable()
        closeAssistantImageBackgroundStyle.cornerRadius = headerProps?.closeAssistantButtonBorderRadius ?: configurationHeaderProps?.closeAssistantButtonBorderRadius ?: 0f
        closeAssistantImageBackgroundStyle.setStroke(headerProps?.closeAssistantButtonBorderWidth ?: configurationHeaderProps?.closeAssistantButtonBorderWidth ?: 0, Color.parseColor(headerProps?.closeAssistantButtonBorderColor ?: configurationHeaderProps?.closeAssistantButtonBorderColor ?: context.getString(R.string.transparent)))
        closeAssistantImageBackgroundStyle.setColor(Color.parseColor(headerProps?.closeAssistantButtonBackgroundColor ?: configurationHeaderProps?.closeAssistantButtonBackgroundColor ?: context.getString(R.string.transparent)))
        closeBackgroundLayout.background = closeAssistantImageBackgroundStyle
        closeBackgroundLayout.setPadding(12,12,12,12)

        HelperMethods.loadImageFromUrl(
            url = headerProps?.closeAssistantButtonImage ?: configurationHeaderProps?.closeAssistantButtonImage ?:context.getString(R.string.close_assistant_image),
            view = closeAssistantImageView,
            imageColor = headerProps?.closeAssistantColor ?: configurationHeaderProps?.closeAssistantColor
        )
    }

    private fun initializeAssistantAvatar(avatarBackgroundLayout: LinearLayout, avatarImageView: ImageView){
        val avatarBackgroundStyle = GradientDrawable()
        avatarBackgroundStyle.cornerRadius = headerProps?.assistantImageBorderRadius ?: configurationHeaderProps?.assistantImageBorderRadius ?: 48f
        avatarBackgroundStyle.setStroke(
            headerProps?.assistantImageBorderWidth ?: configurationHeaderProps?.assistantImageBorderWidth ?: 0,
            Color.parseColor(headerProps?.assistantImageBorderColor ?: configurationHeaderProps?.assistantImageBorderColor ?: context.getString(R.string.transparent))
        )
        avatarBackgroundStyle.setColor(Color.parseColor(headerProps?.assistantImageBackgroundColor ?: configurationHeaderProps?.assistantImageBackgroundColor ?: context.getString(R.string.transparent)))
        avatarBackgroundLayout.background = avatarBackgroundStyle
        avatarBackgroundLayout.setPadding(
            headerProps?.assistantImagePadding ?: configurationHeaderProps?.assistantImagePadding ?:12,
            headerProps?.assistantImagePadding ?: configurationHeaderProps?.assistantImagePadding ?:12,
            headerProps?.assistantImagePadding ?: configurationHeaderProps?.assistantImagePadding ?:12,
            headerProps?.assistantImagePadding ?: configurationHeaderProps?.assistantImagePadding ?:12
        )

        HelperMethods.loadImageFromUrl(
            url = headerProps?.assistantImage ?: configurationHeaderProps?.assistantImage ?: context.getString(R.string.header_avatar_image),
            view = avatarImageView,
            imageColor = headerProps?.assistantImageColor ?: configurationHeaderProps?.assistantImageColor,
            resources = resources,
            isRounded = true,
            borderRadius = headerProps?.assistantImageBorderRadius ?: configurationHeaderProps?.assistantImageBorderRadius ?: 200f
        )
        val assistantImageLayoutParams = LinearLayout.LayoutParams(
            headerProps?.assistantImageWidth ?: configurationHeaderProps?.assistantImageWidth ?: HelperMethods.getPixelsFromDp(34, context.resources.displayMetrics.density),
            headerProps?.assistantImageHeight ?: configurationHeaderProps?.assistantImageWidth ?: HelperMethods.getPixelsFromDp(34, context.resources.displayMetrics.density)
        )
        avatarImageView.layoutParams = assistantImageLayoutParams
    }

    private fun initializeAssistantName(assistantNameTextView: TextView){
        assistantNameTextView.typeface = Typeface.create(
            headerProps?.fontFamily ?: configurationHeaderProps?.fontFamily ?: context.getString(R.string.default_font),
            Typeface.NORMAL
        )
        assistantNameTextView.text = headerProps?.assistantName ?: configurationHeaderProps?.assistantName ?: context.getString(R.string.default_assistant_name)
        assistantNameTextView.textSize = headerProps?.fontSize ?: configurationHeaderProps?.fontSize ?: 18f
        assistantNameTextView.setTextColor(Color.parseColor(headerProps?.assistantNameTextColor ?: configurationHeaderProps?.assistantNameTextColor ?: context.getString(R.string.black)))
    }
}