package com.voicify.voicify_assistant_sdk.assistantDrawerUITypes

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.localbroadcastmanager.content.LocalBroadcastManager


object NotificationCenter {
    fun addObserver(
        context: Context,
        notification: NotificationType,
        responseHandler: BroadcastReceiver?
    ) {
        LocalBroadcastManager.getInstance(context)
            .registerReceiver(responseHandler!!, IntentFilter(notification.name))
    }

    fun removeObserver(context: Context, responseHandler: BroadcastReceiver?) {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(responseHandler!!)
    }

    fun postNotification(
        context: Context,
        notification: NotificationType,
        params: Bundle? = null
    ) {
        val intent = Intent(notification.name)
        // insert parameters if needed
        if(params?.isEmpty == false){
            intent.putExtra("data", params)
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }
}