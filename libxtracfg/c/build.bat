@echo off
REM Windows batch file conversion of build.sh

SET OUT_DIR=build
SET PLATFORM=win32
SET EXT=lib

if not exist %OUT_DIR% mkdir %OUT_DIR%

cd %OUT_DIR%

echo lib

REM ls -l -R %JAVA_HOME%

REM static
REM cl.exe /c /MT /W4 /I./ /I%JAVA_HOME%/include /I%JAVA_HOME%/include/%PLATFORM% /Folibxtracfg.obj ../wrapper/libxtracfg.c
copy libxtracfgjni_static.lib libxtracfg.lib
REM lib.exe /OUT:libxtracfg.lib /VERBOSE libxtracfg.obj libxtracfgjni_static.lib

certutil -hashfile libxtracfg.lib SHA1 > libxtracfg.sha1sum.tmp
REM Extract just the hash from certutil output (it includes headers/footers)
findstr /v "SHA1 CertUtil" libxtracfg.sha1sum.tmp > libxtracfg.sha1sum
del libxtracfg.sha1sum.tmp
copy libxtracfg.sha1sum ..\..\go\xtracfg\

echo test
ls -l

REM static
cl.exe /MT /I./ /Fetest.exe /I%JAVA_HOME%/include /I%JAVA_HOME%/include/%PLATFORM% ^
 ../wrapper/libxtracfg.c ^
 ../test/main.c ^
 libxtracfgjni_static.lib ^
 C:\hostedtoolcache\windows\graalvm-jdk-21_windows-x64_bin\21.0.0\x64\graalvm-jdk-21.0.9+7.1\lib\svm\clibraries\windows-amd64\libchelper.lib ^
 C:\hostedtoolcache\windows\graalvm-jdk-21_windows-x64_bin\21.0.0\x64\graalvm-jdk-21.0.9+7.1\lib\static\windows-amd64\net.lib ^
 C:\hostedtoolcache\windows\graalvm-jdk-21_windows-x64_bin\21.0.0\x64\graalvm-jdk-21.0.9+7.1\lib\static\windows-amd64\extnet.lib ^
 C:\hostedtoolcache\windows\graalvm-jdk-21_windows-x64_bin\21.0.0\x64\graalvm-jdk-21.0.9+7.1\lib\static\windows-amd64\nio.lib ^
 C:\hostedtoolcache\windows\graalvm-jdk-21_windows-x64_bin\21.0.0\x64\graalvm-jdk-21.0.9+7.1\lib\static\windows-amd64\management_ext.lib ^
 C:\hostedtoolcache\windows\graalvm-jdk-21_windows-x64_bin\21.0.0\x64\graalvm-jdk-21.0.9+7.1\lib\static\windows-amd64\java.lib ^
 C:\hostedtoolcache\windows\graalvm-jdk-21_windows-x64_bin\21.0.0\x64\graalvm-jdk-21.0.9+7.1\lib\static\windows-amd64\sunmscapi.lib ^
 C:\hostedtoolcache\windows\graalvm-jdk-21_windows-x64_bin\21.0.0\x64\graalvm-jdk-21.0.9+7.1\lib\static\windows-amd64\zip.lib ^
 C:\hostedtoolcache\windows\graalvm-jdk-21_windows-x64_bin\21.0.0\x64\graalvm-jdk-21.0.9+7.1\lib\svm\clibraries\windows-amd64\jvm.lib ^
 /link ^
  /NODEFAULTLIB:MSVCRT ^
  /FILEALIGN:4096 ^
  /LIBPATH:./ ^
  /LIBPATH:%JAVA_HOME%/lib ^
  /LIBPATH:%JAVA_HOME%/lib/svm/clibraries ^
  /LIBPATH:%JAVA_HOME%/lib/svm/clibraries/windows-amd64 ^
  /LIBPATH:%JAVA_HOME%/lib/static/windows-amd64 ^
  /WHOLEARCHIVE:libxtracfgjni_static.lib ^
  ncrypt.lib ^
  crypt32.lib ^
  winhttp.lib ^
  psapi.lib ^
  version.lib ^
  advapi32.lib ^
  ws2_32.lib ^
  secur32.lib ^
  iphlpapi.lib ^
  userenv.lib ^
  mswsock.lib
REM /verbose
REM %JAVA_HOME%/lib/jvm.lib

ls -l

cd ..
