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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Collection;
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
    @ApiResponse(responseCode = "400", description = "Validation error", content = @Content)
    @ApiResponse(responseCode = "409", description = "Rule name already exists", content = @Content)
    @PostMapping
    public ResponseEntity<RuleResponse> create(@Valid @RequestBody CreateRuleRequest request) {
        log.info("Creating rule '{}'", request.name());
        RuleResponse created = ruleService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/rules/" + created.name())).body(created);
    }

    @Operation(summary = "Update a rule", description = "Returns 404 if the rule does not exist.")
    @ApiResponse(responseCode = "200", description = "Rule updated")
    @ApiResponse(responseCode = "400", description = "Validation error", content = @Content)
    @ApiResponse(responseCode = "404", description = "Rule not found", content = @Content)
    @PutMapping("/{name}")
    public ResponseEntity<RuleResponse> update(
            @Parameter(description = "Rule name") @PathVariable @NotBlank @Size(max = 256) String name,
            @Valid @RequestBody UpdateRuleRequest request) {
        log.info("Updating rule '{}'", name);
        return ResponseEntity.ok(ruleService.update(name, request));
    }

    @Operation(summary = "Get a rule by name")
    @ApiResponse(responseCode = "200", description = "Rule found")
    @ApiResponse(responseCode = "404", description = "Rule not found", content = @Content)
    @GetMapping("/{name}")
    public ResponseEntity<RuleResponse> findByName(
            @Parameter(description = "Rule name") @PathVariable String name) {
        return ruleService.findByName(name).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "List all rules")
    @ApiResponse(responseCode = "200", description = "All stored rules (empty array if none)")
    @GetMapping
    public ResponseEntity<Collection<RuleResponse>> findAll() {
        return ResponseEntity.ok(ruleService.findAll());
    }

    @Operation(summary = "Delete a rule",
            description = "Deletes the rule and evicts its expression from the compile cache.")
    @ApiResponse(responseCode = "204", description = "Rule deleted")
    @ApiResponse(responseCode = "404", description = "Rule not found", content = @Content)
    @DeleteMapping("/{name}")
    public ResponseEntity<Void> deleteByName(
            @Parameter(description = "Rule name") @PathVariable String name) {
        if (ruleService.deleteByName(name)) {
            log.info("Deleted rule '{}'", name);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @Operation(summary = "Evaluate a rule against a context",
            description = "Looks up the rule by name then evaluates it. Pass ?explain=true for a full execution trace.")
    @ApiResponse(responseCode = "200", description = "Rule evaluated successfully")
    @ApiResponse(responseCode = "404", description = "Rule not found", content = @Content)
    @PostMapping("/{name}/evaluate")
    public ResponseEntity<EvaluateResponse> evaluate(
            @Parameter(description = "Rule name") @PathVariable String name,
            @RequestBody Map<String, Object> context,
            @Parameter(description = "Return full execution trace") @RequestParam(required = false, defaultValue = "false") boolean explain) {
        RuleResponse rule = ruleService.findByName(name).orElseThrow(() -> new RuleNotFoundException(name));
        log.info("Evaluating rule '{}', explain={}", name, explain);
        if (explain) {
            RuleEngine.TraceResult traced = ruleEngine.evaluateWithTrace(rule.expression(), context);
            return ResponseEntity.ok(EvaluateResponse.withTrace(
                    traced.result(), rule.expression(), traced.trace()));
        }
        boolean result = ruleEngine.evaluate(rule.expression(), context);
        return ResponseEntity.ok(EvaluateResponse.of(result, rule.expression()));
    }
}
