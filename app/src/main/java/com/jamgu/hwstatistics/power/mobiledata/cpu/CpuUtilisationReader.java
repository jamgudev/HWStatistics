package com.jamgu.hwstatistics.power.mobiledata.cpu;

import android.util.Log;
import com.jamgu.hwstatistics.power.mobiledata.cpu.model.CpuData;
import com.jamgu.hwstatistics.power.mobiledata.cpu.model.CpuInfo;
import com.jamgu.hwstatistics.util.IOHelper;
import com.jamgu.hwstatistics.util.ShellUtils;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

// Taken http://stackoverflow.com/questions/7593829/how-to-get-the-processor-number-on-android
class CpuUtilisationReader {
    private static final String TAG = "CpuUtilisationReader";
    private static final String STAT_FILE = "/proc/stat";

    private RandomAccessFile statFile;
    private CpuInfo mCpuInfoTotal;
    private ArrayList<CpuInfo> mCpuInfoList;
    private final AtomicBoolean mFileOpenedOk = new AtomicBoolean();

    public CpuUtilisationReader() {
        update();
        mFileOpenedOk.set(false);
    }

    private void closeFile() throws IOException {
        if (statFile != null) {
            statFile.close();
        }
    }

    private void createCpuInfo(final int cpuId, final String[] parts) {
        if (cpuId == -1) {
            if (mCpuInfoTotal == null) {
                mCpuInfoTotal = new CpuInfo();
            }
            mCpuInfoTotal.update(parts);
        } else {
            if (mCpuInfoList == null) {
                mCpuInfoList = new ArrayList<>();
            }

            if (cpuId < mCpuInfoList.size()) {
                mCpuInfoList.get(cpuId).update(parts);
            } else {
                final CpuInfo info = new CpuInfo();
                info.update(parts);
                mCpuInfoList.add(info);
            }
        }
    }

    private void openFile() throws FileNotFoundException {
        statFile = new RandomAccessFile(STAT_FILE, "r");
        mFileOpenedOk.set(true);
    }

    public float getCpuUsage(final int cpuId) {
        float usage = 0;
        if (mCpuInfoList != null) {
            int cpuCount = mCpuInfoList.size();
            if (cpuCount > 0) {
                cpuCount--;
                if (cpuId == cpuCount) { // -1 total cpu usage
                    usage = mCpuInfoList.get(0).getUsage();
                } else {
                    if (cpuId <= cpuCount)
                        usage = mCpuInfoList.get(cpuId).getUsage();
                    else
                        usage = -1;
                }
            }
        }
        return usage;
    }

    private float getTotalCpuUsage() {
        float usage = 0;
        if (mCpuInfoTotal != null) {
            usage = mCpuInfoTotal.getUsage();
        }
        return usage;
    }

    private void parseCpuLine(final int cpuId, final String cpuLine) {
        if (cpuLine != null && cpuLine.length() > 0) {
            final String[] parts = cpuLine.split("[ ]+");
            final String cpuLabel = "cpu";

            if (parts[0].contains(cpuLabel)) {
                createCpuInfo(cpuId, parts);
            }
        } else {
//            Log.e(TAG, "unable to get cpu line");
        }
    }

    public CpuData getCpuInfo() {
        final CpuData retVal;

        if (mFileOpenedOk.get()) {
            final List<Float> cpuUtilisation = new ArrayList<>();

            if (mCpuInfoList != null) {
                for (int i = 0; i < mCpuInfoList.size(); i++) {
                    final CpuInfo info = mCpuInfoList.get(i);
                    cpuUtilisation.add(info.getUsage());
                }
            }

            retVal = new CpuData(STAT_FILE, getTotalCpuUsage(), cpuUtilisation);
        } else {
            retVal = new CpuData(STAT_FILE, true);
        }

        return retVal;
    }

    private void parseFile() {
        if (statFile != null) {
            try {
                statFile.seek(0);
                String cpuLine;
                int cpuId = -1;
                do {
                    cpuLine = statFile.readLine();
                    parseCpuLine(cpuId, cpuLine);
                    cpuId++;
                } while (cpuId <= 7);
            } catch (final IOException e) {
                Log.e(TAG, "Error parsing file: " + e);
            }
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        if (mFileOpenedOk.get()) {
            if (mCpuInfoTotal != null) {
                sb.append("Cpu Total : ");
                sb.append(mCpuInfoTotal.getUsage());
                sb.append("%");
            }
            if (mCpuInfoList != null) {
                for (int i = 0; i < mCpuInfoList.size(); i++) {
                    final CpuInfo info = mCpuInfoList.get(i);
                    sb.append(" Cpu Core(");
                    sb.append(i);
                    sb.append(") : ");
                    sb.append(info.getUsage());
                    sb.append("%");
                    info.getUsage();
                }
            }
        } else {
            sb.append("Error: Failed to open " + STAT_FILE);
        }
        return sb.toString();
    }

    /**
     * api 26，android 8.0 之前，/proc/stat 还是可以直接访问的
     * android 8.0之后，需要root权限，通过copy一份，读副本才可以拿到
     */
    public void update() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) {
            updateBeforeO();
        } /*else {  // 屏蔽 root 操作
            updateAfterO();
        }*/
    }

    private void updateBeforeO() {
        try {
            openFile();
            parseFile();
            closeFile();
        } catch (final FileNotFoundException e) {
            mFileOpenedOk.set(false);
            statFile = null;
            Log.e(TAG, "cannot open " + STAT_FILE + ":" + e);
        } catch (final IOException e) {
            Log.e(TAG, "cannot close " + STAT_FILE + ":" + e);
        }
    }

    private void updateAfterO() {
        boolean isRooted = ShellUtils.checkRootPermission();
        if (! isRooted) {
            return;
        }

        String cpuUtils = IOHelper.getCpuUtils();
        String[] cpuSplits = cpuUtils.split("\n");

        if (cpuSplits == null || cpuSplits.length < 9) {
            return;
        }

        mFileOpenedOk.set(true);

        String cpuLine;
        int cpuId = -1;
        do {
            cpuLine = cpuSplits[cpuId + 1];
            parseCpuLine(cpuId, cpuLine);
            cpuId++;
        } while (cpuId <= 7);

    }

}
