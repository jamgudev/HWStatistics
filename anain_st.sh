#!/bin/bash
echo "开始自动化进出教室"
quitTime=10
echo "课程数据量是否很大？(y/n)"
read waitTime
if [ "$waitTime" = "y" ];then
	quitTime=25 #数据量多的课程
elif [ "$waitTime" = "n" ]; then
	quitTime=10 #数据量少的课程
else
	echo "输入错误"
	exit 0
fi
enterRoomX=0 #进入教室按钮坐标
enterRoomY=0
quitRoomX=0 #确认退出按钮坐标
quitRoomY=0
echo "选择机型：1（华为M2平板），2（小米平板2），3（Nexus5），4（红米2A）"
read phone
if [ "$phone" = "1" ];then
	echo "当前选中机型：华为M2平板"
	enterRoomX=400
	enterRoomY=500
	quitRoomX=1300
	quitRoomY=610
elif [ "$phone" = "2" ];then
	echo "当前选中机型：小米平板2"
	enterRoomX=400
	enterRoomY=640
	quitRoomX=1480
	quitRoomY=820
elif [ "$phone" = "3" ];then
	echo "当前选中机型：Nexus5"
	enterRoomX=200
	enterRoomY=900
	quitRoomX=1200
	quitRoomY=610
elif [ "$phone" = "4" ];then
	echo "当前选中机型：红米2A"
	enterRoomX=200
	enterRoomY=600
	quitRoomX=1000
	quitRoomY=410
else
	echo "错误：未知机型"
	exit 0
fi
echo "开始自动化进出教室"
num=1
while [ "1" = "1" ] #死循环
do
	time=$(date +%m-%d--%H:%M:%S)
	echo "时间：$time  第 $num 次进入教室"
	num=$(($num + 1))
	adb shell input tap $enterRoomX $enterRoomY #进入教室
	sleep $quitTime #进教室休眠时间
	adb shell input keyevent 4 #退出教室
	sleep 1
	adb shell input tap $quitRoomX $quitRoomY #退出教室确认
	sleep 4
done