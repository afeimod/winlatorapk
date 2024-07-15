package com.example.datainsert.winlator.all;

import android.app.GameManager;
import android.app.GameState;
import android.content.Context;
import android.media.midi.MidiDevice;
import android.media.midi.MidiDeviceStatus;
import android.media.midi.MidiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.PowerManager;
import android.os.Process;

import androidx.annotation.RequiresApi;

import com.winlator.contentdialog.ContentDialog;

/**
 * 一些电池选项，可能提升游戏性能
 */
public class E8_Battery {
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public void testGameState(Context c, int newState) {
        GameManager gameManager = c.getSystemService(GameManager.class);
        int currState = gameManager.getGameMode();
        gameManager.setGameState(new GameState(false, newState));
    }

    public void test() {
        Process.getExclusiveCores();
        MidiManager m;
        MidiDevice device;
        //device.
    }
}
