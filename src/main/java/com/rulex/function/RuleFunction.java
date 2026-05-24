package com.rulex.function;

import java.util.List;

import com.rulex.engine.EvaluationContext;
import com.rulex.engine.RuleValue;

public interface RuleFunction {

    String getName();

    RuleValue execute(List<RuleValue> args, EvaluationContext context);
}
