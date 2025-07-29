package com.migros.couriertracking.service;

import com.migros.couriertracking.dto.CourierLocationRequest;
import com.migros.couriertracking.dto.TotalTravelDistanceResponse;
import com.migros.couriertracking.entity.CourierTravelSummary;
import com.migros.couriertracking.entity.Store;
import com.migros.couriertracking.entity.StoreEntrance;
import com.migros.couriertracking.observer.StoreEntranceObserver;
import com.migros.couriertracking.repository.CourierTravelSummaryRepository;
import com.migros.couriertracking.repository.StoreEntranceRepository;
import com.migros.couriertracking.repository.StoreRepository;
import com.migros.couriertracking.util.DistanceCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InMemoryCourierTrackingService Tests")
class InMemoryCourierTrackingServiceTest {

    @Mock
    private CourierTravelSummaryRepository travelSummaryRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private StoreEntranceRepository storeEntranceRepository;

    @Mock
    private DistanceCalculator distanceCalculator;

    @Mock
    private StoreEntranceObserver storeEntranceObserver;

    private InMemoryCourierTrackingService service;

    @BeforeEach
    void setUp() {
        List<StoreEntranceObserver> observers = Arrays.asList(storeEntranceObserver);
        service = new InMemoryCourierTrackingService(
                travelSummaryRepository,
                storeRepository,
                storeEntranceRepository,
                distanceCalculator,
                observers);

        // Set configuration values
        ReflectionTestUtils.setField(service, "storeRadius", 100.0);
        ReflectionTestUtils.setField(service, "entranceCooldownMs", 60000L);
        ReflectionTestUtils.setField(service, "syncFrequency", 10);
        ReflectionTestUtils.setField(service, "syncTimeoutMs", 300000L);
    }

    @Test
    @DisplayName("Should log courier location successfully")
    void testLogCourierLocation() {
        // Given
        CourierLocationRequest request = new CourierLocationRequest("COURIER001", 41.0840, 29.0093,
                System.currentTimeMillis());
        when(storeRepository.findAll()).thenReturn(Arrays.asList());

        // When
        assertDoesNotThrow(() -> service.logCourierLocation(request));

        // Then
        verify(storeRepository).findAll();
    }

    @Test
    @DisplayName("Should calculate distance between consecutive locations")
    void testDistanceCalculationBetweenLocations() {
        // Given
        String courierId = "COURIER001";
        long currentTime = System.currentTimeMillis();

        CourierLocationRequest firstLocation = new CourierLocationRequest(courierId, 41.0000, 29.0000, currentTime);
        CourierLocationRequest secondLocation = new CourierLocationRequest(courierId, 41.0100, 29.0100,
                currentTime + 1000);

        when(storeRepository.findAll()).thenReturn(Arrays.asList());
        when(distanceCalculator.calculateDistance(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(1000.0); // 1km distance

        // When
        service.logCourierLocation(firstLocation);
        service.logCourierLocation(secondLocation);

        // Then
        verify(distanceCalculator).calculateDistance(41.0000, 29.0000, 41.0100, 29.0100);
    }

    @Test
    @DisplayName("Should detect store entrance when courier is within radius")
    void testStoreEntranceDetection() {
        // Given
        Store store = new Store("Test Migros", 41.0840, 29.0093);
        store.setId(1L);
        CourierLocationRequest request = new CourierLocationRequest("COURIER001", 41.0840, 29.0093,
                System.currentTimeMillis());

        when(storeRepository.findAll()).thenReturn(Arrays.asList(store));
        when(distanceCalculator.calculateDistance(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(50.0); // Within 100m radius
        when(storeEntranceRepository.save(any(StoreEntrance.class))).thenReturn(new StoreEntrance());

        // When
        service.logCourierLocation(request);

        // Then
        verify(storeEntranceRepository).save(any(StoreEntrance.class));
        verify(storeEntranceObserver).onStoreEntrance(any(StoreEntrance.class));
    }

    @Test
    @DisplayName("Should not create store entrance when outside radius")
    void testNoStoreEntranceWhenOutsideRadius() {
        // Given
        Store store = new Store("Test Migros", 41.0840, 29.0093);
        CourierLocationRequest request = new CourierLocationRequest("COURIER001", 41.1000, 29.1000,
                System.currentTimeMillis());

        when(storeRepository.findAll()).thenReturn(Arrays.asList(store));
        when(distanceCalculator.calculateDistance(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(500.0); // Outside 100m radius

        // When
        service.logCourierLocation(request);

        // Then
        verify(storeEntranceRepository, never()).save(any(StoreEntrance.class));
        verify(storeEntranceObserver, never()).onStoreEntrance(any(StoreEntrance.class));
    }

    @Test
    @DisplayName("Should respect entrance cooldown period")
    void testEntranceCooldownPrevention() {
        // Given
        Store store = new Store("Test Migros", 41.0840, 29.0093);
        store.setId(1L);
        long currentTime = System.currentTimeMillis();

        CourierLocationRequest firstRequest = new CourierLocationRequest("COURIER001", 41.0840, 29.0093, currentTime);
        CourierLocationRequest secondRequest = new CourierLocationRequest("COURIER001", 41.0840, 29.0093,
                currentTime + 30000); // 30 seconds later

        when(storeRepository.findAll()).thenReturn(Arrays.asList(store));
        when(distanceCalculator.calculateDistance(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(50.0); // Within radius
        when(storeEntranceRepository.save(any(StoreEntrance.class))).thenReturn(new StoreEntrance());

        // When
        service.logCourierLocation(firstRequest);
        service.logCourierLocation(secondRequest); // Should be blocked by cooldown

        // Then
        verify(storeEntranceRepository, times(1)).save(any(StoreEntrance.class)); // Only first entrance saved
        verify(storeEntranceObserver, times(1)).onStoreEntrance(any(StoreEntrance.class));
    }

    @Test
    @DisplayName("Should allow entrance after cooldown period expires")
    void testEntranceAfterCooldownExpires() {
        // Given
        Store store = new Store("Test Migros", 41.0840, 29.0093);
        store.setId(1L);
        long currentTime = System.currentTimeMillis();

        CourierLocationRequest firstRequest = new CourierLocationRequest("COURIER001", 41.0840, 29.0093, currentTime);
        CourierLocationRequest secondRequest = new CourierLocationRequest("COURIER001", 41.0840, 29.0093,
                currentTime + 70000); // 70 seconds later

        when(storeRepository.findAll()).thenReturn(Arrays.asList(store));
        when(distanceCalculator.calculateDistance(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(50.0); // Within radius
        when(storeEntranceRepository.save(any(StoreEntrance.class))).thenReturn(new StoreEntrance());

        // When
        service.logCourierLocation(firstRequest);
        service.logCourierLocation(secondRequest); // Should be allowed after cooldown

        // Then
        verify(storeEntranceRepository, times(2)).save(any(StoreEntrance.class)); // Both entrances saved
        verify(storeEntranceObserver, times(2)).onStoreEntrance(any(StoreEntrance.class));
    }

    @Test
    @DisplayName("Should return total travel distance for existing courier")
    void testGetTotalTravelDistanceExistingCourier() {
        // Given
        String courierId = "COURIER001";
        CourierTravelSummary summary = new CourierTravelSummary(courierId);
        summary.setTotalDistance(1500.0);

        when(travelSummaryRepository.findByCourierId(courierId)).thenReturn(Optional.of(summary));

        // When
        TotalTravelDistanceResponse response = service.getTotalTravelDistance(courierId);

        // Then
        assertNotNull(response);
        assertEquals(courierId, response.getCourierId());
        assertEquals(1500.0, response.getTotalDistance());
    }

    @Test
    @DisplayName("Should return zero distance for non-existing courier")
    void testGetTotalTravelDistanceNonExistingCourier() {
        // Given
        String courierId = "NONEXISTENT";
        when(travelSummaryRepository.findByCourierId(courierId)).thenReturn(Optional.empty());

        // When
        TotalTravelDistanceResponse response = service.getTotalTravelDistance(courierId);

        // Then
        assertNotNull(response);
        assertEquals(courierId, response.getCourierId());
        assertEquals(0.0, response.getTotalDistance());
    }

    @Test
    @DisplayName("Should sync distance to database after specified frequency")
    void testDistanceSyncByFrequency() {
        // Given
        String courierId = "COURIER001";
        CourierTravelSummary summary = new CourierTravelSummary(courierId);

        when(storeRepository.findAll()).thenReturn(Arrays.asList());
        when(distanceCalculator.calculateDistance(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(100.0);
        when(travelSummaryRepository.findByCourierId(courierId)).thenReturn(Optional.of(summary));
        when(travelSummaryRepository.save(any(CourierTravelSummary.class))).thenReturn(summary);

        // When - Send 10 location updates to trigger sync
        for (int i = 0; i < 10; i++) {
            CourierLocationRequest request = new CourierLocationRequest(courierId,
                    41.0000 + i * 0.001, 29.0000 + i * 0.001, System.currentTimeMillis() + i * 1000);
            service.logCourierLocation(request);
        }

        // Then
        verify(travelSummaryRepository, atLeastOnce()).save(any(CourierTravelSummary.class));
    }

    @Test
    @DisplayName("Should handle observer exception gracefully")
    void testObserverExceptionHandling() {
        // Given
        Store store = new Store("Test Migros", 41.0840, 29.0093);
        store.setId(1L);
        CourierLocationRequest request = new CourierLocationRequest("COURIER001", 41.0840, 29.0093,
                System.currentTimeMillis());

        when(storeRepository.findAll()).thenReturn(Arrays.asList(store));
        when(distanceCalculator.calculateDistance(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(50.0);
        when(storeEntranceRepository.save(any(StoreEntrance.class))).thenReturn(new StoreEntrance());
        doThrow(new RuntimeException("Observer error")).when(storeEntranceObserver).onStoreEntrance(any());

        // When & Then
        assertDoesNotThrow(() -> service.logCourierLocation(request));
        verify(storeEntranceRepository).save(any(StoreEntrance.class)); // Should still save
    }

    @Test
    @DisplayName("Should handle multiple couriers simultaneously")
    void testMultipleCouriersSimultaneously() {
        // Given
        when(storeRepository.findAll()).thenReturn(Arrays.asList());

        String courier1 = "COURIER001";
        String courier2 = "COURIER002";
        long currentTime = System.currentTimeMillis();

        CourierLocationRequest request1 = new CourierLocationRequest(courier1, 41.0000, 29.0000, currentTime);
        CourierLocationRequest request2 = new CourierLocationRequest(courier2, 41.1000, 29.1000, currentTime);

        // When
        assertDoesNotThrow(() -> {
            service.logCourierLocation(request1);
            service.logCourierLocation(request2);
        });

        // Then
        verify(storeRepository, times(2)).findAll();
    }

    @Test
    @DisplayName("Should handle null values gracefully")
    void testNullValueHandling() {
        // When & Then
        assertThrows(Exception.class, () -> service.logCourierLocation(null));

        TotalTravelDistanceResponse response = service.getTotalTravelDistance(null);
        assertNotNull(response);
        assertNull(response.getCourierId());
        assertEquals(0.0, response.getTotalDistance());
    }
}
