package com.rulex.health;

import com.github.benmanes.caffeine.cache.Cache;
import com.rulex.engine.CompiledRule;
import com.rulex.engine.function.FunctionRegistry;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class RuleEngineHealthIndicator implements HealthIndicator {

    private final FunctionRegistry functionRegistry;
    private final Cache<String, CompiledRule> ruleCache;

    public RuleEngineHealthIndicator(FunctionRegistry functionRegistry,
                                     Cache<String, CompiledRule> ruleCache) {
        this.functionRegistry = functionRegistry;
        this.ruleCache = ruleCache;
    }

    @Override
    public Health health() {
        try {
            Set<String> functions = functionRegistry.getFunctionNames();
            return Health.up()
                    .withDetail("functions", functions)
                    .withDetail("functionCount", functions.size())
                    .withDetail("cachedRules", ruleCache.estimatedSize())
                    .build();
        } catch (Exception ex) {
            return Health.down(ex).build();
        }
    }
}
