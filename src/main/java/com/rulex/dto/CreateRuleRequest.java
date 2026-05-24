package com.rulex.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request body for creating a new rule")
public record CreateRuleRequest(

        @Schema(description = "Unique rule name", example = "senior-check")
        @NotBlank @Size(max = 256) String name,

        @Schema(description = "Rule expression to evaluate", example = "age > 60")
        @NotBlank @Size(max = 4096) String expression,

        @Schema(description = "Optional human-readable description", example = "Matches customers over 60")
        @Size(max = 1024) String description) {
}
