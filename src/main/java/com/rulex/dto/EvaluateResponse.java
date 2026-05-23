package com.rulex.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.rulex.engine.TraceNode;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EvaluateResponse(boolean result, String rule, String requestId, TraceNode trace) {
    public static EvaluateResponse of(boolean result, String rule, String requestId) {
        return new EvaluateResponse(result, rule, requestId, null);
    }

    public static EvaluateResponse withTrace(boolean result, String rule, String requestId, TraceNode trace) {
        return new EvaluateResponse(result, rule, requestId, trace);
    }
}
