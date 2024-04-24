package com.example.datainsert.winlator.all.resouces;

import com.example.datainsert.winlator.all.QH;

public class StringEn {
    public static void apply() {
        QH.string.pulse声音选项 = "PulseAudio Server";
        QH.string.pulse声音简介 = "Pulseaudio is used to play audio. This version is extracted from XSDL.";
        QH.string.旋转屏幕选项 = "Rotate Screen";
        QH.string.游戏样式光标选项 = "Use Game Style Cursor";
        QH.string.游戏样式光标选项说明 = "Winlator inherits the xserver bug from exagear, which can't display mouse cursor image sometimes. If cursor drawable is not available, an alternate png image is used." +
                "\n\nIf this option is checked, xserver will first try to get and display the cursor drawable with game's style. However colors will be only black and white, and the dynamic cursor can only be displayed as a static image.";
        QH.string.绝对位置点击选项 = "Cursor moves to where finger presses";
        QH.string.绝对位置点击选项说明 = "If checked, cursor will go to where you finger is when pressing.\n1 finger click = mouse left click.\n1st finger pressing, 2nd finger press and moving = mouse left button drag and drop.\n1st finger pressing, 2nd finger quick tap = mouse right button click.";
        QH.string.pulse按钮立即运行 = "Run now";
        QH.string.pulse按钮立即停止="Stop now";
        QH.string.pulse按钮注意事项="Note";
        QH.string.pulse选项自动运行="Autorun after container started";
        QH.string.pulse注意事项文字="Test only, does not guarantee better sound playback than Alsa." +
                "\n\nWhen starting, the missing dep-libs are automatically unpacked." +
                "These libs were removed from obb originally, so I'm not sure if there is any conflicts." +
                "Click the button to remove these files if there is." ;
        QH.string.pulse删除依赖库="Delete libs";
        QH.string.额外功能 =  "Extra";

        QH.string.选择数据包说明 = "OBB not found. Please select it from local files, or download it from Github, or put the obb file(renamed as %s) into %s and restart the app.\nNote that when manually selecting you cannot see the directory of %s.";
        QH.string.手动选择 = "Manually Select";
        QH.string.从Github下载 = "Download from Github";
        QH.string.退出 = "Exit";
        QH.string.选择OBB结果 = "Selection cancelled$Selected file(%s) is not an obb file for winlator. Please select another one.$Obb (%s) selected. Start decompression. Please don't switch interfaces.";
        QH.string.下载OBB结果 = "Downloading %s. Please don't switch interfaces. Download progress can be checked on the notification bar.$Download completed. Start decompression.$Download failed.";
        QH.string.解压数据包 = "Uncompress OBB";
        QH.string.Obb下载文件名 = "Winlator_OBB";

        QH.string.logcat日志 = "Logcat info";
        QH.string.logcat日志说明 = "Enable/Disable logcat info output, for debug use. output file is stored at /storage/emulated/0/Download/Winlator/logcat .";
        QH.string.proot终端 = "PRoot mini shell";
        QH.string.proot终端说明 = """
                    - If enabled, starting the container -> press the phone's back button -> click option "PRoot shell" to show the shell dialog.
                    
                    - When proot is started, this funtion will ask proot to run /usr/bin/dash and connect to the inputs and outputs of this process.
                    
                    - At the dialog bottom, you can input commands and press enter to send it.Since it doesn't send characters every time \
                    the key is pressed, it doesn't work properly when using text editor, sending shortcut commands, etc.
                    
                    - Currently Winlator(6.1) by default adds environment variable WINEDEBUG=-all to block all output logs of wine. \
                    If you want to see it, please override this environment variable manually in the container settings. \
                    e.g. WINEDEBUG=err+all,fixme+all
                    
                    - At the dialog bottom-right, you can turn on the "Auto Scroll To Bottom" option. Note that when this option is turned on, \
                    users may be interrupted by a new output line while typing commands, please consider turning it off if it happens.
                    """;
        QH.string.proot终端_启动失败_请关闭选项重试 = "Launch failed. Please disable the PRoot shell option in settings and retry.";
        QH.string.proot终端_请先开启选项 = "Unable to get output. Please go back to settings and enable the proot shell option first.";
        QH.string.proot终端_自动滚动到底部 = "Auto Scroll To Bottom";
    }
}
