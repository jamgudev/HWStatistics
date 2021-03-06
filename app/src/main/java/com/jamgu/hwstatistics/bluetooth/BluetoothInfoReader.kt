/*
 * Copyright 2020 Alexandros Schillings
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jamgu.hwstatistics.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import com.jamgu.hwstatistics.bluetooth.model.BluetoothData
import com.jamgu.hwstatistics.bluetooth.model.BondState
import com.jamgu.hwstatistics.bluetooth.model.LightBluetoothDevice
import com.jamgu.hwstatistics.bluetooth.model.ScanMode


internal class BluetoothInfoReader {

    init {
        update()
    }

    var data: BluetoothData? = null
        private set

    @SuppressLint("HardwareIds")
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