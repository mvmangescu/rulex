package com.rulex.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.rulex.engine.TraceNode;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EvaluateResponse(boolean result, String rule, TraceNode trace) {

    public static EvaluateResponse of(boolean result, String rule) {
        return new EvaluateResponse(result, rule, null);
    }

    public static EvaluateResponse withTrace(boolean result, String rule, TraceNode trace) {
        return new EvaluateResponse(result, rule, trace);
    }
}
