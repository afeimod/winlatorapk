package com.example.datainsert.winlator.all.widget;

import android.text.Editable;
import android.text.TextWatcher;

public interface SimpleTextWatcher extends TextWatcher {
    @Override
    default void onTextChanged(CharSequence s, int start, int before, int count) {}

    @Override
    default void beforeTextChanged(CharSequence s, int start, int count, int after) {}
}
