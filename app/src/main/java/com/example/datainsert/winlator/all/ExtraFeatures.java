package com.example.datainsert.winlator.all;

import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;

//TODO
// 1.wine等输出日志写到侧栏中？
// 2. logcat日志？
public class ExtraFeatures {
    public static class MyApplication extends E0_MyApplication {

    }

    public static class AndroidShortcut extends E1_ShortcutOnAndroidScreen {

    }

    public static class KeyInput extends E2_KeyInput {

    }

    public static class SettingsExtra {
        public static void addItems(AppCompatActivity a, FrameLayout hostRoot) {
            Logcat.addItems(a, hostRoot);
        }
    }

    public static class Logcat extends E3_Logcat{

    }

}
