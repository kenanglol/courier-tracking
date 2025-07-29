@echo off
REM Courier Tracking Microservice - Quick Start Script for Windows
REM This script helps users get the application running quickly on Windows

setlocal enabledelayedexpansion

echo 🚀 Courier Tracking Microservice - Quick Start (Windows)
echo ===========================================================

REM Check if Java 17+ is installed
:check_java
echo Checking Java installation...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Java not found
    echo Please install Java 17 or higher
    pause
    exit /b 1
)

for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set JAVA_VERSION_STRING=%%g
)
set JAVA_VERSION_STRING=%JAVA_VERSION_STRING:"=%
for /f "delims=." %%a in ("%JAVA_VERSION_STRING%") do set JAVA_MAJOR=%%a

if %JAVA_MAJOR% geq 17 (
    echo ✅ Java %JAVA_MAJOR% found
) else (
    echo ❌ Java 17+ required. Found Java %JAVA_MAJOR%
    echo Please install Java 17 or higher
    pause
    exit /b 1
)

REM Check if Maven is installed
:check_maven
echo Checking Maven installation...
mvn -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ⚠️ Maven not found. Checking for Maven Wrapper...
    if exist "mvnw.cmd" (
        echo ✅ Maven Wrapper found
        set MVN_CMD=mvnw.cmd
    ) else (
        echo ❌ Neither Maven nor Maven Wrapper found
        echo Please install Maven 3.6+ or ensure mvnw.cmd is present
        pause
        exit /b 1
    )
) else (
    echo ✅ Maven found
    set MVN_CMD=mvn
)

REM Build the application
:build_app
echo Building the application...
%MVN_CMD% clean package -DskipTests
if %errorlevel% neq 0 (
    echo ❌ Build failed
    pause
    exit /b 1
)
echo ✅ Build successful

REM Run tests
:run_tests
echo Running tests...
%MVN_CMD% test
if %errorlevel% neq 0 (
    echo ⚠️ Some tests failed, but continuing...
) else (
    echo ✅ All tests passed
)

REM Start the application
:start_app
echo Starting the application...
echo The application will be available at: http://localhost:8080
echo H2 Database Console: http://localhost:8080/h2-console
echo Press Ctrl+C to stop the application
echo.

%MVN_CMD% spring-boot:run
goto :eof

REM Docker option
:run_with_docker
echo Running with Docker...
docker --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Docker not found
    echo Please install Docker or use the regular Java method
    pause
    exit /b 1
)

echo Building Docker image...
docker build -t courier-tracking .

echo Starting container...
docker run -p 8080:8080 courier-tracking
goto :eof

REM Docker Compose option
:run_with_docker_compose
echo Running with Docker Compose...
docker-compose --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Docker Compose not found
    echo Please install Docker Compose or use another method
    pause
    exit /b 1
)

docker-compose up --build
goto :eof

REM Show menu
:show_menu
echo.
echo Choose how to run the application:
echo 1) Maven/Java (Recommended for development)
echo 2) Docker
echo 3) Docker Compose  
echo 4) Just build (no run)
echo 5) Exit
echo.
set /p choice="Enter your choice (1-5): "

if "%choice%"=="1" (
    call :build_app
    call :run_tests
    call :start_app
) else if "%choice%"=="2" (
    call :build_app
    call :run_with_docker
) else if "%choice%"=="3" (
    call :build_app
    call :run_with_docker_compose
) else if "%choice%"=="4" (
    call :build_app
    call :run_tests
    echo ✅ Build completed. JAR file: target\courier-tracking-0.0.1-SNAPSHOT.jar
    pause
) else if "%choice%"=="5" (
    echo Goodbye!
    exit /b 0
) else (
    echo Invalid choice. Please try again.
    goto :show_menu
)
goto :eof

REM Main execution
call :check_java
call :check_maven
call :show_menu
