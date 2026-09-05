package com.herdcommand.api.domain.farm;

import com.herdcommand.api.api.error.ResourceNotFoundException;
import com.herdcommand.api.api.farm.FarmResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class FarmService {

    private final FarmRepository farmRepository;

    public FarmService(FarmRepository farmRepository) {
        this.farmRepository = farmRepository;
    }

    @Transactional(readOnly = true)
    public List<FarmResponse> list() {
        return farmRepository.findAllByOrderByNameArAsc().stream().map(FarmResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public Farm requireFarm(UUID farmId) {
        return farmRepository.findById(farmId).orElseThrow(ResourceNotFoundException::new);
    }
}
