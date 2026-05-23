package com.rulex.dto;

import java.time.Instant;

public record RuleDto(
        Long id,
        String name,
        String expression,
        String description,
        Instant createdAt,
        Instant updatedAt
) {}
