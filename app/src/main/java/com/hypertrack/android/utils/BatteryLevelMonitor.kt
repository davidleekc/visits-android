package com.hypertrack.android.utils

import android.content.Context
import android.content.Context.BATTERY_SERVICE
import android.os.BatteryManager
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BatteryLevelMonitor(
    private val crashReportsProvider: CrashReportsProvider
) {

    private lateinit var batteryService: BatteryManager

    fun init(appContext: Context) {
        try {
            batteryService = appContext.getSystemService(BATTERY_SERVICE) as BatteryManager
            GlobalScope.launch {
                try {
                    while (true) {
                        logBatteryLevel()
                        delay(20 * 60 * 1000)
                    }
                } catch (e: Exception) {
                    crashReportsProvider.logException(e)
                }
            }
        } catch (e: Exception) {
            crashReportsProvider.logException(e)
        }
    }

    private fun logBatteryLevel() {
        val batLevel: Int = batteryService.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        crashReportsProvider.log("battery_value: $batLevel")
    }

}