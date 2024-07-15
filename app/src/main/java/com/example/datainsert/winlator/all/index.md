[TOC]
# 概述
- smali.zip https://wwqv.lanzout.com/i76AU1bepcgf. 包含全部额外内容的smali压缩包。
- smali压缩包以及下面教程中的smali均使用原版包名`com.winlator`，请根据情况自行调整。
- 由于 winlator日后可能会更新，此页面的内容随时有可能过时。目前对应winlator版本：1.1



# 前台服务，防止挂后台闪退
1. 下载smali压缩包并添加。
2. manifest添加权限和service。
```
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    ...
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
    <uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />
    ...
    <application
    ...
      <service
      android:name="com.example.datainsert.winlator.all.ForeGroundService"
      android:enabled="true"
      android:exported="false" />

```
3. 插入启动和停止service的代码。
```
# XServerDisplayActivity onCreate最后调用
invoke-static {p0}, Lcom/example/datainsert/winlator/all/ForeGroundService;->start(Landroid/content/Context;)V

# MainActivity onCreate最后（要在:goto 和:cond之后） 调用
invoke-static {p0}, Lcom/example/datainsert/winlator/all/ForeGroundService;->stop(Landroid/content/Context;)V
```

# 修复鼠标移动不准
com.winlator.widget.TouchpadView缺少一个矩阵变换，目前是直接按照安卓屏幕分辨率的移动距离，直接在容器分辨率（通常更小）上移动了相同距离。
1. 添加矩阵
2. finger类 新建时 记录xserver单位下的手指坐标。原来安卓单位的坐标在其他地方有用到不能变。至于实时坐标只记录安卓单位的，要用xserver单位的现算就行了。
   
3. 更新指针坐标时，调用finger.nowPointer() 获取xserver单位的光标坐标。由于缺少相关框架，所以没法实现绝对位置点击。默认容器画面是在左上角（0,0）
3. finger类 新建时，传入当前pointer的xy，记录下来。之后获取最新pointer位置时，计算最新pointer xy与起始pointer xy之差，不用delta了。最新pointer xy 是手指xy安卓单位转为xserver单位后的值。
4. releasebutton的两个方法，延迟30删掉，改为立刻执行
5. 绝对位置点击的话，双击有问题，因为按下时大概率会移动，所以第二次是先移动再点击，不会构成双击事件了。所以按下时要判断一下，只有与当前pointer位置不超过最大点击距离的时候才会移动。

