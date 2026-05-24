package com.rulex.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "rulex")
@Validated
public record RuleEngineProperties(

        @DefaultValue("1000")
        @Min(1)
        @Max(100_000)
        int cacheSize,

        @DefaultValue("3600")
        @Min(1)
        @Max(86_400)
        int cacheTtlSeconds,

        @DefaultValue("4096")
        @Min(100)
        @Max(65_536)
        int maxExpressionLength,

        @DefaultValue("10000")
        @Min(100)
        @Max(1_000_000)
        int maxEvaluationSteps) {
}
