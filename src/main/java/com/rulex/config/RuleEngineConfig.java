package com.rulex.config;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.rulex.engine.CompiledRule;
import com.rulex.engine.function.FunctionRegistry;
import com.rulex.engine.function.RuleFunction;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;

@Configuration
public class RuleEngineConfig {

    @Bean
    public Cache<String, CompiledRule> ruleCache(RuleEngineProperties props, MeterRegistry meterRegistry) {
        Cache<String, CompiledRule> cache = Caffeine.newBuilder()
                .maximumSize(props.cacheSize())
                .expireAfterWrite(props.cacheTtlSeconds(), TimeUnit.SECONDS)
                .recordStats()
                .build();
        CaffeineCacheMetrics.monitor(meterRegistry, cache, "rulex.rule.cache");
        return cache;
    }

    @Bean
    public FunctionRegistry functionRegistry(List<RuleFunction> functions) {
        FunctionRegistry registry = new FunctionRegistry();
        for (RuleFunction fn : functions) {
            registry.register(fn);
        }
        return registry;
    }
}
