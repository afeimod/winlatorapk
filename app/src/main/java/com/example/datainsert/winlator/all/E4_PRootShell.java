package com.example.datainsert.winlator.all;

import static android.view.View.FOCUS_DOWN;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.example.datainsert.winlator.all.QH.dp8;

import android.content.Context;
import android.graphics.Point;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.UiThread;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;

import com.example.datainsert.winlator.all.widget.SimpleTextWatcher;
import com.winlator.R;
import com.winlator.core.Callback;
import com.winlator.core.ProcessHelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * ProcessHelper.exec最长的那个，里面有输出日志到文件的判断标准。两个flag都开启应该就行了。可以参考一下
 */
public class E4_PRootShell {
    private static final String TAG = "PRootTerminal";
    public static final int MAX_LINE = 1600; //允许缓存和显示的最大行数
    public static final int REDUCED_FRAGMENT = 800; //达到最大缓存时，删掉的行数
    static Handler handler = new Handler(Looper.getMainLooper());
    private static final String PREF_KEY_PROOT_TERMINAL_ENABLE = "proot_terminal_enable";
    private static final String PREF_KEY_PROOT_TERMINAL_TEXT_SIZE = "proot_terminal_text_size";
    private static final String PREF_KEY_PROOT_TERMINAL_AUTO_SCROLL = "proot_terminal_auto_scroll";
    //获取输出流的线程。进程结束时应置为null
    private static OutputThread outputThread;
    private static Process runningProcess;
    private static int pid = -1;
    private static boolean isAutoScroll = true; //是否自动滚动到底部


    /**
     * 主界面 - 设置中 添加选项。
     */
    public static void addItemToSettings(AppCompatActivity a, LinearLayout hostRoot) {
        CheckBox checkPRoot = new CheckBox(a);
        checkPRoot.setText(QH.string.proot终端);
        checkPRoot.setChecked(isChecked(a));
        checkPRoot.setOnCheckedChangeListener((v, isChecked) -> QH.getPreference(v.getContext())
                .edit().putBoolean(PREF_KEY_PROOT_TERMINAL_ENABLE, isChecked).apply());

        LinearLayout linearPRoot = QH.wrapHelpBtnWithLinear(a, checkPRoot, QH.string.proot终端说明);
        hostRoot.addView(linearPRoot, QH.lpLinear(-1, -2).top().to());
    }

    public static boolean isChecked(Context context) {
        return QH.getPreference(context).getBoolean(PREF_KEY_PROOT_TERMINAL_ENABLE, false);
    }

    /**
     * 在GuestProgramLauncherComponent.execGuestProgram()中，启动proot时，ProcessHelper.exec替换为此函数
     * <br/> 如果未开启终端，则和原来一样处理(调用ProcessHelper.exec)。否则自己创建process并重定向输入输出流
     * <br/> envp只包含PROOT_TMP_DIR，PROOT_LOADER和PROOT_LOADER_32 三个环境变量。其他环境变量写在command中
     * <br/> 将command分为三部分：
     * 1. proot.so及其设置参数：从开头到 /usr/bin/env 之前
     * 2. linux命令前半部分，设置环境变量：从/usr/bin/env到box64之前
     * 3. linux命令后半部分, 启动wine：从box64到最后
     * <br/> 创建process时，需要将命令按空格把每个参数分隔开，形成数组。由于终端需要能够输入命令，所以需要稍微改一下启动命令。
     * 注意，后续命令通过process.getOutputStream().write()和.flush()传入，注意要在末尾加“\n"否则不会被执行。
     * 第一部分+第二部分+ " /usr/bin/dash", 在给定环境变量下启动shell，保证后续可以继续输入命令。
     * 第三部分 + " &\n" &保证shell可以处理后续输入，注意 & 放在换行前
     */
    public static int exec(Context c, String command, String[] envp, File workingDir, Callback<Integer> terminationCallback) {
        if(!isChecked(c)) {
            Log.d(TAG, "exec: 启动proot。运行的命令为："+ command);
            return ProcessHelper.exec(command, envp, workingDir, terminationCallback);
        }

        pid = -1;
        try {
            int box64CmdIdx = command.indexOf(" box64 ");
            //初始命令，启动proot，设置环境变量，并启动shell（dash）
            String initialCmd = command.substring(0, box64CmdIdx);
            List<String> cmdList = new ArrayList<>(Arrays.asList(ProcessHelper.splitCommand(initialCmd)));
            cmdList.add("/usr/bin/dash");
            Log.d(TAG, "exec: 启动proot。运行的命令为："+ cmdList);

            ProcessBuilder builder = new ProcessBuilder()
                    .command(cmdList.toArray(new String[0])) //命令行
                    .directory(workingDir) //工作目录
                    .redirectErrorStream(true); //合并错误和输出流
            Map<String, String> envMap = builder.environment();
            //XServerDisplayActivity.setupXEnvironment中，会将WINEDEBUG设置为-all。需要手动在容器设置里添加环境变量 WINEDEBUG=err+all,fixme+all
            for(String oneEnv: envp) {
                int idxSplit = oneEnv.indexOf('=');
                envMap.put(oneEnv.substring(0, idxSplit), oneEnv.substring(idxSplit+1));
            }

            runningProcess = builder.start();
            //反射时，类要用实例.getClass()获取，而不能直接传入抽象类Process
            pid = QH.reflectGetFieldInt(runningProcess.getClass(), runningProcess, "pid", true);
            Log.d(TAG, "exec: 获取到的pid="+pid);

            //获取输出流
            outputThread = new OutputThread(runningProcess.getInputStream());
            outputThread.start();

            //启动dash成功后，再输入命令启动box64
            sendInputToProcess(command.substring(box64CmdIdx) + " &\n");

            //ProcessHelper.createWaitForThread。在进程结束时调用callback，并清空成员变量的值
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    int status = runningProcess.waitFor();
                    Log.d(TAG, "exec: proot进程停止运行。");
                    runningProcess = null;
                    pid = -1;
                    outputThread = null; //停止时将变量置为null
                    if(terminationCallback!=null) terminationCallback.call(status);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            QH.showFatalErrorDialog(c, QH.string.proot终端_启动失败_请关闭选项重试);
        }
        return pid;
    }

    /**
     * 向进程中输入命令。若命令末尾不带换行则会补上。
     */
    private static void sendInputToProcess(String command) {
        if(runningProcess == null || !runningProcess.isAlive())
            return;
        String finalCmd = command.endsWith("\n") ? command : (command + '\n');
        try {
            Log.d(TAG, "inputToProcess: 向进程中输入命令："+finalCmd);
            runningProcess.getOutputStream().write(finalCmd.getBytes());
            runningProcess.getOutputStream().flush();
            if(outputThread != null && outputThread.isAlive()) outputThread.onNewLine(finalCmd);
        } catch (IOException e) {
            e.printStackTrace();
        }
        ;
    }

    /**
     * 容器启动后，左侧栏点击选项后显示视图
     */
    public static boolean showTerminalDialog(AppCompatActivity a) {
        ViewGroup root = (ViewGroup) a.getLayoutInflater().inflate(R.layout.zzz_proot_teriminal_dialog, null, false);

        Point point = new Point();
        a.getWindowManager().getDefaultDisplay().getSize(point);
        int viewWidth = point.x/2 < QH.px(a,400) ? (int) (point.x * 0.9f) : point.x/2;
        root.setLayoutParams(new ViewGroup.LayoutParams(viewWidth, -1));

        //输入命令
        EditText editText = root.findViewById(R.id.edittext_input);
        editText.addTextChangedListener((SimpleTextWatcher) s -> {
            //如果为空不做任何处理
            if(s.length() == 0) return;
            //如果最后一个输入的字符为回车，则发送命令并清空输入
            if(s.charAt(s.length()-1) == '\n') {
                sendInputToProcess(s.toString());
                editText.setText("");
            }
        });

        DisplayCallback displayCallback;
        //文字输出
        TextView tvOutput = root.findViewById(R.id.terminal_text);
        tvOutput.setTextSize(TypedValue.COMPLEX_UNIT_PX, QH.getPreference(a).getFloat(PREF_KEY_PROOT_TERMINAL_TEXT_SIZE, dp8*7/4f));
        if(outputThread ==null || !outputThread.isAlive()) {
            tvOutput.setText(QH.string.proot终端_请先开启选项);
            displayCallback = null;
//            runTestThread((displayCallback = new DisplayCallback(tvOutput)), tvOutput);
        } else {
            displayCallback = new DisplayCallback(tvOutput);
            outputThread.setDisplayCallback(displayCallback);
        }

        //菜单显隐
        View groupMenu = root.findViewById(R.id.group_menu_items);
        groupMenu.setVisibility(GONE);
        root.findViewById(R.id.btn_menu).setOnClickListener( v2-> groupMenu.setVisibility(
                groupMenu.getVisibility() == VISIBLE ? GONE : VISIBLE));

        //ctrl+c
//                    android.os.Process.sendSignal(pid, SIGKILL);
//                    sendInputToProcess("\003");
//                    Os.kill(pid, SIGINT);

        //自动滚动到底部
        isAutoScroll = QH.getPreference(a).getBoolean(PREF_KEY_PROOT_TERMINAL_AUTO_SCROLL, true);
        root.findViewById(R.id.btn_auto_scroll).setOnClickListener(v -> {
//            if(displayCallback == null) return;
//            boolean newValue = !displayCallback.isAutoScroll();
//            displayCallback.setAutoScroll(newValue);
            isAutoScroll = !isAutoScroll;
            QH.getPreference(v.getContext()).edit().putBoolean(PREF_KEY_PROOT_TERMINAL_AUTO_SCROLL, isAutoScroll).apply();
        });
        root.findViewById(R.id.btn_auto_scroll).setTooltipText(QH.string.proot终端_自动滚动到底部);

        //帮助
        root.findViewById(R.id.btn_help).setOnClickListener(v -> QH
                .showConfirmDialog(v.getContext(), QH.string.proot终端说明, null));

        //文字放大缩小，一次变2dp
        root.findViewById(R.id.btn_text_size_up).setOnClickListener(v -> changeTextSize(tvOutput, true));
        root.findViewById(R.id.btn_text_size_down).setOnClickListener(v -> changeTextSize(tvOutput, false));

        AlertDialog dialog = new AlertDialog.Builder(a)
                .setView(root)
                .setCancelable(false)
                .setOnDismissListener(dialog1 -> {
                    if(outputThread != null)
                        outputThread.clearDisplayCallback();
                })
                .create();

        //关闭
        root.findViewById(R.id.btn_close).setOnClickListener(v-> dialog.dismiss());

        dialog.show();
        // 在show之前修改window宽高不生效。在show之后修改，外部轮廓生效，但内部视图还不行
        // 原因：即使传入的自定义视图高度设成match，但其父视图默认是wrap。只好给自定义视图里加一个撑满高度的空白view了
        WindowManager.LayoutParams attr = dialog.getWindow().getAttributes();
        attr.width = viewWidth;
        attr.height = -1;
        attr.gravity = Gravity.START;
        dialog.getWindow().setAttributes(attr);

        return true;
    }

    private static void changeTextSize(TextView tv, boolean up) {
        float currSize = tv.getTextSize();
        float newSize = currSize + dp8/4f * (up ? 1 : -1);
        float limitSize = Math.max(Math.min(newSize, dp8*5), dp8);
        //不指定单位设置的是dp？？离谱
        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, limitSize);
        QH.getPreference(tv.getContext()).edit().putFloat(PREF_KEY_PROOT_TERMINAL_TEXT_SIZE, limitSize).apply();
    }

    private static void runTestThread(DisplayCallback callback, TextView tvOutput){
        new Thread(() -> {
            while (true) {
                long time = (long) (Math.random() * 200);
                try {
                    Thread.sleep(time);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                tvOutput.post(() -> callback.call(""+time));
            }
        }).start();
    }

    private static class OutputThread extends Thread{
        private List<String> allLines = new ArrayList<>(); //存储历史文本行
        private DisplayCallback displayCallback = null; //用于将文本显示到屏幕上的回调。其内容应该在主线程上执行
        private InputStream inputStream; //输出内容流
        public OutputThread (InputStream inputStream) {
            this.inputStream = inputStream;
        }
        /**
         * 用于设置显示到屏幕上的callback。
         * 在显示终端视图时，设置此callback，立刻显示全部历史文本行。之后的输出流内容更新时调用回调，因此终端视图会同步更新
         */
        public void setDisplayCallback(DisplayCallback callback) {
            displayCallback = callback;
            handler.post(() -> {
                for(String line : allLines)
                    displayCallback.call(line);
            });
        }

        /**
         * 视图关闭时，应该清除回调
         */
        public void clearDisplayCallback() {
            if(displayCallback != null)
                displayCallback.tv = null;
            displayCallback = null;
        }
        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;

                //TODO 写入文件。先不管了？
//                    File winlatorDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "Winlator");
//                    winlatorDir.mkdirs();
//                    final File debugFile = new File(winlatorDir, isError ? "debug-err.txt" : "debug-out.txt");
//                    if (debugFile.isFile()) debugFile.delete();
//                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(debugFile))) {
//                        while ((line = reader.readLine()) != null) writer.write(line+"\n");
//                    }

                while ((line = reader.readLine()) != null) {
                    onNewLine(line);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                inputStream = null;
                clearDisplayCallback();
                Log.d(TAG, "run: OutputThread结束");
            }
        }

        synchronized public void onNewLine(String line) {
            String finalLine = line.endsWith("\n") ? line.substring(0, line.length()-1) : line;
            allLines.add(finalLine);
            if(allLines.size() > MAX_LINE)
                allLines = allLines.subList(REDUCED_FRAGMENT, allLines.size());
            //TODO 参考一下termux怎么实现terminalView，缓存行，和文本显示（每行一个textview？）
            // 还有ctrl+c 实现？多进程？（留一个rootProcess，用户可以操作的都是从此root分支出来的，保证用户关闭了进程还可以再新建）

            if (displayCallback != null)
                handler.post(() -> displayCallback.call(line));
            else
                Log.d("PRootShell", line);
        }
    }

    private static class DisplayCallback implements Callback<String> {
        TextView tv;
        int lineCount = 0;
        int breakPointCount = 0;
        List<Integer> trimBreakPoint = new ArrayList<>();//要删除缓存行时，移除这里的第一个元素，将此idx之前的字符删掉
        public DisplayCallback(TextView tv) {
            this.tv = tv;

            //设置BufferType为editable，后续用getEditableText获取和编辑
            this.tv.setText("", TextView.BufferType.EDITABLE);

        }

        /**
         * 用于将一行新的文本显示在textview上。若短时间调用多次，可能会延迟几百毫秒后显示。因为涉及UI操作，必须在主线程中调用。
         * @param line 新增加的一行，末尾不能带换行。
         */
        @UiThread
        @Override
        public void call(String line) {
//            if(!isAutoScroll)
//                isAutoScroll = tv.getHeight() <= scrollView.getScrollY() + scrollView.getHeight();
            tv.getEditableText().append(line).append('\n');
            if(isAutoScroll) {
                //原来是fullScroll调用后会设置textview为焦点导致的，即使不再手动调用也会自动滚动。同时也会导致edittext输入到一半被清空焦点。。
                tv.post(() -> {
                    ((NestedScrollView) tv.getParent().getParent()).fullScroll(FOCUS_DOWN);
                    tv.clearFocus();
                });
            }

//            builder.append(line).append("\n");
            //如果快速输入两行，则第一行输入后，尚未滚动到最底部时，第二行输入，此时不会再滚动到最底部。所以只有当确实滚动后再置为false
            //还是不行啊，加个时间限制吧，限制textview.setText()频率
//            updateTextView(false);

            lineCount ++;
            breakPointCount ++;
            if(breakPointCount > REDUCED_FRAGMENT) {
                breakPointCount = 0;
                trimBreakPoint.add(tv.getEditableText().length());
            }

            if(lineCount > MAX_LINE) {
                lineCount = MAX_LINE - REDUCED_FRAGMENT;
                int del = trimBreakPoint.remove(0);
                tv.getEditableText().delete(0, del);
                for(int i=0; i<trimBreakPoint.size(); i++) {
                    int old = trimBreakPoint.remove(i);
                    trimBreakPoint.add(i, old - del);
                }
            }
        }

//        /**
//         * 调用此函数更新textview的文字。
//         * <br/> 如果更新文字前视图处于最底部，则添加一行后再次滚动到最底部
//         * <br/> 如果距离上次刷新文字时间小于200秒，跳过此次刷新，延迟一段时间后重试
//         * @param isFromDelayed 该调用是否为当前频率过快而延迟调用。如果是，检查当前是否还需要刷新
//         */
//        public void updateTextView (boolean isFromDelayed) {
//            //延迟调用，如果已经被之前处理过了，则不管了
//            if(isFromDelayed && !isTextUnhandled)
//                return;
//
//            long currTime = System.currentTimeMillis();
//            NestedScrollView scrollView = (NestedScrollView) tv.getParent().getParent();
//
//            if(!isAutoScroll)
//                isAutoScroll = tv.getHeight() <= scrollView.getScrollY() + scrollView.getHeight();
//
//            if(currTime - lastUpdateTime > 200) {
//                isTextUnhandled = false;
//                lastUpdateTime = currTime;
//                //要重新设置到textview上吗 需要
////                tv.setText("Dd", TextView.BufferType.EDITABLE);
//                //如果当前已经在最底部，添加新一行之后应该继续滚动到最底部。注意要用post
//                if(isAutoScroll)
//                    scrollView.post(() -> {
//                        scrollView.fullScroll(FOCUS_DOWN);
//                        isAutoScroll = false;
//                    });
//            } else {
////                isTextUnhandled = true;
////                tv.postDelayed(() -> updateTextView(true), 200);
//            }
//        }
    }

}