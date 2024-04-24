package com.example.datainsert.winlator.all;

import android.content.Context;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import com.winlator.R;
import com.winlator.XServerDisplayActivity;
import com.winlator.contentdialog.ContentDialog;
import com.winlator.widget.TouchpadView;

/**
 * 绝对位置点击
 */
public class E6_ClickToMovePointer {
    private static final String PREF_KEY_IS_CUR_MOVE_REL = "IS_CUR_MOVE_REL";

    public static void addInputControlsItems(XServerDisplayActivity a, ContentDialog dialog) {
//        QH.refreshIsTest(a);
        try {
            LinearLayout linearRoot = (LinearLayout) dialog.findViewById(R.id.CBLockCursor).getParent();

            CheckBox checkBox2 = new CheckBox(a);
            checkBox2.setText(QH.string.绝对位置点击选项);
            checkBox2.setChecked(!getIsCurMoveRelFromPref(a));
            checkBox2.setOnCheckedChangeListener((compoundButton, isChecked) -> setIsCurMoveRel(a, !isChecked, true));
            //初始化TouchAreaView变量 在ExtraFeatures.XMenuExtra中
            linearRoot.addView(QH.wrapHelpBtnWithLinear(a, checkBox2, QH.string.绝对位置点击选项说明));

            //内容太多，设置为可滚动
            QH.makeDialogContentScrollable(a, dialog);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean getIsCurMoveRelFromPref(Context a) {
        return QH.getPreference(a).getBoolean(PREF_KEY_IS_CUR_MOVE_REL, true);
    }

    public static void setIsCurMoveRel(Context a, boolean isRel, boolean updatePref) {
        if (updatePref)
            QH.getPreference(a).edit().putBoolean(PREF_KEY_IS_CUR_MOVE_REL, isRel).apply();
        try {
            TouchpadView.isRelativeOnStart = isRel;
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }
}
