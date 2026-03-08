package com.simpletask.smsauto

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage
import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import org.json.JSONArray
import org.json.JSONObject

class SmsReceiver : BroadcastReceiver() {
    private val client = OkHttpClient()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.provider.Telephony.SMS_RECEIVED") {
            val bundle = intent.extras
            if (bundle != null) {
                val pdus = bundle.get("pdus") as Array<*>?
                if (pdus != null) {
                    for (pdu in pdus) {
                        val sms = SmsMessage.createFromPdu(pdu as ByteArray)
                        val sender = sms.originatingAddress
                        val body = sms.messageBody

                        if (sender != null && (sender.contains("bKash", true) || sender.contains("Nagad", true))) {
                            Log.d("SmsReceiver", "Payment SMS received from $sender")
                            forwardSmsToBots(context, sender, body)
                        }
                    }
                }
            }
        }
    }

    private fun forwardSmsToBots(context: Context, sender: String, body: String) {
        val prefs = context.getSharedPreferences("SMSAutoPrefs", Context.MODE_PRIVATE)
        val botsStr = prefs.getString("bot_api_keys", "[]") ?: "[]"
        
        try {
            val array = JSONArray(botsStr)
            for (i in 0 until array.length()) {
                val bot = array.getJSONObject(i)
                val url = bot.getString("url")
                val token = bot.getString("token")
                
                val jsonBody = """
                    {
                        "sender": "$sender",
                        "body": "${body.replace("\"", "\\\"").replace("\n", "\\n")}",
                        "token": "$token"
                    }
                """.trimIndent()
                
                val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                
                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()
                
                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.e("SmsReceiver", "Failed to forward SMS to $url", e)
                    }
                    override fun onResponse(call: Call, response: Response) {
                        Log.d("SmsReceiver", "Successfully forwarded SMS to $url, Code: ${response.code}")
                        response.close()
                    }
                })
            }
        } catch (e: Exception) {
            Log.e("SmsReceiver", "Error parsing bot API keys", e)
        }
    }
}
