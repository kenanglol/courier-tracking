package com.migros.couriertracking.repository;

import com.migros.couriertracking.entity.StoreEntrance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StoreEntranceRepository extends JpaRepository<StoreEntrance, Long> {

    @Query("SELECT se FROM StoreEntrance se WHERE se.courierId = :courierId AND se.store.id = :storeId AND se.entranceTime >= :startTime ORDER BY se.entranceTime DESC")
    Optional<StoreEntrance> findLastEntranceInPeriod(
            @Param("courierId") String courierId,
            @Param("storeId") Long storeId,
            @Param("startTime") LocalDateTime startTime);

    @Query("SELECT se FROM StoreEntrance se WHERE se.courierId = :courierId ORDER BY se.entranceTime DESC")
    List<StoreEntrance> findByCourierIdOrderByEntranceTimeDesc(@Param("courierId") String courierId);
}
