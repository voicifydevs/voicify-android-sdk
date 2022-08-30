package com.voicify.voicify_assistant_sdk.assistant

import android.annotation.SuppressLint
import android.app.ActionBar
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.View.OnTouchListener
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Guideline
import androidx.core.view.marginTop
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.squareup.picasso.Picasso
import com.voicify.voicify_assistant_sdk.R
import com.voicify.voicify_assistant_sdk.assistantDrawerUITypes.AssistantSettingsProps
import com.voicify.voicify_assistant_sdk.assistantDrawerUITypes.BodyProps
import com.voicify.voicify_assistant_sdk.assistantDrawerUITypes.HeaderProps
import com.voicify.voicify_assistant_sdk.assistantDrawerUITypes.ToolBarProps
import kotlinx.android.synthetic.main.fragment_assistant_drawer_u_i.*
import java.util.*


//https://medium.com/voice-tech-podcast/android-speech-to-text-tutorial-8f6fa71606ac -> speech recognition tutorial
//https://material.io/develop/android/components/bottom-sheet-dialog-fragment#modal-bottom-sheet -> scroll to bottom sheet modal
//https://stackoverflow.com/questions/9245408/best-practice-for-instantiating-a-new-android-fragment
//https://www.google.com/search?q=full-screen+modal+bottom+sheet&sxsrf=ALiCzsbgqSR_1FYkyqppJdl3gSfydK4EUA%3A1659104463691&ei=z-zjYondKeGeptQP1JqwiAQ&oq=full+screen+bottom+moda&gs_lcp=Cgdnd3Mtd2l6EAMYADIGCAAQHhAWMgYIABAeEBYyBQgAEIYDMgUIABCGAzIFCAAQhgM6BAgjECc6EQguEIAEELEDEIMBEMcBENEDOgUIABCRAjoFCAAQgAQ6CwgAEIAEELEDEIMBOgQIABBDOg4ILhCABBCxAxDHARDRAzoNCC4QsQMQgwEQ1AIQQzoKCAAQgAQQhwIQFDoNCC4QgAQQhwIQsQMQFDoNCAAQgAQQhwIQsQMQFDoTCC4QgAQQsQMQgwEQxwEQ0QMQCjoICAAQgAQQsQM6CAguEIAEELEDOgcIABCABBAKSgQIQRgASgQIRhgAUABYqixgnD1oA3AAeAGAAfsBiAHyD5IBBjIyLjEuMZgBAKABAcABAQ&sclient=gws-wiz#kpvalbx=_6uzjYtKNG9CHptQPjtKH2AU67
// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val HEADER = "header"
private const val BODY = "body"
private const val TOOLBAR = "toolBar"
private const val SETTINGS = "assistantSettings"

/**
 * A simple [Fragment] subclass.
 * Use the [AssistantDrawerUI.newInstance] factory method to
 * create an instance of this fragment.
 */
class AssistantDrawerUI : BottomSheetDialogFragment() {
    // TODO: Rename and change types of parameters
    private var headerProps: HeaderProps? = null
    private var bodyProps: BodyProps? = null
    private var toolBarProps: ToolBarProps? = null
    private var assistantSettingProps: AssistantSettingsProps? = null
    private var assistantIsListening: Boolean = false
    private var isUsingSpeech: Boolean = false
    private var isDrawer: Boolean = true
    private var voicifySTT: VoicifySTTProvider? = null
    private var voicifyTTS: VoicifyTTSProvider? = null
    private var sendMessageTextView: TextView? = null
    private var receiveMessageTextView: TextView? = null
    private var scale: Float = 0f
    //private var assistantState: AssistantState = AssistantState.Start
    private var bottomSheetBehavior : BottomSheetBehavior<View>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            headerProps = it.getSerializable(HEADER) as HeaderProps?
            bodyProps = it.getSerializable(BODY) as BodyProps?
            toolBarProps = it.getSerializable(TOOLBAR) as ToolBarProps?
            assistantSettingProps = it.getSerializable(SETTINGS) as AssistantSettingsProps
        }
    }

    @SuppressLint("ResourceType")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val window = inflater.inflate(R.layout.fragment_assistant_drawer_u_i, container, false)
        scale = requireContext().resources.displayMetrics.density
        //Layouts
        val drawerLayout = window.findViewById<LinearLayout>(R.id.drawerLayout)
        val fullScreenBodyLayout = window.findViewById<RelativeLayout>(R.id.bodyLayout)
        val fullScreenBodyContainerLayout = window.findViewById<RelativeLayout>(R.id.bodyLayoutContainer)
        val sendTextLayout = window.findViewById<LinearLayout>(R.id.sendTextLayout)
        val drawerFooterLayout = window.findViewById<LinearLayout>(R.id.drawerFooterLayout)

        val messagesScrollView = window.findViewById<ScrollView>(R.id.bodyLayoutScrollView)
        drawerLayout.setBackgroundColor(Color.parseColor(toolBarProps?.backgroundColor));

        //Image Views
        val micImageView = window.findViewById<ImageView>(R.id.micImageView)
        val closeAssistantImageView = window.findViewById<ImageView>(R.id.closeAssistantImageView)
        val sendMessageImageView = window.findViewById<ImageView>(R.id.sendMessageButtonImageView)
        val assistantAvatarImageView = window.findViewById<ImageView>(R.id.assistantAvatarImageView)
        val dashedLineImageView = window.findViewById<ImageView>(R.id.dashedLineImageView)

        loadImageFromUrl(if(assistantSettingProps?.initializeWithText == false) "https://voicify-prod-files.s3.amazonaws.com/99a803b7-5b37-426c-a02e-63c8215c71eb/daca643f-6730-4af5-8817-8d9d0d9db0b5/mic-image.png"
        else "https://voicify-prod-files.s3.amazonaws.com/99a803b7-5b37-426c-a02e-63c8215c71eb/3f10b6d7-eb71-4427-adbc-aadacbe8940e/mic-image-1-.png", micImageView)
        loadImageFromUrl("https://voicify-prod-files.s3.amazonaws.com/99a803b7-5b37-426c-a02e-63c8215c71eb/a6de04bb-e572-4a55-8cd9-1a7628285829/delete-2.png", closeAssistantImageView)
        loadImageFromUrl(if(assistantSettingProps?.initializeWithText == false) "https://voicify-prod-files.s3.amazonaws.com/99a803b7-5b37-426c-a02e-63c8215c71eb/0c5aa61c-7d6c-4272-abd2-75d9f5771214/Send-2-.png"
        else "https://voicify-prod-files.s3.amazonaws.com/99a803b7-5b37-426c-a02e-63c8215c71eb/7a39bc6f-eef5-4185-bcf8-2a645aff53b2/Send-3-.png", sendMessageImageView)
        loadImageFromUrl("https://voicify-prod-files.s3.amazonaws.com/99a803b7-5b37-426c-a02e-63c8215c71eb/eb7d2538-a3dc-4304-b58c-06fdb34e9432/Mark-Color-3-.png", assistantAvatarImageView)

        //Text Views
        val assistantStateTextView = window.findViewById<TextView>(R.id.assistantStateTextView)
        val spokenTextView = window.findViewById<TextView>(R.id.spokenTextView)
        val drawerWelcomeTextView = window.findViewById<TextView>(R.id.drawerWelcomeTextView)
        val typeTextView = window.findViewById<TextView>(R.id.typeTextView)
        val speakTextView = window.findViewById<TextView>(R.id.speakTextView)
        val inputTextMessageEditTextView = window.findViewById<EditText>(R.id.inputTextMessage)
        val assistantNameTextView = window.findViewById<TextView>(R.id.assistantNameTextView)

        //Space Views
        val bodySpaceView = window.findViewById<Space>(R.id.bodySpace)

        //set Text View Styles
        typeTextView.setTextColor(if(assistantSettingProps?.initializeWithText == false) Color.parseColor("#8F97A1") else Color.parseColor("#3E77A5"))
        drawerWelcomeTextView.text = "How can i help?"
        drawerWelcomeTextView.setTextColor(Color.parseColor("#8F97A1"))
        drawerWelcomeTextView.textSize = 18f
        assistantStateTextView.textSize = 16f
        spokenTextView.textSize = 16f
        assistantStateTextView.setTextColor(Color.parseColor("#8F97A1"))
        isUsingSpeech = assistantSettingProps?.initializeWithText == false
        assistantNameTextView.text = headerProps?.assistantName ?: ""
        assistantNameTextView.textSize = headerProps?.assistantNameFontSize?.toFloat() ?: 18f

        //Create Styles
        val spokenTextViewStyle = GradientDrawable()
        spokenTextViewStyle.cornerRadius = 24f
        spokenTextViewStyle.setColor(Color.parseColor("#80000000"))
        spokenTextView.background = spokenTextViewStyle

        val micImageViewStyle = GradientDrawable()
        micImageViewStyle.setColor(Color.parseColor("#1f1e7eb9"))
        micImageViewStyle.cornerRadius = 100f

        val sendTextLayoutStyle = GradientDrawable()
        sendTextLayoutStyle.setColor(Color.parseColor("#1f1e7eb9"))
        sendTextLayoutStyle.cornerRadius = 24f

        val fullScreenBodyLayoutStyle = GradientDrawable()
        fullScreenBodyLayoutStyle.setStroke(4, Color.parseColor("#CBCCD2"))
        fullScreenBodyLayoutStyle.setColor(Color.parseColor("#F4F4F6"))
        fullScreenBodyContainerLayout.background = fullScreenBodyLayoutStyle

        val inputTextMessageEditTextViewStyle = GradientDrawable()
        inputTextMessageEditTextViewStyle.setColor(Color.parseColor("#1f1e7eb9"))

        //initialization
        voicifyTTS = VoicifyTTSProvider(VoicifyTextToSpeechSettings(
        appId = "99a803b7-5b37-426c-a02e-63c8215c71eb",
        appKey = "MTAzM2RjNDEtMzkyMC00NWNhLThhOTYtMjljMDc3NWM5NmE3",
        voice = "",
        serverRootUrl = "https://assistant.voicify.com",
        provider = "google"))
        voicifySTT = VoicifySTTProvider(requireContext(), requireActivity())
        val assistant = VoicifyAssistant(voicifySTT, voicifyTTS, VoicifyAssistantSettings(
            appId = assistantSettingProps!!.appId,
            appKey = assistantSettingProps!!.appKey,
            serverRootUrl = assistantSettingProps!!.serverRootUrl,
            locale = assistantSettingProps!!.locale,
            channel = assistantSettingProps!!.channel,
            device = assistantSettingProps!!.device,
            autoRunConversation = assistantSettingProps!!.autoRunConversation,
            initializeWithWelcomeMessage = assistantSettingProps!!.initializeWithWelcomeMessage,
            useVoiceInput = assistantSettingProps!!.useVoiceInput,
            useOutputSpeech = assistantSettingProps!!.useOutputSpeech))

        assistant.initializeAndStart()
        assistant.startNewSession()
        if(assistantSettingProps?.initializeWithText == false)
        {
            voicifySTT?.startListening()
        }

        //Listeners
        //STT
        voicifySTT?.addPartialListener { partialResult ->
            spokenTextView.text = partialResult
        }
        voicifySTT?.addFinalResultListener { fullResult ->
            assistantIsListening = false
            spokenTextView.text = fullResult
            sendMessageTextView = TextView(context)
            sendMessageTextView?.id = View.generateViewId()
            val sendMessageTextViewParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
            sendMessageTextViewParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            if(receiveMessageTextView != null)
            {
                sendMessageTextViewParams.addRule(RelativeLayout.BELOW, receiveMessageTextView?.id!!)
            }
            sendMessageTextViewParams.setMargins(0,getPixelsFromDp(12),0,0)
            sendMessageTextView?.setTextColor(Color.WHITE)
            sendMessageTextView?.layoutParams = sendMessageTextViewParams
            sendMessageTextView?.setPadding(10,10,10,10)
            sendMessageTextView?.setBackgroundColor(Color.parseColor("#80000000"))
            sendMessageTextView?.text = fullResult
            sendMessageTextView?.textSize = 14f
            fullScreenBodyLayout.addView(sendMessageTextView)
            messagesScrollView.post {
                messagesScrollView.fullScroll(View.FOCUS_DOWN)
            }
            assistantStateTextView.text = "Processing..."
            assistant.makeTextRequest(fullResult.toString(), null, "Speech")
        }
        voicifySTT?.addSpeechReadyListener {
            micImageView.background = micImageViewStyle
            assistantIsListening = true;
            assistantStateTextView.text = "Listening..."
        }
        voicifySTT?.addErrorListener { error ->
            Log.d("JAMES", error)
            if (error == "7")
            {
                assistantIsListening = false
                micImageView.setBackgroundColor(Color.TRANSPARENT)
                assistantStateTextView.text = "I didn't catch that..."
            }
        }
        //        voicifySTT.addVolumeListener { volume ->
//            if(assistantState != AssistantState.Speaking)
//            {
//                //assistantState = AssistantState.Speaking
//            }
//        }
        //Assistant
        assistant.onResponseReceived { response ->
            if(!response.endSession)
            {
                activity?.runOnUiThread{
                    if(!isUsingSpeech)
                    {
                        val drawerFooterLayoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                        drawerFooterLayoutParams.setMargins(0,0,0,0)
                        drawerFooterLayout.layoutParams = drawerFooterLayoutParams
                    }
                    bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
                    if(!isUsingSpeech)
                    {
                        dashedLineImageView.visibility = View.INVISIBLE;
                    }
                    isDrawer = false
                    micImageView.setBackgroundColor(Color.TRANSPARENT)
                    val metrics = activity?.resources?.displayMetrics
                    var params = drawerLayout.layoutParams
                    params.height = metrics?.heightPixels as Int
                    drawerLayout.layoutParams = params
                    assistantStateTextView.text = ""
                    drawerWelcomeTextView.text = ""
                    spokenTextView.text = ""
                    assistantAvatarImageView.visibility = View.VISIBLE
                    assistantNameTextView.visibility = View.VISIBLE
                    fullScreenBodyContainerLayout.visibility = View.VISIBLE
                    bodySpaceView.visibility = View.GONE

                    receiveMessageTextView = TextView(context)
                    receiveMessageTextView?.id = View.generateViewId()
                    val receiveMessageTextViewParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
                    receiveMessageTextViewParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                    receiveMessageTextViewParams.addRule(RelativeLayout.BELOW,
                        sendMessageTextView?.id!!
                    )
                    receiveMessageTextViewParams.setMargins(0,getPixelsFromDp(12),0,0)
                    receiveMessageTextView?.layoutParams = receiveMessageTextViewParams
                    receiveMessageTextView?.setPadding(0,10,0,0)
                    receiveMessageTextView?.setTextColor(Color.BLACK)
                    receiveMessageTextView?.setBackgroundColor(Color.parseColor("#0d000000"))
                    receiveMessageTextView?.text = response.displayText
                    receiveMessageTextView?.textSize = 14f
                    fullScreenBodyLayout.addView(receiveMessageTextView)
                    messagesScrollView.post {
                        messagesScrollView.fullScroll(View.FOCUS_DOWN)
                    }
                }
            }
        }

        //Views
        micImageView.setOnClickListener{
            if(!isUsingSpeech)
            {
                isUsingSpeech = true
                sendTextLayout.setBackgroundColor(Color.TRANSPARENT)
                dashedLineImageView.visibility = View.VISIBLE;
                hideKeyboard()
                val drawerLayoutParams = drawerLayout.layoutParams
                if(isDrawer)
                {
                    drawerLayoutParams.height = getPixelsFromDp(350)
                }
                else{
                    val drawerFooterLayoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    drawerFooterLayoutParams.setMargins(0,getPixelsFromDp(20),0,0)
                    drawerFooterLayout.layoutParams = drawerFooterLayoutParams
                }
                drawerLayout.layoutParams = drawerLayoutParams
                spokenTextView.visibility = View.VISIBLE
                assistantStateTextView.visibility = View.VISIBLE
                speakTextView.setTextColor(Color.parseColor("#3E77A5"))
                typeTextView.setTextColor(Color.parseColor("#8F97A1"))
                loadImageFromUrl("https://voicify-prod-files.s3.amazonaws.com/99a803b7-5b37-426c-a02e-63c8215c71eb/daca643f-6730-4af5-8817-8d9d0d9db0b5/mic-image.png", micImageView)
                loadImageFromUrl("https://voicify-prod-files.s3.amazonaws.com/99a803b7-5b37-426c-a02e-63c8215c71eb/0c5aa61c-7d6c-4272-abd2-75d9f5771214/Send-2-.png", sendMessageImageView)
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
                assistantSettingProps?.onEffect?.invoke(effect, data)
            }
        }

        sendMessageImageView.setOnClickListener{
            if(inputTextMessageEditTextView.text.toString().isNotEmpty())
            {
                sendMessageTextView = TextView(context)
                sendMessageTextView?.id = View.generateViewId()
                val sendMessageTextViewParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
                sendMessageTextViewParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                if(receiveMessageTextView != null)
                {
                    sendMessageTextViewParams.addRule(RelativeLayout.BELOW, receiveMessageTextView?.id!!)
                }
                sendMessageTextViewParams.setMargins(0,getPixelsFromDp(12),0,0)
                sendMessageTextView?.setTextColor(Color.WHITE)
                sendMessageTextView?.layoutParams = sendMessageTextViewParams
                sendMessageTextView?.setPadding(10,10,10,10)
                sendMessageTextView?.setBackgroundColor(Color.parseColor("#80000000"))
                sendMessageTextView?.text = inputTextMessageEditTextView.text
                sendMessageTextView?.textSize = 14f
                fullScreenBodyLayout.addView(sendMessageTextView)
                messagesScrollView.post {
                    messagesScrollView.fullScroll(View.FOCUS_DOWN)
                }
                val inputText = inputTextMessageEditTextView.text.toString()
                inputTextMessageEditTextView.setText("")
                hideKeyboard()
                assistant.makeTextRequest(inputText,null, "Text")
            }
        }

        inputTextMessageEditTextView.setOnTouchListener(object : OnTouchListener {
            @SuppressLint("ClickableViewAccessibility")
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> //Do Something
                    Log.d("JAMES", "down")
                }
                when(event?.action) {
                    MotionEvent.ACTION_UP -> {
                        if (assistantIsListening) {
                            cancelSpeech()
                            assistantStateTextView.text = ""
                            spokenTextView.text = ""
                        }
                        if (isUsingSpeech) {
                            sendTextLayout.background = sendTextLayoutStyle
                            isUsingSpeech = false
                            if(!isDrawer)
                            {
                                val drawerFooterLayoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                                drawerFooterLayoutParams.setMargins(0,0,0,0)
                                drawerFooterLayout.layoutParams = drawerFooterLayoutParams
                                dashedLineImageView.visibility = View.INVISIBLE;
                            }
                            //spokenTextView.isVisible = false
                            spokenTextView.visibility = View.GONE
                            //assistantStateTextView.isVisible = false;
                            assistantStateTextView.visibility = View.GONE
                            val drawerLayoutParams = drawerLayout.layoutParams
                            if(isDrawer) {
                                drawerLayoutParams.height = getPixelsFromDp(200)
                            }

                            drawerLayout.layoutParams = drawerLayoutParams
                            speakTextView.setTextColor(Color.parseColor("#8F97A1"))
                            typeTextView.setTextColor(Color.parseColor("#3E77A5"))
                            loadImageFromUrl(
                                "https://voicify-prod-files.s3.amazonaws.com/99a803b7-5b37-426c-a02e-63c8215c71eb/3f10b6d7-eb71-4427-adbc-aadacbe8940e/mic-image-1-.png",
                                micImageView
                            )
                            loadImageFromUrl(
                                "https://voicify-prod-files.s3.amazonaws.com/99a803b7-5b37-426c-a02e-63c8215c71eb/7a39bc6f-eef5-4185-bcf8-2a645aff53b2/Send-3-.png",
                                sendMessageImageView
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

    fun Fragment.hideKeyboard() {
        view?.let { activity?.hideKeyboard(it) }
    }

    fun Activity.hideKeyboard() {
        hideKeyboard(currentFocus ?: View(this))
    }

    fun Context.hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun cancelSpeech() {
        voicifySTT?.stopListening()
        voicifySTT?.cancelListening()
        voicifySTT?.destoryInstance()
        assistantIsListening = false
        micImageView.setBackgroundColor(Color.TRANSPARENT)
        spokenTextView.text =  ""
    }

    private fun loadImageFromUrl(url: String, view: ImageView){
        Picasso.get().load(url).into(view)
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

    override fun onDismiss(dialog: DialogInterface) {
        cancelSpeech()
        voicifyTTS?.stop()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param headerProps Parameter 1.
         * @param bodyProps Parameter 2.
         * @param toolBarProps Parameter 3.
         * @return A new instance of fragment AssistantDrawerUI.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(headerProps: HeaderProps, bodyProps: BodyProps, toolBarProps: ToolBarProps, assistantSettingsProps: AssistantSettingsProps) =
            AssistantDrawerUI().apply {
                arguments = Bundle().apply {
                    putSerializable(HEADER, headerProps)
                    putSerializable(BODY, bodyProps)
                    putSerializable(TOOLBAR, toolBarProps)
                    putSerializable(SETTINGS, assistantSettingsProps)
                }
            }
    }
}