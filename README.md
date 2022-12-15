#### Excel 常用指令
1. 分离整数
=INT(A1)=A1
2. 每5行取平均
=IF(MOD(ROW(B1),5)=1,SUM(B1:B5)/5,"")
3. 每500行取平均
=IF(MOD(ROW(B1),500)=1,SUM(B1:B500)/500,"")

#### 自动化输入
连接手机后，在脚本中定义落点的指针位置（手机需要打开开发者选项，开启指针位置输入），
接着在命令行执行 anain.sh 脚本，脚本基于 sleep，定时较粗糙。

#### Already Done
##### 获取用户行为日志
1. Session: 用户从打开屏幕到关闭屏幕这段时间内记为一次交互Session。
   一次Session内会记录的由用户产生的交互行为信息包括
    - 用户打开的应用包名(Package Name)以及该包下具体的页面类名（Activity Class Name）
    - 用户交互时产生的生命周期事件，包括「手机启动 -> 屏幕亮起 -> 用户解锁 -> 屏幕熄灭 -> 手机关机」
    - Activity Overview: 用户在各个App上停留的时间（用户在各个APP的使用时间）
    - Session Overview: Session 开始与结束的时间点，Session 持续时间（Duration），用户在所有应用上停留的总时间

![1_app_usage_overview.png][1_app_usage_overview.png]

Example file find in here: [Example File](readme/app_usage_file_example.xlsx)

##### TODO List
1. 支持发送心跳包到服务器，方便记录用户存活时长（如果最后24小时无法保证时）
2. 用户使用app的信息，以及用户在不同app的使用时长
3. 用户从打开屏幕到关闭屏幕的总时长，用户每次打开屏幕的时间
4. 用户关机频率
5. 用户充电时机


[1_app_usage_overview.png]: readme/pics/1_app_usage_overview.png