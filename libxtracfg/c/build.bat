@echo off
REM Windows batch file conversion of build.sh

SET OUT_DIR=build
SET PLATFORM=win32
SET EXT=lib

if not exist %OUT_DIR% mkdir %OUT_DIR%

cd %OUT_DIR%

echo lib

ls -l -R %JAVA_HOME%

REM static
cl.exe /c /W4 /I./ /I%JAVA_HOME%/include /I%JAVA_HOME%/include/%PLATFORM% /Folibxtracfg.obj ../wrapper/libxtracfg.c
REM copy libxtracfgjni_static.lib libxtracfg.lib
lib.exe /OUT:libxtracfg.lib /VERBOSE libxtracfg.obj libxtracfgjni_static.lib

certutil -hashfile libxtracfg.lib SHA1 > libxtracfg.sha1sum.tmp
REM Extract just the hash from certutil output (it includes headers/footers)
findstr /v "SHA1 CertUtil" libxtracfg.sha1sum.tmp > libxtracfg.sha1sum
del libxtracfg.sha1sum.tmp
copy libxtracfg.sha1sum ..\..\go\xtracfg\

echo test
ls -l

REM static
cl.exe /I./ /Fetest.exe ../test/main.c libxtracfg.lib 
REM %JAVA_HOME%/lib/jvm.lib

ls -l

cd ..
