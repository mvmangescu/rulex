package com.rulex.controller;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.rulex.exception.GlobalExceptionHandler;
import com.rulex.web.RequestIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rulex.config.RuleEngineProperties;
import com.rulex.engine.RuleEngine;
import com.rulex.function.FunctionRegistry;
import com.rulex.dto.BatchEvaluateRequest;
import com.rulex.dto.BatchEvaluateResponse;
import com.rulex.dto.BatchEvaluateResponse.BatchResult;
import com.rulex.dto.EvaluateRequest;
import com.rulex.dto.EvaluateResponse;
import com.rulex.dto.ValidateRequest;
import com.rulex.dto.ValidateResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/rules")
@Validated
@Tag(name = "Rule Engine", description = "Evaluate, validate, and batch-evaluate rule expressions")
public class RuleController {

    private static final Logger log = LoggerFactory.getLogger(RuleController.class);

    private final RuleEngine ruleEngine;
    private final FunctionRegistry functionRegistry;
    private final RuleEngineProperties properties;

    public RuleController(RuleEngine ruleEngine, FunctionRegistry functionRegistry,
                          RuleEngineProperties properties) {
        this.ruleEngine = ruleEngine;
        this.functionRegistry = functionRegistry;
        this.properties = properties;
    }

    @Operation(summary = "Evaluate a rule expression",
               description = "Evaluates a rule against the given context and returns a boolean result. " +
                             "Pass ?explain=true to include a full execution trace in the response.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evaluated successfully",
                    content = @Content(schema = @Schema(implementation = EvaluateResponse.class))),
            @ApiResponse(responseCode = "400", description = "Parse error or missing fields",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "Evaluation error",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorResponse.class)))
    })
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
               description = "Checks rule syntax without evaluating it against any context.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Validation result",
                    content = @Content(schema = @Schema(implementation = ValidateResponse.class))),
            @ApiResponse(responseCode = "400", description = "Missing required fields",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorResponse.class)))
    })
    @PostMapping("/validate")
    public ResponseEntity<ValidateResponse> validate(@Valid @RequestBody ValidateRequest request) {
        log.info("Validate rule, length={}", request.rule().length());
        RuleEngine.ValidationResult result = ruleEngine.validate(request.rule());
        return ResponseEntity.ok(new ValidateResponse(result.valid(), result.error()));
    }

    @Operation(summary = "Batch-evaluate multiple rule expressions",
               description = "Evaluates multiple rules in a single call. Individual failures do not fail the batch.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Batch results (per-rule success or error)",
                    content = @Content(schema = @Schema(implementation = BatchEvaluateResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorResponse.class)))
    })
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
                boolean result = ruleEngine.evaluate(item.rule(), item.context());
                results.add(BatchResult.ok(index, item.rule(), result));
            } catch (Exception e) {
                String message = Objects.requireNonNullElse(e.getMessage(), e.getClass().getSimpleName());
                results.add(BatchResult.fail(index, item.rule(), message));
            }
            index++;
        }
        return ResponseEntity.ok(new BatchEvaluateResponse(results, requestId()));
    }

    @Operation(summary = "List available built-in functions",
               description = "Returns the names of all registered functions usable in rule expressions.")
    @GetMapping("/functions")
    public ResponseEntity<java.util.Set<String>> functions() {
        return ResponseEntity.ok(functionRegistry.getFunctionNames());
    }

    private static String requestId() {
        return MDC.get(RequestIdFilter.MDC_KEY);
    }
}
