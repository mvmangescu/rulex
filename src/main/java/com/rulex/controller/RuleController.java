package com.rulex.controller;

import com.rulex.dto.CreateRuleRequest;
import com.rulex.dto.EvaluateResponse;
import com.rulex.dto.RuleResponse;
import com.rulex.dto.UpdateRuleRequest;
import com.rulex.engine.RuleEngine;
import com.rulex.exception.RuleNotFoundException;
import com.rulex.service.RuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/rules")
@Validated
@Tag(name = "Rules management", description = "Create, update, retrieve, delete, and evaluate stored rules")
public class RuleController {

    private final RuleService ruleService;
    private final RuleEngine ruleEngine;

    @Operation(summary = "Create a rule", description = "Returns 409 if the name already exists.")
    @ApiResponse(responseCode = "201", description = "Rule created",
            headers = @Header(name = "Location", description = "URL of the created rule",
                    schema = @Schema(type = "string")))
    @ApiResponse(responseCode = "400", description = "Validation error or invalid expression", content = @Content)
    @ApiResponse(responseCode = "409", description = "Rule name already exists", content = @Content)
    @PostMapping
    public ResponseEntity<RuleResponse> create(@Valid @RequestBody CreateRuleRequest request) {
        log.info("Creating rule '{}'", request.name());
        RuleResponse created = ruleService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @Operation(summary = "Update a rule", description = "Partial update — omitted fields are left unchanged.")
    @ApiResponse(responseCode = "200", description = "Rule updated")
    @ApiResponse(responseCode = "400", description = "Validation error or invalid expression", content = @Content)
    @ApiResponse(responseCode = "404", description = "Rule not found", content = @Content)
    @PutMapping("/{id}")
    public ResponseEntity<RuleResponse> update(
            @Parameter(description = "Rule id") @PathVariable Long id,
            @Valid @RequestBody UpdateRuleRequest request) {
        log.info("Updating rule id={}", id);
        return ResponseEntity.ok(ruleService.update(id, request));
    }

    @Operation(summary = "Get a rule by id")
    @ApiResponse(responseCode = "200", description = "Rule found")
    @ApiResponse(responseCode = "404", description = "Rule not found", content = @Content)
    @GetMapping("/{id}")
    public ResponseEntity<RuleResponse> findById(
            @Parameter(description = "Rule id") @PathVariable Long id) {
        return ResponseEntity.ok(ruleService.findById(id).orElseThrow(() -> new RuleNotFoundException(id)));
    }

    @Operation(summary = "List all rules")
    @ApiResponse(responseCode = "200", description = "All stored rules (empty array if none)")
    @GetMapping
    public ResponseEntity<List<RuleResponse>> findAll() {
        return ResponseEntity.ok(ruleService.findAll());
    }

    @Operation(summary = "Delete a rule",
            description = "Deletes the rule and evicts its expression from the compile cache.")
    @ApiResponse(responseCode = "204", description = "Rule deleted")
    @ApiResponse(responseCode = "404", description = "Rule not found", content = @Content)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(
            @Parameter(description = "Rule id") @PathVariable Long id) {
        if (!ruleService.deleteById(id)) {
            throw new RuleNotFoundException(id);
        }
        log.info("Deleted rule id={}", id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Evaluate a stored rule against a context",
            description = "Looks up the rule by id then evaluates it. Pass ?explain=true for a full execution trace.")
    @ApiResponse(responseCode = "200", description = "Rule evaluated successfully")
    @ApiResponse(responseCode = "400", description = "Expression parse error", content = @Content)
    @ApiResponse(responseCode = "404", description = "Rule not found", content = @Content)
    @ApiResponse(responseCode = "422", description = "Evaluation error", content = @Content)
    @PostMapping("/{id}/evaluate")
    public ResponseEntity<EvaluateResponse> evaluate(
            @Parameter(description = "Rule id") @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> context,
            @Parameter(description = "Return full execution trace")
            @RequestParam(required = false, defaultValue = "false") boolean explain) {
        RuleResponse rule = ruleService.findById(id).orElseThrow(() -> new RuleNotFoundException(id));
        Map<String, Object> ctx = context != null ? context : Map.of();
        log.info("Evaluating rule id={}, explain={}", id, explain);
        if (explain) {
            RuleEngine.TraceResult traced = ruleEngine.evaluateWithTrace(rule.expression(), ctx);
            return ResponseEntity.ok(EvaluateResponse.withTrace(traced.result(), rule.expression(), traced.trace()));
        }
        return ResponseEntity.ok(EvaluateResponse.of(ruleEngine.evaluate(rule.expression(), ctx), rule.expression()));
    }
}
