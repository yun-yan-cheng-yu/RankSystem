@echo off
rem ================================================================
rem  run-sql.bat - Run every *.sql file under the project sql folder
rem                against a local MySQL server.
rem
rem  Usage:
rem    run-sql.bat              -> use config below (root, empty password)
rem    run-sql.bat <password>   -> use config below but override password
rem
rem  mysql.exe is resolved in this order:
rem    1. PATH
rem    2. default MySQL install dirs
rem    3. MYSQL_PATH variable below (edit this file)
rem ================================================================
setlocal

rem ---------- config: edit if needed ----------
set "SQL_DIR=%~dp0..\src\main\java\com\zqyyz\ranksystem\sql"
set "MYSQL_HOST=127.0.0.1"
set "MYSQL_PORT=3306"
set "MYSQL_USER=root"
set "MYSQL_PASSWORD=2526"
set "MYSQL_PATH="
rem example: set "MYSQL_PATH=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
rem ---------------------------------------------

if not "%~1"=="" set "MYSQL_PASSWORD=%~1"

rem ---------- 1. locate mysql.exe ----------
if not "%MYSQL_PATH%"=="" goto :mysql_found
where mysql >nul 2>nul
if not errorlevel 1 (
    set "MYSQL_PATH=mysql"
    goto :mysql_found
)
if exist "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" (
    set "MYSQL_PATH=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
    goto :mysql_found
)
if exist "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" (
    set "MYSQL_PATH=C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe"
    goto :mysql_found
)
if exist "C:\Program Files\MySQL\MySQL Server 5.7\bin\mysql.exe" (
    set "MYSQL_PATH=C:\Program Files\MySQL\MySQL Server 5.7\bin\mysql.exe"
    goto :mysql_found
)
echo [ERROR] mysql.exe not found. Add mysql to PATH or set MYSQL_PATH at the top of this script.
exit /b 1
:mysql_found

rem ---------- 2. check sql folder ----------
if not exist "%SQL_DIR%" (
    echo [ERROR] sql directory not found: %SQL_DIR%
    exit /b 1
)

set /a TOTAL=0
for /r "%SQL_DIR%" %%f in (*.sql) do set /a TOTAL+=1
if %TOTAL% EQU 0 (
    echo [ERROR] no *.sql files under %SQL_DIR%
    exit /b 1
)

echo mysql.exe : "%MYSQL_PATH%"
echo target    : %MYSQL_HOST%:%MYSQL_PORT%  user: %MYSQL_USER%
echo sql files : %TOTAL%
for /r "%SQL_DIR%" %%f in (*.sql) do echo   - %%~nxf

rem ---------- 3. run every sql file ----------
rem "< file" feeds the raw bytes of the UTF-8 sql file to mysql stdin;
rem combined with utf8mb4, Chinese text is stored correctly.
set /a FAILED=0
for /r "%SQL_DIR%" %%f in (*.sql) do (
    echo.
    echo ===^> executing %%~nxf ...
    if "%MYSQL_PASSWORD%"=="" (
        "%MYSQL_PATH%" --host=%MYSQL_HOST% --port=%MYSQL_PORT% --user=%MYSQL_USER% --default-character-set=utf8mb4 < "%%f"
    ) else (
        "%MYSQL_PATH%" --host=%MYSQL_HOST% --port=%MYSQL_PORT% --user=%MYSQL_USER% "--password=%MYSQL_PASSWORD%" --default-character-set=utf8mb4 < "%%f"
    )
    if errorlevel 1 (
        echo     FAILED: %%~nxf
        set /a FAILED+=1
        goto :done
    )
    echo     OK
)

:done
echo.
if %FAILED% EQU 0 (
    echo ALL %TOTAL% SQL file(s) executed successfully.
) else (
    echo execution aborted after %FAILED% failed file(s).
)
pause
exit /b %FAILED%
