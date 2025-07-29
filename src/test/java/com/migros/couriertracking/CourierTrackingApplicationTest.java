package com.migros.couriertracking;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "courier.tracking.store.radius=100",
        "courier.tracking.entrance.cooldown=60000",
        "courier.tracking.sync.frequency=10",
        "courier.tracking.sync.timeout=300000"
})
@DisplayName("Courier Tracking Application Integration Tests")
class CourierTrackingApplicationTest {

    @Test
    @DisplayName("Should load Spring context successfully")
    void contextLoads() {
    }
}
