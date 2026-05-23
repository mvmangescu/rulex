package com.rulex.engine;

import com.rulex.config.RuleEngineProperties;
import com.rulex.exception.RuleEvaluationException;
import com.rulex.exception.RuleParseException;
import com.rulex.function.FunctionRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class RuleEngine {

    private static final String METRIC_EVALUATE = "rulex.evaluate.duration";

    private final RuleCompiler compiler;
    private final FunctionRegistry functionRegistry;
    private final MeterRegistry meterRegistry;
    private final RuleEngineProperties properties;

    public RuleEngine(RuleCompiler compiler, FunctionRegistry functionRegistry,
                      MeterRegistry meterRegistry, RuleEngineProperties properties) {
        this.compiler = compiler;
        this.functionRegistry = functionRegistry;
        this.meterRegistry = meterRegistry;
        this.properties = properties;
    }

    public boolean evaluate(String rule, Map<String, Object> context) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "success";
        try {
            log.debug("Evaluating rule, length={}", rule == null ? 0 : rule.length());
            CompiledRule compiledRule = compiler.compile(rule);
            EvaluationContext evalCtx = EvaluationContext.of(context);
            RuleEvaluator evaluator = new RuleEvaluator(evalCtx, functionRegistry,
                    properties.maxEvaluationSteps());
            RuleValue result = evaluator.visit(compiledRule.tree());
            log.debug("Rule evaluated to: {}", result.getRaw());
            return result.asBoolean();
        } catch (RuleParseException | RuleEvaluationException e) {
            outcome = "failure";
            throw e;
        } catch (Exception e) {
            outcome = "error";
            log.error("Unexpected evaluation error [requestId={}]: {}", MDC.get("requestId"), e.getMessage(), e);
            throw new RuleEvaluationException("Unexpected evaluation error: " + e.getMessage(), e);
        } finally {
            sample.stop(Timer.builder(METRIC_EVALUATE)
                    .tag("outcome", outcome)
                    .register(meterRegistry));
        }
    }

    public ValidationResult validate(String rule) {
        try {
            compiler.validate(rule);
            return ValidationResult.success();
        } catch (RuleParseException e) {
            log.debug("Validation failed: {}", e.getMessage());
            return ValidationResult.failure(e.getMessage());
        } catch (Exception e) {
            log.debug("Validation failed with unexpected error: {}", e.getMessage());
            return ValidationResult.failure(e.getMessage());
        }
    }

    public TraceResult evaluateWithTrace(String rule, Map<String, Object> context) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "success";
        try {
            log.debug("Evaluating rule with trace, length={}", rule == null ? 0 : rule.length());
            CompiledRule compiledRule = compiler.compile(rule);
            EvaluationContext evalCtx = EvaluationContext.of(context);
            RuleEvaluator valueEvaluator = new RuleEvaluator(evalCtx, functionRegistry,
                    properties.maxEvaluationSteps());
            ExplainingEvaluator explainer = new ExplainingEvaluator(valueEvaluator, rule);
            TraceNode trace = explainer.visit(compiledRule.tree());
            return new TraceResult(trace.result(), trace);
        } catch (RuleParseException | RuleEvaluationException e) {
            outcome = "failure";
            throw e;
        } catch (Exception e) {
            outcome = "error";
            log.error("Unexpected trace evaluation error [requestId={}]: {}", MDC.get("requestId"), e.getMessage(), e);
            throw new RuleEvaluationException("Unexpected evaluation error: " + e.getMessage(), e);
        } finally {
            sample.stop(Timer.builder(METRIC_EVALUATE)
                    .tag("outcome", outcome)
                    .register(meterRegistry));
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
