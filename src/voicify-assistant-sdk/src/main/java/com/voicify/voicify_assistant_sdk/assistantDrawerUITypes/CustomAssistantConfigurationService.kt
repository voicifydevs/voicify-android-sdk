package com.voicify.voicify_assistant_sdk.assistantDrawerUITypes

import android.util.Log
import com.google.gson.Gson
import com.voicify.voicify_assistant_sdk.models.CustomAssistantConfigurationResponse
import okhttp3.*
import java.lang.Exception


class CustomAssistantConfigurationService {
    private val client: OkHttpClient = OkHttpClient()

    fun getCustomAssistantConfiguration(configurationId: String, serverRootUrl: String, appId: String, appKey: String): CustomAssistantConfigurationResponse?{
        try{
            if(configurationId.isNullOrEmpty())
            {
                return null
            }
            val userDataRequest = Request.Builder()
                .url("${serverRootUrl}/api/CustomAssistantConfiguration/${configurationId}/Kotlin?applicationId=${appId}&applicationSecret=${appKey}")
                .addHeader("Content-Type","application/json")
                .get()
                .build()
            val response = client.newCall(userDataRequest).execute()
            if(response.code == 200)
            {
                val gson = Gson()
                val configurationResult = response.body?.string()
                Log.d("JAMES", configurationResult.toString())
                return gson.fromJson(configurationResult, CustomAssistantConfigurationResponse::class.java)
            }
            return null
        }
        catch(e: Exception){
            return null
        }
    }
}