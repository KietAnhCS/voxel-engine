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
echo [1/3] Build game...
echo ------------------------------------------------------------------
set VIEC=:core:jar :desktop:classes :desktop:writeLaunchArgs
if /i "%KIEU_BUILD%"=="sach" set VIEC=clean %VIEC%

:: Goi bang duong dan day du: co may khong cho tim lenh trong thu muc hien tai.
call "%GOC%\gradlew.bat" %VIEC%
if errorlevel 1 goto :loi_build
if not exist "%THAMSO%" goto :loi_thamso

:: ------------------------------------------------------------------
echo.
echo [2/3] Cap nhat va bat may chu...
echo ------------------------------------------------------------------
where docker >nul 2>nul
if errorlevel 1 (
    echo [Chu y] May nay chua co Docker nen khong bat duoc may chu.
    echo         Khong co may chu thi khong dang nhap duoc.
    goto :mo_game
)

:: "up -d --build" tu build lai anh khi ma nguon server doi, khong doi thi dung cache
:: nen chay lai rat nhanh. Container dang chay san thi no giu nguyen.
docker compose up -d --build
if errorlevel 1 (
    echo [Chu y] Bat may chu that bai. Da mo Docker Desktop chua?
    goto :mo_game
)

:: Cho toi khi may chu chiu tra loi (toi da 60 giay) roi moi mo game,
:: neu khong man hinh dang nhap se bao loi mang oan.
echo Dang cho may chu san sang...
set /a DEM=0
:cho_may_chu
curl -s -o nul -m 2 http://localhost:8080/ >nul 2>nul
if not errorlevel 1 goto :may_chu_ok
set /a DEM+=1
if !DEM! geq 30 (
    echo [Chu y] May chu chua tra loi sau 60 giay - cu mo game, dang nhap co the loi.
    goto :mo_game
)
ping -n 3 127.0.0.1 >nul
goto :cho_may_chu

:may_chu_ok
echo May chu san sang.

:: ------------------------------------------------------------------
:mo_game
echo.
echo [3/3] Mo %SO_CUA_SO% cua so game...
echo ------------------------------------------------------------------
echo Dang nhap %SO_CUA_SO% tai khoan KHAC nhau, cung mot Ma map (vi du 123).
echo Vao game la che do SINH TON, tui do trong.
echo.
echo   Chuot trai       dao khoi / danh zombie va nguoi choi khac
echo   Chuot phai       dat khoi (giu 1.2 giay de AN qua tao)
echo   E                tui do: shift+bam chuyen nhanh, keo chuot chia deu,
echo                    bam dup gom khoi cung loai
echo   Phim cach        nhay, o duoi nuoc thi boi len
echo   F5 / F3          doi goc nhin / bang thong tin
echo   /time night      nhay toi ban dem cho zombie ra
echo.

rem Cho mot chut giua hai cua so. Dung "ping" thay cho "timeout" vi timeout bao loi khi bat
rem duoc goi tu cua so khong co ban phim. Trong khoi for phai dung "rem", KHONG dung "::" -
rem cmd se tuong "::" la ten o dia va in ra "The system cannot find the drive specified".
for /l %%i in (1,1,%SO_CUA_SO%) do (
    start "Voxel Player %%i" /D "%ASSETS%" "%JAVA%" "@%THAMSO%" com.voxel.desktop.DesktopLauncher
    ping -n 3 127.0.0.1 >nul
)

echo Da mo xong. Dong cua so nay luc nao cung duoc.
endlocal
exit /b 0

:loi_build
echo.
echo [Loi] Build game that bai - xem loi code o tren.
echo       Neu nghi build cu bi hong thi chay:  choi.bat 2 sach
pause
endlocal
exit /b 1

:loi_thamso
echo.
echo [Loi] Khong thay "%THAMSO%".
echo       Task :desktop:writeLaunchArgs chua chay duoc.
pause
endlocal
exit /b 1
