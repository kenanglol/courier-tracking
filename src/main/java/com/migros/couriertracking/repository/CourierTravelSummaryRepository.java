package com.migros.couriertracking.repository;

import com.migros.couriertracking.entity.CourierTravelSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CourierTravelSummaryRepository extends JpaRepository<CourierTravelSummary, Long> {

    Optional<CourierTravelSummary> findByCourierId(String courierId);

    boolean existsByCourierId(String courierId);
}
