@echo off

for /f %%i in ('where kscript.bat') do set ABS_KSCRIPT_PATH=%%i
set JAR_PATH=%ABS_KSCRIPT_PATH:~0,-4%.jar

rem kotlin -classpath %JAR_PATH% kscript.app.KscriptKt "windows" %*
set RESULT=
for /F "tokens=* USEBACKQ" %%O in (`kotlin -classpath %JAR_PATH% kscript.app.KscriptKt "windows" %*`) do (set RESULT=%%O)

if errorlevel 1 (
   echo "KScript execution failure: %errorlevel%"
   exit /b %errorlevel%
)

%RESULT%
