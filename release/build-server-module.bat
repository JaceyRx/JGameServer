@echo off
chcp 65001

echo 打包gm服务器
cd gm
start mvn clean install -DskipTests

echo 打包logic服务器
cd ..\logic
start mvn clean install -DskipTests

echo 打包gateway服务器
cd ..\gateway
start mvn clean install -DskipTests

echo 打包chat服务器
cd ..\chat
start mvn clean install -DskipTests

echo 打包battle服务器
cd ..\battle
start mvn clean install -DskipTests

echo maven编译均已启动，请等待各类服务器编译成功后手工关闭各个窗口，注意查看是否编译成功
pause