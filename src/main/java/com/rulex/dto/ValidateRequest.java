package com.rulex.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ValidateRequest(
        @NotBlank(message = "Rule expression must not be blank")
        @Size(max = 4096, message = "Rule expression must not exceed 4096 characters")
        String rule) {
}
