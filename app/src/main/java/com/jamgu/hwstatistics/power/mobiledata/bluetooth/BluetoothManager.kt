package com.jamgu.hwstatistics.power.mobiledata.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.os.Build
import com.jamgu.hwstatistics.power.INeedPermission
import com.jamgu.hwstatistics.power.mobiledata.bluetooth.model.BluetoothData
import com.jamgu.hwstatistics.power.mobiledata.bluetooth.model.BondState
import com.jamgu.hwstatistics.power.mobiledata.bluetooth.model.LightBluetoothDevice
import com.jamgu.hwstatistics.power.mobiledata.bluetooth.model.ScanMode

/**
 * Created by jamgu on 2021/11/18
 */
@SuppressLint("HardwareIds", "MissingPermission")
object BluetoothManager: INeedPermission {

    var data: BluetoothData? = null
        private set

    fun getBluetoothData(): BluetoothData? {
        update()
        return data
    }

    override fun permission(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.BLUETOOTH_SCAN,
                android.Manifest.permission.BLUETOOTH_ADVERTISE,
            )
        } else {
            return arrayOf()
        }
    }

    fun update() {
        data = BluetoothAdapter.getDefaultAdapter()?.let {
            val mac = it.address
            val isEnabled = it.isEnabled
            val scanMode = ScanMode.fromAndroidState(it.scanMode)
            val bonded = toList(it.bondedDevices)

            BluetoothData(
                address = mac,
                enabled = isEnabled,
                scanMode = scanMode,
                bondedDevices = bonded
            )
        }
    }

    private fun toList(devices: Collection<BluetoothDevice>?): List<LightBluetoothDevice> {
        if (devices.isNullOrEmpty()) {
            return emptyList()
        }

        return devices.map {
            LightBluetoothDevice(
                name = it.name ?: "",
                address = it.address ?: "",
                bondState = BondState.fromAndroidState(it.bondState))
        }
    }
}