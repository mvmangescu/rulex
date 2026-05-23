package com.rulex.controller;

import com.rulex.config.RuleEngineProperties;
import com.rulex.dto.*;
import com.rulex.dto.BatchEvaluateResponse.BatchResult;
import com.rulex.engine.RuleEngine;
import com.rulex.exception.GlobalExceptionHandler;
import com.rulex.exception.NamedRuleNotFoundException;
import com.rulex.function.FunctionRegistry;
import com.rulex.dto.RuleDto;
import com.rulex.service.RuleService;
import com.rulex.web.RequestIdFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/rules")
@Validated
@Tag(name = "Rule Engine", description = "Evaluate, validate, batch-evaluate rule expressions, and manage named rules")
public class RuleController {

    private final RuleEngine ruleEngine;
    private final FunctionRegistry functionRegistry;
    private final RuleEngineProperties properties;
    private final RuleService store;

    @Operation(summary = "Evaluate a rule expression",
            description = "Evaluates a rule against the given context. Pass ?explain=true for a full execution trace.")
    @PostMapping("/evaluate")
    public ResponseEntity<EvaluateResponse> evaluate(
            @Valid @RequestBody EvaluateRequest request,
            @RequestParam(required = false, defaultValue = "false") boolean explain) {
        log.info("Evaluate rule, length={}, explain={}", request.rule().length(), explain);
        if (explain) {
            RuleEngine.TraceResult traced = ruleEngine.evaluateWithTrace(request.rule(), request.context());
            return ResponseEntity.ok(EvaluateResponse.withTrace(
                    traced.result(), request.rule(), requestId(), traced.trace()));
        }
        boolean result = ruleEngine.evaluate(request.rule(), request.context());
        return ResponseEntity.ok(EvaluateResponse.of(result, request.rule(), requestId()));
    }

    @Operation(summary = "Validate a rule expression",
            description = "Checks rule syntax without evaluating against any context.")
    @PostMapping("/validate")
    public ResponseEntity<ValidateResponse> validate(@Valid @RequestBody ValidateRequest request) {
        log.info("Validate rule, length={}", request.rule().length());
        RuleEngine.ValidationResult result = ruleEngine.validate(request.rule());
        return ResponseEntity.ok(new ValidateResponse(result.valid(), result.error()));
    }

    @Operation(summary = "Batch-evaluate multiple rule expressions",
            description = "Evaluates multiple rules in a single call. Individual failures do not fail the batch.")
    @PostMapping("/batch-evaluate")
    public ResponseEntity<?> batchEvaluate(@Valid @RequestBody BatchEvaluateRequest request) {
        if (request.rules().size() > properties.maxBatchSize()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new GlobalExceptionHandler.ErrorResponse(
                            "VALIDATION_ERROR",
                            "Batch size " + request.rules().size() + " exceeds limit of " + properties.maxBatchSize(),
                            Instant.now(), requestId()));
        }
        log.info("Batch evaluate, count={}", request.rules().size());
        List<BatchResult> results = new ArrayList<>(request.rules().size());
        int index = 0;
        for (EvaluateRequest item : request.rules()) {
            try {
                results.add(BatchResult.ok(index, item.rule(), ruleEngine.evaluate(item.rule(), item.context())));
            } catch (Exception e) {
                results.add(BatchResult.fail(index, item.rule(),
                        Objects.requireNonNullElse(e.getMessage(), e.getClass().getSimpleName())));
            }
            index++;
        }
        return ResponseEntity.ok(new BatchEvaluateResponse(results, requestId()));
    }

    @Operation(summary = "List available built-in functions")
    @GetMapping("/functions")
    public ResponseEntity<Set<String>> functions() {
        return ResponseEntity.ok(functionRegistry.getFunctionNames());
    }

    public record SaveRequest(
            @NotBlank(message = "expression must not be blank") String expression,
            String description) {}

    @Operation(summary = "Create or update a named rule",
            description = "If the name already exists, its expression is updated and the old compiled form is evicted from cache.")
    @PutMapping("/named/{name}")
    public ResponseEntity<RuleDto> saveNamed(
            @PathVariable @NotBlank @Size(max = 256) String name,
            @Valid @RequestBody SaveRequest request) {
        log.info("Saving named rule '{}'", name);
        boolean exists = store.find(name).isPresent();
        RuleDto saved = store.save(new RuleDto(null, name, request.expression(), request.description(), null, null));
        if (!exists) {
            return ResponseEntity.created(URI.create("/api/v1/rules/named/" + name)).body(saved);
        }
        return ResponseEntity.ok(saved);
    }

    @Operation(summary = "Get a named rule by name")
    @GetMapping("/named/{name}")
    public ResponseEntity<RuleDto> getNamed(@PathVariable String name) {
        return store.find(name).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "List all named rules")
    @GetMapping("/named")
    public ResponseEntity<Collection<RuleDto>> listNamed() {
        return ResponseEntity.ok(store.findAll());
    }

    @Operation(summary = "Delete a named rule",
            description = "Deletes the rule and evicts its expression from the compile cache.")
    @DeleteMapping("/named/{name}")
    public ResponseEntity<Void> deleteNamed(@PathVariable String name) {
        if (store.delete(name)) {
            log.info("Deleted named rule '{}'", name);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @Operation(summary = "Evaluate a named rule against a context")
    @PostMapping("/named/{name}/evaluate")
    public ResponseEntity<EvaluateResponse> evaluateNamed(
            @PathVariable String name,
            @RequestBody Map<String, Object> context,
            @RequestParam(required = false, defaultValue = "false") boolean explain) {
        RuleDto rule = store.find(name).orElseThrow(() -> new NamedRuleNotFoundException(name));
        log.info("Evaluating named rule '{}', explain={}", name, explain);
        if (explain) {
            RuleEngine.TraceResult traced = ruleEngine.evaluateWithTrace(rule.expression(), context);
            return ResponseEntity.ok(EvaluateResponse.withTrace(
                    traced.result(), rule.expression(), requestId(), traced.trace()));
        }
        boolean result = ruleEngine.evaluate(rule.expression(), context);
        return ResponseEntity.ok(EvaluateResponse.of(result, rule.expression(), requestId()));
    }

    private static String requestId() {
        return MDC.get(RequestIdFilter.MDC_KEY);
    }
}
