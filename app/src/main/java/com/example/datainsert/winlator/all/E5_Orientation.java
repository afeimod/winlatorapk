package com.example.datainsert.winlator.all;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.appcompat.app.AppCompatActivity;

import com.winlator.XServerDisplayActivity;
import com.winlator.renderer.GLRenderer;

public class E5_Orientation {
    private static final String PREF_KEY_XSA_ORIENT_LAND = "activity_orientation_landsc";

    public static boolean onClick(XServerDisplayActivity a) {
        boolean isBeforeLandScape = a.getRequestedOrientation() != SCREEN_ORIENTATION_SENSOR_PORTRAIT;
        setIsOrieLandFromPref(a, !isBeforeLandScape, true);
        return true;
    }

    @SuppressLint("SourceLockedOrientationActivity")
    public static void setIsOrieLandFromPref(XServerDisplayActivity a, boolean isLandSc, boolean updatePref) {
        if (updatePref)
            QH.getPreference(a).edit().putBoolean(PREF_KEY_XSA_ORIENT_LAND, isLandSc).apply();//更新存储设置
        a.setRequestedOrientation(isLandSc
                ? SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                : SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        //还是暴力延迟500ms然后切换两次全屏吧。反射修改viewportNeedsUpdate在初次启动时貌似没效果
        GLRenderer renderer = a.getXServer().getRenderer();
        renderer.xServerView.postDelayed(renderer::toggleFullscreen,500);
        renderer.xServerView.postDelayed(renderer::toggleFullscreen,800);
    }

    public static boolean getIsOrieLandFromPref(Context a) {
        return QH.getPreference(a).getBoolean(PREF_KEY_XSA_ORIENT_LAND, true);
    }
}
