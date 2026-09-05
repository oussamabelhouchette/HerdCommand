package com.herdcommand.api.domain.identity;

import com.herdcommand.api.api.error.ApiError;
import com.herdcommand.api.api.error.ApiException;
import com.herdcommand.api.api.error.BadRequestException;
import com.herdcommand.api.api.error.ConflictException;
import com.herdcommand.api.api.error.ErrorCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IdentityLookupService {

    private static final Logger log = LoggerFactory.getLogger(IdentityLookupService.class);

    private final IdentityDirectory identityDirectory;

    public IdentityLookupService(IdentityDirectory identityDirectory) {
        this.identityDirectory = identityDirectory;
    }

    public IdentityLookupResult lookup(String rawEmail) {
        String email;
        try {
            email = EmailAddresses.requireNormalized(rawEmail);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(
                    ErrorCodes.VALIDATION_ERROR,
                    "error.validation",
                    List.of(new ApiError.FieldError("email", "invalid")));
        }

        List<IdentityRecord> matches;
        try {
            matches = identityDirectory.findByExactEmail(email);
        } catch (IdentityProviderException ex) {
            throw unavailable();
        }

        if (matches.size() > 1) {
            log.warn("Identity lookup returned {} exact matches", matches.size());
            throw new ConflictException(ErrorCodes.IDENTITY_AMBIGUOUS, "error.identity.ambiguous");
        }
        if (matches.isEmpty()) {
            log.info("Identity lookup completed found=false invitationRequired=true");
            return IdentityLookupResult.invitationRequired(email);
        }

        IdentityRecord record = matches.get(0);
        log.info("Identity lookup completed found=true enabled={}", record.enabled());
        return IdentityLookupResult.existing(record);
    }

    private static ApiException unavailable() {
        return new ApiException(
                ErrorCodes.IDENTITY_PROVIDER_UNAVAILABLE,
                HttpStatus.SERVICE_UNAVAILABLE,
                "error.identity.unavailable");
    }
}
