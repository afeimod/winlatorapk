package com.example.datainsert.winlator.all;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SubMenu;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.winlator.XServerDisplayActivity;
import com.winlator.contentdialog.ContentDialog;
import com.winlator.core.EnvVars;
import com.winlator.renderer.GLRenderer;
import com.winlator.widget.TouchpadView;

import java.lang.reflect.Field;

public class XserverNavMenuControl {
    private static final String TAG = "XserverNavMenuControl";
    private static final String PREF_KEY_XSA_ORIENT_LAND = "activity_orientation_landsc";
    private static final String PREF_KEY_IS_GAME_STYLE_CURSOR = "IS_GAME_STYLE_CURSOR";
    private static final String PREF_KEY_IS_CUR_MOVE_REL = "IS_CUR_MOVE_REL";
    public static boolean isGameStyleCursor = false;

    @SuppressLint("SourceLockedOrientationActivity")
    public static void addItems(XServerDisplayActivity a) {
        try {
            QH.refreshIsTest(a);
            Log.d(TAG, "addItems: id为啥获取不到navigationview" + QH.id.NavigationView);
            NavigationView navigationView = a.findViewById(QH.id.NavigationView);
            DrawerLayout drawerLayout = a.findViewById(QH.id.DrawerLayout);
            PulseAudio pulseAudio = new PulseAudio(a);

            SubMenu subMenu = navigationView.getMenu().addSubMenu(10, 132, 2, QH.string.额外功能);

            if (QH.versionCode <= 5) {
                subMenu.add(PulseAudio.TITLE).setOnMenuItemClickListener(item -> {
                    pulseAudio.showDialog();
                    drawerLayout.closeDrawers();
                    return true;
                });
            }

            subMenu.add(QH.string.旋转屏幕选项).setOnMenuItemClickListener(item -> {
                boolean isBeforeLandScape = a.getRequestedOrientation() != SCREEN_ORIENTATION_SENSOR_PORTRAIT;
                setIsOrieLandFromPref(a, !isBeforeLandScape, true);
                drawerLayout.closeDrawers();
                return true;
            });


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
                EnvVars envVars = (EnvVars) QH.reflectGetFieldInst(XServerDisplayActivity.class, a, "envVars", true);
                envVars.put("PULSE_SERVER", "tcp:127.0.0.1:4713");
            }

            //设置旋转方向
            setIsOrieLandFromPref(a, getIsOrieLandFromPref(a), false);

            //光标样式
            setIsGameStyleCursor(a, getIsGameStyleCursorFromPref(a), false);

            //绝对位置点击
            setIsCurMoveRel(a, getIsCurMoveRelFromPref(a), false);

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static boolean getIsOrieLandFromPref(Context a) {
        return QH.getPreference(a).getBoolean(PREF_KEY_XSA_ORIENT_LAND, true);
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

    public static boolean getIsGameStyleCursorFromPref(Context a) {
        return QH.getPreference(a).getBoolean(PREF_KEY_IS_GAME_STYLE_CURSOR, false);
    }

    public static void setIsGameStyleCursor(Context a, boolean isGame, boolean updatePef) {
        if (updatePef)
            QH.getPreference(a).edit().putBoolean(PREF_KEY_IS_GAME_STYLE_CURSOR, isGame).apply();
        isGameStyleCursor = isGame;
    }

    public static boolean getIsCurMoveRelFromPref(Context a) {
        return QH.getPreference(a).getBoolean(PREF_KEY_IS_CUR_MOVE_REL, true);
    }

    public static void setIsCurMoveRel(Context a, boolean isRel, boolean updatePref) {
        if (updatePref)
            QH.getPreference(a).edit().putBoolean(PREF_KEY_IS_CUR_MOVE_REL, isRel).apply();
        try {
            TouchpadView.isRelativeOnStart = isRel;
            //如果是绝对位置，设置成拉伸全屏，即glrender.fullscreen设置为true
//            if (!isRel && a instanceof XServerDisplayActivity) {
//                XServerView xServerView = (XServerView) QH.reflectGetFieldInst(XServerDisplayActivity.class,a,"xServerView",true);
//                Boolean isFullScreen = (Boolean) QH.reflectGetFieldInst(GLRenderer.class,xServerView.getRenderer(),"fullscreen",true);
//                if(Boolean.FALSE.equals(isFullScreen))
//                    xServerView.getRenderer().toggleFullscreen();
//            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    public static void addInputControlsItems(XServerDisplayActivity a, ContentDialog dialog) {
        QH.refreshIsTest(a);
        try {
            LinearLayout linearRoot = (LinearLayout) dialog.findViewById(QH.id.CBLockCursor).getParent();

            CheckBox checkGameStyleCursor = new CheckBox(a);
            checkGameStyleCursor.setText(QH.string.游戏样式光标选项);
            checkGameStyleCursor.setChecked(getIsGameStyleCursorFromPref(a));
            checkGameStyleCursor.setOnCheckedChangeListener((buttonView, isChecked) -> setIsGameStyleCursor(a, isChecked, true));
            LinearLayout linearStyle = QH.wrapHelpBtnWithLinear(a, checkGameStyleCursor, QH.string.游戏样式光标选项说明);
//            linearRoot.addView(linearStyle);

            try {
                boolean testHasFeature = TouchpadView.isRelativeOnStart;
                CheckBox checkBox2 = new CheckBox(a);
                checkBox2.setText(QH.string.绝对位置点击选项);
                checkBox2.setChecked(!getIsCurMoveRelFromPref(a));
                checkBox2.setOnCheckedChangeListener((compoundButton, isChecked) -> setIsCurMoveRel(a, !isChecked, true));
                setIsCurMoveRel(a, getIsCurMoveRelFromPref(a), false);
                linearRoot.addView(QH.wrapHelpBtnWithLinear(a, checkBox2, QH.string.绝对位置点击选项说明));

            } catch (Throwable throwable) {
                Log.d(TAG, "addInputControlsItems: 未修改TouchpadView，不添加相对位置点击选项");
            }


            //内容太多，设置为可滚动
            QH.makeDialogContentScrollable(a, dialog);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
