package com.jamgu.hwstatistics.util;

import java.io.File;
import java.io.IOException;

public class IOHelper {

    // gpu当前频率
    // cpu 当前频率和工作模式
    public static String getGpuStatusContent(){
        String gpuCurStatusContent = "get gpu status error";
        try {
            String gpuCurFreq = getGpu3DCurFreq();
            gpuCurStatusContent = "GPU Frequency: " + gpuCurFreq;

        } catch (Exception ep) {
            ep.printStackTrace();
        }
        return gpuCurStatusContent;
    }

    // gpu 可用频率
    public static String[] getGpu3DAvailableFreq(){
        String[] gpu3DFreq = {"null"};
        try{
            gpu3DFreq = RCommand.readFileContent(new File("/sys/class/kgsl/kgsl-3d0/gpu_available_frequencies")).
                    replace("\n", "").split(" ");

        }catch (Exception ep){
            ep.printStackTrace();
        }
        return gpu3DFreq;
    }

    // gpu 当前最大频率
    public static String getGpu3DCruMaxFreq(){
        String gpuCurFreq = "";
        try{
            gpuCurFreq = RCommand.readFileContent(new File("/sys/class/kgsl/kgsl-3d0/max_gpuclk"));

        }catch (Exception ep){
            ep.printStackTrace();
        }
        return gpuCurFreq;
    }

    // gpu 当前频率
    public static String getGpu3DCurFreq(){
        String gpuCurFreq = "";
        try{
            gpuCurFreq = RCommand.readFileContent(new File("/sys/class/kgsl/kgsl-3d0/gpuclk"));

        }catch (Exception ep){
            ep.printStackTrace();
        }
        return gpuCurFreq;
    }

    // gpu 利用率
    public static String getGpu3DUtils() {
        String gpuUtil = "";
        try {
            gpuUtil = RCommand.readFileContent(new File("/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return gpuUtil;
    }

    public static void setGpu3DMaxFreq(String data){
        try {
            RCommand.setEnablePrivilege(new File("/sys/class/kgsl/kgsl-3d0/max_gpuclk"), true);
            RCommand.writeFileContent(new File("/sys/class/kgsl/kgsl-3d0/max_gpuclk"), data);
            RCommand.setEnablePrivilege(new File("/sys/class/kgsl/kgsl-3d0/max_gpuclk"), false);

        }catch (Exception ep){
            ep.printStackTrace();
        }
    }

    /**
     * @return 返回总cpu利用率，各cpu利用率的字符串
     */
    public static String getCpuUtils() {
        String cpuInfo = "";
        try {
            cpuInfo = RCommand.readFileContent(new File("/proc/stat"));
        }catch (Exception ep){
            ep.printStackTrace();
        }
        return cpuInfo;
    }

    // I/O Scheduler
    // /sys/block/mmcblk0/queue/scheduler
//    public static String[] getAvailableScheduler(){
//        String[] availableScheduler = {"null"};
//        try{
//            availableScheduler = RCommand.readFileContent(Constants.SCHEDULER).
//                    replace("\n", "").split(" ");
//
//        }catch (Exception ep){
//            ep.printStackTrace();
//        }
//        return availableScheduler;
//    }
//
//    public static String getCurScheduler(){
//        String curScheduler = "";
//        try{
//            String strAvailableScheduler = RCommand.readFileContent(Constants.SCHEDULER);
//            curScheduler = strAvailableScheduler.substring(strAvailableScheduler.indexOf("[") + 1,
//                    strAvailableScheduler.indexOf("]")).trim();
//
//        }catch (Exception ep){
//            ep.printStackTrace();
//        }
//        return curScheduler;
//    }
//
//    public static void setCurScheduler(String data){
//        try {
//            RCommand.setEnablePrivilege(Constants.SCHEDULER, true);
//            RCommand.writeFileContent(Constants.SCHEDULER, data);
//            RCommand.setEnablePrivilege(Constants.SCHEDULER, false);
//
//        }catch (Exception ep){
//            ep.printStackTrace();
//        }
//    }
//
//    // READ_AHEAD_KB
//    public static String getCurReadAhead(){
//        String curReadAhead = "";
//        try{
//            curReadAhead = RCommand.readFileContent(Constants.READ_AHEAD_KB).
//                    replace("\n", "") + " kb";
//
//        }catch (Exception ep){
//            ep.printStackTrace();
//        }
//        return curReadAhead;
//    }
//
//    public static void setCurReadAhead(String data){
//        try{
//            RCommand.setEnablePrivilege(Constants.READ_AHEAD_KB, true);
//            RCommand.writeFileContent(Constants.READ_AHEAD_KB, data);
//            RCommand.setEnablePrivilege(Constants.READ_AHEAD_KB, false);
//        }catch (Exception ep){
//            ep.printStackTrace();
//        }
//    }
//
//    // i/o scheduler status
//    public static String getIOSchedulerStatusContent(){
//        String ioSchedulerStatus = "get i/o scheduler status error";
//        try {
//            String curIOScheduler = getCurScheduler();
//            String curReadAhead = getCurReadAhead();
//            ioSchedulerStatus = "I/O Scheduler: " + curIOScheduler + "\n" +
//                    "Read Ahead: " + curReadAhead;
//
//        } catch (Exception ep) {
//            ep.printStackTrace();
//        }
//        return ioSchedulerStatus;
//    }
}