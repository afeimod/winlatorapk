package com.winlator.widget;

import android.content.Context;
import android.graphics.Matrix;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.winlator.core.AppUtils;
import com.winlator.core.CursorLocker;
import com.winlator.math.XForm;
import com.winlator.renderer.GLRenderer;
import com.winlator.renderer.Viewport;
import com.winlator.winhandler.MouseEventFlags;
import com.winlator.winhandler.WinHandler;
import com.winlator.xserver.Pointer;
import com.winlator.xserver.XServer;

import java.lang.reflect.Field;
import java.util.Arrays;

public class TouchpadView extends View {
    private static final byte MAX_FINGERS = 4;
    private static final short MAX_TWO_FINGERS_SCROLL_DISTANCE = 350;
    private static final byte MAX_TAP_TRAVEL_DISTANCE = 10;
    private static final String TAG = "TouchpadView";
    public static boolean isRelativeOnStart=true;

    private final Finger[] fingers = new Finger[MAX_FINGERS];
    private byte numFingers = 0;
    private float sensitivity = 1.0f;
    private boolean pointerButtonLeftEnabled = true;
    private boolean pointerButtonRightEnabled = true;
    private Finger fingerPointerButtonLeft;
    private Finger fingerPointerButtonRight;
    private float scrollAccumY = 0;
    private boolean scrolling = false;
    private final XServer xServer;
    private Runnable fourFingersTapCallback;
    /** 这个就相当于那个与aToXMatrix了吧？ */
    private final float[] xform = XForm.getInstance();
    private boolean isFullScreen;


    public TouchpadView(Context context, XServer xServer) {
        super(context);
        this.xServer = xServer;
        setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setClickable(true);
        setFocusable(false);
        setFocusableInTouchMode(false);
        updateXform(AppUtils.getScreenWidth(), AppUtils.getScreenHeight(), xServer.screenInfo.width, xServer.screenInfo.height);

        isFullScreen = xServer.getRenderer().isFullscreen();
        //根据安卓屏幕分辨率和容器分辨率设置一个矩阵。照着exa那样就行了
//        DisplayMetrics displayMetrics = new DisplayMetrics();
//        context.getSystemService(WindowManager.class).getDefaultDisplay().getMetrics(displayMetrics);
//        matrixA2X = refreshMatrix(xServer.screenInfo.width, xServer.screenInfo.height, displayMetrics.widthPixels, displayMetrics.heightPixels);

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateXform(w, h, xServer.screenInfo.width, xServer.screenInfo.height);
    }

    private void updateXform(int outerWidth, int outerHeight, int innerWidth, int innerHeight) {
        //它这个Viewport忽略了拉伸全屏的情况（全屏时scaleX和Y是不同的，不能用同一个）
        float a_div_x_scaleW = 1f*outerWidth/innerWidth, a_div_x_scaleH = 1f*outerHeight/innerHeight;
        float viewTranX=0, viewTranY=0;

        if(!isFullScreen){
            //等比全屏时，最终值选择scaleXY中较小的一个
            a_div_x_scaleW = Math.min(a_div_x_scaleW,a_div_x_scaleH);
            a_div_x_scaleH = a_div_x_scaleW;

            //等比全屏时，比如左右黑边，则安卓单位的view需要往右移动一段距离
            viewTranX = (outerWidth-innerWidth*a_div_x_scaleW)/2;
            viewTranY = (outerHeight-innerHeight*a_div_x_scaleH)/2;
        }

        if (!xServer.getRenderer().isFullscreen()) {
            XForm.makeTranslation(xform, -viewTranX, -viewTranY);
            XForm.scale(xform, 1f/a_div_x_scaleW, 1f/a_div_x_scaleH);
        }
        else XForm.makeScale(xform, 1f/a_div_x_scaleW, 1f/a_div_x_scaleH);
    }

    private class Finger {
        //x, startX, lastX大小现在都是x单位的.
        // 从float改成int，这样可以保证移动位置的过程，不会出现误差：
        // 如果两次float有变化，但整数位大小相同，那么本次不移动。
        // 直到两次float整数位不同了，才移动一次。然后再直到下次整数位不同再移动
        // 不管移动多远，误差只有最后那次移动忽略掉的小数部分。
        private int x;
        private int y;
        private int startX;
        private int startY;
        private int lastX;
        private int lastY;
        private final long touchTime;

        public Finger(float x, float y) {
            float[] transformedPoint = XForm.transformPoint(xform, x, y);
            this.x = this.startX = this.lastX = (int) transformedPoint[0];
            this.y = this.startY = this.lastY = (int) transformedPoint[1];
            touchTime = System.currentTimeMillis();
        }

        public void update(float x, float y) {
            lastX = this.x;
            lastY = this.y;
            float[] transformedPoint = XForm.transformPoint(xform, x, y);
            this.x = (int) transformedPoint[0];
            this.y = (int) transformedPoint[1];
        }

        private int deltaX(float sensitivity) {
            float dx = (x - lastX) * sensitivity;
            return (int)(x <= lastX ? Math.floor(dx) : Math.ceil(dx));
        }

        private int deltaY(float sensitivity) {
            float dy = (y - lastY) * sensitivity;
            return (int)(y <= lastY ? Math.floor(dy) : Math.ceil(dy));
        }

        /**
         * 用于二指右键，只松开了一根手指后，修改第一根手指的startX，以免第一根手指松开时触发左键点击事件
         */
        public void changeStartPos(float sx,float sy){
            startX  = (int) sx;
            startY = (int) sy;
        }

        private boolean isTap() {
            return (System.currentTimeMillis() - touchTime) < 300 && travelDistance() < MAX_TAP_TRAVEL_DISTANCE;
        }

        private float travelDistance() {
            return (float)Math.hypot(x - startX, y - startY);
        }
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int actionIndex = event.getActionIndex();
        int pointerId = event.getPointerId(actionIndex);
        int actionMasked = event.getActionMasked();
        if (pointerId >= MAX_FINGERS) return true;

        switch (actionMasked) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                //时刻刷新检测拉伸全屏状态是否有变化吧
                if(xServer.getRenderer().isFullscreen() != isFullScreen){
                    isFullScreen = !isFullScreen;
                    updateXform(getWidth(), getHeight(), xServer.screenInfo.width, xServer.screenInfo.height);
                }

                if (event.isFromSource(InputDevice.SOURCE_MOUSE)) return true;
                scrollAccumY = 0;
                scrolling = false;
                handleFingerDown(event.getX(actionIndex), event.getY(actionIndex));
                fingers[pointerId] = new Finger(event.getX(actionIndex), event.getY(actionIndex));
                numFingers++;
                break;
            case MotionEvent.ACTION_MOVE:
                if (event.isFromSource(InputDevice.SOURCE_MOUSE)) {
                    float[] transformedPoint = XForm.transformPoint(xform, event.getX(), event.getY());
                    xServer.injectPointerMove((int)transformedPoint[0], (int)transformedPoint[1]);
                }
                else {
                    for (byte i = 0; i < MAX_FINGERS; i++) {
                        if (fingers[i] != null) {
                            int pointerIndex = event.findPointerIndex(i);
                            if (pointerIndex >= 0) {
                                fingers[i].update(event.getX(pointerIndex), event.getY(pointerIndex));
                                handleFingerMove(fingers[i]);
                            }
                            else {
                                handleFingerUp(fingers[i]);
                                fingers[i] = null;
                                numFingers--;
                            }
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                if (fingers[pointerId] != null) {
                    fingers[pointerId].update(event.getX(actionIndex), event.getY(actionIndex));
                    handleFingerUp(fingers[pointerId]);
                    fingers[pointerId] = null;
                    numFingers--;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                for (byte i = 0; i < MAX_FINGERS; i++) fingers[i] = null;
                numFingers = 0;
                break;
        }

        return true;
    }

    /**
     * 手指按下时（此时手指个数尚未增加）
     * <p>
     * 绝对位置点击的话，双击有问题，
     * 因为按下时大概率会移动，所以第二次是先移动再点击，不会构成双击事件了。
     * 所以按下时要判断一下，只有与当前pointer位置不超过最大点击距离的时候才会移动。
     * 数值全部转为xserver单位后再计算（转到安卓单位的话，误差会变大）
     * <p>
     * 应该只有第一根手指才会移动到手指位置，因为第二根往后都是辅助操作，比如右键，拖拽，这些操作都应该以第一根手指的位置为准
     */
    private void handleFingerDown(float pressX, float pressY) {
        if(isRelativeOnStart)
            return;

        if(numFingers>0)
            return;

        float[] pointerPos = new float[]{xServer.pointer.getX(), xServer.pointer.getY()};
        float[] pressPos = XForm.transformPoint(xform, pressX, pressY); //mapPoints(matrixA2X, pressX, pressY);
        //手指位置与当前指针位置距离超过了点击距离，并且是绝对点击，则将指针移动到手指位置
        if (Math.hypot(pressPos[0] - pointerPos[0], pressPos[1] - pointerPos[1]) > MAX_TAP_TRAVEL_DISTANCE) {
            Log.d(TAG, "handleFingerDown: 只有一根手指，此时应该移动到手指位置,坐标变换前="+pressX+", "+pressY+", 坐标变换后="+ Arrays.toString(pressPos));
            xServer.injectPointerMove((int) pressPos[0], (int) pressPos[1]);
        }

    }

    private void handleFingerUp(Finger finger1) {
        switch (numFingers) {
            case 1:
                if (finger1.isTap()) pressPointerButtonLeft(finger1);
                break;
            case 2:
                //两根手指松开一根的时候。这个时候要把光标同步一下

                //如果当前松开的手指是点击事件，且另一根手指还没松开，则右键
                Finger finger2 = findSecondFinger(finger1);
                if (finger2 != null && finger1.isTap()){
                    pressPointerButtonRight(finger1);
                    //屏蔽第一根手指的左键事件
                    finger2.changeStartPos(-100,-100);
                }
                break;
            case 4:
                if (fourFingersTapCallback != null) {
                    for (byte i = 0; i < 4; i++) {
                        if (fingers[i] != null && !fingers[i].isTap()) return;
                    }
                    fourFingersTapCallback.run();
                }
                break;
        }

        releasePointerButtonLeft(finger1);
        releasePointerButtonRight(finger1);
    }

    private void handleFingerMove(Finger finger1) {
        boolean skipPointerMove = false;

        Finger finger2 = numFingers == 2 ? findSecondFinger(finger1) : null;
        if (finger2 != null) {
            float currDistance = (float)Math.hypot(finger1.x - finger2.x, finger1.y - finger2.y);

            //如果两个手指挨的较近，则滚动
            if (currDistance < MAX_TWO_FINGERS_SCROLL_DISTANCE) {
                scrollAccumY += ((finger1.y + finger2.y) * 0.5f) - (finger1.lastY + finger2.lastY) * 0.5f;

                if (scrollAccumY < -100) {
                    xServer.injectPointerButtonPress(Pointer.Button.BUTTON_SCROLL_DOWN);
                    xServer.injectPointerButtonRelease(Pointer.Button.BUTTON_SCROLL_DOWN);
                    scrollAccumY = 0;
                }
                else if (scrollAccumY > 100) {
                    xServer.injectPointerButtonPress(Pointer.Button.BUTTON_SCROLL_UP);
                    xServer.injectPointerButtonRelease(Pointer.Button.BUTTON_SCROLL_UP);
                    scrollAccumY = 0;
                }
                scrolling = true;
            }
            //如果两个手指挨的较远，且当前没有按下左键，且另一根手指固定没懂，则按下鼠标左键，且跳过移动？
            //只有走到这个函数，才能左键拖拽。此时设置的左键finger应该是正在移动的那根手指。另一根手指是固定不动的
            //猜测出问题的原因：循环两个手指，刚读取到第一根手指（不动的那根）时，由于另一根也没动，所以第一根符合这个条件，被认为是应该移动的左键手指了
            //还是刚按下手指时移动的问题。第二根手指按下时距离很远，所以移动到空白处，然后才按下的鼠标左键

            //不对，ano应该是移动的手指，finger是固定的手指
            else if (currDistance >= MAX_TWO_FINGERS_SCROLL_DISTANCE
                    && !xServer.pointer.isButtonPressed(Pointer.Button.BUTTON_LEFT) &&
                     finger2.travelDistance() < MAX_TAP_TRAVEL_DISTANCE) {
                pressPointerButtonLeft(finger1);
                skipPointerMove = true;
            }
        }

        if (!scrolling && numFingers <= 2 && !skipPointerMove) {
            float distance = (float)Math.hypot(finger1.x - finger1.lastX, finger1.y - finger1.lastY);
            float sensitivity = this.sensitivity * Math.min(distance, 1.0f);
            int dx = finger1.deltaX(sensitivity);
            int dy = finger1.deltaY(sensitivity);

            if (xServer.cursorLocker.getState() == CursorLocker.State.LOCKED) {
                WinHandler winHandler = xServer.getWinHandler();
                winHandler.mouseEvent(MouseEventFlags.MOVE, dx, dy, 0);
//                Log.d(TAG, "handleFingerMove: dx,dy"+ Arrays.toString(dxy));
            }
            else xServer.injectPointerMoveDelta(dx, dy);
//            else {
//                if(isRelativeOnStart){
//                    xServer.injectPointerMoveDelta(dx, dy);
////                    Log.d(TAG, "handleFingerMove: dx,dy"+ Arrays.toString(dxy));
//                }else{
//                    //改为设置绝对位置
//                    int[] nowPointer = finger1.nowPointer(sensitivity);
//                    xServer.injectPointerMove(nowPointer[0], nowPointer[1]);
//                }
//            }
        }
    }

    private Finger findSecondFinger(Finger finger) {
        for (byte i = 0; i < MAX_FINGERS; i++) {
            if (fingers[i] != null && fingers[i] != finger) return fingers[i];
        }
        return null;
    }

    private void pressPointerButtonLeft(Finger finger) {
        if (pointerButtonLeftEnabled && !xServer.pointer.isButtonPressed(Pointer.Button.BUTTON_LEFT)) {
            xServer.injectPointerButtonPress(Pointer.Button.BUTTON_LEFT);
            fingerPointerButtonLeft = finger;
        }
    }

    private void pressPointerButtonRight(Finger finger) {
        if (pointerButtonRightEnabled && !xServer.pointer.isButtonPressed(Pointer.Button.BUTTON_RIGHT)) {
            xServer.injectPointerButtonPress(Pointer.Button.BUTTON_RIGHT);
            fingerPointerButtonRight = finger;
        }
    }

    private void releasePointerButtonLeft(final Finger finger) {
        if (pointerButtonLeftEnabled && finger == fingerPointerButtonLeft && xServer.pointer.isButtonPressed(Pointer.Button.BUTTON_LEFT)) {
            postDelayed(() -> {
                xServer.injectPointerButtonRelease(Pointer.Button.BUTTON_LEFT);
                fingerPointerButtonLeft = null;
            }, 30);
        }
    }

    private void releasePointerButtonRight(final Finger finger) {
        if (pointerButtonRightEnabled && finger == fingerPointerButtonRight && xServer.pointer.isButtonPressed(Pointer.Button.BUTTON_RIGHT)) {
            postDelayed(() -> {
                xServer.injectPointerButtonRelease(Pointer.Button.BUTTON_RIGHT);
                fingerPointerButtonRight = null;
            }, 30);
        }
    }

    public void setSensitivity(float sensitivity) {
        this.sensitivity = sensitivity;
    }

    public boolean isPointerButtonLeftEnabled() {
        return pointerButtonLeftEnabled;
    }

    public void setPointerButtonLeftEnabled(boolean pointerButtonLeftEnabled) {
        this.pointerButtonLeftEnabled = pointerButtonLeftEnabled;
    }

    public boolean isPointerButtonRightEnabled() {
        return pointerButtonRightEnabled;
    }

    public void setPointerButtonRightEnabled(boolean pointerButtonRightEnabled) {
        this.pointerButtonRightEnabled = pointerButtonRightEnabled;
    }

    public void setFourFingersTapCallback(Runnable fourFingersTapCallback) {
        this.fourFingersTapCallback = fourFingersTapCallback;
    }

    public boolean onExternalMouseEvent(MotionEvent event) {
        boolean handled = false;
        if (event.isFromSource(InputDevice.SOURCE_MOUSE)) {
            int actionButton = event.getActionButton();
            switch (event.getAction()) {
                case MotionEvent.ACTION_BUTTON_PRESS:
                    if (actionButton == MotionEvent.BUTTON_PRIMARY) {
                        xServer.injectPointerButtonPress(Pointer.Button.BUTTON_LEFT);
                    }
                    else if (actionButton == MotionEvent.BUTTON_SECONDARY) {
                        xServer.injectPointerButtonPress(Pointer.Button.BUTTON_RIGHT);
                    }
                    handled = true;
                    break;
                case MotionEvent.ACTION_BUTTON_RELEASE:
                    if (actionButton == MotionEvent.BUTTON_PRIMARY) {
                        xServer.injectPointerButtonRelease(Pointer.Button.BUTTON_LEFT);
                    }
                    else if (actionButton == MotionEvent.BUTTON_SECONDARY) {
                        xServer.injectPointerButtonRelease(Pointer.Button.BUTTON_RIGHT);
                    }
                    handled = true;
                    break;
                case MotionEvent.ACTION_HOVER_MOVE:
                    float[] transformedPoint = XForm.transformPoint(xform, event.getX(), event.getY());
                    xServer.injectPointerMove((int)transformedPoint[0], (int)transformedPoint[1]);
                    handled = true;
                    break;
                case MotionEvent.ACTION_SCROLL:
                    float scrollY = event.getAxisValue(MotionEvent.AXIS_VSCROLL);
                    if (scrollY <= -1.0f) {
                        xServer.injectPointerButtonPress(Pointer.Button.BUTTON_SCROLL_DOWN);
                        xServer.injectPointerButtonRelease(Pointer.Button.BUTTON_SCROLL_DOWN);
                    }
                    else if (scrollY >= 1.0f) {
                        xServer.injectPointerButtonPress(Pointer.Button.BUTTON_SCROLL_UP);
                        xServer.injectPointerButtonRelease(Pointer.Button.BUTTON_SCROLL_UP);
                    }
                    handled = true;
                    break;
            }
        }
        return handled;
    }
}
