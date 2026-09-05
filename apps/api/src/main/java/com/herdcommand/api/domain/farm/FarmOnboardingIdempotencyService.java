package com.herdcommand.api.domain.farm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.herdcommand.api.api.error.ConflictException;
import com.herdcommand.api.api.error.ErrorCodes;
import com.herdcommand.api.api.platform.farm.PlatformFarmCreatedResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class FarmOnboardingIdempotencyService {

    private static final int POLL_ATTEMPTS = 40;
    private static final long POLL_MILLIS = 50L;

    private final FarmOnboardingRequestRepository requestRepository;
    private final ObjectMapper objectMapper;

    public FarmOnboardingIdempotencyService(
            FarmOnboardingRequestRepository requestRepository, ObjectMapper objectMapper) {
        this.requestRepository = requestRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public OnboardingClaim claim(String idempotencyKey, String fingerprint) {
        return requestRepository.findByIdempotencyKey(idempotencyKey)
                .map(existing -> fromExisting(existing, fingerprint))
                .orElseGet(() -> insertStarted(idempotencyKey, fingerprint));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(UUID requestId) {
        requestRepository.findById(requestId).ifPresent(request -> {
            if (request.getStatus() == FarmOnboardingStatus.STARTED) {
                request.fail();
                requestRepository.saveAndFlush(request);
            }
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void rememberCreatedIdentity(UUID requestId, String keycloakUserId) {
        requestRepository.findById(requestId).ifPresent(request -> {
            request.rememberCreatedIdentity(keycloakUserId);
            requestRepository.saveAndFlush(request);
        });
    }

    public PlatformFarmCreatedResponse awaitReplay(String idempotencyKey, String fingerprint) {
        for (int attempt = 0; attempt < POLL_ATTEMPTS; attempt++) {
            FarmOnboardingRequest request = requestRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
            if (request != null) {
                if (!request.getRequestFingerprint().equals(fingerprint)) {
                    throw conflict();
                }
                if (request.getStatus() == FarmOnboardingStatus.COMPLETED) {
                    return readResponse(request.getResponseJson());
                }
                if (request.getStatus() == FarmOnboardingStatus.FAILED) {
                    throw conflict();
                }
            }
            sleep();
        }
        throw conflict();
    }

    private OnboardingClaim insertStarted(String idempotencyKey, String fingerprint) {
        try {
            FarmOnboardingRequest created = requestRepository.saveAndFlush(
                    new FarmOnboardingRequest(idempotencyKey, fingerprint));
            return new OnboardingClaim.Proceed(created.getId());
        } catch (DataIntegrityViolationException ex) {
            for (int attempt = 0; attempt < POLL_ATTEMPTS; attempt++) {
                FarmOnboardingRequest raced = requestRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
                if (raced != null) {
                    return fromExisting(raced, fingerprint);
                }
                sleep();
            }
            throw conflict();
        }
    }

    private OnboardingClaim fromExisting(FarmOnboardingRequest existing, String fingerprint) {
        if (!existing.getRequestFingerprint().equals(fingerprint)) {
            throw conflict();
        }
        return switch (existing.getStatus()) {
            case COMPLETED -> new OnboardingClaim.Replay(readResponse(existing.getResponseJson()));
            case FAILED -> {
                existing.restart();
                requestRepository.saveAndFlush(existing);
                yield new OnboardingClaim.Proceed(existing.getId());
            }
            case STARTED -> new OnboardingClaim.InProgress();
        };
    }

    PlatformFarmCreatedResponse readResponse(String json) {
        try {
            return objectMapper.readValue(json, PlatformFarmCreatedResponse.class);
        } catch (JsonProcessingException ex) {
            throw conflict();
        }
    }

    String writeResponse(PlatformFarmCreatedResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static ConflictException conflict() {
        return new ConflictException(ErrorCodes.IDEMPOTENCY_CONFLICT, "error.idempotency.conflict");
    }

    private static void sleep() {
        try {
            Thread.sleep(POLL_MILLIS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw conflict();
        }
    }
}
