package com.jamgu.hwstatistics.keeplive.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.jamgu.hwstatistics.keeplive.forground.ForegroundNF;


public class CancelNotifyervice extends Service {
    private static final String TAG = "CancelNotifyervice";
    ForegroundNF _mForgroundNF;
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG,"onCreate--");
        _mForgroundNF = new ForegroundNF(this, CancelNotifyervice.class.getSimpleName()
        );
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG,"onStartCommand--");
        _mForgroundNF.startForegroundNotification();
        stopSelf();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG,"onDestroy--");
        super.onDestroy();
        _mForgroundNF.stopForegroundNotification();
    }
}

