package com.simpletask.smsauto

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class SmsReceiver : BroadcastReceiver() {

    private val client = OkHttpClient()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (sms in messages) {
                val sender = sms.displayOriginatingAddress
                val body = sms.displayMessageBody

                // Only process bKash or Nagad SMS
                if (sender != null && (sender.contains("bKash", ignoreCase = true) || sender.contains("Nagad", ignoreCase = true))) {
                    Log.d("SmsReceiver", "Received Payment SMS from $sender")
                    sendToBot(context, sender, body)
                }
            }
        }
    }

    private fun sendToBot(context: Context, sender: String, body: String) {
        val prefs = context.getSharedPreferences("sms_prefs", Context.MODE_PRIVATE)
        val url = prefs.getString("bot_url", "http://192.168.1.100:5000/api/sms") ?: return

        val json = JSONObject()
        json.put("secret", "sms_auto_approve_123")
        json.put("sender", sender)
        json.put("body", body)

        val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("SmsReceiver", "Failed to send SMS to Bot: ${e.message}")
            }
            override fun onResponse(call: Call, response: Response) {
                Log.d("SmsReceiver", "Bot Response: ${response.body?.string()}")
            }
        })
    }
}
