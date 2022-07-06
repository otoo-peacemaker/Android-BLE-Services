package com.rpt11.bleproofperipheral.services

import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.os.ParcelUuid
import android.util.Log
import com.rpt11.bleproofperipheral.util.Constants
import com.rpt11.bleproofperipheral.util.Constants.SERVICE_UUID
import java.util.*

object BLEAdvertiser {

    //checking advertising status
    private var isAdvertising = true

    //The AdvertiseSettings provide a way to adjust advertising preferences for each Bluetooth LE advertisement
    private val advertiseSettings = AdvertiseSettings.Builder()
        .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
        .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
        .setConnectable(true)
        .build()

    /**Advertise data packet container for Bluetooth LE advertising.
     * This represents the data to be advertised as well as the scan response data for active scans
     * Don't include name, because if name size > 8 bytes, ADVERTISE_FAILED_DATA_TOO_LARGE*/
    private val advertiseData = AdvertiseData.Builder()
        .setIncludeDeviceName(true)
        .addServiceUuid(ParcelUuid(UUID.fromString(SERVICE_UUID)))
        .build()

    /*Bluetooth LE advertising callbacks, used to deliver advertising operation status
    on success and on failure*/
    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            Log.d("Advertiser", "Advertise start success\n$SERVICE_UUID")
            //appendLog("Advertise start success\n$SERVICE_UUID")
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

            Log.d(
                "Advertiser",
                "Advertise start failed: errorCode=$errorCode $desc"
            )
            // appendLog("Advertise start failed: errorCode=$errorCode $desc")
            isAdvertising = false
        }
    }
    //endregion
}