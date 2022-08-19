@file:Suppress("DEPRECATION")

package com.aegis.androidbleperipheral.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.app.ActivityCompat
import com.aegis.androidbleperipheral.R
import com.aegis.androidbleperipheral.services.BLEGattServer
import com.aegis.androidbleperipheral.services.GetMACAddress
import com.aegis.androidbleperipheral.util.Constants.BLUETOOTH_ALL_PERMISSIONS_REQUEST_CODE
import com.aegis.androidbleperipheral.util.Constants.ENABLE_BLUETOOTH_REQUEST_CODE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*


//Logs

@SuppressLint("SetTextI18n")
fun Activity.appendLogs(message: String, textViewLog: TextView, scrollViewLog:ScrollView) {
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

private var activityResultHandlers = mutableMapOf<Int, (Int) -> Unit>()
private var permissionResultHandlers = mutableMapOf<Int, (Array<out String>, IntArray) -> Unit>()





fun Activity.connectivity(status: Boolean) {
    val window = this.window
    val context = this
    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    window.clearFlags(FLAG_TRANSLUCENT_STATUS)
    runBlocking {
        when (status) {
            true -> window.statusBarColor = context.resources.getColor(R.color.purple_700)
            false -> {
                window.statusBarColor = context.resources.getColor(R.color.red)
            }
        }

    }

}


fun Activity.setStatusToDefault() {
    val window = this.window
    val context = this
    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    window.clearFlags(FLAG_TRANSLUCENT_STATUS)
    runOnUiThread {
        Handler().postDelayed({
            window.statusBarColor = context.resources.getColor(android.R.color.transparent)
        }, 3000)
    }
}

fun Activity.connectivityStatus(status: Boolean, resId: TextView) {
    val context = this
    runBlocking {
        when (status) {
            true -> {
                resId.setBackgroundColor(context.resources.getColor(R.color.purple_700))
            }
            false -> {
                resId.setBackgroundColor(context.resources.getColor(R.color.red))
            }
        }
    }

}


fun Activity.displayWritableValues(
    labelId: TextView,
    valueId: TextView,
    valuePairs: KeyValuePair,
    latId: TextView? = null,
    longId: TextView? = null,
) {
    return with(valuePairs) {
        when (key) {
            "1" -> {
                labelId.text = getString(R.string.distance)
                valueId.text = value
            }
            "2" -> {
                labelId.text = getString(R.string.height)
                valueId.text = value
            }
            "3" -> {
                labelId.text = getString(R.string.scale)
                valueId.text = value
            }
            "4" -> {
                labelId.text = getString(R.string.hud_pitch)
                valueId.text = value
            }
            "5" -> {
                labelId.text = getString(R.string.eye_pitch)
                valueId.text = value
            }
            else -> {
                latId?.text = key
                longId?.text = value
            }
        }
    }
}

fun getMacAddress(): String{
    val address = GetMACAddress()
    return address.macAddress
}

