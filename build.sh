#!/bin/bash

# Build and Run Script for Blockchain Supply Chain System (Java)
# =============================================================

set -e  # Exit on error

echo "╔══════════════════════════════════════════════════════════════════════════════╗"
echo "║                                                                              ║"
echo "║     Blockchain Supply Chain System - Java Build & Run                        ║"
echo "║                                                                              ║"
echo "╚══════════════════════════════════════════════════════════════════════════════╝"
echo ""

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Project root directory
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo -e "${BLUE}Project Root: ${NC}$PROJECT_ROOT"
echo ""

# Change to project root
cd "$PROJECT_ROOT"

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo -e "${YELLOW}Maven not found. Installing Maven...${NC}"
    
    # Detect OS and install Maven
    if [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS
        if command -v brew &> /dev/null; then
            brew install maven
        else
            echo "Please install Maven manually or use Homebrew"
            exit 1
        fi
    elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
        # Linux
        if command -v apt-get &> /dev/null; then
            sudo apt-get update
            sudo apt-get install -y maven
        elif command -v yum &> /dev/null; then
            sudo yum install -y maven
        else
            echo "Please install Maven manually"
            exit 1
        fi
    else
        echo "Unsupported OS. Please install Maven manually."
        exit 1
    fi
fi

echo -e "${GREEN}✓ Maven found${NC}"
echo ""

# Display Java version
echo -e "${BLUE}Java Version:${NC}"
java -version 2>&1 | head -1
echo ""

# Display Maven version
echo -e "${BLUE}Maven Version:${NC}"
mvn --version | head -1
echo ""

# Parse command line arguments
RUN_MODE=${1:-menu}

case "$RUN_MODE" in
    "compile")
        echo -e "${BLUE}Compiling project...${NC}"
        mvn clean compile
        echo -e "${GREEN}✓ Compilation successful${NC}"
        ;;
    
    "test")
        echo -e "${BLUE}Running tests...${NC}"
        mvn test
        echo -e "${GREEN}✓ Tests completed${NC}"
        ;;
    
    "run")
        echo -e "${BLUE}Running application...${NC}"
        mvn exec:java -Dexec.mainClass="com.supplychain.Main"
        ;;
    
    "package")
        echo -e "${BLUE}Packaging application...${NC}"
        mvn clean package
        echo -e "${GREEN}✓ Package created: target/blockchain-supply-chain-1.0.0.jar${NC}"
        echo ""
        echo "Run with: java -jar target/blockchain-supply-chain-1.0.0.jar"
        ;;
    
    "all")
        echo -e "${BLUE}Building complete project...${NC}"
        echo ""
        
        # Compile
        echo -e "${BLUE}[1/4] Compiling...${NC}"
        mvn clean compile
        
        # Run tests
        echo ""
        echo -e "${BLUE}[2/4] Running tests...${NC}"
        mvn test
        
        # Package
        echo ""
        echo -e "${BLUE}[3/4] Packaging...${NC}"
        mvn package -DskipTests
        
        # Run
        echo ""
        echo -e "${BLUE}[4/4] Running application...${NC}"
        java -jar target/blockchain-supply-chain-1.0.0.jar
        
        echo ""
        echo -e "${GREEN}═══════════════════════════════════════════════════════════════════════════════${NC}"
        echo -e "${GREEN}✓ FULL BUILD COMPLETE${NC}"
        echo -e "${GREEN}═══════════════════════════════════════════════════════════════════════════════${NC}"
        ;;
    
    "menu")
        echo "╔══════════════════════════════════════════════════════════════════════════════╗"
        echo "║                        BUILD & RUN OPTIONS                                   ║"
        echo "╚══════════════════════════════════════════════════════════════════════════════╝"
        echo ""
        echo "  1. Compile project"
        echo "  2. Run tests"
        echo "  3. Run application"
        echo "  4. Package as JAR"
        echo "  5. Build all (compile + test + package + run)"
        echo "  6. Exit"
        echo ""
        read -p "Select option (1-6): " choice
        
        case "$choice" in
            1) ./build.sh compile ;;
            2) ./build.sh test ;;
            3) ./build.sh run ;;
            4) ./build.sh package ;;
            5) ./build.sh all ;;
            6) exit 0 ;;
            *) echo "Invalid option"; exit 1 ;;
        esac
        ;;
    
    *)
        echo "Usage: $0 [compile|test|run|package|all|menu]"
        echo ""
        echo "Options:"
        echo "  compile  - Compile the project"
        echo "  test     - Run unit tests"
        echo "  run      - Run the application"
        echo "  package  - Package as JAR file"
        echo "  all      - Compile, test, package, and run"
        echo "  menu     - Show interactive menu (default)"
        exit 1
        ;;
esac

echo ""
