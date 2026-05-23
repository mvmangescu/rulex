package com.rulex.controller;

import com.rulex.engine.RuleEngine;
import com.rulex.exception.NamedRuleNotFoundException;
import com.rulex.store.NamedRule;
import com.rulex.store.NamedRuleStore;
import com.rulex.web.RequestIdFilter;
import com.rulex.dto.EvaluateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Collection;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/rules/named")
@Validated
@Tag(name = "Named Rules", description = "Store, manage, and evaluate named rule expressions")
public class NamedRuleController {

    private final NamedRuleStore store;
    private final RuleEngine ruleEngine;

    public NamedRuleController(NamedRuleStore store, RuleEngine ruleEngine) {
        this.store = store;
        this.ruleEngine = ruleEngine;
    }

    public record SaveRequest(
            @NotBlank(message = "expression must not be blank")
            String expression) {
    }

    @Operation(summary = "Create or update a named rule",
            description = "If the rule name already exists, its expression is updated and the old compiled form is evicted from cache.")
    @PutMapping("/{name}")
    public ResponseEntity<NamedRule> save(
            @PathVariable
            @NotBlank(message = "Rule name must not be blank")
            @Size(max = 256, message = "Rule name must not exceed 256 characters")
            String name,
            @Valid @RequestBody SaveRequest request) {
        log.info("Saving named rule '{}'", name);
        NamedRuleStore.SaveResult result = store.save(name, request.expression());
        if (result.created()) {
            return ResponseEntity.created(URI.create("/api/v1/rules/named/" + name)).body(result.rule());
        }
        return ResponseEntity.ok(result.rule());
    }

    @Operation(summary = "Get a named rule by name")
    @GetMapping("/{name}")
    public ResponseEntity<NamedRule> get(@PathVariable String name) {
        return store.find(name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "List all named rules")
    @GetMapping
    public ResponseEntity<Collection<NamedRule>> list() {
        return ResponseEntity.ok(store.findAll());
    }

    @Operation(summary = "Delete a named rule",
            description = "Deletes the named rule and evicts its expression from the compile cache.")
    @DeleteMapping("/{name}")
    public ResponseEntity<Void> delete(@PathVariable String name) {
        if (store.delete(name)) {
            log.info("Deleted named rule '{}'", name);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @Operation(summary = "Evaluate a named rule against a context")
    @PostMapping("/{name}/evaluate")
    public ResponseEntity<EvaluateResponse> evaluate(
            @PathVariable String name,
            @RequestBody Map<String, Object> context,
            @RequestParam(required = false, defaultValue = "false") boolean explain) {
        NamedRule rule = store.find(name)
                .orElseThrow(() -> new NamedRuleNotFoundException(name));
        log.info("Evaluating named rule '{}', explain={}", name, explain);
        String requestId = MDC.get(RequestIdFilter.MDC_KEY);
        if (explain) {
            RuleEngine.TraceResult traced = ruleEngine.evaluateWithTrace(rule.expression(), context);
            return ResponseEntity.ok(EvaluateResponse.withTrace(
                    traced.result(), rule.expression(), requestId, traced.trace()));
        }
        boolean result = ruleEngine.evaluate(rule.expression(), context);
        return ResponseEntity.ok(EvaluateResponse.of(result, rule.expression(), requestId));
    }
}
