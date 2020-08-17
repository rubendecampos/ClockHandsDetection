package com.example.clockhandsdetection.activities

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.example.clockhandsdetection.R
import org.json.JSONArray
import org.json.JSONObject

/**
 * Result Activity. Display the results of the computation and the calibration. So in one TextView
 * the angles of each clock's hand with the state of the calibration. And in another TextView the
 * actions for the device to do (ex: move hand1 by 45° and hand2 by 180°).
 * Then on the button click, send the actions to the device.
 * @author Ruben De Campos
 */
class ResultActivity : AppCompatActivity() {

    private lateinit var jsonString: String

    /**
     * On the activity creation.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        val intent = intent
        val clockArrayString = intent.getStringExtra("clock array")
        jsonString = intent.getStringExtra("action")

        val txtAngles: TextView = findViewById(R.id.angles)
        val txtAction: TextView = findViewById(R.id.actions)
        val btnSendCalibration: Button = findViewById(R.id.btnSendCalibration)

        // From the json received, create a "readable" string
        val jsonObject = JSONObject(jsonString)
        val jsonArray = jsonObject["body"] as JSONArray
        var actionString = "Actions:\n\r"
        for(i in 0 until jsonArray.length()){
            val clock = jsonArray[i] as JSONObject
            val hand1 = clock["moveWP1"]
            val hand2 = clock["moveWP2"]
            actionString += "Move clock $i, hand1 by $hand1 and hand2 by $hand2\n\r"
        }

        txtAngles.text = "Angles: \n" + clockArrayString
        txtAction.text =  actionString

        // On send button clicked, end the activity. It'll send the jsonObject to the connected
        // device.
        btnSendCalibration.setOnClickListener{
            finish()
        }
    }

    /**
     * On the activity destruction.
     */
    override fun onDestroy() {
        super.onDestroy()

        // Send the jsonObject that contain the hands movements to the device.
        if(SerialSocket.isConnected){
            SerialSocket.outputStream!!.write(jsonString.toByteArray())
        }else{
            Toast.makeText(this,"Device not connected", Toast.LENGTH_SHORT).show()
        }
    }
}
