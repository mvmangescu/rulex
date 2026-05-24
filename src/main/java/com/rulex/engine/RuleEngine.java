package com.rulex.engine;

import com.rulex.config.RuleEngineProperties;
import com.rulex.engine.function.FunctionRegistry;
import com.rulex.exception.RuleEvaluationException;
import com.rulex.exception.RuleParseException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.IntStream;

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
            ExplainingEvaluator explaining = new ExplainingEvaluator(
                    EvaluationContext.of(context), functionRegistry, properties.maxEvaluationSteps(), rule);
            explaining.visit(compiled.tree());
            TraceNode trace = explaining.getTrace();
            return new TraceResult(trace.result(), trace);
        });
    }

    public ParseTreeNode dryRun(String rule) {
        CompiledRule compiled = compiler.compile(rule);
        return toNode(compiled.tree(), rule);
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

    private ParseTreeNode toNode(ParseTree tree, String expression) {
        if (tree instanceof TerminalNode t) {
            if (t.getSymbol().getType() == Token.EOF) return null;
            return new ParseTreeNode("TOKEN", t.getText(), null);
        }
        String type = tree.getClass().getSimpleName().replace("Context", "");
        String text = null;
        if (tree instanceof ParserRuleContext prc && prc.start != null && prc.stop != null) {
            text = expression.substring(prc.start.getStartIndex(), prc.stop.getStopIndex() + 1);
        }
        List<ParseTreeNode> children = IntStream.range(0, tree.getChildCount())
                .mapToObj(i -> toNode(tree.getChild(i), expression))
                .filter(Objects::nonNull)
                .toList();
        return new ParseTreeNode(type, text, children.isEmpty() ? null : children);
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
            log.error("Unexpected {} error: {}", operation, e.getMessage(), e);
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
