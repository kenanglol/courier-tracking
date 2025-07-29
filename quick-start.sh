#!/bin/bash

# Courier Tracking Microservice - Quick Start Script
# This script helps users get the application running quickly

set -e

echo "🚀 Courier Tracking Microservice - Quick Start"
echo "=============================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Check if Java 17+ is installed
check_java() {
    echo -e "${BLUE}Checking Java installation...${NC}"
    if command -v java &> /dev/null; then
        JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
        if [ "$JAVA_VERSION" -ge 17 ]; then
            echo -e "${GREEN}✅ Java $JAVA_VERSION found${NC}"
        else
            echo -e "${RED}❌ Java 17+ required. Found Java $JAVA_VERSION${NC}"
            echo "Please install Java 17 or higher"
            exit 1
        fi
    else
        echo -e "${RED}❌ Java not found${NC}"
        echo "Please install Java 17 or higher"
        exit 1
    fi
}

# Check if Maven is installed
check_maven() {
    echo -e "${BLUE}Checking Maven installation...${NC}"
    if command -v mvn &> /dev/null; then
        MAVEN_VERSION=$(mvn -version | head -n 1 | awk '{print $3}')
        echo -e "${GREEN}✅ Maven $MAVEN_VERSION found${NC}"
    else
        echo -e "${YELLOW}⚠️  Maven not found. Trying to use Maven Wrapper...${NC}"
        if [ -f "./mvnw" ]; then
            echo -e "${GREEN}✅ Maven Wrapper found${NC}"
            MVN_CMD="./mvnw"
        else
            echo -e "${RED}❌ Neither Maven nor Maven Wrapper found${NC}"
            echo "Please install Maven 3.6+ or ensure mvnw is present"
            exit 1
        fi
    fi
}

# Build the application
build_app() {
    echo -e "${BLUE}Building the application...${NC}"
    ${MVN_CMD:-mvn} clean package -DskipTests
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ Build successful${NC}"
    else
        echo -e "${RED}❌ Build failed${NC}"
        exit 1
    fi
}

# Run tests
run_tests() {
    echo -e "${BLUE}Running tests...${NC}"
    ${MVN_CMD:-mvn} test
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ All tests passed${NC}"
    else
        echo -e "${YELLOW}⚠️  Some tests failed, but continuing...${NC}"
    fi
}

# Start the application
start_app() {
    echo -e "${BLUE}Starting the application...${NC}"
    echo -e "${YELLOW}The application will be available at: http://localhost:8080${NC}"
    echo -e "${YELLOW}H2 Database Console: http://localhost:8080/h2-console${NC}"
    echo -e "${YELLOW}Press Ctrl+C to stop the application${NC}"
    echo ""
    
    ${MVN_CMD:-mvn} spring-boot:run
}

# Docker option
run_with_docker() {
    echo -e "${BLUE}Running with Docker...${NC}"
    if command -v docker &> /dev/null; then
        echo -e "${BLUE}Building Docker image...${NC}"
        docker build -t courier-tracking .
        
        echo -e "${BLUE}Starting container...${NC}"
        docker run -p 8080:8080 courier-tracking
    else
        echo -e "${RED}❌ Docker not found${NC}"
        echo "Please install Docker or use the regular Java method"
        exit 1
    fi
}

# Docker Compose option
run_with_docker_compose() {
    echo -e "${BLUE}Running with Docker Compose...${NC}"
    if command -v docker-compose &> /dev/null; then
        docker-compose up --build
    else
        echo -e "${RED}❌ Docker Compose not found${NC}"
        echo "Please install Docker Compose or use another method"
        exit 1
    fi
}

# Show menu
show_menu() {
    echo ""
    echo "Choose how to run the application:"
    echo "1) Maven/Java (Recommended for development)"
    echo "2) Docker"
    echo "3) Docker Compose"
    echo "4) Just build (no run)"
    echo "5) Exit"
    echo ""
    read -p "Enter your choice (1-5): " choice
    
    case $choice in
        1)
            check_java
            check_maven
            run_tests
            build_app
            start_app
            ;;
        2)
            check_java
            check_maven
            build_app
            run_with_docker
            ;;
        3)
            check_java
            check_maven
            build_app
            run_with_docker_compose
            ;;
        4)
            check_java
            check_maven
            run_tests
            build_app
            echo -e "${GREEN}✅ Build completed. JAR file: target/courier-tracking-0.0.1-SNAPSHOT.jar${NC}"
            ;;
        5)
            echo "Goodbye!"
            exit 0
            ;;
        *)
            echo -e "${RED}Invalid choice. Please try again.${NC}"
            show_menu
            ;;
    esac
}

# Main execution
main() {
    # Check if running in CI environment
    if [ "${CI}" = "true" ]; then
        echo "Running in CI mode..."
        check_java
        check_maven
        run_tests
        build_app
    else
        show_menu
    fi
}

# Run main function
main "$@"
