package com.voicify.voicify_assistant_sdk.components

import android.animation.AnimatorSet
import android.content.*
import android.content.Context.MODE_PRIVATE
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.speech.SpeechRecognizer
import android.view.*
import android.view.View.OnTouchListener
import android.widget.*
import androidx.core.animation.doOnEnd
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.Gson
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
import kotlinx.android.synthetic.main.fragment_assistant_drawer_u_i.view.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import java.lang.Exception

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
    private var window: View? = null
    private var assistantDrawerUIHeader = AssistantDrawerUIHeader()
    private val assistantDrawerUIBody = AssistantDrawerUIBody()
    private var assistantDrawerUIToolbar = AssistantDrawerUIToolbar()
    private var speakAnimation = SpeakingAnimation()
    private var speakingAnimationBars: Array<View>? = null
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
        window = inflater.inflate(R.layout.fragment_assistant_drawer_u_i, container, false)

        val hintsList = ArrayList<String>()
        val messagesList = ArrayList<Message>()
        speakingAnimationBars = arrayOf(
            window!!.speakingAnimationBar1,
            window!!.speakingAnimationBar2,
            window!!.speakingAnimationBar3,
            window!!.speakingAnimationBar4,
            window!!.speakingAnimationBar5,
            window!!.speakingAnimationBar6,
            window!!.speakingAnimationBar7,
            window!!.speakingAnimationBar8
        )

        scale = requireContext().resources.displayMetrics.density
        isUsingSpeech = (assistantSettingProps?.initializeWithText == true || configurationKotlin?.activeInput == getString(R.string.textbox)) != true && (assistantSettingProps?.useVoiceInput ?: configurationKotlin?.useVoiceInput) != false
        window?.activityIndicator?.setBackgroundColor(Color.parseColor(getString(R.string.black_60_percent)))

        val sendTextLayoutStyle = GradientDrawable()
        sendTextLayoutStyle.setColor(Color.parseColor(toolbarProps?.textboxActiveHighlightColor ?: configurationToolbarProps?.textboxActiveHighlightColor ?: getString(R.string.blue_12_percent)))
        sendTextLayoutStyle.cornerRadius = 24f

        if(!isLoadingConfiguration)
        {
            initializeDrawerUI(messagesList, hintsList, requireContext(), sendTextLayoutStyle)
        }

        loginResponseReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                activity?.runOnUiThread{
                    initializeDrawerUI(messagesList, hintsList, requireContext(), sendTextLayoutStyle)
                }
            }
        }
        NotificationCenter.addObserver(requireContext(), NotificationType.LOADING_COMPLETE, loginResponseReceiver)
        
        if(isLoadingConfiguration)
        {
            window?.container?.visibility = View.GONE
            window?.activityIndicator?.visibility = View.VISIBLE
        }

        // Inflate the layout for this fragment
        return window as View
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bottomSheetBehavior = BottomSheetBehavior.from((view.parent as View))
        bottomSheetBehavior?.isDraggable = false
        bottomSheetBehavior?.maxWidth = ViewGroup.LayoutParams.MATCH_PARENT
        bottomSheetBehavior?.peekHeight = HelperMethods.getPixelsFromDp(500, scale)
        if(!isLoadingConfiguration) {
            checkInitializeWithWelcome()
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

    private fun initializeDrawerUI(messagesList: ArrayList<Message>, hintsList: ArrayList<String>, context: Context, sendTextLayoutStyle: GradientDrawable){
        val assistant = initializeAssistant()
        val onHintClicked: (String) -> Unit = {  hint ->
            messagesList.add(Message(hint, getString(R.string.sent)))
            speakAnimation.clearAnimationValues(animation, context, speakingAnimationBars)
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
            closeAssistantImageView = window!!.closeAssistantImageView,
            avatarImageView = window!!.assistantAvatarImageView,
            closeBackgroundLayout =  window!!.closeAssistantBackgroundContainerLayout,
            avatarBackgroundLayout = window!!.assistantAvatarBackgroundContainerLayout,
            assistantNameTextView = window!!.assistantNameTextView,
            context = requireContext(),
            headerProps = headerProps,
            configurationHeaderProps = configurationHeaderProps,
            resources = resources
        )
        val (messagesAdapter,hintsAdapter) = assistantDrawerUIBody.initializeBody(
            bodyBorderTopView = window!!.bodyBorderTopView,
            bodyBorderBottomView = window!!.bodyBorderBottomView,
            bodyLayout = window!!.bodyContainerLayout,
            messagesRecycler = window!!.messagesRecyclerView,
            hintsRecycler = window!!.hintsRecyclerView,
            messages = messagesList,
            hints = hintsList,
            onHintClicked = onHintClicked,
            context = requireContext(),
            bodyProps = bodyProps,
            configurationBodyProps = configurationBodyProps,
            assistantSettingProps = assistantSettingProps,
            configuration = configurationKotlin,
        )
        messagesRecyclerViewAdapter = messagesAdapter
        hintsRecyclerViewAdapter = hintsAdapter
        assistantDrawerUIToolbar.initializeToolbar(
            micImageView = window!!.micImageView,
            sendMessageImageView = window!!.sendMessageButtonImageView,
            speakTextView = window!!.speakTextView,
            typeTextView = window!!.typeTextView,
            drawerHelpTextView = window!!.drawerWelcomeTextView,
            assistantStateTextView = window!!.assistantStateTextView,
            spokenTextView = window!!.spokenTextView,
            inputeMessageEditText = window!!.inputTextMessage,
            drawerLayout = window!!.drawerLayout,
            context = requireContext(),
            toolbarProps = toolbarProps,
            configurationToolbarProps = configurationToolbarProps,
            assistantSettingProps = assistantSettingProps,
            configuration = configurationKotlin,
        )
        speakAnimation.initializeSpeakingAnimation(
            context = requireContext(),
            toolbarProps = toolbarProps,
            configurationToolbarProps = configurationToolbarProps,
            animationBars = speakingAnimationBars ?: emptyArray()
        )
        window?.container?.visibility = View.VISIBLE
        window?.activityIndicator?.visibility = View.GONE

        addGradientBackground()
        //UI Initialization
        checkInitializeWithText(sendTextLayoutStyle)
        checkInitializeWithWelcome()
        startNewAssistantSession(assistant)

        //Add Listeners
        addKeyboardActiveListener(window)
        addSpeechToTextListeners(assistant, speakAnimation)
        addMicClickListener(speakAnimation)
        addSendMessageClickListener(assistant, messagesList)
        addAssistantHandlers(assistant, hintsList, messagesList)
        addTextboxClickListener(sendTextLayoutStyle)

        window?.closeAssistantImageView?.setOnClickListener{
            dismiss()
        }
        window?.closeAssistantNoInternetImageView?.setOnClickListener{
            dismiss()
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
                initializeWithText = assistantSettingProps?.initializeWithText == true || (configurationKotlin?.activeInput == getString(R.string.textbox)),
                useVoiceInput = assistantSettingProps?.useVoiceInput ?: configurationKotlin?.useVoiceInput ?: true,
                useDraftContent = assistantSettingProps?.useDraftContent ?: configurationKotlin?.useDraftContent ?: false,
                useOutputSpeech = assistantSettingProps?.useOutputSpeech ?: configurationKotlin?.useOutputSpeech ?: true
            )
        )
    }

    private fun checkInitializeWithText(sendTextLayoutStyle: GradientDrawable){
        if((assistantSettingProps?.initializeWithText == true || configurationKotlin?.activeInput == getString(R.string.textbox)) || (assistantSettingProps?.useVoiceInput ?: configurationKotlin?.useVoiceInput) == false)
        {
            window?.speakingAnimation?.visibility = View.GONE
            window?.sendTextLayout?.background = sendTextLayoutStyle
            isUsingSpeech = false
            window?.spokenTextView?.visibility = View.GONE
            window?.assistantStateTextView?.visibility = View.GONE
        }
    }

    private fun checkInitializeWithWelcome(){
        if((assistantSettingProps?.initializeWithWelcomeMessage ?: configurationKotlin?.initializeWithWelcomeMessage) == true)
        {
            isDrawer = false
            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED

            assistantDrawerUIHeader.setFullScreenView(
                assistantAvatarImageView = window?.assistantAvatarImageView,
                assistantNameTextView = window?.assistantNameTextView,
                assistantAvatarBackgroundContainerLayout = window?.assistantAvatarBackgroundContainerLayout,
                headerLayout = window?.headerLayout,
                headerProps = headerProps,
                configurationHeaderProps = configurationHeaderProps,
                context = requireContext(),
                assistantSettingProps = assistantSettingProps,
                configuration = configurationKotlin
            )

            assistantDrawerUIBody.setFullScreenView(
                bodyContainerLayout = window?.bodyContainerLayout,
                hintsRecyclerView = window?.hintsRecyclerView,
                messagesRecyclerView = window?.messagesRecyclerView,
                bodyBorderTopView = window?.bodyBorderTopView,
                bodyBorderBottomView = window?.bodyBorderBottomView
            )

            assistantDrawerUIToolbar.setFullScreenView(
                drawerLayout = window?.drawerLayout,
                spokenTextView = window?.spokenTextView,
                assistantStateTextView = window?.assistantStateTextView,
                drawerWelcomeTextView = window?.drawerWelcomeTextView,
                toolbarLayout = window?.toolbarLayout,
                isUsingSpeech = isUsingSpeech,
                context = requireContext(),
                toolbarProps = toolbarProps,
                configurationToolbarProps = configurationToolbarProps,
                micImageView = window?.micImageView,
                drawerFooterLayout = window?.drawerFooterLayout,
                dashedLineImageView = window?.dashedLineImageView,
                configuration = configurationKotlin,
                assistantSettingProps = assistantSettingProps
            )

            messagesRecyclerViewAdapter?.notifyDataSetChanged()

            if(messagesRecyclerViewAdapter?.itemCount ?: 0 > 0)
            {
                window?.messagesRecyclerView?.smoothScrollToPosition(messagesRecyclerViewAdapter?.itemCount as Int)
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
        if((assistantSettingProps?.initializeWithText == true || configurationKotlin?.activeInput == getString(R.string.textbox)) != true && (assistantSettingProps?.useVoiceInput ?: configurationKotlin?.useVoiceInput) != false && (assistantSettingProps?.initializeWithWelcomeMessage ?: configurationKotlin?.initializeWithWelcomeMessage) != true)
        {
            voicifySTT?.startListening()
        }
    }

    private fun addAssistantHandlers(assistant: VoicifyAssistant, hintsList: ArrayList<String>, messagesList: ArrayList<Message>){
        assistant.onResponseReceived { response ->
            activity?.runOnUiThread{
                window?.closeAssistantNoInternetImageView?.visibility = View.GONE
                bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
                isDrawer = false
                assistantDrawerUIHeader.setFullScreenView(
                    assistantAvatarImageView = window?.assistantAvatarImageView,
                    assistantNameTextView = window?.assistantNameTextView,
                    assistantAvatarBackgroundContainerLayout = window?.assistantAvatarBackgroundContainerLayout,
                    headerLayout = window?.headerLayout,
                    headerProps = headerProps,
                    configurationHeaderProps = configurationHeaderProps,
                    context = requireContext(),
                    assistantSettingProps = assistantSettingProps,
                    configuration = configurationKotlin
                )

                assistantDrawerUIBody.setFullScreenView(
                    bodyContainerLayout = window?.bodyContainerLayout,
                    hintsRecyclerView = window?.hintsRecyclerView,
                    messagesRecyclerView = window?.messagesRecyclerView,
                    bodyBorderTopView = window?.bodyBorderTopView,
                    bodyBorderBottomView = window?.bodyBorderBottomView
                )

                assistantDrawerUIToolbar.setFullScreenView(
                    drawerLayout = window?.drawerLayout,
                    spokenTextView = window?.spokenTextView,
                    assistantStateTextView = window?.assistantStateTextView,
                    drawerWelcomeTextView = window?.drawerWelcomeTextView,
                    toolbarLayout = window?.toolbarLayout,
                    isUsingSpeech = isUsingSpeech,
                    context = requireContext(),
                    toolbarProps = toolbarProps,
                    configurationToolbarProps = configurationToolbarProps,
                    micImageView = window?.micImageView,
                    drawerFooterLayout = window?.drawerFooterLayout,
                    dashedLineImageView = window?.dashedLineImageView,
                    configuration = configurationKotlin,
                    assistantSettingProps = assistantSettingProps
                )

                assistantDrawerUIBody.setHints(
                    response = response,
                    hintsList = hintsList,
                    hintsRecyclerViewAdapter = hintsRecyclerViewAdapter,
                    hintsRecyclerView = window?.hintsRecyclerView
                )

                if(isDrawer)
                {
                    hideKeyboard()
                    isKeyboardActive = false
                }

                if(!speechFullResult.isNullOrEmpty())
                {
                    messagesList.add(Message(speechFullResult as String, getString(R.string.sent)))
                    messagesRecyclerViewAdapter?.notifyDataSetChanged()
                    window?.messagesRecyclerView?.smoothScrollToPosition(messagesRecyclerViewAdapter?.itemCount as Int)
                }

                speechFullResult = null
                messagesList.add(Message(response.displayText?.trim() as String, getString(R.string.received)))
                messagesRecyclerViewAdapter?.notifyDataSetChanged()
                window?.messagesRecyclerView?.smoothScrollToPosition(messagesRecyclerViewAdapter?.itemCount as Int)
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

    private fun addSpeechToTextListeners(assistant: VoicifyAssistant, speakAnimation: SpeakingAnimation){
        voicifySTT?.addPartialListener { partialResult ->
            window?.spokenTextView?.setTextColor(Color.parseColor(toolbarProps?.partialSpeechResultTextColor ?: configurationToolbarProps?.partialSpeechResultTextColor ?: getString(R.string.white_20_percent)))
            window?.spokenTextView?.text = partialResult
        }
        voicifySTT?.addFinalResultListener { fullResult ->
            window?.spokenTextView?.setTextColor(Color.parseColor(toolbarProps?.fullSpeechResultTextColor ?: configurationToolbarProps?.fullSpeechResultTextColor ?: getString(R.string.white)))
            speechFullResult = fullResult
            speakAnimation.clearAnimationValues(animation, requireContext(), speakingAnimationBars)
            assistantIsListening = false
            window?.spokenTextView?.text = fullResult
            window?.assistantStateTextView?.text = getString(R.string.assistant_state_processing)
            assistant.makeTextRequest(fullResult.toString(), null, getString(R.string.speech))
        }

        voicifySTT?.addSpeechReadyListener {
            val micImageViewStyle = GradientDrawable()
            micImageViewStyle.setColor(Color.parseColor(toolbarProps?.micActiveHighlightColor ?: configurationToolbarProps?.micActiveHighlightColor ?: getString(R.string.blue_12_percent)))
            micImageViewStyle.setStroke(toolbarProps?.micImageBorderWidth ?: configurationToolbarProps?.micImageBorderWidth ?: 0, Color.parseColor(toolbarProps?.micImageBorderColor ?: configurationToolbarProps?.micImageBorderColor ?: getString(R.string.transparent)))
            micImageViewStyle.cornerRadius = toolbarProps?.micBorderRadius ?: configurationToolbarProps?.micBorderRadius ?: 100f
            window?.micImageView?.background = micImageViewStyle
            assistantIsListening = true
            window?.assistantStateTextView?.text = getString(R.string.assistant_state_listening)
        }

        voicifySTT?.addErrorListener { error ->
            speakAnimation.clearAnimationValues(animation, requireContext(), speakingAnimationBars)
            if (error == SpeechRecognizer.ERROR_NO_MATCH.toString())
            {
                assistantIsListening = false
                window?.micImageView?.setBackgroundColor(Color.parseColor(toolbarProps?.micInactiveHighlightColor ?: configurationToolbarProps?.micInactiveHighlightColor ?: getString(R.string.transparent)))
                window?.assistantStateTextView?.text = getString(R.string.assistant_state_misunderstood)
            }
        }

        voicifySTT?.addVolumeListener { volume ->
            val bars = speakAnimation.generateAnimationValues(volume, requireContext(), speakingAnimationBars)
            animation = AnimatorSet().apply {
                playTogether(bars)
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

    private fun addSendMessageClickListener(assistant: VoicifyAssistant, messagesList: ArrayList<Message>) {
        window?.sendMessageButtonImageView?.setOnClickListener{
            if(window?.inputTextMessage?.text.toString().isNotEmpty())
            {
                messagesList.add(Message(window?.inputTextMessage?.text.toString(), getString(R.string.sent)))
                messagesRecyclerViewAdapter?.notifyDataSetChanged()
                window?.messagesRecyclerView?.smoothScrollToPosition(messagesRecyclerViewAdapter?.itemCount as Int)
                val inputText = window?.inputTextMessage?.text.toString()
                window?.inputTextMessage?.setText("")
                hideKeyboard()
                assistant.makeTextRequest(inputText,null, getString(R.string.text))
            }
        }
    }

    private fun addTextboxClickListener(sendTextLayoutStyle: GradientDrawable) {
        window?.inputTextMessage?.setOnTouchListener(object : OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when(event?.action) {
                    MotionEvent.ACTION_UP -> {
                        val colorStateList = ColorStateList.valueOf(Color.parseColor(toolbarProps?.textInputActiveLineColor ?: configurationToolbarProps?.textInputLineColor ?: getString(R.string.silver)))
                        ViewCompat.setBackgroundTintList(window?.inputTextMessage!!,colorStateList)
                        if (isUsingSpeech) {
                            voicifySTT?.cancel = true
                            isUsingSpeech = false
                            if (assistantIsListening) {
                                cancelSpeech()
                            }
                            assistantDrawerUIToolbar.setIsUsingTextView(
                                isRotated = isRotated,
                                isDrawer = isDrawer,
                                bodyContainerLayout = window?.bodyContainerLayout,
                                assistantStateTextView = window?.assistantStateTextView,
                                spokenTextView = window?.spokenTextView,
                                speakingAnimationLayout = window?.speakingAnimation,
                                sendTextLayout = window?.sendTextLayout,
                                sendTextLayoutStyle = sendTextLayoutStyle,
                                drawerFooterLayout = window?.drawerFooterLayout,
                                dashedLineImageView = window?.dashedLineImageView,
                                speakTextView = window?.speakTextView,
                                typeTextView = window?.typeTextView,
                                toolbarProps = toolbarProps,
                                configurationToolbarProps = configurationToolbarProps,
                                context = requireContext()
                            )
                            HelperMethods.loadImageFromUrl(
                                url = toolbarProps?.micInactiveImage ?: configurationToolbarProps?.micInactiveImage ?: getString(R.string.mic_inactive_image),
                                view = window!!.micImageView,
                                imageColor = toolbarProps?.micInactiveColor ?: configurationToolbarProps?.micInactiveColor
                            )
                            HelperMethods.loadImageFromUrl(
                                url = toolbarProps?.sendActiveImage ?: configurationToolbarProps?.sendActiveImage ?: getString(R.string.send_active_image),
                                view = window!!.sendMessageButtonImageView,
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

    private fun addMicClickListener(speakAnimation: SpeakingAnimation){
        window?.micImageView?.setOnClickListener{
            speakAnimation.clearAnimationValues(animation, requireContext(), speakingAnimationBars)
            val colorStateList = ColorStateList.valueOf(Color.parseColor(toolbarProps?.textInputLineColor ?: configurationToolbarProps?.textInputLineColor ?: getString(R.string.silver)))
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
                window?.messagesRecyclerView?.smoothScrollToPosition(messagesRecyclerViewAdapter?.itemCount as Int)
                assistantDrawerUIToolbar.setIsUsingSpeechView(
                    speakinAnimationLayout = window?.speakingAnimation,
                    sendTextLayout = window?.sendTextLayout,
                    dashedLineImageView = window?.dashedLineImageView,
                    toolbarProps = toolbarProps,
                    configurationToolbarProps = configurationToolbarProps,
                    context = requireContext(),
                    isDrawer = isDrawer,
                    drawerFooterLayout = window?.drawerFooterLayout,
                    spokenTextView = window?.spokenTextView,
                    assistantStateTextView = window?.assistantStateTextView,
                    speakTextView = window?.speakTextView,
                    typeTextView = window?.typeTextView
                )
                hideKeyboard()
                HelperMethods.loadImageFromUrl(
                    url = toolbarProps?.micActiveImage ?: configurationToolbarProps?.micActiveImage ?: getString(R.string.mic_active_image),
                    view = window!!.micImageView,
                    imageColor = toolbarProps?.micActiveColor ?: configurationToolbarProps?.micActiveColor
                )
                HelperMethods.loadImageFromUrl(
                    url = toolbarProps?.sendInactiveImage ?: configurationToolbarProps?.sendInactiveImage ?: getString(R.string.send_inactive_image),
                    view = window!!.sendMessageButtonImageView,
                    imageColor = toolbarProps?.sendInactiveColor ?: configurationToolbarProps?.sendInactiveColor
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
        window?.micImageView?.setBackgroundColor(Color.parseColor(toolbarProps?.micInactiveHighlightColor ?: configurationToolbarProps?.micInactiveHighlightColor ?: getString(R.string.transparent)))
        window?.spokenTextView?.text =  ""
    }

    private fun addKeyboardActiveListener(window: View?){
        window?.viewTreeObserver?.addOnGlobalLayoutListener {
            val r = Rect()
            window?.getWindowVisibleDisplayFrame(r)
            val heightDiff = window?.rootView.height - (r.bottom - r.top)
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

    private fun addGradientBackground(){
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
                window?.container?.background = gradientDrawable
            }
            else
            {
                window?.container?.setBackgroundColor(Color.parseColor(assistantSettingProps?.backgroundColor))
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
                window?.container?.background = gradientDrawable
            }
            else
            {
                window?.container?.setBackgroundColor(Color.parseColor(configurationKotlin?.styles?.assistant?.backgroundColor))
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