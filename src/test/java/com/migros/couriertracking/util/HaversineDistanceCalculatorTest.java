package com.migros.couriertracking.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Haversine Distance Calculator Tests")
class HaversineDistanceCalculatorTest {

    private final HaversineDistanceCalculator calculator = new HaversineDistanceCalculator();

    @Test
    @DisplayName("Should calculate zero distance for same coordinates")
    void testSameCoordinates() {
        double distance = calculator.calculateDistance(41.0840, 29.0093, 41.0840, 29.0093);
        assertEquals(0.0, distance, 0.001);
    }

    @Test
    @DisplayName("Should calculate correct distance between Istanbul locations")
    void testDistanceBetweenIstanbulLocations() {
        // Kanyon to Etiler (approximately 1.7 km)
        double distance = calculator.calculateDistance(41.0840, 29.0093, 41.0766, 29.0278);
        assertTrue(distance > 1500 && distance < 2000,
                "Distance should be approximately 1.7km, but was: " + distance);
    }

    @Test
    @DisplayName("Should calculate distance symmetrically")
    void testSymmetricDistance() {
        double distance1 = calculator.calculateDistance(41.0840, 29.0093, 41.0766, 29.0278);
        double distance2 = calculator.calculateDistance(41.0766, 29.0278, 41.0840, 29.0093);
        assertEquals(distance1, distance2, 0.001);
    }

    @Test
    @DisplayName("Should handle edge coordinates correctly")
    void testEdgeCoordinates() {
        // Test with maximum valid coordinates
        double distance = calculator.calculateDistance(90.0, 180.0, -90.0, -180.0);
        assertTrue(distance > 0, "Distance should be positive for opposite coordinates");
    }

    @Test
    @DisplayName("Should calculate short distances accurately")
    void testShortDistance() {
        // Test coordinates approximately 100 meters apart
        double lat1 = 41.0840;
        double lng1 = 29.0093;
        double lat2 = 41.0849; // ~100m north
        double lng2 = 29.0093;

        double distance = calculator.calculateDistance(lat1, lng1, lat2, lng2);
        assertTrue(distance > 80 && distance < 120,
                "Distance should be approximately 100m, but was: " + distance);
    }

    @Test
    @DisplayName("Should handle very small coordinate differences")
    void testVerySmallDifferences() {
        double distance = calculator.calculateDistance(41.0840, 29.0093, 41.0840001, 29.0093001);
        assertTrue(distance >= 0 && distance < 1,
                "Very small coordinate differences should result in very small distance");
    }
}
