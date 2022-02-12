package com.jamgu.hwstatistics.sensors

import android.content.Context
import android.content.Context.SENSOR_SERVICE
import android.hardware.Sensor
import android.hardware.Sensor.TYPE_ALL
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log

/**
 * Created by jamgu on 2021/12/08
 */
object SensorsInfoManager {

    private const val TAG = "SensorsInfoManager"
    private var mSensorManager: SensorManager? = null

    private val mSampleRate = SensorManager.SENSOR_DELAY_NORMAL

    private val mSensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            when(event?.sensor?.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    Log.d(TAG, "type: ${event.sensor.type}. power = ${event.sensor.power}")
//                    event.values?.forEach {
//                        Log.d(TAG, "type: ${event.sensor.type}. value = $it")
//                    }
                }
                Sensor.TYPE_GYROSCOPE -> {
                    Log.d(TAG, "type: ${event.sensor.type}. power = ${event.sensor.power}")

//                    event.values?.forEach {
//                        Log.d(TAG, "type: ${event.sensor.type}. value = $it")
//                    }
                }
                Sensor.TYPE_LIGHT -> {
                    Log.d(TAG, "type: ${event.sensor.type}. power = ${event.sensor.power}")

//                    event.values?.forEach {
//                        Log.d(TAG, "type: ${event.sensor.type}. value = $it")
//                    }
                }
                else -> {
                    Log.d(TAG, "sensor: ${event?.sensor?.type} not found.")
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            Log.d(TAG, "onAccuracyChanged, sensor type:[${sensor?.type}], accuracy:[$accuracy]")
        }

    }

    /**
        TYPE_ACCELEROMETER	        加速度传感器，基于硬件
        TYPE_GRAVITY	            重力传感器，基于硬件或软件
        TYPE_GYROSCOPE	            陀螺仪传感器，基于硬件
        TYPE_ROTATION_VECTOR	    旋转矢量传感器，基于硬件或软件
        TYPE_LINEAR_ACCELERATION	线性加速度传感器，基于硬件或软件
        TYPE_MAGNETIC_FIELD	        磁力传感器，基于硬件
        TYPE_ORIENTATION	        方向传感器，基于软件
        TYPE_PROXIMITY	            距离传感器，基于硬件
        TYPE_LIGHT	                光线感应传感器，基于硬件
        TYPE_PRESSURE	            压力传感器，基于硬件
        TYPE_TEMPERATURE	        温度传感器，基于硬件
     */
    fun registerSensorListener(context: Context?) {
        context ?: return

        mSensorManager = context.getSystemService(SENSOR_SERVICE) as? SensorManager

        val defaultSensor = mSensorManager?.getSensorList(TYPE_ALL)
        defaultSensor?.forEach {
            Log.d(TAG, "all size: ${defaultSensor.size}, current: ${it.name}")
        }

//        mSensorManager?.let { sm ->
//            // 加速度传感器
//            sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let{
//                sm.registerListener(mSensorEventListener, it, mSampleRate)
//            }
//            // 陀螺仪传感器
//            sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE)?.let{
//                sm.registerListener(mSensorEventListener, it, mSampleRate)
//            }
//            // 光线传感器
//            sm.getDefaultSensor(Sensor.TYPE_LIGHT)?.let {
//                sm.registerListener(mSensorEventListener, it, mSampleRate)
//            }
//        }
    }

    fun unregisterSensorListener() {
        mSensorManager?.unregisterListener(mSensorEventListener)
        mSensorManager = null
    }

}