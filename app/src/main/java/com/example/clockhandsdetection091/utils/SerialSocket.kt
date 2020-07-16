package com.example.clockhandsdetection091.activities

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.os.AsyncTask
import android.util.Log
import android.widget.Toast
import java.io.IOException
import java.io.OutputStream
import java.util.*

class SerialSocket(val context: Context){

    companion object{
        private val TAG = "SerialSocket"

        private val myUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private var bluetoothSocket: BluetoothSocket? = null
        private var bluetoothAdapter: BluetoothAdapter? = null
        private var deviceAddress: String = ""

        // Output stream object used to send data
        var outputStream: OutputStream? = null
        var isConnected: Boolean = false
    }

    fun connect(address: String){
        deviceAddress = address
        ConnectToDevice(this.context).execute()
    }

    fun disconnect(){
        if(bluetoothSocket!=null){
            try{
                bluetoothSocket!!.close()
                bluetoothSocket = null
                isConnected = false
            }catch(e: IOException){
                e.printStackTrace()
            }
        }
    }

    //------------------------------------------------------------------------------------
    // Async task that'll connect to the selected device
    //------------------------------------------------------------------------------------
    private class ConnectToDevice(private val context: Context): AsyncTask<Void, Void, String>() {
        private var connectSuccess = true

        override fun doInBackground(vararg params: Void?): String? {
            try{
                if(bluetoothSocket == null || !isConnected){
                    bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                    val device = bluetoothAdapter!!.getRemoteDevice(deviceAddress)
                    bluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(myUUID)
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery()
                    bluetoothSocket!!.connect()

                    // Get the output stream object
                    outputStream = bluetoothSocket!!.outputStream
                }
            }catch(e: IOException){
                connectSuccess = false
                e.printStackTrace()
            }
            return null
        }

        override fun onPreExecute() {
            super.onPreExecute()
            Log.d(TAG, "Connecting...")
            Toast.makeText(context,"Connecting... Please wait",Toast.LENGTH_SHORT).show()
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if(!connectSuccess){
                Log.e(TAG, "Couldn't connect")
                Toast.makeText(context,"Couldn't connect",Toast.LENGTH_SHORT).show()
            }else{
                isConnected = true
                Log.e(TAG, "Connected")
                Toast.makeText(context,"Connected",Toast.LENGTH_SHORT).show()
            }
        }
    }
}
