package com.herdcommand.api.api.me;

import java.util.List;

public record MeResponse(
        String subject,
        String username,
        String email,
        List<String> roles
) {}
