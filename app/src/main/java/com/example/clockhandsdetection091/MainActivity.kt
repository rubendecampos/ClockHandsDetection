package com.example.clockhandsdetection091

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.android.synthetic.main.activity_main.*
import org.opencv.android.OpenCVLoader
import java.io.File
import java.io.IOException


class MainActivity : AppCompatActivity() {

    val TAG = "MainActivity"
    val REQUEST_IMAGE_CAPTURE = 1

    var btnCapture: Button? = null
    var txtCol: EditText? = null
    var txtRow: EditText? = null
    lateinit var currentPicturePath: String
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

        btnCapture = findViewById(R.id.btnCapture)
        txtCol = findViewById(R.id.txtCol)
        txtRow = findViewById(R.id.txtRow)

        // On click on the capture Button
        btnCapture?.setOnClickListener {
            // Request the permission to use the camera if not already granted
            if(ContextCompat.checkSelfPermission(this,android.Manifest.permission.CAMERA) !=
                PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this, Array<String>(1, init =
                {android.Manifest.permission.CAMERA}), 100)
            }

            // create and start the intent to take a picture
            var intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val picture_file = try{
                getOutputMediaFile()
            }catch(e: IOException){
                // Error occured while creating the File
                null
            }
            // Continue only if the File was created successfully
            picture_file?.also {
                val pictureUri = FileProvider.getUriForFile(this,
                    "com.example.android.fileprovider",it)
                intent.putExtra(MediaStore.EXTRA_OUTPUT,pictureUri)
                startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
            }

            //Get the nbr of row and col of the matrice
            nbrCol = txtCol!!.text.toString()
            nbrRow = txtRow!!.text.toString()
        }
    }

    //------------------------------------------------------------------------------------
    // On the activity result, switch to the next activity.
    //------------------------------------------------------------------------------------
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_IMAGE_CAPTURE -> if (resultCode == Activity.RESULT_OK) {
                // Start the next Activity
                var nextActivity = Intent(this,HandsDetectionActivity::class.java)
                nextActivity.putExtra("filePath",currentPicturePath)
                nextActivity.putExtra("nbrRow",nbrRow)
                nextActivity.putExtra("nbrCol",nbrCol)
                startActivity(nextActivity)
            }
        }
    }

    //------------------------------------------------------------------------------------
    // Get the output file
    //------------------------------------------------------------------------------------
    private fun getOutputMediaFile(): File? {
        var state = Environment.getExternalStorageState()
        if(state.equals(Environment.MEDIA_MOUNTED)) {
            var storageDir = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString())
            // Create the File
            var outputFile = File(storageDir,
                "temp.jpg").apply()
            {
                // Save the path for use with ACTION_VIEW intents
                currentPicturePath = absolutePath
            }
            return outputFile
        }else{
            return null
        }
    }
}
