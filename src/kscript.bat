@echo off

for /f %%i in ('where kscript.bat') do set ABS_KSCRIPT_PATH=%%i
set JAR_PATH=%ABS_KSCRIPT_PATH:~0,-4%.jar

rem kotlin -classpath %JAR_PATH% kscript.app.KscriptKt "windows" %*
set OSTYPE=windows
FOR /F "tokens=* USEBACKQ" %%O IN (`kotlin -classpath %JAR_PATH% kscript.app.KscriptKt  %*`) DO (SET RESULT=%%O)
%RESULT%
