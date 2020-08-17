package com.example.clockhandsdetection.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.app.AppCompatActivity
import com.example.clockhandsdetection.R
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONArray
import org.json.JSONObject
import org.opencv.android.OpenCVLoader


/**
 * Main activity of this app. It contain most of the buttons to control the matrice device.
 * @property [nbrCol][nbrRow] number of columns and rows in the matrice, given by the properties
 * chosen by the user.
 * @property [clockIndex][handIndex] indexes to select a specific clock hand in the matrcie.
 * @author Ruben De Campos
 */
class MainActivity : AppCompatActivity(), OnSeekBarChangeListener{

    val TAG = "MainActivity"

    private lateinit var switch: Switch

    private var nbrCol = 14
    private var nbrRow = 6
    private var progressValue = 0
    private lateinit var jsonString: String

    /**
     * On the activity creation.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val success = OpenCVLoader.initDebug()
        if(success){
            Log.d(TAG, "OpenCV init properly")
        }else{
            Log.e(TAG, "OpenCV init failed")
        }

        // Find the view by id of all the clickable/editable element in this activity View
        val btnStartCalibration: Button = findViewById(R.id.btnStartCalibration)
        val btnPlus: Button = findViewById(R.id.btnPlus)
        val btnMinus: Button = findViewById(R.id.btnMinus)
        val btnReset: Button = findViewById(R.id.btnReset)
        val btnSendText: Button = findViewById(R.id.btnSendText)
        val btnAnim1: Button = findViewById(R.id.btnAnim1)
        val btnAnim2: Button = findViewById(R.id.btnAnim2)
        val btnAnim3: Button = findViewById(R.id.btnAnim3)
        val btnAnim4: Button = findViewById(R.id.btnAnim4)
        val btnGotoZero: Button = findViewById(R.id.btnGotoZero)
        val txtRowIndex: EditText = findViewById(R.id.txtRowIndex)
        val txtColumnIndex: EditText = findViewById(R.id.txtColumnIndex)
        switch = findViewById(R.id.handIndex)
        val txtText: EditText = findViewById(R.id.txtText)
        val radioBtn3x2: RadioButton = findViewById(R.id.radioBtn3x2)
        val radioBtn3x8: RadioButton = findViewById(R.id.radioBtn3x8)
        val radioBtn6x14: RadioButton = findViewById(R.id.radioBtn6x14)
        val seekBar: SeekBar = findViewById(R.id.seekBar)

        jsonString = resources.openRawResource(R.raw.json_file6x14)
            .bufferedReader().use { it.readText() }

        // On start calibration button click
        btnStartCalibration.setOnClickListener {
            // Start the next Activity
            val intent = Intent(this,
                CalibrationActivity::class.java)
            intent.putExtra("nbrRow",nbrRow.toString())
            intent.putExtra("nbrCol",nbrCol.toString())
            intent.putExtra("jsonString",jsonString)

            startActivity(intent)
        }

        // On plus button click
        btnPlus.setOnClickListener {
            // First get the clock and hand index entered by the user.
            val rowIndex = txtRowIndex.text.toString().toInt() - 1
            val columnIndex = txtColumnIndex.text.toString().toInt() - 1
            val clockIndex = rowIndex*nbrCol + columnIndex

            // The position of the switch give the watchpointer index
            var handIndex: Int
            if(switch.isChecked)
                handIndex = 1
            else
                handIndex = 0

            // Get from the jsonArray in the jsonString, the jsonObject that contain the processorID
            // and clockID in function of this clockIndex.
            val jsonArray = JSONArray(jsonString)
            val jsonClock = jsonArray[clockIndex] as JSONObject
            // Change the watchpointID
            jsonClock.put("watchpointerID",handIndex)

            // Create the json object and send it
            val jsonObject = JSONObject()
            jsonObject.put("header","PLUS")
            jsonObject.put("body",jsonClock)

            sendToDevice(jsonObject.toString().toByteArray())
        }

        // On minus button click
        btnMinus.setOnClickListener {
            // First get the clock and hand index entered by the user.
            val rowIndex = txtRowIndex.text.toString().toInt() - 1
            val columnIndex = txtColumnIndex.text.toString().toInt() - 1
            val clockIndex = rowIndex*nbrCol + columnIndex

            // The position of the switch give the watchpointer index
            var handIndex: Int
            if(switch.isChecked)
                handIndex = 1
            else
                handIndex = 0

            // Get from the jsonArray in the jsonString, the jsonObject that contain the processorID
            // and clockID in function of this clockIndex.
            val jsonArray = JSONArray(jsonString)
            val jsonClock = jsonArray[clockIndex] as JSONObject
            // Change the watchpointID
            jsonClock.put("watchpointerID",handIndex)

            // Create the json object and send it
            val jsonObject = JSONObject()
            jsonObject.put("header","MINUS")
            jsonObject.put("body",jsonClock)

            sendToDevice(jsonObject.toString().toByteArray())
        }

        // On reset button click
        btnReset.setOnClickListener{
            // First get the clock and hand index entered by the user.
            val rowIndex = txtRowIndex.text.toString().toInt() - 1
            val columnIndex = txtColumnIndex.text.toString().toInt() - 1
            val clockIndex = rowIndex*nbrCol + columnIndex

            // The position of the switch give the watchpointer index
            var handIndex: Int
            if(switch.isChecked)
                handIndex = 1
            else
                handIndex = 0

            // Get from the jsonArray in the jsonString, the jsonObject that contain the processorID
            // and clockID in function of this clockIndex.
            val jsonArray = JSONArray(jsonString)
            val jsonClock = jsonArray[clockIndex] as JSONObject
            // Change the watchpointID
            jsonClock.put("watchpointerID",handIndex)

            // Create the json object and send it
            val jsonObject = JSONObject()
            jsonObject.put("header","RESETZERO")
            jsonObject.put("body",jsonClock)

            sendToDevice(jsonObject.toString().toByteArray())
        }

        // Set the seek bar listener
        seekBar.setOnSeekBarChangeListener(this)

        // On send text button click
        btnSendText.setOnClickListener {
            // First get the text entered by the user.
            val text = txtText.text.toString()

            // Create the json object and send it
            val jsonObject: JSONObject = JSONObject()
            jsonObject.put("header","TEXT")
            jsonObject.put("body",text)

            sendToDevice(jsonObject.toString().toByteArray())
        }

        // On anim1 button click
        btnAnim1.setOnClickListener {
            // Create the json object and send it
            val jsonObject: JSONObject = JSONObject()
            jsonObject.put("header","ANIMATION")
            jsonObject.put("body",1)

            sendToDevice(jsonObject.toString().toByteArray())
        }

        // On anim2 button click
        btnAnim2.setOnClickListener {
            // Create the json object and send it
            val jsonObject: JSONObject = JSONObject()
            jsonObject.put("header","ANIMATION")
            jsonObject.put("body",2)

            sendToDevice(jsonObject.toString().toByteArray())
        }

        // On ani3 button click
        btnAnim3.setOnClickListener {
            // Create the json object and send it
            val jsonObject: JSONObject = JSONObject()
            jsonObject.put("header","ANIMATION")
            jsonObject.put("body",3)

            sendToDevice(jsonObject.toString().toByteArray())
        }

        // On anim4 button click
        btnAnim4.setOnClickListener {
            // Create the json object and send it
            val jsonObject: JSONObject = JSONObject()
            jsonObject.put("header","ANIMATION")
            jsonObject.put("body",4)

            sendToDevice(jsonObject.toString().toByteArray())
        }

        // On go to zero button click
        btnGotoZero.setOnClickListener{
            // Create the json object and send it
            val jsonObject: JSONObject = JSONObject()
            jsonObject.put("header","GOTOZERO")

            sendToDevice(jsonObject.toString().toByteArray())
        }

        // On a radio button click
        radioBtn3x2.setOnClickListener{
            jsonString = resources.openRawResource(R.raw.json_file2x3)
                .bufferedReader().use { it.readText() }
            nbrRow = 3
            nbrCol = 2
        }
        radioBtn3x8.setOnClickListener{
            jsonString = resources.openRawResource(R.raw.json_file3x8)
                .bufferedReader().use { it.readText() }
            nbrRow = 3
            nbrCol = 8
        }
        radioBtn6x14.setOnClickListener{
            jsonString = resources.openRawResource(R.raw.json_file6x14)
                .bufferedReader().use { it.readText() }
            nbrRow = 6
            nbrCol = 14
        }
    }

    /**
     * Send data to the device using the bluetooth serialSocket.
     * @param [byteArray] data to send.
     */
    fun sendToDevice(byteArray: ByteArray){
        if(SerialSocket.isConnected){
            SerialSocket.outputStream!!.write(byteArray)
        }else{
            Toast.makeText(this,"Device not connected",Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * On progress changed in a seekBar.
     * @param [seekBar]
     * @param [progress]
     * @param [fromUser]
     */
    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        val stepNumber = progress - progressValue

        // Get the clock and hand index entered by the user.
        val rowIndex = txtRowIndex.text.toString().toInt() - 1
        val columnIndex = txtColumnIndex.text.toString().toInt() - 1
        val clockIndex = rowIndex*nbrCol + columnIndex

        // The position of the switch give the watchpointer index
        var handIndex: Int
        if(switch.isChecked)
            handIndex = 1
        else
            handIndex = 0

        // Get from the jsonArray in the jsonString, the jsonObject that contain the processorID
        // and clockID in function of this clockIndex.
        val jsonArray = JSONArray(jsonString)
        val jsonClock = jsonArray[clockIndex] as JSONObject
        // Change the watchpointID
        jsonClock.put("watchpointerID",handIndex)

        if(stepNumber > 0){
            // Create the json object and send it
            val jsonObject = JSONObject()
            jsonObject.put("header","PLUS")
            jsonObject.put("body",jsonClock)

            sendToDevice(jsonObject.toString().toByteArray())
        }else{
            // Create the json object and send it
            val jsonObject = JSONObject()
            jsonObject.put("header","MINUS")
            jsonObject.put("body",jsonClock)

            sendToDevice(jsonObject.toString().toByteArray())
        }

        // Update the progressValue
        progressValue = progress
    }

    /**
     * On start tracking touch.
     * @param [seekBar]
     */
    override fun onStartTrackingTouch(seekBar: SeekBar?) {
        // Get the progress value
        progressValue = seekBar!!.progress
    }

    /**
     * On stop tracking touch
     * @param [seekBar]
     */
    override fun onStopTrackingTouch(seekBar: SeekBar?) {
        // Reset the seek bar position
        seekBar!!.progress = 30
    }
}
