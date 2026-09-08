package com.herdcommand.api.domain.farm;

import com.herdcommand.api.api.me.OwnerFarmResponse;
import com.herdcommand.api.domain.feature.FarmFeatureCodeView;
import com.herdcommand.api.domain.feature.FarmFeatureRepository;
import com.herdcommand.api.domain.feature.FeatureLocales;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OwnerFarmService {

    private final FarmMembershipRepository membershipRepository;
    private final FarmRepository farmRepository;
    private final FarmSubscriptionRepository subscriptionRepository;
    private final FarmFeatureRepository farmFeatureRepository;

    public OwnerFarmService(
            FarmMembershipRepository membershipRepository,
            FarmRepository farmRepository,
            FarmSubscriptionRepository subscriptionRepository,
            FarmFeatureRepository farmFeatureRepository) {
        this.membershipRepository = membershipRepository;
        this.farmRepository = farmRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.farmFeatureRepository = farmFeatureRepository;
    }

    @Transactional(readOnly = true)
    public List<OwnerFarmResponse> list(String keycloakUserId, String acceptLanguage) {
        if (keycloakUserId == null || keycloakUserId.isBlank()) {
            return List.of();
        }
        List<FarmMembership> memberships = membershipRepository.findByKeycloakUserIdAndRoleCodeAndStatus(
                keycloakUserId.trim(), FarmMembershipRole.FARM_OWNER, FarmMembershipStatus.ACTIVE);
        if (memberships.isEmpty()) {
            return List.of();
        }
        List<UUID> farmIds = memberships.stream().map(FarmMembership::getFarmId).distinct().toList();
        Map<UUID, Farm> farms = farmRepository.findAllById(farmIds).stream()
                .collect(Collectors.toMap(Farm::getId, Function.identity(), (first, ignored) -> first, LinkedHashMap::new));
        Map<UUID, FarmSubscription> subscriptions = subscriptionRepository.findByFarmIdIn(farmIds).stream()
                .collect(Collectors.toMap(FarmSubscription::getFarmId, Function.identity(), (first, ignored) -> first));
        Map<UUID, List<String>> features = new LinkedHashMap<>();
        for (FarmFeatureCodeView row : farmFeatureRepository.findEnabledFeatureCodes(farmIds)) {
            features.computeIfAbsent(row.getFarmId(), ignored -> new ArrayList<>()).add(row.getCode());
        }
        String language = FeatureLocales.fromAcceptLanguage(acceptLanguage);
        return farms.values().stream()
                .map(farm -> toResponse(farm, language, subscriptions.get(farm.getId()), features.getOrDefault(farm.getId(), List.of())))
                .sorted(Comparator.comparing(OwnerFarmResponse::name, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(OwnerFarmResponse::code))
                .toList();
    }

    private static OwnerFarmResponse toResponse(
            Farm farm, String language, FarmSubscription subscription, List<String> enabledFeatureCodes) {
        boolean animalManagement = enabledFeatureCodes.stream()
                .anyMatch(code -> OwnerFarmResponse.ANIMAL_MANAGEMENT.equalsIgnoreCase(code));
        return new OwnerFarmResponse(
                farm.getId(),
                farm.getCode(),
                PlatformFarmQueryService.localizedName(farm, language),
                farm.getNameAr(),
                farm.getNameEn(),
                farm.getNameFr(),
                farm.getGovernorateCode(),
                farm.getAddress(),
                farm.getTimezone(),
                farm.getDefaultLanguage().code(),
                farm.getCurrencyCode(),
                farm.getStatus().name(),
                farm.isActive(),
                subscription == null ? null : subscription.getPlanCode().name(),
                subscription == null ? 0 : subscription.getMaxActiveAnimals(),
                enabledFeatureCodes,
                animalManagement,
                farm.getCreatedAt(),
                farm.getVersion() == null ? 0L : farm.getVersion());
    }
}
