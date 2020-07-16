package com.example.clockhandsdetection091.activities

import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.clockhandsdetection091.R


class LaunchingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launching)

        val btnFindProduct: Button = findViewById(R.id.btnFindProduct)
        val ivBluetooth: ImageView = findViewById(R.id.ivBluetooth)
        val ivHpnosia: ImageView = findViewById(R.id.ivHypnosia)

        // Display pictures
        var myDrawable = resources.getDrawable(R.drawable.logo)
        ivHpnosia.setImageDrawable(myDrawable)
        myDrawable = resources.getDrawable(R.drawable.bluetooth)
        ivBluetooth.setImageDrawable(myDrawable)

        // On "find product" button click
        btnFindProduct.setOnClickListener{
            val intent = Intent(this, BluetoothDevicesActivity::class.java)
            startActivity(intent)
        }
    }
}
