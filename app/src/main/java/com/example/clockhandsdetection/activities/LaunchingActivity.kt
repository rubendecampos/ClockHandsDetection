package com.example.clockhandsdetection.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.clockhandsdetection.R

/**
 * First activity to show when the app is started.
 * @author Ruben De Campos
 */
class LaunchingActivity : AppCompatActivity() {

    /**
     * On the activity creation.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launching)

        val launchingScreen: LinearLayout = findViewById(R.id.launchingScreen)
        val ivBluetooth: ImageView = findViewById(R.id.ivBluetooth)
        val ivHpnosia: ImageView = findViewById(R.id.ivHypnosia)

        // Display pictures
        var myDrawable = resources.getDrawable(R.drawable.logo)
        ivHpnosia.setImageDrawable(myDrawable)
        myDrawable = resources.getDrawable(R.drawable.bluetooth)
        ivBluetooth.setImageDrawable(myDrawable)


        // On click anywhere on the screen
        launchingScreen.setOnClickListener{
            val intent = Intent(this, BluetoothDevicesActivity::class.java)
            startActivity(intent)
        }
    }
}
