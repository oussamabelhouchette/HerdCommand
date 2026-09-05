package com.herdcommand.api.domain.feature;

import com.herdcommand.api.api.error.ApiException;
import com.herdcommand.api.api.error.BadRequestException;
import com.herdcommand.api.api.error.ErrorCodes;
import com.herdcommand.api.api.platform.feature.FeatureCatalogResponse;
import com.herdcommand.api.api.platform.feature.FeatureResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class FeatureCatalogService {

    private final FeatureCatalogRepository featureCatalogRepository;

    public FeatureCatalogService(FeatureCatalogRepository featureCatalogRepository) {
        this.featureCatalogRepository = featureCatalogRepository;
    }

    @Transactional(readOnly = true)
    public FeatureCatalogResponse list(boolean includeComingSoon, String acceptLanguage) {
        String language = FeatureLocales.fromAcceptLanguage(acceptLanguage);
        List<FeatureReleaseStatus> statuses = includeComingSoon
                ? List.of(FeatureReleaseStatus.AVAILABLE, FeatureReleaseStatus.COMING_SOON)
                : List.of(FeatureReleaseStatus.AVAILABLE);
        List<FeatureResponse> items = featureCatalogRepository
                .findByActiveTrueAndReleaseStatusInOrderByDisplayOrderAscCodeAsc(statuses)
                .stream()
                .map(feature -> toResponse(feature, language))
                .toList();
        return new FeatureCatalogResponse(items);
    }

    @Transactional(readOnly = true)
    public FeatureCatalog requireEnableable(UUID featureId) {
        FeatureCatalog feature = featureCatalogRepository.findById(featureId).orElseThrow(
                () -> new ApiException(ErrorCodes.FEATURE_NOT_FOUND, HttpStatus.NOT_FOUND, "error.feature.notFound"));
        if (!feature.isEnableable()) {
            throw new BadRequestException(ErrorCodes.FEATURE_NOT_AVAILABLE, "error.feature.notAvailable");
        }
        return feature;
    }

    static FeatureResponse toResponse(FeatureCatalog feature, String language) {
        return new FeatureResponse(
                feature.getId(),
                feature.getCode(),
                feature.localizedName(language),
                feature.localizedDescription(language),
                feature.getIconCode(),
                feature.getReleaseStatus().name(),
                feature.isEnableable(),
                feature.getDisplayOrder());
    }
}
