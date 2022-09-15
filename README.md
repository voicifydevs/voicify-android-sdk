# Introduction
This project includes the models, API methods and classes for interacting with the Voicify Custom Assistant API in Kotlin, as well as a customizable Voicify Assistant component for native android.

# Getting Started

```
maven {
    url 'https://jitpack.io'
}


dependencies {
   implementation 'com.github.voicifydevs:voicify-android-sdk:1.0.7'
}

```
You may need to add some permssions to your android manifest:
```xml
<uses-permission "android:name=android.permission.RECORD_AUDIO" />
<uses-permission "android:name=android.permission.INTERNET" />
<queries>
   <intent>
     <action android:name="android.speech.RecognitionService" />
   </intent>
</queries>
```
This SDK offers several out-of-the-box solutions, including:
- A customizable Voicify Custom Assistant component
- A speech to text (STT) class 
- A text to speech (TTS) class

This SDK also offers a variety of tools for creating your own Voice Assistant, including:
- A Voicify Assistant class for making requests to your Voicify Custom Assistant
- A speech to text class (STT) for providing your own STT
- A text to speech class (TTS) for providing your own TTS 

Whether you are looking for a quick and easy way to add a Voicify Assistant to your project, or you are building your own assistant from scratch, this SDK has all the details you need as long as you have a Voicify app to make requests against and the `severRootUrl`, `applicationId` and `applicationSecret` required to make requests. Voicify users can find these resources in the deployments page of the application that they are using with the SDK.
​
## Using the Assistant Drawer UI
The Assistant Drawer UI component is prebuilt to allow for an easy solution for implementing a Voicify Assistant in your project. The component requires some settings that get passed in as props and then its ready to go. All of the props that are not part of the styling, (the header, body and toolbar objects), are required. Contrastly, none of the styling props are required.

For example, the component can be initialized with the required settings and a few styling options.
```kt
        val voiceAssistant = AssistantDrawerUI.newInstance(
        HeaderProps(
                backgroundColor = "#ffffff",
                assistantName = "Voicify",
                assistantNameFontSize = 18),
        BodyProps(backgroundColor = "#ffffff"),
        ToolBarProps(backgroundColor = "#ffffff"),
        AssistantSettingsProps(
                appId = "your-app-id",
                appKey = "your-app-secret",
                serverRootUrl = "https://assistant.voicify.com",
                locale = "en-US",
                textToSpeechProvider = "Google",
                channel = "My App",
                device = "My Device",
                autoRunConversation = true,
                initializeWithWelcomeMessage = false,
                initializeWithText = false,
                useVoiceInput = true,
                useOutputSpeech = true,
                initializeWithText = false,
                effects = arrayOf("Dismiss","Play"),
        )
)
```

## Create Your Own Assistant
While the Assistant Drawer UI offers a quick and easy way to integrate a Voicify Assistant, some cases may require more customization. For those cases, this SDK also provides a Voicify Assistant class that can be initialized with your `serverRootUrl`, `applicationId`, and `applicationSecret` from Voicify. Once it's configured, it becomes easy to make requests to your Voicify Custom Assistant. Additionally, the Assistant class can be configured with a TTS provider and a STT provider. In the case that you would like to use your own, you can utilize the `VoicifyTextToSpeechProvider` and `VoicifySpeechToTextProvider` classes. If you would like to utilize the providers that come with the SDK, then you can pass in the `VoicifyTTSProvider` and `VoicifySTTProvider` into the assistant

For example, the assistant class can be initialized with the required settings and provided TTS and STT classes:
```kt
voicifyTTS = VoicifyTTSProvider(VoicifyTextToSpeechSettings(
appId = "your-app-id",
appKey = "your-app-secret",
voice = "",
serverRootUrl = "https://assistant.voicify.com",
provider = "google"))

voicifySTT = VoicifySTTProvider(requireContext(), requireActivity())

val assistant = VoicifyAssistant(voicifySTT, voicifyTTS, VoicifyAssistantSettings(
    appId = "your-app-id",
    appKey = "your-app-secret",
    serverRootUrl = "https://assistant.voicify.com",
    locale = "en-US",
    channel = "your-channel",
    device = "your-device",
    autoRunConversation = true,
    initializeWithWelcomeMessage = false,
    initializeWithText = false,
    useVoiceInput = true,
    useOutputSpeech = true))

assistant.initializeAndStart()
assistant.startNewSession()
```
## Additional Notes
There are a few things to note when using the SDK.

### Markdown
The Assistant Drawer UI component supports the ability to use markdown when displaying messages received from Voicify. In order to utilize this feature, markdown must be provided in the display text of the conversation item that is being hit. Additionally, if the markdown contains a link, it will be launched in a custom tab. For more details about the package, please visit https://github.com/noties/Markwon
