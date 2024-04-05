package com.example.datainsert.winlator.all;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.Nullable;

public class ForeGroundService extends Service {
    private static final String TAG = "ForeGroundService";
    private boolean notified=false;
    Thread mThread ;
    /**
     * xserver activity的onCreate中调用
     */
    public static void start(Context c){
        try{
            Log.d(TAG, "start: 发送启动前台服务的intent");
            c.startService(new Intent(c, ForeGroundService.class));
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * mainactivity的oncreate中调用
     */
    @SuppressLint("BatteryLife")
    public static void stop(Context c){
        try {
            c.stopService(new Intent(c, ForeGroundService.class));
            //顺便检查一下电池优化
            if (!c.getSystemService(PowerManager.class).isIgnoringBatteryOptimizations(c.getPackageName())) {
                c.startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + c.getPackageName())));
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: 启动前台服务");
        if(!notified){
            configureAsForegroundService();
            notified=true;
        }
//        if(mThread==null){
//            mThread = new Thread(()->{
//                while(mThread.isInterrupted()){
//                    try {
//                        Thread.sleep(10000);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//            },"empty thread in foregroundservice");
//        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void configureAsForegroundService() {
        final int ONGOING_NOTIFICATION_ID = 13;
        final String CHANNEL_ID = getPackageName() + "winlator_notification_channel_id";
        Intent notificationIntent = new Intent(this, ForeGroundService.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new Notification.Builder(this, CHANNEL_ID).setContentTitle("Winlator").setContentText("固定通知，防止挂后台闪退").setSmallIcon(null).setContentIntent(pendingIntent).build();
        getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel(CHANNEL_ID, "Winlator", NotificationManager.IMPORTANCE_MIN));
        startForeground(ONGOING_NOTIFICATION_ID, notification);

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
