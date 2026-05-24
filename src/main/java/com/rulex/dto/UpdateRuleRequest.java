package com.rulex.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Request body for updating an existing rule. Null fields are ignored (partial update).")
public record UpdateRuleRequest(

        @Schema(description = "New rule name — omit to keep existing name", example = "elder-check")
        @Size(max = 256) String name,

        @Schema(description = "New rule expression — omit to keep existing expression", example = "age > 65")
        @Size(max = 4096) String expression,

        @Schema(description = "New description — omit to keep existing description", example = "Matches customers over 65")
        @Size(max = 1024) String description) {
}
