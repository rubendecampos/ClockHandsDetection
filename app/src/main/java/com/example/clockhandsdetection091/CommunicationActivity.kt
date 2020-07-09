package com.example.clockhandsdetection091

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView

class CommunicationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_communication)

        val intent = intent
        var values = intent.getStringExtra("calibrate")
        var txtView: TextView = findViewById(R.id.angles)
        txtView.setText(values)
    }
}
