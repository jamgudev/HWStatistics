package com.jamgu.hwstatistics.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.io.FileUtils;

public class RCommand {

    static String strReadContent = "";


    // 修改权限777 恢复权限444
    public static void setEnablePrivilege(File file, boolean bEnable){
        try {
            if(!file.exists()){
                throw new FileNotFoundException(file.getAbsolutePath());
            }
            if (bEnable){
                ShellUtils.execCommand("chmod 777 " + file.getAbsolutePath(), true);
            } else {
                ShellUtils.execCommand("chmod 444 " + file.getAbsolutePath(), true);
                ShellUtils.execCommand("chown system " + file.getAbsolutePath(), true);
            }
        }catch (Exception ep){
            ep.printStackTrace();
        }

    }

    public static String readFileContent(File file) throws IOException {
        if(!file.exists()){
            throw new FileNotFoundException(file.getAbsolutePath());
        }

        if (file.canRead()){
            strReadContent = FileUtils.readFileToString(file);
        }else {
            ShellUtils.CommandResult cmdResult =
                    ShellUtils.execCommand("cat " + file.getAbsolutePath(), true);
            strReadContent = cmdResult.successMsg;
        }
        return strReadContent;
    }

    public static String[] readFileContentAsLineArray(File path) throws IOException{
        return RCommand.readFileContent(path).trim().split("\n");
    }

    public static List readFileContentAsList(File path) throws IOException{
        return Arrays.asList(RCommand.readFileContentAsLineArray(path));
    }

    public static void writeFileContent(File file, String data) throws IOException{
        if(!file.exists()){
            throw new FileNotFoundException(file.getAbsolutePath());
        }

        if (file.canWrite()){
//            FileUtils.writeStringToFile(file, data);
        }else {
            ShellUtils.CommandResult cmdResult =
                    ShellUtils.execCommand("echo " + data + " > "  + file.getAbsolutePath(), true);
            strReadContent = cmdResult.successMsg;
        }

    }

}