package com.herdcommand.api.domain.farm;

import com.herdcommand.api.api.common.PageResponse;
import com.herdcommand.api.api.error.ApiError;
import com.herdcommand.api.api.error.ApiException;
import com.herdcommand.api.api.error.BadRequestException;
import com.herdcommand.api.api.error.ErrorCodes;
import com.herdcommand.api.api.error.ResourceNotFoundException;
import com.herdcommand.api.api.platform.farm.PlatformFarmDetailsResponse;
import com.herdcommand.api.api.platform.farm.PlatformFarmListItemResponse;
import com.herdcommand.api.api.platform.farm.PlatformFarmSummaryResponse;
import com.herdcommand.api.domain.feature.FarmFeatureCodeView;
import com.herdcommand.api.domain.feature.FarmFeatureRepository;
import com.herdcommand.api.domain.feature.FeatureLocales;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PlatformFarmQueryService {

    public static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> SORTABLE = Set.of(
            "createdAt", "code", "status", "nameAr", "nameEn", "nameFr");
    private static final List<FarmMembershipStatus> VISIBLE_OWNER_STATUSES = List.of(
            FarmMembershipStatus.ACTIVE, FarmMembershipStatus.INVITED, FarmMembershipStatus.SUSPENDED);

    private final FarmRepository farmRepository;
    private final FarmMembershipRepository membershipRepository;
    private final FarmSubscriptionRepository subscriptionRepository;
    private final FarmFeatureRepository farmFeatureRepository;

    public PlatformFarmQueryService(
            FarmRepository farmRepository,
            FarmMembershipRepository membershipRepository,
            FarmSubscriptionRepository subscriptionRepository,
            FarmFeatureRepository farmFeatureRepository) {
        this.farmRepository = farmRepository;
        this.membershipRepository = membershipRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.farmFeatureRepository = farmFeatureRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<PlatformFarmListItemResponse> search(
            String search,
            String status,
            String planCode,
            String featureCode,
            String acceptLanguage,
            Pageable pageable) {
        String language = FeatureLocales.fromAcceptLanguage(acceptLanguage);
        Pageable safePage = sanitize(pageable);
        Page<Farm> page = farmRepository.searchPlatformFarms(
                blankToNull(search),
                parseStatus(status),
                parsePlan(planCode),
                blankToNull(featureCode),
                FarmMembershipRole.FARM_OWNER,
                FarmMembershipStatus.REMOVED,
                safePage);
        FarmLookups lookups = loadLookups(page.getContent());
        return new PageResponse<>(
                page.getContent().stream().map(farm -> toListItem(farm, language, lookups)).toList(),
                page.getTotalElements(),
                page.getNumber(),
                page.getSize(),
                page.getSort().toString());
    }

    @Transactional(readOnly = true)
    public PlatformFarmDetailsResponse get(UUID farmId, String acceptLanguage) {
        Farm farm = farmRepository.findById(farmId).orElseThrow(ResourceNotFoundException::new);
        String language = FeatureLocales.fromAcceptLanguage(acceptLanguage);
        FarmLookups lookups = loadLookups(List.of(farm));
        return toDetails(farm, language, lookups);
    }

    @Transactional(readOnly = true)
    public PlatformFarmSummaryResponse summary() {
        EnumMap<FarmStatus, Long> counts = new EnumMap<>(FarmStatus.class);
        for (FarmStatus status : FarmStatus.values()) {
            counts.put(status, 0L);
        }
        long total = 0;
        for (Object[] row : farmRepository.countGroupedByStatus()) {
            FarmStatus status = (FarmStatus) row[0];
            long count = ((Number) row[1]).longValue();
            counts.put(status, count);
            total += count;
        }
        return new PlatformFarmSummaryResponse(
                total,
                counts.get(FarmStatus.ACTIVE),
                counts.get(FarmStatus.SETUP),
                counts.get(FarmStatus.SUSPENDED),
                counts.get(FarmStatus.ARCHIVED),
                0L);
    }

    private FarmLookups loadLookups(List<Farm> farms) {
        if (farms.isEmpty()) {
            return FarmLookups.empty();
        }
        List<UUID> ids = farms.stream().map(Farm::getId).toList();
        Map<UUID, FarmMembership> owners = membershipRepository
                .findByFarmIdInAndRoleCodeAndStatusIn(ids, FarmMembershipRole.FARM_OWNER, VISIBLE_OWNER_STATUSES)
                .stream()
                .sorted(ownerPreference())
                .collect(Collectors.toMap(FarmMembership::getFarmId, membership -> membership, (first, ignored) -> first, LinkedHashMap::new));
        Map<UUID, FarmSubscription> subscriptions = subscriptionRepository.findByFarmIdIn(ids).stream()
                .collect(Collectors.toMap(FarmSubscription::getFarmId, subscription -> subscription, (first, ignored) -> first));
        Map<UUID, List<String>> features = new LinkedHashMap<>();
        for (FarmFeatureCodeView row : farmFeatureRepository.findEnabledFeatureCodes(ids)) {
            features.computeIfAbsent(row.getFarmId(), ignored -> new ArrayList<>()).add(row.getCode());
        }
        return new FarmLookups(owners, subscriptions, features);
    }

    private static Comparator<FarmMembership> ownerPreference() {
        return Comparator
                .comparingInt((FarmMembership membership) -> membership.getStatus() == FarmMembershipStatus.ACTIVE ? 0 : 1)
                .thenComparing(FarmMembership::getInvitedAt, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private PlatformFarmListItemResponse toListItem(Farm farm, String language, FarmLookups lookups) {
        FarmMembership owner = lookups.owners.get(farm.getId());
        FarmSubscription subscription = lookups.subscriptions.get(farm.getId());
        return new PlatformFarmListItemResponse(
                farm.getId(),
                farm.getCode(),
                localizedName(farm, language),
                owner == null ? null : owner.getDisplayName(),
                owner == null ? null : owner.getInvitedEmail(),
                subscription == null ? null : subscription.getPlanCode().name(),
                0,
                subscription == null ? 0 : subscription.getMaxActiveAnimals(),
                lookups.features.getOrDefault(farm.getId(), List.of()),
                farm.getStatus().name(),
                farm.getCreatedAt(),
                farm.getVersion() == null ? 0L : farm.getVersion());
    }

    private PlatformFarmDetailsResponse toDetails(Farm farm, String language, FarmLookups lookups) {
        FarmMembership owner = lookups.owners.get(farm.getId());
        FarmSubscription subscription = lookups.subscriptions.get(farm.getId());
        return new PlatformFarmDetailsResponse(
                farm.getId(),
                farm.getCode(),
                localizedName(farm, language),
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
                owner == null ? null : owner.getDisplayName(),
                owner == null ? null : owner.getInvitedEmail(),
                owner == null ? null : owner.getStatus().name(),
                subscription == null ? null : subscription.getPlanCode().name(),
                0,
                subscription == null ? 0 : subscription.getMaxActiveAnimals(),
                subscription == null ? null : subscription.getMaxTeamMembers(),
                subscription == null ? null : subscription.getTrialEndsAt(),
                lookups.features.getOrDefault(farm.getId(), List.of()),
                farm.getCreatedAt(),
                farm.getCreatedBy(),
                farm.getVersion() == null ? 0L : farm.getVersion());
    }

    static String localizedName(Farm farm, String language) {
        return switch (language) {
            case "fr" -> farm.getNameFr();
            case "en" -> farm.getNameEn();
            default -> farm.getNameAr();
        };
    }

    public static Pageable sanitize(Pageable pageable) {
        int size = Math.min(Math.max(pageable.getPageSize(), 1), MAX_PAGE_SIZE);
        int page = Math.max(pageable.getPageNumber(), 0);
        List<Sort.Order> orders = new ArrayList<>();
        for (Sort.Order order : pageable.getSort()) {
            if (!SORTABLE.contains(order.getProperty())) {
                throw new ApiException(
                        ErrorCodes.VALIDATION_ERROR,
                        HttpStatus.BAD_REQUEST,
                        "error.validation",
                        List.of(new ApiError.FieldError("sort", "invalid")));
            }
            orders.add(order);
        }
        Sort sort = orders.isEmpty() ? Sort.by(Sort.Direction.DESC, "createdAt") : Sort.by(orders);
        return PageRequest.of(page, size, sort);
    }

    private static FarmStatus parseStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return FarmStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(
                    ErrorCodes.VALIDATION_ERROR,
                    "error.validation",
                    List.of(new ApiError.FieldError("status", "invalid")));
        }
    }

    private static FarmPlanCode parsePlan(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return FarmPlanLimits.requirePlan(raw);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private record FarmLookups(
            Map<UUID, FarmMembership> owners,
            Map<UUID, FarmSubscription> subscriptions,
            Map<UUID, List<String>> features
    ) {
        static FarmLookups empty() {
            return new FarmLookups(Map.of(), Map.of(), Map.of());
        }
    }
}
