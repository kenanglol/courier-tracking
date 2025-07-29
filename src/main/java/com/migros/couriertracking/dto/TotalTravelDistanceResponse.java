package com.migros.couriertracking.dto;

public class TotalTravelDistanceResponse {

    private String courierId;
    private Double totalDistance;

    public TotalTravelDistanceResponse() {
    }

    public TotalTravelDistanceResponse(String courierId, Double totalDistance) {
        this.courierId = courierId;
        this.totalDistance = totalDistance;
    }

    // Getters and Setters
    public String getCourierId() {
        return courierId;
    }

    public void setCourierId(String courierId) {
        this.courierId = courierId;
    }

    public Double getTotalDistance() {
        return totalDistance;
    }

    public void setTotalDistance(Double totalDistance) {
        this.totalDistance = totalDistance;
    }
}
