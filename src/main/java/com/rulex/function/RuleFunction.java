package com.rulex.function;

import java.util.List;

import com.rulex.engine.EvaluationContext;
import com.rulex.engine.RuleValue;

public interface RuleFunction {

    /**
     * Returns the name of this function (used for lookup, case-insensitive).
     */
    String getName();

    /**
     * Executes the function with the given arguments and evaluation context.
     *
     * @param args    the list of argument values
     * @param context the current evaluation context
     * @return the result of the function
     */
    RuleValue execute(List<RuleValue> args, EvaluationContext context);
}
