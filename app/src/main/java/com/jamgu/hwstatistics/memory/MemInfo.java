package com.jamgu.hwstatistics.memory;

import android.app.ActivityManager;
import android.content.Context;
import android.os.SystemClock;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Formatter;

public class MemInfo {
    Context mContext;
    String mTime;
    int mMemFree;
    int mCached;
    int mMediaMemIdle;
    int mRealFree;

    int mAvaliMem;

    public MemInfo(Context context) {
        mContext = context;
        refreshCurrentMeminfo();
    }

    public void refreshCurrentMeminfo(){
        // mTime 获取开机后的时间
        mTime = getRunTime();

        // mMemFree 和 mCached 通过读取/proc/meminfo出来信息进行解释
        String meminfoPath = "/proc/meminfo";
        String sMemFreeLine = null;
        String sCachedLine = null;

        String[] toFindMeminfoSArr = new String[2];
        toFindMeminfoSArr[0] = "MemFree";
        toFindMeminfoSArr[1] = "Cached";

        String[] meminfoSArr = getMeminfoFromPathByKey(meminfoPath,toFindMeminfoSArr);
        if(meminfoSArr != null){
            for(int i=0 ; i< meminfoSArr.length; i++ ){
                meminfoSArr[i] = meminfoSArr[i].substring(meminfoSArr[i].indexOf(':')+1,meminfoSArr[i].indexOf('k')).trim();
            }
            sMemFreeLine = meminfoSArr[0];
            sCachedLine = meminfoSArr[1];
        }

        mMemFree = Integer.parseInt(sMemFreeLine)/1024;
        mCached = Integer.parseInt(sCachedLine)/1024;


        // mMediaMemIdle 通过读取/proc/media-mem出来信息进行解释
        String mediaMeminfoPath = "/proc/media-mem";
        String meidaMeminfoString = getMediaMeminfoFromPathByKey(mediaMeminfoPath,"Idle",2);//下一行
        if(meidaMeminfoString == null) {
            android.util.Log.d("linrunyu","meidaMeminfoString: null" );
            mMediaMemIdle = 0;
        }else {
            //Log.d("linrunyu","meidaMeminfoString: " + meidaMeminfoString);
            String[] mediaSArr = meidaMeminfoString.split("\\s+");
            if((mediaSArr!=null) && (mediaSArr.length > 5)) {
                mediaSArr[4] = mediaSArr[4].substring(0,mediaSArr[4].indexOf('M')).trim();
                mMediaMemIdle = Integer.parseInt(mediaSArr[4]);
            }
        }

        // mRealFree = mMemFree + mCached - mMediaMemIdle
        mRealFree = mMemFree + mCached - mMediaMemIdle;

        // mavaliMem
        mAvaliMem = getFreeMemorySize();


    }

    private String getRunTime() {
        long msec = SystemClock.uptimeMillis();
        long sec = msec / 1000;
        long min = sec / 60;
        long hr = min / 60;
        long sec_show = sec % 60;
        long min_show = min % 60;
        Formatter formatter = new Formatter();
        return formatter.format("%d:%02d:%02d",
                hr, min_show, sec_show).toString().toLowerCase();
    }

    public String[] getMeminfoFromPathByKey(String path ,String[] sKey){
        if((path == null) || (sKey == null)){
            return null;
        }

        int sKeyCount = sKey.length;
        String[] lineResultValue = new String[sKeyCount];
        String lineTemp = null;

        BufferedReader br = null;
        try {
            int findStringCount = 0;
            br = new BufferedReader(new FileReader(path), 8192);
            while  ((lineTemp = br.readLine()) != null) {
//                Log.i("lxt", "---" + line);
                for(int i=0; i<sKeyCount ; i++) {
                    if (lineTemp.startsWith(sKey[i])) {
                        lineResultValue[i] = lineTemp;
                        findStringCount++;
                        break;
                    }
                }
                if(sKeyCount == findStringCount){
                    break;
                }
            }
        } catch (IOException e){
            e.printStackTrace();
        }finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return lineResultValue;
    }

    public String getMediaMeminfoFromPathByKey(String path ,String sKey, int offset){
        if((path == null) || (sKey == null) || (offset < 0)){
            return null;
        }

        String lineResultValue = null;
        String lineTemp = null;

        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(path), 8192);
            while  ((lineTemp = br.readLine()) != null) {
//                Log.i("lxt", "---" + line);
                if (lineTemp.contains(sKey)) {
                    while (lineTemp!=null && (offset-- > 0)){
                        lineTemp = br.readLine();
                    }
                    lineResultValue = lineTemp;
                    break;
                }
            }
        } catch (IOException e){
            e.printStackTrace();
        }finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return lineResultValue;
    }

    public int getFreeMemorySize() {
        ActivityManager.MemoryInfo outInfo = new ActivityManager.MemoryInfo();
        ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        am.getMemoryInfo(outInfo);
        long avaliMem = outInfo.availMem;
        avaliMem = (avaliMem / 1024 / 1024 );
        return (int)avaliMem; // "MB"
    }

}