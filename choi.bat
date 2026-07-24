@echo off
setlocal enabledelayedexpansion
title Voxel Engine

:: ==================================================================
::  Mot lenh lam het: build lai game, build + bat may chu, roi mo game.
::
::  Cach dung:
::     choi.bat        -> mo 2 cua so (de thu choi chung)
::     choi.bat 1      -> mo 1 cua so
::     choi.bat 2 sach -> build lai tu dau (khi nghi build cu bi loi)
:: ==================================================================

set SO_CUA_SO=%~1
if "%SO_CUA_SO%"=="" set SO_CUA_SO=2
set KIEU_BUILD=%~2

:: Moi duong dan deu suy ra tu cho dat file .bat nay, khong ghi cung o dia nao ca.
set GOC=%~dp0
if "%GOC:~-1%"=="\" set GOC=%GOC:~0,-1%
set ASSETS=%GOC%\assets
set THAMSO=%GOC%\desktop\build\launch-args.txt

:: Tim java: uu tien JAVA_HOME, khong co thi lay java trong PATH.
if defined JAVA_HOME (
    set JAVA=%JAVA_HOME%\bin\java.exe
) else (
    set JAVA=java
)

cd /d "%GOC%"

:: ------------------------------------------------------------------
echo.
echo [1/3] Building the game...
echo ------------------------------------------------------------------
set VIEC=:core:jar :desktop:classes :desktop:writeLaunchArgs
if /i "%KIEU_BUILD%"=="sach" set VIEC=clean %VIEC%

:: Goi bang duong dan day du: co may khong cho tim lenh trong thu muc hien tai.
call "%GOC%\gradlew.bat" %VIEC%
if errorlevel 1 goto :loi_build
if not exist "%THAMSO%" goto :loi_thamso

:: ------------------------------------------------------------------
echo.
echo [2/3] Updating and starting the server...
echo ------------------------------------------------------------------
where docker >nul 2>nul
if errorlevel 1 (
    echo [Note] Docker is not installed, so the server cannot start.
    echo        Without the server you cannot log in.
    goto :mo_game
)

:: "up -d --build" tu build lai anh khi ma nguon server doi, khong doi thi dung cache
:: nen chay lai rat nhanh. Container dang chay san thi no giu nguyen.
docker compose up -d --build
if errorlevel 1 (
    echo [Note] Starting the server failed. Is Docker Desktop running?
    goto :mo_game
)

:: Cho toi khi may chu chiu tra loi (toi da 60 giay) roi moi mo game,
:: neu khong man hinh dang nhap se bao loi mang oan.
echo Waiting for the server to come up...
set /a DEM=0
:cho_may_chu
curl -s -o nul -m 2 http://localhost:8080/ >nul 2>nul
if not errorlevel 1 goto :may_chu_ok
set /a DEM+=1
if !DEM! geq 30 (
    echo [Note] No answer after 60 seconds - opening the game anyway, login may fail.
    goto :mo_game
)
ping -n 3 127.0.0.1 >nul
goto :cho_may_chu

:may_chu_ok
echo Server is ready.

:: ------------------------------------------------------------------
:mo_game
echo.
echo [3/3] Opening %SO_CUA_SO% game window(s)...
echo ------------------------------------------------------------------
echo Log in with %SO_CUA_SO% DIFFERENT accounts using the same Map code (e.g. 123).
echo The game starts in SURVIVAL mode with an empty inventory.
echo.
echo   Left click       mine blocks / fight zombies and other players
echo   Right click      place a block (hold 1.2s to EAT an apple)
echo   E                inventory: shift+click quick-moves, drag to split,
echo                    double-click gathers a stack
echo   Space            jump; hold it under water to swim up
echo   T  /  ESC        chat  /  settings
echo   F5 / F3          change view / debug panel
echo   /weather rain    let it rain    -    /time night brings the monsters
echo.

rem Cho mot chut giua hai cua so. Dung "ping" thay cho "timeout" vi timeout bao loi khi bat
rem duoc goi tu cua so khong co ban phim. Trong khoi for phai dung "rem", KHONG dung "::" -
rem cmd se tuong "::" la ten o dia va in ra "The system cannot find the drive specified".
for /l %%i in (1,1,%SO_CUA_SO%) do (
    start "Voxel Player %%i" /D "%ASSETS%" "%JAVA%" "@%THAMSO%" com.voxel.desktop.DesktopLauncher
    ping -n 3 127.0.0.1 >nul
)

echo Done. You can close this window at any time.
endlocal
exit /b 0

:loi_build
echo.
echo [Error] Building the game failed - see the compiler output above.
echo         If the old build looks broken, run:  choi.bat 2 sach
pause
endlocal
exit /b 1

:loi_thamso
echo.
echo [Error] Cannot find "%THAMSO%".
echo         The :desktop:writeLaunchArgs task did not run.
pause
endlocal
exit /b 1
