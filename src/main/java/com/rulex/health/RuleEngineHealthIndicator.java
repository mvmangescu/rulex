package com.rulex.health;

import com.github.benmanes.caffeine.cache.Cache;
import com.rulex.engine.CompiledRule;
import com.rulex.engine.function.FunctionRegistry;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

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
            return Health.up()
                    .withDetail("registeredFunctions", functionRegistry.getFunctionNames().size())
                    .withDetail("availableFunctions", functionRegistry.getFunctionNames())
                    .withDetail("cachedRules", ruleCache.estimatedSize())
                    .build();
        } catch (Exception ex) {
            return Health.down(ex)
                    .withDetail("error", ex.getMessage())
                    .build();
        }
    }
}
