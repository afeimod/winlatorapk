package com.example.datainsert.winlator.all;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.SubMenu;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.winlator.R;
import com.winlator.XServerDisplayActivity;
import com.winlator.contentdialog.ContentDialog;
import com.winlator.core.EnvVars;
import com.winlator.renderer.GLRenderer;
import com.winlator.widget.TouchpadView;

public class XserverNavMenuControl {
    private static final String TAG = "XserverNavMenuControl";
    private static final String PREF_KEY_IS_GAME_STYLE_CURSOR = "IS_GAME_STYLE_CURSOR";
    public static boolean isGameStyleCursor = false;

    @SuppressLint("SourceLockedOrientationActivity")
    public static void addItems(XServerDisplayActivity a) {
        try {
//            QH.refreshIsTest(a);
            Log.d(TAG, "addItems: id为啥获取不到navigationview" + R.id.NavigationView);
            NavigationView navigationView = a.findViewById(R.id.NavigationView);
            DrawerLayout drawerLayout = a.findViewById(R.id.DrawerLayout);
            PulseAudio pulseAudio = new PulseAudio(a);

            SubMenu subMenu = navigationView.getMenu().addSubMenu(10, 132, 2, QH.string.额外功能);

            if (QH.versionCode <= 5) {
                subMenu.add(PulseAudio.TITLE).setOnMenuItemClickListener(item -> {
                    pulseAudio.showDialog();
                    drawerLayout.closeDrawers();
                    return true;
                });
            }

//            if(QH.isTest){
//                subMenu.add("测试spinner").setOnMenuItemClickListener(item->{
//                    AlertDialog dialog = new AlertDialog.Builder(a).setView(R.layout.container_detail_fragment).create();
//                    dialog.show();
//                    ContainerSettings.addOptionsTest(a,dialog.findViewById(R.id.SScreenSize).getRootView());
//                    return true;
//                });
//            }


            //记得根据默认设置进行初始化

            if (QH.versionCode <= 5) {
                //pulse自动启动
                if (PulseAudio.isAutoRun(a))
                    pulseAudio.installAndExec(true);
                //添加pulse环境变量
                Log.d(TAG, "addItems: 反射获取环境变量map");
                EnvVars envVars = QH.reflectGetField(XServerDisplayActivity.class, a, "envVars",  true);
                envVars.put("PULSE_SERVER", "tcp:127.0.0.1:4713");
            }

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static boolean getIsGameStyleCursorFromPref(Context a) {
        return QH.getPreference(a).getBoolean(PREF_KEY_IS_GAME_STYLE_CURSOR, false);
    }

    public static void setIsGameStyleCursor(Context a, boolean isGame, boolean updatePef) {
        if (updatePef)
            QH.getPreference(a).edit().putBoolean(PREF_KEY_IS_GAME_STYLE_CURSOR, isGame).apply();
        isGameStyleCursor = isGame;
    }
}
