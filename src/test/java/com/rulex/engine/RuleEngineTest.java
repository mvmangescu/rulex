package com.rulex.engine;

import com.rulex.engine.RuleEngine.TraceResult;
import com.rulex.engine.RuleEngine.ValidationResult;
import com.rulex.exception.RuleEvaluationException;
import com.rulex.exception.RuleParseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DisplayName("RuleEngine integration tests")
class RuleEngineTest {

    @Autowired
    private RuleEngine ruleEngine;

    @Nested
    @DisplayName("Simple comparisons")
    class SimpleComparisons {

        @Test
        @DisplayName("age > 18 returns true when age is 25")
        void numericGreaterThan_true() {
            assertThat(ruleEngine.evaluate("age > 18", Map.of("age", 25))).isTrue();
        }

        @Test
        @DisplayName("age > 18 returns false when age is 10")
        void numericGreaterThan_false() {
            assertThat(ruleEngine.evaluate("age > 18", Map.of("age", 10))).isFalse();
        }

        @Test
        @DisplayName("score <= 100 returns true when score is 100")
        void numericLessThanOrEqual_boundary() {
            assertThat(ruleEngine.evaluate("score <= 100", Map.of("score", 100))).isTrue();
        }

        @Test
        @DisplayName("score < 100 returns false when score is 100")
        void numericLessThan_boundary() {
            assertThat(ruleEngine.evaluate("score < 100", Map.of("score", 100))).isFalse();
        }

        @Test
        @DisplayName("score >= 50 returns true when score is 75")
        void numericGreaterThanOrEqual_true() {
            assertThat(ruleEngine.evaluate("score >= 50", Map.of("score", 75))).isTrue();
        }

        @Test
        @DisplayName("name = 'John' returns true")
        void stringEquality_true() {
            assertThat(ruleEngine.evaluate("name = 'John'", Map.of("name", "John"))).isTrue();
        }

        @Test
        @DisplayName("name = 'John' returns false when name is 'Jane'")
        void stringEquality_false() {
            assertThat(ruleEngine.evaluate("name = 'John'", Map.of("name", "Jane"))).isFalse();
        }

        @Test
        @DisplayName("status != 'active' returns true when status is 'inactive'")
        void notEqual_true() {
            assertThat(ruleEngine.evaluate("status != 'active'", Map.of("status", "inactive"))).isTrue();
        }

        @Test
        @DisplayName("Decimal number comparison: price > 9.99")
        void decimalComparison() {
            assertThat(ruleEngine.evaluate("price > 9.99", Map.of("price", 10.50))).isTrue();
        }

        @Test
        @DisplayName("String lexicographic: 'B' > 'A' returns true")
        void stringGreaterThan_true() {
            assertThat(ruleEngine.evaluate("code > 'A'", Map.of("code", "B"))).isTrue();
        }

        @Test
        @DisplayName("String lexicographic: 'A' < 'B' returns true")
        void stringLessThan_true() {
            assertThat(ruleEngine.evaluate("code < 'B'", Map.of("code", "A"))).isTrue();
        }

        @Test
        @DisplayName("Numeric equality: 1.0 = 1 returns true")
        void numericEqualityAcrossTypes() {
            assertThat(ruleEngine.evaluate("score = 1", Map.of("score", 1.0))).isTrue();
        }
    }

    @Nested
    @DisplayName("Boolean literals")
    class BooleanLiterals {

        @Test
        @DisplayName("Literal 'true' evaluates to true")
        void trueLiteral() {
            assertThat(ruleEngine.evaluate("true", Map.of())).isTrue();
        }

        @Test
        @DisplayName("Literal 'false' evaluates to false")
        void falseLiteral() {
            assertThat(ruleEngine.evaluate("false", Map.of())).isFalse();
        }
    }

    @Nested
    @DisplayName("NOT operator")
    class NotOperator {

        @Test
        @DisplayName("NOT active = true returns false when active is true")
        void notTrue_returnsFalse() {
            assertThat(ruleEngine.evaluate("NOT active = true", Map.of("active", true))).isFalse();
        }

        @Test
        @DisplayName("NOT active = true returns true when active is false")
        void notFalse_returnsTrue() {
            assertThat(ruleEngine.evaluate("NOT active = true", Map.of("active", false))).isTrue();
        }

        @Test
        @DisplayName("Double NOT: NOT NOT true returns true")
        void doubleNot() {
            assertThat(ruleEngine.evaluate("NOT NOT true", Map.of())).isTrue();
        }
    }

    @Nested
    @DisplayName("AND operator")
    class AndOperator {

        @Test
        @DisplayName("Returns true when both conditions hold")
        void andBothTrue() {
            assertThat(ruleEngine.evaluate("age > 18 AND active = true",
                    Map.of("age", 25, "active", true))).isTrue();
        }

        @Test
        @DisplayName("Returns false when first condition fails")
        void andFirstFails() {
            assertThat(ruleEngine.evaluate("age > 18 AND active = true",
                    Map.of("age", 10, "active", true))).isFalse();
        }

        @Test
        @DisplayName("Returns false when second condition fails")
        void andSecondFails() {
            assertThat(ruleEngine.evaluate("age > 18 AND active = true",
                    Map.of("age", 25, "active", false))).isFalse();
        }

        @Test
        @DisplayName("Returns false when both conditions fail")
        void andBothFail() {
            assertThat(ruleEngine.evaluate("age > 18 AND active = true",
                    Map.of("age", 10, "active", false))).isFalse();
        }
    }

    @Nested
    @DisplayName("OR operator")
    class OrOperator {

        @Test
        @DisplayName("Returns true when first condition holds")
        void orFirstTrue() {
            assertThat(ruleEngine.evaluate("status = 'A' OR status = 'B'",
                    Map.of("status", "A"))).isTrue();
        }

        @Test
        @DisplayName("Returns true when second condition holds")
        void orSecondTrue() {
            assertThat(ruleEngine.evaluate("status = 'A' OR status = 'B'",
                    Map.of("status", "B"))).isTrue();
        }

        @Test
        @DisplayName("Returns false when both conditions fail")
        void orBothFalse() {
            assertThat(ruleEngine.evaluate("status = 'A' OR status = 'B'",
                    Map.of("status", "C"))).isFalse();
        }
    }

    @Nested
    @DisplayName("Complex nesting")
    class ComplexNesting {

        @Test
        @DisplayName("(age > 18 AND score > 50) OR vip = true: vip overrides")
        void vipOverride() {
            assertThat(ruleEngine.evaluate("(age > 18 AND score > 50) OR vip = true",
                    Map.of("age", 10, "score", 10, "vip", true))).isTrue();
        }

        @Test
        @DisplayName("(age > 18 AND score > 50) OR vip = true: all conditions fail")
        void allFail() {
            assertThat(ruleEngine.evaluate("(age > 18 AND score > 50) OR vip = true",
                    Map.of("age", 10, "score", 10, "vip", false))).isFalse();
        }

        @Test
        @DisplayName("Deeply nested: (a > 1 AND b > 1) AND (c > 1 OR d > 1)")
        void deeplyNested() {
            assertThat(ruleEngine.evaluate("(a > 1 AND b > 1) AND (c > 1 OR d > 1)",
                    Map.of("a", 2, "b", 2, "c", 0, "d", 2))).isTrue();
        }

        @Test
        @DisplayName("Real-world: age > 60 AND (Col_1 = 'A' AND Col_2 IN ('A1','A2')) OR name CONTAINS 'ABC'")
        void realWorldExpression() {
            assertThat(ruleEngine.evaluate(
                    "age > 60 AND (Col_1 = 'A' AND Col_2 IN ('A1', 'A2')) OR name CONTAINS 'ABC'",
                    Map.of("age", 65, "Col_1", "A", "Col_2", "A1", "name", "XYZ"))).isTrue();
        }
    }

    @Nested
    @DisplayName("Null checks")
    class NullChecks {

        @Test
        @DisplayName("email IS NULL returns true when email is absent")
        void isNull_missingVar() {
            assertThat(ruleEngine.evaluate("email IS NULL", Map.of())).isTrue();
        }

        @Test
        @DisplayName("email IS NULL returns false when email is present")
        void isNull_presentVar() {
            assertThat(ruleEngine.evaluate("email IS NULL", Map.of("email", "test@example.com"))).isFalse();
        }

        @Test
        @DisplayName("email IS NOT NULL returns true when email is present")
        void isNotNull_presentVar() {
            assertThat(ruleEngine.evaluate("email IS NOT NULL", Map.of("email", "test@example.com"))).isTrue();
        }

        @Test
        @DisplayName("email IS NOT NULL returns false when email is absent")
        void isNotNull_missingVar() {
            assertThat(ruleEngine.evaluate("email IS NOT NULL", Map.of())).isFalse();
        }

        @Test
        @DisplayName("null variable compared with = returns false")
        void nullEquality_returnsFalse() {
            assertThat(ruleEngine.evaluate("email = 'x'", Map.of())).isFalse();
        }

        @Test
        @DisplayName("null variable with != returns true")
        void nullNotEqual_returnsTrue() {
            assertThat(ruleEngine.evaluate("email != 'x'", Map.of())).isTrue();
        }

        @Test
        @DisplayName("CONTAINS with null left returns false")
        void containsNullLeft_returnsFalse() {
            assertThat(ruleEngine.evaluate("email CONTAINS 'x'", Map.of())).isFalse();
        }

        @Test
        @DisplayName("value IS NUMERIC returns false for null/absent variable")
        void isNumeric_nullVar_returnsFalse() {
            assertThat(ruleEngine.evaluate("value IS NUMERIC", Map.of())).isFalse();
        }
    }

    @Nested
    @DisplayName("CONTAINS operator")
    class ContainsOperator {

        @Test
        @DisplayName("String CONTAINS substring — true")
        void stringContains_true() {
            assertThat(ruleEngine.evaluate("name CONTAINS 'oh'", Map.of("name", "John"))).isTrue();
        }

        @Test
        @DisplayName("String CONTAINS substring — false")
        void stringContains_false() {
            assertThat(ruleEngine.evaluate("name CONTAINS 'xyz'", Map.of("name", "John"))).isFalse();
        }

        @Test
        @DisplayName("Collection CONTAINS element — true")
        void listContains_true() {
            assertThat(ruleEngine.evaluate("tags CONTAINS 'java'",
                    Map.of("tags", List.of("java", "spring")))).isTrue();
        }

        @Test
        @DisplayName("Collection CONTAINS element — false")
        void listContains_false() {
            assertThat(ruleEngine.evaluate("tags CONTAINS 'python'",
                    Map.of("tags", List.of("java", "spring")))).isFalse();
        }
    }

    @Nested
    @DisplayName("IS NUMERIC check")
    class IsNumericCheck {

        @Test
        @DisplayName("Returns true for integer")
        void integer_true() {
            assertThat(ruleEngine.evaluate("value IS NUMERIC", Map.of("value", 42))).isTrue();
        }

        @Test
        @DisplayName("Returns true for double")
        void double_true() {
            assertThat(ruleEngine.evaluate("value IS NUMERIC", Map.of("value", 3.14))).isTrue();
        }

        @Test
        @DisplayName("Returns true for numeric string")
        void numericString_true() {
            assertThat(ruleEngine.evaluate("value IS NUMERIC", Map.of("value", "123.45"))).isTrue();
        }

        @Test
        @DisplayName("Returns false for non-numeric string")
        void nonNumericString_false() {
            assertThat(ruleEngine.evaluate("value IS NUMERIC", Map.of("value", "hello"))).isFalse();
        }
    }

    @Nested
    @DisplayName("IN and NOT IN operators")
    class InOperator {

        @Test
        @DisplayName("String IN list — match")
        void in_string_match() {
            assertThat(ruleEngine.evaluate("status IN ('A', 'B', 'C')", Map.of("status", "A"))).isTrue();
        }

        @Test
        @DisplayName("String IN list — no match")
        void in_string_noMatch() {
            assertThat(ruleEngine.evaluate("status IN ('A', 'B', 'C')", Map.of("status", "D"))).isFalse();
        }

        @Test
        @DisplayName("Numeric IN list — match")
        void in_numeric_match() {
            assertThat(ruleEngine.evaluate("age IN (18, 21, 25)", Map.of("age", 21))).isTrue();
        }

        @Test
        @DisplayName("Numeric IN list — no match")
        void in_numeric_noMatch() {
            assertThat(ruleEngine.evaluate("age IN (18, 21, 25)", Map.of("age", 30))).isFalse();
        }

        @Test
        @DisplayName("NOT IN — value absent from list")
        void notIn_noMatch() {
            assertThat(ruleEngine.evaluate("status NOT IN ('A', 'B')", Map.of("status", "C"))).isTrue();
        }

        @Test
        @DisplayName("NOT IN — value present in list")
        void notIn_match() {
            assertThat(ruleEngine.evaluate("status NOT IN ('A', 'B')", Map.of("status", "A"))).isFalse();
        }

        @Test
        @DisplayName("IN with single-element list")
        void in_singleValue() {
            assertThat(ruleEngine.evaluate("role IN ('admin')", Map.of("role", "admin"))).isTrue();
        }

        @Test
        @DisplayName("Case-insensitive 'in' keyword")
        void in_caseInsensitive() {
            assertThat(ruleEngine.evaluate("status in ('A', 'B')", Map.of("status", "A"))).isTrue();
        }

        @Test
        @DisplayName("Case-insensitive 'not in' keyword")
        void notIn_caseInsensitive() {
            assertThat(ruleEngine.evaluate("status not in ('A', 'B')", Map.of("status", "C"))).isTrue();
        }
    }

    @Nested
    @DisplayName("Arithmetic expressions")
    class ArithmeticExpressions {

        @Test
        @DisplayName("Addition: a + b > 10")
        void addition() {
            assertThat(ruleEngine.evaluate("a + b > 10", Map.of("a", 6, "b", 7))).isTrue();
        }

        @Test
        @DisplayName("Subtraction: a - b > 3")
        void subtraction() {
            assertThat(ruleEngine.evaluate("a - b > 3", Map.of("a", 10, "b", 5))).isTrue();
        }

        @Test
        @DisplayName("Multiplication: a * b <= 100")
        void multiplication() {
            assertThat(ruleEngine.evaluate("a * b <= 100", Map.of("a", 5, "b", 20))).isTrue();
        }

        @Test
        @DisplayName("Division: a / b > 2.0")
        void division() {
            assertThat(ruleEngine.evaluate("a / b > 2.0", Map.of("a", 10.0, "b", 4.0))).isTrue();
        }

        @Test
        @DisplayName("Modulo: a % 2 = 0 (even number check)")
        void modulo() {
            assertThat(ruleEngine.evaluate("a % 2 = 0", Map.of("a", 8))).isTrue();
        }

        @Test
        @DisplayName("Operator precedence: a + b * c = 14 (mul before add)")
        void precedence_mulBeforeAdd() {
            assertThat(ruleEngine.evaluate("a + b * c = 14", Map.of("a", 2, "b", 3, "c", 4))).isTrue();
        }

        @Test
        @DisplayName("Grouping overrides precedence: (a + b) * c = 20")
        void arithmeticGrouping() {
            assertThat(ruleEngine.evaluate("(a + b) * c = 20", Map.of("a", 2, "b", 3, "c", 4))).isTrue();
        }

        @Test
        @DisplayName("Unary minus: -a > -10 when a = 5")
        void unaryMinus() {
            assertThat(ruleEngine.evaluate("-a > -10", Map.of("a", 5))).isTrue();
        }

        @Test
        @DisplayName("Arithmetic with literals only: 10 + 5 > 12")
        void arithmeticLiterals() {
            assertThat(ruleEngine.evaluate("10 + 5 > 12", Map.of())).isTrue();
        }

        @Test
        @DisplayName("abs(Col_1 - Col_2) > 0.2 — field subtraction in function")
        void absOfDifference_true() {
            assertThat(ruleEngine.evaluate("abs(Col_1 - Col_2) > 0.2",
                    Map.of("Col_1", 1.5, "Col_2", 1.0))).isTrue();
        }

        @Test
        @DisplayName("abs(Col_1 - Col_2) > 0.2 — false when difference is small")
        void absOfDifference_false() {
            assertThat(ruleEngine.evaluate("abs(Col_1 - Col_2) > 0.2",
                    Map.of("Col_1", 1.1, "Col_2", 1.0))).isFalse();
        }
    }

    @Nested
    @DisplayName("Built-in functions")
    class BuiltInFunctions {

        @Test
        @DisplayName("abs(-150) > 100 — true")
        void abs_true() {
            assertThat(ruleEngine.evaluate("abs(balance) > 100", Map.of("balance", -150.0))).isTrue();
        }

        @Test
        @DisplayName("abs(-50) > 100 — false")
        void abs_false() {
            assertThat(ruleEngine.evaluate("abs(balance) > 100", Map.of("balance", -50.0))).isFalse();
        }

        @Test
        @DisplayName("ceil(4.1) = 5")
        void ceil() {
            assertThat(ruleEngine.evaluate("ceil(value) = 5", Map.of("value", 4.1))).isTrue();
        }

        @Test
        @DisplayName("floor(4.9) = 4")
        void floor() {
            assertThat(ruleEngine.evaluate("floor(value) = 4", Map.of("value", 4.9))).isTrue();
        }

        @Test
        @DisplayName("round(4.6) = 5")
        void round() {
            assertThat(ruleEngine.evaluate("round(value) = 5", Map.of("value", 4.6))).isTrue();
        }

        @Test
        @DisplayName("length('John') > 3")
        void length_string() {
            assertThat(ruleEngine.evaluate("length(name) > 3", Map.of("name", "John"))).isTrue();
        }

        @Test
        @DisplayName("length([a,b,c]) = 3")
        void length_list() {
            assertThat(ruleEngine.evaluate("length(tags) = 3",
                    Map.of("tags", List.of("a", "b", "c")))).isTrue();
        }

        @Test
        @DisplayName("upper('active') = 'ACTIVE'")
        void upper() {
            assertThat(ruleEngine.evaluate("upper(status) = 'ACTIVE'", Map.of("status", "active"))).isTrue();
        }

        @Test
        @DisplayName("lower('INACTIVE') = 'inactive'")
        void lower() {
            assertThat(ruleEngine.evaluate("lower(status) = 'inactive'", Map.of("status", "INACTIVE"))).isTrue();
        }

        @Test
        @DisplayName("trim('  John  ') = 'John'")
        void trim() {
            assertThat(ruleEngine.evaluate("trim(name) = 'John'", Map.of("name", "  John  "))).isTrue();
        }
    }

    @Nested
    @DisplayName("Case-insensitive keywords")
    class CaseInsensitiveKeywords {

        @Test
        @DisplayName("Lowercase 'and'")
        void lowercaseAnd() {
            assertThat(ruleEngine.evaluate("age > 18 and active = true",
                    Map.of("age", 25, "active", true))).isTrue();
        }

        @Test
        @DisplayName("Lowercase 'or'")
        void lowercaseOr() {
            assertThat(ruleEngine.evaluate("status = 'A' or status = 'B'", Map.of("status", "A"))).isTrue();
        }

        @Test
        @DisplayName("Lowercase 'not'")
        void lowercaseNot() {
            assertThat(ruleEngine.evaluate("not active = true", Map.of("active", false))).isTrue();
        }

        @Test
        @DisplayName("Mixed-case 'And'")
        void mixedCaseAnd() {
            assertThat(ruleEngine.evaluate("age > 18 And active = true",
                    Map.of("age", 25, "active", true))).isTrue();
        }

        @Test
        @DisplayName("Lowercase 'is null'")
        void lowercaseIsNull() {
            assertThat(ruleEngine.evaluate("email is null", Map.of())).isTrue();
        }

        @Test
        @DisplayName("Lowercase 'contains'")
        void lowercaseContains() {
            assertThat(ruleEngine.evaluate("name contains 'oh'", Map.of("name", "John"))).isTrue();
        }
    }

    @Nested
    @DisplayName("String literal edge cases")
    class StringLiteralEdgeCases {

        @Test
        @DisplayName("Double-quoted string literal")
        void doubleQuotedString() {
            assertThat(ruleEngine.evaluate("name = \"John\"", Map.of("name", "John"))).isTrue();
        }

        @Test
        @DisplayName("Escaped single quote '' in string literal")
        void escapedSingleQuote() {
            assertThat(ruleEngine.evaluate("note = 'it''s here'", Map.of("note", "it's here"))).isTrue();
        }
    }

    @Nested
    @DisplayName("Error handling")
    class ErrorHandling {

        @Test
        @DisplayName("Invalid operator throws RuleParseException")
        void invalidExpression_throwsParseException() {
            Map<String, Object> context = Map.of("age", 25);
            assertThatThrownBy(() -> ruleEngine.evaluate("age >>> 18", context))
                    .isInstanceOf(RuleParseException.class);
        }

        @Test
        @DisplayName("Incomplete expression throws RuleParseException")
        void incompleteExpression_throwsParseException() {
            Map<String, Object> context = Map.of("age", 25);
            assertThatThrownBy(() -> ruleEngine.evaluate("age >", context))
                    .isInstanceOf(RuleParseException.class);
        }

        @Test
        @DisplayName("Blank expression throws RuleParseException")
        void blankExpression_throwsParseException() {
            Map<String, Object> context = Map.of();
            assertThatThrownBy(() -> ruleEngine.evaluate("", context))
                    .isInstanceOf(RuleParseException.class);
        }

        @Test
        @DisplayName("Division by zero throws RuleEvaluationException")
        void divisionByZero_throwsEvaluationException() {
            Map<String, Object> context = Map.of("a", 10, "b", 0);
            assertThatThrownBy(() -> ruleEngine.evaluate("a / b > 0", context))
                    .isInstanceOf(RuleEvaluationException.class)
                    .hasMessageContaining("zero");
        }

        @Test
        @DisplayName("Modulo by zero throws RuleEvaluationException")
        void moduloByZero_throwsEvaluationException() {
            Map<String, Object> context = Map.of("a", 10, "b", 0);
            assertThatThrownBy(() -> ruleEngine.evaluate("a % b = 0", context))
                    .isInstanceOf(RuleEvaluationException.class)
                    .hasMessageContaining("zero");
        }

        @Test
        @DisplayName("Unknown function throws RuleEvaluationException")
        void unknownFunction_throwsEvaluationException() {
            Map<String, Object> context = Map.of("x", 1);
            assertThatThrownBy(() -> ruleEngine.evaluate("unknown(x) = 1", context))
                    .isInstanceOf(RuleEvaluationException.class)
                    .hasMessageContaining("unknown");
        }

        @Test
        @DisplayName("Arithmetic on non-numeric field throws RuleEvaluationException")
        void arithmeticOnNonNumeric_throwsEvaluationException() {
            Map<String, Object> context = Map.of("name", "John");
            assertThatThrownBy(() -> ruleEngine.evaluate("name + 1 > 0", context))
                    .isInstanceOf(RuleEvaluationException.class);
        }

        @Test
        @DisplayName("Arithmetic on absent (null) field throws RuleEvaluationException")
        void arithmeticOnNull_throwsEvaluationException() {
            Map<String, Object> context = Map.of();
            assertThatThrownBy(() -> ruleEngine.evaluate("missing + 1 > 0", context))
                    .isInstanceOf(RuleEvaluationException.class);
        }
    }

    @Nested
    @DisplayName("Rule trace (evaluateWithTrace)")
    class TraceTests {

        @Test
        @DisplayName("COMPARISON produces leaf node with evaluated text")
        void comparison_producesLeafNode() {
            TraceResult result = ruleEngine.evaluateWithTrace("age > 18", Map.of("age", 25));
            assertThat(result.result()).isTrue();
            assertThat(result.trace().type()).isEqualTo("COMPARISON");
            assertThat(result.trace().evaluated()).contains(">");
            assertThat(result.trace().children()).isNull();
        }

        @Test
        @DisplayName("AND produces compound node with two children")
        void and_producesCompoundNode() {
            TraceResult result = ruleEngine.evaluateWithTrace(
                    "age > 18 AND active = true", Map.of("age", 25, "active", true));
            assertThat(result.trace().type()).isEqualTo("AND");
            assertThat(result.trace().children()).hasSize(2)
                    .allMatch(c -> c.type().equals("COMPARISON"));
        }

        @Test
        @DisplayName("OR evaluates both branches in trace regardless of short-circuit")
        void or_traceIncludesBothBranches() {
            TraceResult result = ruleEngine.evaluateWithTrace(
                    "age > 18 OR score > 100", Map.of("age", 25, "score", 50));
            assertThat(result.trace().type()).isEqualTo("OR");
            assertThat(result.trace().children()).hasSize(2);
        }

        @Test
        @DisplayName("NOT produces NOT node wrapping child")
        void not_producesNotNode() {
            TraceResult result = ruleEngine.evaluateWithTrace(
                    "NOT active = true", Map.of("active", false));
            assertThat(result.result()).isTrue();
            assertThat(result.trace().type()).isEqualTo("NOT");
            assertThat(result.trace().children()).hasSize(1);
            assertThat(result.trace().children().getFirst().result()).isFalse();
        }

        @Test
        @DisplayName("IN produces IN leaf node")
        void in_producesInNode() {
            TraceResult result = ruleEngine.evaluateWithTrace(
                    "status IN ('A', 'B')", Map.of("status", "A"));
            assertThat(result.result()).isTrue();
            assertThat(result.trace().type()).isEqualTo("IN");
        }

        @Test
        @DisplayName("NOT IN produces NOT_IN leaf node")
        void notIn_producesNotInNode() {
            TraceResult result = ruleEngine.evaluateWithTrace(
                    "status NOT IN ('A', 'B')", Map.of("status", "C"));
            assertThat(result.result()).isTrue();
            assertThat(result.trace().type()).isEqualTo("NOT_IN");
        }

        @Test
        @DisplayName("CONTAINS produces CONTAINS leaf node")
        void contains_producesContainsNode() {
            TraceResult result = ruleEngine.evaluateWithTrace(
                    "name CONTAINS 'oh'", Map.of("name", "John"));
            assertThat(result.result()).isTrue();
            assertThat(result.trace().type()).isEqualTo("CONTAINS");
        }

        @Test
        @DisplayName("IS NULL produces IS_NULL leaf node")
        void isNull_producesIsNullNode() {
            TraceResult result = ruleEngine.evaluateWithTrace("email IS NULL", Map.of());
            assertThat(result.result()).isTrue();
            assertThat(result.trace().type()).isEqualTo("IS_NULL");
        }

        @Test
        @DisplayName("IS NOT NULL produces IS_NOT_NULL leaf node")
        void isNotNull_producesIsNotNullNode() {
            TraceResult result = ruleEngine.evaluateWithTrace(
                    "email IS NOT NULL", Map.of("email", "x"));
            assertThat(result.result()).isTrue();
            assertThat(result.trace().type()).isEqualTo("IS_NOT_NULL");
        }

        @Test
        @DisplayName("IS NUMERIC produces IS_NUMERIC leaf node")
        void isNumeric_producesIsNumericNode() {
            TraceResult result = ruleEngine.evaluateWithTrace(
                    "value IS NUMERIC", Map.of("value", 42));
            assertThat(result.result()).isTrue();
            assertThat(result.trace().type()).isEqualTo("IS_NUMERIC");
        }

        @Test
        @DisplayName("Boolean literal produces BOOL_LITERAL leaf node")
        void boolLiteral_producesBoolLiteralNode() {
            TraceResult result = ruleEngine.evaluateWithTrace("true", Map.of());
            assertThat(result.result()).isTrue();
            assertThat(result.trace().type()).isEqualTo("BOOL_LITERAL");
        }

        @Test
        @DisplayName("Function inside comparison produces COMPARISON node")
        void functionInComparison_producesComparisonNode() {
            TraceResult result = ruleEngine.evaluateWithTrace(
                    "length(name) > 3", Map.of("name", "John"));
            assertThat(result.result()).isTrue();
            assertThat(result.trace().type()).isEqualTo("COMPARISON");
        }

        @Test
        @DisplayName("Trace result matches direct evaluate result")
        void traceResult_matchesEvaluate() {
            Map<String, Object> ctx = Map.of("age", 10, "active", true);
            boolean direct = ruleEngine.evaluate("age > 18 AND active = true", ctx);
            TraceResult traced = ruleEngine.evaluateWithTrace("age > 18 AND active = true", ctx);
            assertThat(traced.result()).isEqualTo(direct);
        }
    }

    @Nested
    @DisplayName("Validation")
    class ValidationTests {

        @Test
        @DisplayName("Valid rule returns valid=true with no error")
        void validRule_returnsSuccess() {
            ValidationResult result = ruleEngine.validate("age > 18 AND active = true");
            assertThat(result.valid()).isTrue();
            assertThat(result.error()).isNull();
        }

        @ParameterizedTest(name = "[{index}] \"{0}\" returns valid=false")
        @ValueSource(strings = {"age >>> 18", "", "age >"})
        @DisplayName("Invalid expressions return valid=false with error message")
        void invalidRule_returnsFailure(String expression) {
            ValidationResult result = ruleEngine.validate(expression);
            assertThat(result.valid()).isFalse();
            assertThat(result.error()).isNotBlank();
        }
    }
}
