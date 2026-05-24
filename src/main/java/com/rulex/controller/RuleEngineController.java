package com.rulex.controller;

import com.rulex.dto.EvaluateRequest;
import com.rulex.dto.EvaluateResponse;
import com.rulex.dto.ValidateRequest;
import com.rulex.dto.ValidateResponse;
import com.rulex.engine.RuleEngine;
import com.rulex.engine.RuleEngine.ValidationResult;
import com.rulex.engine.function.FunctionRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/rules")
@Validated
@Tag(name = "Rule engine", description = "Stateless rule expression evaluation and validation")
public class RuleEngineController {

    private final RuleEngine ruleEngine;
    private final FunctionRegistry functionRegistry;

    @Operation(summary = "Evaluate a rule expression",
            description = "Evaluates a rule against the given context. Pass ?explain=true for a full execution trace.")
    @ApiResponse(responseCode = "200", description = "Rule evaluated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid rule or request body", content = @Content)
    @PostMapping("/evaluate")
    public ResponseEntity<EvaluateResponse> evaluate(
            @Valid @RequestBody EvaluateRequest request,
            @Parameter(description = "Return full execution trace") @RequestParam(required = false, defaultValue = "false") boolean explain) {
        log.info("Evaluate rule, length={}, explain={}", request.rule().length(), explain);
        if (explain) {
            RuleEngine.TraceResult traced = ruleEngine.evaluateWithTrace(request.rule(), request.context());
            return ResponseEntity.ok(EvaluateResponse.withTrace(
                    traced.result(), request.rule(), traced.trace()));
        }
        boolean result = ruleEngine.evaluate(request.rule(), request.context());
        return ResponseEntity.ok(EvaluateResponse.of(result, request.rule()));
    }

    @Operation(summary = "Validate a rule expression",
            description = "Checks rule syntax without evaluating against any context.")
    @ApiResponse(responseCode = "200", description = "Validation result returned (valid or invalid with error message)")
    @ApiResponse(responseCode = "400", description = "Malformed request body", content = @Content)
    @PostMapping("/validate")
    public ResponseEntity<ValidateResponse> validate(@Valid @RequestBody ValidateRequest request) {
        log.info("Validate rule, length={}", request.rule().length());
        ValidationResult result = ruleEngine.validate(request.rule());
        return ResponseEntity.ok(new ValidateResponse(result.valid(), result.error()));
    }

    @Operation(summary = "List available built-in functions",
            description = "Returns the names of all functions that can be used in rule expressions.")
    @ApiResponse(responseCode = "200", description = "Set of function names")
    @GetMapping("/functions")
    public ResponseEntity<Set<String>> functions() {
        return ResponseEntity.ok(functionRegistry.getFunctionNames());
    }
}
