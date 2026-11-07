@echo off
setlocal
echo ==============================================
echo [Lernspace] Fast Build & Install to Tablet
echo ==============================================

where adb >nul 2>&1
if %ERRORLEVEL% neq 0 (
    set "PATH=%LOCALAPPDATA%\Android\Sdk\platform-tools;%PATH%"
)

call gradlew.bat installDebug
if %ERRORLEVEL% equ 0 (
    echo.
    echo Launching Lernspace on tablet...
    adb shell am start -n com.lernspace.app/.MainActivity
    echo Done!
) else (
    echo.
    echo Build failed! Please check error output above.
)
