package com.rulex.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateRuleRequest(
        @NotBlank @Size(max = 4096) String expression,
        @Size(max = 1024) String description
) {}
