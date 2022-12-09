##### 生成jks密钥指令
keytool -genkey -alias [platform] -keypass 123456 -keyalg RSA -keysize 1024 -validity 36500 -keystore [/Users/gara/Desktop/projects/HWStatistics/signature/platform.jks] -storepass 123456

##### java
java -jar signapk.jar platform.x509.pem platform.pk8 app-debug.apk app-debug-signed.apk


##### Excel 
1. 分离整数
=INT(A1)=A1
2. 每5行取平均
=IF(MOD(ROW(B1),5)=1,SUM(B1:B5)/5,"")
3. 每500行取平均
=IF(MOD(ROW(B1),500)=1,SUM(B1:B500)/500,"")

##### 自动化输入
连接手机后，在脚本中定义落点的指针位置（手机需要打开开发者选项，开启指针位置输入），
接着在命令行执行 anain.sh 脚本，脚本基于 sleep，定时较粗糙。

##### TODO List
1. 支持发送心跳包到服务器，方便记录用户存活时长（如果最后24小时无法保证时）
2. 用户使用app的信息，以及用户在不同app的使用时长
3. 用户从打开屏幕到关闭屏幕的总时长，用户每次打开屏幕的时间
4. 用户关机频率
5. 用户充电时机