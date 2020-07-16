package com.example.clockhandsdetection091.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.JsonReader
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.example.clockhandsdetection091.R
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONStringer

class ResultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        val intent = intent
        val clockArrayString = intent.getStringExtra("clock array")
        val actions = intent.getStringExtra("action")

        val txtAngles: TextView = findViewById(R.id.angles)
        val txtAction: TextView = findViewById(R.id.actions)
        val btnSend: Button = findViewById(R.id.btnSend)

        // From the json received, create a "readable" string
        var jsonArray: JSONArray = JSONObject(actions).get("clocks") as JSONArray
        var actionString = ""
        for(i in 0 until jsonArray.length()){
            val clock = jsonArray[i] as JSONObject
            actionString += "Clock "+i+" move: hand1 by "+clock.get("hand1")+
                    " and hand2 by "+clock.get("hand2")+"\n\r"
        }

        txtAngles.text = clockArrayString
        txtAction.text = actionString

        // On send button clicked, send "actions" to the device
        btnSend.setOnClickListener{
            if(SerialSocket.isConnected){
                SerialSocket.outputStream!!.write(actionString.toByteArray())
            }else{
                Toast.makeText(this,"Device not connected", Toast.LENGTH_SHORT).show()
            }

            // End this activity
            finish()
        }

    }
}
