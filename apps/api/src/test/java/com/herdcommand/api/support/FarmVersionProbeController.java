package com.herdcommand.api.support;

import com.herdcommand.api.domain.farm.Farm;
import com.herdcommand.api.domain.farm.FarmTenantService;
import com.herdcommand.api.domain.farm.FarmTenantSnapshot;
import com.herdcommand.api.domain.farm.UpdateFarmIdentityCommand;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Profile("test")
@RestController
@RequestMapping("/api/v1/platform/farms")
@PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
public class FarmVersionProbeController {

    private final FarmTenantService farmTenantService;

    public FarmVersionProbeController(FarmTenantService farmTenantService) {
        this.farmTenantService = farmTenantService;
    }

    @PostMapping("/{farmId}/version-check")
    public FarmTenantSnapshot versionCheck(@PathVariable UUID farmId, @RequestBody ProbeRequest request) {
        Farm farm = farmTenantService.requireFarm(farmId);
        return farmTenantService.updateIdentity(new UpdateFarmIdentityCommand(
                farmId,
                request.version(),
                request.nameAr(),
                farm.getNameEn(),
                farm.getNameFr(),
                farm.getGovernorateCode(),
                farm.getAddress(),
                farm.getTimezone(),
                farm.getDefaultLanguage().code()));
    }

    public record ProbeRequest(long version, String nameAr) {}
}
