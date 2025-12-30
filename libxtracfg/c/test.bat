@echo off

set DIR=%~dp0
set DIR=%DIR:~0,-1%

build\test.exe "{\"command\": \"info\", \"source\": \"%DIR%\", \"debug\": \"true\", \"verbose\": \"true\"}"
