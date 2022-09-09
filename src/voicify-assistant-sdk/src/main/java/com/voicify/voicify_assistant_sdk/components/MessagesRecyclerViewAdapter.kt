package com.voicify.voicify_assistant_sdk.components

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import com.voicify.voicify_assistant_sdk.assistantDrawerUITypes.Message
import com.voicify.voicify_assistant_sdk.R

//TODO: PASS IN MESSAGES PROPS HERE
internal class MessagesRecyclerViewAdapter(private var messagesList: List<Message>) :
    RecyclerView.Adapter<MessagesRecyclerViewAdapter.MyViewHolder>() {
    internal inner class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var messageTextView: TextView = view.findViewById(R.id.messageTextView)
        var messagesContainerLinearLayout: LinearLayout = view.findViewById(R.id.messagesContainerLinearLayout)
        var messagesSpace: Space = view.findViewById(R.id.messagesSpace)
        var messagesAvatar: ImageView = view.findViewById(R.id.messagesAssistantAvatarImageView)
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
        val messagesAvatar = holder.messagesAvatar
        if(message.origin == "Sent")
        {
            messagesAvatar.visibility = View.GONE
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
            messagesAvatar.visibility = View.VISIBLE
            Picasso.get().load("https://voicify-prod-files.s3.amazonaws.com/99a803b7-5b37-426c-a02e-63c8215c71eb/eb7d2538-a3dc-4304-b58c-06fdb34e9432/Mark-Color-3-.png").into(messagesAvatar)
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