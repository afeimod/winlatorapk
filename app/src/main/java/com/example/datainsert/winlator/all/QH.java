package com.example.datainsert.winlator.all;

import static android.content.pm.ApplicationInfo.FLAG_TEST_ONLY;
import static android.os.Environment.DIRECTORY_DOWNLOADS;
import static android.os.Environment.getExternalStoragePublicDirectory;


import static com.example.datainsert.winlator.all.QH.dimen.dialogPadding;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.widget.NestedScrollView;
import androidx.preference.PreferenceManager;

import com.winlator.R;
import com.winlator.contentdialog.ContentDialog;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@SuppressLint("DiscouragedApi")
public class QH {
    private static final String TAG = "QH";
    public static boolean isTest = false;
    public static boolean hasBeenRefreshed = false;
    public static int versionCode = 0;
    public static String locale = "zh";
    public static int dp8 = 16;

    /**
     * 初始化一次。可能耗时很久所以最好不要多次初始化
     * 内容：是否为自己的apk，版本号（反射获取），语言，资源id
     * 现在改为在MainActivity.onCreate里调用一次。不知道XServerDisplayActivity不调用有没有影响
     */
    public static void refreshIsTest(Activity a) {
//        if(hasBeenRefreshed)
//            return;

        isTest = (a.getApplicationInfo().flags & FLAG_TEST_ONLY) != 0;
        versionCode = refreshVersionCode(a);
        locale = refreshLocale(a);
//        refreshIdentifiers(a,"drawable",QH.drawable.class);
//        refreshIdentifiers(a,"style",QH.style.class);
//        refreshIdentifiers(a,"id",QH.id.class);
        refreshTexts(locale);
        dp8 = px(a, 8);
        hasBeenRefreshed = true;

        onActivityCreate(a);
    }

    /**
     * 在QH的初始化函数末尾被调用。当activity启动（onCreate）时，进行一些app启动后需要自动执行的操作。
     * <br/> 注意目前QH初始化是在MainActivity中被调用，如果XServer退出，那么这个函数会被再次调用，而且全部变量会被清空（因为使用runtime.exit()结束了进程）
     */
    private static void onActivityCreate(Activity a) {
        ExtraFeatures.Logcat.onCheckChange(a, ExtraFeatures.Logcat.isChecked(a));
    }

    private static int refreshVersionCode(Context c) {
        try {
            PackageInfo info =  c.getPackageManager().getPackageInfo(c.getPackageName(), 0);
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                    ? (int)info.getLongVersionCode()
                    :info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return 0;
        }
    }

    private static String refreshLocale(Context c) {
        return c.getResources().getConfiguration().getLocales().get(0).getLanguage();
    }

    public static int px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * 结构：
     * - frameRoot (dialog自定义视图中占位的）
     * - linearRoot （input dialog的根视图）
     * - checkbox
     * 更改：
     * 1. frameRoot weight改为1，height改为0，这样保证低端按钮会显示（只有点击确定按钮后数据才会更新）
     * 2. linearRoot和frameRoot之间再加一个scrollview
     */
    public static void makeDialogContentScrollable(Context a, ContentDialog dialog) {
        try {
            FrameLayout frameRoot = dialog.findViewById(R.id.FrameLayout);
            if (frameRoot.getChildCount() == 0)
                return;

            View childRoot = frameRoot.getChildAt(0);
            if(childRoot instanceof ScrollView || childRoot instanceof NestedScrollView)
                return;

            LinearLayout.LayoutParams frameRootParams = (LinearLayout.LayoutParams) frameRoot.getLayoutParams();
            frameRootParams.weight = 1;
            frameRootParams.height = 0;
            frameRoot.setLayoutParams(frameRootParams);

            ScrollView scrollView = new ScrollView(a);
            frameRoot.removeView(childRoot);
            scrollView.addView(childRoot);
            frameRoot.addView(scrollView);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //    public static Object reflectGetClassInst(){
//
//    }
    public static <T> T reflectGetFieldInst(Class<?> clz, Object clzInst, String fieldName, boolean isHide) {
        Object fieldInst = null;
        try {
            Field field = clz.getDeclaredField(fieldName);
            if (isHide)
                field.setAccessible(true);
            fieldInst = field.get(clzInst);
            if (isHide)
                field.setAccessible(false);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        return (T) fieldInst;
    }

    public static Object reflectInvokeMethod(Class<?> clz, String methodName, Class<?>[] clzs, Object inst, Object... params){
        Method method = null;
        try {
            method = clz.getDeclaredMethod(methodName, clzs);
            method.setAccessible(true);
            Object result = method.invoke(inst,params);
            method.setAccessible(false);
            return result;
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * 在给定的viewgroup中遍历全部子view，寻找特定条件的view
     *
     * @param rootParent viewgroup
     * @return 特定视图，或者null
     */
    public static View findSpecView(ViewGroup rootParent, ViewFilter viewPattern) {
        List<ViewGroup> unCheckedViewGroups = new ArrayList<>();
        unCheckedViewGroups.add(rootParent);
        while (unCheckedViewGroups.size() > 0) {
            ViewGroup currGroup = unCheckedViewGroups.remove(0);
            for (int i = 0; i < currGroup.getChildCount(); i++) {
                View currSub = currGroup.getChildAt(i);
                if (viewPattern.accept(currSub))
                    return currSub;
                if (currSub instanceof ViewGroup) {
                    unCheckedViewGroups.add((ViewGroup) currSub);
                    continue;
                }
            }
        }
        return null;
    }

    public static int resolveId(int my, int ori) {
        return isTest ? my : ori;
    }

    /**
     * dialog的自定义视图，最外部加一层Nested滚动视图，并且添加padding<br/>
     * 滚动视图会获取焦点，以阻止edittext自动弹出输入法，和解决自动滚动到回收视图的位置而非第一个视图位置的问题
     */
    public static NestedScrollView wrapAsDialogScrollView(View view) {
        Context c = view.getContext();
        NestedScrollView scrollView = new NestedScrollView(c);
        scrollView.setPadding(dialogPadding(c), 0, dialogPadding(c), 0);
        scrollView.addView(view);
        //阻止edittext获取焦点弹出输入法 / 回收视图获取焦点自动滚动到回收视图的位置
        scrollView.setFocusable(true);
        scrollView.setFocusableInTouchMode(true);
        scrollView.requestFocus();
        return scrollView;
    }

    public static SharedPreferences getPreference(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    /**
     * 在视图后面跟一个问号按钮，点击可以查看说明
     */
    public static LinearLayout wrapHelpBtnWithLinear(Context a, View view, String info) {

        LinearLayout linearRoot = new LinearLayout(a);
        linearRoot.setOrientation(LinearLayout.HORIZONTAL);
        linearRoot.addView(view);

        ImageView infoImage = new ImageView(a);
        infoImage.setImageDrawable(AppCompatResources.getDrawable(a, R.drawable.icon_help));
        int imagePadding = QH.px(a, 5);
        infoImage.setPadding(0, imagePadding, 0, imagePadding);
        infoImage.setImageTintList(ColorStateList.valueOf(0xff607D8B));
        infoImage.setOnClickListener(v -> {
            new AlertDialog
                    .Builder(a)
                    .setMessage(info)
                    .setPositiveButton(android.R.string.ok, null)
                    .create()
                    .show();
        });
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(-2, -1);
        imageParams.setMarginStart(QH.px(a, 8));
        linearRoot.addView(infoImage, imageParams);
        return linearRoot;
    }

    private static void versionNotSupported() {
        throw new RuntimeException("不支持的版本号：" + versionCode);
    }

    public static LayoutParams.Linear lpLinear(int w, int h) {
        return LayoutParams.Linear.one(w, h);
    }

    private static void refreshTexts(String locale) {
        if(!locale.equals("zh")){
            string.pulse声音选项 = "PulseAudio Server";
            string.pulse声音简介 = "Pulseaudio is used to play audio. This version is extracted from XSDL.";
            string.旋转屏幕选项 = "Rotate Screen";
            string.游戏样式光标选项 = "Use Game Style Cursor";
            string.游戏样式光标选项说明 = "Winlator inherits the xserver bug from exagear, which can't display mouse cursor image sometimes. If cursor drawable is not available, an alternate png image is used." +
                    "\n\nIf this option is checked, xserver will first try to get and display the cursor drawable with game's style. However colors will be only black and white, and the dynamic cursor can only be displayed as a static image.";
            string.绝对位置点击选项 = "Cursor moves to where finger presses";
            string.绝对位置点击选项说明 = "If checked, cursor will go to where you finger is when pressing.\n1 finger click = mouse left click.\n1st finger pressing, 2nd finger press and moving = mouse left button drag and drop.\n1st finger pressing, 2nd finger quick tap = mouse right button click.";
            string.pulse按钮立即运行 = "Run now";
            string.pulse按钮立即停止="Stop now";
            string.pulse按钮注意事项="Note";
            string.pulse选项自动运行="Autorun after container started";
            string.pulse注意事项文字="Test only, does not guarantee better sound playback than Alsa." +
                    "\n\nWhen starting, the missing dep-libs are automatically unpacked." +
                    "These libs were removed from obb originally, so I'm not sure if there is any conflicts." +
                    "Click the button to remove these files if there is." ;
            string.pulse删除依赖库="Delete libs";
            string.额外功能 =  "Extra";

            string.选择数据包说明 = "OBB not found. Please select it from local files, or download it from Github, or put the obb file(renamed as %s) into %s and restart the app.\nNote that when manually selecting you cannot see the directory of %s.";
            string.手动选择 = "Manually Select";
            string.从Github下载 = "Download from Github";
            string.退出 = "Exit";
            string.选择OBB结果 = "Selection cancelled$Selected file(%s) is not an obb file for winlator. Please select another one.$Obb (%s) selected. Start decompression. Please don't switch interfaces.";
            string.下载OBB结果 = "Downloading %s. Please don't switch interfaces. Download progress can be checked on the notification bar.$Download completed. Start decompression.$Download failed.";
            string.解压数据包 = "Uncompress OBB";
            string.Obb下载文件名 = "Winlator_OBB";

            string.logcat日志 = "Logcat info";
            string.logcat日志说明 = "Enable/Disable logcat info output, for debug use. output file is stored at /storage/emulated/0/Download/Winlator/logcat .";
        }else{
            string.pulse声音选项 = "PulseAudio声音服务";
            string.pulse声音简介 = "pulseaudio服务用于播放声音。本服务提取自XSDL。";
            string.旋转屏幕选项 = "旋转屏幕";
            string.游戏样式光标选项 = "光标样式尽量使用游戏样式";
            string.游戏样式光标选项说明 = "winlator继承了exagear的xserver的bug，在使用gpu渲染时鼠标光标无法正常显示，因此一般使用一个备用png图片作为鼠标光标。" +
                    "\n\n若勾选此选项，会首先尝试获取游戏样式的光标，但色彩只有黑白，且动态光标只能显示为静态图片。";
            string.绝对位置点击选项 = "触屏后 鼠标移动到手指位置";
            string.绝对位置点击选项说明 = "如果勾选该选项，手指按下时，光标立即移动到对应位置。\n单指点击=鼠标左键点击.\n一指按住，第二指按下并移动=左键拖拽。\n一指按住，第二指按下并立刻松开=右键点击。";
            string.pulse按钮立即运行 = "立即运行";
            string.pulse按钮立即停止="立即停止";
            string.pulse按钮注意事项="注意事项";
            string.pulse选项自动运行="启动容器后自动运行";
            string.pulse注意事项文字="本功能仅供测试，不保证声音播放效果比alsa更好。" +
                    "\n\n运行pulseaudio服务时，缺少的依赖库文件会被自动解压补全。" +
                    "作者制作数据包的脚本中特意移除了这些库，所以不确定补全后是否会有冲突。若有冲突，请点击按钮删除这些文件。";
            string.pulse删除依赖库="删除文件";
            string.额外功能 = "额外功能";

            string.选择数据包说明 = "未找到数据包。请从本地选择，或从Github下载，或将数据包(名称为 %s )放到 %s 文件夹中，并重启app。\n手动选择时无法查看%s文件夹。";
            string.手动选择 = "手动选择";
            string.从Github下载 = "从Github下载原版数据包";
            string.退出 = "退出";
            string.选择OBB结果 = "选择文件取消$选择文件不是数据包(%s)。请重新选择$已选择数据包(%s)，开始解压，请勿切换界面";
            string.下载OBB结果 = "正在下载%s，请勿切换界面。下载进度可在手机通知栏查看$下载完成，正在解压$下载失败";
            string.解压数据包 = "解压数据包";
            string.Obb下载文件名 = "Winlator数据包";

            string.logcat日志 = "logcat日志";
            string.logcat日志说明 = "开启或关闭安卓logcat日志，用于调试。\n\n日志文件存储在 /storage/emulated/0/Download/Winlator/logcat 目录下。";
        }
    }

    /**
     * 从Resources中获取资源名对应的资源id。不需要为每个版本手动适配id了。
     * @param type 类型，如drawable，string，id等
     */
    private static void refreshIdentifiers(Context c,String type,Class<?> myClass){
//        c.getResources().getIdentifier("icon_help","drawable",pkg   );
//        Class<?> myClass = Arrays.stream(QH.class.getClasses()).filter(aClass -> aClass.getSimpleName().contains("drawable")).findFirst().get();
        String pkg = c.getPackageName();
        for(Field field:myClass.getFields()){
            try {
                field.setInt(null,c.getResources().getIdentifier(field.getName(), type,pkg));
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }


    public static class string{
        public static String logcat日志;
        public static String logcat日志说明;
        public static String Obb下载文件名;
        public static String 解压数据包;
        public static String 下载OBB结果;
        public static String 选择OBB结果;
        public static String 退出;
        public static String 从Github下载;
        public static  String 绝对位置点击选项;
        public static  String 绝对位置点击选项说明;
        public static String 游戏样式光标选项;
        public static String 游戏样式光标选项说明;
        public static String pulse声音选项;
        public static String 旋转屏幕选项;
        public static String pulse声音简介;
        public static String pulse按钮立即运行;
        public static String pulse按钮立即停止;
        public static String pulse按钮注意事项;
        public static String pulse注意事项文字;
        public static String pulse删除依赖库;
        public static String pulse选项自动运行;
        public static String 额外功能;
        public static String 选择数据包说明;
        public static String 手动选择;
    }

    public static String[] getSArr(String s){
        return s.split("\\$");
    }

    interface ViewFilter {
        boolean accept(View view);
    }

    @Deprecated
    static class style {
        public static int ButtonNeutral;
    }

    @Deprecated
    static class id {
        public static int BTCancel;
        public static int BTConfirm;
        public static int FrameLayout;
        public static int NavigationView;
        public static int DrawerLayout;
        public static int CBLockCursor;
        public static int FLFragmentContainer;
        public static int Toolbar;
        public static int installing_obb_image;
    }

    @Deprecated
    public static class drawable {
        public static int icon_help;
    }


    public static class dimen {

        public static int margin8Dp(Context c) {
            return QH.px(c, 8);
        }

        /**
         * 48dp对应的px值
         */
        public static int minCheckSize(Context c) {
            return QH.px(c, 48);
        }

        /**
         * 24dp对应的px值
         */
        public static int dialogPadding(Context c) {
            return QH.px(c, 24);
        }

    }

    public static class attr {
        /**
         * 为对话框设置自定义视图的时候，手动设置边距
         */
        @Deprecated
        public static int dialogPaddingDp = 24;

        public static int textColorPrimary(Context c) {
            TypedArray array = c.obtainStyledAttributes(new int[]{android.R.attr.textColorPrimary});
            int color = array.getColor(0, Color.BLACK);
            array.recycle();
            return color;
        }

        public static int colorPrimary(Context c) {
            TypedArray array = c.obtainStyledAttributes(new int[]{android.R.attr.colorPrimary});
            int color = array.getColor(0, 0xff2196F3);
            array.recycle();
            return color;
        }

        public static Drawable selectableItemBackground(Context c) {
            TypedArray array = c.obtainStyledAttributes(new int[]{android.R.attr.selectableItemBackground});
            Drawable d = array.getDrawable(0);
            array.recycle();
            return d;
        }

    }

    public static class Files {
        public static File Download() {
            return getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS);
        }
    }

    public static class LayoutParams {

        /**
         * 常见的LinearLayout.LayoutParams构建
         */
        public static class Linear {
            private int w = -1;
            private int h = -2;
            private float weight = 0;
            private int[] margins = {0, 0, 0, 0};
            private int gravity = -111;
//
//            /**
//             * 宽为match，高为rap
//             */
//            public static Linear one() {
//                return new Linear();
//            }

            public static Linear one(int w, int h) {
                Linear linear = new Linear();
                linear.w = w;
                linear.h = h;
                return linear;
            }

            public Linear gravity(int pg) {
                gravity = pg;
                return this;
            }

            public Linear weight(float pw) {
                weight = pw;
                return this;
            }

            public Linear weight() {
                weight = 1;
                return this;
            }

            /**
             * 顶部margin设为8dp
             */
            public Linear top() {
                margins[1] = dp8;
                return this;
            }

            public Linear top(int margin) {
                margins[1] = margin;
                return this;
            }

            public Linear bottom() {
                margins[3] = dp8;
                return this;
            }

            public Linear left() {
                margins[0] = dp8;
                return this;
            }

            public Linear left(int margin) {
                margins[0] = margin;
                return this;
            }

            public Linear right() {
                margins[2] = dp8;
                return this;
            }

            public Linear margin(int left, int top, int right, int bottom) {
                margins = new int[]{left, top, right, bottom};
                return this;
            }

            public LinearLayout.LayoutParams to() {
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(w, h, weight);
                params.weight = weight;
                if (gravity != -111)
                    params.gravity = gravity;
                params.setMargins(margins[0], margins[1], margins[2], margins[3]);
                return params;
            }
        }
    }

}
