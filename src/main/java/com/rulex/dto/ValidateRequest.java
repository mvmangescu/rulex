package com.rulex.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request body for validating a rule expression")
public record ValidateRequest(

        @Schema(description = "Rule expression to validate", example = "age > 18 AND active = true")
        @NotBlank(message = "Rule expression must not be blank")
        @Size(max = 4096, message = "Rule expression must not exceed 4096 characters")
        String rule) {
}
