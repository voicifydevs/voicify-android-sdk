package com.voicify.voicify_assistant_sdk.components.body

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.voicify.voicify_assistant_sdk.R
import com.voicify.voicify_assistant_sdk.assistantDrawerUITypes.AssistantSettingsProps
import com.voicify.voicify_assistant_sdk.assistantDrawerUITypes.BodyProps
import com.voicify.voicify_assistant_sdk.assistantDrawerUITypes.Message
import com.voicify.voicify_assistant_sdk.models.CustomAssistantConfigurationResponse

class AssistantDrawerUIBody{
    internal fun initializeBody(bodyBorderTopView: View,
                       bodyBorderBottomView: View,
                       bodyLayout: LinearLayout,
                       messagesRecycler: RecyclerView,
                       hintsRecycler: RecyclerView,
                       messages: ArrayList<Message>,
                       hints: ArrayList<String>,
                       onHintClicked: (String) -> Unit,
                       context: Context,
                       bodyProps: BodyProps?,
                       configurationBodyProps: BodyProps?,
                       assistantSettingProps: AssistantSettingsProps?,
                       configuration: CustomAssistantConfigurationResponse?

    ): Pair<MessagesRecyclerViewAdapter, HintsRecyclerViewAdapter>{
        initializeBorders(bodyBorderTopView, bodyBorderBottomView, bodyProps, configurationBodyProps, context)
        initializeBodyLayout(bodyLayout, bodyProps, configurationBodyProps, context, configuration, assistantSettingProps)
        return (initializeRecyclerViews(messagesRecycler, hintsRecycler, messages, hints, onHintClicked, bodyProps, configurationBodyProps, context))
    }

    private fun initializeBorders(bodyBorderTopView: View, bodyBorderBottomView: View, bodyProps: BodyProps?, configurationBodyProps: BodyProps?, context: Context){
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

    private fun initializeBodyLayout(bodyLayout: LinearLayout, bodyProps: BodyProps?, configurationBodyProps: BodyProps?, context: Context, configuration: CustomAssistantConfigurationResponse?, assistantSettingProps: AssistantSettingsProps?){
        val bodyContainerLayoutStyle = GradientDrawable()
        if(!(bodyProps?.backgroundColor ?: configurationBodyProps?.backgroundColor).isNullOrEmpty()){
            bodyContainerLayoutStyle.setColor(Color.parseColor(bodyProps?.backgroundColor ?: configurationBodyProps?.backgroundColor))
        }
        else if ((assistantSettingProps?.backgroundColor ?: configuration?.styles?.assistant?.backgroundColor).isNullOrEmpty())
        {
            bodyContainerLayoutStyle.setColor(Color.parseColor(context.getString(R.string.light_gray)))
        }
        bodyLayout.background = bodyContainerLayoutStyle
        bodyLayout.setPadding(
            bodyProps?.paddingLeft ?: configurationBodyProps?.paddingLeft ?: 20,
            bodyProps?.paddingTop ?: configurationBodyProps?.paddingTop ?: 0,
            bodyProps?.paddingRight ?: configurationBodyProps?.paddingRight ?: 20,
            bodyProps?.paddingBottom ?: configurationBodyProps?.paddingBottom ?: 0
        )
    }

    private fun initializeRecyclerViews(messagesRecycler: RecyclerView, hintsRecycler: RecyclerView, messages: ArrayList<Message>, hints: ArrayList<String>, onHintClicked: (String) -> Unit, bodyProps: BodyProps?, configurationBodyProps: BodyProps?, context: Context): Pair<MessagesRecyclerViewAdapter, HintsRecyclerViewAdapter>{
       val hintsRecyclerViewAdapter = HintsRecyclerViewAdapter(hints, bodyProps, configurationBodyProps, onHintClicked, context)
        hintsRecycler.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        hintsRecycler.adapter = hintsRecyclerViewAdapter

        val messagesRecyclerViewAdapter = MessagesRecyclerViewAdapter(messages, bodyProps, configurationBodyProps, context)
        messagesRecycler.layoutManager = LinearLayoutManager(context)
        messagesRecycler.adapter = messagesRecyclerViewAdapter
        return Pair(messagesRecyclerViewAdapter, hintsRecyclerViewAdapter)
    }
}