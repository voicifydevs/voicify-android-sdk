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
import kotlin.math.roundToInt

class SpeakingAnimation{

    fun initializeSpeakingAnimation(
        context: Context,
        toolbarProps: ToolbarProps?,
        configurationToolbarProps: ToolbarProps?,
        animationBars: Array<View>
    ){
        initializeBarViews(context, toolbarProps, configurationToolbarProps, animationBars)
    }

    private fun initializeBarViews(context: Context, toolbarProps: ToolbarProps?, configurationToolbarProps: ToolbarProps?, animationBars: Array<View>){
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

    fun generateAnimationValues(volume: Float, context: Context, animationBars: Array<View>?): List<ObjectAnimator>{
        var barsToAnimate: List<ObjectAnimator> = emptyList()
        val duration = 100L
        var index = 0
        animationBars?.forEach {bar ->
            val barToAnimateSize =
                if(index == 0 || index == 7) {
                    (1..(volume.roundToInt() * 2 + 1)).random().toFloat()
                } else if (index == 1 || index == 6) {
                    (1..(volume.roundToInt() * 3 + 1)).random().toFloat()
                } else if (index == 2 || index == 5) {
                    (1..(volume.roundToInt() * 5 + 1)).random().toFloat()
                } else {
                    (1..(volume.roundToInt() * 6 + 1)).random().toFloat()
                }
            barsToAnimate = barsToAnimate.plus(ObjectAnimator.ofFloat(bar, context.getString(R.string.animation_scale_y), barToAnimateSize))
            barsToAnimate[index].duration = duration
            index++
        }
        return barsToAnimate
    }

    fun clearAnimationValues(animation: AnimatorSet?, context: Context, animationBars: Array<View>?){
        var barToClear: Array<ObjectAnimator> = emptyArray()
        animation?.end()
        val duration = 300L
        animationBars?.forEach { bar ->
            barToClear = barToClear.plus(ObjectAnimator.ofFloat(bar,context.getString(R.string.animation_scale_y),1f))
            barToClear[barToClear.size - 1].duration = duration
        }
        AnimatorSet().apply {
            playTogether(barToClear[0], barToClear[1], barToClear[2],barToClear[3],barToClear[4],barToClear[5],barToClear[6],barToClear[7])
            start()
        }
    }
}