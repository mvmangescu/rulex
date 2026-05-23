package com.rulex.function.builtin;

import java.util.List;

import org.springframework.stereotype.Component;

import com.rulex.engine.EvaluationContext;
import com.rulex.engine.RuleValue;
import com.rulex.exception.RuleEvaluationException;
import com.rulex.function.RuleFunction;

@Component
public class FloorFunction implements RuleFunction {

    @Override
    public String getName() {
        return "floor";
    }

    @Override
    public RuleValue execute(List<RuleValue> args, EvaluationContext context) {
        if (args.size() != 1) {
            throw new RuleEvaluationException(
                    "Function 'floor' requires exactly 1 argument, got " + args.size());
        }
        RuleValue arg = args.get(0);
        if (!arg.isNumeric()) {
            throw new RuleEvaluationException(
                    "Function 'floor' requires a numeric argument, got: " + arg.getRaw());
        }
        return RuleValue.of(Math.floor(arg.asDouble()));
    }
}
