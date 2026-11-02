@echo off
title DocuSafe - Personal Document Management System
echo ====================================================================
echo Starting DocuSafe Personal Document Management System (PDMS)
echo ====================================================================

REM Ensure JAVA_HOME is set
if "%JAVA_HOME%"=="" (
    if exist "C:\Program Files\Java\jdk-27.0.1.0.3" (
        set "JAVA_HOME=C:\Program Files\Java\jdk-27.0.1.0.3"
    ) else if exist "C:\Program Files\Java\latest" (
        set "JAVA_HOME=C:\Program Files\Java\latest"
    )
)

if "%JAVA_HOME%"=="" (
    echo [ERROR] JAVA_HOME is not set. Please install JDK and set JAVA_HOME.
    pause
    exit /b 1
)

set "PATH=%JAVA_HOME%\bin;%PATH%"

echo [1/3] Checking Java runtime...
java -version

echo.
echo [2/3] Checking MySQL personal_document_db database...
REM Create uploads folder if missing
if not exist "uploads" mkdir uploads

echo.
echo [3/3] Starting Spring Boot Server on http://localhost:8080 ...
if exist "jvm_args.txt" (
    "%JAVA_HOME%\bin\java.exe" @jvm_args.txt com.pdms.PdmsApplication
) else (
    call mvnw.cmd spring-boot:run
)

pause
