package com.example.datainsert.winlator.all;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.preference.PreferenceManager;

import com.winlator.MainActivity;
import com.winlator.R;
import com.winlator.core.AppUtils;
import com.winlator.core.TarCompressorUtils;
import com.winlator.xenvironment.ImageFs;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;

/**
 * 内置/obb目录下/手动选择/ 处理数据包
 */
public class OBBFinder {
    private static final String TAG = "OBBFinder";
//    static boolean shouldReturn = false; //标志extract是否应该停止睡眠并返回
//    static boolean isExtractSuccess = false;//标志返回时 是true还是false

    //https://developer.android.google.cn/training/basics/intents/result?hl=zh-cn
//    public static void registerSelectOBBResult(MainActivity activity) {
//        dialog = new OBBSelectDialog(activity);
//        dialog.registerSelectOBBResult();
//    }


    /**
     * 进入这里说明没找到文件，此时在主线程
     */
    public static void extract_3_2(MainActivity activity){
        QH.refreshIsTest(activity);

        //内置obb
        String assetFileName = findBundled(activity);
        if (assetFileName != null){
            int id = activity.getResources().getIdentifier("installing_obb_image","string",activity.getPackageName());
            activity.preloaderDialog.showOnUiThread(id);
            new Thread(()->{
                TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, activity, assetFileName, ImageFs.find(activity).getRootDir());
                afterExtractFinished(activity);
                activity.preloaderDialog.closeOnUiThread();
            },"解压数据包（从assets中）").start();
            return;
        }

        //手动选择

        //R.id.FLFragmentContainer 用add不用replace，这样containerFragment还是isVisible，按返回键直接退出app而不是回到container
        activity.runOnUiThread(() -> activity.getSupportFragmentManager().beginTransaction().add(QH.id.FLFragmentContainer, new OBBSelectFragment(),"OBBSelectFragment").commit());
    }

    /**
     * 原本OBBImageInstaller解压完会有一些善后操作。所以不管是在线下载，手选还是直接读取，都会调用installFromSource解压。这里就不调用了 模拟一遍全部操作
     */
    static void afterExtractFinished(MainActivity activity){
        final ImageFs imageFs = ImageFs.find(activity);
        PreferenceManager.getDefaultSharedPreferences(activity).edit()
                .remove("current_box86_version")
                .remove("current_box64_version")
                .apply();
        imageFs.createImgVersionFile(getIdealObbVersion(activity));
    }

    private static String findBundled(MainActivity c) {
        try {
            String[] list = c.getAssets().list("obb");
            if (list == null || list.length == 0)
                return null;

            String subName = list[0];
//            if (subName.matches("main\\.[0-9]+\\..+\\.obb"))
//                version = Integer.parseInt(subName.split("\\.", 3)[1]);
//            else {
//                QH.refreshIsTest(c);
//                version = QH.versionCode;
//            }
            return "obb/" + subName;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    static String getIdealObbName(Context context) {
        return "main." + getIdealObbVersion(context) + "." + context.getPackageName() + ".obb";
    }

    static int getIdealObbVersion (Context context){
        int foundObbVersion;
        try {
            String versionName = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
            foundObbVersion = Integer.parseInt(versionName.split("\\.",2)[0]);
        } catch (Exception e) {
            e.printStackTrace();
            foundObbVersion = 1;
        }
        return foundObbVersion;
    }

}
