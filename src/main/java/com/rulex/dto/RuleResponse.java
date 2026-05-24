package com.rulex.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RuleResponse(
        Long id,
        @NotBlank @Size(max = 256) String name,
        @NotBlank @Size(max = 4096) String expression,
        String description
) {}
