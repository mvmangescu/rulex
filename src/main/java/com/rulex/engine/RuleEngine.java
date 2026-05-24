package com.rulex.engine;

import com.rulex.config.RuleEngineProperties;
import com.rulex.exception.RuleEvaluationException;
import com.rulex.exception.RuleParseException;
import com.rulex.engine.function.FunctionRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class RuleEngine {

    private static final String METRIC_EVALUATE = "rulex.evaluate.duration";

    private final RuleCompiler compiler;
    private final FunctionRegistry functionRegistry;
    private final MeterRegistry meterRegistry;
    private final RuleEngineProperties properties;

    public boolean evaluate(String rule, Map<String, Object> context) {
        return measure("evaluation", () -> {
            log.debug("Evaluating rule, length={}", rule == null ? 0 : rule.length());
            CompiledRule compiled = compiler.compile(rule);
            RuleValue result = evaluator(context).visit(compiled.tree());
            log.debug("Rule evaluated to: {}", result.getRaw());
            return result.asBoolean();
        });
    }

    public TraceResult evaluateWithTrace(String rule, Map<String, Object> context) {
        return measure("trace evaluation", () -> {
            log.debug("Evaluating rule with trace, length={}", rule == null ? 0 : rule.length());
            CompiledRule compiled = compiler.compile(rule);
            TraceNode trace = new ExplainingEvaluator(evaluator(context), rule).visit(compiled.tree());
            return new TraceResult(trace.result(), trace);
        });
    }

    public ValidationResult validate(String rule) {
        try {
            compiler.validate(rule);
            return ValidationResult.success();
        } catch (Exception e) {
            log.debug("Validation failed: {}", e.getMessage());
            return ValidationResult.failure(e.getMessage());
        }
    }

    private RuleEvaluator evaluator(Map<String, Object> context) {
        return new RuleEvaluator(EvaluationContext.of(context), functionRegistry, properties.maxEvaluationSteps());
    }

    private <T> T measure(String operation, Supplier<T> action) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "success";
        try {
            return action.get();
        } catch (RuleParseException | RuleEvaluationException e) {
            outcome = "failure";
            throw e;
        } catch (Exception e) {
            outcome = "error";
            log.error("Unexpected {} error [requestId={}]: {}", operation, MDC.get("requestId"), e.getMessage(), e);
            throw new RuleEvaluationException("Unexpected " + operation + " error: " + e.getMessage(), e);
        } finally {
            sample.stop(Timer.builder(METRIC_EVALUATE).tag("outcome", outcome).register(meterRegistry));
        }
    }

    public record TraceResult(boolean result, TraceNode trace) {
    }

    public record ValidationResult(boolean valid, String error) {
        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult failure(String error) {
            return new ValidationResult(false, error);
        }
    }
}
