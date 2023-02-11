package com.voicify.voicify_assistant_sdk.components.toolbar

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import com.voicify.voicify_assistant_sdk.R
import com.voicify.voicify_assistant_sdk.assistantDrawerUITypes.ToolbarProps
import kotlinx.android.synthetic.main.fragment_assistant_drawer_u_i.*

class SpeakingAnimation (
    private var context: Context,
    private var toolbarProps: ToolbarProps?,
    private var configurationToolbarProps: ToolbarProps?,
    private var animationBars: Array<View>
    ) {

    fun initializeSpeakingAnimation(){
        initializeBarViews()
    }

    private fun initializeBarViews(){
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

    fun clearAnimationValues(animation: AnimatorSet?){
        var barToClear: Array<ObjectAnimator> = emptyArray()
        animation?.end()
        val duration = 300L
        animationBars.forEach { bar ->
            barToClear = barToClear.plus(ObjectAnimator.ofFloat(bar,context.getString(R.string.animation_scale_y),1f))
            barToClear[barToClear.size - 1].duration = duration
        }
        AnimatorSet().apply {
            playTogether(barToClear[0], barToClear[1], barToClear[2],barToClear[3],barToClear[4],barToClear[5],barToClear[6],barToClear[7])
            start()
        }
    }
}