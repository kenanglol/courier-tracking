# API Testing Scripts

This directory contains scripts to test the Courier Tracking Microservice API endpoints.

## Files

- `test-api.sh` - Unix/Linux/Mac testing script
- `test-api.bat` - Windows testing script
- `sample-data.json` - Sample courier location data for testing

## Usage

### Unix/Linux/Mac

```bash
chmod +x test-api.sh
./test-api.sh
```

### Windows

```cmd
test-api.bat
```

## Test Scenarios

The scripts test the following scenarios:

1. **Store Entrance Detection** - Send courier locations near stores
2. **Distance Calculation** - Verify travel distance tracking
3. **Duplicate Prevention** - Test 1-minute duplicate entry prevention
4. **Multiple Couriers** - Test multiple couriers simultaneously
5. **Edge Cases** - Test boundary conditions

## Requirements

- `curl` command must be available
- Application must be running on `http://localhost:8080`

## Sample Output

```
✅ Test 1 PASSED: Store entrance detected
✅ Test 2 PASSED: Travel distance calculated correctly
✅ Test 3 PASSED: Duplicate entry prevented
```
