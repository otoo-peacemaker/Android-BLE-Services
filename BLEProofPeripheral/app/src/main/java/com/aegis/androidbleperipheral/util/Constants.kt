
package com.aegis.androidbleperipheral.util

import android.os.ParcelUuid

/**
 * Constants for use in the Bluetooth Advertisements sample
 */
object Constants {
    /**
     * UUID identified with this app - set as Service UUID for BLE Advertisements.
     *
     * Bluetooth requires a certain format for UUIDs associated with Services.
     * The official specification can be found here:
     * [://www.bluetooth.org/en-us/specification/assigned-numbers/service-discovery][https]
     */
    val Service_UUID = ParcelUuid.fromString("0000b81d-0000-1000-8000-00805f9b34fb")

    //Advertiser failure callbacks
    const val FAILED_ON_LARGER_DATA_ADVERTISER = "\nADVERTISE_FAILED_DATA_TOO_LARGE"
    const val FAILED_ON_UNAVAILABLE_ADVERTISER = "\nADVERTISE_FAILED_TOO_MANY_ADVERTISERS"
    const val FAILED_ON_ALREADY_STARTED_ADVERTISER = "\nADVERTISE_FAILED_ALREADY_STARTED"
    const val FAILED_ON_TERMINAL_ERROR = "\nADVERTISE_FAILED_INTERNAL_ERROR"
    const val FAILED_ON_UNSUPPORTED_PLATFORM = "\nADVERTISE_FAILED_FEATURE_UNSUPPORTED"


    const val ACTION_GATT_CONNECTED =
        "ACTION_GATT_CONNECTED" //Strings representing actions to broadcast to activities
    const val ACTION_GATT_DISCONNECTED = "ACTION_GATT_DISCONNECTED" // Old com.zco.ble
    const val ACTION_GATT_SERVICES_DISCOVERED = "ACTION_GATT_SERVICES_DISCOVERED"
    const val ACTION_DATA_AVAILABLE = "ACTION_DATA_AVAILABLE"
    const val ACTION_DATA_WRITTEN = "ACTION_DATA_WRITTEN"
    const val EXTRA_DATA = "EXTRA_DATA"
    const val EXTRA_UUID = "EXTRA_UUID"
    const val ENABLE_BLUETOOTH_REQUEST_CODE = 1
    const val BLUETOOTH_ALL_PERMISSIONS_REQUEST_CODE = 2
    const val SERVICE_UUID = "25AE1441-05D3-4C5B-8281-93D4E07420CF"
    const val CHAR_FOR_READ_UUID = "25AE1442-05D3-4C5B-8281-93D4E07420CF"
    const val CHAR_FOR_WRITE_UUID = "25AE1443-05D3-4C5B-8281-93D4E07420CF"//32
    const val CHAR_FOR_INDICATE_UUID = "25AE1444-05D3-4C5B-8281-93D4E07420CF"
    const val CCC_DESCRIPTOR_UUID = "00002902-0000-1000-8000-00805f9b34fb"
    const val REQUEST_ENABLE_BT = 1
}