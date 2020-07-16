package com.example.clockhandsdetection091.activities

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import com.example.clockhandsdetection091.R

class BluetoothDevicesActivity : AppCompatActivity() {

    lateinit var listViewDevices: ListView
    lateinit var btnRefresh: Button

    private var arrayAdapter: ArrayAdapter<*>? = null
    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private var deviceList: ArrayList<BluetoothDevice> = ArrayList()
    private var socket: SerialSocket? = null

    override fun onDestroy() {
        super.onDestroy()

        if(socket!=null){
            socket!!.disconnect()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth_devices)

        listViewDevices = findViewById(R.id.devicesList)
        btnRefresh = findViewById(R.id.btnRefresh)

        btnRefresh.setOnClickListener {
            if (bluetoothAdapter == null) {
                Toast.makeText(
                    getApplicationContext(),
                    "Bluetooth Not Supported",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                // Get all the paired devices with this smartphone
                val pairedDevices: Set<BluetoothDevice> = bluetoothAdapter.getBondedDevices()
                val list: ArrayList<String> = ArrayList()

                // Not empty
                if (pairedDevices.size > 0) {
                    pairedDevices.forEach {
                        val deviceName: String = it.name
                        val macAddress: String = it.address
                        list.add("Name: " + deviceName + "\nMAC Address: " + macAddress + "\n")
                        deviceList.add(it)
                    }

                    // Display all the paired devices on screen
                    arrayAdapter = ArrayAdapter(applicationContext,
                        android.R.layout.simple_list_item_1, list)
                    listViewDevices.adapter = arrayAdapter
                }
            }
        }

        // When the devices are displayed on screen, a click on one of them enable a connection
        // with it.
        listViewDevices.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val device: BluetoothDevice = deviceList[position]
            val address: String = device.address

            // Enable the connection
            socket = SerialSocket(this)
            socket!!.connect(address)

            // Start next activity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}
