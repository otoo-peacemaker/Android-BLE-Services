package com.rpt11.bleproofperipheral.util

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.app.ActivityCompat
import com.rpt11.bleproofperipheral.R
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
