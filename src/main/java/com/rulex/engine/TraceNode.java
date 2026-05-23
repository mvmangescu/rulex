package com.rulex.engine;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TraceNode(
        String expression,
        String type,
        boolean result,
        String evaluated,
        String error,
        List<TraceNode> children
) {
    public static TraceNode leaf(String expression, String type, boolean result, String evaluated) {
        return new TraceNode(expression, type, result, evaluated, null, null);
    }

    public static TraceNode compound(String expression, String type, boolean result, List<TraceNode> children) {
        return new TraceNode(expression, type, result, null, null, children);
    }

    public static TraceNode error(String expression, String type, String error) {
        return new TraceNode(expression, type, false, null, error, null);
    }
}
