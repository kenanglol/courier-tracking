package com.migros.couriertracking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.migros.couriertracking.dto.CourierLocationRequest;
import com.migros.couriertracking.dto.TotalTravelDistanceResponse;
import com.migros.couriertracking.service.InMemoryCourierTrackingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CourierTrackingController.class)
@DisplayName("CourierTrackingController Integration Tests")
class CourierTrackingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InMemoryCourierTrackingService courierTrackingService;

    @Test
    @DisplayName("Should log courier location successfully")
    void testLogCourierLocationSuccess() throws Exception {
        // Given
        CourierLocationRequest request = new CourierLocationRequest("COURIER001", 41.0840, 29.0093,
                System.currentTimeMillis());
        doNothing().when(courierTrackingService).logCourierLocation(any(CourierLocationRequest.class));

        // When & Then
        mockMvc.perform(post("/api/couriers/location")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Location logged successfully"));

        verify(courierTrackingService).logCourierLocation(any(CourierLocationRequest.class));
    }

    @Test
    @DisplayName("Should return validation error for missing courier ID")
    void testLogLocationMissingCourierId() throws Exception {
        // Given
        CourierLocationRequest request = new CourierLocationRequest(null, 41.0840, 29.0093, System.currentTimeMillis());

        // When & Then
        mockMvc.perform(post("/api/couriers/location")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(courierTrackingService, never()).logCourierLocation(any());
    }

    @Test
    @DisplayName("Should return validation error for empty courier ID")
    void testLogLocationEmptyCourierId() throws Exception {
        // Given
        CourierLocationRequest request = new CourierLocationRequest("", 41.0840, 29.0093, System.currentTimeMillis());

        // When & Then
        mockMvc.perform(post("/api/couriers/location")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(courierTrackingService, never()).logCourierLocation(any());
    }

    @Test
    @DisplayName("Should return validation error for missing latitude")
    void testLogLocationMissingLatitude() throws Exception {
        // Given
        CourierLocationRequest request = new CourierLocationRequest("COURIER001", null, 29.0093,
                System.currentTimeMillis());

        // When & Then
        mockMvc.perform(post("/api/couriers/location")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(courierTrackingService, never()).logCourierLocation(any());
    }

    @Test
    @DisplayName("Should return validation error for missing longitude")
    void testLogLocationMissingLongitude() throws Exception {
        // Given
        CourierLocationRequest request = new CourierLocationRequest("COURIER001", 41.0840, null,
                System.currentTimeMillis());

        // When & Then
        mockMvc.perform(post("/api/couriers/location")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(courierTrackingService, never()).logCourierLocation(any());
    }

    @Test
    @DisplayName("Should return validation error for missing timestamp")
    void testLogLocationMissingTimestamp() throws Exception {
        // Given
        CourierLocationRequest request = new CourierLocationRequest("COURIER001", 41.0840, 29.0093, null);

        // When & Then
        mockMvc.perform(post("/api/couriers/location")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(courierTrackingService, never()).logCourierLocation(any());
    }

    @Test
    @DisplayName("Should return validation error for invalid JSON")
    void testLogLocationInvalidJson() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/couriers/location")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ invalid json }"))
                .andExpect(status().isBadRequest());

        verify(courierTrackingService, never()).logCourierLocation(any());
    }

    @Test
    @DisplayName("Should get total travel distance successfully")
    void testGetTotalTravelDistanceSuccess() throws Exception {
        // Given
        String courierId = "COURIER001";
        TotalTravelDistanceResponse response = new TotalTravelDistanceResponse(courierId, 1500.75);
        when(courierTrackingService.getTotalTravelDistance(courierId)).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/couriers/{courierId}/total-travel-distance", courierId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.courierId").value(courierId))
                .andExpect(jsonPath("$.totalDistance").value(1500.75));

        verify(courierTrackingService).getTotalTravelDistance(courierId);
    }

    @Test
    @DisplayName("Should get zero distance for non-existent courier")
    void testGetTotalTravelDistanceNonExistentCourier() throws Exception {
        // Given
        String courierId = "NONEXISTENT";
        TotalTravelDistanceResponse response = new TotalTravelDistanceResponse(courierId, 0.0);
        when(courierTrackingService.getTotalTravelDistance(courierId)).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/couriers/{courierId}/total-travel-distance", courierId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.courierId").value(courierId))
                .andExpect(jsonPath("$.totalDistance").value(0.0));

        verify(courierTrackingService).getTotalTravelDistance(courierId);
    }

    @Test
    @DisplayName("Should handle special characters in courier ID")
    void testGetTotalTravelDistanceSpecialCharacters() throws Exception {
        // Given
        String courierId = "COURIER-001_TEST";
        TotalTravelDistanceResponse response = new TotalTravelDistanceResponse(courierId, 500.0);
        when(courierTrackingService.getTotalTravelDistance(courierId)).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/couriers/{courierId}/total-travel-distance", courierId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courierId").value(courierId))
                .andExpect(jsonPath("$.totalDistance").value(500.0));

        verify(courierTrackingService).getTotalTravelDistance(courierId);
    }

    @Test
    @DisplayName("Should handle edge coordinate values")
    void testLogLocationEdgeCoordinates() throws Exception {
        // Given - Maximum valid coordinates
        CourierLocationRequest request = new CourierLocationRequest("COURIER001", 90.0, 180.0,
                System.currentTimeMillis());
        doNothing().when(courierTrackingService).logCourierLocation(any(CourierLocationRequest.class));

        // When & Then
        mockMvc.perform(post("/api/couriers/location")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(courierTrackingService).logCourierLocation(any(CourierLocationRequest.class));
    }

    @Test
    @DisplayName("Should accept minimum valid coordinates")
    void testLogLocationMinimumCoordinates() throws Exception {
        // Given - Minimum valid coordinates
        CourierLocationRequest request = new CourierLocationRequest("COURIER001", -90.0, -180.0,
                System.currentTimeMillis());
        doNothing().when(courierTrackingService).logCourierLocation(any(CourierLocationRequest.class));

        // When & Then
        mockMvc.perform(post("/api/couriers/location")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(courierTrackingService).logCourierLocation(any(CourierLocationRequest.class));
    }
}
