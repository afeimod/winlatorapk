package com.example.datainsert.winlator.all;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT;

import android.annotation.SuppressLint;

import androidx.appcompat.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;
import android.util.SparseArray;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.winlator.R;
import com.winlator.XServerDisplayActivity;
import com.winlator.contentdialog.ContentDialog;
import com.winlator.core.EnvVars;
import com.winlator.renderer.GLRenderer;
import com.winlator.widget.TouchpadView;
import com.winlator.xserver.Cursor;
import com.winlator.xserver.CursorManager;
import com.winlator.xserver.Drawable;
import com.winlator.xserver.requests.DrawRequests;

import java.nio.ByteBuffer;
import java.util.Locale;

public class XserverNavMenuControl {
    private static final String TAG = "XserverNavMenuControl";
    private static final String PREF_KEY_XSA_ORIENT_LAND = "activity_orientation_landsc";
    private static final String PREF_KEY_IS_GAME_STYLE_CURSOR = "IS_GAME_STYLE_CURSOR";
    private static final String PREF_KEY_IS_CUR_MOVE_REL = "IS_CUR_MOVE_REL";
    public static boolean isGameStyleCursor = false;
    public static XServerDisplayActivity aInstance;

    @SuppressLint("SourceLockedOrientationActivity")
    public static void addItems(XServerDisplayActivity a) {
        try {
            aInstance = a;
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


            if(QH.isTest){
                subMenu.add("查看cursor").setOnMenuItemClickListener(item->{
                    //显示对应cursor的图片
                    ImageView imageView = new ImageView(a);
                    TextView textView = new TextView(a);
                    textView.setTextColor(Color.WHITE);

                    //可供选择的id
                    LinearLayout linearCursorIds = new LinearLayout(a);
                    linearCursorIds.setOrientation(LinearLayout.VERTICAL);

                    SparseArray<Cursor> cursors = QH.reflectGetFieldInst(CursorManager.class, a.getXServer().cursorManager, "cursors", true);
                    for(int keyIdx = 0; keyIdx < cursors.size(); keyIdx++) {
                        int cursorId = cursors.keyAt(keyIdx);
                        Cursor cursor = cursors.get(cursorId);
                        Drawable d = cursor.cursorImage;
                        Button btnId = new Button(a);
                        btnId.setText(String.format(Locale.ROOT, "%d(%dx%d)",cursorId,d.width, d.height));
                        btnId.setOnClickListener(new View.OnClickListener() {
                            int flag = 0;
                            @Override
                            public void onClick(View v) {
                                try {
                                    String text = "";
                                    Drawable image;
                                    switch (flag) {
                                        case 0-> {
                                            image = cursor.cursorImage;
                                            text += "cursorImage";
                                        }
                                        case 1 -> {
                                            image = cursor.maskImage;
                                            text += "maskImage";
                                        }
                                        case 2 -> {
                                            image = cursor.sourceImage;
                                            text += "sourceImage";
                                        }
                                        default -> throw new RuntimeException("");
                                    }
                                    ByteBuffer buffer = image.getData();
                                    int[] colors = new int[image.width * image.height];
                                    for(int i=0; i<colors.length; i++) {
                                        colors[i] = buffer.getInt();// | 0x80000000;
                                        if(colors[i] != 0x000000)
                                            colors[i] = colors[i] | 0xff000000;
                                    }
                                    buffer.rewind();
                                    Bitmap bitmap = Bitmap.createBitmap(colors,image.width, image.height, Bitmap.Config.ARGB_8888);
                                    imageView.setImageBitmap(bitmap);

                                    text += "\nid="+image.id;
                                    text += "\ndepth="+(image.visual != null ? image.visual.depth : "null");
                                    textView.setText(text);
                                    flag = (flag + 1) % 3;
                                }catch (Exception e){
                                    e.printStackTrace();
                                }
                            }
                        });
                        linearCursorIds.addView(btnId);
                    }
                    ScrollView scrollView = new ScrollView(a);
                    scrollView.addView(linearCursorIds);

                    LinearLayout linearImageAndText = new LinearLayout(a);
                    linearImageAndText.setOrientation(LinearLayout.VERTICAL);
                    linearImageAndText.addView(imageView, new LinearLayout.LayoutParams(QH.px(a,160), QH.px(a, 160)));
                    linearImageAndText.addView(textView);

                    LinearLayout linearRoot = new LinearLayout(a);
                    linearRoot.setBackgroundColor(Color.DKGRAY);
                    linearRoot.setOrientation(LinearLayout.HORIZONTAL);
                    linearRoot.addView(scrollView);
                    linearRoot.addView(linearImageAndText);

                    AlertDialog dialog = new AlertDialog.Builder(a)
                            .setView(linearRoot)
                            .create();
                    dialog.show();
                    return true;
                });


                linearBitmapList = new LinearLayout(a);
                linearBitmapList.setOrientation(LinearLayout.VERTICAL);
                linearBitmapList.setBackgroundColor(Color.DKGRAY);
                ScrollView scrollView = new ScrollView(a);
                scrollView.addView(linearBitmapList);
                ((FrameLayout)a.findViewById(R.id.FLXServerDisplay)).addView(scrollView, new FrameLayout.LayoutParams(-2,-1));
            }


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

    static LinearLayout linearBitmapList;
    public static void addBitmap(ByteBuffer data, int dstX, int dstY, int w, int h, int depth, DrawRequests.Format format){
        try {
            boolean depthIs1 = depth == 1;
            int[] colors = new int[w * h];
            try {
                if(depthIs1 && format == DrawRequests.Format.Z_PIXMAP){
                    byte[] bytes = data.array();
                    int stride = ((w + 32 - 1) >> 5) << 2;
                    for(int y=0; y<h; y++){
                        for(int x=0; x<w; x++){
                            int mask = (1 << (x & 7));
                            int bit = (bytes[stride*y + (x>>3)] & mask) != 0 ? 1 : 0;
                            colors[w*y+x] = bit !=0 ? Color.WHITE : Color.BLACK;
                        }
                    }
                }
                else
                    for(int i=0; i<colors.length; i++) {
                        colors[i] = depthIs1 ?data.get() : data.getInt();
                    }
            }catch (Exception e){
                e.printStackTrace();
            }
            Bitmap bitmap = Bitmap.createBitmap(colors,w, h, Bitmap.Config.ARGB_8888);
            linearBitmapList.post(()->{
                ImageView imageView = new ImageView(aInstance);
                imageView.setImageBitmap(bitmap);
                linearBitmapList.addView(imageView, 0, new ViewGroup.LayoutParams(80,80));
                View view = new View(aInstance);
                view.setBackgroundColor(Color.RED);
                view.setMinimumHeight(4);
                linearBitmapList.addView(view, 0);
            });
        }catch (Exception e){
            e.printStackTrace();
        }
        data.rewind();
    }

    public static void addBitmap(Drawable image) {
        try {
            ByteBuffer buffer = image.getData();
            int[] colors = new int[image.width * image.height];
            for(int i=0; i<colors.length; i++) {
                colors[i] = buffer.getInt() | 0xff000000;
            }
            buffer.rewind();
            Bitmap bitmap = Bitmap.createBitmap(colors,image.width, image.height, Bitmap.Config.ARGB_8888);
            linearBitmapList.post(()->{
                ImageView imageView = new ImageView(aInstance);
                imageView.setImageBitmap(bitmap);
                linearBitmapList.addView(imageView, 0, new ViewGroup.LayoutParams(80,80));
                View view = new View(aInstance);
                view.setBackgroundColor(Color.RED);
                view.setMinimumHeight(4);
                linearBitmapList.addView(view, 0);

                imageView.setOnClickListener(v->{
                    FrameLayout frameLayout = new FrameLayout(aInstance);
                    ImageView bigImage = new ImageView(aInstance);
                    if(bitmap.getWidth()<50 || bigImage.getHeight() < 50)
                        bigImage.setLayoutParams(new FrameLayout.LayoutParams(QH.px(aInstance,160),QH.px(aInstance,160)));
                    bigImage.setImageBitmap(bitmap);
                    frameLayout.addView(bigImage);
                    new AlertDialog.Builder(aInstance)
                            .setView(frameLayout)
                            .show();
                });
            });
        }catch (Exception e){
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
