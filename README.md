此仓库为fork，用于尝试添加一些额外功能。
原仓库：https://github.com/brunodev85/winlator

- MT的文件提供器
  - 自带文件提供器，无需再次MT注入。可使用SAF访问rootfs。
- 全局异常捕获
  - 当winlator app闪退时，java端的异常会被记录在 外部存储/Download/Winlator/crash 文件夹中。
- 安卓快捷方式
  - 主界面 - 快捷方式页面，可对某个快捷方式添加到安卓桌面。长按app图标选中并启动。
- 安卓输入法输入
  - 支持完整Unicode（中文）输入（会多输入一个代替keycode原本的keysym以及backspace。）
- logcat日志
  - 主界面 - 设置页面开启。开启后，将logcat输出保存到 外部存储/Download/Winlator/logcat 文件夹中。
- PRoot简易终端
  - 主界面 - 设置页面开启。开启后，启动容器，返回键显示左侧菜单中进入。可查看PRoot进程内的输出或向其输入命令。
  - 目前（6.1）WINEDEBUG会被默认设置成-all，想查看wine输出需要容器设置中手动覆盖，如WINEDEBUG=fixme+all,err+all
  - 不支持快捷键指令
- 屏幕方向旋转
  - 启动容器，返回键显示左侧菜单中点击，可旋转屏幕方向。
- 绝对位置点击
  - 启动容器，返回键显示左侧菜单 - 输入控制 中开启。手指在某一个位置点击后，鼠标会移动到手指位置。

