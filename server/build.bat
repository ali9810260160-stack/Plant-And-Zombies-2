@echo off
REM Compile the PvZ2 server with plain javac (no gradle).
REM Compiles shared/ protocol + server/ sources; gson from lib/.
setlocal
cd /d "%~dp0"
if not exist out mkdir out
if exist sources.txt del /q sources.txt
dir /b /s src\*.java > sources.txt
dir /b /s ..\shared\src\*.java >> sources.txt
echo Compiling...
javac -encoding UTF-8 -d out -cp "lib\gson-2.13.1.jar" @sources.txt
set RC=%ERRORLEVEL%
del /q sources.txt
if not "%RC%"=="0" (
  echo BUILD FAILED
  exit /b %RC%
)
echo BUILD OK  ^(classes in server\out^)
endlocal
