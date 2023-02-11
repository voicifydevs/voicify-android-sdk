package com.voicify.voicify_assistant_sdk.components.toolbar

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import com.voicify.voicify_assistant_sdk.R
import com.voicify.voicify_assistant_sdk.assistantDrawerUITypes.ToolbarProps

class SpeakingAnimation (
    private var context: Context,
    private var toolbarProps: ToolbarProps?,
    private var configurationToolbarProps: ToolbarProps?,
    ) {

    fun initializeSpeakingAnimation(animationBars: Array<View>){
        initializeBarViews(animationBars)
    }

    private fun initializeBarViews(animationBars: Array<View>){
        if(!toolbarProps?.equalizerColor.isNullOrEmpty())
        {
            val splitColors = toolbarProps?.equalizerColor?.split(",")
            if (splitColors!!.size > 1)
            {
                var colors = intArrayOf()
                splitColors.forEach {
                    colors = colors.plus(Color.parseColor(it))
                }
                val gradientDrawable = GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    colors)
                animationBars.forEach { bar ->
                    bar.background = gradientDrawable
                }
            }
            else
            {
                animationBars.forEach { bar ->
                    bar.setBackgroundColor(Color.parseColor(toolbarProps?.equalizerColor))
                }
            }
        }
        else if (!configurationToolbarProps?.equalizerColor.isNullOrEmpty())
        {
            val splitColors = configurationToolbarProps?.equalizerColor?.split(",")
            if (splitColors!!.size > 1)
            {
                var colors = intArrayOf()
                splitColors.forEach {
                    colors = colors.plus(Color.parseColor(it))
                }
                val gradientDrawable = GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    colors)
                animationBars.forEach { bar ->
                    bar.background = gradientDrawable
                }
            }
            else
            {
                animationBars.forEach { bar ->
                    bar.setBackgroundColor(Color.parseColor(configurationToolbarProps?.equalizerColor))
                }
            }
        }
        else{
            animationBars.forEach { bar ->
                bar.setBackgroundColor(Color.parseColor(context.getString(R.string.black_50_percent)))
            }
        }
    }
}