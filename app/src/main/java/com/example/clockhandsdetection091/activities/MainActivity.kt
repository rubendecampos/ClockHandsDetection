package com.example.clockhandsdetection091.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.clockhandsdetection091.R
import org.opencv.android.OpenCVLoader


class MainActivity : AppCompatActivity() {

    val TAG = "MainActivity"

    var btnStartCalibration: Button? = null
    var txtCol: EditText? = null
    var txtRow: EditText? = null
    lateinit var nbrCol: String
    lateinit var nbrRow: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var success = OpenCVLoader.initDebug()
        if(success){
            Log.d(TAG, "OpenCV init properly")
        }else{
            Log.e(TAG, "OpenCV init failed")
        }

        btnStartCalibration = findViewById(R.id.btnStartCalibration)
        txtCol = findViewById(R.id.txtCol)
        txtRow = findViewById(R.id.txtRow)

        // On click on the capture Button
        btnStartCalibration?.setOnClickListener {
            //Get the nbr of row and col of the matrice
            nbrCol = txtCol!!.text.toString()
            nbrRow = txtRow!!.text.toString()

            // Start the next Activity
            val nextActivity = Intent(this,
                CalibrationActivity::class.java)
            nextActivity.putExtra("nbrRow",nbrRow)
            nextActivity.putExtra("nbrCol",nbrCol)
            startActivity(nextActivity)
        }
    }
}
