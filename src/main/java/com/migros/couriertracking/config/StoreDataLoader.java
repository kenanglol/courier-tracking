package com.migros.couriertracking.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.migros.couriertracking.entity.Store;
import com.migros.couriertracking.repository.StoreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Component
public class StoreDataLoader implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(StoreDataLoader.class);

    private final StoreRepository storeRepository;
    private final ObjectMapper objectMapper;

    public StoreDataLoader(StoreRepository storeRepository, ObjectMapper objectMapper) {
        this.storeRepository = storeRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) throws Exception {
        if (storeRepository.count() == 0) {
            loadStoresFromJson();
        }
    }

    private void loadStoresFromJson() {
        try {
            ClassPathResource resource = new ClassPathResource("stores.json");
            InputStream inputStream = resource.getInputStream();

            List<Map<String, Object>> storeData = objectMapper.readValue(
                    inputStream, new TypeReference<List<Map<String, Object>>>() {
                    });

            for (Map<String, Object> storeMap : storeData) {
                String name = (String) storeMap.get("name");
                Double lat = ((Number) storeMap.get("lat")).doubleValue();
                Double lng = ((Number) storeMap.get("lng")).doubleValue();

                Store store = new Store(name, lat, lng);
                storeRepository.save(store);

                logger.info("Loaded store: {} at coordinates ({}, {})", name, lat, lng);
            }

            logger.info("Successfully loaded {} stores from stores.json", storeData.size());

        } catch (IOException e) {
            logger.error("Failed to load stores from stores.json", e);
            throw new RuntimeException("Failed to initialize store data", e);
        }
    }
}
