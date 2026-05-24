package com.rulex.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Stored named rule")
public record RuleResponse(

        @Schema(description = "Auto-generated rule id", example = "1")
        Long id,

        @Schema(description = "Unique rule name", example = "senior-check")
        @NotBlank @Size(max = 256) String name,

        @Schema(description = "Rule expression", example = "age > 60")
        @NotBlank @Size(max = 4096) String expression,

        @Schema(description = "Optional description", example = "Matches customers over 60")
        String description) {
}
