package com.rulex.engine.function.builtin;

import com.rulex.engine.EvaluationContext;
import com.rulex.engine.RuleValue;
import com.rulex.engine.function.RuleFunction;
import com.rulex.exception.RuleEvaluationException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CeilFunction implements RuleFunction {

    @Override
    public String getName() {
        return "ceil";
    }

    @Override
    public RuleValue execute(List<RuleValue> args, EvaluationContext context) {
        if (args.size() != 1) {
            throw new RuleEvaluationException("Function 'ceil' requires exactly 1 argument, got " + args.size());
        }
        RuleValue arg = args.getFirst();
        if (!arg.isNumeric()) {
            throw new RuleEvaluationException("Function 'ceil' requires a numeric argument, got: " + arg.getRaw());
        }
        return RuleValue.of(Math.ceil(arg.asDouble()));
    }
}
