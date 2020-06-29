package com.example.clockhandsdetection091

import android.content.Intent
import android.graphics.BitmapFactory
import android.hardware.Camera
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import org.opencv.android.OpenCVLoader
import org.w3c.dom.Text
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : AppCompatActivity() {

    val TAG = "MainActivity"

    var camera: Camera? = null
    var frameLayout: FrameLayout? = null
    var btnCapture: Button? = null
    var btnCompute: Button? = null
    var btnCaptureAgain: Button? = null
    var ivPicture: ImageView? = null
    var showCamera: ShowCamera? = null
    var picture_file: File? = null
    var text: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var success = OpenCVLoader.initDebug()
        if(success){
            Log.d(TAG, "OpenCV init properly")
        }else{
            Log.e(TAG, "OpenCV init failed")
        }
        Toast.makeText(this, "Take a picture of the matrice", Toast.LENGTH_LONG).show()

        btnCapture = findViewById(R.id.btnCapture)
        frameLayout = findViewById(R.id.frameLayout)
        text = findViewById(R.id.text)

        //Open the camera and show it on the frameLayout
        camera = Camera.open()
        showCamera = ShowCamera(this,camera!!)
        frameLayout!!.addView(showCamera)

        btnCapture?.setOnClickListener({
            captureImage(showCamera!!)
        })

    }

    //------------------------------------------------------------------------------------
    // Capture an image
    //------------------------------------------------------------------------------------
    private fun captureImage(view: View){
        if(camera != null){
            camera!!.takePicture(null,null,mPictureCallback)
        }
    }

    //------------------------------------------------------------------------------------
    // Callback method of the taken picture ( captureImage() )
    // Save the picture in an external directory
    //------------------------------------------------------------------------------------
    var mPictureCallback: Camera.PictureCallback = Camera.PictureCallback(
        function = {data: ByteArray, camera:Camera ->
            picture_file = getOutputMediaFile()

            if(picture_file != null){
                try {
                    var fos = FileOutputStream(picture_file)
                    fos.write(data)
                    Toast.makeText(this, "Saved in: "+picture_file.toString(),
                        Toast.LENGTH_LONG).show()
                    fos.close()
                    //camera.startPreview()
                }catch (e: IOException){
                    e.printStackTrace()
                }

                var intent = Intent(this,HandsDetectionActivity::class.java)
                intent.putExtra("filePath",picture_file!!.absolutePath)
                startActivity(intent)
            }
        })

    //------------------------------------------------------------------------------------
    // Get the output file
    //------------------------------------------------------------------------------------
    private fun getOutputMediaFile(): File? {
        var state = Environment.getExternalStorageState()
        if(state.equals(Environment.MEDIA_MOUNTED)) {
            var folder_gui = File(
                getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString()
                        + File.separator + "GUI"
            )
            //If the file doesn't exist, create it
            if (!folder_gui.exists()) {
                folder_gui.mkdirs()
            }

            var outputFile = File(folder_gui, "temp.jpg")
            return outputFile
        }else{
            return null
        }
    }
}
