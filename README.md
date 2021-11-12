#### 生成jks密钥指令
keytool -genkey -alias [platform] -keypass 123456 -keyalg RSA -keysize 1024 -validity 36500 -keystore [/Users/gara/Desktop/projects/HWStatistics/signature/platform.jks] -storepass 123456

#### java
java -jar signapk.jar platform.x509.pem platform.pk8 app-debug.apk app-debug-signed.apk

mount -o rw,remount -t auto /

分离整数
=INT(A1)=A1

每5行取平均
=IF(MOD(ROW(B1),5)=1,SUM(B1:B5)/5,"")

=IF(MOD(ROW(B1),500)=1,SUM(B1:B500)/500,"")