package com.rulex.engine;

import lombok.Getter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Getter
public class EvaluationContext {

    private final Map<String, Object> variables;

    private EvaluationContext(Map<String, Object> variables) {
        this.variables = variables;
    }

    public static EvaluationContext of(Map<String, Object> variables) {
        return new EvaluationContext(Collections.unmodifiableMap(new HashMap<>(variables)));
    }

    public RuleValue resolve(String name) {
        if (name == null || name.isBlank()) return RuleValue.NULL;
        return RuleValue.of(variables.get(name));
    }
}
