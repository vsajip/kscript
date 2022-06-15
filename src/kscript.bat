@echo off

for /f "tokens=* USEBACKQ" %%o in (`where kscript.bat`) do set ABS_KSCRIPT_PATH=%%o
set JAR_PATH=%ABS_KSCRIPT_PATH:~0,-4%.jar

set COMMAND=kotlin -classpath %JAR_PATH% kscript.app.KscriptKt windows %*

set RESULT=1
set RESULT=
set errorlevel=1
set errorlevel=

for /f "tokens=* USEBACKQ" %%o in (`%COMMAND%`) do set RESULT=%%o

rem https://stackoverflow.com/questions/10935693/foolproof-way-to-check-for-nonzero-error-return-code-in-windows-batch-file/10936093#10936093
if errorlevel 1 (
    echo Execution failure: %errorlevel%
    exit /b %errorlevel%
)

if NOT [%RESULT%]==[] (
    %RESULT%
)
