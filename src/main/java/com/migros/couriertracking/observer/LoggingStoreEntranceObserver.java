package com.migros.couriertracking.observer;

import com.migros.couriertracking.entity.StoreEntrance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingStoreEntranceObserver implements StoreEntranceObserver {

    private static final Logger logger = LoggerFactory.getLogger(LoggingStoreEntranceObserver.class);

    @Override
    public void onStoreEntrance(StoreEntrance storeEntrance) {
        if (storeEntrance == null) {
            logger.warn("Received null store entrance event");
            return;
        }

        String courierId = storeEntrance.getCourierId();
        String storeName = storeEntrance.getStore() != null ? storeEntrance.getStore().getName() : "Unknown Store";

        logger.info("Store entrance recorded: Courier '{}' entered store '{}' at {}",
                courierId,
                storeName,
                storeEntrance.getEntranceTime());
    }
}
