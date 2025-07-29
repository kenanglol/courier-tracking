package com.migros.couriertracking.util;

/**
 * Strategy Pattern: Interface for distance calculation algorithms
 */
public interface DistanceCalculator {

    double calculateDistance(double lat1, double lng1, double lat2, double lng2);
}
