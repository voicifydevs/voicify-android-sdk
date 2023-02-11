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
import com.voicify.voicify_assistant_sdk.components.toolbar.AssistantDrawerUIToolbar
import com.voicify.voicify_assistant_sdk.components.toolbar.SpeakingAnimation
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

        //Image Views
        val micImageView = window.findViewById<ImageView>(R.id.micImageView)
        val closeAssistantImageView = window.findViewById<ImageView>(R.id.closeAssistantImageView)
        val closeAssistantNoInternetImageView = window.findViewById<ImageView>(R.id.closeAssistantNoInternetImageView)
        val sendMessageImageView = window.findViewById<ImageView>(R.id.sendMessageButtonImageView)
        val assistantAvatarImageView = window.findViewById<ImageView>(R.id.assistantAvatarImageView)
        val dashedLineImageView = window.findViewById<ImageView>(R.id.dashedLineImageView)

        val assistantDrawerUIHeader = AssistantDrawerUIHeader(
            context = requireContext(),
            headerProps = headerProps,
            configurationHeaderProps = configurationHeaderProps,
            resources = resources
        )
        val assistantDrawerUIBody = AssistantDrawerUIBody(
            context = requireContext(),
            bodyProps = bodyProps,
            configurationBodyProps = configurationBodyProps,
            assistantSettingProps = assistantSettingProps,
            configuration = configurationKotlin,
        )
        val assistantDrawerUIToolbar = AssistantDrawerUIToolbar(
            context = requireContext(),
            toolbarProps = toolbarProps,
            configurationToolbarProps = configurationToolbarProps,
            assistantSettingProps = assistantSettingProps,
            configuration = configurationKotlin,
        )
        val speakAnimation = SpeakingAnimation(
            context = requireContext(),
            toolbarProps = toolbarProps,
            configurationToolbarProps = configurationToolbarProps,
            animationBars = speakingAnimationBars
        )
        sendTextLayoutStyle.setColor(Color.parseColor(toolbarProps?.textboxActiveHighlightColor ?: configurationToolbarProps?.textboxActiveHighlightColor ?: getString(R.string.blue_12_percent)))
        sendTextLayoutStyle.cornerRadius = 24f

        if(!isLoadingConfiguration)
        {
            val assistant = initializeAssistant()
            val onHintClicked: (String) -> Unit = {  hint ->
                messagesList.add(Message(hint, getString(R.string.sent)))
                speakAnimation.clearAnimationValues(animation)
                messagesRecyclerViewAdapter?.notifyDataSetChanged()
                messagesRecyclerView.smoothScrollToPosition(messagesRecyclerViewAdapter?.itemCount as Int)
                hideKeyboard()
                hintsList.clear()
                cancelSpeech()
                voicifyTTS?.stop()
                hintsRecyclerViewAdapter?.notifyDataSetChanged()
                assistant.makeTextRequest(hint ,null, getString(R.string.text))
            }
            assistantDrawerUIHeader.initializeHeader(
                closeAssistantImageView = closeAssistantImageView,
                avatarImageView = assistantAvatarImageView,
                closeBackgroundLayout =  closeAssistantBackground,
                avatarBackgroundLayout = assistantAvatarBackground,
                assistantNameTextView = assistantNameTextView
            )
            val (messagesAdapter,hintsAdapter) = assistantDrawerUIBody.initializeBody(
                bodyBorderTopView = bodyBorderTopView,
                bodyBorderBottomView = bodyBorderBottomView,
                bodyLayout = bodyContainerLayout,
                messagesRecycler = messagesRecyclerView,
                hintsRecycler = hintsRecyclerView,
                messages = messagesList,
                hints = hintsList,
                onHintClicked = onHintClicked
            )
            messagesRecyclerViewAdapter = messagesAdapter
            hintsRecyclerViewAdapter = hintsAdapter
            assistantDrawerUIToolbar.initializeToolbar(
                micImageView = micImageView,
                sendMessageImageView = sendMessageImageView,
                speakTextView = speakTextView,
                typeTextView = typeTextView,
                drawerHelpTextView = drawerWelcomeTextView,
                assistantStateTextView = assistantStateTextView,
                spokenTextView = spokenTextView,
                inputeMessageEditText = inputTextMessageEditTextView,
                drawerLayout = drawerLayout,
            )
            speakAnimation.initializeSpeakingAnimation()
            containerLayout.visibility = View.VISIBLE
            activityIndicator.visibility = View.GONE

            //UI Initialization
            addGradientBackground(containerLayout)
            checkInitializeWithText(speakingAnimationLayout, sendTextLayoutStyle, sendTextLayout, spokenTextView, assistantStateTextView)
            startNewAssistantSession(assistant)

            //Add Listeners
            addKeyboardActiveListener(window)
            addSpeechToTextListeners(assistant, spokenTextView, assistantStateTextView, micImageView, speakAnimation)
            addMicClickListener(micImageView, messagesRecyclerView, speakingAnimationLayout, sendTextLayout, dashedLineImageView, drawerFooterLayout,
                spokenTextView, assistantStateTextView, speakTextView, typeTextView, sendMessageImageView, speakAnimation)

            addSendMessageClickListener(sendMessageImageView, inputTextMessageEditTextView, messagesList, messagesRecyclerView, assistant)
            addAssistantHandlers(assistant, drawerLayout, bodyContainerLayout, spokenTextView, hintsRecyclerView, closeAssistantImageView, closeAssistantNoInternetImageView,
                hintsList, drawerWelcomeTextView, drawerFooterLayout, dashedLineImageView, messagesList, messagesRecyclerView, toolbarLayout, headerLayout,
                assistantAvatarBackground, micImageView, assistantStateTextView, assistantAvatarImageView, assistantNameTextView, bodyBorderTopView, bodyBorderBottomView, speakAnimation)

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
                    val assistant = initializeAssistant()
                    val onHintClicked: (String) -> Unit = {  hint ->
                        messagesList.add(Message(hint, getString(R.string.sent)))
                        speakAnimation.clearAnimationValues(animation)
                        messagesRecyclerViewAdapter?.notifyDataSetChanged()
                        messagesRecyclerView.smoothScrollToPosition(messagesRecyclerViewAdapter?.itemCount as Int)
                        hideKeyboard()
                        hintsList.clear()
                        cancelSpeech()
                        voicifyTTS?.stop()
                        hintsRecyclerViewAdapter?.notifyDataSetChanged()
                        assistant.makeTextRequest(hint ,null, getString(R.string.text))
                    }
                    assistantDrawerUIHeader.initializeHeader(
                        closeAssistantImageView = closeAssistantImageView,
                        avatarImageView = assistantAvatarImageView,
                        closeBackgroundLayout =  closeAssistantBackground,
                        avatarBackgroundLayout = assistantAvatarBackground,
                        assistantNameTextView = assistantNameTextView
                    )
                    val (messagesAdapter,hintsAdapter) = assistantDrawerUIBody.initializeBody(
                        bodyBorderTopView = bodyBorderTopView,
                        bodyBorderBottomView = bodyBorderBottomView,
                        bodyLayout = bodyContainerLayout,
                        messagesRecycler = messagesRecyclerView,
                        hintsRecycler = hintsRecyclerView,
                        messages = messagesList,
                        hints = hintsList,
                        onHintClicked = onHintClicked
                    )
                    messagesRecyclerViewAdapter = messagesAdapter
                    hintsRecyclerViewAdapter = hintsAdapter
                    assistantDrawerUIToolbar.initializeToolbar(
                        micImageView = micImageView,
                        sendMessageImageView = sendMessageImageView,
                        speakTextView = speakTextView,
                        typeTextView = typeTextView,
                        drawerHelpTextView = drawerWelcomeTextView,
                        assistantStateTextView = assistantStateTextView,
                        spokenTextView = spokenTextView,
                        inputeMessageEditText = inputTextMessageEditTextView,
                        drawerLayout = drawerLayout,
                    )
                    speakAnimation.initializeSpeakingAnimation()
                    containerLayout.visibility = View.VISIBLE
                    activityIndicator.visibility = View.GONE

                    addGradientBackground(containerLayout)
                    checkInitializeWithText(speakingAnimationLayout, sendTextLayoutStyle, sendTextLayout, spokenTextView, assistantStateTextView)

                    //UI Initialization
                    checkInitializeWithText(speakingAnimationLayout, sendTextLayoutStyle, sendTextLayout, spokenTextView, assistantStateTextView)
                    checkInitializeWithWelcome(drawerLayout, bodyContainerLayout, spokenTextView, hintsRecyclerView, drawerFooterLayout,
                        dashedLineImageView, toolbarLayout, assistantAvatarBackgroundContainerLayout, headerLayout,
                        micImageView, assistantStateTextView, drawerWelcomeTextView, assistantAvatarImageView,
                        assistantNameTextView, messagesRecyclerView, bodyBorderTopView, bodyBorderBottomView)
                    startNewAssistantSession(assistant)

                    //Add Listeners
                    addKeyboardActiveListener(window)
                    addSpeechToTextListeners(assistant, spokenTextView, assistantStateTextView, micImageView, speakAnimation)
                    addMicClickListener(micImageView, messagesRecyclerView, speakingAnimationLayout, sendTextLayout, dashedLineImageView, drawerFooterLayout,
                        spokenTextView, assistantStateTextView, speakTextView, typeTextView, sendMessageImageView, speakAnimation)
                    addSendMessageClickListener(sendMessageImageView, inputTextMessageEditTextView, messagesList, messagesRecyclerView, assistant)
                    addAssistantHandlers(assistant, drawerLayout, bodyContainerLayout, spokenTextView, hintsRecyclerView, closeAssistantImageView, closeAssistantNoInternetImageView,
                        hintsList, drawerWelcomeTextView, drawerFooterLayout, dashedLineImageView, messagesList, messagesRecyclerView, toolbarLayout, headerLayout,
                        assistantAvatarBackground, micImageView, assistantStateTextView, assistantAvatarImageView, assistantNameTextView, bodyBorderTopView, bodyBorderBottomView, speakAnimation)

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
        bottomSheetBehavior?.peekHeight = HelperMethods.getPixelsFromDp(500, scale)
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
                val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, HelperMethods.getPixelsFromDp(0, scale))
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
                bottomSheetBehavior?.peekHeight = HelperMethods.getPixelsFromDp(500,scale)
            }
            else{
                if(messagesRecyclerViewAdapter?.itemCount ?: 0 > 0)
                {
                    messagesRecyclerView.smoothScrollToPosition(messagesRecyclerViewAdapter?.itemCount as Int)
                }
                val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, HelperMethods.getPixelsFromDp(if(isUsingSpeech) {50} else {200}, scale))
                layoutParams.weight = 0f
                bodyContainerLayout.layoutParams = layoutParams
            }
            isRotated = true
        }
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
                toolbar.setPadding(toolbarProps?.paddingLeft ?: configurationToolbarProps?.paddingLeft ?: HelperMethods.getPixelsFromDp(16, scale), HelperMethods.getPixelsFromDp(0, scale),toolbarProps?.paddingRight ?: configurationToolbarProps?.paddingRight ?: HelperMethods.getPixelsFromDp(16, scale),toolbarProps?.paddingBottom ?: configurationToolbarProps?.paddingBottom ?: HelperMethods.getPixelsFromDp(16, scale))
                assistantAvatarBackground.visibility = View.VISIBLE
                if(!(headerProps?.backgroundColor ?: configurationHeaderProps?.backgroundColor).isNullOrEmpty()){
                    header.setBackgroundColor(Color.parseColor(headerProps?.backgroundColor ?: configurationHeaderProps?.backgroundColor))
                }
                else if ((assistantSettingProps?.backgroundColor ?: configurationKotlin?.styles?.assistant?.backgroundColor).isNullOrEmpty())
                {
                    header.setBackgroundColor(Color.parseColor(getString(R.string.white)))
                }
                header.setPadding(headerProps?.paddingLeft ?: configurationHeaderProps?.paddingLeft ?: HelperMethods.getPixelsFromDp(16, scale), headerProps?.paddingTop ?: configurationHeaderProps?.paddingTop ?: HelperMethods.getPixelsFromDp(16, scale), headerProps?.paddingRight ?: configurationHeaderProps?.paddingRight ?: HelperMethods.getPixelsFromDp(16, scale), headerProps?.paddingBottom ?: configurationHeaderProps?.paddingBottom ?: HelperMethods.getPixelsFromDp(16, scale))
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
                                    avatarImage: ImageView, assistantName: TextView, bodyTopBorder: View, bodyBottomBorder: View, speakAnimation: SpeakingAnimation){
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
                toolbar.setPadding(toolbarProps?.paddingLeft ?: configurationToolbarProps?.paddingLeft ?: HelperMethods.getPixelsFromDp(16, scale), HelperMethods.getPixelsFromDp(0, scale),toolbarProps?.paddingRight ?: configurationToolbarProps?.paddingRight ?: HelperMethods.getPixelsFromDp(16, scale),toolbarProps?.paddingBottom ?: configurationToolbarProps?.paddingBottom ?: HelperMethods.getPixelsFromDp(16, scale))
                avatarBackground.visibility = View.VISIBLE
                if(!(headerProps?.backgroundColor ?: configurationHeaderProps?.backgroundColor).isNullOrEmpty()){
                    header.setBackgroundColor(Color.parseColor(headerProps?.backgroundColor ?: configurationHeaderProps?.backgroundColor))
                }
                else if ((assistantSettingProps?.backgroundColor ?: configurationKotlin?.styles?.assistant?.backgroundColor).isNullOrEmpty())
                {
                    header.setBackgroundColor(Color.parseColor(getString(R.string.white)))
                }
                header.setPadding(headerProps?.paddingLeft ?: configurationHeaderProps?.paddingLeft ?: HelperMethods.getPixelsFromDp(16, scale), headerProps?.paddingTop ?: configurationHeaderProps?.paddingTop ?: HelperMethods.getPixelsFromDp(16, scale), headerProps?.paddingRight ?: configurationHeaderProps?.paddingRight ?: HelperMethods.getPixelsFromDp(16, scale), headerProps?.paddingBottom ?: configurationHeaderProps?.paddingBottom ?: HelperMethods.getPixelsFromDp(16, scale))
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

    private fun addSpeechToTextListeners(assistant: VoicifyAssistant, spokenText: TextView, assistantStateText: TextView, micImage: ImageView, speakAnimation: SpeakingAnimation){
        voicifySTT?.addPartialListener { partialResult ->
            spokenText.setTextColor(Color.parseColor(toolbarProps?.partialSpeechResultTextColor ?: configurationToolbarProps?.partialSpeechResultTextColor ?: getString(R.string.white_20_percent)))
            spokenText.text = partialResult
        }
        voicifySTT?.addFinalResultListener { fullResult ->
            spokenText.setTextColor(Color.parseColor(toolbarProps?.fullSpeechResultTextColor ?: configurationToolbarProps?.fullSpeechResultTextColor ?: getString(R.string.white)))
            speechFullResult = fullResult
            speakAnimation.clearAnimationValues(animation)
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
            speakAnimation.clearAnimationValues(animation)
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
                                val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, HelperMethods.getPixelsFromDp(200, scale))
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
                            HelperMethods.loadImageFromUrl(
                                url = toolbarProps?.micInactiveImage ?: configurationToolbarProps?.micInactiveImage ?: getString(R.string.mic_inactive_image),
                                view = micImage,
                                imageColor = toolbarProps?.micInactiveColor ?: configurationToolbarProps?.micInactiveColor
                            )
                            HelperMethods.loadImageFromUrl(
                                url = toolbarProps?.sendActiveImage ?: configurationToolbarProps?.sendActiveImage ?: getString(R.string.send_active_image),
                                view = sendMessage,
                                imageColor = toolbarProps?.sendActiveColor ?: configurationToolbarProps?.sendActiveColor
                            )
                        }
                    }
                }
                v?.performClick()
                return v?.onTouchEvent(event) ?: true
            }
        })
    }

    private fun addMicClickListener(micImage: ImageView, messagesRecycler: RecyclerView, speakingAnimationLayout: LinearLayout, sendLayout: LinearLayout, dashedLineView: ImageView,
                                    drawerFooter: LinearLayout, spokenText: TextView, assistantStateText: TextView, speakText: TextView, typeText: TextView,
                                    sendMessage: ImageView, speakAnimation: SpeakingAnimation){
        micImage.setOnClickListener{
            speakAnimation.clearAnimationValues(animation)
            val colorStateList = ColorStateList.valueOf(Color.parseColor(toolbarProps?.textInputLineColor ?: com.voicify.voicify_assistant_sdk.components.configurationToolbarProps?.textInputLineColor ?: getString(R.string.silver)))
            ViewCompat.setBackgroundTintList(inputTextMessage,colorStateList)
            if(!isUsingSpeech)
            {
                if(isRotated){
                    val layoutParams1 = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, HelperMethods.getPixelsFromDp(50, scale))
                    layoutParams1.weight = 0f
                    bodyContainerLayout.layoutParams = layoutParams1
                }
                voicifySTT?.cancel = false
                isUsingSpeech = true
                messagesRecyclerViewAdapter?.notifyDataSetChanged()
                messagesRecycler.smoothScrollToPosition(messagesRecyclerViewAdapter?.itemCount as Int)
                speakingAnimationLayout.visibility = View.VISIBLE
                sendLayout.setBackgroundColor(Color.parseColor(toolbarProps?.textboxInactiveHighlightColor ?: com.voicify.voicify_assistant_sdk.components.configurationToolbarProps?.textboxInactiveHighlightColor ?: getString(R.string.transparent)))
                dashedLineView.visibility = View.VISIBLE
                hideKeyboard()
                if(!isDrawer){
                    val drawerFooterLayoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    drawerFooterLayoutParams.setMargins(0,HelperMethods.getPixelsFromDp(20, scale),0,0)
                    drawerFooter.layoutParams = drawerFooterLayoutParams
                }
                spokenText.visibility = View.VISIBLE
                assistantStateText.visibility = View.VISIBLE
                speakText.setTextColor(Color.parseColor(toolbarProps?.speakActiveTitleColor ?: com.voicify.voicify_assistant_sdk.components.configurationToolbarProps?.speakActiveTitleColor ?: getString(R.string.dark_blue)))
                typeText.setTextColor(Color.parseColor(toolbarProps?.typeInactiveTitleColor ?: com.voicify.voicify_assistant_sdk.components.configurationToolbarProps?.speakInactiveTitleColor ?: getString(R.string.dark_gray)))
                HelperMethods.loadImageFromUrl(
                    url = toolbarProps?.micActiveImage ?: com.voicify.voicify_assistant_sdk.components.configurationToolbarProps?.micActiveImage ?: getString(R.string.mic_active_image),
                    view = micImage,
                    imageColor = toolbarProps?.micActiveColor ?: com.voicify.voicify_assistant_sdk.components.configurationToolbarProps?.micActiveColor
                )
                HelperMethods.loadImageFromUrl(
                    url = toolbarProps?.sendInactiveImage ?: com.voicify.voicify_assistant_sdk.components.configurationToolbarProps?.sendInactiveImage ?: getString(R.string.send_inactive_image),
                    view = sendMessage,
                    imageColor = toolbarProps?.sendInactiveColor ?: com.voicify.voicify_assistant_sdk.components.configurationToolbarProps?.sendInactiveColor
                )
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
                    val layoutParams1 = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, HelperMethods.getPixelsFromDp(320, scale))
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
                    val layoutParams1 = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, HelperMethods.getPixelsFromDp(0, scale))
                    layoutParams1.weight = 1f
                    bodyContainerLayout.layoutParams = layoutParams1
                    isKeyboardActive = false
                }
                else if(isKeyboardActive && isRotated && !isUsingSpeech) {
                    val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, HelperMethods.getPixelsFromDp(200, scale))
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


}