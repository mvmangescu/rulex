# Rulex — Rule Engine

A production-ready rule engine built with Spring Boot 3 and ANTLR4. Write human-readable boolean expressions, evaluate them at runtime against any data context.

## Stack

| Component | Version |
|---|---|
| Java | 21 |
| Spring Boot | 3.2.5 |
| ANTLR | 4.13.1 |
| Cache | Caffeine |
| Storage | H2 (in-memory JPA) |
| Metrics | Micrometer / Prometheus |
| API Docs | SpringDoc / Swagger UI |

## Quick Start

```bash
mvn spring-boot:run
```

- Swagger UI: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/actuator/health
- Metrics: http://localhost:8080/actuator/prometheus

---

## Expression Syntax

### Comparisons

| Operator | Example |
|---|---|
| `=` | `status = 'active'` |
| `!=` | `status != 'banned'` |
| `>` | `age > 18` |
| `<` | `score < 50` |
| `>=` | `score >= 85.5` |
| `<=` | `price <= 99.99` |

### Boolean Logic

Precedence (highest to lowest): `NOT` → `AND` → `OR`

```
NOT active = true
age > 18 AND score > 50
status = 'A' OR status = 'B'
(age > 18 AND score > 50) OR vip = true
```

### Null and Type Checks

```
email IS NULL
email IS NOT NULL
value IS NUMERIC
```

### Membership

```
status IN ('active', 'pending', 'trial')
role NOT IN ('banned', 'suspended')
```

### Contains

Works on strings and collections:

```
name CONTAINS 'oh'
tags CONTAINS 'java'
```

### Arithmetic

Standard operators with correct precedence (`*/%` before `+-`, unary minus highest):

```
(score * 1.1) + bonus >= 90
abs(balance - target) < 10
```

### Built-in Functions

| Function | Description |
|---|---|
| `abs(x)` | Absolute value |
| `ceil(x)` | Ceiling |
| `floor(x)` | Floor |
| `round(x)` | Round to nearest integer |
| `length(s)` | String length or collection size |
| `upper(s)` | Uppercase |
| `lower(s)` | Lowercase |
| `trim(s)` | Strip whitespace |

```
abs(balance) > 100
length(name) >= 3
upper(status) = 'ACTIVE'
round(score) >= 90
```

### Field References

Field names are flat keys resolved from the evaluation context:

```
age > 18
status = 'active'
score >= 85.5
```

### Keywords are Case-Insensitive

```
age > 18 AND active = true
age > 18 and active = true   -- same result
```

### String Literals

Single or double quoted. Escape a single quote by doubling it:

```
name = 'O''Brien'
name = "O'Brien"
```

---

## REST API

### Evaluate a rule

```
POST /api/v1/rules/evaluate
```

```json
{
  "rule": "age > 18 AND active = true",
  "context": { "age": 25, "active": true }
}
```

Response:

```json
{ "result": true, "rule": "age > 18 AND active = true", "requestId": "abc-123" }
```

### Evaluate with execution trace

Add `?explain=true` to see exactly how the rule was evaluated:

```
POST /api/v1/rules/evaluate?explain=true
```

```json
{
  "result": true,
  "rule": "age > 18 AND active = true",
  "requestId": "abc-123",
  "trace": {
    "expression": "age > 18 AND active = true",
    "type": "AND",
    "result": true,
    "children": [
      { "expression": "age > 18",     "type": "COMPARISON", "result": true,  "evaluated": "25.0 > 18.0" },
      { "expression": "active = true","type": "COMPARISON", "result": true,  "evaluated": "true = true" }
    ]
  }
}
```

### Validate syntax (no context required)

```
POST /api/v1/rules/validate
```

```json
{ "rule": "age >> 18" }
```

Response:

```json
{ "valid": false, "error": "Parse error in expression [age >> 18] at line 1:5 — extraneous input '>'" }
```

### Batch evaluate

Evaluate multiple rules in a single call. Per-rule failures do not fail the batch.

```
POST /api/v1/rules/batch-evaluate
```

```json
{
  "rules": [
    { "rule": "age > 18",      "context": { "age": 25 } },
    { "rule": "score >= 90",   "context": { "score": 85 } }
  ]
}
```

Response:

```json
{
  "results": [
    { "index": 0, "rule": "age > 18",    "result": true,  "success": true },
    { "index": 1, "rule": "score >= 90", "result": false, "success": true }
  ],
  "requestId": "abc-123"
}
```

### List available functions

```
GET /api/v1/rules/functions
```

---

## Named Rules

Store, manage, and evaluate rules by name. Updates automatically evict the old compiled form from cache.

### Create or update

```
PUT /api/v1/rules/named/{name}
```

```json
{ "expression": "age > 60 AND tier = 'gold'" }
```

Returns `201 Created` with a `Location` header on first save; `200 OK` on update.

### Get

```
GET /api/v1/rules/named/{name}
```

```json
{ "name": "senior-gold", "expression": "age > 60 AND tier = 'gold'", "createdAt": "...", "updatedAt": "..." }
```

### List all

```
GET /api/v1/rules/named
```

### Delete

```
DELETE /api/v1/rules/named/{name}
```

Returns `204 No Content`.

### Evaluate a named rule

```
POST /api/v1/rules/named/{name}/evaluate
```

```json
{ "age": 65, "tier": "gold" }
```

Supports `?explain=true` the same as the anonymous evaluate endpoint.

---

## Error Responses

All errors return a consistent JSON envelope:

```json
{
  "error": "PARSE_ERROR",
  "message": "Parse error in expression [age >> 18] at line 1:5 — extraneous input '>'",
  "timestamp": "2026-01-01T00:00:00Z",
  "requestId": "abc-123"
}
```

| HTTP | Error code | When |
|---|---|---|
| 400 | `PARSE_ERROR` | Invalid expression syntax |
| 400 | `VALIDATION_ERROR` | Missing/invalid request fields or batch too large |
| 404 | `NOT_FOUND` | Named rule does not exist |
| 422 | `EVALUATION_ERROR` | Runtime type mismatch or evaluation error |
| 500 | `INTERNAL_ERROR` | Unexpected server error |

Every response includes a `requestId` (echoed from the `X-Request-ID` request header, or server-generated) for log correlation.

---

## Configuration

```yaml
rulex:
  cache-size: 1000           # max compiled rules cached (Caffeine)
  cache-ttl-seconds: 3600    # evict after 1 hour
  max-expression-length: 4096
  max-evaluation-steps: 10000  # guards against pathologically complex rules
  max-batch-size: 50
```

Compiled rules are cached in Caffeine — repeated evaluations of the same expression string skip re-parsing entirely.

---

## Adding Custom Functions

Implement `RuleFunction` and annotate with `@Component` — Spring auto-discovers and registers it at startup:

```java
@Component
public class StartsWithFunction implements RuleFunction {

    @Override
    public String getName() { return "startsWith"; }

    @Override
    public RuleValue execute(List<RuleValue> args, EvaluationContext ctx) {
        if (args.size() != 2)
            throw new RuleEvaluationException("startsWith requires 2 arguments");
        return RuleValue.of(args.get(0).asString().startsWith(args.get(1).asString()));
    }
}
```

Use immediately in any rule:

```
startsWith(name, 'Jo') AND age > 18
```

---

## Programmatic Usage

```java
@Autowired RuleEngine ruleEngine;

Map<String, Object> context = Map.of(
    "age",    25,
    "status", "active",
    "tags",   List.of("java", "spring")
);

// Simple evaluation
boolean result = ruleEngine.evaluate(
    "age > 18 AND status = 'active' AND tags CONTAINS 'java'",
    context
);

// With trace
RuleEngine.TraceResult traced = ruleEngine.evaluateWithTrace(
    "age > 18 AND status = 'active'", context);
System.out.println(traced.result());  // true
System.out.println(traced.trace());   // TraceNode tree
```

---

## Project Structure

```
src/
├── main/
│   ├── antlr4/com/rulex/grammar/
│   │   └── Rule.g4                     ANTLR4 grammar (case-insensitive)
│   └── java/com/rulex/
│       ├── engine/
│       │   ├── RuleEngine.java          Evaluate / validate / trace
│       │   ├── RuleCompiler.java        Parse + Caffeine cache
│       │   ├── RuleEvaluator.java       ANTLR visitor — produces RuleValue
│       │   ├── ExplainingEvaluator.java ANTLR visitor — produces TraceNode
│       │   ├── Predicates.java          Shared comparison / contains / in logic
│       │   ├── EvaluationContext.java   Field resolution (single identifier)
│       │   ├── RuleValue.java           Immutable value wrapper
│       │   ├── TraceNode.java           Execution trace node (JSON serializable)
│       │   └── CompiledRule.java        Parse tree + source (record)
│       ├── function/
│       │   ├── RuleFunction.java        Extension SPI
│       │   ├── FunctionRegistry.java    Thread-safe, sorted name cache
│       │   └── builtin/                 abs, ceil, floor, round, length, upper, lower, trim
│       ├── store/
│       │   ├── NamedRuleStore.java      JPA-backed named rule CRUD + cache invalidation
│       │   ├── NamedRuleEntity.java     H2 entity (package-private)
│       │   ├── NamedRuleRepository.java Spring Data JPA
│       │   └── NamedRule.java           Domain record
│       ├── web/
│       │   ├── RuleController.java      /evaluate, /validate, /batch-evaluate, /functions
│       │   ├── NamedRuleController.java /named CRUD + evaluate
│       │   ├── GlobalExceptionHandler.java Structured error responses
│       │   ├── RequestIdFilter.java     MDC request ID propagation
│       │   └── SecurityHeadersFilter.java Security response headers
│       ├── config/
│       │   ├── RuleEngineConfig.java    Caffeine cache bean + metrics
│       │   └── RuleEngineProperties.java @ConfigurationProperties record
│       ├── exception/
│       │   ├── RuleParseException.java
│       │   └── RuleEvaluationException.java
│       └── health/
│           └── RuleEngineHealthIndicator.java
└── test/
    └── java/com/rulex/
        ├── engine/RuleEngineTest.java      Integration tests — 100+ scenarios
        ├── web/RuleControllerTest.java     Web layer (MockMvc)
        └── web/NamedRuleControllerTest.java
```

## Build and Test

```bash
mvn test              # run all 117 tests
mvn package           # build fat jar → target/rulex-1.0.0.jar
java -jar target/rulex-1.0.0.jar
```
