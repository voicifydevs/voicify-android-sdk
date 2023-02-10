package com.voicify.voicify_assistant_sdk.components

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.*
import android.content.Context.MODE_PRIVATE
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.VectorDrawable
import android.os.Build
import android.os.Bundle
import android.speech.SpeechRecognizer
import android.view.*
import android.view.View.OnTouchListener
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.annotation.ColorInt
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
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
import com.voicify.voicify_assistant_sdk.assistant.*
import com.voicify.voicify_assistant_sdk.assistantDrawerUITypes.*
import com.voicify.voicify_assistant_sdk.components.body.AssistantDrawerUIBody
import com.voicify.voicify_assistant_sdk.components.body.HintsRecyclerViewAdapter
import com.voicify.voicify_assistant_sdk.components.body.MessagesRecyclerViewAdapter
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
    private val gson = Gson()
    private var isRotated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            assistantSettingProps = it.getSerializable(SETTINGS) as AssistantSettingsProps?
            headerProps = it.getSerializable(HEADER) as HeaderProps?
            bodyProps = it.getSerializable(BODY) as BodyProps?
            toolbarProps = it.getSerializable(TOOLBAR) as ToolbarProps?
        }
        isDrawer = assistantSettingProps?.initializeWithWelcomeMessage != true
        if(configurationKotlin == null && !assistantSettingProps?.configurationId.isNullOrEmpty() && !isLoadingConfiguration)
        {
            // if the configuration call returns null, but the configuration id is specified, try to grab the config from shared preferences
            val prefs = requireActivity().getSharedPreferences(CONFIGURATION, MODE_PRIVATE)
            if(prefs != null)
            {
                val preferenceConfig = prefs.getString(CONFIGURATION_KOTLIN ,"")
                if (!preferenceConfig.isNullOrEmpty())
                {
                    configurationKotlin = gson.fromJson(preferenceConfig, CustomAssistantConfigurationResponse::class.java)
                    configurationHeaderProps = configurationKotlin?.styles?.header
                    configurationBodyProps = configurationKotlin?.styles?.body
                    configurationToolbarProps = configurationKotlin?.styles?.toolbar
                }
            }
        }
        else{
            val editor = requireActivity().getSharedPreferences(CONFIGURATION, MODE_PRIVATE).edit()
            editor.putString(CONFIGURATION_KOTLIN, gson.toJson(configurationKotlin))
            editor.apply()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val window = inflater.inflate(R.layout.fragment_assistant_drawer_u_i, container, false)
        val hintsList = ArrayList<String>()
        val messagesList = ArrayList<Message>()
        val assistantDrawerUIHeader = AssistantDrawerUIHeader(
            context = requireContext(),
            headerProps = headerProps,
            configurationHeaderProps = configurationHeaderProps,
            resources = resources
        )
        val assistantDrawerUIBody = AssistantDrawerUIBody(
            context = requireContext(),
            bodyProps = bodyProps,
            configurationBodyProps = configurationBodyProps
        )
        scale = requireContext().resources.displayMetrics.density
        isUsingSpeech = (assistantSettingProps?.initializeWithText ?: configurationKotlin?.activeInput == getString(R.string.textbox)) != true && (assistantSettingProps?.useVoiceInput ?: configurationKotlin?.useVoiceInput) != false

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
        activityIndicator.setBackgroundColor(Color.parseColor(getString(R.string.black_60_percent)))

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
        sendTextLayoutStyle.setColor(Color.parseColor(toolbarProps?.textboxActiveHighlightColor ?: configurationToolbarProps?.textboxActiveHighlightColor ?: getString(R.string.blue_12_percent)))
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
            assistantDrawerUIHeader.initializeHeader(
                closeAssistantImageView = closeAssistantImageView,
                avatarImageView = assistantAvatarImageView,
                closeBackgroundLayout =  closeAssistantBackground,
                avatarBackgroundLayout = assistantAvatarBackground,
                assistantNameTextView = assistantNameTextView
            )
            assistantDrawerUIBody.initializeBody(
                bodyBorderTopView = bodyBorderTopView,
                bodyBorderBottomView = bodyBorderBottomView
            )
            containerLayout.visibility = View.VISIBLE
            activityIndicator.visibility = View.GONE
            val assistant = initializeAssistant()
            val onHintClicked: (String) -> Unit = {  hint ->
                messagesList.add(Message(hint, getString(R.string.sent)))
                clearAnimationValues()
                messagesRecyclerViewAdapter?.notifyDataSetChanged()
                messagesRecyclerView.smoothScrollToPosition(messagesRecyclerViewAdapter?.itemCount as Int)
                hideKeyboard()
                hintsList.clear()
                cancelSpeech()
                voicifyTTS?.stop()
                hintsRecyclerViewAdapter?.notifyDataSetChanged()
                assistant.makeTextRequest(hint ,null, getString(R.string.text))
            }

            //UI Initialization
            addGradientBackground(containerLayout)
            initializeImageViews(micImageView, sendMessageImageView, speakTextView)
            initializeLinearLayouts(drawerLayout, bodyContainerLayout)
            checkInitializeWithText(speakingAnimationLayout, sendTextLayoutStyle, sendTextLayout, spokenTextView, assistantStateTextView)
            initializeRecyclerViews(messagesRecyclerView, hintsRecyclerView, messagesList, hintsList, onHintClicked)
            initializeViews(speakingAnimationBars)
            initializeTextViews(speakTextView, typeTextView, drawerWelcomeTextView, spokenTextView, assistantStateTextView, inputTextMessageEditTextView)

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
                    containerLayout.visibility = View.VISIBLE
                    activityIndicator.visibility = View.GONE
                    val assistant = initializeAssistant()
                    val onHintClicked: (String) -> Unit = {  hint ->
                        messagesList.add(Message(hint, getString(R.string.sent)))
                        clearAnimationValues()
                        messagesRecyclerViewAdapter?.notifyDataSetChanged()
                        messagesRecyclerView.smoothScrollToPosition(messagesRecyclerViewAdapter?.itemCount as Int)
                        hideKeyboard()
                        hintsList.clear()
                        cancelSpeech()
                        voicifyTTS?.stop()
                        hintsRecyclerViewAdapter?.notifyDataSetChanged()
                        assistant.makeTextRequest(hint ,null, getString(R.string.text))
                    }

                    addGradientBackground(containerLayout)
                    initializeImageViews(micImageView, sendMessageImageView, speakTextView)
                    initializeLinearLayouts(drawerLayout, bodyContainerLayout)
                    checkInitializeWithText(speakingAnimationLayout, sendTextLayoutStyle, sendTextLayout, spokenTextView, assistantStateTextView)
                    initializeRecyclerViews(messagesRecyclerView, hintsRecyclerView, messagesList, hintsList, onHintClicked)
                    initializeViews(speakingAnimationBars)
                    initializeTextViews(speakTextView, typeTextView, drawerWelcomeTextView, spokenTextView, assistantStateTextView, inputTextMessageEditTextView)

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
        NotificationCenter.addObserver(requireContext(), NotificationType.LOADING_COMPLETE, loginResponseReceiver)
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
        bottomSheetBehavior?.maxWidth = ViewGroup.LayoutParams.MATCH_PARENT
        bottomSheetBehavior?.peekHeight = (getPixelsFromDp(500))
        if(!isLoadingConfiguration) {
            checkInitializeWithWelcome(drawerLayout, bodyContainerLayout, spokenTextView, hintsRecyclerView,
                drawerFooterLayout, dashedLineImageView, toolbarLayout, assistantAvatarBackgroundContainerLayout,
                headerLayout, micImageView, assistantStateTextView, drawerWelcomeTextView, assistantAvatarImageView,
                assistantNameTextView, messagesRecyclerView, bodyBorderTopView, bodyBorderBottomView
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
        transaction.commitAllowingStateLoss()
    }

    override fun onDestroy() {
        super.onDestroy()
        NotificationCenter.removeObserver(requireContext(), loginResponseReceiver)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val orientation = newConfig.orientation
        if (orientation == Configuration.ORIENTATION_PORTRAIT)
        {
            hideKeyboard()
            if(isDrawer)
            {
                val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                drawerLayout.layoutParams = layoutParams
            }
            else{
                val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, getPixelsFromDp(0))
                layoutParams.weight = 1f
                bodyContainerLayout.layoutParams = layoutParams
                val metrics = activity?.resources?.displayMetrics
                val params = drawerLayout.layoutParams
                params.height = metrics?.heightPixels as Int
                drawerLayout.layoutParams = params
            }

            isRotated = false
        }
        else if (orientation == Configuration.ORIENTATION_LANDSCAPE)
        {
            hideKeyboard()
            if(isDrawer)
            {
                bottomSheetBehavior?.peekHeight = (getPixelsFromDp(500))
            }
            else{
                if(messagesRecyclerViewAdapter?.itemCount ?: 0 > 0)
                {
                    messagesRecyclerView.smoothScrollToPosition(messagesRecyclerViewAdapter?.itemCount as Int)
                }
                val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, getPixelsFromDp(if(isUsingSpeech) {50} else {200}))
                layoutParams.weight = 0f
                bodyContainerLayout.layoutParams = layoutParams
            }
            isRotated = true
        }
    }

    private fun initializeLinearLayouts(drawer: LinearLayout, bodyLayout: LinearLayout){
        if(!(toolbarProps?.backgroundColor ?: configurationToolbarProps?.backgroundColor).isNullOrEmpty()){
            drawer.setBackgroundColor(Color.parseColor(toolbarProps?.backgroundColor ?: configurationToolbarProps?.backgroundColor))
        }
        else if ((assistantSettingProps?.backgroundColor ?: configurationKotlin?.styles?.assistant?.backgroundColor).isNullOrEmpty())
        {
            drawer.setBackgroundColor(Color.parseColor(getString(R.string.white)))
        }

        drawer.setPadding(toolbarProps?.paddingLeft ?: configurationToolbarProps?.paddingLeft ?: getPixelsFromDp(16),toolbarProps?.paddingTop ?: configurationToolbarProps?.paddingTop ?: getPixelsFromDp(16),toolbarProps?.paddingRight ?: configurationToolbarProps?.paddingRight ?: getPixelsFromDp(16),toolbarProps?.paddingBottom ?: configurationToolbarProps?.paddingBottom ?: getPixelsFromDp(16))

        val bodyContainerLayoutStyle = GradientDrawable()
        if(!(bodyProps?.backgroundColor ?: configurationBodyProps?.backgroundColor).isNullOrEmpty()){
            bodyContainerLayoutStyle.setColor(Color.parseColor(bodyProps?.backgroundColor ?: configurationBodyProps?.backgroundColor))
        }
        else if ((assistantSettingProps?.backgroundColor ?: configurationKotlin?.styles?.assistant?.backgroundColor).isNullOrEmpty())
        {
            bodyContainerLayoutStyle.setColor(Color.parseColor(getString(R.string.light_gray)))
        }
        bodyLayout.background = bodyContainerLayoutStyle
        bodyLayout.setPadding(
            bodyProps?.paddingLeft ?: configurationBodyProps?.paddingLeft ?: 20,
            bodyProps?.paddingTop ?: configurationBodyProps?.paddingTop ?: 0,
            bodyProps?.paddingRight ?: configurationBodyProps?.paddingRight ?: 20,
            bodyProps?.paddingBottom ?: configurationBodyProps?.paddingBottom ?: 0
        )
    }

    private fun initializeViews(animationBars: Array<View>){
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
                bar.setBackgroundColor(Color.parseColor(getString(R.string.black_50_percent)))
            }
        }
    }

    private fun initializeTextViews(speakText: TextView, typeText: TextView, drawerText: TextView, spokenText: TextView, assistantStateText: TextView, inputTextMessage: EditText){
        speakText.setTextColor(if((assistantSettingProps?.initializeWithText ?: configurationKotlin?.activeInput == getString(R.string.textbox)) != true && (assistantSettingProps?.useVoiceInput ?: configurationKotlin?.useVoiceInput) != false) Color.parseColor(toolbarProps?.speakActiveTitleColor ?: configurationToolbarProps?.speakActiveTitleColor ?: getString(R.string.dark_blue)) else Color.parseColor(toolbarProps?.speakInactiveTitleColor ?: configurationToolbarProps?.speakInactiveTitleColor ?:getString(R.string.dark_gray)))
        speakText.textSize = toolbarProps?.speakFontSize ?: configurationToolbarProps?.speakFontSize ?: 12f
        speakText.typeface = Typeface.create(toolbarProps?.speakFontFamily ?: configurationToolbarProps?.speakFontFamily ?: getString(R.string.default_font), Typeface.NORMAL)

        typeText.setTextColor(if((assistantSettingProps?.initializeWithText ?: configurationKotlin?.activeInput == getString(R.string.textbox)) != true && (assistantSettingProps?.useVoiceInput ?: configurationKotlin?.useVoiceInput) != false) Color.parseColor(toolbarProps?.typeInactiveTitleColor ?: configurationToolbarProps?.typeInactiveTitleColor ?:getString(R.string.dark_gray)) else Color.parseColor(toolbarProps?.typeActiveTitleColor ?: configurationToolbarProps?.typeActiveTitleColor ?:getString(R.string.dark_blue)))
        typeText.textSize = toolbarProps?.typeFontSize ?: configurationToolbarProps?.typeFontSize ?: 12f
        typeText.typeface = Typeface.create(toolbarProps?.typeFontFamily ?: configurationToolbarProps?.typeFontFamily ?: getString(R.string.default_font), Typeface.NORMAL)

        drawerText.text = toolbarProps?.helpText ?: configurationToolbarProps?.helpText ?: getString(R.string.drawer_welcome_text)
        drawerText.setTextColor(Color.parseColor(toolbarProps?.helpTextFontColor ?: configurationToolbarProps?.helpTextFontColor ?: getString(R.string.dark_gray)))
        drawerText.textSize = toolbarProps?.helpTextFontSize ?: configurationToolbarProps?.helpTextFontSize ?: 18f
        drawerText.typeface = Typeface.create(toolbarProps?.helpTextFontFamily ?: configurationToolbarProps?.helpTextFontFamily ?: getString(R.string.default_font), Typeface.NORMAL)

        assistantStateText.setTextColor(Color.parseColor(toolbarProps?.assistantStateTextColor ?: configurationToolbarProps?.assistantStateTextColor ?: getString(R.string.dark_gray)))
        assistantStateText.textSize = toolbarProps?.assistantStateFontSize ?: configurationToolbarProps?.assistantStateFontSize ?: 16f
        assistantStateText.typeface = Typeface.create(toolbarProps?.assistantStateFontFamily ?: configurationToolbarProps?.assistantStateFontFamily ?: getString(R.string.default_font), Typeface.NORMAL)

        spokenText.textSize = 16f
        spokenText.typeface = Typeface.create(toolbarProps?.partialSpeechResultFontFamily ?: configurationToolbarProps?.partialSpeechResultFontFamily ?: getString(R.string.default_font), Typeface.NORMAL)
        val spokenTextViewStyle = GradientDrawable()
        spokenTextViewStyle.cornerRadius = 24f
        spokenTextViewStyle.setColor(Color.parseColor(toolbarProps?.speechResultBoxBackgroundColor ?: configurationToolbarProps?.speechResultBoxBackgroundColor ?: getString(R.string.black_50_percent)))
        spokenText.background = spokenTextViewStyle

        inputTextMessage.hint = toolbarProps?.placeholder ?: configurationToolbarProps?.placeholder ?: getString(R.string.textbox_placeholder_text)
        inputTextMessage.typeface = Typeface.create(toolbarProps?.textboxFontFamily ?: configurationToolbarProps?.textboxFontFamily ?: getString(R.string.default_font), Typeface.NORMAL)
        inputTextMessage.setCursorDrawableColor(Color.parseColor(toolbarProps?.textInputCursorColor ?: configurationToolbarProps?.textInputCursorColor ?: getString(R.string.dark_light_gray)))
        inputTextMessage.setTextColor(Color.parseColor(toolbarProps?.textInputTextColor ?: configurationToolbarProps?.textInputTextColor ?: getString(R.string.black)))
        val colorStateList = ColorStateList.valueOf(Color.parseColor(if(isUsingSpeech) {toolbarProps?.textInputLineColor ?: configurationToolbarProps?.textInputLineColor ?: getString(R.string.silver)} else {toolbarProps?.textInputActiveLineColor ?: configurationToolbarProps?.textInputLineColor ?:getString(R.string.silver)}))
        ViewCompat.setBackgroundTintList(inputTextMessage,colorStateList)
        val inputTextMessageEditTextViewStyle = GradientDrawable()
        inputTextMessageEditTextViewStyle.setColor(Color.parseColor(getString(R.string.blue_12_percent)))
        inputTextMessage.textSize = toolbarProps?.textboxFontSize ?: configurationToolbarProps?.textboxFontSize ?: 18f
    }

    private fun initializeImageViews(micImage: ImageView,  sendMessage: ImageView, speakText: TextView){
        if((assistantSettingProps?.useVoiceInput ?: configurationKotlin?.useVoiceInput) == false)
        {
            micImage.visibility = View.GONE
            speakText.visibility = View.GONE
        }
        else{
            loadImageFromUrl(if((assistantSettingProps?.initializeWithText ?: configurationKotlin?.activeInput == getString(R.string.textbox)) != true) toolbarProps?.micActiveImage ?: configurationToolbarProps?.micActiveImage ?: getString(R.string.mic_active_image)
            else toolbarProps?.micInactiveImage ?: configurationToolbarProps?.micInactiveImage ?: getString(R.string.mic_inactive_image), micImage,
                if(!(assistantSettingProps?.initializeWithText ?: configurationKotlin?.activeInput == getString(R.string.textbox))) toolbarProps?.micActiveColor ?: configurationToolbarProps?.micActiveColor else toolbarProps?.micInactiveColor ?: configurationToolbarProps?.micInactiveColor)
        }



        loadImageFromUrl(if(!(assistantSettingProps?.initializeWithText ?: configurationKotlin?.activeInput == getString(R.string.textbox)) && assistantSettingProps?.useVoiceInput != true) toolbarProps?.sendInactiveImage ?: configurationToolbarProps?.sendInactiveImage ?: getString(R.string.send_inactive_image)
        else toolbarProps?.sendActiveImage ?: configurationToolbarProps?.sendActiveImage ?: getString(R.string.send_active_image), sendMessage,
            if(!(assistantSettingProps?.initializeWithText ?: configurationKotlin?.activeInput == getString(R.string.textbox)) && (assistantSettingProps?.useVoiceInput ?: configurationKotlin?.useVoiceInput) == false) toolbarProps?.sendInactiveColor ?: configurationToolbarProps?.sendInactiveColor else toolbarProps?.sendActiveColor ?: configurationToolbarProps?.sendActiveColor)
        val sendImageLayoutParams = LinearLayout.LayoutParams(toolbarProps?.sendImageWidth ?: configurationToolbarProps?.sendImageWidth ?: getPixelsFromDp(25), toolbarProps?.sendImageHeight ?: configurationToolbarProps?.sendImageHeight ?: getPixelsFromDp(25))
        sendImageLayoutParams.gravity = Gravity.CENTER
        sendMessage.layoutParams = sendImageLayoutParams

        val micImageLayoutParams = LinearLayout.LayoutParams(toolbarProps?.micImageWidth ?: configurationToolbarProps?.micImageWidth ?: getPixelsFromDp(48), toolbarProps?.micImageHeight ?: configurationToolbarProps?.micImageHeight ?: getPixelsFromDp(48))
        micImageLayoutParams.setMargins(0,getPixelsFromDp(12),0,0)
        micImage.layoutParams = micImageLayoutParams
        micImage.setPadding(toolbarProps?.micImagePadding ?: configurationToolbarProps?.micImagePadding ?: getPixelsFromDp(4), toolbarProps?.micImagePadding ?: configurationToolbarProps?.micImagePadding ?: getPixelsFromDp(4),toolbarProps?.micImagePadding ?: configurationToolbarProps?.micImagePadding ?: getPixelsFromDp(4),toolbarProps?.micImagePadding ?: configurationToolbarProps?.micImagePadding ?: getPixelsFromDp(4))
    }

    private fun initializeRecyclerViews(messagesRecycler: RecyclerView, hintsRecycler: RecyclerView, messages: ArrayList<Message>, hints: ArrayList<String>, hintClicked: (String) -> Unit){
        messagesRecyclerViewAdapter = MessagesRecyclerViewAdapter(messages, bodyProps, configurationBodyProps, requireContext())
        hintsRecyclerViewAdapter = HintsRecyclerViewAdapter(hints, bodyProps, configurationBodyProps, hintClicked, requireContext())
        messagesRecycler.layoutManager = LinearLayoutManager(context)
        messagesRecycler.adapter = messagesRecyclerViewAdapter
        hintsRecycler.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        hintsRecycler.adapter = hintsRecyclerViewAdapter
    }

    private fun initializeAssistant(): VoicifyAssistant {
        voicifyTTS = VoicifyTTSProvider(
            VoicifyTextToSpeechSettings(
            appId = assistantSettingProps?.appId ?: configurationKotlin?.applicationId ?: "",
            appKey = assistantSettingProps?.appKey ?: configurationKotlin?.applicationSecret ?: "",
            voice = assistantSettingProps?.textToSpeechVoice ?: configurationKotlin?.textToSpeechVoice ?: "",
            serverRootUrl = assistantSettingProps?.serverRootUrl ?: getString(R.string.default_server_root_url),
            provider = assistantSettingProps?.textToSpeechProvider ?: configurationKotlin?.textToSpeechProvider ?: getString(R.string.google_tts_default))
        )
        voicifySTT = VoicifySTTProvider(requireContext(), requireActivity())
        voicifyTTS?.cancelSpeech = false
        return VoicifyAssistant(voicifySTT, voicifyTTS,
            VoicifyAssistantSettings(
                appId = assistantSettingProps?.appId ?: configurationKotlin?.applicationId ?: "",
                appKey = assistantSettingProps?.appKey ?: configurationKotlin?.applicationSecret ?: "",
                serverRootUrl = assistantSettingProps?.serverRootUrl ?: "",
                locale = assistantSettingProps?.locale ?: configurationKotlin?.locale ?: getString(R.string.en_US_default_locale),
                channel = assistantSettingProps?.channel ?: configurationKotlin?.channel ?: getString(R.string.android_default_channel),
                device = assistantSettingProps?.device ?: configurationKotlin?.device ?: getString(R.string.mobile_default_device),
                noTracking = assistantSettingProps?.noTracking ?: configurationKotlin?.noTracking ?: false,
                autoRunConversation = assistantSettingProps?.autoRunConversation ?: configurationKotlin?.autoRunConversation ?: false,
                initializeWithWelcomeMessage = assistantSettingProps?.initializeWithWelcomeMessage ?: configurationKotlin?.initializeWithWelcomeMessage ?: false,
                initializeWithText = assistantSettingProps?.initializeWithText ?: (configurationKotlin?.activeInput == getString(R.string.textbox)),
                useVoiceInput = assistantSettingProps?.useVoiceInput ?: configurationKotlin?.useVoiceInput ?: true,
                useDraftContent = assistantSettingProps?.useDraftContent ?: configurationKotlin?.useDraftContent ?: false,
                useOutputSpeech = assistantSettingProps?.useOutputSpeech ?: configurationKotlin?.useOutputSpeech ?: true
            )
        )
    }

    private fun checkInitializeWithText(animationLayout: LinearLayout, sendLayoutStyle: GradientDrawable, sendLayout: LinearLayout, spokenText: TextView, assistantStateText: TextView){
        if((assistantSettingProps?.initializeWithText ?: configurationKotlin?.activeInput == getString(R.string.textbox)) || (assistantSettingProps?.useVoiceInput ?: configurationKotlin?.useVoiceInput) == false)
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
                    dashedLineView.visibility = View.INVISIBLE
                }
                drawer.setPadding(0,0,0,0)
                drawer.setBackgroundColor(Color.TRANSPARENT)
                if(!(toolbarProps?.backgroundColor ?: configurationToolbarProps?.backgroundColor).isNullOrEmpty())
                {
                    toolbar.setBackgroundColor(Color.parseColor(toolbarProps?.backgroundColor ?: configurationToolbarProps?.backgroundColor))
                }
                else if ((assistantSettingProps?.backgroundColor ?: configurationKotlin?.styles?.assistant?.backgroundColor).isNullOrEmpty())
                {
                    toolbar.setBackgroundColor(Color.parseColor(getString(R.string.white)))
                }
                toolbar.setPadding(toolbarProps?.paddingLeft ?: configurationToolbarProps?.paddingLeft ?: getPixelsFromDp(16), getPixelsFromDp(0),toolbarProps?.paddingRight ?: configurationToolbarProps?.paddingRight ?: getPixelsFromDp(16),toolbarProps?.paddingBottom ?: configurationToolbarProps?.paddingBottom ?: getPixelsFromDp(16))
                assistantAvatarBackground.visibility = View.VISIBLE
                if(!(headerProps?.backgroundColor ?: configurationHeaderProps?.backgroundColor).isNullOrEmpty()){
                    header.setBackgroundColor(Color.parseColor(headerProps?.backgroundColor ?: configurationHeaderProps?.backgroundColor))
                }
                else if ((assistantSettingProps?.backgroundColor ?: configurationKotlin?.styles?.assistant?.backgroundColor).isNullOrEmpty())
                {
                    header.setBackgroundColor(Color.parseColor(getString(R.string.white)))
                }
                header.setPadding(headerProps?.paddingLeft ?: configurationHeaderProps?.paddingLeft ?: getPixelsFromDp(16), headerProps?.paddingTop ?: configurationHeaderProps?.paddingTop ?: getPixelsFromDp(16), headerProps?.paddingRight ?: configurationHeaderProps?.paddingRight ?: getPixelsFromDp(16), headerProps?.paddingBottom ?: configurationHeaderProps?.paddingBottom ?: getPixelsFromDp(16))
                bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
                isDrawer = false
                micImage.setBackgroundColor(Color.parseColor(toolbarProps?.micInactiveHighlightColor ?: configurationToolbarProps?.micInactiveHighlightColor ?: getString(R.string.transparent)))
                val metrics = activity?.resources?.displayMetrics
                val params = drawerLayout.layoutParams
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
                    messagesRecycler.smoothScrollToPosition(messagesRecyclerViewAdapter?.itemCount as Int)
                }

        }
    }
    private fun startNewAssistantSession(assistant: VoicifyAssistant){
        if((assistantSettingProps?.locale ?: configurationKotlin?.locale).toString().isNotEmpty())
        {
            voicifySTT?.initialize((assistantSettingProps?.locale ?: configurationKotlin?.locale).toString())
        }
        assistant.initializeAndStart()
        assistant.startNewSession(null, null, this.sessionAttributes, this.userAttributes)
        if((assistantSettingProps?.initializeWithText ?: configurationKotlin?.activeInput == getString(R.string.textbox)) != true && (assistantSettingProps?.useVoiceInput ?: configurationKotlin?.useVoiceInput) != false && (assistantSettingProps?.initializeWithWelcomeMessage ?: configurationKotlin?.initializeWithWelcomeMessage) != true)
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
                if(isDrawer)
                {
                    hideKeyboard()
                    isKeyboardActive = false
                }
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
                    dashedLineView.visibility = View.INVISIBLE
                }
                if(!speechFullResult.isNullOrEmpty())
                {
                    messagesList.add(Message(speechFullResult as String, getString(R.string.sent)))
                    messagesRecyclerViewAdapter?.notifyDataSetChanged()
                    messagesRecycler.smoothScrollToPosition(messagesRecyclerViewAdapter?.itemCount as Int)
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
                    toolbar.setBackgroundColor(Color.parseColor(getString(R.string.white)))
                }
                toolbar.setPadding(toolbarProps?.paddingLeft ?: configurationToolbarProps?.paddingLeft ?: getPixelsFromDp(16), getPixelsFromDp(0),toolbarProps?.paddingRight ?: configurationToolbarProps?.paddingRight ?: getPixelsFromDp(16),toolbarProps?.paddingBottom ?: configurationToolbarProps?.paddingBottom ?: getPixelsFromDp(16))
                avatarBackground.visibility = View.VISIBLE
                if(!(headerProps?.backgroundColor ?: configurationHeaderProps?.backgroundColor).isNullOrEmpty()){
                    header.setBackgroundColor(Color.parseColor(headerProps?.backgroundColor ?: configurationHeaderProps?.backgroundColor))
                }
                else if ((assistantSettingProps?.backgroundColor ?: configurationKotlin?.styles?.assistant?.backgroundColor).isNullOrEmpty())
                {
                    header.setBackgroundColor(Color.parseColor(getString(R.string.white)))
                }
                header.setPadding(headerProps?.paddingLeft ?: configurationHeaderProps?.paddingLeft ?: getPixelsFromDp(16), headerProps?.paddingTop ?: configurationHeaderProps?.paddingTop ?: getPixelsFromDp(16), headerProps?.paddingRight ?: configurationHeaderProps?.paddingRight ?: getPixelsFromDp(16), headerProps?.paddingBottom ?: configurationHeaderProps?.paddingBottom ?: getPixelsFromDp(16))
                bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
                isDrawer = false
                micImage.setBackgroundColor(Color.parseColor(toolbarProps?.micInactiveHighlightColor ?: configurationToolbarProps?.micInactiveHighlightColor ?: getString(R.string.transparent)))
                val metrics = activity?.resources?.displayMetrics
                val params = drawerLayout.layoutParams
                params.height = metrics?.heightPixels as Int
                drawer.layoutParams = params
                assistantStateText.text = ""
                drawerText.text = ""
                avatarImage.visibility = View.VISIBLE
                assistantName.visibility = View.VISIBLE
                messagesRecycler.visibility = View.VISIBLE
                bodyTopBorder.visibility = View.VISIBLE
                bodyBottomBorder.visibility = View.VISIBLE
                messagesList.add(Message(response.displayText?.trim() as String, getString(R.string.received)))
                messagesRecyclerViewAdapter?.notifyDataSetChanged()
                messagesRecycler.smoothScrollToPosition(messagesRecyclerViewAdapter?.itemCount as Int)
            }
        }

        assistantSettingProps?.effects?.forEach { effect ->
            assistant.onEffect(effect) { data ->
                onEffectCallback?.invoke(effect, data)
            }
        }

        // add out of box close effect
        assistant.onEffect(getString(R.string.close_assistant_effect_name)){
            dismiss()
        }

        // handle errors"Assistant Call Failed"
        assistant.onError{ errorMessage, request ->
            activity?.runOnUiThread{
                if(errorMessage == getString(R.string.assistant_call_failed_response))
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
            spokenText.setTextColor(Color.parseColor(toolbarProps?.partialSpeechResultTextColor ?: configurationToolbarProps?.partialSpeechResultTextColor ?: getString(R.string.white_20_percent)))
            spokenText.text = partialResult
        }
        voicifySTT?.addFinalResultListener { fullResult ->
            spokenText.setTextColor(Color.parseColor(toolbarProps?.fullSpeechResultTextColor ?: configurationToolbarProps?.fullSpeechResultTextColor ?: getString(R.string.white)))
            speechFullResult = fullResult
            clearAnimationValues()
            assistantIsListening = false
            spokenText.text = fullResult
            assistantStateText.text = getString(R.string.assistant_state_processing)
            assistant.makeTextRequest(fullResult.toString(), null, getString(R.string.speech))
        }

        voicifySTT?.addSpeechReadyListener {
            val micImageViewStyle = GradientDrawable()
            micImageViewStyle.setColor(Color.parseColor(toolbarProps?.micActiveHighlightColor ?: configurationToolbarProps?.micActiveHighlightColor ?: getString(R.string.blue_12_percent)))
            micImageViewStyle.setStroke(toolbarProps?.micImageBorderWidth ?: configurationToolbarProps?.micImageBorderWidth ?: 0, Color.parseColor(toolbarProps?.micImageBorderColor ?: configurationToolbarProps?.micImageBorderColor ?: getString(R.string.transparent)))
            micImageViewStyle.cornerRadius = toolbarProps?.micBorderRadius ?: configurationToolbarProps?.micBorderRadius ?: 100f
            micImage.background = micImageViewStyle
            assistantIsListening = true
            assistantStateText.text = getString(R.string.assistant_state_listening)
        }
        voicifySTT?.addErrorListener { error ->
            clearAnimationValues()
            if (error == SpeechRecognizer.ERROR_NO_MATCH.toString())
            {
                assistantIsListening = false
                micImage.setBackgroundColor(Color.parseColor(toolbarProps?.micInactiveHighlightColor ?: configurationToolbarProps?.micInactiveHighlightColor ?: getString(R.string.transparent)))
                assistantStateText.text = getString(R.string.assistant_state_misunderstood)
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
            val bar1 = ObjectAnimator.ofFloat(speakingAnimationBar1, getString(R.string.animation_scale_y), rnd1)
            bar1.duration = duration
            val bar2 = ObjectAnimator.ofFloat(speakingAnimationBar2, getString(R.string.animation_scale_y), rnd2)
            bar2.duration = duration
            val bar3 = ObjectAnimator.ofFloat(speakingAnimationBar3, getString(R.string.animation_scale_y), rnd3)
            bar3.duration = duration
            val bar4 = ObjectAnimator.ofFloat(speakingAnimationBar4, getString(R.string.animation_scale_y), rnd4)
            bar4.duration = duration
            val bar5 = ObjectAnimator.ofFloat(speakingAnimationBar5, getString(R.string.animation_scale_y), rnd5)
            bar5.duration = duration
            val bar6 = ObjectAnimator.ofFloat(speakingAnimationBar6, getString(R.string.animation_scale_y), rnd6)
            bar6.duration = duration
            val bar7 = ObjectAnimator.ofFloat(speakingAnimationBar7, getString(R.string.animation_scale_y), rnd7)
            bar7.duration = duration
            val bar8 = ObjectAnimator.ofFloat(speakingAnimationBar8, getString(R.string.animation_scale_y), rnd8)
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
            val colorStateList = ColorStateList.valueOf(Color.parseColor(toolbarProps?.textInputLineColor ?: configurationToolbarProps?.textInputLineColor ?: getString(R.string.silver)))
            ViewCompat.setBackgroundTintList(inputTextMessage,colorStateList)
            if(!isUsingSpeech)
            {
                if(isRotated){
                    val layoutParams1 = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, getPixelsFromDp(50))
                    layoutParams1.weight = 0f
                    bodyContainerLayout.layoutParams = layoutParams1
                }
                voicifySTT?.cancel = false
                isUsingSpeech = true
                messagesRecyclerViewAdapter?.notifyDataSetChanged()
                messagesRecycler.smoothScrollToPosition(messagesRecyclerViewAdapter?.itemCount as Int)
                speakingAnimationLayout.visibility = View.VISIBLE
                sendLayout.setBackgroundColor(Color.parseColor(toolbarProps?.textboxInactiveHighlightColor ?: configurationToolbarProps?.textboxInactiveHighlightColor ?: getString(R.string.transparent)))
                dashedLineView.visibility = View.VISIBLE
                hideKeyboard()
                if(!isDrawer){
                    val drawerFooterLayoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    drawerFooterLayoutParams.setMargins(0,getPixelsFromDp(20),0,0)
                    drawerFooter.layoutParams = drawerFooterLayoutParams
                }
                spokenText.visibility = View.VISIBLE
                assistantStateText.visibility = View.VISIBLE
                speakText.setTextColor(Color.parseColor(toolbarProps?.speakActiveTitleColor ?: configurationToolbarProps?.speakActiveTitleColor ?: getString(R.string.dark_blue)))
                typeText.setTextColor(Color.parseColor(toolbarProps?.typeInactiveTitleColor ?: configurationToolbarProps?.speakInactiveTitleColor ?: getString(R.string.dark_gray)))
                loadImageFromUrl(toolbarProps?.micActiveImage ?: configurationToolbarProps?.micActiveImage ?: getString(R.string.mic_active_image), micImage, toolbarProps?.micActiveColor ?: configurationToolbarProps?.micActiveColor)
                loadImageFromUrl(toolbarProps?.sendInactiveImage ?: configurationToolbarProps?.sendInactiveImage ?: getString(R.string.send_inactive_image), sendMessage, toolbarProps?.sendInactiveColor ?: configurationToolbarProps?.sendInactiveColor)
            }
            if(!assistantIsListening)
            {
                voicifyTTS?.stop()
                voicifySTT?.startListening()
            }
            else
            {
                cancelSpeech()
                assistantStateTextView.text = getString(R.string.assistant_state_misunderstood)
            }
        }
    }

    private fun addSendMessageClickListener(sendMessage: ImageView, inputTextMessage: EditText, messagesList: ArrayList<Message>, messagesRecycler: RecyclerView, assistant: VoicifyAssistant) {
        sendMessage.setOnClickListener{
            if(inputTextMessage.text.toString().isNotEmpty())
            {
                messagesList.add(Message(inputTextMessage.text.toString(), getString(R.string.sent)))
                messagesRecyclerViewAdapter?.notifyDataSetChanged()
                messagesRecycler.smoothScrollToPosition(messagesRecyclerViewAdapter?.itemCount as Int)
                val inputText = inputTextMessage.text.toString()
                inputTextMessage.setText("")
                hideKeyboard()
                assistant.makeTextRequest(inputText,null, getString(R.string.text))
            }
        }
    }

    private fun addTextboxClickListener(inputTextMessage: EditText, assistantStateText: TextView, spokenText: TextView, speakingAnimationLayout: LinearLayout,
                                        sendTextLayoutStyle: GradientDrawable, sendLayout: LinearLayout, drawerFooter: LinearLayout, dashedLineView: ImageView,
                                        speakText: TextView, typeText: TextView, micImage: ImageView, sendMessage: ImageView) {
        inputTextMessage.setOnTouchListener(object : OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when(event?.action) {
                    MotionEvent.ACTION_UP -> {
                        val colorStateList = ColorStateList.valueOf(Color.parseColor(toolbarProps?.textInputActiveLineColor ?: configurationToolbarProps?.textInputLineColor ?: getString(R.string.silver)))
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
                            if(isRotated)
                            {
                                val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, getPixelsFromDp(200))
                                layoutParams.weight = 0f
                                bodyContainerLayout.layoutParams = layoutParams
                            }
                            if(!isDrawer)
                            {
                                val drawerFooterLayoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                                drawerFooterLayoutParams.setMargins(0,0,0,0)
                                drawerFooter.layoutParams = drawerFooterLayoutParams
                                dashedLineView.visibility = View.INVISIBLE
                            }
                            spokenText.visibility = View.GONE
                            assistantStateText.visibility = View.GONE
                            speakText.setTextColor(Color.parseColor(toolbarProps?.speakInactiveTitleColor ?: configurationToolbarProps?.speakInactiveTitleColor ?: getString(R.string.dark_gray)))
                            typeText.setTextColor(Color.parseColor(toolbarProps?.typeActiveTitleColor ?: configurationToolbarProps?.speakActiveTitleColor ?: getString(R.string.dark_blue)))
                            loadImageFromUrl(
                                toolbarProps?.micInactiveImage ?: configurationToolbarProps?.micInactiveImage ?: getString(R.string.mic_inactive_image),
                                micImage,
                                toolbarProps?.micInactiveColor ?: configurationToolbarProps?.micInactiveColor
                            )
                            loadImageFromUrl(
                                toolbarProps?.sendActiveImage ?: configurationToolbarProps?.sendActiveImage ?: getString(R.string.send_active_image),
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
        micImageView.setBackgroundColor(Color.parseColor(toolbarProps?.micInactiveHighlightColor ?: configurationToolbarProps?.micInactiveHighlightColor ?: getString(R.string.transparent)))
        spokenTextView.text =  ""
    }

    private fun addKeyboardActiveListener(window: View){
        window.viewTreeObserver.addOnGlobalLayoutListener {
            val r = Rect()
            window.getWindowVisibleDisplayFrame(r)
            val heightDiff = window.rootView.height - (r.bottom - r.top)
            if (heightDiff > 500) {
                if(!isKeyboardActive && !isRotated)
                {
                    val layoutParams1 = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, getPixelsFromDp(320))
                    layoutParams1.weight = 0f
                    bodyContainerLayout.layoutParams = layoutParams1
                    messagesRecyclerViewAdapter?.notifyDataSetChanged()
                    messagesRecyclerView.smoothScrollToPosition(messagesRecyclerViewAdapter?.itemCount as Int)
                    isKeyboardActive = true
                }
            }
            else
            {
                if(isKeyboardActive && !isRotated)
                {
                    val layoutParams1 = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, getPixelsFromDp(0))
                    layoutParams1.weight = 1f
                    bodyContainerLayout.layoutParams = layoutParams1
                    isKeyboardActive = false
                }
                else if(isKeyboardActive && isRotated && !isUsingSpeech) {
                    val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, getPixelsFromDp(200))
                    layoutParams.weight = 0f
                    bodyContainerLayout.layoutParams = layoutParams
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

    private fun loadImageFromUrl(url: String, view: ImageView, imageColor: String? = null, isAssistantAvatar: Boolean = false){
        if(isAssistantAvatar)
        {
            Picasso.get().load(url).into(view , object: Callback {
                override fun onSuccess() {
                    val imageBitmap = view.drawable as BitmapDrawable
                    val bitmap = imageBitmap.bitmap
                    val imageDrawable = RoundedBitmapDrawableFactory.create(resources, bitmap);
                    imageDrawable.isCircular = true;
                    imageDrawable.cornerRadius = headerProps?.assistantImageBorderRadius ?: configurationHeaderProps?.assistantImageBorderRadius ?: 200f
                    view.setImageDrawable(imageDrawable);
                    if(!imageColor.isNullOrEmpty())
                    {
                        DrawableCompat.setTint(view.drawable, Color.parseColor(imageColor))
                    }
                }
                override fun onError(e: Exception?) {
                }
            });
        }
        else if(imageColor.isNullOrEmpty())
        {
            Picasso.get().load(url).into(view)
        }
        else{
            Picasso.get().load(url).into(view, object: Callback {
                override fun onSuccess() {
                    DrawableCompat.setTint(view.drawable, Color.parseColor(imageColor))
                }

                override fun onError(e: Exception?) {
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
        val bar1 = ObjectAnimator.ofFloat(speakingAnimationBar1, getString(R.string.animation_scale_y), 1f)
        bar1.duration = duration
        val bar2 = ObjectAnimator.ofFloat(speakingAnimationBar2, getString(R.string.animation_scale_y), 1f)
        bar2.duration = duration
        val bar3 = ObjectAnimator.ofFloat(speakingAnimationBar3, getString(R.string.animation_scale_y), 1f)
        bar3.duration = duration
        val bar4 = ObjectAnimator.ofFloat(speakingAnimationBar4, getString(R.string.animation_scale_y), 1f)
        bar4.duration = duration
        val bar5 = ObjectAnimator.ofFloat(speakingAnimationBar5, getString(R.string.animation_scale_y), 1f)
        bar5.duration = duration
        val bar6 = ObjectAnimator.ofFloat(speakingAnimationBar6, getString(R.string.animation_scale_y), 1f)
        bar6.duration = duration
        val bar7 = ObjectAnimator.ofFloat(speakingAnimationBar7, getString(R.string.animation_scale_y), 1f)
        bar7.duration = duration
        val bar8 = ObjectAnimator.ofFloat(speakingAnimationBar8, getString(R.string.animation_scale_y), 1f)
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
            val editorField = TextView::class.java.getFieldByName(getString(R.string.textbox_cursor_editor_field_name))
            val editor = editorField?.get(this) ?: this
            val editorClass: Class<*> = if (editorField != null) editor.javaClass else TextView::class.java
            val cursorRes = TextView::class.java.getFieldByName(getString(R.string.cursor_drawable_res))?.get(this) as? Int ?: return

            val tintedCursorDrawable = ContextCompat.getDrawable(context, cursorRes)?.tinted(color) ?: return

            val cursorField = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                editorClass.getFieldByName(getString(R.string.drawable_for_cursor))
            } else {
                null
            }
            if (cursorField != null) {
                cursorField.set(editor, tintedCursorDrawable)
            } else {
                editorClass.getFieldByName(getString(R.string.cursor_drawable_res), getString(R.string.drawable_for_cursor))
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
        this is VectorDrawable -> {
            this.apply { setTintList(ColorStateList.valueOf(color)) }
        }
        else -> {
            DrawableCompat.wrap(this)
                .also { DrawableCompat.setTint(it, color) }
                .let { DrawableCompat.unwrap(it) }
        }
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
                        NotificationCenter.postNotification(requireContext(), NotificationType.LOADING_COMPLETE)
                    }

                    GlobalScope.async (Dispatchers.IO + coroutineExceptionHandler){
                        try{
                            configurationKotlin = customAssistantConfigurationService.getCustomAssistantConfiguration(
                                assistantSettingsProperties?.configurationId ?: "",
                                assistantSettingsProperties.serverRootUrl,
                                assistantSettingsProperties.appId,
                                assistantSettingsProperties.appKey
                            )
                            configurationHeaderProps = configurationKotlin?.styles?.header
                            configurationBodyProps = configurationKotlin?.styles?.body
                            configurationToolbarProps = configurationKotlin?.styles?.toolbar
                            isLoadingConfiguration = false
                            NotificationCenter.postNotification(requireContext(), NotificationType.LOADING_COMPLETE)
                        }
                        catch(e: Exception){
                            NotificationCenter.postNotification(requireContext(), NotificationType.LOADING_COMPLETE)
                        }
                    }
                }
                else{
                    NotificationCenter.postNotification(requireContext(), NotificationType.LOADING_COMPLETE)
                }
            }
    }
}