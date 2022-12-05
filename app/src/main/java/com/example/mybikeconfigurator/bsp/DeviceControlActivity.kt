/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.mybikeconfigurator.bsp

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.mybikeconfigurator.R
import java.util.*


/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with `BluetoothLeService`, which in turn interacts with the
 * Bluetooth LE API.
 */
class DeviceControlActivity : AppCompatActivity() {
    //private var mConnectionState: TextView? = null
    // private var mConnectionState: Boolean = false
    private var mConnected = false
    private var mDataField: TextView? = null
    private var mDeviceName: String? = null
    private var mDeviceAddress: String? = null
    private var mGattServicesList: ExpandableListView? = null
    private var mGattServiceData = ArrayList<HashMap<String, String>>()
    private var mGattCharacteristicData = ArrayList<ArrayList<HashMap<String, String>>>()
    private var mBluetoothLeService: BluetoothLeService? = null
    private var mGattCharacteristics: ArrayList<ArrayList<BluetoothGattCharacteristic>>? = ArrayList()
    private var mNotifyCharacteristic: BluetoothGattCharacteristic? = null

    private val LIST_NAME = "NAME"
    private val LIST_UUID = "UUID"


    private var mLight = false

    // Code to manage Service lifecycle.
    private val mServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            mBluetoothLeService = (service as BluetoothLeService.LocalBinder).service
            if (!mBluetoothLeService!!.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth")
                finish()
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService!!.connect(mDeviceAddress)
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            mBluetoothLeService = null
        }
    }

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private val mGattUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothLeService.ACTION_GATT_CONNECTED == action) {
                mConnected = true
                updateConnectionState(true)
                invalidateOptionsMenu()

            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED == action) {
                mConnected = false
                updateConnectionState(false)
                invalidateOptionsMenu()
                clearUI()
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED == action) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService!!.supportedGattServices)
                var characteristic: BluetoothGattCharacteristic? = getCharacteristicForUUID(SampleGattAttributes.PAS_MODE_CHARACTERISTIC_READ)
                if (characteristic != null) {
                    mBluetoothLeService!!.setCharacteristicNotification(characteristic,true)
                    mBluetoothLeService!!.readCharacteristic(characteristic)
                }
                readCurrentSettings()
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE == action) {
                var dataString = intent.getStringExtra(BluetoothLeService.EXTRA_DATA)
                var dataBytes: ByteArray? = intent.getByteArrayExtra(BluetoothLeService.EXTRA_BYTES)
                if (dataString == null) dataString = ""
                displayData(dataString, dataBytes )
            }
        }
    }

    private fun getCharacteristicForUUID(uuid: String): BluetoothGattCharacteristic? {
        if (mGattCharacteristics != null) {
            // get Characteristic from gathered array
            for(c in mGattCharacteristics!!) {
                //if c.
                for(z in c){
                    // Log.d(TAG,"UUID: ${z.uuid.toString()}")
                    if(z.uuid.toString().equals(uuid, true)){
                        return z
                    }
                }
            }
        }
        return null
    }

    private fun readCurrentSettings() {
        // read and subscribe
        var characteristic: BluetoothGattCharacteristic? = getCharacteristicForUUID(SampleGattAttributes.PAS_MODE_CHARACTERISTIC_READ)
        if (characteristic != null) {
            // mBluetoothLeService!!.setCharacteristicNotification(characteristic,true)
            mBluetoothLeService!!.readCharacteristic(characteristic)
        }

        //if(characteristic != null) {
        //}
        // characteristic = getCharacteristicForUUID(SampleGattAttributes.DEVICE_NAME)
        // if(characteristic != null) {
        //    mBluetoothLeService?.readCharacteristic(characteristic)
        // }
    }

    // If a given GATT characteristic is selected, check for supported features.  This sample
    // demonstrates 'Read' and 'Notify' features.  See
    // http://d.android.com/reference/android/bluetooth/BluetoothGatt.html for the complete
    // list of supported characteristic features.
    private val servicesListClickListner = ExpandableListView.OnChildClickListener { parent, v, groupPosition, childPosition, id ->
        if (mGattCharacteristics != null) {
            val characteristic = mGattCharacteristics!![groupPosition][childPosition]
            val charaProp = characteristic.properties
            if (charaProp or BluetoothGattCharacteristic.PROPERTY_READ > 0) {
                // If there is an active notification on a characteristic, clear
                // it first so it doesn't update the data field on the user interface.
                if (mNotifyCharacteristic != null) {
                    mBluetoothLeService!!.setCharacteristicNotification(
                            mNotifyCharacteristic!!, false)
                    mNotifyCharacteristic = null
                }
                mBluetoothLeService!!.readCharacteristic(characteristic)
            }
            if (charaProp or BluetoothGattCharacteristic.PROPERTY_NOTIFY > 0) {
                mNotifyCharacteristic = characteristic
                mBluetoothLeService!!.setCharacteristicNotification(
                        characteristic, true)
            }
            return@OnChildClickListener true
        }
        false
    }

    private fun clearUI() {
        // mGattServicesList!!.setAdapter(null as SimpleExpandableListAdapter?)
        // mDataField!!.setText(R.string.no_data)
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.gatt_services_characteristics)
        setContentView(R.layout.activity_main)

        val intent = intent
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME)
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS)

        // Sets up UI references.
        // (findViewById(R.id.device_address) as TextView).text = mDeviceAddress
        // mGattServicesList = findViewById(R.id.gatt_services_list) as ExpandableListView
        // mGattServicesList!!.setOnChildClickListener(servicesListClickListner)
        // mConnectionState = findViewById(R.id.connection_state) as TextView
        // mDataField = findViewById(R.id.data_value) as TextView

        //println(mDataField)

        //actionBar!!.title = mDeviceName
        //actionBar!!.setDisplayHomeAsUpEnabled(true)
        val gattServiceIntent = Intent(this, BluetoothLeService::class.java)
        bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE)


        // wire up modes
        var radio_mode_group: RadioGroup = findViewById(R.id.radio_mode_group)
        radio_mode_group.setOnCheckedChangeListener(RadioGroup.OnCheckedChangeListener { group, checkedId ->
            val radio: RadioButton = findViewById(checkedId)
            Toast.makeText(applicationContext," On checked change :"+
                    " ${radio.text}",
                Toast.LENGTH_SHORT).show()
            var bikeMode = 1
            when(radio.id) {
                R.id.radio_mode_2 -> bikeMode = 2
                R.id.radio_mode_3 -> bikeMode = 3
                R.id.radio_mode_4 -> bikeMode = 4
                else ->bikeMode = 1
            }
            setBikeMode(bikeMode)
        })

        var reloadButton: Button = findViewById(R.id.reload_button)
        reloadButton.setOnClickListener {
            readCurrentSettings()
        }
    }

    fun setBikeMode(mode: Int) {
        // get Characteristics
        Log.d(TAG, "try to set mode ${mode}")
        if (mBluetoothLeService != null) {
            var data: ByteArray = hexStringToByteArray("00d1000" + mode + "040000000000")
            var characteristic = getCharacteristicForUUID(SampleGattAttributes.PAS_MODE_CHARACTERISTIC_WRITE)
            if (characteristic != null) {
                mBluetoothLeService!!.writeCharacteristics(characteristic, data)
            }
        }
    }

    private fun hexVal(ch: Char): Int {
        return ch.digitToIntOrNull(16) ?: -1
    }

    private fun parseHex(hexString: String): ByteArray? {
        var hexString = hexString
        hexString = hexString.replace("\\s".toRegex(), "").uppercase(Locale.getDefault())
        var filtered = String()
        for (i in 0 until hexString.length) {
            if (hexVal(hexString[i]) != -1) filtered += hexString[i]
        }
        if (filtered.length % 2 != 0) {
            val last = filtered[filtered.length - 1]
            filtered = filtered.substring(0, filtered.length - 1) + '0' + last
        }
        return hexStringToByteArray(filtered)
    }

    private fun hexStringToByteArray(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((s[i].digitToIntOrNull(16) ?: -1 shl 4)
            + s[i + 1].digitToIntOrNull(16)!! ?: -1).toByte()
            i += 2
        }
        Log.d(TAG, "convertet ${data.size} Bytes from $s to $data")
        return data
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter())
        if (mBluetoothLeService != null) {
            val result = mBluetoothLeService!!.connect(mDeviceAddress)
            Log.d(TAG, "Connect request result=" + result)
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(mGattUpdateReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(mServiceConnection)
        mBluetoothLeService = null
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.gatt_services, menu)
        if (mConnected) {
            menu.findItem(R.id.menu_connect).isVisible = false
            menu.findItem(R.id.menu_disconnect).isVisible = true
        } else {
            menu.findItem(R.id.menu_connect).isVisible = true
            menu.findItem(R.id.menu_disconnect).isVisible = false
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_connect -> {
                mBluetoothLeService!!.connect(mDeviceAddress)
                return true
            }
            R.id.menu_disconnect -> {
                mBluetoothLeService!!.disconnect()
                return true
            }
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun updateConnectionState(connected: Boolean) {
        runOnUiThread {
            val myLayout = findViewById<View>(R.id.activity_main)
            if (connected) {
                myLayout.visibility = View.VISIBLE
            }
            else {
                myLayout.visibility = View.INVISIBLE
            }
        }
    }

    private fun displayData(data: String, bytes: ByteArray?) {
        if (data != null) {
            var dataList = data.split("\n")
            var uuid: String = dataList[0]
            // var bytes: ByteArray = (dataList[1].toByteArray(Charsets.UTF_8))
            var string: String = dataList[2]
            //mDataField!!.text = data
            Log.d(TAG, "received data for uuid ${uuid} String: ${string} Bytes: ${bytes} ByteArray Length: ${bytes?.size}")
            //System.out.println(data);
            when(uuid){
                SampleGattAttributes.PAS_MODE_CHARACTERISTIC_READ -> {
                    Log.d(TAG,"received notificationRoute")
                    // Mode 3: 03 00 03 00 01 04 00 00 00 00
                    // Mode 1: 03 00 01 00 01 04 00 00 00 00
                    // Mode 0: 03 00 00 00 01 04 00 00 00 00

                    // Keepalive?: 02 02 00 3F 00 00 AB 44 00 00
                    //                      ^^ counts down

                    // Speed?: 04 01 F9 C5 00 00 00 00 00 00
                    //         04 01 EF C5 00 00 00 00 00 00
                    //         04 01 EE C5 00 00 00 00 00 00
                    //         04 01 F5 C5 00 00 00 00 00 00
                    //         04 01 F4 C5 00 00 00 00 00 00
                    //         04 01 F3 C5 00 00 00 00 00 00
                    //         04 01 F2 C5 00 00 00 00 00 00
                }
                else -> {
                    Log.d(TAG, "received unrecognized value")
                }
            }
        }
    }

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private fun displayGattServices(gattServices: List<BluetoothGattService>?) {
        if (gattServices == null) return
        var uuid: String? = null
        val unknownServiceString = resources.getString(R.string.unknown_service)
        val unknownCharaString = resources.getString(R.string.unknown_characteristic)
        mGattServiceData = ArrayList<HashMap<String, String>>()
        mGattCharacteristicData = ArrayList<ArrayList<HashMap<String, String>>>()
        mGattCharacteristics = ArrayList<ArrayList<BluetoothGattCharacteristic>>()

        // Loops through available GATT Services.
        for (gattService in gattServices) {
            val currentServiceData = HashMap<String, String>()
            uuid = gattService.uuid.toString()
            Log.d(TAG,"Service uuid: ${uuid} : ${SampleGattAttributes.lookup(uuid, unknownServiceString)}")
            currentServiceData.put(
                    LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString)
            )
            currentServiceData.put(LIST_UUID, uuid)
            mGattServiceData.add(currentServiceData)

            val gattCharacteristicGroupData = ArrayList<HashMap<String, String>>()
            val gattCharacteristics = gattService.characteristics
            val charas = ArrayList<BluetoothGattCharacteristic>()

            // Loops through available Characteristics.
            for (gattCharacteristic in gattCharacteristics) {
                charas.add(gattCharacteristic)
                val currentCharaData = HashMap<String, String>()
                uuid = gattCharacteristic.uuid.toString()
                Log.d(TAG,"Characteristic found: ${uuid} with data: ${currentCharaData}")
                Log.d(TAG,"value: ${gattCharacteristic.value}")
                // println(uuid)
                // println(currentCharaData)

                currentCharaData.put(
                        LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString)
                )
                currentCharaData.put(LIST_UUID, uuid)
                gattCharacteristicGroupData.add(currentCharaData)
            }
            mGattCharacteristics!!.add(charas)
            mGattCharacteristicData.add(gattCharacteristicGroupData)
        }

        /*
        val gattServiceAdapter = SimpleExpandableListAdapter(
                this,
                gattServiceData,
                android.R.layout.simple_expandable_list_item_2,
                arrayOf(LIST_NAME, LIST_UUID),
                intArrayOf(android.R.id.text1, android.R.id.text2),
                gattCharacteristicData,
                android.R.layout.simple_expandable_list_item_2,
                arrayOf(LIST_NAME, LIST_UUID),
                intArrayOf(android.R.id.text1, android.R.id.text2)
        )
        mGattServicesList!!.setAdapter
         */
    }

    companion object {
        private val TAG = DeviceControlActivity::class.java!!.getSimpleName()


       @JvmField var EXTRAS_DEVICE_NAME = "DEVICE_NAME"
       @JvmField var EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS"

        private fun makeGattUpdateIntentFilter(): IntentFilter {
            val intentFilter = IntentFilter()
            intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
            intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
            intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED)
            intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE)
            return intentFilter
        }
    }


}
