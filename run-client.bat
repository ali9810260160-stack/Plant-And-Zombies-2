@echo off
REM Launch a SINGLE game instance (normal play). Builds the runtime if needed.
REM Portable: finds Gradle automatically (no hardcoded path).
setlocal enableextensions
set "REPO=%~dp0"
if "%REPO:~-1%"=="\" set "REPO=%REPO:~0,-1%"
set "INSTALL=%REPO%\lwjgl3\build\install\Plant-And-Zombies-2"

if not exist "%INSTALL%\lib" (
  call :find_gradle
  if not defined GRADLE_CMD goto :no_gradle
  echo First run: building the game runtime with "%GRADLE_CMD%" ^(1-2 min^)...
  call "%GRADLE_CMD%" -p "%REPO%" lwjgl3:installDist
  if errorlevel 1 ( echo. & echo BUILD FAILED. & pause & exit /b 1 )
)

call "%REPO%\_run-one-instance.bat"
endlocal
exit /b 0

REM ---- locate a usable Gradle: env override > PATH > ~/.gradle cache ----
:find_gradle
set "GRADLE_CMD="
if defined GRADLE_BIN if exist "%GRADLE_BIN%" ( set "GRADLE_CMD=%GRADLE_BIN%" & goto :eof )
for %%G in (gradle.bat gradle.exe) do (
  where %%G >nul 2>nul && ( set "GRADLE_CMD=%%G" & goto :eof )
)
REM NOTE: `for /r ... in (name)` yields a path for EVERY dir, so guard with `if exist`.
for /r "%USERPROFILE%\.gradle\wrapper\dists" %%F in (gradle.bat) do (
  if exist "%%F" ( echo %%F| findstr /i "gradle-9" >nul && if not defined GRADLE_CMD set "GRADLE_CMD=%%F" )
)
if not defined GRADLE_CMD for /r "%USERPROFILE%\.gradle\wrapper\dists" %%F in (gradle.bat) do (
  if exist "%%F" if not defined GRADLE_CMD set "GRADLE_CMD=%%F"
)
goto :eof

:no_gradle
echo.
echo Could not find Gradle. Install Gradle 9.4 and either add it to PATH,
echo or set GRADLE_BIN to the full path of gradle.bat, then re-run.
echo   example:  set GRADLE_BIN=C:\gradle\gradle-9.4\bin\gradle.bat
pause
exit /b 1
