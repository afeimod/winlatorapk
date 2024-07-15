package com.example.datainsert.winlator.all;

import com.winlator.renderer.GLRenderer;
import com.winlator.widget.XServerView;

import java.lang.reflect.Field;

public class UtilsReflect {

    public static int getPid(Process process) {
        int pid = -1;
        try {
            Field pidField = process.getClass().getDeclaredField("pid");
            pidField.setAccessible(true);
            pid = pidField.getInt(process);
            pidField.setAccessible(false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return pid;
    }
}
