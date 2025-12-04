@echo off
REM Maven wrapper script for Photos48 Android project (Windows)

setlocal

set MAVEN_VERSION=3.9.5
set MAVEN_HOME=%USERPROFILE%\.m2\wrapper\maven-%MAVEN_VERSION%

REM Check if Maven wrapper is installed
if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
    echo Installing Maven %MAVEN_VERSION%...
    mkdir "%MAVEN_HOME%" 2>nul
    
    set MAVEN_URL=https://archive.apache.org/dist/maven/maven-3/%MAVEN_VERSION%/binaries/apache-maven-%MAVEN_VERSION%-bin.zip
    
    REM Download using PowerShell
    powershell -Command "& {[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri '%MAVEN_URL%' -OutFile '%TEMP%\maven.zip'}"
    
    REM Extract using PowerShell
    powershell -Command "& {Expand-Archive -Path '%TEMP%\maven.zip' -DestinationPath '%TEMP%\maven-extract' -Force}"
    
    REM Move to final location
    xcopy /E /I /Y "%TEMP%\maven-extract\apache-maven-%MAVEN_VERSION%\*" "%MAVEN_HOME%\"
    
    REM Cleanup
    del "%TEMP%\maven.zip"
    rmdir /S /Q "%TEMP%\maven-extract"
    
    echo Maven %MAVEN_VERSION% installed successfully
)

REM Execute Maven with provided arguments
"%MAVEN_HOME%\bin\mvn.cmd" %*

endlocal
