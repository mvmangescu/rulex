package com.rulex.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record EvaluateRequest(
        @NotBlank(message = "Rule expression must not be blank")
        @Size(max = 4096, message = "Rule expression must not exceed 4096 characters")
        String rule,

        @NotNull(message = "Context must not be null")
        Map<String, Object> context) {
}
