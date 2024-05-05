package com.example.datainsert.winlator.all;

import android.app.PictureInPictureParams;
import android.util.Rational;
import android.view.Menu;
import android.view.SubMenu;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.winlator.R;
import com.winlator.XServerDisplayActivity;

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


    public static class Logcat extends E3_Logcat {
    }

    public static class PRootShell extends E4_PRootShell{

    }

    public static class Rotate extends E5_Orientation {

    }

    public static class ClickToMovePointer extends E6_ClickToMovePointer {

    }

    public static class PIP extends E7_PIP {

    }

    /**
     * 设置界面的额外功能
     */
    public static class SettingsExtra {
        public static void addItems(AppCompatActivity a, FrameLayout hostRoot) {
            LinearLayout myLinearRoot = hostRoot.findViewById(R.id.setting_linear_other_root);
            Logcat.addItemToSettings(a, myLinearRoot);
            PRootShell.addItemToSettings(a, myLinearRoot);

            if(QH.isTest) {
                Button btn = new Button(a);
                myLinearRoot.addView(btn, QH.lpLinear(-1, -2).top().to());
                btn.setOnClickListener(v-> PRootShell.showTerminalDialog(a));
            }

        }
    }

    /**
     * 启动容器后左侧菜单的额外功能
     */
    public static class XMenuExtra {
        public static void addItemsAndInit(XServerDisplayActivity a) {
            //添加选项。顺带初始化
            SubMenu menu = ((NavigationView) a.findViewById(R.id.NavigationView)).getMenu().addSubMenu(QH.string.额外功能);

            menu.add(QH.string.旋转屏幕选项).setOnMenuItemClickListener(item -> {
                ((DrawerLayout) a.findViewById(R.id.DrawerLayout)).closeDrawers();
                return Rotate.onClick(a);
            });

            menu.add(QH.string.proot终端).setOnMenuItemClickListener(item -> {
                ((DrawerLayout) a.findViewById(R.id.DrawerLayout)).closeDrawers();
                return PRootShell.showTerminalDialog(a);
            });

            menu.add(QH.string.画中画模式).setOnMenuItemClickListener(item -> {
                ((DrawerLayout) a.findViewById(R.id.DrawerLayout)).closeDrawers();
                return PIP.enterPIP(a);
            });

            //初次启动设置
            //设置旋转方向
            E5_Orientation.setIsOrieLandFromPref(a, E5_Orientation.getIsOrieLandFromPref(a), false);
            //绝对位置点击
            E6_ClickToMovePointer.setIsCurMoveRel(a, E6_ClickToMovePointer.getIsCurMoveRelFromPref(a), false);
        }
    }

}
