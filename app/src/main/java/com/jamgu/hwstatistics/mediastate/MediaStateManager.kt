package com.jamgu.hwstatistics.mediastate

import android.content.Context
import android.media.AudioManager

/**
 * Created by jamgu on 2021/10/15
 */
object MediaStateManager {

    private const val DEFAULT_MUSIC_STATE_NOT_FOUND = -1

    fun getMusicState(context: Context?): Int {
        val manager = context?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return DEFAULT_MUSIC_STATE_NOT_FOUND
        return if (manager.isMusicActive) 1 else 0
    }

    fun getMusicVolume(context: Context?): Int {
        val manager = context?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return DEFAULT_MUSIC_STATE_NOT_FOUND
        return manager.getStreamVolume(AudioManager.STREAM_MUSIC)
    }

}