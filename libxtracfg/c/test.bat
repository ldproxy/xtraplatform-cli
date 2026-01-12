REM @echo off

set DIR=%~dp0
set DIR=%DIR:~0,-1%
set DIR=D:\\a\\xtraplatform-cli\\xtraplatform-cli\\libxtracfg\\c

cd build

cmd.exe /c test.exe "{\"command\": \"info\", \"source\": \"%DIR%\", \"debug\": \"true\", \"verbose\": \"true\"}"

ldd test.exe
