package com.example.datainsert.winlator.all;

import static android.os.VibrationEffect.DEFAULT_AMPLITUDE;

import android.content.Context;
import android.media.AudioAttributes;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.provider.MediaStore;

public class E9_Vibrate {
    static VibrationEffect ve = VibrationEffect.createOneShot(100, 16);
    static AudioAttributes aa = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .build();
//    VibrationAttributes va = VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH);

    public static void on(Context c) {
        Vibrator vibrator = c.getSystemService(Vibrator.class);

        vibrator.vibrate(ve, aa);
//        vibrator.vibrate(100);
    }
}
