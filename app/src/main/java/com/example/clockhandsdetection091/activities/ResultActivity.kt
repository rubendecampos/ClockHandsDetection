package com.example.clockhandsdetection091.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.example.clockhandsdetection091.R

class ResultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        val intent = intent
        val values = intent.getStringExtra("calibrate")
        val actions = intent.getStringExtra("action")
        val txtAngles: TextView = findViewById(R.id.angles)
        val txtAction: TextView = findViewById(R.id.actions)
        txtAngles.text = values
        txtAction.text = actions
    }
}
