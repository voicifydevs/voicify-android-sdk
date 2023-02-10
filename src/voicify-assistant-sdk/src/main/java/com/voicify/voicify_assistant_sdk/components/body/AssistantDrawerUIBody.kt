package com.voicify.voicify_assistant_sdk.components.body

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.view.View
import android.widget.LinearLayout
import com.voicify.voicify_assistant_sdk.R
import com.voicify.voicify_assistant_sdk.assistantDrawerUITypes.BodyProps

class AssistantDrawerUIBody (
    private var context: Context,
    private var bodyProps: BodyProps?,
    private var configurationBodyProps: BodyProps?,
) {
    fun initializeBody(bodyBorderTopView: View, bodyBorderBottomView: View){
        initializeBorders(
            bodyBorderTopView = bodyBorderTopView,
            bodyBorderBottomView = bodyBorderBottomView
        )
    }

    private fun initializeBorders(bodyBorderTopView: View, bodyBorderBottomView: View){
        bodyBorderTopView.setBackgroundColor(Color.parseColor(bodyProps?.borderTopColor ?: configurationBodyProps?.borderTopColor ?: context.getString(R.string.gray)))
        val bodyBorderTopViewLayoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, bodyProps?.borderTopWidth ?: configurationBodyProps?.borderTopWidth ?: 4)
        bodyBorderTopView.layoutParams = bodyBorderTopViewLayoutParams

        bodyBorderBottomView.setBackgroundColor(Color.parseColor(bodyProps?.borderBottomColor ?: configurationBodyProps?.borderBottomColor ?: context.getString(R.string.gray)))
        val bodyBorderBottomViewLayoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            bodyProps?.borderBottomWidth ?: configurationBodyProps?.borderBottomWidth ?: 4
        )
        bodyBorderBottomView.layoutParams = bodyBorderBottomViewLayoutParams
    }
}