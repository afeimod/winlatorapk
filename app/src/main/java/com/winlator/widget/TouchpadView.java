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
    public static Matrix matrixA2X = new Matrix();

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
    private final float[] xform = XForm.getInstance();

    private boolean currentIsFullScreen; //存一份备份

    public TouchpadView(Context context, XServer xServer) {
        super(context);
        this.xServer = xServer;
        setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setClickable(true);
        setFocusable(false);
        setFocusableInTouchMode(false);
        updateXform(AppUtils.getScreenWidth(), AppUtils.getScreenHeight(), xServer.screenInfo.width, xServer.screenInfo.height);



        currentIsFullScreen = xServer.getRenderer().isFullscreen();

        //根据安卓屏幕分辨率和容器分辨率设置一个矩阵。照着exa那样就行了
        DisplayMetrics displayMetrics = new DisplayMetrics();
        context.getSystemService(WindowManager.class).getDefaultDisplay().getMetrics(displayMetrics);
        matrixA2X = refreshMatrix(xServer.screenInfo.width, xServer.screenInfo.height, displayMetrics.widthPixels, displayMetrics.heightPixels);

    }

    /**
     * 生成安卓单位的坐标到xserver单位的坐标的矩阵。参数：xserver的宽高，安卓视图的宽高
     */
    private Matrix refreshMatrix(int wX, int hX, int wA, int hA) {

        float a_div_x_scaleW = 1f*wA/wX, a_div_x_scaleH = 1f*hA/hX,
                whRatioX = 1f*wX/hX, whRatioA = 1f*wA/hA;
        float viewTranX=0,viewTranY=0,
                x11TranX = 0, x11TranY = 0;

        if(!currentIsFullScreen){
            //等比全屏时，最终值选择scaleXY中较小的一个
            a_div_x_scaleW = Math.min(a_div_x_scaleW,a_div_x_scaleH);
            a_div_x_scaleH = a_div_x_scaleW;

            //等比全屏时，比如左右黑屏，需要先将xserver移到左上角原点，即向左移动一段距离 (要先计算出scale才行）
            x11TranX = (wA/a_div_x_scaleW - wX)/2;
            x11TranY = (hA/a_div_x_scaleH - hX)/2;

            //等比全屏时，比如左右黑边，则安卓单位的view需要往右移动一段距离
            viewTranX = (wA-wX*a_div_x_scaleW)/2;
            viewTranY = (hA-hX*a_div_x_scaleH)/2;



        }

        Matrix newMatrix = new Matrix();
        newMatrix.postTranslate(-viewTranX,-viewTranY);
        newMatrix.postScale(1f/a_div_x_scaleW,1f/a_div_x_scaleH);
        return newMatrix;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateXform(w, h, xServer.screenInfo.width, xServer.screenInfo.height);
        //解决旋转屏幕后矩阵不对的问题
        matrixA2X = refreshMatrix(xServer.screenInfo.width, xServer.screenInfo.height, w, h);
    }

    private void updateXform(int outerWidth, int outerHeight, int innerWidth, int innerHeight) {
        Viewport viewport = new Viewport();
        viewport.update(outerWidth, outerHeight, innerWidth, innerHeight);

        float invAspect = 1.0f / viewport.aspect;
        if (!xServer.getRenderer().isFullscreen()) {
            XForm.makeTranslation(xform, -viewport.x, -viewport.y);
            XForm.scale(xform, invAspect, invAspect);
        }
        else XForm.makeScale(xform, invAspect, invAspect);
    }

    private static class Finger {
        private float x;
        private float y;
        private float startX;
        private float startY;
        private float lastX;
        private float lastY;
        private final long touchTime;
        private int pointerStartX, pointerStartY; //记录光标起始位置
        private final int startXInXUnit, startYInXUnit; //单独拿出来一个记录xserver单位的起始坐标，原来安卓单位的在其他地方有用到不能变。至于实时坐标只记录安卓单位的，要用xserver单位的现算就行了。


        public Finger(float ax, float ay, int psx, int psy) {
//            float[] transformedPoint = XForm.transformPoint(xform, x, y);
//            this.x = this.startX = this.lastX = transformedPoint[0];
//            this.y = this.startY = this.lastY = transformedPoint[1];
//            touchTime = System.currentTimeMillis();
            //记录光标起始位置
            pointerStartX = psx;
            pointerStartY = psy;

            this.lastX = ax;
            this.startX = ax;
            this.x = ax;
            this.y = ay;
            this.lastY = ay;
            this.startY = ay;

            //转换为xserver单位
            float[] mapPoints = mapPoints(matrixA2X, ax, ay);
            startXInXUnit = (int) mapPoints[0];
            startYInXUnit = (int) mapPoints[1];

            touchTime = System.currentTimeMillis();
        }

        public void update(float x, float y) {
//            lastX = this.x;
//            lastY = this.y;
//            float[] transformedPoint = XForm.transformPoint(xform, x, y);
//            this.x = transformedPoint[0];
//            this.y = transformedPoint[1];
            //为啥要用dp？改回px
            lastX = this.x;
            lastY = this.y;
            this.x = x;
            this.y = y;
        }

        private int[] deltaXY(float sensitivity){
            float[] xyInXUnit = mapPoints(TouchpadView.matrixA2X,x,y);
            float[] lastXYInXUnit = mapPoints(TouchpadView.matrixA2X,lastX,lastY);

            float dx = (xyInXUnit[0] - lastXYInXUnit[0])  * sensitivity;
            float dy = (xyInXUnit[1] - lastXYInXUnit[1])  * sensitivity;
            return new int[]{(int) dx, (int) dy};
        }

        /**
         * @deprecated 请使用deltaXY
         */
        @Deprecated
        private int deltaX(float sensitivity) {
            float dx = (x - lastX) * sensitivity;
            return (int)(x <= lastX ? Math.floor(dx) : Math.ceil(dx));
        }

        @Deprecated
        private int deltaY(float sensitivity) {
            float dy = (y - lastY) * sensitivity;
            return (int)(y <= lastY ? Math.floor(dy) : Math.ceil(dy));
        }

        //获取光标应该在的最新位置
        private int[] nowPointer(float sensitivity) {
            float[] nowXYInXUnit = mapPoints(matrixA2X, x, y);
            return new int[]{(int) (pointerStartX + (nowXYInXUnit[0] - startXInXUnit) * sensitivity),
                    (int) (pointerStartY + (nowXYInXUnit[1] - startYInXUnit) * sensitivity)}
                    ;
        }
        /**
         * 用于拖拽时，拖拽手指松开后，修正固定手指记录的指针位置
         */
        public void changePointerStartPos(int psx, int psy){
            pointerStartX = psx;
            pointerStartY = psy;
        }

        /**
         * 用于二指右键，只松开了一根手指后，修改第一根手指的startX，以免第一根手指松开时触发左键点击事件
         */
        public void changeStartPos(float sx,float sy){
            startX  =sx;
            startY = sy;
        }

        private boolean isTap() {
            return (System.currentTimeMillis() - touchTime) < 200 && travelDistance() < MAX_TAP_TRAVEL_DISTANCE;
        }

        private float travelDistance() {
            return (float)Math.hypot(x - startX, y - startY);
        }
    }

    public static float[] mapPoints(Matrix matrix, float ax, float ay) {
        float[] testFloats = new float[]{ax,ay};
        TouchpadView.matrixA2X.mapPoints(testFloats);
        return testFloats;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int actionIndex = event.getActionIndex();
        int pointerId = event.getPointerId(actionIndex);
        int actionMasked = event.getActionMasked();
        if (pointerId >= MAX_FINGERS) return true;

        //时刻刷新检测拉伸全屏状态是否有变化吧
        if(xServer.getRenderer().isFullscreen() != currentIsFullScreen){
            currentIsFullScreen = !currentIsFullScreen;
            matrixA2X = refreshMatrix(xServer.screenInfo.width, xServer.screenInfo.height, getWidth(), getHeight());
        }

        switch (actionMasked) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                if (event.isFromSource(InputDevice.SOURCE_MOUSE)) return true;
                scrollAccumY = 0;
                scrolling = false;
                handleFingerDown(event.getX(actionIndex), event.getY(actionIndex));
                fingers[pointerId] = new Finger(event.getX(actionIndex), event.getY(actionIndex), xServer.pointer.getX(), xServer.pointer.getY());
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

        float max_tap_dist_in_x_unit = 20f;//Finger.mapPoints(matrixA2X, MAX_TAP_TRAVEL_DISTANCE, 0)[0];
        float[] pointerPos = new float[]{xServer.pointer.getX(), xServer.pointer.getY()};
        float[] pressPos = mapPoints(matrixA2X, pressX, pressY);
        //手指位置与当前指针位置距离超过了点击距离，并且是绝对点击，则将指针移动到手指位置
        if (Math.pow(pressPos[0] - pointerPos[0], 2) + Math.pow(pressPos[1] - pointerPos[1], 2) > Math.pow(max_tap_dist_in_x_unit, 2)) {
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
                //如果没松开的那根手指 是鼠标左键的手指
                else if(finger2!=null && fingerPointerButtonLeft == finger2){
                    finger2.changePointerStartPos(xServer.pointer.getX(),xServer.pointer.getY());
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

    private void handleFingerMove(Finger finger) {
        boolean skipPointerMove = false;

        Finger anoFinger = numFingers == 2 ? findSecondFinger(finger) : null;
        if (anoFinger != null) {
            float lastDistance = (float)Math.hypot(finger.lastX - anoFinger.lastX, finger.lastY - anoFinger.lastY);
            float currDistance = (float)Math.hypot(finger.x - anoFinger.x, finger.y - anoFinger.y);

            //如果两个手指挨的较近，则滚动
            if (currDistance < MAX_TWO_FINGERS_SCROLL_DISTANCE) {
                scrollAccumY += ((finger.y + anoFinger.y) * 0.5f) - (finger.lastY + anoFinger.lastY) * 0.5f;

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
                     anoFinger.travelDistance() < MAX_TAP_TRAVEL_DISTANCE) {
                pressPointerButtonLeft(finger);
                skipPointerMove = true;
            }
        }

        if (!scrolling && numFingers <= 2 && !skipPointerMove) {
            float distance = (float)Math.hypot(finger.x - finger.lastX, finger.y - finger.lastY);
            float sensitivity = this.sensitivity * Math.min(distance, 1.0f);
//            int dx = finger.deltaX(sensitivity);
//            int dy = finger.deltaY(sensitivity);
            int[] dxy = finger.deltaXY(sensitivity);
            int dx = dxy[0];// finger.deltaX();
            int dy = dxy[1];//finger.deltaY();

            if (xServer.cursorLocker.getState() == CursorLocker.State.LOCKED) {
                WinHandler winHandler = xServer.getWinHandler();
                winHandler.mouseEvent(MouseEventFlags.MOVE, dx, dy, 0);
                Log.d(TAG, "handleFingerMove: dx,dy"+ Arrays.toString(dxy));
            }
            else {
                if(isRelativeOnStart){
                    xServer.injectPointerMoveDelta(dx, dy);
                    Log.d(TAG, "handleFingerMove: dx,dy"+ Arrays.toString(dxy));
                }else{
                    //改为设置绝对位置
                    int[] nowPointer = finger.nowPointer(sensitivity);
                    xServer.injectPointerMove(nowPointer[0], nowPointer[1]);
                }
            }
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
