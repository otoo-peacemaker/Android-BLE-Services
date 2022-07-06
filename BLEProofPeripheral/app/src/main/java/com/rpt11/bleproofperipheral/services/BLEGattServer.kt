package com.rpt11.bleproofperipheral.services

import android.annotation.SuppressLint
import android.app.Service
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.rpt11.bleproofperipheral.util.Constants.CCC_DESCRIPTOR_UUID
import com.rpt11.bleproofperipheral.util.Constants.CHAR_FOR_INDICATE_UUID
import com.rpt11.bleproofperipheral.util.Constants.CHAR_FOR_READ_UUID
import com.rpt11.bleproofperipheral.util.Constants.CHAR_FOR_WRITE_UUID
import com.rpt11.bleproofperipheral.util.Constants.SERVICE_UUID
import kotlinx.coroutines.runBlocking
import java.util.*

class BLEGattServer : Service() {
    private val mBinder = LocalBinder()//Binder for Activity that binds to this Service
     val bluetoothAdapter: BluetoothAdapter by lazy {
        bluetoothManager.adapter
    }

    //region BLE advertise
    private val bleAdvertiser by lazy {
        bluetoothAdapter.bluetoothLeAdvertiser
    }

    //region BLE GATT server
    private var gattServer: BluetoothGattServer? = null
    private val charForIndicate
        get() = gattServer?.getService(UUID.fromString(SERVICE_UUID))
            ?.getCharacteristic(UUID.fromString(CHAR_FOR_INDICATE_UUID))
    private val subscribedDevices = mutableSetOf<BluetoothDevice>()

    //update the ui for number of subscribers
    private fun updateSubscribersUI() {
        val strSubscribers = "${subscribedDevices.count()} subscribers"
        runBlocking {
            Log.d("Subscribers", strSubscribers)
            //binding.textViewSubscribers.text = strSubscribers
        }
    }

    private val bluetoothManager: BluetoothManager by lazy {
        getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
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
        Log.d(
            "AddServices", "::" +
                    "addService " + when (result) {
                true -> "OK"
                false -> "fail"
            }
        )
        /*appendLog(
            "addService " + when (result) {
                true -> "OK"
                false -> "fail"
            }
        )*/
    }


    //callback indicating when a remote device has been connected or disconnected.
    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            runBlocking {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    // binding.textViewConnectionState.text = getString(R.string.text_connected)
                    //appendLog("Central did connect")
                    Log.d("Manager", "Central did connect")
                } else {
                    //  binding.textViewConnectionState.text = getString(R.string.text_disconnected)
                    Log.d("Manager", "Central did connect")
                    // appendLog("Central did disconnect")
                    subscribedDevices.remove(device)
                    updateSubscribersUI()
                }
            }
        }

        override fun onNotificationSent(device: BluetoothDevice, status: Int) {
            //appendLog("onNotificationSent status=$status")
            Log.d("Notification status:", "$status")
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
                runBlocking {
                    //val strValue = binding.editTextCharForRead.text.toString()
                    val strValue = ""
                    gattServer?.sendResponse(
                        device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        strValue.toByteArray(Charsets.UTF_8)
                    )
                    log += "\nresponse=success, value=\"$strValue\""
                    Log.d("Logs:", log)
                    // appendLog(log)
                }
            } else {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null)
                log += "\nresponse=failure, unknown UUID\n${characteristic.uuid}"
                Log.d("Logs:", log)
                // appendLog(log)
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
                runBlocking {
                    Log.d("VALUE", strValue)
                    // binding.textViewCharForWrite.text = strValue
                }
            } else {
                log += if (responseNeeded) {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null)
                    "\nresponse=failure, unknown UUID\n${characteristic.uuid}"
                } else {
                    "\nresponse=notNeeded, unknown UUID\n${characteristic.uuid}"
                }
            }
            Log.d("Logs:", log)
            //appendLog(log)
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

            Log.d("Logs:", log)
            // appendLog(log)
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
            Log.d("Logs:", strLog)
            //appendLog(strLog)
        }
    }
    //endregion


    override fun onBind(intent: Intent?): IBinder {
        return mBinder//Return LocalBinder when an Activity binds to this Service
    }

    // A Binder to return to an activity to let it bind to this service
    inner class LocalBinder : Binder() {
        internal fun getService(): BLEGattServer {
            return this@BLEGattServer//Return this instance of BluetoothLeService so clients can call its public methods
        }
    }
}