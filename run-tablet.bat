@echo off
echo ==============================================
echo [Lernspace] Fast Build & Install to Tablet
echo ==============================================
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

