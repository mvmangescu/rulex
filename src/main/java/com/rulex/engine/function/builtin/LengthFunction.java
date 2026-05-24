package com.rulex.engine.function.builtin;

import com.rulex.engine.EvaluationContext;
import com.rulex.engine.RuleValue;
import com.rulex.engine.function.RuleFunction;
import com.rulex.exception.RuleEvaluationException;
import org.springframework.stereotype.Component;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;

@Component
public class LengthFunction implements RuleFunction {

    @Override
    public String getName() {
        return "length";
    }

    @Override
    public RuleValue execute(List<RuleValue> args, EvaluationContext context) {
        if (args.size() != 1) {
            throw new RuleEvaluationException("Function 'length' requires exactly 1 argument, got " + args.size());
        }
        RuleValue arg = args.getFirst();
        if (arg.isNull()) {
            throw new RuleEvaluationException("Function 'length' does not accept null");
        }
        Object raw = arg.getRaw();
        if (raw instanceof String s)        return RuleValue.of((double) s.length());
        if (raw instanceof Collection<?> c) return RuleValue.of((double) c.size());
        if (raw.getClass().isArray())       return RuleValue.of((double) Array.getLength(raw));
        throw new RuleEvaluationException(
                "Function 'length' requires a String, Collection or array argument, got: "
                + raw.getClass().getSimpleName());
    }
}
