package com.jamgu.hwstatistics

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import androidx.appcompat.app.AppCompatActivity
import com.jamgu.common.thread.ThreadPool
import com.jamgu.common.util.log.JLog
import com.jamgu.krouter.annotation.KRouter

/**
 * @author gujiaming.dev@bytedance.com
 * @date 2022/11/29 10:16 下午
 *
 * @description 开机自启，用于过渡的activity
 */
@KRouter(value = [TRANSITION_PAGE])
class TransitionActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "TransitionActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_one_pixel)

//        val window = window ?: return
//        window.setGravity(Gravity.START or Gravity.TOP)
//        window.attributes?.apply {
//            this.x = 0
//            this.y = 0
//            this.width = 1
//            this.height = 1
//        }

        ThreadPool.runUITask({
            val intent = Intent(this, AutoMonitorActivity::class.java)
            intent.putExtra(AUTO_MONITOR_START_FROM_NOTIFICATION, true)
            this@TransitionActivity.startActivity(intent)
            finish()
        }, 500)

        JLog.d(TAG, "onCreate")
    }

    override fun onDestroy() {
        super.onDestroy()
        JLog.d(TAG, "onDestroy")
    }
}