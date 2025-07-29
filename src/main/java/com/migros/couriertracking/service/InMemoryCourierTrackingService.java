package com.migros.couriertracking.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

@Service
@Transactional
public class InMemoryCourierTrackingService {

    private static final Logger logger = LoggerFactory.getLogger(InMemoryCourierTrackingService.class);

    private final CourierTravelSummaryRepository travelSummaryRepository;
    private final StoreRepository storeRepository;
    private final StoreEntranceRepository storeEntranceRepository;
    private final DistanceCalculator distanceCalculator;
    private final List<StoreEntranceObserver> storeEntranceObservers;

    @Value("${courier.tracking.store.radius:100}")
    private double storeRadius;

    @Value("${courier.tracking.entrance.cooldown:60000}")
    private long entranceCooldownMs;

    @Value("${courier.tracking.sync.frequency:10}")
    private int syncFrequency;

    @Value("${courier.tracking.sync.timeout:300000}")
    private long syncTimeoutMs;

    private final Map<String, CourierLocationData> lastLocations = new ConcurrentHashMap<>();
    private final Map<String, DoubleAdder> tempDistances = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> locationCounts = new ConcurrentHashMap<>();
    private final Map<String, Long> storeEntranceCooldowns = new ConcurrentHashMap<>();
    private final Map<String, Long> lastSyncTimes = new ConcurrentHashMap<>();

    public InMemoryCourierTrackingService(CourierTravelSummaryRepository travelSummaryRepository,
            StoreRepository storeRepository,
            StoreEntranceRepository storeEntranceRepository,
            DistanceCalculator distanceCalculator,
            List<StoreEntranceObserver> storeEntranceObservers) {
        this.travelSummaryRepository = travelSummaryRepository;
        this.storeRepository = storeRepository;
        this.storeEntranceRepository = storeEntranceRepository;
        this.distanceCalculator = distanceCalculator;
        this.storeEntranceObservers = storeEntranceObservers;
    }

    public void logCourierLocation(CourierLocationRequest request) {
        logger.debug("Processing location for courier: {} at lat: {}, lng: {} at time: {}",
                request.getCourierId(), request.getLatitude(), request.getLongitude(), request.getTime());

        String courierId = request.getCourierId();

        CourierLocationData lastLocation = lastLocations.get(courierId);

        if (lastLocation != null) {
            double distance = distanceCalculator.calculateDistance(
                    lastLocation.latitude, lastLocation.longitude,
                    request.getLatitude(), request.getLongitude());

            tempDistances.computeIfAbsent(courierId, k -> new DoubleAdder()).add(distance);
        }

        lastLocations.put(courierId, new CourierLocationData(
                request.getLatitude(),
                request.getLongitude(),
                request.getTime()));

        checkStoreProximity(courierId, request.getLatitude(), request.getLongitude(), request.getTime());

        AtomicLong count = locationCounts.computeIfAbsent(courierId, k -> new AtomicLong(0));
        long currentTime = System.currentTimeMillis();
        Long lastSyncTime = lastSyncTimes.get(courierId);

        boolean shouldSyncByCount = count.incrementAndGet() % syncFrequency == 0;
        boolean shouldSyncByTime = lastSyncTime == null || (currentTime - lastSyncTime) > syncTimeoutMs;

        if (shouldSyncByCount || shouldSyncByTime) {
            syncDistanceToDatabase(courierId);
            lastSyncTimes.put(courierId, currentTime);

            if (shouldSyncByTime && !shouldSyncByCount) {
                logger.debug("Time-based sync triggered for courier {} after {} ms",
                        courierId, lastSyncTime == null ? "never" : (currentTime - lastSyncTime));
            }
        }

        if (count.get() % 100 == 0) {
            cleanupOldData();
        }
    }

    private void syncDistanceToDatabase(String courierId) {
        try {
            DoubleAdder tempDistance = tempDistances.get(courierId);
            if (tempDistance != null && tempDistance.sum() > 0) {
                double distanceToAdd = tempDistance.sumThenReset();

                CourierTravelSummary summary = travelSummaryRepository.findByCourierId(courierId)
                        .orElseGet(() -> new CourierTravelSummary(courierId));

                summary.addDistance(distanceToAdd);

                CourierLocationData lastLocation = lastLocations.get(courierId);
                if (lastLocation != null) {
                    summary.setLastLatitude(lastLocation.latitude);
                    summary.setLastLongitude(lastLocation.longitude);
                }

                travelSummaryRepository.save(summary);

                logger.debug("Synced {} meters to database for courier {}", distanceToAdd, courierId);
            }
        } catch (Exception e) {
            logger.error("Error syncing distance to database for courier: " + courierId, e);
        }
    }

    private void checkStoreProximity(String courierId, Double latitude, Double longitude, Long time) {
        List<Store> allStores = storeRepository.findAll();

        for (Store store : allStores) {
            double distance = distanceCalculator.calculateDistance(
                    latitude, longitude,
                    store.getLatitude(), store.getLongitude());

            if (distance <= storeRadius) {
                handleStoreEntrance(courierId, store, time);
            }
        }
    }

    private void handleStoreEntrance(String courierId, Store store, Long time) {
        String cooldownKey = courierId + ":" + store.getId();
        Long lastEntrance = storeEntranceCooldowns.get(cooldownKey);

        if (lastEntrance == null || (time - lastEntrance) > entranceCooldownMs) {
            LocalDateTime entranceTime = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(time), ZoneId.systemDefault());

            StoreEntrance entrance = new StoreEntrance(courierId, store);
            entrance.setEntranceTime(entranceTime);
            storeEntranceRepository.save(entrance);

            storeEntranceCooldowns.put(cooldownKey, time);

            notifyStoreEntranceObservers(entrance);

            logger.info("New store entrance recorded for courier '{}' at store '{}' at time {}",
                    courierId, store.getName(), entranceTime);
        } else {
            logger.debug("Store entrance ignored due to cooldown period for courier '{}' at store '{}'",
                    courierId, store.getName());
        }
    }

    private void cleanupOldData() {
        long currentTime = System.currentTimeMillis();

        storeEntranceCooldowns.entrySet()
                .removeIf(entry -> (currentTime - entry.getValue()) > (entranceCooldownMs * 2));

        long inactiveThreshold = 60 * 60 * 1000;

        lastLocations.entrySet().removeIf(entry -> {
            boolean isInactive = (currentTime - entry.getValue().time) > inactiveThreshold;
            if (isInactive) {
                String courierId = entry.getKey();
                syncDistanceToDatabase(courierId);

                tempDistances.remove(courierId);
                locationCounts.remove(courierId);
                lastSyncTimes.remove(courierId);

                logger.debug("Cleaned up inactive courier: {}", courierId);
            }
            return isInactive;
        });
    }

    private void notifyStoreEntranceObservers(StoreEntrance storeEntrance) {
        for (StoreEntranceObserver observer : storeEntranceObservers) {
            try {
                observer.onStoreEntrance(storeEntrance);
            } catch (Exception e) {
                logger.error("Error notifying store entrance observer", e);
            }
        }
    }

    public TotalTravelDistanceResponse getTotalTravelDistance(String courierId) {
        syncDistanceToDatabase(courierId);

        Optional<CourierTravelSummary> summaryOpt = travelSummaryRepository.findByCourierId(courierId);

        double totalDistance = summaryOpt.map(CourierTravelSummary::getTotalDistance).orElse(0.0);

        return new TotalTravelDistanceResponse(courierId, totalDistance);
    }

    private static class CourierLocationData {
        final double latitude;
        final double longitude;
        final long time;

        CourierLocationData(double latitude, double longitude, long time) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.time = time;
        }
    }
}
