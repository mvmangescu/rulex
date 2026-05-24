# Rulex — Rule Engine

A production-ready rule engine built with Spring Boot 3 and ANTLR4. Write human-readable boolean expressions, evaluate
them at runtime against any data context.

## Stack

| Component   | Version                        |
|-------------|--------------------------------|
| Java        | 21                             |
| Spring Boot | 3.2.5                          |
| ANTLR       | 4.13.1                         |
| Cache       | Caffeine                       |
| Storage     | H2 (in-memory JPA + Liquibase) |
| Metrics     | Micrometer / Prometheus        |
| API Docs    | SpringDoc / Swagger UI         |

## Quick Start

```bash
mvn spring-boot:run
```

- Swagger UI: http://localhost:8484/swagger-ui.html
- API Docs: http://localhost:8484/api-docs
- Health: http://localhost:8484/actuator/health
- Metrics: http://localhost:8484/actuator/prometheus

---

## Expression Syntax

### Comparisons

| Operator | Example              |
|----------|----------------------|
| `=`      | `status = 'active'`  |
| `!=`     | `status != 'banned'` |
| `>`      | `age > 18`           |
| `<`      | `score < 50`         |
| `>=`     | `score >= 85.5`      |
| `<=`     | `price <= 99.99`     |

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
name NOT CONTAINS 'test'
tags NOT CONTAINS 'legacy'
```

### Arithmetic

Standard operators with correct precedence (`*/%` before `+-`, unary minus highest):

```
(score * 1.1) + bonus >= 90
abs(balance - target) < 10
```

### Built-in Functions

| Function    | Description                      | Example                  |
|-------------|----------------------------------|--------------------------|
| `abs(x)`    | Absolute value                   | `abs(-10)` → 10          |
| `ceil(x)`   | Ceiling                          | `ceil(3.2)` → 4          |
| `floor(x)`  | Floor                            | `floor(3.8)` → 3         |
| `round(x)`  | Round to nearest integer         | `round(3.5)` → 4         |
| `length(s)` | String length or collection size | `length('hello')` → 5    |
| `upper(s)`  | Uppercase                        | `upper('hello')` → HELLO |
| `lower(s)`  | Lowercase                        | `lower('HELLO')` → hello |
| `trim(s)`   | Strip whitespace                 | `trim(' hi ')` → hi      |

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

Base path: `/api/v1/rules`

### Stateless Evaluation

#### Evaluate a rule

```
POST /api/v1/rules/evaluate
```

```json
{
  "rule": "age > 18 AND active = true",
  "context": {
    "age": 25,
    "active": true
  }
}
```

Response:

```json
{
  "result": true,
  "rule": "age > 18 AND active = true"
}
```

#### Evaluate with execution trace

Add `?explain=true` to see exactly how the rule was evaluated:

```
POST /api/v1/rules/evaluate?explain=true
```

```json
{
  "result": true,
  "rule": "age > 18 AND active = true",
  "trace": {
    "expression": "age > 18 AND active = true",
    "type": "AND",
    "result": true,
    "children": [
      {
        "expression": "age > 18",
        "type": "COMPARISON",
        "result": true,
        "evaluated": "25.0 > 18.0"
      },
      {
        "expression": "active = true",
        "type": "COMPARISON",
        "result": true,
        "evaluated": "true = true"
      }
    ]
  }
}
```

#### Validate syntax

Checks syntax without evaluating. No context required.

```
POST /api/v1/rules/validate
```

```json
{
  "rule": "age >> 18"
}
```

Response:

```json
{
  "valid": false,
  "error": "Parse error in expression [age >> 18] at line 1:5 — extraneous input '>'"
}
```

#### Dry-run (parse tree)

Parses the expression and returns its parse tree without evaluating. Useful for tooling and syntax highlighting.

```
POST /api/v1/rules/dry-run
```

```json
{
  "rule": "age > 18 AND active = true"
}
```

Response:

```json
{
  "rule": "age > 18 AND active = true",
  "parseTree": {
    "type": "Program",
    "text": "age > 18 AND active = true",
    "children": [
      {
        "type": "AndExpr",
        "text": "age > 18 AND active = true",
        "children": [
          {
            "type": "ComparisonPred",
            "text": "age > 18"
          },
          {
            "type": "ComparisonPred",
            "text": "active = true"
          }
        ]
      }
    ]
  }
}
```

---

### Rule CRUD

Store, manage, and evaluate rules by id. Updates automatically evict the old compiled form from cache.

#### Create

```
POST /api/v1/rules
```

```json
{
  "name": "senior-gold",
  "expression": "age > 60 AND tier = 'gold'",
  "description": "Senior gold tier check"
}
```

Returns `201 Created` with a `Location: /api/v1/rules/{id}` header.

#### Update (partial)

```
PUT /api/v1/rules/{id}
```

```json
{
  "expression": "age > 65 AND tier = 'gold'"
}
```

Omitted fields are left unchanged. Returns `200 OK` with the updated rule.

#### Get by id

```
GET /api/v1/rules/{id}
```

```json
{
  "id": 1,
  "name": "senior-gold",
  "expression": "age > 60 AND tier = 'gold'",
  "description": "Senior gold tier check",
  "createdAt": "2026-01-01T10:00:00Z",
  "updatedAt": "2026-01-02T08:30:00Z"
}
```

#### List all

```
GET /api/v1/rules
```

Returns an array of rule objects (empty array if none).

#### Delete

```
DELETE /api/v1/rules/{id}
```

Returns `204 No Content`. Evicts the expression from the compile cache.

#### Evaluate a stored rule

```
POST /api/v1/rules/{id}/evaluate
```

Request body is the context (optional — defaults to empty):

```json
{
  "age": 65,
  "tier": "gold"
}
```

Supports `?explain=true` the same as the anonymous evaluate endpoint.

---

## Error Responses

All errors return a consistent JSON envelope:

```json
{
  "error": "PARSE_ERROR",
  "message": "Parse error in expression [age >> 18] at line 1:5 — extraneous input '>'",
  "timestamp": "2026-01-01T00:00:00Z"
}
```

| HTTP | Error code         | When                                      |
|------|--------------------|-------------------------------------------|
| 400  | `PARSE_ERROR`      | Invalid expression syntax                 |
| 400  | `VALIDATION_ERROR` | Missing/invalid request fields            |
| 404  | `NOT_FOUND`        | Rule does not exist                       |
| 422  | `EVALUATION_ERROR` | Runtime type mismatch or evaluation error |
| 500  | `INTERNAL_ERROR`   | Unexpected server error                   |

---

## Configuration

```yaml
rulex:
  cache-size: 1000           # max compiled rules cached (Caffeine)
  cache-ttl-seconds: 3600    # evict after 1 hour
  max-expression-length: 4096
  max-evaluation-steps: 10000  # guards against pathologically complex rules
```

Compiled rules are cached in Caffeine — repeated evaluations of the same expression string skip re-parsing entirely.

---

## Adding Custom Functions

Implement `RuleFunction` and annotate with `@Component` — Spring auto-discovers and registers it at startup:

```java

@Component
public class StartsWithFunction implements RuleFunction {

    @Override
    public String getName() {
        return "startsWith";
    }

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

@Autowired
RuleEngine ruleEngine;

Map<String, Object> context = Map.of(
        "age", 25,
        "status", "active",
        "tags", List.of("java", "spring")
);

// Simple evaluation
boolean result = ruleEngine.evaluate(
        "age > 18 AND status = 'active' AND tags CONTAINS 'java'",
        context
);

// With trace
RuleEngine.TraceResult traced = ruleEngine.evaluateWithTrace(
        "age > 18 AND status = 'active'", context);
System.out.

println(traced.result());  // true
        System.out.

println(traced.trace());   // TraceNode tree
```

---

## Project Structure

```
src/
├── main/
│   ├── antlr4/com/rulex/grammar/
│   │   └── Rule.g4                      ANTLR4 grammar (case-insensitive)
│   ├── resources/
│   │   ├── application.yml
│   │   └── db/changelog/
│   │       ├── db.changelog-master.yaml
│   │       └── 1/
│   │           ├── db.changelog.yaml
│   │           └── 001-initial-schema.sql
│   └── java/com/rulex/
│       ├── engine/
│       │   ├── RuleEngine.java           Evaluate / validate / trace / dry-run
│       │   ├── RuleCompiler.java         Parse + Caffeine cache
│       │   ├── RuleEvaluator.java        ANTLR visitor — produces RuleValue
│       │   ├── ExplainingEvaluator.java  ANTLR visitor — produces TraceNode
│       │   ├── ParseTreeNode.java        Parse tree structure (dry-run)
│       │   ├── Predicates.java           Comparison / contains / in logic
│       │   ├── EvaluationContext.java    Field resolution
│       │   ├── RuleValue.java            Immutable value wrapper
│       │   ├── TraceNode.java            Execution trace node (JSON)
│       │   ├── CompiledRule.java         Parse tree + source (record)
│       │   └── function/
│       │       ├── RuleFunction.java     Extension SPI
│       │       ├── FunctionRegistry.java Thread-safe registry
│       │       └── builtin/             abs, ceil, floor, round, length, upper, lower, trim
│       ├── controller/
│       │   ├── RuleEngineController.java /evaluate, /validate, /dry-run
│       │   └── RuleController.java       CRUD + /{id}/evaluate
│       ├── service/
│       │   └── RuleService.java          CRUD + cache invalidation
│       ├── entity/
│       │   └── RuleEntity.java           JPA entity
│       ├── dto/                          Request/response records
│       ├── config/
│       │   ├── RuleEngineConfig.java     Caffeine cache bean + metrics
│       │   ├── RuleEngineProperties.java @ConfigurationProperties record
│       │   └── OpenApiConfig.java        Swagger configuration
│       ├── exception/
│       │   ├── GlobalExceptionHandler.java
│       │   ├── RuleParseException.java
│       │   ├── RuleEvaluationException.java
│       │   └── RuleNotFoundException.java
│       ├── web/
│       │   ├── RequestIdFilter.java      MDC request ID propagation
│       │   └── SecurityHeadersFilter.java
│       └── health/
│           └── RuleEngineHealthIndicator.java
└── test/
    └── java/com/rulex/
        ├── engine/RuleEngineTest.java        Integration tests
        ├── web/RuleControllerTest.java        Web layer (MockMvc)
        └── web/RuleEngineControllerTest.java
```

## Build and Test

```bash
mvn test              # run all tests
mvn package           # build fat jar → target/rulex-1.0.0.jar
java -jar target/rulex-1.0.0.jar
```
