package com.rulex.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Stored rule")
public record RuleResponse(

        @Schema(description = "Auto-generated rule id", example = "1")
        Long id,

        @Schema(description = "Unique rule name", example = "senior-check")
        String name,

        @Schema(description = "Rule expression", example = "age > 60")
        String expression,

        @Schema(description = "Optional description", example = "Matches customers over 60")
        String description,

        @Schema(description = "When the rule was created")
        Instant createdAt,

        @Schema(description = "When the rule was last updated")
        Instant updatedAt) {
}
