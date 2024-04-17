package com.example.datainsert.winlator.all.e3;

import android.content.SharedPreferences;
import android.text.SpannableStringBuilder;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.datainsert.winlator.all.QH;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class LogInfoManager {
    public abstract class Handler {
        private boolean isEnabled = false;
        private List<String> lines = new ArrayList<>();
        private TextView textView;
        public Handler() {
            isEnabled = getPref().getBoolean(getPrefKey(), false);
            if(isEnabled) start();
            else stop();
        }

        /**
         * 设置isEnabled属性，同时开始或停止读取日志
         */
        public void setEnabled(boolean enabled) {
            isEnabled = enabled;
            getPref().edit().putBoolean(getPrefKey(), enabled).apply();
            if(enabled) start();
            else stop();
        }

        public boolean isEnabled() {
            return isEnabled;
        }

        /**
         * 当读取到新一行时调用此方法存入文本列表
         */
        protected void storeNewLine(String line) {
            lines.add(line);
            addLineToTextView(line);
        }

        /**
         * 外部调用此方法。将文本输出到指定的安卓视图中
         */
        public void bindTextView(TextView textView) {
            this.textView = textView;
            SpannableStringBuilder builder = new SpannableStringBuilder();
            textView.setText(builder);
            for(String line: lines)
                addLineToTextView(line);
        }

        public void clearTextView() {
            textView = null;
        }

        private void addLineToTextView(String line) {
            if(textView == null)
                return;
            SpannableStringBuilder builder = (SpannableStringBuilder) textView.getText();

            builder.append(line).append('\n');
            //要重新设置到textview上吗 需要
            textView.setText(builder);

            if(lines.size() > 3000) {
                lines = lines.subList(300, lines.size());
                builder.delete(0, builder.length()/10);
            }
        }

        abstract String getPrefKey();
        abstract void start();
        abstract void stop();
    }
    public class Logcat extends Handler {

        @Override
        String getPrefKey() {
            return "log_enable_logcat";
        }

        @Override
        void start() {

        }

        @Override
        void stop() {

        }
    }
    AppCompatActivity activity;
    public LogInfoManager(AppCompatActivity activity) {
        this.activity = activity;
    }

    private SharedPreferences getPref() {
        return QH.getPreference(activity);
    }

    public void show(AppCompatActivity a) {
        BottomSheetDialog dialog = new BottomSheetDialog(a);
        LinearLayout linearRoot = new LinearLayout(a);
        linearRoot.setOrientation(LinearLayout.VERTICAL);

        Logcat logHandler = new Logcat(); //new了之后如果默认开启则自动运行。无需手动调用start()

        TextView tvOutput = new TextView(a);
        tvOutput.setText("\n\n无内容\n\n");
        if(logHandler.isEnabled())
            logHandler.bindTextView(tvOutput);

        Toolbar toolbar = new Toolbar(a);
        toolbar.setTitle("Logcat");
//        MenuProvider menuProvider = new MenuProvider() {
//            @Override
//            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
////                menu.add(0,0,1,"启用")
////                        .setActionView()
////                        .setCheckable(true).setChecked(logHandler.isEnabled())
////                        .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM);
//            }
//
//            @Override
//            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
//                switch (menuItem.getOrder()) {
//                    case 1 -> {
//                        menuItem.setChecked(!menuItem.isChecked());
//                        if(menuItem.isChecked()) {
//                            logHandler.setEnabled(menuItem.isChecked());
//                            logHandler.bindTextView(tvOutput);
//                        }
//                        else {
//                            logHandler.setEnabled(menuItem.isChecked());
//                            logHandler.clearTextView();
//                        }
//                    }
//                    default -> {
//                        return false;
//                    }
//                }
//                return true;
//            }
//        };
//        toolbar.addMenuProvider(menuProvider);


        linearRoot.addView(toolbar);
        linearRoot.addView(tvOutput);
        dialog.setContentView(linearRoot);
//        dialog.setOnDismissListener(dialog1 -> toolbar.removeMenuProvider(menuProvider));
        dialog.show();
    }

    private void closeStreamForHandler(Logcat handler) {
    }

    public void openStreamForHandler(Handler handler) {

    }
}
