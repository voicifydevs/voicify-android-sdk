package com.voicify.voicify_assistant_sdk.assistantDrawerUITypes

import android.util.Log
import com.google.gson.Gson
import com.voicify.voicify_assistant_sdk.models.CustomAssistantConfigurationResponse
import com.voicify.voicify_assistant_sdk.models.CustomAssistantResponse
import com.voicify.voicify_assistant_sdk.models.VoicifyUserData
import okhttp3.*
import java.io.IOException


class CustomAssistantConfigurationService {
    private val client: OkHttpClient = OkHttpClient()

    fun getCustomAssistantConfiguration(configurationId: String, serverRootUrl: String, appId: String, appKey: String): CustomAssistantConfigurationResponse?{
        if(configurationId.isNullOrEmpty())
        {
            return null
        }
        val userDataRequest = Request.Builder()
            .url("${serverRootUrl}/api/CustomAssistantConfiguration/${configurationId}?applicationId=${appId}&applicationSecret=${appKey}")
            .addHeader("Content-Type","application/json")
            .get()
            .build()
        val response = client.newCall(userDataRequest).execute()
        if(response.code == 200)
        {
            Log.d("JAMES", "Success")
            val gson = Gson()
            val configurationResult = response.body?.string()
            return gson.fromJson(configurationResult, CustomAssistantConfigurationResponse::class.java)
        }
        return null
    }
}