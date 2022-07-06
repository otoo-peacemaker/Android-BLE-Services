package com.rpt11.bleproofperipheral.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.app.ActivityCompat
import com.rpt11.bleproofperipheral.R
import com.rpt11.bleproofperipheral.services.BLEGattServer
import com.rpt11.bleproofperipheral.util.Constants.BLUETOOTH_ALL_PERMISSIONS_REQUEST_CODE
import com.rpt11.bleproofperipheral.util.Constants.ENABLE_BLUETOOTH_REQUEST_CODE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*


//Logs

@SuppressLint("SetTextI18n")
fun Activity.appendLogs(message: String) {
    val view = layoutInflater.inflate(R.layout.activity_main, null)
    val textViewLog = view.findViewById<TextView>(R.id.textViewLog)
    val scrollViewLog = view.findViewById<ScrollView>(R.id.scrollViewLog)
    Log.d("appendLog", message)

    runBlocking {
        val strTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        textViewLog.text = textViewLog.text.toString() + "\n$strTime $message"
        launch(Dispatchers.IO) {
            // scroll after delay, because textView has to be updated first
            scrollViewLog.fullScroll(View.FOCUS_DOWN)
        }
        delay(16)
    }
}

fun Context.hasPermissions(permissions: Array<String>): Boolean = permissions.all {
    ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
}

fun Activity.requestPermissionArray(permissions: Array<String>, requestCode: Int) {
    ActivityCompat.requestPermissions(this, permissions, requestCode)
}

fun Activity.ensureBluetoothCanBeUsed(completion: (Boolean, String) -> Unit) {
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
}

fun Activity.grantBluetoothPeripheralPermissions(askType: AskType, completion: (Boolean) -> Unit) {
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
            permissionResultHandlers[requestCode] = { _ /*permissions*/, grantResults ->
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
}

@SuppressLint("MissingPermission")
private fun Activity.enableBluetooth(askType: AskType, completion: (Boolean) -> Unit) {
    val gattServer: BLEGattServer by lazy { BLEGattServer() }
    val bluetoothAdapter = gattServer.bluetoothAdapter
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

var activityResultHandlers = mutableMapOf<Int, (Int) -> Unit>()
var permissionResultHandlers = mutableMapOf<Int, (Array<out String>, IntArray) -> Unit>()


//region Permissions and Settings management
enum class AskType {
    AskOnce,
    InsistUntilSuccess
}