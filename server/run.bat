@echo off
REM Run the PvZ2 server (build first with build.bat).
REM Opens the Swing admin dashboard and listens on port 5599 by default.
setlocal
cd /d "%~dp0"
if not exist out (
  echo Not built yet - running build.bat first...
  call build.bat || exit /b 1
)
java -cp "out;lib\gson-2.13.1.jar" com.pvz2.server.Main %*
endlocal
