package com.example.clockhandsdetection091.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.clockhandsdetection091.R
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
class MainActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    val TAG = "MainActivity"

    private var nbrCol = 0
    private var nbrRow = 0
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
        val txtHandIndex: EditText = findViewById(R.id.txtHandIndex)
        val txtText: EditText = findViewById(R.id.txtText)
        val spnProperty: Spinner = findViewById(R.id.spnProperty)

        // create the spinner
        val spinnerList: MutableList<String> = arrayListOf()
        spinnerList.add("matrice : 3x2")
        spinnerList.add("matrice : 3x8")
        spinnerList.add("matrice : 6x14")

        // Set the Spinner and is ArrayAdapter
        spnProperty.onItemSelectedListener = this
        val arrayAdapter = ArrayAdapter(this,android.R.layout.simple_spinner_item,spinnerList)
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spnProperty.adapter = arrayAdapter

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
            val handIndex = txtHandIndex.text.toString().toInt() - 1

            // Get from the jsonArray in the jsonString, the jsonObject that contain the processorID
            // and clockID in function of this clockIndex.
            val jsonArray = JSONArray(jsonString)
            val jsonClock = jsonArray[clockIndex] as JSONObject
            // Change the watchpointID
            jsonClock.put("watchpointerID",handIndex)

            // Create the json object and send it
            val jsonObject: JSONObject = JSONObject()
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
            val handIndex = txtHandIndex.text.toString().toInt() - 1

            // Get from the jsonArray in the jsonString, the jsonObject that contain the processorID
            // and clockID in function of this clockIndex.
            val jsonArray = JSONArray(jsonString)
            val jsonClock = jsonArray[clockIndex] as JSONObject
            // Change the watchpointID
            jsonClock.put("watchpointerID",handIndex)

            // Create the json object and send it
            val jsonObject: JSONObject = JSONObject()
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
            val handIndex = txtHandIndex.text.toString().toInt() - 1

            // Get from the jsonArray in the jsonString, the jsonObject that contain the processorID
            // and clockID in function of this clockIndex.
            val jsonArray = JSONArray(jsonString)
            val jsonClock = jsonArray[clockIndex] as JSONObject
            // Change the watchpointID
            jsonClock.put("watchpointerID",handIndex)

            // Create the json object and send it
            val jsonObject: JSONObject = JSONObject()
            jsonObject.put("header","RESETZERO")
            jsonObject.put("body",jsonClock)

            sendToDevice(jsonObject.toString().toByteArray())
        }

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
     * If nothing has been selected in the spinner.
     */
    override fun onNothingSelected(parent: AdapterView<*>?) {
        Toast.makeText(this,"Please select a matrice property",Toast.LENGTH_SHORT).show()
    }

    /**
     * On item selected in the spinner.
     */
    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        // Update the nbrRow, nbrCol and jsonArray var.
        when(position){
            0 -> {
                jsonString = resources.openRawResource(R.raw.json_file2x3)
                    .bufferedReader().use { it.readText() }
                nbrRow = 3
                nbrCol = 2
            }
            1 -> {
                jsonString = resources.openRawResource(R.raw.json_file3x8)
                    .bufferedReader().use { it.readText() }
                nbrRow = 3
                nbrCol = 8
            }
            2 -> {
                jsonString = resources.openRawResource(R.raw.json_file6x14)
                    .bufferedReader().use { it.readText() }
                nbrRow = 6
                nbrCol = 14
            }
        }
    }
}
