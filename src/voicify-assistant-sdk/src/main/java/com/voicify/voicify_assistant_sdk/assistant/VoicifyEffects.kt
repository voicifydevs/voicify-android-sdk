package com.voicify.voicify_assistant_sdk.assistant

import android.util.Log

val closeAssistantCallback: (data: Any) -> Unit = {
    Log.d("JAMES", "closing")
}

val scrollToCallback: (data: Any) -> Unit = {
    Log.d("JAMES", "scrolling")
}

val clickTapCallback: (data: Any) -> Unit = {
    Log.d("JAMES", "clickTapping")
}