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

#### Features
##### 获取用户行为日志以及对应行为发生时对应的设备信息
1. Session: 用户从打开屏幕到关闭屏幕这段时间内记为一次交互Session。
   一次Session内会记录的由用户产生的交互行为信息包括
    - 用户打开的应用包名(Package Name)以及该包下具体的页面类名（Activity Class Name）
    - 用户交互时产生的生命周期事件，包括「手机启动 -> 屏幕亮起 -> 用户解锁 -> 屏幕熄灭 -> 手机关机」
    - Activity Overview: 用户在各个App上停留的时间（用户在各个APP的使用时间）
    - Session Overview: Session 开始与结束的时间点，Session 持续时间（Duration），用户在所有应用上停留的总时间

![1_app_usage_overview.png][1_app_usage_overview.png]

Example file find in here: [App Usage Example File](readme/app_usage_file_example.xlsx)

2. 用户充电记录: 充电事件和取消充电事件
   记录信息包括：用户充电和取消充电的事件、事件发生时间点，事件发生时手机当前电量

![2_charge_usage_record.png][2_charge_usage_record.png]

Example file find in here: [Charge Usage Example File](readme/charge_usage_example.xlsx)

3. 用户在一次Session过程内产生的PowerData。
   信息主要包括：屏幕亮度、CPU频率、网络状态（2G/3G/4G/5G）、网络速度、内存信息、蓝牙状态、媒体状态等

![3_power_data_record.png][3_power_data_record.png]

Example file find in here: [Power Data Example File](readme/power_usage_example.xlsx)


##### TODO List
1. 支持发送心跳包到服务器，方便记录用户存活时长（如果最后24小时无法保证时） 
2. 用户关机频率 
3. 用户充电时机 
4. 实验对象信息注册
5. 数据文件上传


[1_app_usage_overview.png]: readme/pics/1_app_usage_overview.png
[2_charge_usage_record.png]: readme/pics/2_charge_usage_record.png
[3_power_data_record.png]: readme/pics/3_power_data_record.png 