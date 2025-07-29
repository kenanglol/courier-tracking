package com.migros.couriertracking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CourierLocationRequest {

    @NotBlank(message = "Courier ID is required")
    private String courierId;

    @NotNull(message = "Latitude is required")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    private Double longitude;

    @NotNull(message = "Time is required")
    private Long time;

    public CourierLocationRequest() {
    }

    public CourierLocationRequest(String courierId, Double latitude, Double longitude, Long time) {
        this.courierId = courierId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.time = time;
    }

    public String getCourierId() {
        return courierId;
    }

    public void setCourierId(String courierId) {
        this.courierId = courierId;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }
}
