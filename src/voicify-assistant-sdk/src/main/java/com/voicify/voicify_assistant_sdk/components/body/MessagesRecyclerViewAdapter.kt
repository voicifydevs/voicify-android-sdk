package com.voicify.voicify_assistant_sdk.components.body

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import androidx.browser.customtabs.CustomTabsIntent
import androidx.recyclerview.widget.RecyclerView
import com.voicify.voicify_assistant_sdk.R
import com.voicify.voicify_assistant_sdk.assistantDrawerUITypes.BodyProps
import com.voicify.voicify_assistant_sdk.assistantDrawerUITypes.HelperMethods
import com.voicify.voicify_assistant_sdk.assistantDrawerUITypes.Message

//https://noties.io/Markwon/docs/v3/ext-tasklist/#task-list-mutation
internal class MessagesRecyclerViewAdapter(
    private var messagesList: List<Message>,
    private var bodyProps: BodyProps?,
    private var configurationBodyProps: BodyProps?,
    private var context: Context
    ) :
    RecyclerView.Adapter<MessagesRecyclerViewAdapter.MyViewHolder>() {
    internal inner class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var messageTextView: TextView = view.findViewById(R.id.messageTextView)
        var messagesContainerLinearLayout: LinearLayout = view.findViewById(R.id.messagesContainerLinearLayout)
        var messagesSpace: Space = view.findViewById(R.id.messagesSpace)
        var messagesAvatar: ImageView = view.findViewById(R.id.messagesAssistantAvatarImageView)
        val assistantAvatarBackgroundLayout: LinearLayout = view.findViewById(R.id.assistantAvatarBackgroundLayout)
    }

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
        val scale = context.resources.displayMetrics.density
        var openLink: (String) -> Unit =  {link ->
            val builder = CustomTabsIntent.Builder()
            builder.setShareState(CustomTabsIntent.SHARE_STATE_ON)
            builder.setInstantAppsEnabled(true)
            val customBuilder = builder.build()
            customBuilder.intent.setPackage(context.getString(R.string.chrome_package_name))
            customBuilder.launchUrl(context, Uri.parse(link))
        }
        val markwon = HelperMethods.buildMarkwon(context, openLink)
        //User sent message to assistant
        if(message.origin == context.getString(R.string.sent))
        {
            avatarBackground.visibility = View.GONE
            messagesAvatar.visibility = View.GONE

            val messagesTextViewStyle = GradientDrawable()
            messagesTextViewStyle.shape = GradientDrawable.RECTANGLE
            messagesTextViewStyle.setStroke(
                bodyProps?.messageSentBorderWidth ?: configurationBodyProps?.messageSentBorderWidth ?: 0,
                Color.parseColor(bodyProps?.messageSentBorderColor ?: configurationBodyProps?.messageSentBorderColor ?: context.getString(R.string.transparent))
            )
            val cornerRadaii = floatArrayOf(
                bodyProps?.messageSentBorderTopLeftRadius ?: configurationBodyProps?.messageSentBorderTopLeftRadius ?: 25f,
                bodyProps?.messageSentBorderTopLeftRadius ?: configurationBodyProps?.messageSentBorderTopLeftRadius ?: 25f,
                bodyProps?.messageSentBorderTopRightRadius ?: configurationBodyProps?.messageSentBorderTopRightRadius ?: 0f,
                bodyProps?.messageSentBorderTopRightRadius ?: configurationBodyProps?.messageSentBorderTopRightRadius ?: 0f,
                bodyProps?.messageSentBorderBottomLeftRadius ?: configurationBodyProps?.messageSentBorderBottomLeftRadius ?: 25f,
                bodyProps?.messageSentBorderBottomLeftRadius ?: configurationBodyProps?.messageSentBorderBottomLeftRadius ?: 25f,
                bodyProps?.messageSentBorderBottomRightRadius ?: configurationBodyProps?.messageSentBorderBottomRightRadius ?: 25f,
                bodyProps?.messageSentBorderBottomRightRadius ?: configurationBodyProps?.messageSentBorderBottomRightRadius ?: 25f
            )

            messagesTextViewStyle.cornerRadii = cornerRadaii
            messagesTextViewStyle.setColor(Color.parseColor(bodyProps?.messageSentBackgroundColor ?: configurationBodyProps?.messageSentBackgroundColor ?: context.getString(R.string.black_50_percent)))
            messageTextView.background = messagesTextViewStyle
            messagesSpace.visibility = View.VISIBLE

            val messagesContainerLayoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            messagesContainerLayoutParams.setMargins(0, 60, 0 , 0)
            messagesContainerLayoutParams.marginStart = 150
            messagesContainerLinearLayout.layoutParams = messagesContainerLayoutParams
            messageTextView.setPadding(20,20,20,20)
            messageTextView.setTextColor(Color.parseColor(bodyProps?.messageSentTextColor ?: configurationBodyProps?.messageSentTextColor ?: context.getString(R.string.white)))
            messageTextView.textSize = bodyProps?.messageSentFontSize ?: 14f
            messageTextView.typeface = Typeface.create(
                bodyProps?.messageSentFontFamily ?: configurationBodyProps?.messageSentFontFamily ?: context.getString(R.string.default_font),
                Typeface.NORMAL
            )
        }
        //Assistant Responded
        else
        {
            val avatarBackgroundStyle = GradientDrawable()
            avatarBackgroundStyle.cornerRadius = bodyProps?.assistantImageBorderRadius ?: configurationBodyProps?.assistantImageBorderRadius ?: 60f
            avatarBackgroundStyle.setStroke(
                bodyProps?.assistantImageBorderWidth ?: configurationBodyProps?.assistantImageBorderWidth ?: 0,
                Color.parseColor(bodyProps?.assistantImageBorderColor ?: configurationBodyProps?.assistantImageBorderColor ?: context.getString(R.string.transparent))
            )

            avatarBackgroundStyle.setColor(Color.parseColor(bodyProps?.assistantImageBackgroundColor ?: configurationBodyProps?.assistantImageBackgroundColor ?: context.getString(R.string.transparent)))
            avatarBackground.background = avatarBackgroundStyle
            avatarBackground.setPadding(
                bodyProps?.assistantImagePadding ?: configurationBodyProps?.assistantImagePadding ?: 12,
                bodyProps?.assistantImagePadding ?: configurationBodyProps?.assistantImagePadding ?:12,
                bodyProps?.assistantImagePadding ?: configurationBodyProps?.assistantImagePadding ?:12,
                bodyProps?.assistantImagePadding ?: configurationBodyProps?.assistantImagePadding ?:12
            )
            avatarBackground.visibility = View.VISIBLE

            messagesAvatar.visibility = View.VISIBLE
            HelperMethods.loadImageFromUrl(
                url = bodyProps?.assistantImage ?: configurationBodyProps?.assistantImage ?: context.getString(R.string.body_avatar_image),
                view = messagesAvatar,
                imageColor = bodyProps?.assistantImageColor ?: configurationBodyProps?.assistantImageColor,
                resources = context.resources,
                isRounded = true,
                borderRadius = bodyProps?.assistantImageBorderRadius ?: configurationBodyProps?.assistantImageBorderRadius ?: 200f
            )
            val messagesAvatarLayoutParams = LinearLayout.LayoutParams(
                bodyProps?.assistantImageWidth ?: configurationBodyProps?.assistantImageWidth ?: HelperMethods.getPixelsFromDp(28, scale),
                bodyProps?.assistantImageHeight ?: configurationBodyProps?.assistantImageWidth ?: HelperMethods.getPixelsFromDp(28, scale)
            )
            if(!bodyProps?.assistantImageBackgroundColor.isNullOrEmpty() || !configurationBodyProps?.assistantImageBackgroundColor.isNullOrEmpty())
            {
                messagesAvatarLayoutParams.width = messagesAvatarLayoutParams.width - HelperMethods.getPixelsFromDp(6, scale)
                messagesAvatarLayoutParams.height = messagesAvatarLayoutParams.height - HelperMethods.getPixelsFromDp(6, scale)
            }
            messagesAvatar.layoutParams = messagesAvatarLayoutParams

            val messagesTextViewStyle = GradientDrawable()
            messagesTextViewStyle.shape = GradientDrawable.RECTANGLE
            messagesTextViewStyle.setStroke(
                bodyProps?.messageReceivedBorderWidth ?: configurationBodyProps?.messageReceivedBorderWidth ?: 4,
                Color.parseColor(bodyProps?.messageReceivedBorderColor ?: configurationBodyProps?.messageReceivedBorderColor ?: context.getString(R.string.gray))
            )
            val cornerRadaii = floatArrayOf(
                bodyProps?.messageReceivedBorderTopLeftRadius ?: configurationBodyProps?.messageReceivedBorderTopLeftRadius ?: 0f,
                bodyProps?.messageReceivedBorderTopLeftRadius ?: configurationBodyProps?.messageReceivedBorderTopLeftRadius ?: 0f,
                bodyProps?.messageReceivedBorderTopRightRadius ?: configurationBodyProps?.messageReceivedBorderTopRightRadius ?: 25f,
                bodyProps?.messageReceivedBorderTopRightRadius ?: configurationBodyProps?.messageReceivedBorderTopRightRadius ?: 25f,
                bodyProps?.messageReceivedBorderBottomLeftRadius ?: configurationBodyProps?.messageReceivedBorderBottomLeftRadius ?: 25f,
                bodyProps?.messageReceivedBorderBottomLeftRadius ?: configurationBodyProps?.messageReceivedBorderBottomLeftRadius ?: 25f,
                bodyProps?.messageReceivedBorderBottomRightRadius ?: configurationBodyProps?.messageReceivedBorderBottomRightRadius ?: 25f,
                bodyProps?.messageReceivedBorderBottomRightRadius ?: configurationBodyProps?.messageReceivedBorderBottomRightRadius ?: 25f
            )
            messagesTextViewStyle.cornerRadii = cornerRadaii
            messagesTextViewStyle.setColor(Color.parseColor(bodyProps?.messageReceivedBackgroundColor ?: configurationBodyProps?.messageReceivedBackgroundColor ?: context.getString(R.string.black_5_percent)))
            messageTextView.background = messagesTextViewStyle

            val messagesContainerLayoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            messagesContainerLayoutParams.setMargins(0, 60, 0 , 0)
            messagesContainerLayoutParams.marginEnd = 150
            messagesSpace.visibility = View.GONE
            messagesContainerLinearLayout.layoutParams = messagesContainerLayoutParams

            val messagesTextViewLayoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            messagesTextViewLayoutParams.setMargins(20,60,0,0)
            messageTextView.layoutParams = messagesTextViewLayoutParams
            messageTextView.setPadding(20,20,20,20)
            messageTextView.setTextColor(Color.parseColor(bodyProps?.messageReceivedTextColor ?: configurationBodyProps?.messageReceivedTextColor ?: context.getString(R.string.black)))
            messageTextView.typeface = Typeface.create(
                bodyProps?.messageReceivedFontFamily ?: configurationBodyProps?.messageReceivedFontFamily ?: context.getString(R.string.default_font),
                Typeface.NORMAL
            )
            messageTextView.textSize = bodyProps?.messageReceivedFontSize ?: configurationBodyProps?.messageReceivedFontSize ?: 14f
        }
        markwon.setMarkdown(messageTextView, message.message)
    }
    override fun getItemCount(): Int {
        return messagesList.size
    }
}