package com.migros.couriertracking.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.migros.couriertracking.dto.CourierLocationRequest;
import com.migros.couriertracking.dto.TotalTravelDistanceResponse;
import com.migros.couriertracking.service.InMemoryCourierTrackingService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/couriers")
@Validated
public class CourierTrackingController {

    private static final Logger logger = LoggerFactory.getLogger(CourierTrackingController.class);

    private final InMemoryCourierTrackingService courierTrackingService;

    public CourierTrackingController(InMemoryCourierTrackingService courierTrackingService) {
        this.courierTrackingService = courierTrackingService;
    }

    @PostMapping("/location")
    public ResponseEntity<String> logCourierLocation(@Valid @RequestBody CourierLocationRequest request) {
        logger.info("Received location update for courier: {}", request.getCourierId());

        courierTrackingService.logCourierLocation(request);

        return ResponseEntity.ok("Location logged successfully");
    }

    @GetMapping("/{courierId}/total-travel-distance")
    public ResponseEntity<TotalTravelDistanceResponse> getTotalTravelDistance(@PathVariable String courierId) {
        logger.info("Requesting total travel distance for courier: {}", courierId);

        TotalTravelDistanceResponse response = courierTrackingService.getTotalTravelDistance(courierId);

        return ResponseEntity.ok(response);
    }
}
