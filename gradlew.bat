@echo off
setlocal

set "APP_HOME=%~dp0"

if defined JAVA_HOME (
    set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
) else (
    set "JAVA_EXE=java.exe"
)

if not exist "%JAVA_EXE%" (
    echo Java was not found. Set JAVA_HOME to a JDK 17 installation. 1>&2
    exit /b 1
)

"%JAVA_EXE%" -Xmx64m -Xms64m -Dorg.gradle.appname=gradlew -classpath "%APP_HOME%\gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*

endlocal
exit /b %ERRORLEVEL%

