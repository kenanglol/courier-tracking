package com.migros.couriertracking.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Entity
@Table(name = "store_entrances")
public class StoreEntrance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Courier ID is required")
    @Column(name = "courier_id", nullable = false)
    private String courierId;

    @NotNull(message = "Store is required")
    @ManyToOne
    @JoinColumn(name = "store_id")
    private Store store;

    @Column(name = "entrance_time", nullable = false)
    private LocalDateTime entranceTime;

    public StoreEntrance() {
        this.entranceTime = LocalDateTime.now();
    }

    public StoreEntrance(String courierId, Store store) {
        this();
        this.courierId = courierId;
        this.store = store;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCourierId() {
        return courierId;
    }

    public void setCourierId(String courierId) {
        this.courierId = courierId;
    }

    public Store getStore() {
        return store;
    }

    public void setStore(Store store) {
        this.store = store;
    }

    public LocalDateTime getEntranceTime() {
        return entranceTime;
    }

    public void setEntranceTime(LocalDateTime entranceTime) {
        this.entranceTime = entranceTime;
    }
}
