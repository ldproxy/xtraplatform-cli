REM @echo off

set DIR=%~dp0
set DIR=%DIR:~0,-1%

ls -l build

cmd.exe /c build\test.exe "{\"command\": \"info\", \"source\": \"./\", \"debug\": \"true\", \"verbose\": \"true\"}"

dumpbin /dependents build\test.exe

ldd build\test.exe
