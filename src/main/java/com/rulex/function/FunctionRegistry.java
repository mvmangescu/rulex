package com.rulex.function;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import com.rulex.engine.EvaluationContext;
import com.rulex.engine.RuleValue;
import com.rulex.exception.FunctionNotFoundException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FunctionRegistry {

    private final ConcurrentHashMap<String, RuleFunction> functions = new ConcurrentHashMap<>();
    private volatile Set<String> sortedNamesCache;

    public void register(RuleFunction fn) {
        String name = fn.getName().toLowerCase();
        functions.put(name, fn);
        sortedNamesCache = null;
        log.debug("Registered function: {}", name);
    }

    public RuleValue execute(String name, List<RuleValue> args, EvaluationContext ctx) {
        String key = name.toLowerCase();
        RuleFunction fn = functions.get(key);
        if (fn == null) {
            throw new FunctionNotFoundException(name, getFunctionNames());
        }
        return fn.execute(args, ctx);
    }

    public boolean contains(String name) {
        return functions.containsKey(name.toLowerCase());
    }

    public Set<String> getFunctionNames() {
        Set<String> cached = sortedNamesCache;
        if (cached == null) {
            cached = Collections.unmodifiableSet(new TreeSet<>(functions.keySet()));
            sortedNamesCache = cached;
        }
        return cached;
    }
}
