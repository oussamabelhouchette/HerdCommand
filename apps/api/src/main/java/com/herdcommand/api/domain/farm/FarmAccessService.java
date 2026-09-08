package com.herdcommand.api.domain.farm;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class FarmAccessService {

    private final FarmMembershipRepository membershipRepository;

    public FarmAccessService(FarmMembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    @Transactional(readOnly = true)
    public void requireOwnerAccess(UUID farmId) {
        String subject = currentSubject();
        if (subject == null
                || !membershipRepository.existsByFarmIdAndKeycloakUserIdAndRoleCodeAndStatus(
                        farmId, subject, FarmMembershipRole.FARM_OWNER, FarmMembershipStatus.ACTIVE)) {
            throw new AccessDeniedException("forbidden");
        }
    }

    public static String currentSubject() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt && jwt.getSubject() != null && !jwt.getSubject().isBlank()) {
            return jwt.getSubject();
        }
        String name = authentication.getName();
        return name == null || name.isBlank() ? null : name;
    }
}
