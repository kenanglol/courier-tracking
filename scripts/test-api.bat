@echo off
REM API Testing Script for Courier Tracking Microservice - Windows Version
REM Tests all endpoints with various scenarios

setlocal enabledelayedexpansion

set BASE_URL=http://localhost:8080/api/couriers
set CONTENT_TYPE=Content-Type: application/json

echo 🧪 Courier Tracking API Tests (Windows)
echo =========================================

REM Check if curl is available
curl --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ curl not found. Please install curl to run API tests.
    echo You can download curl from: https://curl.se/download.html
    pause
    exit /b 1
)

REM Check if application is running
echo Checking if application is running...
curl -s -o nul %BASE_URL% >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Application not running at %BASE_URL%
    echo Please start the application first using:
    echo   quick-start.bat
    pause
    exit /b 1
)
echo ✅ Application is running

set TESTS_PASSED=0
set TESTS_FAILED=0

REM Get current timestamp for requests
for /f "delims=" %%i in ('powershell -command "Get-Date -UFormat '%%Y-%%m-%%dT%%H:%%M:%%S.%%3NZ'"') do set TIMESTAMP=%%i

echo.
echo === Test 1: Store Entrance Detection ===
echo Testing: Courier near Kanyon store

curl -s -w "%%{http_code}" -X POST -H "%CONTENT_TYPE%" -d "{\"courierId\":\"COURIER001\",\"latitude\":41.0840,\"longitude\":29.0093,\"timestamp\":\"%TIMESTAMP%\"}" %BASE_URL%/location > temp_response.txt
set /p response=<temp_response.txt
set http_code=%response:~-3%

if "%http_code%"=="200" (
    echo ✅ PASSED ^(HTTP %http_code%^)
    set /a TESTS_PASSED+=1
) else (
    echo ❌ FAILED ^(Expected HTTP 200, got %http_code%^)
    set /a TESTS_FAILED+=1
)

echo.
echo === Test 2: Normal Location (No Store) ===
echo Testing: Courier away from stores

curl -s -w "%%{http_code}" -X POST -H "%CONTENT_TYPE%" -d "{\"courierId\":\"COURIER001\",\"latitude\":41.1000,\"longitude\":29.1000,\"timestamp\":\"%TIMESTAMP%\"}" %BASE_URL%/location > temp_response.txt
set /p response=<temp_response.txt
set http_code=%response:~-3%

if "%http_code%"=="200" (
    echo ✅ PASSED ^(HTTP %http_code%^)
    set /a TESTS_PASSED+=1
) else (
    echo ❌ FAILED ^(Expected HTTP 200, got %http_code%^)
    set /a TESTS_FAILED+=1
)

echo.
echo === Test 3: Travel Distance Query ===
echo Testing: Get travel distance for COURIER001

curl -s -w "%%{http_code}" %BASE_URL%/COURIER001/total-travel-distance > temp_response.txt
set /p response=<temp_response.txt
set http_code=%response:~-3%

if "%http_code%"=="200" (
    echo ✅ PASSED ^(HTTP %http_code%^)
    echo Response: %response:~0,-3%
    set /a TESTS_PASSED+=1
) else (
    echo ❌ FAILED ^(Expected HTTP 200, got %http_code%^)
    set /a TESTS_FAILED+=1
)

echo.
echo === Test 4: Different Courier Near Different Store ===
echo Testing: Courier002 near Etiler store

curl -s -w "%%{http_code}" -X POST -H "%CONTENT_TYPE%" -d "{\"courierId\":\"COURIER002\",\"latitude\":41.0766,\"longitude\":29.0278,\"timestamp\":\"%TIMESTAMP%\"}" %BASE_URL%/location > temp_response.txt
set /p response=<temp_response.txt
set http_code=%response:~-3%

if "%http_code%"=="200" (
    echo ✅ PASSED ^(HTTP %http_code%^)
    set /a TESTS_PASSED+=1
) else (
    echo ❌ FAILED ^(Expected HTTP 200, got %http_code%^)
    set /a TESTS_FAILED+=1
)

echo.
echo === Test 5: Multiple Locations for Distance ===
echo Testing: COURIER003 - Location 1

curl -s -w "%%{http_code}" -X POST -H "%CONTENT_TYPE%" -d "{\"courierId\":\"COURIER003\",\"latitude\":41.0000,\"longitude\":29.0000,\"timestamp\":\"%TIMESTAMP%\"}" %BASE_URL%/location > temp_response.txt
set /p response=<temp_response.txt
set http_code=%response:~-3%

if "%http_code%"=="200" (
    echo ✅ PASSED ^(HTTP %http_code%^)
    set /a TESTS_PASSED+=1
) else (
    echo ❌ FAILED ^(Expected HTTP 200, got %http_code%^)
    set /a TESTS_FAILED+=1
)

timeout /t 2 /nobreak >nul

echo Testing: COURIER003 - Location 2
curl -s -w "%%{http_code}" -X POST -H "%CONTENT_TYPE%" -d "{\"courierId\":\"COURIER003\",\"latitude\":41.0100,\"longitude\":29.0100,\"timestamp\":\"%TIMESTAMP%\"}" %BASE_URL%/location > temp_response.txt
set /p response=<temp_response.txt
set http_code=%response:~-3%

if "%http_code%"=="200" (
    echo ✅ PASSED ^(HTTP %http_code%^)
    set /a TESTS_PASSED+=1
) else (
    echo ❌ FAILED ^(Expected HTTP 200, got %http_code%^)
    set /a TESTS_FAILED+=1
)

echo Testing: Get travel distance for COURIER003
curl -s -w "%%{http_code}" %BASE_URL%/COURIER003/total-travel-distance > temp_response.txt
set /p response=<temp_response.txt
set http_code=%response:~-3%

if "%http_code%"=="200" (
    echo ✅ PASSED ^(HTTP %http_code%^)
    echo Response: %response:~0,-3%
    set /a TESTS_PASSED+=1
) else (
    echo ❌ FAILED ^(Expected HTTP 200, got %http_code%^)
    set /a TESTS_FAILED+=1
)

echo.
echo === Test 6: Invalid Data Handling ===
echo Testing: Invalid latitude

curl -s -w "%%{http_code}" -X POST -H "%CONTENT_TYPE%" -d "{\"courierId\":\"COURIER005\",\"latitude\":999,\"longitude\":29.0093,\"timestamp\":\"%TIMESTAMP%\"}" %BASE_URL%/location > temp_response.txt
set /p response=<temp_response.txt
set http_code=%response:~-3%

if "%http_code%"=="400" (
    echo ✅ PASSED ^(HTTP %http_code%^)
    set /a TESTS_PASSED+=1
) else (
    echo ❌ FAILED ^(Expected HTTP 400, got %http_code%^)
    set /a TESTS_FAILED+=1
)

echo.
echo === Test 7: Non-existent Courier ===
echo Testing: Get distance for non-existent courier

curl -s -w "%%{http_code}" %BASE_URL%/NONEXISTENT/total-travel-distance > temp_response.txt
set /p response=<temp_response.txt
set http_code=%response:~-3%

if "%http_code%"=="200" (
    echo ✅ PASSED ^(HTTP %http_code%^)
    echo Response: %response:~0,-3%
    set /a TESTS_PASSED+=1
) else (
    echo ❌ FAILED ^(Expected HTTP 200, got %http_code%^)
    set /a TESTS_FAILED+=1
)

REM Cleanup
del temp_response.txt >nul 2>&1

echo.
echo === Test Summary ===
echo Tests Passed: %TESTS_PASSED%
echo Tests Failed: %TESTS_FAILED%

if %TESTS_FAILED% equ 0 (
    echo.
    echo 🎉 All tests passed!
) else (
    echo.
    echo ❌ Some tests failed
)

pause
