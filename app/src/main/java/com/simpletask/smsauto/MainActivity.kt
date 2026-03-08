package com.simpletask.smsauto

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
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
import org.json.JSONArray

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Request SMS permissions
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.RECEIVE_SMS, android.Manifest.permission.READ_SMS), 1)
        }

        val apiKeyInput = findViewById<EditText>(R.id.api_key_input)
        val addBtn = findViewById<Button>(R.id.add_btn)
        val clearBtn = findViewById<Button>(R.id.clear_btn)
        val botsList = findViewById<TextView>(R.id.bots_list)
        
        val prefs = getSharedPreferences("SMSAutoPrefs", Context.MODE_PRIVATE)

        fun updateBotsList() {
            val botsStr = prefs.getString("bot_api_keys", "[]") ?: "[]"
            try {
                val array = JSONArray(botsStr)
                if (array.length() == 0) {
                    botsList.text = "No bots configured."
                } else {
                    val sb = StringBuilder()
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        sb.append("Bot ${i+1}:\n")
                        sb.append("URL: ${obj.getString("url")}\n\n")
                    }
                    botsList.text = sb.toString()
                }
            } catch (e: Exception) {
                botsList.text = "Error reading bots list."
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
                        
                        Toast.makeText(this, "Bot added successfully!", Toast.LENGTH_SHORT).show()
                        apiKeyInput.setText("")
                        updateBotsList()
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
}
