package com.example.datainsert.winlator.all;

import android.content.Context;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

class E3_Logcat {
    private static final String TAG = "Logcat";
    private static final String PREF_KEY_LOGCAT_ENABLE = "logcat_enable";
    //视图可能多次进入，但process只需要运行一次。所以static记录一个，如果发现此值不为null，则跳过进程创建。
    // 有个问题就是xserverActivity退回到MainActivity切换时，直接使用Runtime.exit然后新启动一个activity，导致这里变成null。
    private static Process runningProcess = null;

    public static void addItemToSettings(AppCompatActivity a, LinearLayout hostRoot) {
        CheckBox checkLogcat = new CheckBox(a);
        checkLogcat.setText(QH.string.logcat日志);
        checkLogcat.setChecked(isChecked(a));
        onCheckChange(a);
        checkLogcat.setOnCheckedChangeListener((v, isChecked) -> onCheckChange(v.getContext(), isChecked));

        LinearLayout linearLogcat = QH.wrapHelpBtnWithLinear(a, checkLogcat, QH.string.logcat日志说明);
        hostRoot.addView(linearLogcat, QH.lpLinear(-1, -2).top().to());
    }

    public static boolean isChecked(Context context) {
        return QH.getPreference(context).getBoolean(PREF_KEY_LOGCAT_ENABLE, false);
    }

    /**
     * 从pref中读取当前是否启用。其他同{@link #onCheckChange(Context, boolean)}
     */
    public static void onCheckChange(Context c) {
        handle(isChecked(c));
    }
    /**
     * 开关改变时调用。将新的值写入pref，并启动或停止logcat。
     * 若当前为开启且之前已经启动过，则跳过启动。
     */
    public static void onCheckChange(Context c, boolean enable) {
        QH.getPreference(c).edit().putBoolean(PREF_KEY_LOGCAT_ENABLE, enable).apply();
        handle(enable);
    }

    /**
     * 根据是否启用，执行对应操作。
     */
    private static void handle(boolean enabled) {

        try {
            File parentFile = new File(QH.Files.Download(), "Winlator/logcat");
            File outputFile = new File(parentFile, "output.txt");
            if (!parentFile.exists())
                parentFile.mkdirs();
            if (enabled) {
                if(runningProcess != null)
                    return;

                //启动logcat
                String cmd = "logcat" +
                        " -f " + outputFile.getAbsolutePath() +
//                        " -r 500" + //500kb换一个文件
//                        " --id=" + lastRunningTime +  //id不同时删掉文件重来
                        " *:V";// "*:S",
                runningProcess = Runtime.getRuntime().exec(cmd);
            } else {
                if(runningProcess != null) {
                    runningProcess.destroy();
                    runningProcess = null;
                }
                Runtime.getRuntime().exec("logcat -c"); // -c: clear(flush) the entire log and exit. 貌似这样，下次启动时就不会把缓存内容写到初始文件中？
                if(outputFile.exists())
                    outputFile.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
