package com.voicify.voicify_assistant_sdk.assistantDrawerUITypes

import android.util.Log
import com.google.gson.Gson
import com.voicify.voicify_assistant_sdk.models.CustomAssistantConfigurationResponse
import com.voicify.voicify_assistant_sdk.models.CustomAssistantResponse
import com.voicify.voicify_assistant_sdk.models.VoicifyUserData
import okhttp3.*
import java.io.IOException
import java.lang.Exception


class CustomAssistantConfigurationService {
    private val client: OkHttpClient = OkHttpClient()

    fun getCustomAssistantConfiguration(configurationId: String, serverRootUrl: String, appId: String, appKey: String): CustomAssistantConfigurationResponse?{
        try{
            Log.d("JAMES", "HELOO?????")
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
                Log.d("JAMES", "Success")
                val gson = Gson()
                val configurationResult = response.body?.string()
                return gson.fromJson(configurationResult, CustomAssistantConfigurationResponse::class.java)
            }
            else{
                Log.d("JAMES",response.code.toString())
                Log.d("JAMES", response.body?.string().toString())
            }
            Log.d("JAMES","SOMETHING IS FAIL")
            return null
        }
        catch(e: Exception){
            return null
        }
    }
}