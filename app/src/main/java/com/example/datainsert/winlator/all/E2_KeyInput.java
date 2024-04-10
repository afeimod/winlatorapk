package com.example.datainsert.winlator.all;

import android.util.Log;
import android.view.KeyEvent;

import com.winlator.xserver.XKeycode;
import com.winlator.xserver.XServer;

public class E2_KeyInput {
    private static final String TAG = "E2_KeyInput";
    /** 用于输入unicode文字时，临时充数的keycode */
    public static final XKeycode[] avaiKeyCode = {XKeycode.KEY_A, XKeycode.KEY_B, XKeycode.KEY_C, XKeycode.KEY_D, XKeycode.KEY_E, XKeycode.KEY_F, XKeycode.KEY_G, XKeycode.KEY_H, XKeycode.KEY_I, XKeycode.KEY_J, XKeycode.KEY_K, XKeycode.KEY_L, XKeycode.KEY_M, XKeycode.KEY_N, XKeycode.KEY_O, XKeycode.KEY_P, XKeycode.KEY_Q, XKeycode.KEY_R, XKeycode.KEY_S, XKeycode.KEY_T, XKeycode.KEY_U, XKeycode.KEY_V, XKeycode.KEY_W, XKeycode.KEY_X, XKeycode.KEY_Y, XKeycode.KEY_Z,};
    /** 用于输入unicode文字时，记录本次该用哪个充数的keycode，然后++ */
    public static int  currIndex = 0;



    /**
     * 此函数只处理KeyEvent.ACTION_MULTIPLE的情况
     */
    public static boolean handleAndroidKeyEvent(XServer xServer, KeyEvent event){
        boolean handled = false;
        if(event.getAction() == KeyEvent.ACTION_MULTIPLE){
            String characters = event.getCharacters();
            for (int i = 0; i < characters.codePointCount(0, characters.length()); i++) {
                //大于0xff的，直接加上0x100,0000
                int keySym = characters.codePointAt(characters.offsetByCodePoints(0, i));
                if(keySym>0xff)
                    keySym = keySym | 0x1000000;
                xServer.injectKeyPress(avaiKeyCode[currIndex], keySym);
                xServer.injectKeyRelease(avaiKeyCode[currIndex]);
                currIndex = (currIndex+1)%avaiKeyCode.length;//数组下标+1，为下一次设置另一个keycode做准备
                handled = true;
            }
            Log.d(TAG, "handleAKeyEvent: action=multiple, string="+characters);
        }
        return handled;

    }
}
