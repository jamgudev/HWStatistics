package com.jamgu.hwstatistics.bluetooth

import com.jamgu.hwstatistics.bluetooth.model.BluetoothData

/**
 * Created by jamgu on 2021/11/18
 */
object BluetoothManager {

    private val mBluetoothInfoReader = BluetoothInfoReader()

    fun getBluetoothData(): BluetoothData? {
        mBluetoothInfoReader.update()
        return mBluetoothInfoReader.data
    }

}