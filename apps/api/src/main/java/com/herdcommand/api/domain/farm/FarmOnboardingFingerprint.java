package com.herdcommand.api.domain.farm;

import com.herdcommand.api.api.platform.farm.CreatePlatformFarmRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.stream.Collectors;

public final class FarmOnboardingFingerprint {

    private FarmOnboardingFingerprint() {}

    public static String sha256(CreatePlatformFarmRequest request) {
        String canonical = String.join("\n",
                nv(request.nameAr()),
                nv(request.nameEn()),
                nv(request.nameFr()),
                nv(request.governorateCode()).toUpperCase(Locale.ROOT),
                nv(request.address()),
                nv(request.timezone()),
                nv(request.defaultLanguage()).toLowerCase(Locale.ROOT),
                nv(request.currencyCode()).toUpperCase(Locale.ROOT),
                nv(request.initialStatus()).toUpperCase(Locale.ROOT),
                request.owner() == null ? "" : nv(request.owner().email()).toLowerCase(Locale.ROOT),
                request.owner() == null ? "" : nv(request.owner().displayName()),
                request.owner() == null ? "" : nv(request.owner().phoneNumber()),
                request.subscription() == null ? "" : nv(request.subscription().planCode()).toUpperCase(Locale.ROOT),
                request.subscription() == null ? "" : String.valueOf(request.subscription().maxActiveAnimals()),
                request.subscription() == null ? "" : String.valueOf(request.subscription().maxTeamMembers()),
                request.subscription() == null ? "" : nv(request.subscription().trialEndsAt()),
                request.enabledFeatureCodes() == null
                        ? ""
                        : request.enabledFeatureCodes().stream()
                                .map(code -> code == null ? "" : code.trim().toUpperCase(Locale.ROOT))
                                .sorted()
                                .collect(Collectors.joining(",")));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static String nv(String value) {
        return value == null ? "" : value.trim();
    }
}
