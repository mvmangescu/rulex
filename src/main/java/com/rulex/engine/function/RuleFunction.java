package com.rulex.engine.function;

import com.rulex.engine.EvaluationContext;
import com.rulex.engine.RuleValue;

import java.util.List;

public interface RuleFunction {

    String getName();

    RuleValue execute(List<RuleValue> args, EvaluationContext context);
}
