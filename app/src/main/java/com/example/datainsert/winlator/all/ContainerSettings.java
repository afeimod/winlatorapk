//package com.example.datainsert.winlator.all;
//
//import static android.text.InputType.TYPE_TEXT_VARIATION_NORMAL;
//
//import android.content.Context;
//import android.database.DataSetObserver;
//import android.text.InputFilter;
//import android.text.Spannable;
//import android.text.SpannableString;
//import android.text.Spanned;
//import android.text.TextUtils;
//import android.util.Log;
//import android.view.View;
//import android.view.ViewGroup;
//import android.view.inputmethod.EditorInfo;
//import android.widget.ArrayAdapter;
//import android.widget.EditText;
//import android.widget.ImageButton;
//import android.widget.ImageView;
//import android.widget.LinearLayout;
//import android.widget.Spinner;
//import android.widget.SpinnerAdapter;
//import android.widget.TextView;
//
//import androidx.appcompat.content.res.AppCompatResources;
//
//import com.github.luben.zstd.ZstdCompressCtx;
//import com.winlator.ContainerDetailFragment;
//import com.winlator.R;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class ContainerSettings {
//    private static final String PREF_KEY_CUSTOM_SCREEN_SIZE = "CUSTOM_SCREEN_SIZE";
//
//    public static void addOptions(ContainerDetailFragment fragment, View rootView) {
//        boolean editContainer = QH.reflectGetFieldInst(ContainerDetailFragment.class, fragment, "container", true) != null;
//        if (!editContainer)
//            return;
//
//        Spinner sScreenSize = rootView.findViewById(R.id.SScreenSize);
//        String customSize = QH.getPreference(fragment.requireActivity()).getString(PREF_KEY_CUSTOM_SCREEN_SIZE, "800x600");
//        SpinnerAdapter adapterWrapper = new SpinnerAdapterWrapper(customSize, sScreenSize.getAdapter());
//        sScreenSize.setAdapter(adapterWrapper);
//
//    }
//
//    public static void addOptionsTest(Context a, View rootView) {
//        Spinner sScreenSize = rootView.findViewById(R.id.SScreenSize);
//        String customSize = QH.getPreference(a).getString(PREF_KEY_CUSTOM_SCREEN_SIZE, "800x600");
//        SpinnerAdapter adapterWrapper = new SpinnerAdapterWrapper(customSize, sScreenSize.getAdapter());
//        sScreenSize.setAdapter(adapterWrapper);
//
//    }
//
//    private static class SpinnerAdapterWrapper implements SpinnerAdapter {
//        private static final String TAG = "SpinnerAdapterWrapper";
//        String[] screenList;
//        Spinner spinner;
//        List<DataSetObserver> observerList = new ArrayList<>();
//
//        public SpinnerAdapterWrapper(String customSize, SpinnerAdapter ori, Spinner spinner) {
//            this.spinner = spinner;
//            this.screenList = new String[ori.getCount() + 1];
//            for (int i = 0; i < ori.getCount(); i++) {
//                screenList[i] = (String) ori.getItem(i);
//            }
//            screenList[screenList.length - 1] = customSize;
//        }
//
//        @Override
//        public View getDropDownView(int position, View convertView, ViewGroup parent) {
//            if (convertView != null) {
//                Log.d(TAG, "getDropDownView: convertView有点击事件吗？" + convertView.isClickable() + convertView.hasOnClickListeners());
//            }
//            Context c = parent.getContext();
//            //好像理解了。这个convertView不是紧挨着的上一个，而是滑动之后已经超出可见范围的，才会被拿来回收利用
//            if (position < screenList.length - 1) {
//                TextView textView = convertView instanceof TextView ? (TextView) convertView : new TextView(c);
//                textView.setText(screenList[0]);
//                return textView;
//            } else {
//                LinearLayout linearLayout = new LinearLayout(c);
//                linearLayout.setOrientation(LinearLayout.HORIZONTAL);
//                TextView tv = new TextView(c);
//                tv.setText("自定义：");
//                linearLayout.addView(tv);
//
//                EditText editText = new EditText(c);
//                editText.setText(screenList[screenList.length-1]);
//                editText.setInputType(EditorInfo.TYPE_CLASS_TEXT | TYPE_TEXT_VARIATION_NORMAL);
//                linearLayout.addView(editText);
//                linearLayout.setClickable(true);
//
//                ImageView imageButton = new ImageView(c);
//                imageButton.setImageDrawable(AppCompatResources.getDrawable(c, R.drawable.icon_confirm));
//                LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(-2, -1);
//                btnParams.setMarginStart(QH.px(c, 12));
//                int padding = QH.px(c, 4);
//                imageButton.setPadding(0, padding, 0, padding);
//                linearLayout.addView(imageButton);
//                imageButton.setOnClickListener(v -> {
//                    screenList[screenList.length-1] = editText.getText().toString();
//                    new ArrayAdapter<>()
////                    for (DataSetObserver observer : observerList) {
////                        observer.onChanged();
////                        observer.onInvalidated();
////                    }
//                });
//                return linearLayout;
//            }
//        }
//
//        @Override
//        public void registerDataSetObserver(DataSetObserver observer) {
//            Log.d(TAG, "registerDataSetObserver: ");
////            observer.onInvalidated();
////            oriAdapter.registerDataSetObserver(observer);
//            if (!observerList.contains(observer))
//                observerList.add(observer);
//        }
//
//        @Override
//        public void unregisterDataSetObserver(DataSetObserver observer) {
//            Log.d(TAG, "unregisterDataSetObserver: ");
////            oriAdapter.registerDataSetObserver(observer);
//            observerList.remove(observer);
//        }
//
//        @Override
//        public int getCount() {
//            return oriCount + 1;
//        }
//
//        @Override
//        public Object getItem(int position) {
//            if (position < oriCount)
//                return oriAdapter.getItem(position);
//            else {
//                return customSize;
//            }
//        }
//
//        @Override
//        public long getItemId(int position) {
//            if (position < oriCount)
//                return oriAdapter.getItemId(position);
//            else {
//                return 555;
//            }
//        }
//
//        @Override
//        public boolean hasStableIds() {
//            return oriAdapter.hasStableIds();
//        }
//
//        @Override
//        public View getView(int position, View convertView, ViewGroup parent) {
//            if (position < oriCount)
//                return oriAdapter.getView(position, convertView, parent);
//            else {
//                TextView textView = convertView instanceof TextView ? (TextView) convertView : new TextView(convertView.getContext());
//                textView.setText(customSize);
//                return textView;
//            }
//        }
//
//        @Override
//        public int getItemViewType(int position) {
//            return 1;
//        }
//
//        @Override
//        public int getViewTypeCount() {
//            return 1;
//        }
//
//        @Override
//        public boolean isEmpty() {
//            return false;
//        }
//    }
//}
