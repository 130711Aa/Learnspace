@echo off
setlocal
echo ==============================================
echo [Lernspace] Fast Build ^& Install to Tablet
echo ==============================================

set "JAVA_HOME=C:\Users\ADVAN\.jdks\jdk-17.0.13+11"
set "PATH=%JAVA_HOME%\bin;%LOCALAPPDATA%\Android\Sdk\platform-tools;%PATH%"

echo Checking device status...
adb devices

echo.
echo Building debug APK with Room compiler and Java/Kotlin sources...
call gradlew.bat assembleDebug
if %ERRORLEVEL% equ 0 (
    echo.
    echo Installing APK to tablet...
    adb install -r app\build\outputs\apk\debug\app-debug.apk
    echo Launching Lernspace on tablet...
    adb shell am start -n com.lernspace.app/.MainActivity
    echo Done! App successfully launched on tablet!
) else (
    echo.
    echo Build failed! Please check error output above.
)
