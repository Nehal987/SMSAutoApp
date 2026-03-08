package com.simpletask.smsauto

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val SMS_PERMISSION_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkPermission()

        val saveButton = findViewById<Button>(R.id.btnSave)
        val etUrl = findViewById<EditText>(R.id.etBotUrl)

        val prefs = getSharedPreferences("sms_prefs", MODE_PRIVATE)
        etUrl.setText(prefs.getString("bot_url", "http://192.168.1.100:5000/api/sms"))

        saveButton.setOnClickListener {
            val url = etUrl.text.toString()
            prefs.edit().putString("bot_url", url).apply()
            Toast.makeText(this, "Settings Saved!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS), SMS_PERMISSION_CODE)
        }
    }
}
