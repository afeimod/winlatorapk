package com.winlator.xserver;

import static java.lang.Integer.toHexString;

import android.util.Log;
import android.util.SparseArray;

import java.nio.IntBuffer;

public class CursorManager extends XResourceManager {
    private final SparseArray<Cursor> cursors = new SparseArray<>();
    private final DrawableManager drawableManager;

    public CursorManager(DrawableManager drawableManager) {
        this.drawableManager = drawableManager;
    }

    public Cursor getCursor(int id) {
        return cursors.get(id);
    }

    public Cursor createCursor(int id, short x, short y, Pixmap sourcePixmap, Pixmap maskPixmap) {
        if (cursors.indexOfKey(id) >= 0) return null;
        Drawable drawable = drawableManager.createDrawable(0, sourcePixmap.drawable.width, sourcePixmap.drawable.height, sourcePixmap.drawable.visual);
        Cursor cursor = new Cursor(id, x, y, drawable, sourcePixmap.drawable, maskPixmap != null ? maskPixmap.drawable : null);
        cursors.put(id, cursor);
        triggerOnCreateResourceListener(cursor);
        return cursor;
    }

    public void freeCursor(int id) {
        triggerOnFreeResourceListener(cursors.get(id));
        cursors.remove(id);
    }

    private static boolean isEmptyMaskImage(Drawable maskImage) {
        IntBuffer maskData = maskImage.getData().asIntBuffer();
        boolean result = true;
        for (int i = 0; i < maskData.capacity(); i++) {
            if (maskData.get(i) != 0x000000) {
                result = false;
                break;
            }
        }
        return result;
    }

    public void recolorCursor(Cursor cursor, byte foreRed, byte foreGreen, byte foreBlue, byte backRed, byte backGreen, byte backBlue) {
        String str = "maskImage!=null: "+(cursor.maskImage != null);
        if (cursor.maskImage != null) {
            boolean visible = !isEmptyMaskImage(cursor.maskImage);
            str += ", visible="+visible;
            cursor.setVisible(visible);
            if (visible) cursor.cursorImage.drawAlphaMaskedBitmap(foreRed, foreGreen, foreBlue, backRed, backGreen, backBlue, cursor.sourceImage, cursor.maskImage);
        }
        Log.e("TAG", "recolorCursor: cursorid="+cursors.indexOfValue(cursor)+", "+str
                +String.format(", sourceId=%s, maskId=%s",(cursor.sourceImage!=null?cursor.sourceImage.id:"-1"),(cursor.maskImage!=null?cursor.maskImage.id:"-1"))
         +String.format(", foreRGB=%s,%s,%s, backRGB=%s,%s,%s",
                toHexString(foreRed), toHexString(foreGreen), toHexString(foreBlue),
                toHexString(backRed), toHexString(backGreen), toHexString(backBlue)));
    }
}