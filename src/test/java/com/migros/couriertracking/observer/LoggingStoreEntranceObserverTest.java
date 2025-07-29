package com.migros.couriertracking.observer;

import com.migros.couriertracking.entity.Store;
import com.migros.couriertracking.entity.StoreEntrance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoggingStoreEntranceObserver Tests")
class LoggingStoreEntranceObserverTest {

    private LoggingStoreEntranceObserver observer;

    @BeforeEach
    void setUp() {
        observer = new LoggingStoreEntranceObserver();
    }

    @Test
    @DisplayName("Should handle store entrance notification successfully")
    void testOnStoreEntranceSuccess() {
        // Given
        Store store = new Store("Test Migros", 41.0840, 29.0093);
        store.setId(1L);
        StoreEntrance entrance = new StoreEntrance("COURIER001", store);
        entrance.setEntranceTime(LocalDateTime.now());

        // When & Then
        assertDoesNotThrow(() -> observer.onStoreEntrance(entrance));
    }

    @Test
    @DisplayName("Should handle null store entrance gracefully")
    void testOnStoreEntranceWithNull() {
        // When & Then
        assertDoesNotThrow(() -> observer.onStoreEntrance(null));
    }

    @Test
    @DisplayName("Should handle store entrance with null courier ID")
    void testOnStoreEntranceWithNullCourierId() {
        // Given
        Store store = new Store("Test Migros", 41.0840, 29.0093);
        StoreEntrance entrance = new StoreEntrance(null, store);

        // When & Then
        assertDoesNotThrow(() -> observer.onStoreEntrance(entrance));
    }

    @Test
    @DisplayName("Should handle store entrance with null store")
    void testOnStoreEntranceWithNullStore() {
        // Given
        StoreEntrance entrance = new StoreEntrance("COURIER001", null);

        // When & Then
        assertDoesNotThrow(() -> observer.onStoreEntrance(entrance));
    }

    @Test
    @DisplayName("Should handle store entrance with null entrance time")
    void testOnStoreEntranceWithNullTime() {
        // Given
        Store store = new Store("Test Migros", 41.0840, 29.0093);
        StoreEntrance entrance = new StoreEntrance("COURIER001", store);
        entrance.setEntranceTime(null);

        // When & Then
        assertDoesNotThrow(() -> observer.onStoreEntrance(entrance));
    }

    @Test
    @DisplayName("Should handle multiple consecutive notifications")
    void testMultipleNotifications() {
        // Given
        Store store1 = new Store("Migros 1", 41.0840, 29.0093);
        Store store2 = new Store("Migros 2", 41.0766, 29.0278);

        StoreEntrance entrance1 = new StoreEntrance("COURIER001", store1);
        StoreEntrance entrance2 = new StoreEntrance("COURIER002", store2);
        StoreEntrance entrance3 = new StoreEntrance("COURIER001", store2);

        // When & Then
        assertDoesNotThrow(() -> {
            observer.onStoreEntrance(entrance1);
            observer.onStoreEntrance(entrance2);
            observer.onStoreEntrance(entrance3);
        });
    }
}
