package com.jamgu.hwstatistics.util

import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStreamReader

/**
 * Created by jamgu on 2021/11/18
 */
object ShellUtils {

    private const val COMMAND_SU = "su"
    private const val COMMAND_SH = "su"
    private const val COMMAND_EXIT = "exit\n"
    private const val COMMAND_LINE_END = "\n"

    @JvmStatic
    fun checkRootPermission(): Boolean = execCommand("echo root", isRootCmd = true, isNeedResultMsg = false).result == 0

    /**
     * execute shell command, default return result msg
     *
     * @param command command
     * @param isRootCmd whether need to run with root
     * @return
     * @see ShellUtils.execCommand
     */
    @JvmStatic
    fun execCommand(command: String, isRootCmd: Boolean): CommandResult {
        return execCommand(arrayOf(command), isRootCmd, true)
    }

    /**
     * execute shell commands, default return result msg
     *
     * @param commands command list
     * @param isRootCmd whether need to run with root
     * @return
     * @see ShellUtils.execCommand
     */
    @JvmStatic
    fun execCommand(commands: List<String?>?, isRootCmd: Boolean): CommandResult? {
        return execCommand(commands, isRootCmd, true)
    }

    /**
     * execute shell commands, default return result msg
     *
     * @param commands command array
     * @param isRootCmd whether need to run with root
     * @return
     * @see ShellUtils.execCommand
     */
    @JvmStatic
    fun execCommand(commands: Array<String?>?, isRootCmd: Boolean): CommandResult {
        return execCommand(commands, isRootCmd, true)
    }

    /**
     * execute shell command
     *
     * @param command command
     * @param isRootCmd whether need to run with root
     * @param isNeedResultMsg whether need result msg
     * @return
     * @see ShellUtils.execCommand
     */
    @JvmStatic
    fun execCommand(command: String, isRootCmd: Boolean, isNeedResultMsg: Boolean): CommandResult {
        return execCommand(arrayOf(command), isRootCmd, isNeedResultMsg)
    }

    /**
     * execute shell commands
     *
     * @param commands command list
     * @param isRootCmd whether need to run with root
     * @param isNeedResultMsg whether need result msg
     * @return
     * @see ShellUtils.execCommand
     */
    @JvmStatic
    fun execCommand(commands: List<String?>?, isRootCmd: Boolean, isNeedResultMsg: Boolean): CommandResult? {
        return execCommand(commands, isRootCmd, isNeedResultMsg)
    }


    /**
     * execute shell commands
     *
     * @param commands command array
     * @param isRootCmd whether need to run with root
     * @param isNeedResultMsg whether need result msg
     * @return <ul>
     *         <li>if isNeedResultMsg is false, {@link CommandResult#successMsg} is null and
     *         {@link CommandResult#errorMsg} is null.</li>
     *         <li>if {@link CommandResult#result} is -1, there maybe some excepiton.</li>
     *         </ul>
     */
    @Suppress("UselessCallOnNotNull")
    @JvmStatic
    fun execCommand(
        commands: Array<String?>?,
        isRootCmd: Boolean = false,
        isNeedResultMsg: Boolean = false,
    ): CommandResult {
        var result = -1
        if (commands.isNullOrEmpty()) {
            return CommandResult(result)
        }

        val process: Process?
        var successResult: BufferedReader?
        var errorResult: BufferedReader?
        var successMsg: StringBuilder? = null
        var errorMsg: StringBuilder? = null

        val outputStream: DataOutputStream?

        try {
            process =
                Runtime.getRuntime().exec(if (isRootCmd) COMMAND_SU else COMMAND_SH) ?: return CommandResult(result)
            outputStream = DataOutputStream(process.outputStream)
            outputStream.use { os ->
                commands.forEach { cmd ->
                    if (cmd == null) return@forEach

                    // do not use os.writeBytes(commmand), avoid chinese charset error
                    os.write(cmd.toByteArray())
                    os.writeBytes(COMMAND_LINE_END)
                    os.flush()
                }
                os.writeBytes(COMMAND_EXIT)
                os.flush()

                result = process.waitFor()
                // get command result
                if (isNeedResultMsg) {
                    successMsg = StringBuilder()
                    errorMsg = StringBuilder()
                    successResult = BufferedReader(InputStreamReader(process.inputStream))
                    errorResult = BufferedReader(InputStreamReader(process.errorStream))

                    var sTemp: String?
                    successResult?.use { sr ->
                        sTemp = sr.readLine()
                        while (!sTemp.isNullOrEmpty()) {
                            successMsg?.append(sTemp)
                            successMsg?.append(COMMAND_LINE_END)
                            sTemp = sr.readLine()
                        }
                    }
                    errorResult?.use { er ->
                        sTemp = er.readLine()
                        while (!sTemp.isNullOrEmpty()) {
                            errorMsg?.append(sTemp)
                            errorMsg?.append(COMMAND_LINE_END)
                            sTemp = er.readLine()
                        }
                    }
                }
            }
        } catch (e: IOException) {
//            e.printStackTrace()
            return CommandResult(result, null, e.message.toString())
        }

        return CommandResult(
            result, successMsg?.toString(),
            errorMsg?.toString()
        )
    }

}

class CommandResult(val result: Int, val successMsg: String? = null, val errorMsg: String? = null) {

    constructor(result: Int) : this(result, null, null)

}