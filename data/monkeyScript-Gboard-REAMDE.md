##### Tap(x, y, duration)
ap(x坐标, y坐标, duration(单位ms))：duration 指的是，按下的持续时间。

##### Drag(x1, y1, x2, y2, duration)
滑动操作，在duration时间内从（x1, y1）滑到（x2, y2）

##### UserWait(duration)
用户等待多久时间

##### 怎么使用
把脚本文件传到手机上：adb push source_add(脚本文件地址) dest_add(目标文件地址)

执行脚本文件指令：adb shell monkey -p package_name –v 100 –f /sdcard/monkey_script.txt
不指定包名：adb shell monkey -f .. -v 100

-v  为执行的次数，一定要写，不然不会执行。

-f  为要执行的文件，其路径

-p 包名

adb shell monkey -p tv.danmaku.bili –v 1 –f /sdcard/monkey_script.txt 

##### adb 查看包名
adb shell dumpsys activity activities

##### 脚本注释
typ=user #指明脚本类型

count = 10 #脚本执行次数

speed = 1.0 #命令执行速率

start data >> #用户脚本入口，下面是用户自己编写的脚本