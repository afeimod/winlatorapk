package com.example.datainsert.winlator.all;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.preference.PreferenceManager;

import com.winlator.XServerDisplayActivity;
import com.winlator.core.FileUtils;
import com.winlator.core.TarCompressorUtils;
import com.winlator.contentdialog.ContentDialog;
import com.winlator.xenvironment.ImageFs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Objects;

public class PulseAudio {
    //    private final static File logFile = new File(Globals.getAppContext().getExternalFilesDir(""), "logs/palog.txt");
//    private final static File paWorkDir = new File(Globals.getAppContext().getFilesDir(), "pulseaudio-xsdl");
    private static final String TAG = "PulseAudio";
    private static final String DEFAULT_LAUNCH_PARAMS =
            "./pulseaudio --start --exit-idle-time=-1 -n -F ./pulseaudio.conf --daemonize=true";
    public static String TITLE = QH.string.pulse声音选项;
    public static String PREF_KEY_PULSE_AUTO = "PULSE_AUTO";
    private final File paWorkDir;
    private final File logFile;
    private final XServerDisplayActivity a;

    public PulseAudio(XServerDisplayActivity a) {
        this.a = a;
        ImageFs imageFs = ImageFs.find(a);
        paWorkDir = new File(imageFs.getRootDir(), "opt/pulseaudio-xsdl");
        logFile = new File(paWorkDir, "log/log.txt");
    }

    public static boolean isAutoRun(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_KEY_PULSE_AUTO, false);
    }

    public void showDialog() {
        ContentDialog mDialog = new ContentDialog(a);
        mDialog.setTitle(TITLE);

        LinearLayout rootUI = new LinearLayout(a);
        rootUI.setOrientation(LinearLayout.VERTICAL);

        TextView tv = new TextView(a);
        tv.setText(QH.string.pulse声音简介);
        rootUI.addView(tv);

        LinearLayout linearBtnOnOff = new LinearLayout(a);
        linearBtnOnOff.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(0, -2);
        btnParams.weight = 1;
        Button btnOn = new Button(a, null, 0, QH.style.ButtonNeutral);
        btnOn.setText(QH.string.pulse按钮立即运行);
        btnOn.setOnClickListener(v -> {
            installAndExec(true);
            mDialog.dismiss();
        });
        linearBtnOnOff.addView(btnOn, btnParams);


        Button btnOff = new Button(a, null, 0, QH.style.ButtonNeutral);
        btnOff.setText(QH.string.pulse按钮立即停止);
        btnOff.setOnClickListener(v -> {
            installAndExec(false);
            mDialog.dismiss();
        });
        linearBtnOnOff.addView(btnOff, btnParams);
        rootUI.addView(linearBtnOnOff);

        Button btnNote = new Button(a, null, 0, QH.style.ButtonNeutral);
        btnNote.setText(QH.string.pulse按钮注意事项);
        btnNote.setOnClickListener(v -> showNoteDialog());
        rootUI.addView(btnNote);

        CheckBox checkAuto = new CheckBox(a);
        checkAuto.setText(QH.string.pulse选项自动运行);
        checkAuto.setChecked(isAutoRun(a));
        checkAuto.setOnCheckedChangeListener((buttonView, isChecked) -> QH.getPreference(a).edit().putBoolean(PREF_KEY_PULSE_AUTO, isChecked).apply());
        rootUI.addView(checkAuto);

        FrameLayout frameLayout = mDialog.findViewById(QH.id.FrameLayout);
        frameLayout.setVisibility(View.VISIBLE);
        ScrollView rootScrollView = new ScrollView(a);
        rootScrollView.addView(rootUI);
        frameLayout.addView(rootScrollView);

        mDialog.findViewById(QH.id.BTCancel).setVisibility(View.GONE);
        mDialog.findViewById(QH.id.BTConfirm).setVisibility(View.GONE);
        mDialog.show();

    }

    /**
     * 在pulseaudio的dialog中，点击”注意事项“按钮后显示的对话框。
     */
    @SuppressLint("SetTextI18n")
    private void showNoteDialog() {
//        new AlertDialog.Builder(a).setView();
        ContentDialog noteDialog = new ContentDialog(a);
        LinearLayout linearRoot = new LinearLayout(a);
        linearRoot.setOrientation(LinearLayout.VERTICAL);
        TextView tvNote = new TextView(a);
        tvNote.setText(QH.string.pulse注意事项文字);
        linearRoot.addView(tvNote);

        Button btnDelPulseDep = new Button(a, null, 0, QH.style.ButtonNeutral);
        btnDelPulseDep.setText(QH.string.pulse删除依赖库);
        btnDelPulseDep.setEnabled(paWorkDir.exists());
        File rootfsDir = ImageFs.find(a).getRootDir();
        btnDelPulseDep.setOnClickListener(v1 -> {
            noteDialog.dismiss();
            for(String filename: LibPulse0.fileNames)
                FileUtils.delete(new File(rootfsDir,filename));
        });
        linearRoot.addView(btnDelPulseDep);

        FrameLayout frameLayout = noteDialog.findViewById(QH.id.FrameLayout);
        frameLayout.setVisibility(View.VISIBLE);
        ScrollView scrollView = new ScrollView(a);
        scrollView.addView(linearRoot);
        frameLayout.addView(scrollView);
        noteDialog.findViewById(QH.id.BTCancel).setVisibility(View.GONE);
        QH.makeDialogContentScrollable(a,noteDialog);
        noteDialog.show();
    }

    public void installAndExec(boolean isRun) {
        //启动pulseaudio （貌似多次启动会导致失效，要么就启动一次，要么就先停止再启动）
//        //解压要求paDir不存在
//        if (paWorkDir.exists() && (!paWorkDir.isDirectory() || paWorkDir.list().length == 0) && !paWorkDir.delete())
//            return;


        new Thread(() -> {
            if (!new File(paWorkDir, "pulseaudio").exists()) {
                boolean b = paWorkDir.mkdirs();
                TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, a, "pulseaudio-xsdl.tar.zst", ImageFs.find(a).getRootDir());//解压必要文件
            }

            if (!new File(paWorkDir, "pulseaudio").exists()) {
                boolean b = paWorkDir.delete();
                return;
            }

            killOrStartPulseaudio(isRun);

        }, "thread: pulseaudio extract").start();


    }

    /**
     * 确保so文件存在，新建java进程 停止pulse。
     * 若勾选启用，则运行pulseaudio
     */
    private void killOrStartPulseaudio(boolean isRun) {
        Log.d(TAG, "startPulseaudio: 停止pulseaudio");

        try {
            //ProcessBulder可以设置环境变量，stdout err重定向等
            assert paWorkDir.exists();
            assert new File(paWorkDir, "pulseaudio").exists();
            String dir = paWorkDir.getAbsolutePath();

            ProcessBuilder builder = new ProcessBuilder(
                    "./pulseaudio",
                    "--kill"
            );

            builder.environment().put("HOME", dir);
            builder.environment().put("TMPDIR", dir);
            builder.environment().put("LD_LIBRARY_PATH", dir);
            builder.directory(paWorkDir);
            builder.redirectErrorStream(true);
            boolean b = Objects.requireNonNull(logFile.getParentFile()).mkdirs();
            builder.redirectOutput(logFile);
            long startTime = System.currentTimeMillis();

            builder.start().waitFor();
            Log.d(TAG, "startPulseaudio: 停止pulseaudio用了多长时间：" + (System.currentTimeMillis() - startTime));

            //删除残留.config文件夹和pulse-xxxx文件夹，防止pa_pid_file_create() failed.?
            for (File subFile : Objects.requireNonNull(paWorkDir.listFiles()))
                if (subFile.isDirectory() && subFile.getName().startsWith("pulse-"))
                    FileUtils.delete(subFile);
                else if (subFile.isDirectory() && subFile.getName().contains(".config")) {
                    //config不知道要不要删啊，留着daemon.conf 其他的删了吧
                    for (File subInConfig : Objects.requireNonNull(subFile.listFiles()))
                        if (!subInConfig.getName().equals("daemon.conf") && !subInConfig.getName().equals("daemon.conf.d"))
                            FileUtils.delete(subFile);
                }

            //如果设置不开启pulse，直接返回
            if (!isRun)
                return;

            //分割字符串，不能识别引号
            String[] splitParams = DEFAULT_LAUNCH_PARAMS.trim().split(" ");
            Log.d(TAG, "startPulseaudio: 启动pulseaudio，启动参数为：" + Arrays.toString(splitParams));

            builder.command(splitParams);
            builder.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile));
            builder.start();

        } catch (IOException | InterruptedException e) {
            try (PrintWriter printWriter = new PrintWriter(logFile);) {
                e.printStackTrace(printWriter);
            } catch (FileNotFoundException ignored) {

            }
            e.printStackTrace();
        }
    }

    private static class LibPulse0 {
        static String[] fileNames = new String[]{
                "opt/pulseaudio-xsdl",
                "usr/lib/aarch64-linux-gnu/libasyncns.so.0.3.1",
                "usr/lib/aarch64-linux-gnu/libasyncns.so.0",
                "usr/lib/aarch64-linux-gnu/libpulse-simple.so.0.1.1",
                "usr/lib/aarch64-linux-gnu/libpulse.so.0.21.2",
                "usr/lib/aarch64-linux-gnu/pulseaudio/libpulsecommon-13.99.so",
                "usr/lib/aarch64-linux-gnu/libpulse-simple.so.0",
                "usr/lib/aarch64-linux-gnu/libpulse.so.0",
                "usr/lib/aarch64-linux-gnu/libsndfile.so.1.0.28",
                "usr/lib/aarch64-linux-gnu/libsndfile.so.1",
                "usr/lib/aarch64-linux-gnu/libwrap.so.0.7.6",
                "usr/lib/aarch64-linux-gnu/libwrap.so.0",
                "usr/lib/arm-linux-gnueabihf/libasyncns.so.0.3.1",
                "usr/lib/arm-linux-gnueabihf/libasyncns.so.0",
                "usr/lib/arm-linux-gnueabihf/liblz4.so.1.9.2",
                "usr/lib/arm-linux-gnueabihf/liblz4.so.1",
                "usr/lib/arm-linux-gnueabihf/libpulse-simple.so.0.1.1",
                "usr/lib/arm-linux-gnueabihf/libpulse.so.0.21.2",
                "usr/lib/arm-linux-gnueabihf/pulseaudio/libpulsecommon-13.99.so",
                "usr/lib/arm-linux-gnueabihf/libpulse-simple.so.0",
                "usr/lib/arm-linux-gnueabihf/libpulse.so.0",
                "usr/lib/arm-linux-gnueabihf/libsndfile.so.1.0.28",
                "usr/lib/arm-linux-gnueabihf/libsndfile.so.1",
                "usr/lib/arm-linux-gnueabihf/libwrap.so.0.7.6",
                "usr/lib/arm-linux-gnueabihf/libwrap.so.0",
                "etc/pulse/client.conf",
                "lib/arm-linux-gnueabihf/libdbus-1.so.3.19.11",
                "lib/arm-linux-gnueabihf/libdbus-1.so.3",
                "lib/arm-linux-gnueabihf/libsystemd.so.0.28.0",
                "lib/arm-linux-gnueabihf/libsystemd.so.0"
        };
    }
}
