package com.voicify.voicify_assistant_sdk.assistant

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.*
import android.content.Context.MODE_PRIVATE
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.VectorDrawable
import android.os.Build
import android.os.Bundle
import android.speech.SpeechRecognizer
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
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
import com.voicify.voicify_assistant_sdk.models.CustomAssistantConfigurationResponse
import com.voicify.voicify_assistant_sdk.models.CustomAssistantRequest
import kotlinx.android.synthetic.main.fragment_assistant_drawer_u_i.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import java.lang.Exception
import java.lang.reflect.Field
import kotlin.math.roundToInt


private const val SETTINGS = "assistantSettings"
private const val HEADER = "header"
private const val BODY = "body"
private const val TOOLBAR = "toolbar"
private const val CONFIGURATION = "configuration"
private const val CONFIGURATION_KOTLIN = "configurationKotlin"

private var configurationKotlin: CustomAssistantConfigurationResponse? = null
private var configurationHeaderProps: HeaderProps? = null
private var configurationBodyProps: BodyProps? = null
private var configurationToolbarProps: ToolbarProps? = null
private var loginResponseReceiver: BroadcastReceiver? = null
private var isLoadingConfiguration = true
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
    private var toolbarProps: ToolbarProps? = null
    private var assistantIsListening: Boolean = false
    private var isUsingSpeech: Boolean = true
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
    private var onAssistantErrorCallback: ((errorMessage: String, request: CustomAssistantRequest) -> Unit)? = null
    private var sessionAttributes: Map<String, Any>? = emptyMap()
    private var userAttributes: Map<String, Any> = emptyMap()
    private var customAssistantConfigurationService: CustomAssistantConfigurationService = CustomAssistantConfigurationService()
    val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            assistantSettingProps = it.getSerializable(SETTINGS) as AssistantSettingsProps?
            headerProps = it.getSerializable(HEADER) as HeaderProps?
            bodyProps = it.getSerializable(BODY) as BodyProps?
            toolbarProps = it.getSerializable(TOOLBAR) as ToolbarProps?
        }
        if(configurationKotlin == null && !assistantSettingProps?.configurationId.isNullOrEmpty())
        {
            Log.d("JAMES", "TRYING SHARED PREFS")
            // if the configuration call returns null, but the configuration id is specified, try to grab the config from shared preferences
            val prefs = requireActivity().getSharedPreferences(CONFIGURATION, MODE_PRIVATE)
            if(prefs != null)
            {
                val preferenceConfig = prefs.getString(CONFIGURATION_KOTLIN ,"")
                if (!preferenceConfig.isNullOrEmpty())
                {
                    Log.d("JAMES", "GRABBING SHARED PREFS")
                    configurationKotlin = gson.fromJson(preferenceConfig, CustomAssistantConfigurationResponse::class.java)
                }
            }
        }
        else{
            val editor = requireActivity().getSharedPreferences(CONFIGURATION, MODE_PRIVATE).edit()
            editor.putString(CONFIGURATION_KOTLIN, gson.toJson(configurationKotlin))
            editor.apply()
        }
    }

    @SuppressLint("ResourceType")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val window = inflater.inflate(R.layout.fragment_assistant_drawer_u_i, container, false)
        val hintsList = ArrayList<String>()
        val messagesList = ArrayList<Message>()

        scale = requireContext().resources.displayMetrics.density
        isUsingSpeech = (assistantSettingProps?.initializeWithText ?: configurationKotlin?.activeInput == "textbox") != true && (assistantSettingProps?.useVoiceInput ?: configurationKotlin?.useVoiceInput) != false

        //Linear Layouts
        val containerLayout = window.findViewById<LinearLayout>(R.id.container)
        val drawerLayout = window.findViewById<LinearLayout>(R.id.drawerLayout)
        val bodyContainerLayout = window.findViewById<LinearLayout>(R.id.bodyContainerLayout)
        val headerLayout = window.findViewById<LinearLayout>(R.id.headerLayout)
        val toolbarLayout = window.findViewById<LinearLayout>(R.id.toolbarLayout)
        val sendTextLayout = window.findViewById<LinearLayout>(R.id.sendTextLayout)
        val speakingAnimationLayout = window.findViewById<LinearLayout>(R.id.speakingAnimation)
        val assistantAvatarBackground = window.findViewById<LinearLayout>(R.id.assistantAvatarBackgroundContainerLayout)
        val closeAssistantBackground = window.findViewById<LinearLayout>(R.id.closeAssistantBackgroundContainerLayout)
        val drawerFooterLayout = window.findViewById<LinearLayout>(R.id.drawerFooterLayout)

        //Progress Bars
        val activityIndicator = window.findViewById<ProgressBar>(R.id.activityIndicator)
        activityIndicator.setBackgroundColor(Color.parseColor("#99000000"))

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
        val speakingAnimationBars = arrayOf(speakingAnimationBar1,speakingAnimationBar2,speakingAnimationBar3,speakingAnimationBar4,speakingAnimationBar5,speakingAnimationBar6,speakingAnimationBar7,speakingAnimationBar8)

        // Recycler Views
        val messagesRecyclerView = window.findViewById<RecyclerView>(R.id.messagesRecyclerView)
        val hintsRecyclerView = window.findViewById<RecyclerView>(R.id.hintsRecyclerView)

        //Text Views
        val assistantStateTextView = window.findViewById<TextView>(R.id.assistantStateTextView)
        val spokenTextView = window.findViewById<TextView>(R.id.spokenTextView)
        val drawerWelcomeTextView = window.findViewById<TextView>(R.id.drawerWelcomeTextView)
        val typeTextView = window.findViewById<TextView>(R.id.typeTextView)
        val inputTextMessageEditTextView = window.findViewById<EditText>(R.id.inputTextMessage)
        val assistantNameTextView = window.findViewById<TextView>(R.id.assistantNameTextView)
        val speakTextView = window.findViewById<TextView>(R.id.speakTextView)
        val sendTextLayoutStyle = GradientDrawable()
        sendTextLayoutStyle.setColor(Color.parseColor(toolbarProps?.textboxActiveHighlightColor ?: configurationToolbarProps?.textboxActiveHighlightColor ?: "#1f1e7eb9"))
        sendTextLayoutStyle.cornerRadius = 24f

        //Image Views
        val micImageView = window.findViewById<ImageView>(R.id.micImageView)
        val closeAssistantImageView = window.findViewById<ImageView>(R.id.closeAssistantImageView)
        val closeAssistantNoInternetImageView = window.findViewById<ImageView>(R.id.closeAssistantNoInternetImageView)
        val sendMessageImageView = window.findViewById<ImageView>(R.id.sendMessageButtonImageView)
        val assistantAvatarImageView = window.findViewById<ImageView>(R.id.assistantAvatarImageView)
        val dashedLineImageView = window.findViewById<ImageView>(R.id.dashedLineImageView)

        if(!isLoadingConfiguration)
        {
            Log.d("JAMES", "WE ARE NOT LOADING IN ON CREATE VIEW")
            containerLayout.visibility = View.VISIBLE
            activityIndicator.visibility = View.GONE
            val assistant = initializeAssistant()
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

            //UI Initialization
            addGradientBackground(containerLayout)
            initializeImageViews(micImageView, closeAssistantImageView, sendMessageImageView, assistantAvatarImageView, speakTextView)
            initializeLinearLayouts(drawerLayout, closeAssistantBackground, assistantAvatarBackground, bodyContainerLayout)
            checkInitializeWithText(speakingAnimationLayout, sendTextLayoutStyle, sendTextLayout, spokenTextView, assistantStateTextView)
            initializeRecyclerViews(messagesRecyclerView, hintsRecyclerView, messagesList, hintsList, onHintClicked)
            initializeViews(bodyBorderTopView,bodyBorderBottomView,speakingAnimationBars)
            initializeTextViews(speakTextView, typeTextView, drawerWelcomeTextView, spokenTextView, assistantStateTextView, assistantNameTextView, inputTextMessageEditTextView)

            startNewAssistantSession(assistant)

            //Add Listeners
            addKeyboardActiveListener(window)
            addSpeechToTextListeners(assistant, spokenTextView, assistantStateTextView, micImageView)
            addMicClickListener(micImageView, messagesRecyclerView, speakingAnimationLayout, sendTextLayout, dashedLineImageView, drawerFooterLayout,
                spokenTextView, assistantStateTextView, speakTextView, typeTextView, sendMessageImageView)

            addSendMessageClickListener(sendMessageImageView, inputTextMessageEditTextView, messagesList, messagesRecyclerView, assistant)
            addAssistantHandlers(assistant, drawerLayout, bodyContainerLayout, spokenTextView, hintsRecyclerView, closeAssistantImageView, closeAssistantNoInternetImageView,
                hintsList, drawerWelcomeTextView, drawerFooterLayout, dashedLineImageView, messagesList, messagesRecyclerView, toolbarLayout, headerLayout,
                assistantAvatarBackground, micImageView, assistantStateTextView, assistantAvatarImageView, assistantNameTextView, bodyBorderTopView, bodyBorderBottomView)

            addTextboxClickListener(inputTextMessageEditTextView, assistantStateTextView, spokenTextView, speakingAnimationLayout, sendTextLayoutStyle,
                sendTextLayout, drawerFooterLayout, dashedLineImageView, speakTextView, typeTextView, micImageView, sendMessageImageView)

            closeAssistantImageView.setOnClickListener{
                dismiss()
            }
            closeAssistantNoInternetImageView.setOnClickListener{
                dismiss()
            }
        }

        loginResponseReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                activity?.runOnUiThread{
                    Log.d("JAMES", "LOADING COMPLETE")
                    containerLayout.visibility = View.VISIBLE
                    activityIndicator.visibility = View.GONE
                    val assistant = initializeAssistant()
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

                    addGradientBackground(containerLayout)
                    initializeImageViews(micImageView, closeAssistantImageView, sendMessageImageView, assistantAvatarImageView, speakTextView)
                    initializeLinearLayouts(drawerLayout, closeAssistantBackground, assistantAvatarBackground, bodyContainerLayout)
                    checkInitializeWithText(speakingAnimationLayout, sendTextLayoutStyle, sendTextLayout, spokenTextView, assistantStateTextView)
                    initializeRecyclerViews(messagesRecyclerView, hintsRecyclerView, messagesList, hintsList, onHintClicked)
                    initializeViews(bodyBorderTopView,bodyBorderBottomView,speakingAnimationBars)
                    initializeTextViews(speakTextView, typeTextView, drawerWelcomeTextView, spokenTextView, assistantStateTextView, assistantNameTextView, inputTextMessageEditTextView)

                    //UI Initialization
                    checkInitializeWithText(speakingAnimationLayout, sendTextLayoutStyle, sendTextLayout, spokenTextView, assistantStateTextView)
                    checkInitializeWithWelcome(drawerLayout, bodyContainerLayout, spokenTextView, hintsRecyclerView, drawerFooterLayout,
                        dashedLineImageView, toolbarLayout, assistantAvatarBackgroundContainerLayout, headerLayout,
                        micImageView, assistantStateTextView, drawerWelcomeTextView, assistantAvatarImageView,
                        assistantNameTextView, messagesRecyclerView, bodyBorderTopView, bodyBorderBottomView)
                    startNewAssistantSession(assistant)

                    //Add Listeners
                    addKeyboardActiveListener(window)
                    addSpeechToTextListeners(assistant, spokenTextView, assistantStateTextView, micImageView)
                    addMicClickListener(micImageView, messagesRecyclerView, speakingAnimationLayout, sendTextLayout, dashedLineImageView, drawerFooterLayout,
                        spokenTextView, assistantStateTextView, speakTextView, typeTextView, sendMessageImageView)
                    addSendMessageClickListener(sendMessageImageView, inputTextMessageEditTextView, messagesList, messagesRecyclerView, assistant)
                    addAssistantHandlers(assistant, drawerLayout, bodyContainerLayout, spokenTextView, hintsRecyclerView, closeAssistantImageView, closeAssistantNoInternetImageView,
                        hintsList, drawerWelcomeTextView, drawerFooterLayout, dashedLineImageView, messagesList, messagesRecyclerView, toolbarLayout, headerLayout,
                        assistantAvatarBackground, micImageView, assistantStateTextView, assistantAvatarImageView, assistantNameTextView, bodyBorderTopView, bodyBorderBottomView)

                    addTextboxClickListener(inputTextMessageEditTextView, assistantStateTextView, spokenTextView, speakingAnimationLayout, sendTextLayoutStyle,
                        sendTextLayout, drawerFooterLayout, dashedLineImageView, speakTextView, typeTextView, micImageView, sendMessageImageView)
                    closeAssistantImageView.setOnClickListener{
                        dismiss()
                    }
                    closeAssistantNoInternetImageView.setOnClickListener{
                        dismiss()
                    }
                }
            }
        }
        NotificationCenter.addObserver(requireContext(), NotificationType.LOADING_COMPLETE, loginResponseReceiver);
        if(isLoadingConfiguration)
        {
            containerLayout.visibility = View.GONE
            activityIndicator.visibility = View.VISIBLE
        }

        // Inflate the layout for this fragment
        return window
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bottomSheetBehavior = BottomSheetBehavior.from((view.parent as View))
        bottomSheetBehavior?.isDraggable = false
        if(!isLoadingConfiguration) {
            Log.d("JAMES", "WE ARE NOT LOADING IN ON CREATE VIEW")
            checkInitializeWithWelcome(
                drawerLayout,
                bodyContainerLayout,
                spokenTextView,
                hintsRecyclerView,
                drawerFooterLayout,
                dashedLineImageView,
                toolbarLayout,
                assistantAvatarBackgroundContainerLayout,
                headerLayout,
                micImageView,
                assistantStateTextView,
                drawerWelcomeTextView,
                assistantAvatarImageView,
                assistantNameTextView,
                messagesRecyclerView,
                bodyBorderTopView,
                bodyBorderBottomView
            )
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
        transaction.commitAllowingStateLoss();
    }

    override fun onDestroy() {
        super.onDestroy()
        NotificationCenter.removeObserver(requireContext(), loginResponseReceiver)
    }

    private fun initializeLinearLayouts(drawer: LinearLayout, closeBackground: LinearLayout, avatarBackground: LinearLayout, bodyLayout: LinearLayout){
        if(!(toolbarProps?.backgroundColor ?: configurationToolbarProps?.backgroundColor).isNullOrEmpty()){
            drawer.setBackgroundColor(Color.parseColor(toolbarProps?.backgroundColor ?: configurationToolbarProps?.backgroundColor));
        }
        else if ((assistantSettingProps?.backgroundColor ?: configurationKotlin?.styles?.assistant?.backgroundColor).isNullOrEmpty())
        {
            drawer.setBackgroundColor(Color.parseColor("#ffffff"));
        }

        drawer.setPadding(toolbarProps?.paddingLeft ?: configurationToolbarProps?.paddingLeft ?: getPixelsFromDp(16),toolbarProps?.paddingTop ?: configurationToolbarProps?.paddingTop ?: getPixelsFromDp(16),toolbarProps?.paddingRight ?: configurationToolbarProps?.paddingRight ?: getPixelsFromDp(16),toolbarProps?.paddingBottom ?: configurationToolbarProps?.paddingBottom ?: getPixelsFromDp(16))

        val closeAssistantImageBackgroundStyle = GradientDrawable()
        closeAssistantImageBackgroundStyle.cornerRadius = headerProps?.closeAssistantButtonBorderRadius ?: configurationHeaderProps?.closeAssistantButtonBorderRadius ?: 0f
        closeAssistantImageBackgroundStyle.setStroke(headerProps?.closeAssistantButtonBorderWidth ?: configurationHeaderProps?.closeAssistantButtonBorderWidth ?: 0, Color.parseColor(headerProps?.closeAssistantButtonBorderColor ?: configurationHeaderProps?.closeAssistantButtonBorderColor ?: "#00ffffff"))
        closeAssistantImageBackgroundStyle.setColor(Color.parseColor(headerProps?.closeAssistantButtonBackgroundColor ?: configurationHeaderProps?.closeAssistantButtonBackgroundColor ?: "#00ffffff"))
        closeBackground.background = closeAssistantImageBackgroundStyle
        closeBackground.setPadding(12,12,12,12)

        val avatarBackgroundStyle = GradientDrawable()
        avatarBackgroundStyle.cornerRadius = headerProps?.assistantImageBorderRadius ?: configurationHeaderProps?.assistantImageBorderRadius ?: 48f
        avatarBackgroundStyle.setStroke(headerProps?.assistantImageBorderWidth ?: configurationHeaderProps?.assistantImageBorderWidth ?: 4, Color.parseColor(headerProps?.assistantImageBorderColor ?: configurationHeaderProps?.assistantImageBorderColor ?: "#CBCCD2"))
        avatarBackgroundStyle.setColor(Color.parseColor(headerProps?.assistantImageBackgroundColor ?: configurationHeaderProps?.assistantImageBackgroundColor ?: "#ffffff"))
        avatarBackground.background = avatarBackgroundStyle
        avatarBackground.setPadding(12,12,12,12)

        val bodyContainerLayoutStyle = GradientDrawable()
        if(!(bodyProps?.backgroundColor ?: configurationBodyProps?.backgroundColor).isNullOrEmpty()){
            bodyContainerLayoutStyle.setColor(Color.parseColor(bodyProps?.backgroundColor ?: configurationBodyProps?.backgroundColor))
        }
        else if ((assistantSettingProps?.backgroundColor ?: configurationKotlin?.styles?.assistant?.backgroundColor).isNullOrEmpty())
        {
            bodyContainerLayoutStyle.setColor(Color.parseColor("#F4F4F6"))
        }
        bodyLayout.background = bodyContainerLayoutStyle
        bodyLayout.setPadding(
            bodyProps?.paddingLeft ?: configurationBodyProps?.paddingLeft ?: 20,
            bodyProps?.paddingTop ?: configurationBodyProps?.paddingTop ?: 0,
            bodyProps?.paddingRight ?: configurationBodyProps?.paddingRight ?: 20,
            bodyProps?.paddingBottom ?: configurationBodyProps?.paddingBottom ?: 0
        )
    }

    private fun initializeViews(bodyBorderTop: View, bodyBorderBottom: View, animationBars: Array<View>){
        bodyBorderTop.setBackgroundColor(Color.parseColor(bodyProps?.borderTopColor ?: configurationBodyProps?.borderTopColor ?: "#CBCCD2"))
        val bodyBorderTopViewLayoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, bodyProps?.borderTopWidth ?: configurationBodyProps?.borderTopWidth ?: 4)
        bodyBorderTop.layoutParams = bodyBorderTopViewLayoutParams

        bodyBorderBottom.setBackgroundColor(Color.parseColor(bodyProps?.borderBottomColor ?: configurationBodyProps?.borderBottomColor ?: "#CBCCD2"))
        val bodyBorderBottomViewLayoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, bodyProps?.borderBottomWidth ?: configurationBodyProps?.borderBottomWidth ?: 4)
        bodyBorderBottom.layoutParams = bodyBorderBottomViewLayoutParams

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
                bar.setBackgroundColor(Color.parseColor("#80000000"))
            }
        }
    }

    private fun initializeTextViews(speakText: TextView, typeText: TextView, drawerText: TextView, spokenText: TextView, assistantStateText: TextView, assistantNameText: TextView, inputTextMessage: EditText){
        speakText.setTextColor(if((assistantSettingProps?.initializeWithText ?: configurationKotlin?.activeInput == "textbox") != true && (assistantSettingProps?.useVoiceInput ?: configurationKotlin?.useVoiceInput) != false) Color.parseColor(toolbarProps?.speakActiveTitleColor ?: configurationToolbarProps?.speakActiveTitleColor ?: "#3E77A5") else Color.parseColor(toolbarProps?.speakInactiveTitleColor ?: configurationToolbarProps?.speakInactiveTitleColor ?:"#8F97A1"))
        speakText.textSize = toolbarProps?.speakFontSize ?: configurationToolbarProps?.speakFontSize ?: 12f
        speakText.typeface = Typeface.create(toolbarProps?.speakFontFamily ?: configurationToolbarProps?.speakFontFamily ?: "sans-serif", Typeface.NORMAL)

        typeText.setTextColor(if((assistantSettingProps?.initializeWithText ?: configurationKotlin?.activeInput == "textbox") != true && (assistantSettingProps?.useVoiceInput ?: configurationKotlin?.useVoiceInput) != false) Color.parseColor(toolbarProps?.typeInactiveTitleColor ?: configurationToolbarProps?.typeInactiveTitleColor ?:"#8F97A1") else Color.parseColor(toolbarProps?.typeActiveTitleColor ?: configurationToolbarProps?.typeActiveTitleColor ?:"#3E77A5"))
        typeText.textSize = toolbarProps?.typeFontSize ?: configurationToolbarProps?.typeFontSize ?: 12f
        typeText.typeface = Typeface.create(toolbarProps?.typeFontFamily ?: configurationToolbarProps?.typeFontFamily ?: "sans-serif", Typeface.NORMAL)

        drawerText.text = toolbarProps?.helpText ?: configurationToolbarProps?.helpText ?: "How can i help?"
        drawerText.setTextColor(Color.parseColor(toolbarProps?.helpTextFontColor ?: configurationToolbarProps?.helpTextFontColor ?: "#8F97A1"))
        drawerText.textSize = toolbarProps?.helpTextFontSize ?: configurationToolbarProps?.helpTextFontSize ?: 18f
        drawerText.typeface = Typeface.create(toolbarProps?.helpTextFontFamily ?: configurationToolbarProps?.helpTextFontFamily ?: "sans-serif", Typeface.NORMAL)

        assistantStateText.textSize = toolbarProps?.assistantStateFontSize ?: configurationToolbarProps?.assistantStateFontSize ?: 16f
        assistantStateText.typeface = Typeface.create(toolbarProps?.assistantStateFontFamily ?: configurationToolbarProps?.assistantStateFontFamily ?: "sans-serif", Typeface.NORMAL)

        spokenText.textSize = 16f
        spokenText.typeface = Typeface.create(toolbarProps?.partialSpeechResultFontFamily ?: configurationToolbarProps?.partialSpeechResultFontFamily ?: "sans-serif", Typeface.NORMAL)
        val spokenTextViewStyle = GradientDrawable()
        spokenTextViewStyle.cornerRadius = 24f
        spokenTextViewStyle.setColor(Color.parseColor(toolbarProps?.speechResultBoxBackgroundColor ?: configurationToolbarProps?.speechResultBoxBackgroundColor ?: "#80000000"))
        spokenText.background = spokenTextViewStyle

        assistantNameText.setTextColor(Color.parseColor(toolbarProps?.assistantStateTextColor ?: configurationToolbarProps?.assistantStateTextColor ?: "#8F97A1"))
        assistantNameText.typeface = Typeface.create(headerProps?.fontFamily ?: configurationHeaderProps?.fontFamily ?: "sans-serif", Typeface.NORMAL)
        assistantNameText.text = headerProps?.assistantName ?: configurationHeaderProps?.assistantName ?: "Voicify Assistant"
        assistantNameText.textSize = headerProps?.fontSize ?: configurationHeaderProps?.fontSize ?: 18f
        assistantNameText.setTextColor(Color.parseColor(headerProps?.assistantNameTextColor ?: configurationHeaderProps?.assistantNameTextColor ?: "#000000"))

        inputTextMessage.hint = toolbarProps?.placeholder ?: configurationToolbarProps?.placeholder ?: "Enter a message..."
        inputTextMessage.typeface = Typeface.create(toolbarProps?.textboxFontFamily ?: configurationToolbarProps?.textboxFontFamily ?: "sans-serif", Typeface.NORMAL)
        inputTextMessage.setCursorDrawableColor(Color.parseColor(toolbarProps?.textInputCursorColor ?: configurationToolbarProps?.textInputCursorColor ?: "#000000"))
        inputTextMessage.setTextColor(Color.parseColor(toolbarProps?.textInputTextColor ?: configurationToolbarProps?.textInputTextColor ?: "#000000"))
        val colorStateList = ColorStateList.valueOf(Color.parseColor(if(isUsingSpeech) {toolbarProps?.textInputLineColor ?: configurationToolbarProps?.textInputLineColor ?: "#000000"} else {toolbarProps?.textInputActiveLineColor ?: configurationToolbarProps?.textInputActiveLineColor ?: "#000000"}))
        ViewCompat.setBackgroundTintList(inputTextMessage,colorStateList)
        val inputTextMessageEditTextViewStyle = GradientDrawable()
        inputTextMessageEditTextViewStyle.setColor(Color.parseColor("#1f1e7eb9"))
        inputTextMessage.textSize = toolbarProps?.textboxFontSize ?: configurationToolbarProps?.textboxFontSize ?: 18f
    }

    private fun initializeImageViews(micImage: ImageView, closeAssistant: ImageView, sendMessage: ImageView, assistantAvatar: ImageView, speakText: TextView){
        Log.d("JAMES", configurationKotlin?.applicationId ?: "null")
        if((assistantSettingProps?.useVoiceInput ?: configurationKotlin?.useVoiceInput) == false)
        {
            micImage.visibility = View.GONE
            speakText.visibility = View.GONE
        }
        else{
            loadImageFromUrl(if((assistantSettingProps?.initializeWithText ?: configurationKotlin?.activeInput == "textbox") != true) toolbarProps?.micActiveImage ?: configurationToolbarProps?.micActiveImage ?: "https://voicify-prod-files.s3.amazonaws.com/99a803b7-5b37-426c-a02e-63c8215c71eb/daca643f-6730-4af5-8817-8d9d0d9db0b5/mic-image.png"
            else toolbarProps?.micInactiveImage ?: configurationToolbarProps?.micInactiveImage ?: "https://voicify-prod-files.s3.amazonaws.com/99a803b7-5b37-426c-a02e-63c8215c71eb/3f10b6d7-eb71-4427-adbc-aadacbe8940e/mic-image-1-.png", micImage,
                if(!(assistantSettingProps?.initializeWithText ?: configurationKotlin?.activeInput == "textbox")) toolbarProps?.micActiveColor ?: configurationToolbarProps?.micActiveColor else toolbarProps?.micInactiveColor ?: configurationToolbarProps?.micInactiveColor)
        }

        loadImageFromUrl(
            headerProps?.closeAssistantButtonImage ?: configurationHeaderProps?.closeAssistantButtonImage
            ?: "https://voicify-prod-files.s3.amazonaws.com/99a803b7-5b37-426c-a02e-63c8215c71eb/a6de04bb-e572-4a55-8cd9-1a7628285829/delete-2.png",
            closeAssistant,
            headerProps?.closeAssistantColor ?: configurationHeaderProps?.closeAssistantColor
        )

        loadImageFromUrl(if(!(assistantSettingProps?.initializeWithText ?: configurationKotlin?.activeInput == "textbox") && assistantSettingProps?.useVoiceInput != true) toolbarProps?.sendInactiveImage ?: configurationToolbarProps?.sendInactiveImage ?: "https://voicify-prod-files.s3.amazonaws.com/99a803b7-5b37-426c-a02e-63c8215c71eb/0c5aa61c-7d6c-4272-abd2-75d9f5771214/Send-2-.png"
        else toolbarProps?.sendActiveImage ?: configurationToolbarProps?.sendActiveImage ?: "https://voicify-prod-files.s3.amazonaws.com/99a803b7-5b37-426c-a02e-63c8215c71eb/7a39bc6f-eef5-4185-bcf8-2a645aff53b2/Send-3-.png", sendMessage,
            if(!(assistantSettingProps?.initializeWithText ?: configurationKotlin?.activeInput == "textbox") && (assistantSettingProps?.useVoiceInput ?: configurationKotlin?.useVoiceInput) == false) toolbarProps?.sendInactiveColor ?: configurationToolbarProps?.sendInactiveColor else toolbarProps?.sendActiveColor ?: configurationToolbarProps?.sendActiveColor)

        loadImageFromUrl(headerProps?.assistantImage ?: configurationHeaderProps?.assistantImage ?: "https://voicify-prod-files.s3.amazonaws.com/99a803b7-5b37-426c-a02e-63c8215c71eb/eb7d2538-a3dc-4304-b58c-06fdb34e9432/Mark-Color-3-.png", assistantAvatar, headerProps?.assistantImageColor ?: configurationHeaderProps?.assistantImageColor)

        val micImageLayoutParams = LinearLayout.LayoutParams(toolbarProps?.micImageWidth ?: configurationToolbarProps?.micImageWidth ?: getPixelsFromDp(48), toolbarProps?.micImageHeight ?: configurationToolbarProps?.micImageHeight ?: getPixelsFromDp(48))
        micImageLayoutParams.setMargins(0,getPixelsFromDp(12),0,0)
        micImage.layoutParams = micImageLayoutParams
        micImage.setPadding(toolbarProps?.micImagePadding ?: configurationToolbarProps?.micImagePadding ?: getPixelsFromDp(4), toolbarProps?.micImagePadding ?: configurationToolbarProps?.micImagePadding ?: getPixelsFromDp(4),toolbarProps?.micImagePadding ?: configurationToolbarProps?.micImagePadding ?: getPixelsFromDp(4),toolbarProps?.micImagePadding ?: configurationToolbarProps?.micImagePadding ?: getPixelsFromDp(4))
    }

    private fun initializeRecyclerViews(messagesRecycler: RecyclerView, hintsRecycler: RecyclerView, messages: ArrayList<Message>, hints: ArrayList<String>, hintClicked: (String) -> Unit){
        messagesRecyclerViewAdapter = MessagesRecyclerViewAdapter(messages, bodyProps, configurationBodyProps, requireContext())
        hintsRecyclerViewAdapter = HintsRecyclerViewAdapter(hints, bodyProps, configurationBodyProps, hintClicked)
        messagesRecycler.layoutManager = LinearLayoutManager(context)
        messagesRecycler.adapter = messagesRecyclerViewAdapter
        hintsRecycler.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        hintsRecycler.adapter = hintsRecyclerViewAdapter
    }

    private fun initializeAssistant(): VoicifyAssistant {
        voicifyTTS = VoicifyTTSProvider(VoicifyTextToSpeechSettings(
            appId = assistantSettingProps?.appId ?: configurationKotlin?.applicationId ?: "",
            appKey = assistantSettingProps?.appKey ?: configurationKotlin?.applicationSecret ?: "",
            voice = assistantSettingProps?.textToSpeechVoice ?: configurationKotlin?.textToSpeechVoice ?: "",
            serverRootUrl = assistantSettingProps?.serverRootUrl ?: "https://assistant.voicify.com/",
            provider = assistantSettingProps?.textToSpeechProvider ?: configurationKotlin?.textToSpeechProvider ?: "Google"))
        voicifySTT = VoicifySTTProvider(requireContext(), requireActivity())
        voicifyTTS?.cancelSpeech = false
        return VoicifyAssistant(voicifySTT, voicifyTTS,
            VoicifyAssistantSettings(
                appId = assistantSettingProps?.appId ?: configurationKotlin?.applicationId ?: "",
                appKey = assistantSettingProps?.appKey ?: configurationKotlin?.applicationSecret ?: "",
                serverRootUrl = assistantSettingProps?.serverRootUrl ?: "",
                locale = assistantSettingProps?.locale ?: configurationKotlin?.locale ?: "en-US",
                channel = assistantSettingProps?.channel ?: configurationKotlin?.channel ?: "Android",
                device = assistantSettingProps?.device ?: configurationKotlin?.device ?: "Mobile",
                noTracking = assistantSettingProps?.noTracking ?: configurationKotlin?.noTracking ?: false,
                autoRunConversation = assistantSettingProps?.autoRunConversation ?: configurationKotlin?.autoRunConversation ?: false,
                initializeWithWelcomeMessage = assistantSettingProps?.initializeWithWelcomeMessage ?: configurationKotlin?.initializeWithWelcomeMessage ?: false,
                initializeWithText = assistantSettingProps?.initializeWithText ?: (configurationKotlin?.activeInput == "textbox"),
                useVoiceInput = assistantSettingProps?.useVoiceInput ?: configurationKotlin?.useVoiceInput ?: true,
                useDraftContent = assistantSettingProps?.useDraftContent ?: configurationKotlin?.useDraftContent ?: false,
                useOutputSpeech = assistantSettingProps?.useOutputSpeech ?: configurationKotlin?.useOutputSpeech ?: true
            )
        )
    }

    private fun checkInitializeWithText(animationLayout: LinearLayout, sendLayoutStyle: GradientDrawable, sendLayout: LinearLayout, spokenText: TextView, assistantStateText: TextView){
        if((assistantSettingProps?.initializeWithText ?: configurationKotlin?.activeInput == "textbox") || (assistantSettingProps?.useVoiceInput ?: configurationKotlin?.useVoiceInput) == false)
        {
            animationLayout.visibility = View.GONE
            sendLayout.background = sendLayoutStyle
            isUsingSpeech = false
            spokenText.visibility = View.GONE
            assistantStateText.visibility = View.GONE
        }
    }

    private fun checkInitializeWithWelcome(drawer: LinearLayout, bodyLayout: LinearLayout, spokenText: TextView, hintsRecycler: RecyclerView,
                                            drawerFooter: LinearLayout, dashedLineView: ImageView, toolbar: LinearLayout,
                                           assistantAvatarBackground: LinearLayout, header: LinearLayout, micImage: ImageView, assistantStateText: TextView,
                                           drawerText: TextView, assistantAvatar: ImageView, assistantName: TextView, messagesRecycler: RecyclerView,
                                            bodyTopBorder: View, bodyBottomBorder: View){
        if((assistantSettingProps?.initializeWithWelcomeMessage ?: configurationKotlin?.initializeWithWelcomeMessage) == true)
        {
                drawer.visibility = View.VISIBLE
                bodyLayout.visibility = View.VISIBLE
                spokenText.text = ""
                hintsRecycler.visibility = View.VISIBLE
                if(!isUsingSpeech)
                {
                    val drawerFooterLayoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    drawerFooterLayoutParams.setMargins(0,0,0,0)
                    drawerFooter.layoutParams = drawerFooterLayoutParams
                    dashedLineView.visibility = View.INVISIBLE;
                }
                drawer.setPadding(0,0,0,0)
                drawer.setBackgroundColor(Color.TRANSPARENT)
                if(!(toolbarProps?.backgroundColor ?: configurationToolbarProps?.backgroundColor).isNullOrEmpty())
                {
                    toolbar.setBackgroundColor(Color.parseColor(toolbarProps?.backgroundColor ?: configurationToolbarProps?.backgroundColor))
                }
                else if ((assistantSettingProps?.backgroundColor ?: configurationKotlin?.styles?.assistant?.backgroundColor).isNullOrEmpty())
                {
                    toolbar.setBackgroundColor(Color.parseColor("#ffffff"))
                }
                toolbar.setPadding(toolbarProps?.paddingLeft ?: configurationToolbarProps?.paddingLeft ?: getPixelsFromDp(16), getPixelsFromDp(0),toolbarProps?.paddingRight ?: configurationToolbarProps?.paddingRight ?: getPixelsFromDp(16),toolbarProps?.paddingBottom ?: configurationToolbarProps?.paddingBottom ?: getPixelsFromDp(16))
                assistantAvatarBackground.visibility = View.VISIBLE
                if(!(headerProps?.backgroundColor ?: configurationHeaderProps?.backgroundColor).isNullOrEmpty()){
                    header.setBackgroundColor(Color.parseColor(headerProps?.backgroundColor ?: configurationHeaderProps?.backgroundColor))
                }
                else if ((assistantSettingProps?.backgroundColor ?: configurationKotlin?.styles?.assistant?.backgroundColor).isNullOrEmpty())
                {
                    header.setBackgroundColor(Color.parseColor("#ffffff"))
                }
                header.setPadding(headerProps?.paddingLeft ?: configurationHeaderProps?.paddingLeft ?: getPixelsFromDp(16), headerProps?.paddingTop ?: configurationHeaderProps?.paddingTop ?: getPixelsFromDp(16), headerProps?.paddingRight ?: configurationHeaderProps?.paddingRight ?: getPixelsFromDp(16), headerProps?.paddingBottom ?: configurationHeaderProps?.paddingBottom ?: getPixelsFromDp(16))
                bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
                isDrawer = false
                micImage.setBackgroundColor(Color.parseColor(toolbarProps?.micInactiveHighlightColor ?: configurationToolbarProps?.micInactiveHighlightColor ?: "#00ffffff"))
                val metrics = activity?.resources?.displayMetrics
                var params = drawerLayout.layoutParams
                params.height = metrics?.heightPixels as Int
                drawer.layoutParams = params
                assistantStateText.text = ""
                drawerText.text = ""
                assistantAvatar.visibility = View.VISIBLE
                assistantName.visibility = View.VISIBLE
                messagesRecycler.visibility = View.VISIBLE
                bodyTopBorder.visibility = View.VISIBLE
                bodyBottomBorder.visibility = View.VISIBLE
                messagesRecyclerViewAdapter?.notifyDataSetChanged()
                if(messagesRecyclerViewAdapter?.itemCount ?: 0 > 0)
                {
                    messagesRecycler.smoothScrollToPosition(messagesRecyclerViewAdapter?.itemCount as Int);
                }

        }
    }
    private fun startNewAssistantSession(assistant: VoicifyAssistant){
        if(!(assistantSettingProps?.locale ?: configurationKotlin?.locale).toString().isNullOrEmpty())
        {
            voicifySTT?.initialize((assistantSettingProps?.locale ?: configurationKotlin?.locale).toString())
        }
        assistant.initializeAndStart()
        assistant.startNewSession(null, null, this.sessionAttributes, this.userAttributes)
        if((assistantSettingProps?.initializeWithText ?: configurationKotlin?.activeInput == "textbox") != true && (assistantSettingProps?.useVoiceInput ?: configurationKotlin?.useVoiceInput) != false && (assistantSettingProps?.initializeWithWelcomeMessage ?: configurationKotlin?.initializeWithWelcomeMessage) != true)
        {
            voicifySTT?.startListening()
        }
    }

    private fun addAssistantHandlers(assistant: VoicifyAssistant, drawer: LinearLayout, bodyLayout: LinearLayout, spokenText: TextView, hintsRecycler: RecyclerView,
                                     closeAssistant: ImageView, closeAssistantNoInternet: ImageView, hintsList: ArrayList<String>, drawerText: TextView,
                                    drawerFooter: LinearLayout, dashedLineView: ImageView, messagesList: ArrayList<Message>, messagesRecycler: RecyclerView,
                                    toolbar: LinearLayout, header: LinearLayout, avatarBackground: LinearLayout, micImage: ImageView, assistantStateText: TextView,
                                    avatarImage: ImageView, assistantName: TextView, bodyTopBorder: View, bodyBottomBorder: View){
        assistant.onResponseReceived { response ->
            activity?.runOnUiThread{
                drawer.visibility = View.VISIBLE
                bodyLayout.visibility = View.VISIBLE
                spokenText.text = ""
                hintsRecycler.visibility = View.VISIBLE
                closeAssistant.visibility = View.VISIBLE
                closeAssistantNoInternet.visibility = View.GONE
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
                    hintsRecycler.visibility = View.GONE
                }
                if(!isUsingSpeech)
                {
                    val drawerFooterLayoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    drawerFooterLayoutParams.setMargins(0,0,0,0)
                    drawerFooter.layoutParams = drawerFooterLayoutParams
                    dashedLineView.visibility = View.INVISIBLE;
                }
                if(!speechFullResult.isNullOrEmpty())
                {
                    messagesList.add(Message(speechFullResult as String, "Sent"))
                    messagesRecyclerViewAdapter?.notifyDataSetChanged()
                    messagesRecycler.smoothScrollToPosition(messagesRecyclerViewAdapter?.itemCount as Int);
                }
                speechFullResult = null
                drawer.setPadding(0,0,0,0)
                drawer.setBackgroundColor(Color.TRANSPARENT)
                if(!(toolbarProps?.backgroundColor ?: configurationToolbarProps?.backgroundColor).isNullOrEmpty())
                {
                    toolbar.setBackgroundColor(Color.parseColor(toolbarProps?.backgroundColor ?: configurationToolbarProps?.backgroundColor))
                }
                else if ((assistantSettingProps?.backgroundColor ?: configurationKotlin?.styles?.assistant?.backgroundColor).isNullOrEmpty())
                {
                    toolbar.setBackgroundColor(Color.parseColor("#ffffff"))
                }
                toolbar.setPadding(toolbarProps?.paddingLeft ?: configurationToolbarProps?.paddingLeft ?: getPixelsFromDp(16), getPixelsFromDp(0),toolbarProps?.paddingRight ?: configurationToolbarProps?.paddingRight ?: getPixelsFromDp(16),toolbarProps?.paddingBottom ?: configurationToolbarProps?.paddingBottom ?: getPixelsFromDp(16))
                avatarBackground.visibility = View.VISIBLE
                if(!(headerProps?.backgroundColor ?: configurationHeaderProps?.backgroundColor).isNullOrEmpty()){
                    header.setBackgroundColor(Color.parseColor(headerProps?.backgroundColor ?: configurationHeaderProps?.backgroundColor))
                }
                else if ((assistantSettingProps?.backgroundColor ?: configurationKotlin?.styles?.assistant?.backgroundColor).isNullOrEmpty())
                {
                    header.setBackgroundColor(Color.parseColor("#ffffff"))
                }
                header.setPadding(headerProps?.paddingLeft ?: configurationHeaderProps?.paddingLeft ?: getPixelsFromDp(16), headerProps?.paddingTop ?: configurationHeaderProps?.paddingTop ?: getPixelsFromDp(16), headerProps?.paddingRight ?: configurationHeaderProps?.paddingRight ?: getPixelsFromDp(16), headerProps?.paddingBottom ?: configurationHeaderProps?.paddingBottom ?: getPixelsFromDp(16))
                bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
                isDrawer = false
                micImage.setBackgroundColor(Color.parseColor(toolbarProps?.micInactiveHighlightColor ?: configurationToolbarProps?.micInactiveHighlightColor ?: "#00ffffff"))
                val metrics = activity?.resources?.displayMetrics
                var params = drawerLayout.layoutParams
                params.height = metrics?.heightPixels as Int
                drawer.layoutParams = params
                assistantStateText.text = ""
                drawerText.text = ""
                avatarImage.visibility = View.VISIBLE
                assistantName.visibility = View.VISIBLE
                messagesRecycler.visibility = View.VISIBLE
                bodyTopBorder.visibility = View.VISIBLE
                bodyBottomBorder.visibility = View.VISIBLE
                messagesList.add(Message(response.displayText?.trim() as String, "Received"))
                messagesRecyclerViewAdapter?.notifyDataSetChanged()
                messagesRecycler.smoothScrollToPosition(messagesRecyclerViewAdapter?.itemCount as Int);
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

        // handle errors"Assistant Call Failed"
        assistant.onError{ errorMessage, request ->
            activity?.runOnUiThread{
                if(errorMessage == "Assistant Call Failed")
                {
                    closeAssistantImageView.visibility = View.GONE
                    closeAssistantNoInternetImageView.visibility = View.VISIBLE
                }
            }

            if(onAssistantErrorCallback != null)
            {
                this.onAssistantErrorCallback?.invoke(errorMessage, request)
            }
        }
    }

    private fun addSpeechToTextListeners(assistant: VoicifyAssistant, spokenText: TextView, assistantStateText: TextView, micImage: ImageView){
        voicifySTT?.addPartialListener { partialResult ->
            spokenText.setTextColor(Color.parseColor(toolbarProps?.partialSpeechResultTextColor ?: configurationToolbarProps?.partialSpeechResultTextColor ?: "#33ffffff"))
            spokenText.text = partialResult
        }
        voicifySTT?.addFinalResultListener { fullResult ->
            spokenText.setTextColor(Color.parseColor(toolbarProps?.fullSpeechResultTextColor ?: configurationToolbarProps?.fullSpeechResultTextColor ?: "#ffffff"))
            speechFullResult = fullResult
            clearAnimationValues()
            assistantIsListening = false
            spokenText.text = fullResult
            assistantStateText.text = "Processing..."
            assistant.makeTextRequest(fullResult.toString(), null, "Speech")
        }

        voicifySTT?.addSpeechReadyListener {
            val micImageViewStyle = GradientDrawable()
            micImageViewStyle.setColor(Color.parseColor(toolbarProps?.micActiveHighlightColor ?: configurationToolbarProps?.micActiveHighlightColor ?: "#1f1e7eb9"))
            micImageViewStyle.setStroke(toolbarProps?.micImageBorderWidth ?: configurationToolbarProps?.micImageBorderWidth ?: 0, Color.parseColor(toolbarProps?.micImageBorderColor ?: configurationToolbarProps?.micImageBorderColor ?: "#00ffffff"))
            micImageViewStyle.cornerRadius = toolbarProps?.micBorderRadius ?: configurationToolbarProps?.micBorderRadius ?: 100f
            micImage.background = micImageViewStyle
            assistantIsListening = true;
            assistantStateText.text = "Listening..."
        }
        voicifySTT?.addErrorListener { error ->
            clearAnimationValues()
            if (error == SpeechRecognizer.ERROR_NO_MATCH.toString())
            {
                assistantIsListening = false
                micImage.setBackgroundColor(Color.parseColor(toolbarProps?.micInactiveHighlightColor ?: configurationToolbarProps?.micInactiveHighlightColor ?: "#00ffffff"))
                assistantStateText.text = "I didn't catch that..."
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
    }

    private fun addMicClickListener(micImage: ImageView, messagesRecycler: RecyclerView, speakingAnimationLayout: LinearLayout, sendLayout: LinearLayout, dashedLineView: ImageView,
                                    drawerFooter: LinearLayout, spokenText: TextView, assistantStateText: TextView, speakText: TextView, typeText: TextView,
                                    sendMessage: ImageView){
        micImage.setOnClickListener{
            clearAnimationValues()
            val colorStateList = ColorStateList.valueOf(Color.parseColor(toolbarProps?.textInputLineColor ?: configurationToolbarProps?.textInputLineColor ?: "#000000"))
            ViewCompat.setBackgroundTintList(inputTextMessage,colorStateList)
            if(!isUsingSpeech)
            {
                voicifySTT?.cancel = false
                isUsingSpeech = true
                messagesRecyclerViewAdapter?.notifyDataSetChanged()
                messagesRecycler.smoothScrollToPosition(messagesRecyclerViewAdapter?.itemCount as Int);
                speakingAnimationLayout.visibility = View.VISIBLE
                sendLayout.setBackgroundColor(Color.parseColor(toolbarProps?.textboxInactiveHighlightColor ?: configurationToolbarProps?.textboxInactiveHighlightColor ?: "#00ffffff"))
                dashedLineView.visibility = View.VISIBLE;
                hideKeyboard()
                if(!isDrawer){
                    val drawerFooterLayoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    drawerFooterLayoutParams.setMargins(0,getPixelsFromDp(20),0,0)
                    drawerFooter.layoutParams = drawerFooterLayoutParams
                }
                spokenText.visibility = View.VISIBLE
                assistantStateText.visibility = View.VISIBLE
                speakText.setTextColor(Color.parseColor(toolbarProps?.speakActiveTitleColor ?: configurationToolbarProps?.speakActiveTitleColor ?: "#3E77A5"))
                typeText.setTextColor(Color.parseColor(toolbarProps?.typeInactiveTitleColor ?: configurationToolbarProps?.speakInactiveTitleColor ?: "#8F97A1"))
                loadImageFromUrl(toolbarProps?.micActiveImage ?: configurationToolbarProps?.micActiveImage ?: "https://voicify-prod-files.s3.amazonaws.com/99a803b7-5b37-426c-a02e-63c8215c71eb/daca643f-6730-4af5-8817-8d9d0d9db0b5/mic-image.png", micImage, toolbarProps?.micActiveColor ?: configurationToolbarProps?.micActiveColor)
                loadImageFromUrl(toolbarProps?.sendInactiveImage ?: configurationToolbarProps?.sendInactiveImage ?: "https://voicify-prod-files.s3.amazonaws.com/99a803b7-5b37-426c-a02e-63c8215c71eb/0c5aa61c-7d6c-4272-abd2-75d9f5771214/Send-2-.png", sendMessage, toolbarProps?.sendInactiveColor ?: configurationToolbarProps?.sendInactiveColor)
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
    }

    private fun addSendMessageClickListener(sendMessage: ImageView, inputTextMessage: EditText, messagesList: ArrayList<Message>, messagesRecycler: RecyclerView, assistant: VoicifyAssistant) {
        sendMessage.setOnClickListener{
            if(inputTextMessage.text.toString().isNotEmpty())
            {
                messagesList.add(Message(inputTextMessage.text.toString(), "Sent"))
                messagesRecyclerViewAdapter?.notifyDataSetChanged()
                messagesRecycler.smoothScrollToPosition(messagesRecyclerViewAdapter?.itemCount as Int);
                val inputText = inputTextMessage.text.toString()
                inputTextMessage.setText("")
                hideKeyboard()
                assistant.makeTextRequest(inputText,null, "Text")
            }
        }
    }

    private fun addTextboxClickListener(inputTextMessage: EditText, assistantStateText: TextView, spokenText: TextView, speakingAnimationLayout: LinearLayout,
                                        sendTextLayoutStyle: GradientDrawable, sendLayout: LinearLayout, drawerFooter: LinearLayout, dashedLineView: ImageView,
                                        speakText: TextView, typeText: TextView, micImage: ImageView, sendMessage: ImageView) {
        inputTextMessage.setOnTouchListener(object : OnTouchListener {
            @SuppressLint("ClickableViewAccessibility")
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when(event?.action) {
                    MotionEvent.ACTION_UP -> {
                        val colorStateList = ColorStateList.valueOf(Color.parseColor(toolbarProps?.textInputActiveLineColor ?: configurationToolbarProps?.textInputActiveLineColor ?: "#000000"))
                        ViewCompat.setBackgroundTintList(inputTextMessage,colorStateList)
                        if (assistantIsListening) {
                            cancelSpeech()
                            assistantStateText.text = ""
                            spokenText.text = ""
                        }
                        if (isUsingSpeech) {
                            voicifySTT?.cancel = true
                            speakingAnimationLayout.visibility = View.GONE
                            sendLayout.background = sendTextLayoutStyle
                            isUsingSpeech = false
                            if(!isDrawer)
                            {
                                val drawerFooterLayoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                                drawerFooterLayoutParams.setMargins(0,0,0,0)
                                drawerFooter.layoutParams = drawerFooterLayoutParams
                                dashedLineView.visibility = View.INVISIBLE;
                            }
                            spokenText.visibility = View.GONE
                            assistantStateText.visibility = View.GONE
                            speakText.setTextColor(Color.parseColor(toolbarProps?.speakInactiveTitleColor ?: configurationToolbarProps?.speakInactiveTitleColor ?: "#8F97A1"))
                            typeText.setTextColor(Color.parseColor(toolbarProps?.typeActiveTitleColor ?: configurationToolbarProps?.speakActiveTitleColor ?: "#3E77A5"))
                            loadImageFromUrl(
                                toolbarProps?.micInactiveImage ?: configurationToolbarProps?.micInactiveImage ?: "https://voicify-prod-files.s3.amazonaws.com/99a803b7-5b37-426c-a02e-63c8215c71eb/3f10b6d7-eb71-4427-adbc-aadacbe8940e/mic-image-1-.png",
                                micImage,
                                toolbarProps?.micInactiveColor ?: configurationToolbarProps?.micInactiveColor
                            )
                            loadImageFromUrl(
                                toolbarProps?.sendActiveImage ?: configurationToolbarProps?.sendActiveImage ?: "https://voicify-prod-files.s3.amazonaws.com/99a803b7-5b37-426c-a02e-63c8215c71eb/7a39bc6f-eef5-4185-bcf8-2a645aff53b2/Send-3-.png",
                                sendMessage,
                                toolbarProps?.sendActiveColor ?: configurationToolbarProps?.sendActiveColor
                            )
                        }
                    }
                }
                v?.performClick()
                return v?.onTouchEvent(event) ?: true
            }
        })
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
        micImageView.setBackgroundColor(Color.parseColor(toolbarProps?.micInactiveHighlightColor ?: configurationToolbarProps?.micInactiveHighlightColor ?: "#00ffffff"))
        spokenTextView.text =  ""
    }

    private fun addKeyboardActiveListener(window: View){
        window!!.viewTreeObserver.addOnGlobalLayoutListener {
            val r = Rect()
            window.getWindowVisibleDisplayFrame(r)
            val heightDiff = window.rootView.height - (r.bottom - r.top)
            if (heightDiff > 500) {
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
    }

    private fun addGradientBackground(containerLayout: LinearLayout){
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
        else if (!configurationKotlin?.styles?.assistant?.backgroundColor.isNullOrEmpty()){
            val splitColors = configurationKotlin?.styles?.assistant?.backgroundColor?.split(",")
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
                containerLayout.setBackgroundColor(Color.parseColor(configurationKotlin?.styles?.assistant?.backgroundColor))
            }
        }
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

    fun onAssistantError(callback: (errorMessage: String, request: CustomAssistantRequest) -> Unit){
        this.onAssistantErrorCallback = callback
    }

    fun addUserAttributes(userAttributes: Map<String, Any>){
        this.userAttributes = userAttributes
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param assistantSettingsProperties Parameter 1.
         * @param headerProps Parameter 2.
         * @param bodyProps Parameter 3.
         * @param toolbarProps Parameter 4.
         * @return A new instance of fragment AssistantDrawerUI.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance( assistantSettingsProperties: AssistantSettingsProps, headerProps: HeaderProps? = null, bodyProps: BodyProps? = null, toolbarProps: ToolbarProps? = null) =
            AssistantDrawerUI().apply {
                arguments = Bundle().apply {
                    putSerializable(SETTINGS, assistantSettingsProperties)
                    putSerializable(HEADER, headerProps)
                    putSerializable(BODY, bodyProps)
                    putSerializable(TOOLBAR, toolbarProps)
                }
                if(configurationKotlin == null)
                {
                    val coroutineExceptionHandler = CoroutineExceptionHandler{_, throwable ->
                        configurationKotlin = null
                        isLoadingConfiguration = false
                        NotificationCenter.postNotification(requireContext(), NotificationType.LOADING_COMPLETE);
                        Log.d("JAMES","here???")
                    }

                    GlobalScope.async (Dispatchers.IO + coroutineExceptionHandler){
                        try{
                            configurationKotlin = customAssistantConfigurationService.getCustomAssistantConfiguration(
                                assistantSettingsProperties?.configurationId ?: "",
                                assistantSettingsProperties?.serverRootUrl ?: "",
                                assistantSettingsProperties?.appId ?: "",
                                assistantSettingsProperties?.appKey ?: ""
                            )
                            configurationHeaderProps = configurationKotlin?.styles?.header
                            configurationBodyProps = configurationKotlin?.styles?.body
                            configurationToolbarProps = configurationKotlin?.styles?.toolbar
                            isLoadingConfiguration = false
                            Log.d("JAMES", "we are in the async method oh boy")
                            NotificationCenter.postNotification(requireContext(), NotificationType.LOADING_COMPLETE);
                        }
                        catch(e: Exception){
                            NotificationCenter.postNotification(requireContext(), NotificationType.LOADING_COMPLETE)
                        }
                    }
                }
            }
    }
}