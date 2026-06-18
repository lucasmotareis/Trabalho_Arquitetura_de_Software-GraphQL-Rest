@echo off
setlocal

set MAVEN_VERSION=3.9.9
set WRAPPER_DIR=%~dp0.mvn\wrapper
set MAVEN_HOME=%WRAPPER_DIR%\apache-maven-%MAVEN_VERSION%
set MAVEN_ZIP=%WRAPPER_DIR%\apache-maven-%MAVEN_VERSION%-bin.zip

if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $wrapperDir='%WRAPPER_DIR%'; $zipPath='%MAVEN_ZIP%'; $url='https://archive.apache.org/dist/maven/maven-3/%MAVEN_VERSION%/binaries/apache-maven-%MAVEN_VERSION%-bin.zip'; New-Item -ItemType Directory -Force -Path $wrapperDir | Out-Null; Invoke-WebRequest -Uri $url -OutFile $zipPath; Expand-Archive -Force -Path $zipPath -DestinationPath $wrapperDir"
)

call "%MAVEN_HOME%\bin\mvn.cmd" %*

