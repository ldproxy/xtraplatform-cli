@echo off
REM Windows batch file conversion of build.sh

SET OUT_DIR=build
SET PLATFORM=windows
SET EXT=lib

if not exist %OUT_DIR% mkdir %OUT_DIR%

cd %OUT_DIR%

echo lib

ls -Recurse %JAVA_HOME%/include

REM static
cl.exe /c /W4 /I./ /I%JAVA_HOME%/include /I%JAVA_HOME%/include/%PLATFORM% /Folibxtracfg.obj ../wrapper/libxtracfg.c
copy libxtracfgjni_static.lib libxtracfg.lib
lib.exe /OUT:libxtracfg.lib libxtracfg.lib libxtracfg.obj

certutil -hashfile libxtracfg.lib SHA1 > libxtracfg.sha1sum.tmp
REM Extract just the hash from certutil output (it includes headers/footers)
findstr /v "SHA1 CertUtil" libxtracfg.sha1sum.tmp > libxtracfg.sha1sum
del libxtracfg.sha1sum.tmp
copy libxtracfg.sha1sum ..\..\go\xtracfg\

echo test

REM static
cl.exe /I./ /Fetest.exe ../test/main.c libxtracfg.lib

cd ..
