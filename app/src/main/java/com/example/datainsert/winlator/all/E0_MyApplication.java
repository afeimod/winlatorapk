package com.example.datainsert.winlator.all;

import static android.os.Environment.DIRECTORY_DOWNLOADS;
import static android.os.Environment.getExternalStoragePublicDirectory;

import android.app.Application;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

class E0_MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // 注册全局异常处理类
        String logFilePath = getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS) + "/Winlator/crash/crash-%s.log";
        Thread.setDefaultUncaughtExceptionHandler(new CrashExceptionHandler(logFilePath));

    }

    /**
     * 系统处理异常类，处理整个APP的异常
     */
    public static class CrashExceptionHandler implements Thread.UncaughtExceptionHandler{

        private final String mLogFilePath;


        public CrashExceptionHandler(String logFilePath){
            this.mLogFilePath = logFilePath;
        }


        @Override
        public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
            //写入文件
            File fl = new File(String.format(mLogFilePath, System.currentTimeMillis()));
            if(!fl.getParentFile().exists()) fl.mkdirs();

            try (StringWriter strWriter = new StringWriter();
                 PrintWriter writer = new PrintWriter(strWriter);
                 FileOutputStream fos = new FileOutputStream(fl)) {
                e.printStackTrace(writer);
                strWriter.flush();
                fos.write(strWriter.toString().getBytes());
            } catch (IOException e2) {
                e2.printStackTrace();
            }
            //退出程序
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);

        }

    }
}