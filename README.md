#### 生成jks密钥指令
keytool -genkey -alias [platform] -keypass 123456 -keyalg RSA -keysize 1024 -validity 36500 -keystore [/Users/gara/Desktop/projects/HWStatistics/signature/platform.jks] -storepass 123456

#### java
java -jar signapk.jar platform.x509.pem platform.pk8 app-debug.apk app-debug-signed.apk

mount -o rw,remount -t auto /