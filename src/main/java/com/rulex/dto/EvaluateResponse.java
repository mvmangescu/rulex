package com.rulex.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.rulex.engine.TraceNode;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Result of evaluating a rule expression")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EvaluateResponse(

        @Schema(description = "Whether the rule matched", example = "true")
        boolean result,

        @Schema(description = "The evaluated rule expression", example = "age > 18")
        String rule,

        @Schema(description = "Execution trace — present only when ?explain=true")
        TraceNode trace) {

    public static EvaluateResponse of(boolean result, String rule) {
        return new EvaluateResponse(result, rule, null);
    }

    public static EvaluateResponse withTrace(boolean result, String rule, TraceNode trace) {
        return new EvaluateResponse(result, rule, trace);
    }
}
