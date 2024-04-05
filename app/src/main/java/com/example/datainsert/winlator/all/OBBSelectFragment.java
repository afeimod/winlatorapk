package com.example.datainsert.winlator.all;

import static android.content.Context.RECEIVER_EXPORTED;
import static com.example.datainsert.winlator.all.QH.dimen.margin8Dp;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;

import com.winlator.MainActivity;
import com.winlator.core.Callback;
import com.winlator.core.OnExtractFileListener;
import com.winlator.core.TarCompressorUtils;
import com.winlator.xenvironment.ImageFs;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;

public class OBBSelectFragment extends Fragment {
    static final int PICK_OBB_FILE = 123;
    private static final String TAG = "OBBSelectFragment";
    private static final int COLOR_RED = 0xFFCA2400, COLOR_GREEN = 0xFF2CAE1D;
    static boolean isHandlingDownloadComplete = false; //不知道为什么接收器会接收到两次，只好用个flag卡一下了
    private static long downloadID = -1;
    private static Callback<Uri> runAfterSelect = uri -> Log.d(TAG, ": 尚未注册回调");
    File rootDir;
    TextView tvResult;
    List<Button> btnList;
    BroadcastReceiver broadcastReceiver = new DownloadCompletedReceiver(); //最好在关闭dialog的时候取消注册这个接收器

    public OBBSelectFragment() {
        super();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requireActivity().findViewById(QH.id.Toolbar).setVisibility(View.GONE);
//        ((MainActivity)requireActivity()).getSupportActionBar().hide();

    }


    static boolean testIsZstd(InputStream is) {
        try (DataInputStream dis = new DataInputStream(is)) {
            byte[] fileMagic = new byte[4];
            dis.readFully(fileMagic);
            StringBuilder s = new StringBuilder();
            for (int i = 0; i < 4; i++)
                s.append(Integer.toHexString(fileMagic[i] & 0x000000ff));
            Log.d(TAG, "extract: 读取magic结果" + s);

            return s.toString().equalsIgnoreCase("28b52ffd") || s.toString().equalsIgnoreCase("502a4d18");
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }


    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        Context c = requireContext();
        rootDir = ImageFs.find(c).getRootDir();

        LinearLayout linearRoot = new LinearLayout(c);
        linearRoot.setOrientation(LinearLayout.VERTICAL);

        TextView tvInfo = new TextView(c);
        tvInfo.setTextIsSelectable(true);
        String obbDir = c.getObbDir().getAbsolutePath();

        tvInfo.setText(String.format(QH.string.选择数据包说明, OBBFinder.getIdealObbName(c), obbDir, obbDir));

        tvResult = new TextView(c);
        tvResult.setTextColor(QH.attr.colorPrimary(c));

        Button btnSelect = new Button(c, null, 0, QH.style.ButtonNeutral);
        btnSelect.setText(QH.string.手动选择);
        btnSelect.setAllCaps(false);
        btnSelect.setOnClickListener(v -> {
//            launcher.launch(new String[]{"*/*"})
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");//仅显示obb类型
//            startActivityForResult(intent, PICK_OBB_FILE);
            requireActivity().startActivityFromFragment(this, intent, PICK_OBB_FILE);

        });

        Button btnDownload = new Button(c, null, 0, QH.style.ButtonNeutral);
        btnDownload.setText(QH.string.从Github下载);
        btnDownload.setAllCaps(false);
        btnDownload.setOnClickListener(v -> downloadFromGithub());

        Button btnExit = new Button(c, null, 0, QH.style.ButtonNeutral);
        btnExit.setText(QH.string.退出);
        btnExit.setAllCaps(false);
        btnExit.setOnClickListener(v -> dismissDialogAndInterruptThread(false));

        btnList = Arrays.asList(btnSelect, btnDownload, btnExit);

        //注册下载完成时的接收器 (服了旧版androidx没有ContextCompat.registerReceiver）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            c.registerReceiver(broadcastReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), RECEIVER_EXPORTED);//要用exported，不然接收不到
        } else {
            c.registerReceiver(broadcastReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        }


        runAfterSelect = uri -> {
            Log.d(TAG, "extract: 当前线程不是主线程吗");
            setTextRight("");

            String[] results = QH.getSArr(QH.string.选择OBB结果);
            if (uri == null) {
                setTextWrong(results[0]);//选择文件取消
                return;
            }
            //检查是否是zst类型
            try {
                if (!testIsZstd(requireActivity().getContentResolver().openInputStream(uri))) {
                    setTextWrong(String.format(results[1], uri));//"选择文件不是数据包(%s)。请重新选择"
                    return;
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            setTextRight(String.format(results[2], uri));//"已选择数据包(%s)，开始解压，请勿切换界面"
            setAllBtnEnabled(false);

            // 接收结果是在主线程，所以需要新建线程再解压
            extractFromUri(uri, null);
        };


        linearRoot.addView(tvInfo);
        linearRoot.addView(tvResult);
        linearRoot.addView(btnSelect);
        linearRoot.addView(btnDownload);
        linearRoot.addView(btnExit);

        ScrollView scrollView = new ScrollView(c);
        scrollView.setPadding(margin8Dp(c),margin8Dp(c),margin8Dp(c),margin8Dp(c));
        scrollView.addView(linearRoot);
        return scrollView;

//        try {
//            setCancelable(false);
//        } catch (Throwable throwable) {
//            throwable.printStackTrace();
//            try {
//                Field field = DialogFragment.class.getDeclaredField("mCancelable");
//                field.setAccessible(true);
//                field.setBoolean(this, false);
//                field.setAccessible(false);
//            } catch (Throwable throwable2) {
//                throwable2.printStackTrace();
//            }
//        }
//
//        ContentDialog mDialog = new ContentDialog(c);
//        mDialog.setCancelable(false);
//        mDialog.setTitle(QH.string.解压数据包);
//        FrameLayout frameLayout = mDialog.findViewById(QH.id.FrameLayout);
//        frameLayout.setVisibility(View.VISIBLE);
//
//        frameLayout.addView(scrollView);
//        mDialog.findViewById(QH.id.BTCancel).setVisibility(View.GONE);
//        mDialog.findViewById(QH.id.BTConfirm).setVisibility(View.GONE);
//        return mDialog;

    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if ((requestCode & 0x0000ffff) != PICK_OBB_FILE) {
            return;
        }
        runAfterSelect.call(data != null ? data.getData() : null);
    }


    void setTextRight(String text) {
        requireActivity().runOnUiThread(() -> {
            tvResult.setTextColor(COLOR_GREEN);
            tvResult.setText(text);
        });

    }

    void setTextWrong(String text) {
        requireActivity().runOnUiThread(() -> {
            tvResult.setTextColor(COLOR_RED);
            tvResult.setText(text);
        });
    }

    private void downloadFromGithub() {
        setAllBtnEnabled(false);

        String downloadLink =
                "https://github.com/brunodev85/winlator/releases/download/v3.0.0/main.3.com.winlator.obb";
//                "https://github.com/brunodev85/winlator/releases/download/v1.0.0/Winlator_1.0.apk";
//                "https://github.com/brunodev85/winlator/blob/main/input_controls/Call%20of%20Juarez%20Gunslinger.icp";
//                "https://github.com/brunodev85/winlator/blob/main/obb_image_generator/wine.txz";
        String fileName = downloadLink.substring(downloadLink.lastIndexOf('/') + 1);
        DownloadManager downloadManager = requireActivity().getSystemService(DownloadManager.class);

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadLink));
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
//        request.setDescription("下载winlator运行必需的obb数据包"); //这个不知道会在哪显示，通知栏显示的是 “正在下载”+title
        request.setTitle(QH.string.Obb下载文件名);

        //开始下载。获取唯一id

        Log.d(TAG, "downloadFromGithub: 提交下载链接到downloadManager：" + downloadLink);
        downloadID = downloadManager.enqueue(request);
        setTextRight(String.format(QH.getSArr(QH.string.下载OBB结果)[0], fileName));//正在下载%s，请勿切换界面。下载进度可在手机通知栏查看。

    }

    private void extractFromUri(Uri uri, Runnable callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            boolean extractResult = TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, requireActivity(),uri,rootDir);
//            try {
//                extractResult = (Boolean) QH.reflectInvokeMethod(
//                        TarZstdUtils.class,
//                        "extract",
//                        new Class[]{InputStream.class, File.class, OnExtractFileListener.class},
//                        null, requireActivity().getContentResolver().openInputStream(uri), rootDir, null);
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//            }

            if(callback!=null)
                callback.run();
            Log.d(TAG, "extractFromUri: 解压结果？" + extractResult);
            dismissDialogAndInterruptThread(extractResult);
        });
    }

    private void setAllBtnEnabled(boolean enable) {
        for (Button btn : btnList)
            btn.setEnabled(enable);
    }

    private void dismissDialogAndInterruptThread(boolean success) {
        if(success){
            OBBFinder.afterExtractFinished((MainActivity) requireActivity());
        }
        requireContext().unregisterReceiver(broadcastReceiver);
        requireActivity().onBackPressed();
//        dismiss();
        getParentFragmentManager().popBackStack(null,0);

        requireActivity().finish();
    }



    class DownloadCompletedReceiver extends BroadcastReceiver {


        @Override
        public void onReceive(Context context, Intent intent) {
            if (isHandlingDownloadComplete)
                return;
            isHandlingDownloadComplete = true;
            Log.d(TAG, "onReceive: 接收器接收到信息");

            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (!DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction()) || id == -1 || id != downloadID)
                return;
            DownloadManager downloadManager = requireContext().getSystemService(DownloadManager.class);
            Uri obbUri = downloadManager.getUriForDownloadedFile(id);
            String[] results = QH.getSArr(QH.string.下载OBB结果);
            if (obbUri != null) {
                Log.d(TAG, "onReceive: 下载完成，正在解压" + obbUri);
                setTextRight(results[1]);//下载完成，正在解压
                extractFromUri(obbUri, () -> isHandlingDownloadComplete = false);
            } else {
                setTextWrong(results[2]);//下载失败
                setAllBtnEnabled(true);
            }
        }
    }
}
