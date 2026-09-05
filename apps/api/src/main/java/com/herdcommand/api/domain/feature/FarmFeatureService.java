package com.herdcommand.api.domain.feature;

import com.herdcommand.api.api.error.ApiError;
import com.herdcommand.api.api.error.BadRequestException;
import com.herdcommand.api.api.error.ErrorCodes;
import com.herdcommand.api.api.platform.feature.AssignFarmFeatureRequest;
import com.herdcommand.api.api.platform.feature.FarmFeatureResponse;
import com.herdcommand.api.domain.farm.FarmService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class FarmFeatureService {

    private final FarmFeatureRepository farmFeatureRepository;
    private final FeatureCatalogService featureCatalogService;
    private final FarmService farmService;
    private final ObjectMapper objectMapper;

    public FarmFeatureService(
            FarmFeatureRepository farmFeatureRepository,
            FeatureCatalogService featureCatalogService,
            FarmService farmService,
            ObjectMapper objectMapper) {
        this.farmFeatureRepository = farmFeatureRepository;
        this.featureCatalogService = featureCatalogService;
        this.farmService = farmService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public FarmFeatureResponse assign(UUID farmId, AssignFarmFeatureRequest request) {
        farmService.requireFarm(farmId);
        FeatureCatalog feature = featureCatalogService.requireEnableable(request.featureId());
        String configuration = normalizeConfiguration(request.configurationJson());

        FarmFeature assignment = farmFeatureRepository
                .findByFarmIdAndFeatureId(farmId, feature.getId())
                .orElseGet(() -> new FarmFeature(farmId, feature.getId()));

        if (Boolean.TRUE.equals(request.enabled())) {
            assignment.enable(currentAuditor(), configuration, Instant.now());
        } else {
            assignment.disable();
            assignment.setConfigurationJson(configuration);
        }
        FarmFeature saved = farmFeatureRepository.saveAndFlush(assignment);
        return FarmFeatureResponse.from(saved, feature.getCode());
    }

    private String normalizeConfiguration(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(raw);
            if (!node.isObject()) {
                throw invalidConfiguration();
            }
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException ex) {
            throw invalidConfiguration();
        }
    }

    private static BadRequestException invalidConfiguration() {
        return new BadRequestException(
                ErrorCodes.FEATURE_ASSIGNMENT_INVALID,
                "error.feature.assignmentInvalid",
                List.of(new ApiError.FieldError("configurationJson", "invalid")));
    }

    private static String currentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return "system";
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt && jwt.getSubject() != null && !jwt.getSubject().isBlank()) {
            return jwt.getSubject();
        }
        String name = authentication.getName();
        return name == null || name.isBlank() ? "system" : name;
    }
}
