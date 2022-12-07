package com.jamgu.hwstatistics.mobiledata.bluetooth

import com.jamgu.hwstatistics.mobiledata.bluetooth.model.BluetoothData

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