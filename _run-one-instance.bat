@echo off
REM Internal helper: launch ONE game instance.
REM   %1 = optional working-dir name under run\ (e.g. p1, p2). Empty = use assets\.
REM Each instance runs in its OWN working dir so their data\ (session token,
REM net log) don't collide — required to log in as two DIFFERENT players.
setlocal
set "REPO=%~dp0"
if "%REPO:~-1%"=="\" set "REPO=%REPO:~0,-1%"
set "INSTALL=%REPO%\lwjgl3\build\install\Plant-And-Zombies-2"

if not exist "%INSTALL%\lib" (
  echo Game runtime not built yet. Run run-client.bat or run-2p.bat first.
  pause
  exit /b 1
)

if "%~1"=="" (
  cd /d "%REPO%\assets"
) else (
  if not exist "%REPO%\run\%~1" mkdir "%REPO%\run\%~1"
  cd /d "%REPO%\run\%~1"
)

java -Dpvz.assets="%REPO%\assets" -cp "%INSTALL%\lib\*" com.pvz2.lwjgl3.Lwjgl3Launcher
endlocal
