package com.voicify.voicify_assistant_sdk.assistant

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.VectorDrawable
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.Gson
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import com.voicify.voicify_assistant_sdk.R
import com.voicify.voicify_assistant_sdk.assistantDrawerUITypes.*
import com.voicify.voicify_assistant_sdk.components.HintsRecyclerViewAdapter
import com.voicify.voicify_assistant_sdk.components.MessagesRecyclerViewAdapter
import kotlinx.android.synthetic.main.fragment_assistant_drawer_u_i.*
import java.lang.reflect.Field
import kotlin.collections.ArrayList
import kotlin.math.roundToInt

private const val SETTINGS = "assistantSettings"
private const val HEADER = "header"
private const val BODY = "body"
private const val TOOLBAR = "toolBar"

/**
 * A simple [Fragment] subclass.
 * Use the [AssistantDrawerUI.newInstance] factory method to
 * create an instance of this fragment.
 */
class AssistantDrawerUI : BottomSheetDialogFragment() {
    // TODO: Rename and change types of parameters
    private var assistantSettingProps: AssistantSettingsProps? = null
    private var headerProps: HeaderProps? = null
    private var bodyProps: BodyProps? = null
    private var toolBarProps: ToolBarProps? = null
    private var assistantIsListening: Boolean = false
    private var isUsingSpeech: Boolean = false
    private var isDrawer: Boolean = true
    private var voicifySTT: VoicifySTTProvider? = null
    private var voicifyTTS: VoicifyTTSProvider? = null
    private var scale: Float = 0f
    private var canRun = true
    private var hintsRecyclerViewAdapter: HintsRecyclerViewAdapter? = null
    private var messagesRecyclerViewAdapter: MessagesRecyclerViewAdapter? = null
    private var animation: AnimatorSet? = null
    private var speechFullResult : String? = null
    private var bottomSheetBehavior : BottomSheetBehavior<View>? = null
    private var isKeyboardActive = false
    private var onEffectCallback: ((effect: String, data: Any) -> Unit)? = null
    private var onAssistantDismissCallback: (() -> Unit)? = null
    private var sessionAttributes: Map<String, Any>? = emptyMap()
    private var userAttributes: Map<String, Any> = emptyMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            assistantSettingProps = it.getSerializable(SETTINGS) as AssistantSettingsProps?
            headerProps = it.getSerializable(HEADER) as HeaderProps?
            bodyProps = it.getSerializable(BODY) as BodyProps?
            toolBarProps = it.getSerializable(TOOLBAR) as ToolBarProps?
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("w", 1)
    }

    @SuppressLint("ResourceType")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val window = inflater.inflate(R.layout.fragment_assistant_drawer_u_i, container, false)
        window!!.viewTreeObserver.addOnGlobalLayoutListener {
            val r = Rect()
            //r will be populated with the coordinates of your view that area still visible.
            window.getWindowVisibleDisplayFrame(r)
            val heightDiff = window.rootView.height - (r.bottom - r.top)
            if (heightDiff > 500) { // if more than 100 pixels, its probably a keyboard...
                if(!isKeyboardActive)
                {
                    val layoutParams1 = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, getPixelsFromDp(320))
                    layoutParams1.weight = 0f
                    bodyContainerLayout.layoutParams = layoutParams1
                    messagesRecyclerViewAdapter?.notifyDataSetChanged()
                    messagesRecyclerView.smoothScrollToPosition(messagesRecyclerViewAdapter?.itemCount as Int);
                    isKeyboardActive = true
                }

            }
            else
            {
                if(isKeyboardActive)
                {
                    val layoutParams1 = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, getPixelsFromDp(0))
                    layoutParams1.weight = 1f
                    bodyContainerLayout.layoutParams = layoutParams1
                    isKeyboardActive = false
                }
            }
        }
        scale = requireContext().resources.displayMetrics.density
        //Linear Layouts
        val containerLayout = window.findViewById<LinearLayout>(R.id.container)
        if(!assistantSettingProps?.backgroundColor.isNullOrEmpty())
        {
            val splitColors = assistantSettingProps?.backgroundColor?.split(",")
            if (splitColors!!.size > 1)
            {
                var colors = intArrayOf()
                splitColors.forEach {
                    colors = colors.plus(Color.parseColor(it))
                }
                val gradientDrawable = GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    colors)
                containerLayout.background = gradientDrawable
            }
            else
            {
                containerLayout.setBackgroundColor(Color.parseColor(assistantSettingProps?.backgroundColor))
            }
        }
        val drawerLayout = window.findViewById<LinearLayout>(R.id.drawerLayout)
        val bodyContainerLayout = window.findViewById<LinearLayout>(R.id.bodyContainerLayout)
        val headerLayout = window.findViewById<LinearLayout>(R.id.headerLayout)
        val toolBarLayout = window.findViewById<LinearLayout>(R.id.toolbarLayout)
        val sendTextLayout = window.findViewById<LinearLayout>(R.id.sendTextLayout)
        val speakingAnimationLayout = window.findViewById<LinearLayout>(R.id.speakingAnimation)
        val assistantAvatarBackground = window.findViewById<LinearLayout>(R.id.assistantAvatarBackgroundLayout)
        val drawerFooterLayout = window.findViewById<LinearLayout>(R.id.drawerFooterLayout)

        // Views
        val bodyBorderTopView = window.findViewById<View>(R.id.bodyBorderTopView)
        val bodyBorderBottomView = window.findViewById<View>(R.id.bodyBorderBottomView)
        val speakingAnimationBar1 = window.findViewById<View>(R.id.speakingAnimationBar1)
        val speakingAnimationBar2 = window.findViewById<View>(R.id.speakingAnimationBar2)
        val speakingAnimationBar3 = window.findViewById<View>(R.id.speakingAnimationBar3)
        val speakingAnimationBar4 = window.findViewById<View>(R.id.speakingAnimationBar4)
        val speakingAnimationBar5 = window.findViewById<View>(R.id.speakingAnimationBar5)
        val speakingAnimationBar6 = window.findViewById<View>(R.id.speakingAnimationBar6)
        val speakingAnimationBar7 = window.findViewById<View>(R.id.speakingAnimationBar7)
        val speakingAnimationBar8 = window.findViewById<View>(R.id.speakingAnimationBar8)

        // Recycler Views
        val messagesRecyclerView = window.findViewById<RecyclerView>(R.id.messagesRecyclerView)
        val hintsRecyclerView = window.findViewById<RecyclerView>(R.id.hintsRecyclerView)
        val hintsList = ArrayList<String>()
        val messagesList = ArrayList<Message>()
        messagesRecyclerViewAdapter = MessagesRecyclerViewAdapter(messagesList, bodyProps, requireContext())

        voicifyTTS = VoicifyTTSProvider(VoicifyTextToSpeechSettings(
            appId = assistantSettingProps!!.appId,
            appKey = assistantSettingProps!!.appKey,
            voice = assistantSettingProps?.textToSpeechVoice ?: "",
            serverRootUrl = assistantSettingProps!!.serverRootUrl,
            provider = assistantSettingProps?.textToSpeechProvider ?: "Google"))
        voicifySTT = VoicifySTTProvider(requireContext(), requireActivity())
        val assistant = VoicifyAssistant(voicifySTT, voicifyTTS, VoicifyAssistantSettings(
            appId = assistantSettingProps!!.appId,
            appKey = assistantSettingProps!!.appKey,
            serverRootUrl = assistantSettingProps!!.serverRootUrl,
            locale = assistantSettingProps!!.locale,
            channel = assistantSettingProps!!.channel,
            device = assistantSettingProps!!.device,
            noTracking = assistantSettingProps?.noTracking ?: false,
            autoRunConversation = assistantSettingProps!!.autoRunConversation,
            initializeWithWelcomeMessage = assistantSettingProps!!.initializeWithWelcomeMessage,
            initializeWithText = assistantSettingProps!!.initializeWithText,
            useVoiceInput = assistantSettingProps!!.useVoiceInput,
            useDraftContent = assistantSettingProps!!.useDraftContent,
            useOutputSpeech = assistantSettingProps!!.useOutputSpeech))

        val onHintClicked: (String) -> Unit = {  hint ->
            messagesList.add(Message(hint, "Sent"))
            clearAnimationValues()
            messagesRecyclerViewAdapter?.notifyDataSetChanged()
            messagesRecyclerView.smoothScrollToPosition(messagesRecyclerViewAdapter?.itemCount as Int);
            hideKeyboard()
            hintsList.clear()
            cancelSpeech()
            voicifyTTS?.stop()
            hintsRecyclerViewAdapter?.notifyDataSetChanged()
            assistant.makeTextRequest(hint ,null, "Text")
        }
        hintsRecyclerViewAdapter = HintsRecyclerViewAdapter(hintsList, bodyProps, onHintClicked)
        messagesRecyclerView.layoutManager = LinearLayoutManager(context)
        messagesRecyclerView.adapter = messagesRecyclerViewAdapter
        hintsRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        hintsRecyclerView.adapter = hintsRecyclerViewAdapter
        //Image Views
        val micImageView = window.findViewById<ImageView>(R.id.micImageView)
        val closeAssistantImageView = window.findViewById<ImageView>(R.id.closeAssistantImageView)
        val sendMessageImageView = window.findViewById<ImageView>(R.id.sendMessageButtonImageView)
        val assistantAvatarImageView = window.findViewById<ImageView>(R.id.assistantAvatarImageView)
        val dashedLineImageView = window.findViewById<ImageView>(R.id.dashedLineImageView)
        val speakTextView = window.findViewById<TextView>(R.id.speakTextView)
        if(assistantSettingProps?.useVoiceInput == false)
        {
            micImageView.visibility = View.GONE
            speakTextView.visibility = View.GONE
        }
        else{
            loadImageFromUrl(if(assistantSettingProps?.initializeWithText == false) toolBarProps?.micActiveImage ?: "https://voicify-prod-files.s3.amazonaws.com/99a803b7-5b37-426c-a02e-63c8215c71eb/daca643f-6730-4af5-8817-8d9d0d9db0b5/mic-image.png"
            else toolBarProps?.micInactiveImage ?: "https://voicify-prod-files.s3.amazonaws.com/99a803b7-5b37-426c-a02e-63c8215c71eb/3f10b6d7-eb71-4427-adbc-aadacbe8940e/mic-image-1-.png", micImageView,
                if(assistantSettingProps?.initializeWithText == false) toolBarProps?.micActiveColor else toolBarProps?.micInactiveColor)
        }
        loadImageFromUrl(headerProps?.closeAssistantButtonImage ?: "https://voicify-prod-files.s3.amazonaws.com/99a803b7-5b37-426c-a02e-63c8215c71eb/a6de04bb-e572-4a55-8cd9-1a7628285829/delete-2.png", closeAssistantImageView, headerProps?.closeAssistantColor,)
        loadImageFromUrl(if(assistantSettingProps?.initializeWithText == false && assistantSettingProps?.useVoiceInput == true) toolBarProps?.sendInactiveImage ?: "https://voicify-prod-files.s3.amazonaws.com/99a803b7-5b37-426c-a02e-63c8215c71eb/0c5aa61c-7d6c-4272-abd2-75d9f5771214/Send-2-.png"
        else toolBarProps?.sendActiveImage ?: "https://voicify-prod-files.s3.amazonaws.com/99a803b7-5b37-426c-a02e-63c8215c71eb/7a39bc6f-eef5-4185-bcf8-2a645aff53b2/Send-3-.png", sendMessageImageView,
            if(assistantSettingProps?.initializeWithText == false && assistantSettingProps?.useVoiceInput == true) toolBarProps?.sendInactiveColor else toolBarProps?.sendActiveColor)
        loadImageFromUrl(headerProps?.assistantImage ?: "https://voicify-prod-files.s3.amazonaws.com/99a803b7-5b37-426c-a02e-63c8215c71eb/eb7d2538-a3dc-4304-b58c-06fdb34e9432/Mark-Color-3-.png", assistantAvatarImageView, headerProps?.assistantImageColor)

        //Text Views
        val assistantStateTextView = window.findViewById<TextView>(R.id.assistantStateTextView)
        val spokenTextView = window.findViewById<TextView>(R.id.spokenTextView)
        val drawerWelcomeTextView = window.findViewById<TextView>(R.id.drawerWelcomeTextView)
        val typeTextView = window.findViewById<TextView>(R.id.typeTextView)
        val inputTextMessageEditTextView = window.findViewById<EditText>(R.id.inputTextMessage)
        val assistantNameTextView = window.findViewById<TextView>(R.id.assistantNameTextView)
        inputTextMessageEditTextView.setCursorDrawableColor(Color.parseColor(toolBarProps?.textInputCursorColor ?: "#000000"))
        inputTextMessageEditTextView.setTextColor(Color.parseColor(toolBarProps?.textInputTextColor ?: "#000000"))
        val colorStateList = ColorStateList.valueOf(Color.parseColor(if(isUsingSpeech) {toolBarProps?.textInputLineColor ?: "#000000"} else {toolBarProps?.textInputActiveLineColor ?: "#000000"}))
        ViewCompat.setBackgroundTintList(inputTextMessageEditTextView,colorStateList)

        if(!toolBarProps?.backgroundColor.isNullOrEmpty()){
            drawerLayout.setBackgroundColor(Color.parseColor(toolBarProps?.backgroundColor));
        }
        else if (assistantSettingProps?.backgroundColor.isNullOrEmpty())
        {
            drawerLayout.setBackgroundColor(Color.parseColor("#ffffff"));
        }
        drawerLayout.setPadding(toolBarProps?.paddingLeft ?: getPixelsFromDp(16),toolBarProps?.paddingTop ?: getPixelsFromDp(16),toolBarProps?.paddingRight ?: getPixelsFromDp(16),toolBarProps?.paddingBottom ?: getPixelsFromDp(16))
        if(assistantSettingProps?.initializeWithWelcomeMessage == true)
        {
            drawerLayout.visibility = View.GONE
        }
        //set View styles
        bodyBorderTopView.setBackgroundColor(Color.parseColor(bodyProps?.borderTopColor ?: "#CBCCD2"))
        val bodyBorderTopViewLayoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, bodyProps?.borderTopWidth ?: 4)
        bodyBorderTopView.layoutParams = bodyBorderTopViewLayoutParams
        bodyBorderBottomView.setBackgroundColor(Color.parseColor(bodyProps?.borderBottomColor ?: "#CBCCD2"))
        val bodyBorderBottomViewLayoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, bodyProps?.borderBottomWidth ?: 4)
        bodyBorderBottomView.layoutParams = bodyBorderBottomViewLayoutParams
        if(!toolBarProps?.equalizerColor.isNullOrEmpty())
        {
            val splitColors = toolBarProps?.equalizerColor?.split(",")
            if (splitColors!!.size > 1)
            {
                var colors = intArrayOf()
                splitColors.forEach {
                    colors = colors.plus(Color.parseColor(it))
                }
                val gradientDrawable = GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    colors)
                speakingAnimationBar1.background = gradientDrawable
                speakingAnimationBar2.background = gradientDrawable
                speakingAnimationBar3.background = gradientDrawable
                speakingAnimationBar4.background = gradientDrawable
                speakingAnimationBar5.background = gradientDrawable
                speakingAnimationBar6.background = gradientDrawable
                speakingAnimationBar7.background = gradientDrawable
                speakingAnimationBar8.background = gradientDrawable
            }
            else
            {
                speakingAnimationBar1.setBackgroundColor(Color.parseColor(toolBarProps?.equalizerColor))
                speakingAnimationBar2.setBackgroundColor(Color.parseColor(toolBarProps?.equalizerColor))
                speakingAnimationBar3.setBackgroundColor(Color.parseColor(toolBarProps?.equalizerColor))
                speakingAnimationBar4.setBackgroundColor(Color.parseColor(toolBarProps?.equalizerColor ))
                speakingAnimationBar5.setBackgroundColor(Color.parseColor(toolBarProps?.equalizerColor ))
                speakingAnimationBar6.setBackgroundColor(Color.parseColor(toolBarProps?.equalizerColor))
                speakingAnimationBar7.setBackgroundColor(Color.parseColor(toolBarProps?.equalizerColor ))
                speakingAnimationBar8.setBackgroundColor(Color.parseColor(toolBarProps?.equalizerColor ))
            }
        }
        else{
            speakingAnimationBar1.setBackgroundColor(Color.parseColor("#80000000"))
            speakingAnimationBar2.setBackgroundColor(Color.parseColor("#80000000"))
            speakingAnimationBar3.setBackgroundColor(Color.parseColor("#80000000"))
            speakingAnimationBar4.setBackgroundColor(Color.parseColor("#80000000"))
            speakingAnimationBar5.setBackgroundColor(Color.parseColor("#80000000"))
            speakingAnimationBar6.setBackgroundColor(Color.parseColor("#80000000"))
            speakingAnimationBar7.setBackgroundColor(Color.parseColor("#80000000"))
            speakingAnimationBar8.setBackgroundColor(Color.parseColor("#80000000"))
        }

        //set Text View Styles
        speakTextView.setTextColor(if(assistantSettingProps?.initializeWithText == false && assistantSettingProps?.useVoiceInput == true) Color.parseColor(toolBarProps?.speakActiveTitleColor ?: "#3E77A5") else Color.parseColor(toolBarProps?.speakInactiveTitleColor ?:"#8F97A1"))
        typeTextView.setTextColor(if(assistantSettingProps?.initializeWithText == false && assistantSettingProps?.useVoiceInput == true) Color.parseColor(toolBarProps?.typeInactiveTitleColor ?:"#8F97A1") else Color.parseColor(toolBarProps?.typeActiveTitleColor ?:"#3E77A5"))
        speakTextView.textSize = toolBarProps?.speakFontSize ?: 12f
        speakTextView.typeface = Typeface.create(toolBarProps?.speakFontFamily ?: "sans-serif", Typeface.NORMAL)
        typeTextView.textSize = toolBarProps?.typeFontSize ?: 12f
        typeTextView.typeface = Typeface.create(toolBarProps?.typeFontFamily ?: "sans-serif", Typeface.NORMAL)
        drawerWelcomeTextView.text = toolBarProps?.helpText ?: "How can i help?"
        drawerWelcomeTextView.setTextColor(Color.parseColor(toolBarProps?.helpTextFontColor ?: "#8F97A1"))
        drawerWelcomeTextView.textSize = toolBarProps?.helpTextFontSize ?: 18f
        drawerWelcomeTextView.typeface = Typeface.create(toolBarProps?.helpTextFontFamily ?: "sans-serif", Typeface.NORMAL)
        assistantStateTextView.textSize = toolBarProps?.assistantStateFontSize ?: 16f
        assistantStateTextView.typeface = Typeface.create(toolBarProps?.assistantStateFontFamily ?: "sans-serif", Typeface.NORMAL)
        spokenTextView.textSize = 16f
        spokenTextView.typeface = Typeface.create(toolBarProps?.partialSpeechResultFontFamily ?: "sans-serif", Typeface.NORMAL)
        assistantStateTextView.setTextColor(Color.parseColor(toolBarProps?.assistantStateTextColor ?: "#8F97A1"))
        isUsingSpeech = assistantSettingProps?.initializeWithText == false && assistantSettingProps?.useVoiceInput == true
        assistantNameTextView.typeface = Typeface.create(headerProps?.fontFamily ?: "sans-serif", Typeface.NORMAL)
        assistantNameTextView.text = headerProps?.assistantName ?: "Voicify Assistant"
        assistantNameTextView.textSize = headerProps?.fontSize ?: 18f
        inputTextMessageEditTextView.hint = toolBarProps?.placeholder ?: "Enter a message..."
        inputTextMessageEditTextView.typeface = Typeface.create(toolBarProps?.textboxFontFamily ?: "sans-serif", Typeface.NORMAL)
        assistantNameTextView.setTextColor(Color.parseColor(headerProps?.assistantNameTextColor ?: "#000000"))

        //Create Styles
        val closeAssistantImageBackgroundStyle = GradientDrawable()
        closeAssistantImageBackgroundStyle.cornerRadius = headerProps?.closeAssistantButtonBorderRadius ?: 0f
        closeAssistantImageBackgroundStyle.setStroke(headerProps?.assistantImageBorderWidth ?: 0, Color.parseColor(headerProps?.closeAssistantButtonBorderColor ?: "#00ffffff"))
        closeAssistantImageBackgroundStyle.setColor(Color.parseColor(headerProps?.closeAssistantButtonBackgroundColor ?: "#00ffffff"))

        val avatarBackgroundStyle = GradientDrawable()
        avatarBackgroundStyle.cornerRadius = headerProps?.assistantImageBorderRadius ?: 48f
        avatarBackgroundStyle.setStroke(headerProps?.assistantImageBorderWidth ?: 4, Color.parseColor(headerProps?.assistantImageBorderColor ?: "#CBCCD2"))
        avatarBackgroundStyle.setColor(Color.parseColor(headerProps?.assistantImageBackgroundColor ?: "#ffffff"))
        assistantAvatarBackground.background = avatarBackgroundStyle
        assistantAvatarBackground.setPadding(12,12,12,12)

        val spokenTextViewStyle = GradientDrawable()
        spokenTextViewStyle.cornerRadius = 24f
        spokenTextViewStyle.setColor(Color.parseColor(toolBarProps?.speechResultBoxBackgroundColor ?: "#80000000"))
        spokenTextView.background = spokenTextViewStyle

        val micImageViewStyle = GradientDrawable()
        micImageViewStyle.setColor(Color.parseColor(toolBarProps?.micActiveHighlightColor ?: "#1f1e7eb9"))
        micImageViewStyle.setStroke(toolBarProps?.micImageBorderWidth ?: 0, Color.parseColor(toolBarProps?.micImageBorderColor ?: "#00ffffff"))
        micImageViewStyle.cornerRadius = toolBarProps?.micBorderRadius ?: 100f
        micImageView.setPadding(toolBarProps?.micImagePadding ?: getPixelsFromDp(4), toolBarProps?.micImagePadding ?: getPixelsFromDp(4),toolBarProps?.micImagePadding ?: getPixelsFromDp(4),toolBarProps?.micImagePadding ?: getPixelsFromDp(4))
        val micImageLayoutParams = LinearLayout.LayoutParams(toolBarProps?.micImageWidth ?: getPixelsFromDp(48), toolBarProps?.micImageHeight ?: getPixelsFromDp(48))
        micImageLayoutParams.setMargins(0,getPixelsFromDp(12),0,0)
        micImageView.layoutParams = micImageLayoutParams

        val sendTextLayoutStyle = GradientDrawable()
        sendTextLayoutStyle.setColor(Color.parseColor(toolBarProps?.textboxActiveHighlightColor ?: "#1f1e7eb9"))
        sendTextLayoutStyle.cornerRadius = 24f

        if(assistantSettingProps?.initializeWithText == true || assistantSettingProps?.useVoiceInput == false)
        {
            speakingAnimationLayout.visibility = View.GONE
            sendTextLayout.background = sendTextLayoutStyle
            isUsingSpeech = false
            spokenTextView.visibility = View.GONE
            assistantStateTextView.visibility = View.GONE
        }

        val bodyContainerLayoutStyle = GradientDrawable()
        if(!bodyProps?.backgroundColor.isNullOrEmpty()){
            bodyContainerLayoutStyle.setColor(Color.parseColor(bodyProps?.backgroundColor))
        }
        else if (assistantSettingProps?.backgroundColor.isNullOrEmpty())
        {
            bodyContainerLayoutStyle.setColor(Color.parseColor("#F4F4F6"))
        }
        bodyContainerLayout.background = bodyContainerLayoutStyle
        bodyContainerLayout.setPadding(
            bodyProps?.paddingLeft ?: 20,
            bodyProps?.paddingTop ?: 0,
            bodyProps?.paddingRight ?: 20,
            bodyProps?.paddingBottom ?: 0
        )
        voicifyTTS?.cancelSpeech = false
        val inputTextMessageEditTextViewStyle = GradientDrawable()
        inputTextMessageEditTextViewStyle.setColor(Color.parseColor("#1f1e7eb9"))
        inputTextMessageEditTextView.textSize = toolBarProps?.textboxFontSize ?: 18f

        //initialization
        if(!assistantSettingProps?.locale.toString().isNullOrEmpty())
        {
            voicifySTT?.initialize(assistantSettingProps?.locale.toString())
        }
        assistant.initializeAndStart()
        assistant.startNewSession(null, null, this.sessionAttributes, this.userAttributes)
        if(assistantSettingProps?.initializeWithText == false && assistantSettingProps?.initializeWithWelcomeMessage == false)
        {
            voicifySTT?.startListening()
        }

        //Listeners
        //STT
        voicifySTT?.addPartialListener { partialResult ->
            spokenTextView.setTextColor(Color.parseColor(toolBarProps?.partialSpeechResultTextColor ?: "#33ffffff"))
            spokenTextView.text = partialResult
        }
        voicifySTT?.addFinalResultListener { fullResult ->
            spokenTextView.setTextColor(Color.parseColor(toolBarProps?.fullSpeechResultTextColor ?: "#ffffff"))
            speechFullResult = fullResult
            clearAnimationValues()
            assistantIsListening = false
            spokenTextView.text = fullResult
            assistantStateTextView.text = "Processing..."
            assistant.makeTextRequest(fullResult.toString(), null, "Speech")
        }
        voicifySTT?.addEndListener {

        }
        voicifySTT?.addSpeechReadyListener {
            micImageView.background = micImageViewStyle
            assistantIsListening = true;
            assistantStateTextView.text = "Listening..."
        }
        voicifySTT?.addErrorListener { error ->
            clearAnimationValues()
            if (error == "7")
            {
                assistantIsListening = false
                micImageView.setBackgroundColor(Color.parseColor(toolBarProps?.micInactiveHighlightColor ?: "#00ffffff"))
                assistantStateTextView.text = "I didn't catch that..."
            }
        }
        voicifySTT?.addVolumeListener { volume ->
            val rnd1 = (1..(volume.roundToInt() * 2 + 1)).random().toFloat()
            val rnd2 = (1..(volume.roundToInt() * 3 + 1)).random().toFloat()
            val rnd3 = (1..(volume.roundToInt() * 5 + 1)).random().toFloat()
            val rnd4 = (1..(volume.roundToInt() * 6 + 1)).random().toFloat()
            val rnd5 = (1..(volume.roundToInt() * 6 + 1)).random().toFloat()
            val rnd6 = (1..(volume.roundToInt() * 5 + 1)).random().toFloat()
            val rnd7 = (1..(volume.roundToInt() * 3 + 1)).random().toFloat()
            val rnd8 = (1..(volume.roundToInt() * 2 + 1)).random().toFloat()
            val duration = 100L
            val bar1 = ObjectAnimator.ofFloat(speakingAnimationBar1, "scaleY", rnd1)
            bar1.duration = duration
            val bar2 = ObjectAnimator.ofFloat(speakingAnimationBar2, "scaleY", rnd2)
            bar2.duration = duration
            val bar3 = ObjectAnimator.ofFloat(speakingAnimationBar3, "scaleY", rnd3)
            bar3.duration = duration
            val bar4 = ObjectAnimator.ofFloat(speakingAnimationBar4, "scaleY", rnd4)
            bar4.duration = duration
            val bar5 = ObjectAnimator.ofFloat(speakingAnimationBar5, "scaleY", rnd5)
            bar5.duration = duration
            val bar6 = ObjectAnimator.ofFloat(speakingAnimationBar6, "scaleY", rnd6)
            bar6.duration = duration
            val bar7 = ObjectAnimator.ofFloat(speakingAnimationBar7, "scaleY", rnd7)
            bar7.duration = duration
            val bar8 = ObjectAnimator.ofFloat(speakingAnimationBar8, "scaleY", rnd8)
            bar8.duration = duration
            animation = AnimatorSet().apply {
                playTogether(bar1, bar2, bar3,bar4,bar5,bar6,bar7,bar8)
                if(canRun)
                {
                    start()
                    canRun = false
                }
               doOnEnd {
                   canRun = true
               }
            }
        }
        //Assistant
        assistant.onResponseReceived { response ->
            activity?.runOnUiThread{
                drawerLayout.visibility = View.VISIBLE
                bodyContainerLayout.visibility = View.VISIBLE
                spokenTextView.text = ""
                hintsRecyclerView.visibility = View.VISIBLE
                if(!response.hints.isNullOrEmpty())
                {
                    if(!hintsList.isNullOrEmpty())
                    {
                        hintsList.clear()
                        hintsRecyclerViewAdapter?.notifyDataSetChanged()
                    }
                    response.hints.forEach { hint ->
                        hintsList.add(hint)
                        hintsRecyclerViewAdapter?.notifyDataSetChanged()
                    }
                }
                else
                {
                    hintsRecyclerView.visibility = View.GONE
                }
                if(!isUsingSpeech)
                {
                    val drawerFooterLayoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    drawerFooterLayoutParams.setMargins(0,0,0,0)
                    drawerFooterLayout.layoutParams = drawerFooterLayoutParams
                    dashedLineImageView.visibility = View.INVISIBLE;
                }
                if(!speechFullResult.isNullOrEmpty())
                {
                    messagesList.add(Message(speechFullResult as String, "Sent"))
                    messagesRecyclerViewAdapter?.notifyDataSetChanged()
                    messagesRecyclerView.smoothScrollToPosition(messagesRecyclerViewAdapter?.itemCount as Int);
                }
                speechFullResult = null
                drawerLayout.setPadding(0,0,0,0)
                drawerLayout.setBackgroundColor(Color.TRANSPARENT)
                if(!toolBarProps?.backgroundColor.isNullOrEmpty())
                {
                    toolbarLayout.setBackgroundColor(Color.parseColor(toolBarProps?.backgroundColor))
                }
                else if (assistantSettingProps?.backgroundColor.isNullOrEmpty())
                {
                    toolbarLayout.setBackgroundColor(Color.parseColor("#ffffff"))
                }
                toolBarLayout.setPadding(toolBarProps?.paddingLeft ?: getPixelsFromDp(16), getPixelsFromDp(0),toolBarProps?.paddingRight ?: getPixelsFromDp(16),toolBarProps?.paddingBottom ?: getPixelsFromDp(16))
                assistantAvatarBackground.visibility = View.VISIBLE
                if(!headerProps?.backgroundColor.isNullOrEmpty()){
                    headerLayout.setBackgroundColor(Color.parseColor(headerProps?.backgroundColor))
                }
                else if (assistantSettingProps?.backgroundColor.isNullOrEmpty())
                {
                    headerLayout.setBackgroundColor(Color.parseColor("#ffffff"))
                }
                headerLayout.setPadding(headerProps?.paddingLeft ?: getPixelsFromDp(16), headerProps?.paddingTop ?: getPixelsFromDp(16), headerProps?.paddingRight ?: getPixelsFromDp(16), headerProps?.paddingBottom ?: getPixelsFromDp(16))
                bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
                isDrawer = false
                micImageView.setBackgroundColor(Color.parseColor(toolBarProps?.micInactiveHighlightColor ?: "#00ffffff"))
                val metrics = activity?.resources?.displayMetrics
                var params = drawerLayout.layoutParams
                params.height = metrics?.heightPixels as Int
                drawerLayout.layoutParams = params
                assistantStateTextView.text = ""
                drawerWelcomeTextView.text = ""
                assistantAvatarImageView.visibility = View.VISIBLE
                assistantNameTextView.visibility = View.VISIBLE
                messagesRecyclerView.visibility = View.VISIBLE
                bodyBorderTopView.visibility = View.VISIBLE
                bodyBorderBottomView.visibility = View.VISIBLE
                messagesList.add(Message(response.displayText?.trim() as String, "Received"))
                messagesRecyclerViewAdapter?.notifyDataSetChanged()
                messagesRecyclerView.smoothScrollToPosition(messagesRecyclerViewAdapter?.itemCount as Int);
            }
        }

        //Views
        micImageView.setOnClickListener{
            clearAnimationValues()
            if(!isUsingSpeech)
            {
                voicifySTT?.cancel = false
                isUsingSpeech = true
                messagesRecyclerViewAdapter?.notifyDataSetChanged()
                messagesRecyclerView.smoothScrollToPosition(messagesRecyclerViewAdapter?.itemCount as Int);
                speakingAnimationLayout.visibility = View.VISIBLE
                sendTextLayout.setBackgroundColor(Color.parseColor(toolBarProps?.textboxInactiveHighlightColor ?: "#00ffffff"))
                dashedLineImageView.visibility = View.VISIBLE;
                hideKeyboard()
               if(!isDrawer){
                    val drawerFooterLayoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    drawerFooterLayoutParams.setMargins(0,getPixelsFromDp(20),0,0)
                    drawerFooterLayout.layoutParams = drawerFooterLayoutParams
                }
                spokenTextView.visibility = View.VISIBLE
                assistantStateTextView.visibility = View.VISIBLE
                speakTextView.setTextColor(Color.parseColor(toolBarProps?.speakActiveTitleColor ?: "#3E77A5"))
                typeTextView.setTextColor(Color.parseColor(toolBarProps?.typeInactiveTitleColor ?: "#8F97A1"))
                loadImageFromUrl(toolBarProps?.micActiveImage?: "https://voicify-prod-files.s3.amazonaws.com/99a803b7-5b37-426c-a02e-63c8215c71eb/daca643f-6730-4af5-8817-8d9d0d9db0b5/mic-image.png", micImageView, toolBarProps?.micActiveColor)
                loadImageFromUrl(toolBarProps?.sendInactiveImage ?: "https://voicify-prod-files.s3.amazonaws.com/99a803b7-5b37-426c-a02e-63c8215c71eb/0c5aa61c-7d6c-4272-abd2-75d9f5771214/Send-2-.png", sendMessageImageView, toolBarProps?.sendInactiveColor)
            }
            if(!assistantIsListening)
            {
                voicifyTTS?.stop()
                voicifySTT?.startListening()
            }
            else
            {
                cancelSpeech()
                assistantStateTextView.text = "I didn't catch that..."
            }
        }

        assistantSettingProps?.effects?.forEach { effect ->
            assistant.onEffect(effect) { data ->
                onEffectCallback?.invoke(effect, data)
            }
        }

        // add out of box close effect
       assistant.onEffect("closeAssistant"){
           dismiss()
       }

        sendMessageImageView.setOnClickListener{
            if(inputTextMessageEditTextView.text.toString().isNotEmpty())
            {
                messagesList.add(Message(inputTextMessageEditTextView.text.toString(), "Sent"))
                messagesRecyclerViewAdapter?.notifyDataSetChanged()
                messagesRecyclerView.smoothScrollToPosition(messagesRecyclerViewAdapter?.itemCount as Int);
                val inputText = inputTextMessageEditTextView.text.toString()
                inputTextMessageEditTextView.setText("")
                hideKeyboard()
                assistant.makeTextRequest(inputText,null, "Text")
            }
        }

        inputTextMessageEditTextView.setOnTouchListener(object : OnTouchListener {
            @SuppressLint("ClickableViewAccessibility")
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when(event?.action) {
                    MotionEvent.ACTION_UP -> {
                        if (assistantIsListening) {
                            cancelSpeech()
                            assistantStateTextView.text = ""
                            spokenTextView.text = ""
                        }
                        if (isUsingSpeech) {
                            voicifySTT?.cancel = true
                            speakingAnimationLayout.visibility = View.GONE
                            sendTextLayout.background = sendTextLayoutStyle
                            isUsingSpeech = false
                            if(!isDrawer)
                            {
                                val drawerFooterLayoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                                drawerFooterLayoutParams.setMargins(0,0,0,0)
                                drawerFooterLayout.layoutParams = drawerFooterLayoutParams
                                dashedLineImageView.visibility = View.INVISIBLE;
                            }
                            spokenTextView.visibility = View.GONE
                            assistantStateTextView.visibility = View.GONE
                            speakTextView.setTextColor(Color.parseColor(toolBarProps?.speakInactiveTitleColor ?: "#8F97A1"))
                            typeTextView.setTextColor(Color.parseColor(toolBarProps?.typeActiveTitleColor ?: "#3E77A5"))
                            loadImageFromUrl(
                                toolBarProps?.micInactiveImage ?: "https://voicify-prod-files.s3.amazonaws.com/99a803b7-5b37-426c-a02e-63c8215c71eb/3f10b6d7-eb71-4427-adbc-aadacbe8940e/mic-image-1-.png",
                                micImageView,
                                toolBarProps?.micInactiveColor
                            )
                            loadImageFromUrl(
                                toolBarProps?.sendActiveImage?: "https://voicify-prod-files.s3.amazonaws.com/99a803b7-5b37-426c-a02e-63c8215c71eb/7a39bc6f-eef5-4185-bcf8-2a645aff53b2/Send-3-.png",
                                sendMessageImageView,
                                toolBarProps?.sendActiveColor
                            )
                        }
                    }
                }
                v?.performClick()
                return v?.onTouchEvent(event) ?: true
            }
        })

        closeAssistantImageView.setOnClickListener{
            dismiss()
        }

        // Inflate the layout for this fragment
        return window
    }

    private fun Fragment.hideKeyboard() {
        view?.let { activity?.hideKeyboard(it) }
    }

    private fun Context.hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun cancelSpeech() {
        voicifySTT?.stopListening()
        voicifySTT?.cancelListening()
        voicifySTT?.destoryInstance()
        assistantIsListening = false
        micImageView.setBackgroundColor(Color.parseColor(toolBarProps?.micInactiveHighlightColor ?: "#00ffffff"))
        spokenTextView.text =  ""
    }
    private fun loadImageFromUrl(url: String, view: ImageView, imageColor: String? = null){
        if(imageColor.isNullOrEmpty())
        {
            Picasso.get().load(url).into(view)
        }
        else{
            Picasso.get().load(url).into(view, object: Callback {
                override fun onSuccess() {
                    DrawableCompat.setTint(view.drawable, Color.parseColor(imageColor));
                }

                override fun onError(e: java.lang.Exception?) {
                }
            })
        }
    }
    private fun getPixelsFromDp(dp: Int): Int {
        return (dp * scale + 0.5f).toInt()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bottomSheetBehavior = BottomSheetBehavior.from((view.parent as View))
        bottomSheetBehavior?.isDraggable = false

        val touchOutsideView =
            dialog!!.window!!.decorView.findViewById<View>(R.id.touch_outside)
        touchOutsideView.setOnClickListener(null) // dont allow modal to close when touched outside
    }

    private fun clearAnimationValues(){
        animation?.end()
        val duration = 300L
        val bar1 = ObjectAnimator.ofFloat(speakingAnimationBar1, "scaleY", 1f)
        bar1.duration = duration
        val bar2 = ObjectAnimator.ofFloat(speakingAnimationBar2, "scaleY", 1f)
        bar2.duration = duration
        val bar3 = ObjectAnimator.ofFloat(speakingAnimationBar3, "scaleY", 1f)
        bar3.duration = duration
        val bar4 = ObjectAnimator.ofFloat(speakingAnimationBar4, "scaleY", 1f)
        bar4.duration = duration
        val bar5 = ObjectAnimator.ofFloat(speakingAnimationBar5, "scaleY", 1f)
        bar5.duration = duration
        val bar6 = ObjectAnimator.ofFloat(speakingAnimationBar6, "scaleY", 1f)
        bar6.duration = duration
        val bar7 = ObjectAnimator.ofFloat(speakingAnimationBar7, "scaleY", 1f)
        bar7.duration = duration
        val bar8 = ObjectAnimator.ofFloat(speakingAnimationBar8, "scaleY", 1f)
        bar8.duration = duration
        AnimatorSet().apply {
            playTogether(bar1, bar2, bar3,bar4,bar5,bar6,bar7,bar8)
            start()
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        cancelSpeech()
        voicifyTTS?.stop()
        voicifyTTS?.cancelSpeech = true
        if(onAssistantDismissCallback != null)
        {
            onAssistantDismissCallback?.invoke()
        }
        val manager = parentFragmentManager
        val transaction: FragmentTransaction = manager.beginTransaction()
        transaction.remove(this)
        transaction.commit()
    }



    private fun TextView.setCursorDrawableColor(@ColorInt color: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            textCursorDrawable?.tinted(color)
            return
        }

        try {
            val editorField = TextView::class.java.getFieldByName("mEditor")
            val editor = editorField?.get(this) ?: this
            val editorClass: Class<*> = if (editorField != null) editor.javaClass else TextView::class.java
            val cursorRes = TextView::class.java.getFieldByName("mCursorDrawableRes")?.get(this) as? Int ?: return

            val tintedCursorDrawable = ContextCompat.getDrawable(context, cursorRes)?.tinted(color) ?: return

            val cursorField = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                editorClass.getFieldByName("mDrawableForCursor")
            } else {
                null
            }
            if (cursorField != null) {
                cursorField.set(editor, tintedCursorDrawable)
            } else {
                editorClass.getFieldByName("mCursorDrawable", "mDrawableForCursor")
                    ?.set(editor, arrayOf(tintedCursorDrawable, tintedCursorDrawable))
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    private fun Class<*>.getFieldByName(vararg name: String): Field? {
        name.forEach {
            try{
                return this.getDeclaredField(it).apply { isAccessible = true }
            } catch (t: Throwable) { }
        }
        return null
    }

    private fun Drawable.tinted(@ColorInt color: Int): Drawable = when {
        this is VectorDrawableCompat -> {
            this.apply { setTintList(ColorStateList.valueOf(color)) }
        }
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && this is VectorDrawable -> {
            this.apply { setTintList(ColorStateList.valueOf(color)) }
        }
        else -> {
            DrawableCompat.wrap(this)
                .also { DrawableCompat.setTint(it, color) }
                .let { DrawableCompat.unwrap(it) }
        }
    }

    private fun Number.spToPx(context: Context? = null): Float {
        val res = context?.resources ?: android.content.res.Resources.getSystem()
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, this.toFloat(), res.displayMetrics)
    }

    fun <T> deserializeEffectData(data: Any, type: Class<T>): T {
        val gson = Gson()
        val dataString = gson.toJson(data)
        val newData =  gson.fromJson(dataString, type)
        return newData as T
    }

    fun onEffect(callback: ((effect: String, data: Any) -> Unit)){
        onEffectCallback = callback
    }

    fun addSessionAttributes(sessionAttributes: Map<String, Any>){
        this.sessionAttributes = sessionAttributes
    }

    fun onAssistantDismiss(callback: () -> Unit)
    {
        this.onAssistantDismissCallback = callback
    }

    fun addUserAttributes(userAttributes: Map<String, Any>){
        this.userAttributes = userAttributes
    }
    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param headerProps Parameter 1.
         * @param bodyProps Parameter 2.
         * @param toolBarProps Parameter 3.
         * @param assistantSettingsProps Parameter 3.
         * @return A new instance of fragment AssistantDrawerUI.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance( assistantSettingsProps: AssistantSettingsProps, headerProps: HeaderProps, bodyProps: BodyProps, toolBarProps: ToolBarProps) =
            AssistantDrawerUI().apply {
                arguments = Bundle().apply {
                    putSerializable(SETTINGS, assistantSettingsProps)
                    putSerializable(HEADER, headerProps)
                    putSerializable(BODY, bodyProps)
                    putSerializable(TOOLBAR, toolBarProps)
                }
            }
    }
}