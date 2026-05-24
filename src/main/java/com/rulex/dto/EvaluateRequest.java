package com.rulex.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

@Schema(description = "Request body for evaluating a rule expression")
public record EvaluateRequest(

        @Schema(description = "Rule expression to evaluate", example = "age > 18 AND active = true")
        @NotBlank(message = "Rule expression must not be blank")
        @Size(max = 4096, message = "Rule expression must not exceed 4096 characters")
        String rule,

        @Schema(description = "Key-value context the rule is evaluated against",
                example = "{\"age\": 25, \"active\": true}")
        @NotNull(message = "Context must not be null")
        Map<String, Object> context) {
}
