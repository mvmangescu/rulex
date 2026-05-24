package com.rulex.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Result of validating a rule expression")
public record ValidateResponse(

        @Schema(description = "Whether the expression is syntactically valid", example = "true")
        boolean valid,

        @Schema(description = "Parse error message — present only when valid=false", example = "Parse error at 1:5 — extraneous input '>'")
        String error) {
}
