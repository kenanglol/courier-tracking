package com.migros.couriertracking.observer;

import com.migros.couriertracking.entity.StoreEntrance;

public interface StoreEntranceObserver {

    void onStoreEntrance(StoreEntrance storeEntrance);
}
