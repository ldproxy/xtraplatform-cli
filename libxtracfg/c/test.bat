REM @echo off

set DIR=%~dp0
set DIR=%DIR:~0,-1%

cp C:\hostedtoolcache\windows\graalvm-jdk-21_windows-x64_bin\21.0.0\x64\graalvm-jdk-21.0.9+7.1/bin/server\jvm.dll build\

ls -l build

cd build

cmd.exe /c test.exe "{\"command\": \"info\", \"source\": \"./\", \"debug\": \"true\", \"verbose\": \"true\"}"

dumpbin /dependents test.exe

ldd test.exe
