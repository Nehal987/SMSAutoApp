package com.simpletask.smsauto

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONObject
import android.util.Base64
import android.util.Log
import org.json.JSONArray
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Request SMS permissions
        val perms = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val needed = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), 1)
        }

        // Start Foreground Service for 24/7 background operation
        val serviceIntent = Intent(this, SmsForwardService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        val apiKeyInput = findViewById<EditText>(R.id.api_key_input)
        val addBtn = findViewById<Button>(R.id.add_btn)
        val clearBtn = findViewById<Button>(R.id.clear_btn)
        val botsContainer = findViewById<android.widget.LinearLayout>(R.id.bots_container)
        
        val prefs = getSharedPreferences("SMSAutoPrefs", Context.MODE_PRIVATE)

        fun updateBotsList() {
            val botsStr = prefs.getString("bot_api_keys", "[]") ?: "[]"
            botsContainer.removeAllViews()
            try {
                val array = JSONArray(botsStr)
                if (array.length() == 0) {
                    val tv = TextView(this)
                    tv.text = "No bots configured."
                    botsContainer.addView(tv)
                } else {
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        
                        // Create a row Layout
                        val rowLayout = android.widget.LinearLayout(this)
                        rowLayout.orientation = android.widget.LinearLayout.HORIZONTAL
                        rowLayout.setPadding(0, 0, 0, 16)
                        
                        // Bot info Text
                        val tv = TextView(this)
                        tv.text = "Bot ${i+1}: ✅\nURL: ${obj.getString("url")}"
                        tv.layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                        
                        // Delete Button
                        val delBtn = Button(this)
                        delBtn.text = "Delete"
                        delBtn.setOnClickListener {
                            val newArray = JSONArray()
                            // Rebuild array without this index
                            for (j in 0 until array.length()) {
                                if (j != i) {
                                    newArray.put(array.getJSONObject(j))
                                }
                            }
                            prefs.edit().putString("bot_api_keys", newArray.toString()).apply()
                            Toast.makeText(this, "Bot Deleted", Toast.LENGTH_SHORT).show()
                            updateBotsList()
                        }
                        
                        rowLayout.addView(tv)
                        rowLayout.addView(delBtn)
                        botsContainer.addView(rowLayout)
                    }
                }
            } catch (e: Exception) {
                val tv = TextView(this)
                tv.text = "Error reading bots list."
                botsContainer.addView(tv)
            }
        }

        updateBotsList()

        addBtn.setOnClickListener {
            val key = apiKeyInput.text.toString().trim()
            if (key.isNotEmpty()) {
                try {
                    val decodedBytes = Base64.decode(key, Base64.DEFAULT)
                    val decodedString = String(decodedBytes, Charsets.UTF_8)
                    val newBot = JSONObject(decodedString)
                    
                    if (newBot.has("url") && newBot.has("token")) {
                        val botsStr = prefs.getString("bot_api_keys", "[]") ?: "[]"
                        val array = JSONArray(botsStr)
                        array.put(newBot)
                        prefs.edit().putString("bot_api_keys", array.toString()).apply()
                        
                        apiKeyInput.setText("")
                        updateBotsList()
                        
                        // Auto-ping the bot to verify connection
                        val testUrl = newBot.getString("url").replace("/api/sms", "/api/test")
                        val token = newBot.getString("token")
                        pingBot(testUrl, token)
                        
                        Toast.makeText(this, "Bot added! Testing connection...", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Invalid API Key format", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Invalid API Key: Not Base64 JSON", Toast.LENGTH_LONG).show()
                }
            }
        }

        clearBtn.setOnClickListener {
            prefs.edit().putString("bot_api_keys", "[]").apply()
            Toast.makeText(this, "All bots cleared!", Toast.LENGTH_SHORT).show()
            updateBotsList()
        }
    }

    private fun pingBot(testUrl: String, token: String) {
        val jsonBody = """{"token": "$token"}"""
        val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder().url(testUrl).post(requestBody).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("MainActivity", "Connection test failed: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "❌ Connection failed! Check bot is running.", Toast.LENGTH_LONG).show()
                }
            }
            override fun onResponse(call: Call, response: Response) {
                val code = response.code
                response.close()
                Log.d("MainActivity", "Connection test response Code: $code")
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this@MainActivity, "✅ Connected! Bot has been notified.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@MainActivity, "❌ Connection rejected by server! (Error $code)", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }
}
