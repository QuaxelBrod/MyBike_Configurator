package com.example.mybikeconfigurator.ble

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.core.app.ActivityCompat
import com.example.mybikeconfigurator.bsp.DeviceControlActivity
import com.example.mybikeconfigurator.bsp.DeviceScanActivity
import java.util.ArrayList


class BLEWrapper {
    val TAG = "BLEWrapper"
    var bleAvailable = false
    var blePermission = false
    var bleActivated = false
    var activityContext: Context? = null

    // private var mLeDeviceListAdapter: LeDeviceListAdapter? = null
    var bleAdapter: BluetoothAdapter? = null
    var mScanning = false


    // var gattClientService: GattClientService = null

    var onCallback: () -> Unit = fun() {}
    var offCallback: () -> Unit = fun() {}

    val bleBroadCastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            var action: String? = intent?.getAction()
            Log.d(TAG, "bluetooth receiver received message: " + action)

            // It means the user has changed his bluetooth state.
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                if (intent?.getIntExtra(
                        BluetoothAdapter.EXTRA_STATE,
                        -1
                    ) == BluetoothAdapter.STATE_TURNING_OFF
                ) {
                    // The user bluetooth is turning off yet, but it is not disabled yet.
                    Log.d(TAG, "Bluetooth is going down")
                    offCallback()
                    bleActivated = false
                    return
                }

                if (intent?.getIntExtra(
                        BluetoothAdapter.EXTRA_STATE,
                        -1
                    ) == BluetoothAdapter.STATE_OFF
                ) {
                    // The user bluetooth is already disabled.
                    Log.d(TAG, "Bluetooth is down")
                    bleActivated = false
                    offCallback()
                    return
                }

                if (intent?.getIntExtra(
                        BluetoothAdapter.EXTRA_STATE,
                        -1
                    ) == BluetoothAdapter.STATE_TURNING_ON
                ) {
                    // The user bluetooth is already disabled.
                    Log.d(TAG, "Bluetooth is going up")
                    bleActivated = true
                    onCallback()
                    return
                }

                if (intent?.getIntExtra(
                        BluetoothAdapter.EXTRA_STATE,
                        -1
                    ) == BluetoothAdapter.STATE_ON
                ) {
                    // The user bluetooth is already disabled.
                    Log.d(TAG, "Bluetooth is up")
                    bleActivated = true
                    onCallback()
                    return
                }
            }
            // }
        }
    }


    @RequiresApi(Build.VERSION_CODES.M)
    fun init(context: Context) {
        Log.d(TAG, "init called")
        //if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
        //    Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
        //    finish();
        //}
        activityContext = context
        blePermission = checkPermissions()
        Log.d(TAG, "Permission = " + blePermission)
        bleActivated = this.isBluetoothEnabled()
        Log.d(TAG, "Bluetooth on: " + bleActivated)
    }

    fun destroy() {
        activityContext?.let { it.unregisterReceiver(bleBroadCastReceiver) }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun isBluetoothEnabled(): Boolean {
        val bluetoothManager: BluetoothManager? =
            activityContext?.getSystemService(BluetoothManager::class.java)
        val bluetoothAdapter: BluetoothAdapter = bluetoothManager?.adapter ?: return false
        Log.d(TAG, "Bluetooth-adapter available")
        activityContext?.let {
            it.registerReceiver(
                bleBroadCastReceiver,
                IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
            )
        }
        Log.d(TAG, "Bluetooth change registeres")
        bleAvailable = true
        return bluetoothAdapter.isEnabled
    }

    /**
     * Checks is bluetooth permission is given
     */
    private fun checkPermissions(): Boolean {
        val permission = "android.permission.ACCESS_COARSE_LOCATION"
        val res = activityContext?.checkCallingOrSelfPermission(permission)
        return res == PackageManager.PERMISSION_GRANTED
    }


  /*
    fun scanLeDevice(enable: Boolean) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            bleAdapter!!.postDelayed({
                mScanning = false
                mBluetoothAdapter!!.stopLeScan(mLeScanCallback)
                invalidateOptionsMenu()
            }, DeviceScanActivity.SCAN_PERIOD)

            mScanning = true
            bleAdapter!!.startLeScan(mLeScanCallback)
        } else {
            mScanning = false
            bleAdapter!!.stopLeScan(mLeScanCallback)
        }
        invalidateOptionsMenu()
    }
*/

    // Device scan callback.
    private val mLeScanCallback = BluetoothAdapter.LeScanCallback { device, rssi, scanRecord ->

        Log.d(TAG, "device received " + device.toString())
    /*
        runOnUiThread {
            mLeDeviceListAdapter!!.addDevice(device)
            mLeDeviceListAdapter!!.notifyDataSetChanged()
        }

         */
    }

    /*
    // Adapter for holding devices found through scanning.
    private inner class LeDeviceListAdapter : BaseAdapter() {
        private val mLeDevices: ArrayList<BluetoothDevice>
        private val mInflator: LayoutInflater

        init {
            mLeDevices = ArrayList<BluetoothDevice>()
            // mInflator = this@DeviceScanActivity.layoutInflater
        }

        fun addDevice(device: BluetoothDevice) {
            if (!mLeDevices.contains(device)) {
                mLeDevices.add(device)
            }
        }

        fun getDevice(position: Int): BluetoothDevice? {
            return mLeDevices[position]
        }

        fun clear() {
            mLeDevices.clear()
        }

        override fun getCount(): Int {
            return mLeDevices.size
        }

        override fun getItem(i: Int): Any {
            return mLeDevices[i]
        }

        override fun getItemId(i: Int): Long {
            return i.toLong()
        }

        /*
        override fun getView(i: Int, view: View?, viewGroup: ViewGroup): View {
            var view = view
            val viewHolder: DeviceScanActivity.ViewHolder
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null)
                viewHolder = DeviceScanActivity.ViewHolder()
                viewHolder.deviceAddress = view!!.findViewById(R.id.device_address) as TextView
                viewHolder.deviceName = view.findViewById(R.id.device_name) as TextView
                view.tag = viewHolder
            } else {
                viewHolder = view.tag as DeviceScanActivity.ViewHolder
            }

            val device = mLeDevices[i]
            val deviceName = device.name
            if (deviceName != null && deviceName.length > 0)
                viewHolder.deviceName!!.text = deviceName
            else
                viewHolder.deviceName!!.setText(R.string.unknown_device)
            viewHolder.deviceAddress!!.text = device.address

            return view
        }
         */
    }

     */
}