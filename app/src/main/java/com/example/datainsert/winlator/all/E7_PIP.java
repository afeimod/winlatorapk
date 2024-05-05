package com.example.datainsert.winlator.all;

import android.app.PictureInPictureParams;
import android.util.Rational;

import com.winlator.XServerDisplayActivity;
import com.winlator.renderer.GLRenderer;

/**
 * 画中画模式
 */
public class E7_PIP {
    public static boolean enterPIP(XServerDisplayActivity a) {
        a.enterPictureInPictureMode(new PictureInPictureParams.Builder()
                .setAspectRatio(new Rational(a.getXServer().screenInfo.width, a.getXServer().screenInfo.height))
                .build());

//        GLRenderer renderer = a.getXServer().getRenderer();
//        renderer.xServerView.postDelayed(renderer::toggleFullscreen,1000);
//        renderer.xServerView.postDelayed(renderer::toggleFullscreen,1300);
        return true;
    }
}
