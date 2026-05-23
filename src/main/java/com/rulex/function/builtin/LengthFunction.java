package com.rulex.function.builtin;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Component;

import com.rulex.engine.EvaluationContext;
import com.rulex.engine.RuleValue;
import com.rulex.exception.RuleEvaluationException;
import com.rulex.function.RuleFunction;

@Component
public class LengthFunction implements RuleFunction {

    @Override
    public String getName() { return "length"; }

    @Override
    public RuleValue execute(List<RuleValue> args, EvaluationContext context) {
        if (args.size() != 1) {
            throw new RuleEvaluationException("Function 'length' requires exactly 1 argument, got " + args.size());
        }
        RuleValue arg = args.get(0);
        if (arg.isNull()) {
            throw new RuleEvaluationException("Function 'length' does not accept null");
        }
        Object raw = arg.getRaw();
        if (raw instanceof String s)      return RuleValue.of((double) s.length());
        if (raw instanceof Collection<?> c) return RuleValue.of((double) c.size());
        // Handles both Object[] and primitive arrays (int[], double[], etc.)
        if (raw.getClass().isArray())     return RuleValue.of((double) Array.getLength(raw));
        throw new RuleEvaluationException(
                "Function 'length' requires a String, Collection or array argument, got: "
                + raw.getClass().getSimpleName());
    }
}
