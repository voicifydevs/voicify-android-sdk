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
import com.voicify.voicify_assistant_sdk.assistantDrawerUITypes.BodyProps

//TODO: PASS IN MESSAGES PROPS HERE
internal class MessagesRecyclerViewAdapter(private var messagesList: List<Message>, private var bodyProps: BodyProps?) :
    RecyclerView.Adapter<MessagesRecyclerViewAdapter.MyViewHolder>() {
    internal inner class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var messageTextView: TextView = view.findViewById(R.id.messageTextView)
        var messagesContainerLinearLayout: LinearLayout = view.findViewById(R.id.messagesContainerLinearLayout)
        var messagesSpace: Space = view.findViewById(R.id.messagesSpace)
        var messagesAvatar: ImageView = view.findViewById(R.id.messagesAssistantAvatarImageView)
        val assistantAvatarBackgroundLayout = view.findViewById<LinearLayout>(R.id.assistantAvatarBackgroundLayout)
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
        val avatarBackground = holder.assistantAvatarBackgroundLayout
        if(message.origin == "Sent")
        {
            avatarBackground.visibility = View.GONE
            messagesAvatar.visibility = View.GONE
            val messagesTextViewStyle = GradientDrawable()
            messagesTextViewStyle.shape = GradientDrawable.RECTANGLE
            messagesTextViewStyle.setStroke(bodyProps?.messageSentBorderWidth ?: 0, Color.parseColor(bodyProps?.messageSentBorderColor ?: "#00ffffff"))
            val cornerRadaii = floatArrayOf(bodyProps?.messageSentBorderTopLeftRadius ?: 18f,bodyProps?.messageSentBorderTopLeftRadius ?: 18f,bodyProps?.messageSentBorderTopRightRadius ?: 0f,bodyProps?.messageSentBorderTopRightRadius ?: 0f, bodyProps?.messageSentBorderBottomLeftRadius ?: 18f,bodyProps?.messageSentBorderBottomLeftRadius ?: 18f,bodyProps?.messageSentBorderBottomRightRadius ?: 18f, bodyProps?.messageSentBorderBottomRightRadius ?: 18f)
            messagesTextViewStyle.cornerRadii = cornerRadaii
            messagesTextViewStyle.setColor(Color.parseColor(bodyProps?.messageSentBackgroundColor ?: "#80000000"))
            messageTextView.background = messagesTextViewStyle
            messagesSpace.visibility = View.VISIBLE
            val messagesContainerLayoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            messagesContainerLayoutParams.setMargins(0, 60, 0 , 0)
            messagesContainerLayoutParams.marginStart = 150
            messagesContainerLinearLayout.layoutParams = messagesContainerLayoutParams
            messageTextView.setPadding(20,20,20,20)
            messageTextView.setTextColor(Color.parseColor(bodyProps?.messageSentTextColor ?: "#ffffff"))
            messageTextView.textSize = bodyProps?.messageSentFontSize ?: 14f
        }
        else
        {
            val avatarBackgroundStyle = GradientDrawable()
            avatarBackgroundStyle.cornerRadius = bodyProps?.assistantImageBorderRadius ?: 38f
            avatarBackgroundStyle.setStroke(bodyProps?.assistantImageBorderWidth ?: 4, Color.parseColor(bodyProps?.assistantImageBorderColor ?: "#CBCCD2"))
            avatarBackgroundStyle.setColor(Color.parseColor("#ffffff"))
            avatarBackground.background = avatarBackgroundStyle
            avatarBackground.setPadding(12,12,12,12)
            avatarBackground.visibility = View.VISIBLE
            messagesAvatar.visibility = View.VISIBLE
            if(!bodyProps?.assistantImage.isNullOrEmpty())
            {
                Picasso.get().load("https://voicify-prod-files.s3.amazonaws.com/99a803b7-5b37-426c-a02e-63c8215c71eb/eb7d2538-a3dc-4304-b58c-06fdb34e9432/Mark-Color-3-.png").into(messagesAvatar)
            }
            val messagesTextViewStyle = GradientDrawable()
            messagesTextViewStyle.shape = GradientDrawable.RECTANGLE
            messagesTextViewStyle.setStroke(bodyProps?.messageReceivedBorderWidth ?: 4, Color.parseColor(bodyProps?.messageReceivedBorderColor ?: "#CBCCD2"))
            val cornerRadaii = floatArrayOf(bodyProps?.messageReceivedBorderTopLeftRadius ?: 0f, bodyProps?.messageReceivedBorderTopLeftRadius ?: 0f,bodyProps?.messageReceivedBorderTopRightRadius ?: 18f,bodyProps?.messageReceivedBorderTopRightRadius ?: 18f, bodyProps?.messageReceivedBorderBottomLeftRadius ?: 18f,bodyProps?.messageReceivedBorderBottomLeftRadius ?: 18f,bodyProps?.messageReceivedBorderBottomRightRadius ?: 18f,bodyProps?.messageReceivedBorderBottomRightRadius ?: 18f)
            messagesTextViewStyle.cornerRadii = cornerRadaii
            messagesTextViewStyle.setColor(Color.parseColor(bodyProps?.messageReceivedBackgroundColor ?: "#0d000000"))
            messageTextView.background = messagesTextViewStyle
            val messagesContainerLayoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            messagesContainerLayoutParams.setMargins(0, 60, 0 , 0)
            messagesContainerLayoutParams.marginEnd = 150
            messagesSpace.visibility = View.GONE
            messagesContainerLinearLayout.layoutParams = messagesContainerLayoutParams
            val messagesTextViewLayoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            messagesTextViewLayoutParams.setMargins(20,40,0,0)
            messageTextView.layoutParams = messagesTextViewLayoutParams
            messageTextView.setPadding(20,20,20,20)
            messageTextView.setTextColor(Color.parseColor(bodyProps?.messageReceivedTextColor ?: "#000000"))
            messageTextView.textSize = bodyProps?.messageReceivedFontSize ?: 14f
        }
        messageTextView.text = message.message
    }
    override fun getItemCount(): Int {
        return messagesList.size
    }

}