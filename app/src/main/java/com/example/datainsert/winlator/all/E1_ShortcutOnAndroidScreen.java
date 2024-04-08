package com.example.datainsert.winlator.all;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.util.Log;
import android.widget.Toast;

import com.winlator.MainActivity;
import com.winlator.R;
import com.winlator.ShortcutsFragment;
import com.winlator.container.Container;
import com.winlator.container.ContainerManager;
import com.winlator.container.Shortcut;

import java.io.File;
import java.util.List;
import java.util.Objects;

class E1_ShortcutOnAndroidScreen {
    //包名前缀动态添加吧
    private static final String DESKTOP_FILE_ABSOLUTE_PATH = ".desktop_file_absolute_path";
    private static final String CONTAINER_ID = ".container_id";
    public static final String SHOULD_SHOW_TIP = "should_show_tip";
    /**
     * 插入位置：ShortcutsFragment.ShortcutsAdapter.showListItemMenu
     * 点击添加到屏幕选项后的操作
     */
    public static void addToScreen(Activity a, Shortcut shortcut){
//        throw new RuntimeException("测试");
        //调起初始acitivity的intent，并添加额外参数
        Intent intent = new Intent(a, MainActivity.class);
        //extra好像要带包名前缀
        intent.setAction(Intent.ACTION_MAIN);
        intent.putExtra(a.getPackageName() + DESKTOP_FILE_ABSOLUTE_PATH, shortcut.file.getAbsolutePath());
        intent.putExtra(a.getPackageName() + CONTAINER_ID, shortcut.container.id);

        //构建shortcutinfo，设置intent
        ShortcutInfo shortcutInfo = new ShortcutInfo.Builder(a, shortcut.name)
                .setShortLabel(shortcut.name)
                .setIntent(intent) //设置intent又不一定非要指向目标activity，那难道会加到栈中？如果不指定
                .setActivity(new ComponentName(a, MainActivity.class.getName())) //设置目标activity
                .build();

        //使用旧版shortcutmanager，设置动态快捷方式
        ShortcutManager shortcutManager = a.getSystemService(ShortcutManager.class);
        List<ShortcutInfo> shortcutInfoList = shortcutManager.getDynamicShortcuts();
        shortcutInfoList.add(shortcutInfo);
        //动态+静态快捷方式上限好像是4个，再添加会失败了 shortcutManager.getMaxShortcutCountPerActivity()返回的是15
        if (shortcutInfoList.size() > 4)
            shortcutInfoList.remove(0);

        //设置图标，由于get到的信息会丢失图标，所以每次设置快捷方式前需要重新设置一遍
        setDynamicShortcuts(a, shortcutInfoList);
        Toast.makeText(a, a.getString(R.string.screen_shortcut_add_finish), Toast.LENGTH_LONG).show();
    }

    /**
     * 插入位置：MainActivity.onCreate()结尾。
     * 应用启动后，选择正常显示是直接启动游戏（即使直接启动也不会直接结束onCreate，后面的都走一遍吧应该没问题）
     * @param a activity
     */
    public static void handleIfStartFromScreenShortcut(MainActivity a) {
        setDynamicShortcuts(a, null);

        Intent intent = a.getIntent();
        String shortcutPath = intent.getStringExtra(a.getPackageName() + DESKTOP_FILE_ABSOLUTE_PATH);
        if(shortcutPath!=null){
            int contId = intent.getIntExtra(a.getPackageName() + CONTAINER_ID, 0);
            Container container = new ContainerManager(a).getContainerById(contId);
            File desktopFile = new File(shortcutPath);
            if(container !=null && desktopFile.exists()){
                Log.d("MainActivity", "launchFromShortCut: 从快捷方式入口直接进入xserver");
                Shortcut shortcut = new Shortcut(container, desktopFile);
                ShortcutsFragment.runFromShortcut(a, shortcut);
                return;
            }
            Toast.makeText(a, a.getString(R.string.screen_shortcut_file_not_exist), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 添加动态快捷方式。同时会检查就的快捷方式是否失效，失效则删除
     *
     * @param list 新增的动态快捷方式列表，为null的话则获取当前的并检查是否有应该删除的
     */
    private static void setDynamicShortcuts(Activity a, List<ShortcutInfo> list) {

        ShortcutManager shortcutManager = a.getSystemService(ShortcutManager.class);
        final List<ShortcutInfo> nonNullList = list != null ? list : shortcutManager.getDynamicShortcuts();
        for (int i = 0; i < nonNullList.size(); i++) {
            ShortcutInfo info = nonNullList.get(i);
            Intent intent = Objects.requireNonNull(info.getIntent());
            File desktopFile;
            String filePath = intent.getStringExtra(a.getPackageName() + DESKTOP_FILE_ABSOLUTE_PATH);
            if (filePath==null || !(desktopFile = new File(filePath)).exists()) {
                nonNullList.remove(i);
                i--;
            }
            //通过getDynamicShortcuts获取的信息丢失了图标，需要重新设置
            else {
                ContainerManager manager = new ContainerManager(a);
                Container container = manager.getContainerById(intent.getIntExtra(a.getPackageName() + CONTAINER_ID, 0));
                Shortcut shortcut = new Shortcut(container, desktopFile);
                //构建shortcutinfo
                ShortcutInfo.Builder builder = new ShortcutInfo.Builder(a, shortcut.name)
                        .setShortLabel(shortcut.name)
                        .setIntent(info.getIntent()) //设置intent又不一定非要指向目标activity，那难道会加到栈中？如果不指定
                        .setActivity(info.getActivity()) //设置目标activity
                        ;
                if (shortcut.icon != null)
                    builder.setIcon(Icon.createWithBitmap(shortcut.icon));

                nonNullList.remove(i);
                nonNullList.add(i, builder.build());
            }
        }
        shortcutManager.setDynamicShortcuts(nonNullList);
    }
}