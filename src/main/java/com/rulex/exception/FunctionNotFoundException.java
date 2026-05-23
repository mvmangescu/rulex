package com.rulex.exception;

import java.util.Set;

public class FunctionNotFoundException extends RuleEvaluationException {

    public FunctionNotFoundException(String functionName, Set<String> available) {
        super("Function not found: '" + functionName + "'. Available functions: " + available);
    }
}
