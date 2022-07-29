package com.voicify.voicify_assistant_sdk.assistant

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.squareup.picasso.Picasso
import com.voicify.voicify_assistant_sdk.R
import com.voicify.voicify_assistant_sdk.assistantDrawerUITypes.BodyProps
import com.voicify.voicify_assistant_sdk.assistantDrawerUITypes.HeaderProps
import com.voicify.voicify_assistant_sdk.assistantDrawerUITypes.ToolBarProps


//https://medium.com/voice-tech-podcast/android-speech-to-text-tutorial-8f6fa71606ac -> speech recognition tutorial
//https://material.io/develop/android/components/bottom-sheet-dialog-fragment#modal-bottom-sheet -> scroll to bottom sheet modal
//https://stackoverflow.com/questions/9245408/best-practice-for-instantiating-a-new-android-fragment
//https://www.google.com/search?q=full-screen+modal+bottom+sheet&sxsrf=ALiCzsbgqSR_1FYkyqppJdl3gSfydK4EUA%3A1659104463691&ei=z-zjYondKeGeptQP1JqwiAQ&oq=full+screen+bottom+moda&gs_lcp=Cgdnd3Mtd2l6EAMYADIGCAAQHhAWMgYIABAeEBYyBQgAEIYDMgUIABCGAzIFCAAQhgM6BAgjECc6EQguEIAEELEDEIMBEMcBENEDOgUIABCRAjoFCAAQgAQ6CwgAEIAEELEDEIMBOgQIABBDOg4ILhCABBCxAxDHARDRAzoNCC4QsQMQgwEQ1AIQQzoKCAAQgAQQhwIQFDoNCC4QgAQQhwIQsQMQFDoNCAAQgAQQhwIQsQMQFDoTCC4QgAQQsQMQgwEQxwEQ0QMQCjoICAAQgAQQsQM6CAguEIAEELEDOgcIABCABBAKSgQIQRgASgQIRhgAUABYqixgnD1oA3AAeAGAAfsBiAHyD5IBBjIyLjEuMZgBAKABAcABAQ&sclient=gws-wiz#kpvalbx=_6uzjYtKNG9CHptQPjtKH2AU67
// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
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
    private var headerProps: HeaderProps? = null
    private var bodyProps: BodyProps? = null
    private var toolBarProps: ToolBarProps? = null
    //private var assistantState: AssistantState = AssistantState.Start
    private var bottomSheetBehavior : BottomSheetBehavior<View>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            headerProps = it.getSerializable(HEADER) as HeaderProps?
            bodyProps = it.getSerializable(BODY) as BodyProps?
            toolBarProps = it.getSerializable(TOOLBAR) as ToolBarProps?
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val container = inflater.inflate(R.layout.fragment_assistant_drawer_u_i, container, false)

        //Layouts
        val drawerLayout = container.findViewById<LinearLayout>(R.id.drawerLayout)

        drawerLayout.setBackgroundColor(Color.parseColor(toolBarProps?.backgroundColor));
        
        //Image Views
        val micImageView = container.findViewById<ImageView>(R.id.micImageView)
        val closeAssistantImageView = container.findViewById<ImageView>(R.id.closeAssistantImageView)
        val sendMessageImageView = container.findViewById<ImageView>(R.id.sendMessageButtonImageView)

        Picasso.get().load("https://voicify-prod-files.s3.amazonaws.com/99a803b7-5b37-426c-a02e-63c8215c71eb/daca643f-6730-4af5-8817-8d9d0d9db0b5/mic-image.png").into(micImageView)
        Picasso.get().load("https://voicify-prod-files.s3.amazonaws.com/99a803b7-5b37-426c-a02e-63c8215c71eb/a6de04bb-e572-4a55-8cd9-1a7628285829/delete-2.png").into(closeAssistantImageView)
        Picasso.get().load("https://voicify-prod-files.s3.amazonaws.com/99a803b7-5b37-426c-a02e-63c8215c71eb/7a39bc6f-eef5-4185-bcf8-2a645aff53b2/Send-3-.png").into(sendMessageImageView)

        //Text Views
        val assistantStateTextView = container.findViewById<TextView>(R.id.assistantStateTextView)
        val spokenTextView = container.findViewById<TextView>(R.id.spokenTextView)
        val drawerWelcomeTextView = container.findViewById<TextView>(R.id.drawerWelcomeTextView)

        drawerWelcomeTextView.text = "How can i help?"
        drawerWelcomeTextView.setTextColor(Color.parseColor("#8F97A1"))
        drawerWelcomeTextView.textSize = 18f
        assistantStateTextView.textSize = 16f
        spokenTextView.textSize = 16f
        assistantStateTextView.setTextColor(Color.parseColor("#8F97A1"))

        //Styles
        val spokenTextViewStyle = GradientDrawable()
        spokenTextViewStyle.cornerRadius = 24f
        spokenTextViewStyle.setColor(Color.parseColor("#80000000"))
        spokenTextView.background = spokenTextViewStyle

        //initialization
        val voicifyTTS = VoicifyTTSProvider(VoicifyTextToSpeechSettings(
        appId = "99a803b7-5b37-426c-a02e-63c8215c71eb",
        appKey = "MTAzM2RjNDEtMzkyMC00NWNhLThhOTYtMjljMDc3NWM5NmE3",
        voice = "",
        serverRootUrl = "https://assistant.voicify.com",
        provider = "google"))
        val voicifySTT = VoicifySTTProvider(requireContext() ,requireActivity())
        val assistant = VoicifyAssistant(voicifySTT, voicifyTTS, VoicifyAssistantSettings(
            appId = "99a803b7-5b37-426c-a02e-63c8215c71eb",
            appKey = "MTAzM2RjNDEtMzkyMC00NWNhLThhOTYtMjljMDc3NWM5NmE3",
            serverRootUrl = "https://assistant.voicify.com",
            locale = "en-US",
            channel = "My App",
            device = "My Device",
            autoRunConversation = true,
            initializeWithWelcomeMessage = false,
            useVoiceInput = true,
            useOutputSpeech = true))

        assistant.initializeAndStart()
        assistant.startNewSession()

        //Listeners
        voicifySTT.addPartialListener { partialResult ->
            spokenTextView.text = partialResult
        }
        voicifySTT.addFinalResultListener { fullResult ->
            spokenTextView.text = fullResult
            assistantStateTextView.text = "Processing..."
            assistant.makeTextRequest(fullResult.toString(), null, "Speech")
        }
        micImageView.setOnClickListener{
            voicifySTT.startListening()
        }

        voicifySTT.addSpeechReadyListener {
            assistantStateTextView.text = "Listening..."
        }

//        voicifySTT.addVolumeListener { volume ->
//            if(assistantState != AssistantState.Speaking)
//            {
//                //assistantState = AssistantState.Speaking
//            }
//        }

        assistant.onResponseReceived { response ->
            if(!response.endSession)
            {
                activity?.runOnUiThread{
                    val metrics = activity?.resources?.displayMetrics
                    var params = drawerLayout.layoutParams
                    params.height = metrics?.heightPixels as Int
                    drawerLayout.layoutParams = params
                    bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
                }
            }
        }
        closeAssistantImageView.setOnClickListener{
            dismiss()
        }
        // Inflate the layout for this fragment
        return container
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bottomSheetBehavior = BottomSheetBehavior.from((view.parent as View))
        bottomSheetBehavior?.isDraggable = false

        val touchOutsideView =
            dialog!!.window!!.decorView.findViewById<View>(R.id.touch_outside)
        touchOutsideView.setOnClickListener(null) // dont allow modal to close when touched outside
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
        fun newInstance(headerProps: HeaderProps, bodyProps: BodyProps, toolBarProps: ToolBarProps) =
            AssistantDrawerUI().apply {
                arguments = Bundle().apply {
                    putSerializable(HEADER, headerProps)
                    putSerializable(BODY, bodyProps)
                    putSerializable(TOOLBAR, toolBarProps)
                }
            }
    }
}