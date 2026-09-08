package com.herdcommand.api.api.farm.animal;

import java.util.List;

public record AnimalPageResponse(
        List<AnimalResponse> items,
        long total,
        int page,
        int size,
        int totalPages,
        boolean first,
        boolean last,
        String sort
) {}
