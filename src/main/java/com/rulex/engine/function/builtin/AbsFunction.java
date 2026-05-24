package com.rulex.engine.function.builtin;

import com.rulex.engine.EvaluationContext;
import com.rulex.engine.RuleValue;
import com.rulex.engine.function.RuleFunction;
import com.rulex.exception.RuleEvaluationException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AbsFunction implements RuleFunction {

    @Override
    public String getName() {
        return "abs";
    }

    @Override
    public RuleValue execute(List<RuleValue> args, EvaluationContext context) {
        if (args.size() != 1) {
            throw new RuleEvaluationException("Function 'abs' requires exactly 1 argument, got " + args.size());
        }
        RuleValue arg = args.getFirst();
        if (!arg.isNumeric()) {
            throw new RuleEvaluationException("Function 'abs' requires a numeric argument, got: " + arg.getRaw());
        }
        return RuleValue.of(Math.abs(arg.asDouble()));
    }
}
