package com.rulex.engine.function;

import com.rulex.engine.EvaluationContext;
import com.rulex.engine.RuleValue;
import com.rulex.exception.FunctionNotFoundException;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class FunctionRegistry {

    private final ConcurrentHashMap<String, RuleFunction> functions = new ConcurrentHashMap<>();

    public void register(RuleFunction fn) {
        String name = fn.getName().toLowerCase();
        functions.put(name, fn);
        log.debug("Registered function: {}", name);
    }

    public RuleValue execute(String name, List<RuleValue> args, EvaluationContext ctx) {
        RuleFunction fn = functions.get(name.toLowerCase());
        if (fn == null) {
            throw new FunctionNotFoundException(name, getFunctionNames());
        }
        return fn.execute(args, ctx);
    }

    public boolean contains(String name) {
        return functions.containsKey(name.toLowerCase());
    }

    public Set<String> getFunctionNames() {
        return Collections.unmodifiableSet(new TreeSet<>(functions.keySet()));
    }
}
