# Courier Tracking Microservice

A high-performance Spring Boot microservice for tracking courier locations and calculating travel distances for Migros delivery operations.

## Features

- **Real-time Location Tracking**: Log courier locations with timestamp
- **Distance Calculation**: Calculate total travel distance using Haversine formula
- **Store Proximity Detection**: Detect when couriers enter 100m radius of Migros stores
- **Entrance Cooldown**: Prevent duplicate store entrances within 1-minute window
- **High Performance**: In-memory caching with hybrid sync strategy
- **Design Patterns**: Strategy Pattern (distance calculation) and Observer Pattern (store entrance notifications)

## Architecture

### Core Service: InMemoryCourierTrackingService

- **Thread-Safe Caching**: Uses ConcurrentHashMap for safe concurrent operations
- **Hybrid Sync Strategy**: Dual-trigger mechanism (count + time based)
- **Auto Memory Management**: Automatic cleanup of inactive couriers
- **Zero External Dependencies**: No Redis, Kafka, or external cache required

### Sync Strategy Details

The service implements a smart sync mechanism to handle irregular courier traffic:

1. **Count-Based Sync**: Forces database sync every N location updates (default: 10)
2. **Time-Based Sync**: Forces sync after timeout period (default: 5 minutes)
3. **Auto Cleanup**: Removes inactive couriers after 1 hour of inactivity

This prevents:

- Memory accumulation for inactive couriers
- Data loss when couriers don't complete count thresholds
- Performance degradation under high load

## API Endpoints

### Log Courier Location

```http
POST /api/couriers/location
Content-Type: application/json

{
  "courierId": "COURIER001",
  "latitude": 40.9923307,
  "longitude": 29.1244229,
  "time": 1640995200000
}
```

### Get Total Travel Distance

```http
GET /api/couriers/{courierId}/total-travel-distance

Response:
{
  "courierId": "COURIER001",
  "totalDistance": 1500.75
}
```

## Configuration (YAML)

```yaml
spring:
  application:
    name: courier-tracking
  datasource:
    url: jdbc:h2:mem:courierdb
    driverClassName: org.h2.Driver
    username: sa
    password: password
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
  h2:
    console:
      enabled: true
      path: /h2-console

server:
  port: 8080

logging:
  level:
    com.migros.couriertracking: DEBUG

courier:
  tracking:
    store:
      radius: 100
    entrance:
      cooldown: 60000
    sync:
      frequency: 10
      timeout: 300000
```

## Database Schema

### Stores

Preloaded from `stores.json` with 5 Istanbul Migros locations:

- Ataşehir MMM Migros (40.9923307, 29.1244229)
- Novada MMM Migros (40.986106, 29.1161293)
- Beylikdüzü 5M Migros (41.006851, 28.6552262)
- Ortaköy MMM Migros (41.055783, 29.0210292)
- Caddebostan MMM Migros (40.9632463, 29.0630908)

```sql
CREATE TABLE stores (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL
);
```

### Courier Travel Summary

Aggregated distance data per courier:

```sql
CREATE TABLE courier_travel_summaries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    courier_id VARCHAR(255) UNIQUE NOT NULL,
    total_distance DOUBLE DEFAULT 0,
    last_latitude DOUBLE,
    last_longitude DOUBLE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

### Store Entrances

Records when couriers enter store proximity:

```sql
CREATE TABLE store_entrances (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    courier_id VARCHAR(255) NOT NULL,
    store_id BIGINT NOT NULL,
    entrance_time TIMESTAMP NOT NULL,
    FOREIGN KEY (store_id) REFERENCES stores(id)
);
```

## Running the Application

### Prerequisites

- Java 17+
- Maven 3.6+

### Quick Start

````bash
# Clone repository
git clone https://github.com/kenanglol/courier-tracking.git
cd courier-tracking

# Run with Maven
mvn spring-boot:run

### Building

```bash
# Create executable JAR
mvn clean package

# Run JAR
java -jar target/courier-tracking-0.0.1-SNAPSHOT.jar
````

## Design Patterns

### 1. Strategy Pattern - Distance Calculation

Allows different distance calculation algorithms:

```java
public interface DistanceCalculator {
    double calculateDistance(double lat1, double lng1, double lat2, double lng2);
}

@Component
public class HaversineDistanceCalculator implements DistanceCalculator {
    // Accurate distance calculation using Haversine formula
}
```

### 2. Observer Pattern - Store Entrance Events

Enables extensible notifications when couriers enter stores:

```java
public interface StoreEntranceObserver {
    void onStoreEntrance(StoreEntrance storeEntrance);
}

@Component
public class LoggingStoreEntranceObserver implements StoreEntranceObserver {
    // Logs store entrance events for monitoring
}
```

## Dependencies

The application uses minimal dependencies:

- **Spring Boot 3.2.1** (Web, Data JPA, Validation)
- **H2 Database** (In-memory database)
- **Jackson** (JSON processing)
- **SLF4J/Logback** (Logging)

**Removed Dependencies:**

- ~~Redis~~ (Replaced with in-memory caching)
- ~~SpringDoc/Swagger~~ (Simplified API documentation)

## Performance Characteristics

- **Throughput**: Optimized for thousands of concurrent location updates
- **Memory Usage**: Bounded with automatic cleanup mechanisms
- **Database Load**: Minimized through intelligent batching (10x reduction)
- **Response Time**: Sub-millisecond for in-memory operations
- **Scalability**: Single-instance optimized, ready for horizontal scaling

## Monitoring & Debugging

**Application Monitoring:**

- Structured logging with DEBUG level for tracking operations
- H2 Console available at `http://localhost:8080/h2-console`
- SQL query logging enabled for debugging

**Cache Management:**

- Automatic cleanup of inactive couriers (1 hour threshold)
- Memory usage bounded by cleanup mechanisms
- Sync statistics available via internal metrics

## Testing

```bash
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=CourierTrackingServiceTest

# Integration testing
mvn integration-test
```

## Production Deployment

### Single Instance Deployment

The application is optimized for single-instance deployment with:

- In-memory caching for maximum performance
- Automatic data persistence through hybrid sync
- Zero external dependencies

### Multi-Instance Considerations

For horizontal scaling:

- Use sticky sessions (session affinity)
- Consider external cache (Redis) for shared state
- Implement distributed locking for coordination

### Security Recommendations

- Add API authentication (JWT/OAuth2)
- Implement input validation and rate limiting
- Use HTTPS in production
- Configure proper logging levels

### Monitoring in Production

- Set up application metrics collection
- Monitor database connection pools
- Track cache hit ratios and sync performance
- Alert on excessive memory usage or sync delays
