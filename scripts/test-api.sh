#!/bin/bash

# API Testing Script for Courier Tracking Microservice
# Tests all endpoints with various scenarios

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

BASE_URL="http://localhost:8080/api/couriers"
CONTENT_TYPE="Content-Type: application/json"

echo -e "${BLUE}🧪 Courier Tracking API Tests${NC}"
echo "=============================="

# Check if application is running
echo -e "${BLUE}Checking if application is running...${NC}"
if ! curl -s -o /dev/null -w "%{http_code}" $BASE_URL > /dev/null; then
    echo -e "${RED}❌ Application not running at $BASE_URL${NC}"
    echo "Please start the application first using:"
    echo "  ./quick-start.sh"
    exit 1
fi
echo -e "${GREEN}✅ Application is running${NC}"

# Test counters
TESTS_PASSED=0
TESTS_FAILED=0

# Helper function to run test
run_test() {
    local test_name="$1"
    local expected_status="$2"
    local url="$3"
    local data="$4"
    local method="${5:-POST}"
    
    echo -e "\n${YELLOW}Testing: $test_name${NC}"
    
    if [ "$method" = "POST" ]; then
        response=$(curl -s -w "\n%{http_code}" -X POST -H "$CONTENT_TYPE" -d "$data" "$url")
    else
        response=$(curl -s -w "\n%{http_code}" "$url")
    fi
    
    http_code=$(echo "$response" | tail -n1)
    response_body=$(echo "$response" | sed '$d')
    
    if [ "$http_code" = "$expected_status" ]; then
        echo -e "${GREEN}✅ PASSED${NC} (HTTP $http_code)"
        if [ ! -z "$response_body" ]; then
            echo "Response: $response_body"
        fi
        ((TESTS_PASSED++))
    else
        echo -e "${RED}❌ FAILED${NC} (Expected HTTP $expected_status, got $http_code)"
        echo "Response: $response_body"
        ((TESTS_FAILED++))
    fi
}

# Test 1: Send courier location near Kanyon store
echo -e "\n${BLUE}=== Test 1: Store Entrance Detection ===${NC}"
run_test "Courier near Kanyon store" "200" "$BASE_URL/location" '{
    "courierId": "COURIER001",
    "latitude": 41.0840,
    "longitude": 29.0093,
    "timestamp": "'$(date -u +%Y-%m-%dT%H:%M:%S.%3NZ)'"
}'

# Test 2: Send courier location away from stores
echo -e "\n${BLUE}=== Test 2: Normal Location (No Store) ===${NC}"
run_test "Courier away from stores" "200" "$BASE_URL/location" '{
    "courierId": "COURIER001", 
    "latitude": 41.1000,
    "longitude": 29.1000,
    "timestamp": "'$(date -u +%Y-%m-%dT%H:%M:%S.%3NZ)'"
}'

# Test 3: Get travel distance
echo -e "\n${BLUE}=== Test 3: Travel Distance Query ===${NC}"
run_test "Get travel distance for COURIER001" "200" "$BASE_URL/COURIER001/total-travel-distance" "" "GET"

# Test 4: Another courier near different store
echo -e "\n${BLUE}=== Test 4: Different Courier Near Different Store ===${NC}"
run_test "Courier002 near Etiler store" "200" "$BASE_URL/location" '{
    "courierId": "COURIER002",
    "latitude": 41.0766,
    "longitude": 29.0278,
    "timestamp": "'$(date -u +%Y-%m-%dT%H:%M:%S.%3NZ)'"
}'

# Test 5: Send multiple locations for distance calculation
echo -e "\n${BLUE}=== Test 5: Multiple Locations for Distance ===${NC}"

# Location 1
run_test "COURIER003 - Location 1" "200" "$BASE_URL/location" '{
    "courierId": "COURIER003",
    "latitude": 41.0000,
    "longitude": 29.0000,
    "timestamp": "'$(date -u +%Y-%m-%dT%H:%M:%S.%3NZ)'"
}'

sleep 1

# Location 2 (some distance away)
run_test "COURIER003 - Location 2" "200" "$BASE_URL/location" '{
    "courierId": "COURIER003",
    "latitude": 41.0100,
    "longitude": 29.0100,
    "timestamp": "'$(date -u +%Y-%m-%dT%H:%M:%S.%3NZ)'"
}'

# Get distance for COURIER003
run_test "Get travel distance for COURIER003" "200" "$BASE_URL/COURIER003/total-travel-distance" "" "GET"

# Test 6: Test duplicate prevention (same location within 1 minute)
echo -e "\n${BLUE}=== Test 6: Duplicate Prevention ===${NC}"
run_test "COURIER004 - First location" "200" "$BASE_URL/location" '{
    "courierId": "COURIER004",
    "latitude": 41.0840,
    "longitude": 29.0093,
    "timestamp": "'$(date -u +%Y-%m-%dT%H:%M:%S.%3NZ)'"
}'

# Immediately send same location (should be prevented)
run_test "COURIER004 - Duplicate location (should be ignored)" "200" "$BASE_URL/location" '{
    "courierId": "COURIER004",
    "latitude": 41.0840,
    "longitude": 29.0093,
    "timestamp": "'$(date -u +%Y-%m-%dT%H:%M:%S.%3NZ)'"
}'

# Test 7: Invalid data
echo -e "\n${BLUE}=== Test 7: Invalid Data Handling ===${NC}"
run_test "Invalid latitude" "400" "$BASE_URL/location" '{
    "courierId": "COURIER005",
    "latitude": 999,
    "longitude": 29.0093,
    "timestamp": "'$(date -u +%Y-%m-%dT%H:%M:%S.%3NZ)'"
}'

run_test "Missing courierId" "400" "$BASE_URL/location" '{
    "latitude": 41.0840,
    "longitude": 29.0093,
    "timestamp": "'$(date -u +%Y-%m-%dT%H:%M:%S.%3NZ)'"
}'

# Test 8: Non-existent courier distance query
echo -e "\n${BLUE}=== Test 8: Non-existent Courier ===${NC}"
run_test "Get distance for non-existent courier" "200" "$BASE_URL/NONEXISTENT/total-travel-distance" "" "GET"

# Summary
echo -e "\n${BLUE}=== Test Summary ===${NC}"
echo -e "Tests Passed: ${GREEN}$TESTS_PASSED${NC}"
echo -e "Tests Failed: ${RED}$TESTS_FAILED${NC}"

if [ $TESTS_FAILED -eq 0 ]; then
    echo -e "\n${GREEN}🎉 All tests passed!${NC}"
    exit 0
else
    echo -e "\n${RED}❌ Some tests failed${NC}"
    exit 1
fi
