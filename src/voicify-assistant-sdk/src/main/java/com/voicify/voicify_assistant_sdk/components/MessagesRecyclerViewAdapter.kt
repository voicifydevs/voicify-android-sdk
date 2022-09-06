package com.voicify.voicify_assistant_sdk.components

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.ShapeDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Space
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.core.view.marginBottom
import androidx.recyclerview.widget.RecyclerView
import com.voicify.voicify_assistant_sdk.assistantDrawerUITypes.Message
import com.voicify.voicify_assistant_sdk.R

//TODO: PASS IN MESSAGES PROPS HERE
internal class MessagesRecyclerViewAdapter(private var messagesList: List<Message>) :
    RecyclerView.Adapter<MessagesRecyclerViewAdapter.MyViewHolder>() {
    internal inner class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var messageTextView: TextView = view.findViewById(R.id.messageTextView)
        var messagesContainerLinearLayout: LinearLayout = view.findViewById(R.id.messagesContainerLinearLayout)
        var messagesSpace: Space = view.findViewById(R.id.messagesSpace)
    }
    @NonNull
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.messages_recyclerview_row, parent, false)
        return MyViewHolder(itemView)
    }
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val message = messagesList[position]
        val messageTextView = holder.messageTextView
        val messagesContainerLinearLayout = holder.messagesContainerLinearLayout
        val messagesSpace = holder.messagesSpace
        if(message.origin == "Sent")
        {
            val messagesTextViewStyle = GradientDrawable()
            messagesTextViewStyle.shape = GradientDrawable.RECTANGLE
            val cornerRadaii = floatArrayOf(18f,18f,0f,0f,18f,18f,18f,18f)
            messagesTextViewStyle.cornerRadii = cornerRadaii
            messagesTextViewStyle.setColor(Color.parseColor("#80000000"))
            messageTextView.background = messagesTextViewStyle
            messagesSpace.visibility = View.VISIBLE
            val messagesContainerLayoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            messagesContainerLayoutParams.setMargins(0, 24, 0 , 0)
            messagesContainerLayoutParams.marginStart = 150
            messagesContainerLinearLayout.layoutParams = messagesContainerLayoutParams
            messageTextView.setPadding(10,10,10,10)
            messageTextView.setTextColor(Color.WHITE)
            messageTextView.textSize = 14f
        }
        else
        {
            val messagesTextViewStyle = GradientDrawable()
            messagesTextViewStyle.shape = GradientDrawable.RECTANGLE
            val cornerRadaii = floatArrayOf(0f,0f,18f,18f,18f,18f,18f,18f)
            messagesTextViewStyle.cornerRadii = cornerRadaii
            messagesTextViewStyle.setColor(Color.parseColor("#0d000000"))
            messageTextView.background = messagesTextViewStyle
            val messagesContainerLayoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            messagesContainerLayoutParams.setMargins(0, 24, 0 , 0)
            messagesContainerLayoutParams.marginEnd = 150
            messagesSpace.visibility = View.GONE
            messagesContainerLinearLayout.layoutParams = messagesContainerLayoutParams
            messageTextView.setPadding(10,10,10,10)
            messageTextView.setTextColor(Color.BLACK)
            messageTextView.textSize = 14f
        }
        messageTextView.text = message.message
    }
    override fun getItemCount(): Int {
        return messagesList.size
    }

}