#!/bin/bash
echo "开始自动化点击"
# 执行间隔时间
quitTime=1

# 模拟点击的位置
inputX=955
inputY=1714
# 执行次数
num=1
while [ "1" = "1" ] #死循环
do
	time=$(date +%m-%d--%H:%M:%S)
	echo "时间：$time  第 $num 次执行脚本"
	num=$(($num + 1))
	adb shell input tap $inputX $inputY # 模拟点击
	sleep $quitTime
done