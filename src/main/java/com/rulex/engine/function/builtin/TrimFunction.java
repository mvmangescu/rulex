package com.rulex.engine.function.builtin;

import com.rulex.engine.EvaluationContext;
import com.rulex.engine.RuleValue;
import com.rulex.engine.function.RuleFunction;
import com.rulex.exception.RuleEvaluationException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TrimFunction implements RuleFunction {

    @Override
    public String getName() {
        return "trim";
    }

    @Override
    public RuleValue execute(List<RuleValue> args, EvaluationContext context) {
        if (args.size() != 1) {
            throw new RuleEvaluationException("Function 'trim' requires exactly 1 argument, got " + args.size());
        }
        RuleValue arg = args.get(0);
        if (arg.isNull()) {
            throw new RuleEvaluationException("Function 'trim' does not accept null");
        }
        Object raw = arg.getRaw();
        if (!(raw instanceof String)) {
            throw new RuleEvaluationException(
                    "Function 'trim' requires a String argument, got: " + raw.getClass().getName());
        }
        return RuleValue.of(((String) raw).strip());
    }
}
