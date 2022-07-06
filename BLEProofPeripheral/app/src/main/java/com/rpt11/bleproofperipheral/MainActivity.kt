package com.rpt11.bleproofperipheral

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.ParcelUuid
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.rpt11.bleproofperipheral.util.Constants.BLUETOOTH_ALL_PERMISSIONS_REQUEST_CODE
import com.rpt11.bleproofperipheral.util.Constants.CCC_DESCRIPTOR_UUID
import com.rpt11.bleproofperipheral.util.Constants.CHAR_FOR_INDICATE_UUID
import com.rpt11.bleproofperipheral.util.Constants.CHAR_FOR_READ_UUID
import com.rpt11.bleproofperipheral.util.Constants.CHAR_FOR_WRITE_UUID
import com.rpt11.bleproofperipheral.util.Constants.ENABLE_BLUETOOTH_REQUEST_CODE
import com.rpt11.bleproofperipheral.util.Constants.SERVICE_UUID
import com.rpt11.bleproofperipheral.databinding.ActivityMainBinding
import com.rpt11.bleproofperipheral.services.BLEAdvertiser.advertiseCallback
import com.rpt11.bleproofperipheral.services.BLEAdvertiser.advertiseData
import com.rpt11.bleproofperipheral.services.BLEAdvertiser.advertiseSettings
import com.rpt11.bleproofperipheral.util.*
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val bluetoothManager: BluetoothManager by lazy {
        getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }

    private val bluetoothAdapter: BluetoothAdapter by lazy {
        bluetoothManager.adapter
    }
    //region BLE advertise
    private val bleAdvertiser by lazy {
        bluetoothAdapter.bluetoothLeAdvertiser
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appendLog("MainActivity.onCreate")

        binding.switchAdvertising.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                prepareAndStartAdvertising()
            } else {
                bleStopAdvertising()
            }
        }
    }

    //checking advertising status
    private var isAdvertising = false
        set(value) {
            field = value
            // update visual state of the switch
            runBlocking {
                launch(Dispatchers.IO) {
                     if (value != binding.switchAdvertising.isChecked)
                        binding.switchAdvertising.isChecked = value
                }
                delay(200)
            }
        }

    //Prepare and start advertising near by devices
    private fun prepareAndStartAdvertising() {
        ensureBluetoothCanBeUsed { isSuccess, message ->
            runOnUiThread {
                appendLog(message)
                if (isSuccess) {
                    bleStartAdvertising()
                } else {
                    isAdvertising = false
                }
            }
        }
    }


    //start advertising nearby devices
    @SuppressLint("MissingPermission")
    private fun bleStartAdvertising() {
        isAdvertising = true
        bleStartGattServer()
        bleAdvertiser.startAdvertising(advertiseSettings, advertiseData, advertiseCallback)
    }

    //update the ui for number of subscribers
     private fun updateSubscribersUI() {
        val strSubscribers = "${subscribedDevices.count()} subscribers"
        runOnUiThread {
            binding.textViewSubscribers.text = strSubscribers
        }
    }

    override fun onDestroy() {
        bleStopAdvertising()
        super.onDestroy()
    }

    fun onTapSend(view: View) {
        bleIndicate()
    }

    @SuppressLint("SetTextI18n")
    fun onTapClearLog(view: View) {
        binding.textViewLog.text = "Logs:"
        appendLog("log cleared")
    }

    @SuppressLint("MissingPermission")
    private fun bleStopAdvertising() {
        isAdvertising = false
        bleStopGattServer()
        bleAdvertiser.stopAdvertising(advertiseCallback)
    }

    @SuppressLint("MissingPermission")
     fun bleStartGattServer() {
        val gattServer = bluetoothManager.openGattServer(this, gattServerCallback)
        val service = BluetoothGattService(
            UUID.fromString(SERVICE_UUID),
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        )
        val charForRead = BluetoothGattCharacteristic(
            UUID.fromString(CHAR_FOR_READ_UUID),
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ
        )
        val charForWrite = BluetoothGattCharacteristic(
            UUID.fromString(CHAR_FOR_WRITE_UUID),
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        val charForIndicate = BluetoothGattCharacteristic(
            UUID.fromString(CHAR_FOR_INDICATE_UUID),
            BluetoothGattCharacteristic.PROPERTY_INDICATE,
            BluetoothGattCharacteristic.PERMISSION_READ
        )
        val charConfigDescriptor = BluetoothGattDescriptor(
            UUID.fromString(CCC_DESCRIPTOR_UUID),
            BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
        )
        charForIndicate.addDescriptor(charConfigDescriptor)

        service.addCharacteristic(charForRead)
        service.addCharacteristic(charForWrite)
        service.addCharacteristic(charForIndicate)

        val result = gattServer.addService(service)
        this.gattServer = gattServer
        appendLog(
            "addService " + when (result) {
                true -> "OK"
                false -> "fail"
            }
        )
    }

    /*//The AdvertiseSettings provide a way to adjust advertising preferences for each Bluetooth LE advertisement
    private val advertiseSettings = AdvertiseSettings.Builder()
        .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
        .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
        .setConnectable(true)
        .build()

    *//**Advertise data packet container for Bluetooth LE advertising.
     * This represents the data to be advertised as well as the scan response data for active scans
     * Don't include name, because if name size > 8 bytes, ADVERTISE_FAILED_DATA_TOO_LARGE*//*
    private val advertiseData = AdvertiseData.Builder()
        .setIncludeDeviceName(true)
        .addServiceUuid(ParcelUuid(UUID.fromString(SERVICE_UUID)))
        .build()

   *//*Bluetooth LE advertising callbacks, used to deliver advertising operation status
   on success and on failure*//*
    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            appendLog("Advertise start success\n$SERVICE_UUID")
        }

        override fun onStartFailure(errorCode: Int) {
            val desc = when (errorCode) {
                ADVERTISE_FAILED_DATA_TOO_LARGE -> Constants.FAILED_ON_LARGER_DATA_ADVERTISER
                ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> Constants.FAILED_ON_UNAVAILABLE_ADVERTISER
                ADVERTISE_FAILED_ALREADY_STARTED -> Constants.FAILED_ON_ALREADY_STARTED_ADVERTISER
                ADVERTISE_FAILED_INTERNAL_ERROR -> Constants.FAILED_ON_TERMINAL_ERROR
                ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> Constants.FAILED_ON_UNSUPPORTED_PLATFORM
                else -> ""
            }
            appendLog("Advertise start failed: errorCode=$errorCode $desc")
            isAdvertising = false
        }
    }*/
    //endregion

    //region BLE GATT server
    private var gattServer: BluetoothGattServer? = null
    private val charForIndicate
        get() = gattServer?.getService(UUID.fromString(SERVICE_UUID))
            ?.getCharacteristic(UUID.fromString(CHAR_FOR_INDICATE_UUID))
    private val subscribedDevices = mutableSetOf<BluetoothDevice>()


    //callback indicating when a remote device has been connected or disconnected.
    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            runOnUiThread {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    binding.textViewConnectionState.text = getString(R.string.text_connected)
                    appendLog("Central did connect")
                } else {
                    binding.textViewConnectionState.text = getString(R.string.text_disconnected)
                    appendLog("Central did disconnect")
                    subscribedDevices.remove(device)
                    updateSubscribersUI()
                }
            }
        }

        override fun onNotificationSent(device: BluetoothDevice, status: Int) {
            appendLog("onNotificationSent status=$status")
        }

        @SuppressLint("MissingPermission")
        override fun onCharacteristicReadRequest(
            device: BluetoothDevice,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic
        ) {
            var log = "onCharacteristicRead offset=$offset"
            if (characteristic.uuid == UUID.fromString(CHAR_FOR_READ_UUID)) {
                runOnUiThread {
                    val strValue = binding.editTextCharForRead.text.toString()
                    gattServer?.sendResponse(
                        device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        strValue.toByteArray(Charsets.UTF_8)
                    )
                    log += "\nresponse=success, value=\"$strValue\""
                    appendLog(log)
                }
            } else {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null)
                log += "\nresponse=failure, unknown UUID\n${characteristic.uuid}"
                appendLog(log)
            }
        }

        @SuppressLint("MissingPermission")
        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            var log =
                "onCharacteristicWrite offset=$offset responseNeeded=$responseNeeded preparedWrite=$preparedWrite"
            if (characteristic.uuid == UUID.fromString(CHAR_FOR_WRITE_UUID)) {
                val strValue = value?.toString(Charsets.UTF_8) ?: ""
                log += if (responseNeeded) {
                    gattServer?.sendResponse(
                        device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        strValue.toByteArray(Charsets.UTF_8)
                    )
                    "\nresponse=success, value=\"$strValue\""
                } else {
                    "\nresponse=notNeeded, value=\"$strValue\""
                }
                runOnUiThread {
                    binding.textViewCharForWrite.text = strValue
                }
            } else {
                log += if (responseNeeded) {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null)
                    "\nresponse=failure, unknown UUID\n${characteristic.uuid}"
                } else {
                    "\nresponse=notNeeded, unknown UUID\n${characteristic.uuid}"
                }
            }
            appendLog(log)
        }

        @SuppressLint("MissingPermission")
        override fun onDescriptorReadRequest(
            device: BluetoothDevice,
            requestId: Int,
            offset: Int,
            descriptor: BluetoothGattDescriptor
        ) {
            var log = "onDescriptorReadRequest"
            if (descriptor.uuid == UUID.fromString(CCC_DESCRIPTOR_UUID)) {
                val returnValue = if (subscribedDevices.contains(device)) {
                    log += " CCCD response=ENABLE_NOTIFICATION"
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                } else {
                    log += " CCCD response=DISABLE_NOTIFICATION"
                    BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                }
                gattServer?.sendResponse(
                    device,
                    requestId,
                    BluetoothGatt.GATT_SUCCESS,
                    0,
                    returnValue
                )
            } else {
                log += " unknown uuid=${descriptor.uuid}"
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null)
            }
            appendLog(log)
        }

        @SuppressLint("MissingPermission")
        override fun onDescriptorWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            descriptor: BluetoothGattDescriptor,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray
        ) {
            var strLog = "onDescriptorWriteRequest"
            if (descriptor.uuid == UUID.fromString(CCC_DESCRIPTOR_UUID)) {
                var status = BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED
                if (descriptor.characteristic.uuid == UUID.fromString(CHAR_FOR_INDICATE_UUID)) {
                    if (Arrays.equals(value, BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)) {
                        subscribedDevices.add(device)
                        status = BluetoothGatt.GATT_SUCCESS
                        strLog += ", subscribed"
                    } else if (Arrays.equals(
                            value,
                            BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                        )
                    ) {
                        subscribedDevices.remove(device)
                        status = BluetoothGatt.GATT_SUCCESS
                        strLog += ", unsubscribed"
                    }
                }
                if (responseNeeded) {
                    gattServer?.sendResponse(device, requestId, status, 0, null)
                }
                updateSubscribersUI()
            } else {
                strLog += " unknown uuid=${descriptor.uuid}"
                if (responseNeeded) {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null)
                }
            }
            appendLog(strLog)
        }
    }
    //endregion

  /*  private var activityResultHandlers = mutableMapOf<Int, (Int) -> Unit>()
    private var permissionResultHandlers = mutableMapOf<Int, (Array<out String>, IntArray) -> Unit>()*/

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        activityResultHandlers[requestCode]?.let { handler ->
            handler(resultCode)
        } ?: run {
            appendLog("Error: onActivityResult requestCode=$requestCode result=$resultCode not handled")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionResultHandlers[requestCode]?.let { handler ->
            handler(permissions, grantResults)
        } ?: run {
            appendLog("Error: onRequestPermissionsResult requestCode=$requestCode not handled")
        }
    }

    /*private fun ensureBluetoothCanBeUsed(completion: (Boolean, String) -> Unit) {
        grantBluetoothPeripheralPermissions(AskType.AskOnce) { isGranted ->
            if (!isGranted) {
                completion(false, "Bluetooth permissions denied")
                return@grantBluetoothPeripheralPermissions
            }

            enableBluetooth(AskType.AskOnce) { isEnabled ->
                if (!isEnabled) {
                    completion(false, "Bluetooth OFF")
                    return@enableBluetooth
                }

                completion(true, "BLE ready for use")
            }
        }
    }*/

   /* @SuppressLint("MissingPermission")
    private fun enableBluetooth(askType: AskType, completion: (Boolean) -> Unit) {
        if (bluetoothAdapter.isEnabled) {
            completion(true)
        } else {
            val intentString = BluetoothAdapter.ACTION_REQUEST_ENABLE
            val requestCode = ENABLE_BLUETOOTH_REQUEST_CODE

            // set activity result handler
            activityResultHandlers[requestCode] = { result ->
                Unit
                val isSuccess = result == Activity.RESULT_OK
                if (isSuccess || askType != AskType.InsistUntilSuccess) {
                    activityResultHandlers.remove(requestCode)
                    completion(isSuccess)
                } else {
                    // start activity for the request again
                    startActivityForResult(Intent(intentString), requestCode)
                }
            }
            // start activity for the request
            startActivityForResult(Intent(intentString), requestCode)
        }
    }

    private fun grantBluetoothPeripheralPermissions(
        askType: AskType,
        completion: (Boolean) -> Unit
    ) {
        val wantedPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE,
            )
        } else {
            emptyArray()
        }

        if (wantedPermissions.isEmpty() || hasPermissions(wantedPermissions)) {
            completion(true)
        } else {
            runOnUiThread {
                val requestCode = BLUETOOTH_ALL_PERMISSIONS_REQUEST_CODE
                // set permission result handler
                permissionResultHandlers[requestCode] = { _ *//*permissions*//*, grantResults ->
                    val isSuccess = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
                    if (isSuccess || askType != AskType.InsistUntilSuccess) {
                        permissionResultHandlers.remove(requestCode)
                        completion(isSuccess)
                    } else {
                        // request again
                        requestPermissionArray(wantedPermissions, requestCode)
                    }
                }
                requestPermissionArray(wantedPermissions, requestCode)
            }
        }
    }*/

    //endregion



    @SuppressLint("MissingPermission")
    private fun bleStopGattServer() {
        gattServer?.close()
        gattServer = null
        appendLog("gattServer closed")
        runOnUiThread {
            binding.textViewConnectionState.text = getString(R.string.text_disconnected)
        }
    }

    @SuppressLint("MissingPermission")
    private fun bleIndicate() {
        val text = binding.editTextCharForIndicate.text.toString()
        val data = text.toByteArray(Charsets.UTF_8)
        charForIndicate?.let {
            it.value = data
            for (device in subscribedDevices) {
                appendLog("sending indication \"$text\"")
                gattServer?.notifyCharacteristicChanged(device, it, true)
            }
        }
    }

     @SuppressLint("SetTextI18n")
    private fun appendLog(message: String) {
        Log.d("appendLog", message)
        runOnUiThread {
            val strTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            binding.textViewLog.text = binding.textViewLog.text.toString() + "\n$strTime $message"

            // scroll after delay, because textView has to be updated first
            Handler().postDelayed({
                binding.scrollViewLog.fullScroll(View.FOCUS_DOWN)
            }, 16)
        }
    }
}