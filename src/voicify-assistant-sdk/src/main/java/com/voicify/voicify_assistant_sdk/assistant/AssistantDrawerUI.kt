package com.voicify.voicify_assistant_sdk.assistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.squareup.picasso.Picasso
import com.voicify.voicify_assistant_sdk.assistantDrawerUITypes.ToolBar
import com.voicify.voicify_assistant_sdk.R
import java.util.*
//https://medium.com/voice-tech-podcast/android-speech-to-text-tutorial-8f6fa71606ac -> speech recognition tutorial
//https://material.io/develop/android/components/bottom-sheet-dialog-fragment#modal-bottom-sheet -> scroll to bottom sheet modal
//https://stackoverflow.com/questions/9245408/best-practice-for-instantiating-a-new-android-fragment
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
//    private var header: String? = null
//    private var body: String? = null
//    private var toolBar: ToolBar? = null
    private var props: Map<String, Any> = emptyMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        activity?.setContentView(R.layout.fragment_assistant_drawer_u_i)
//        var text = activity?.findViewById<TextView>(R.id.hello_text_field)
//        Log.d("JAMES", text.toString())
//        text?.text = "We changed the text!"
//        arguments?.let {
//            header = it.get(HEADER) as String;
//            body = it.get(BODY) as String
//            toolBar = it.get(TOOLBAR) as ToolBar;
//        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val toolBarProps = props[TOOLBAR] as ToolBar;
        val container = inflater.inflate(R.layout.fragment_assistant_drawer_u_i, container, false)
        val drawerLayout = container.findViewById<RelativeLayout>(R.id.drawer)
        val micImageView = container.findViewById<ImageView>(R.id.micImageView)
        val spokenTextView = container.findViewById<TextView>(R.id.spokenTextView)
        val voicifyTTS = VoicifyTTSProvider(VoicifyTextToSpeechSettings(appId = "99a803b7-5b37-426c-a02e-63c8215c71eb",
        appKey = "MTAzM2RjNDEtMzkyMC00NWNhLThhOTYtMjljMDc3NWM5NmE3",
        voice = "",
        serverRootUrl = "https://assistant.voicify.com",
        provider = "Google"))
        voicifyTTS.initialize("en-US")
        val voicifySTT = VoicifySTTProvider(requireContext() ,requireActivity())
        voicifySTT.initialize("en-US");
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
        assistant.initializeAndStart();
        assistant.startNewSession(null, null, null, null)
        spokenTextView.setBackgroundColor(Color.parseColor("#80000000"))
        voicifySTT.addPartialListener { partialResult ->
            spokenTextView.text = partialResult
        }
        voicifySTT.addFinalResultListener { fullResult ->
            //voicifyTTS.speakSsml(fullResult.toString())
            assistant.makeTextRequest(fullResult.toString(), null, "Speech")
        }
        micImageView.setOnClickListener{
            voicifySTT.startListening()
        }
        drawerLayout.setBackgroundColor(Color.parseColor(toolBarProps.backgroundColor));
        Picasso.get().load("https://voicify-prod-files.s3.amazonaws.com/99a803b7-5b37-426c-a02e-63c8215c71eb/daca643f-6730-4af5-8817-8d9d0d9db0b5/mic-image.png").into(container.findViewById<ImageButton>(R.id.micImageView))
        // Inflate the layout for this fragment
        return container
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment AssistantDrawerUI.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(header: String, body: String, toolBar: ToolBar) =
            AssistantDrawerUI().apply {
                Log.d("JAMES", toolBar.backgroundColor.toString())
//                arguments = emptyMap();
                props = props.plus(Pair(HEADER, header))
                props = props.plus(Pair(BODY, body))
                props = props.plus(Pair(TOOLBAR, toolBar))
//                arguments = Bundle().apply {
//                    putString(HEADER, header)
//                    putString(BODY, body)
//                }
            }
    }
}