package com.jupiter.snifferframework;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by wangqiang on 16/6/9.
 * 用于执行一条shell指令
 */
class CmdLineExecuter {

    public static final String Tag = "CmdLineExecuter";

    public static int exec(String[] cmds, boolean exit) {
        // TODO Auto-generated method stub
        Process process = null;
        DataOutputStream os = null;
        try {
            //1.启动su，以root权限运行必要的程序
            process = Runtime.getRuntime().exec("su");
            //2.向su输入命令,并读取结果
            os = new DataOutputStream(process.getOutputStream());
            for (String cmd : cmds) {
                Log.e(Tag, "run cmd:" + cmd);
                os.writeBytes(cmd + "\n");
                os.flush();
            }

            //3.如果需要退出su，则向su输入exit以便退出
            if (exit) {
                os.writeBytes("exit\n");
                os.flush();
            }

            //4.等待su进程结束
            int exitCode = process.waitFor();
            Log.e(Tag, "-->su exit at:" + exitCode);

            return exitCode;
        } catch (Exception e) {
            Log.e(Tag, "-->run su err:" + e.toString());
            return -1;
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                process.destroy();
            } catch (Exception e) {
            }
        }
    }
}
