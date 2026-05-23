package com.rulex.function.builtin;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.rulex.engine.EvaluationContext;
import com.rulex.engine.RuleValue;
import com.rulex.exception.RuleEvaluationException;
import com.rulex.function.RuleFunction;

@Component
public class LowerFunction implements RuleFunction {

    @Override
    public String getName() { return "lower"; }

    @Override
    public RuleValue execute(List<RuleValue> args, EvaluationContext context) {
        if (args.size() != 1) {
            throw new RuleEvaluationException("Function 'lower' requires exactly 1 argument, got " + args.size());
        }
        RuleValue arg = args.get(0);
        if (arg.isNull()) {
            throw new RuleEvaluationException("Function 'lower' does not accept null");
        }
        if (!(arg.getRaw() instanceof String s)) {
            throw new RuleEvaluationException(
                    "Function 'lower' requires a String argument, got: " + arg.getRaw().getClass().getSimpleName());
        }
        return RuleValue.of(s.toLowerCase(Locale.ROOT));
    }
}
