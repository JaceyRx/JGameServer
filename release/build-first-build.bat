@echo off
chcp 65001

:: 1.第一次打包该项目需要整个项目打包一次。不然子模块将无法打包
echo 编译 root pom 文件
cd ..\
start mvn clean install -DskipTests

::pause


