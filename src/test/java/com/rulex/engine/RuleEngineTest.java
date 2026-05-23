package com.rulex.engine;

import com.rulex.exception.RuleParseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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

    // ── Simple comparisons ────────────────────────────────────────────────────

    @Nested
    @DisplayName("Simple comparisons")
    class SimpleComparisons {

        @Test
        @DisplayName("age > 18 returns true when age is 25")
        void numericGreaterThan_true() {
            boolean result = ruleEngine.evaluate("age > 18", Map.of("age", 25));
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("age > 18 returns false when age is 10")
        void numericGreaterThan_false() {
            boolean result = ruleEngine.evaluate("age > 18", Map.of("age", 10));
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("name = 'John' returns true")
        void stringEquality_true() {
            boolean result = ruleEngine.evaluate("name = 'John'", Map.of("name", "John"));
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("name = 'John' returns false when name is 'Jane'")
        void stringEquality_false() {
            boolean result = ruleEngine.evaluate("name = 'John'", Map.of("name", "Jane"));
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("score <= 100 returns true when score is 100")
        void numericLessThanOrEqual_true() {
            boolean result = ruleEngine.evaluate("score <= 100", Map.of("score", 100));
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("score < 100 returns false when score is 100")
        void numericLessThan_false() {
            boolean result = ruleEngine.evaluate("score < 100", Map.of("score", 100));
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("score >= 50 returns true when score is 75")
        void numericGreaterThanOrEqual_true() {
            boolean result = ruleEngine.evaluate("score >= 50", Map.of("score", 75));
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("status != 'active' returns true when status is 'inactive'")
        void notEqual_true() {
            boolean result = ruleEngine.evaluate("status != 'active'", Map.of("status", "inactive"));
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Decimal number comparison: price > 9.99")
        void decimalComparison() {
            boolean result = ruleEngine.evaluate("price > 9.99", Map.of("price", 10.50));
            assertThat(result).isTrue();
        }
    }

    // ── Boolean literals ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("Boolean literals")
    class BooleanLiterals {

        @Test
        @DisplayName("Literal 'true' evaluates to true")
        void trueLiteral() {
            boolean result = ruleEngine.evaluate("true", Map.of());
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Literal 'false' evaluates to false")
        void falseLiteral() {
            boolean result = ruleEngine.evaluate("false", Map.of());
            assertThat(result).isFalse();
        }
    }

    // ── NOT operator ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("NOT operator")
    class NotOperator {

        @Test
        @DisplayName("NOT active = true returns false when active is true")
        void notTrue_returnsFalse() {
            boolean result = ruleEngine.evaluate("NOT active = true", Map.of("active", true));
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("NOT active = true returns true when active is false")
        void notFalse_returnsTrue() {
            boolean result = ruleEngine.evaluate("NOT active = true", Map.of("active", false));
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Double NOT: NOT NOT true returns true")
        void doubleNot() {
            boolean result = ruleEngine.evaluate("NOT NOT true", Map.of());
            assertThat(result).isTrue();
        }
    }

    // ── AND operator ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("AND operator")
    class AndOperator {

        @Test
        @DisplayName("age > 18 AND active = true returns true when both conditions hold")
        void andBothTrue() {
            boolean result = ruleEngine.evaluate("age > 18 AND active = true",
                    Map.of("age", 25, "active", true));
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("age > 18 AND active = true returns false when one condition fails")
        void andOneFails() {
            boolean result = ruleEngine.evaluate("age > 18 AND active = true",
                    Map.of("age", 25, "active", false));
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("age > 18 AND active = true returns false when both conditions fail")
        void andBothFail() {
            boolean result = ruleEngine.evaluate("age > 18 AND active = true",
                    Map.of("age", 10, "active", false));
            assertThat(result).isFalse();
        }
    }

    // ── OR operator ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("OR operator")
    class OrOperator {

        @Test
        @DisplayName("status = 'A' OR status = 'B' returns true when status is 'A'")
        void orFirstTrue() {
            boolean result = ruleEngine.evaluate("status = 'A' OR status = 'B'",
                    Map.of("status", "A"));
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("status = 'A' OR status = 'B' returns true when status is 'B'")
        void orSecondTrue() {
            boolean result = ruleEngine.evaluate("status = 'A' OR status = 'B'",
                    Map.of("status", "B"));
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("status = 'A' OR status = 'B' returns false when status is 'C'")
        void orBothFalse() {
            boolean result = ruleEngine.evaluate("status = 'A' OR status = 'B'",
                    Map.of("status", "C"));
            assertThat(result).isFalse();
        }
    }

    // ── Complex nesting ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("Complex nesting")
    class ComplexNesting {

        @Test
        @DisplayName("(age > 18 AND score > 50) OR vip = true: vip overrides")
        void complexNesting_vipOverride() {
            boolean result = ruleEngine.evaluate("(age > 18 AND score > 50) OR vip = true",
                    Map.of("age", 10, "score", 10, "vip", true));
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("(age > 18 AND score > 50) OR vip = true: both conditions met")
        void complexNesting_allMet() {
            boolean result = ruleEngine.evaluate("(age > 18 AND score > 50) OR vip = true",
                    Map.of("age", 25, "score", 75, "vip", false));
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("(age > 18 AND score > 50) OR vip = true: all conditions fail")
        void complexNesting_allFail() {
            boolean result = ruleEngine.evaluate("(age > 18 AND score > 50) OR vip = true",
                    Map.of("age", 10, "score", 10, "vip", false));
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Deeply nested: (a > 1 AND b > 1) AND (c > 1 OR d > 1)")
        void deeplyNested() {
            boolean result = ruleEngine.evaluate(
                    "(a > 1 AND b > 1) AND (c > 1 OR d > 1)",
                    Map.of("a", 2, "b", 2, "c", 0, "d", 2));
            assertThat(result).isTrue();
        }
    }

    // ── Null checks ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Null checks")
    class NullChecks {

        @Test
        @DisplayName("email IS NULL returns true when email is absent")
        void isNull_missingVar_returnsTrue() {
            boolean result = ruleEngine.evaluate("email IS NULL", Map.of());
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("email IS NULL returns false when email is present")
        void isNull_presentVar_returnsFalse() {
            boolean result = ruleEngine.evaluate("email IS NULL", Map.of("email", "test@example.com"));
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("email IS NOT NULL returns true when email is present")
        void isNotNull_presentVar_returnsTrue() {
            boolean result = ruleEngine.evaluate("email IS NOT NULL", Map.of("email", "test@example.com"));
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("email IS NOT NULL returns false when email is absent")
        void isNotNull_missingVar_returnsFalse() {
            boolean result = ruleEngine.evaluate("email IS NOT NULL", Map.of());
            assertThat(result).isFalse();
        }
    }

    // ── CONTAINS ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("CONTAINS operator")
    class ContainsOperator {

        @Test
        @DisplayName("name CONTAINS 'oh' returns true for 'John'")
        void stringContains_true() {
            boolean result = ruleEngine.evaluate("name CONTAINS 'oh'", Map.of("name", "John"));
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("name CONTAINS 'xyz' returns false for 'John'")
        void stringContains_false() {
            boolean result = ruleEngine.evaluate("name CONTAINS 'xyz'", Map.of("name", "John"));
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("tags CONTAINS 'java' returns true when list has 'java'")
        void listContains_true() {
            boolean result = ruleEngine.evaluate("tags CONTAINS 'java'",
                    Map.of("tags", List.of("java", "spring", "boot")));
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("tags CONTAINS 'python' returns false when list doesn't have it")
        void listContains_false() {
            boolean result = ruleEngine.evaluate("tags CONTAINS 'python'",
                    Map.of("tags", List.of("java", "spring", "boot")));
            assertThat(result).isFalse();
        }
    }

    // ── IS NUMERIC ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("IS NUMERIC check")
    class IsNumericCheck {

        @Test
        @DisplayName("value IS NUMERIC returns true for integer value")
        void isNumeric_integer_true() {
            boolean result = ruleEngine.evaluate("value IS NUMERIC", Map.of("value", 42));
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("value IS NUMERIC returns true for double value")
        void isNumeric_double_true() {
            boolean result = ruleEngine.evaluate("value IS NUMERIC", Map.of("value", 3.14));
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("value IS NUMERIC returns true for numeric string")
        void isNumeric_numericString_true() {
            boolean result = ruleEngine.evaluate("value IS NUMERIC", Map.of("value", "123.45"));
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("value IS NUMERIC returns false for non-numeric string")
        void isNumeric_nonNumericString_false() {
            boolean result = ruleEngine.evaluate("value IS NUMERIC", Map.of("value", "hello"));
            assertThat(result).isFalse();
        }
    }

    // ── Functions ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Built-in functions")
    class BuiltInFunctions {

        @Test
        @DisplayName("abs(balance) > 100 returns true when balance is -150")
        void absFunction() {
            boolean result = ruleEngine.evaluate("abs(balance) > 100", Map.of("balance", -150.0));
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("abs(balance) > 100 returns false when balance is -50")
        void absFunction_false() {
            boolean result = ruleEngine.evaluate("abs(balance) > 100", Map.of("balance", -50.0));
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("ceil(value) = 5 when value is 4.1")
        void ceilFunction() {
            boolean result = ruleEngine.evaluate("ceil(value) = 5", Map.of("value", 4.1));
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("floor(value) = 4 when value is 4.9")
        void floorFunction() {
            boolean result = ruleEngine.evaluate("floor(value) = 4", Map.of("value", 4.9));
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("round(value) = 5 when value is 4.6")
        void roundFunction() {
            boolean result = ruleEngine.evaluate("round(value) = 5", Map.of("value", 4.6));
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("length(name) > 3 returns true when name is 'John'")
        void lengthFunction_string() {
            boolean result = ruleEngine.evaluate("length(name) > 3", Map.of("name", "John"));
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("length(tags) = 3 returns true when tags has 3 elements")
        void lengthFunction_list() {
            boolean result = ruleEngine.evaluate("length(tags) = 3",
                    Map.of("tags", List.of("a", "b", "c")));
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("upper(status) = 'ACTIVE' returns true when status is 'active'")
        void upperFunction() {
            boolean result = ruleEngine.evaluate("upper(status) = 'ACTIVE'",
                    Map.of("status", "active"));
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("lower(status) = 'inactive' returns true when status is 'INACTIVE'")
        void lowerFunction() {
            boolean result = ruleEngine.evaluate("lower(status) = 'inactive'",
                    Map.of("status", "INACTIVE"));
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("trim(name) = 'John' returns true when name has leading/trailing spaces")
        void trimFunction() {
            boolean result = ruleEngine.evaluate("trim(name) = 'John'",
                    Map.of("name", "  John  "));
            assertThat(result).isTrue();
        }
    }

    // ── Case-insensitive keywords ─────────────────────────────────────────────

    @Nested
    @DisplayName("Case-insensitive keywords")
    class CaseInsensitiveKeywords {

        @Test
        @DisplayName("Lowercase 'and' keyword works")
        void lowercaseAnd() {
            boolean result = ruleEngine.evaluate("age > 18 and active = true",
                    Map.of("age", 25, "active", true));
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Lowercase 'or' keyword works")
        void lowercaseOr() {
            boolean result = ruleEngine.evaluate("status = 'A' or status = 'B'",
                    Map.of("status", "A"));
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Lowercase 'not' keyword works")
        void lowercaseNot() {
            boolean result = ruleEngine.evaluate("not active = true", Map.of("active", false));
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Mixed case 'And' keyword works")
        void mixedCaseAnd() {
            boolean result = ruleEngine.evaluate("age > 18 And active = true",
                    Map.of("age", 25, "active", true));
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Lowercase 'is null' keywords work")
        void lowercaseIsNull() {
            boolean result = ruleEngine.evaluate("email is null", Map.of());
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Lowercase 'contains' keyword works")
        void lowercaseContains() {
            boolean result = ruleEngine.evaluate("name contains 'oh'", Map.of("name", "John"));
            assertThat(result).isTrue();
        }
    }

    // ── String literal edge cases ─────────────────────────────────────────────

    @Nested
    @DisplayName("String literal edge cases")
    class StringLiteralEdgeCases {

        @Test
        @DisplayName("Double-quoted string literal works")
        void doubleQuotedString() {
            boolean result = ruleEngine.evaluate("name = \"John\"", Map.of("name", "John"));
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Escaped single quote '' in string literal")
        void escapedSingleQuote() {
            boolean result = ruleEngine.evaluate("note = 'it''s here'",
                    Map.of("note", "it's here"));
            assertThat(result).isTrue();
        }
    }

    // ── Error handling ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Error handling")
    class ErrorHandling {

        @Test
        @DisplayName("Invalid expression throws RuleParseException")
        void invalidExpression_throwsRuleParseException() {
            Map<String, Object> context = Map.of("age", 25);
            assertThatThrownBy(() -> ruleEngine.evaluate("age >>> 18", context))
                    .isInstanceOf(RuleParseException.class);
        }

        @Test
        @DisplayName("Incomplete expression throws RuleParseException")
        void incompleteExpression_throwsRuleParseException() {
            Map<String, Object> context = Map.of("age", 25);
            assertThatThrownBy(() -> ruleEngine.evaluate("age >", context))
                    .isInstanceOf(RuleParseException.class);
        }

        @Test
        @DisplayName("Empty expression throws RuleParseException")
        void emptyExpression_throwsRuleParseException() {
            Map<String, Object> context = Map.of();
            assertThatThrownBy(() -> ruleEngine.evaluate("", context))
                    .isInstanceOf(RuleParseException.class);
        }
    }

    // ── IN / NOT IN ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("IN and NOT IN operators")
    class InOperator {

        @Test
        @DisplayName("status IN ('A','B','C') returns true when status is 'A'")
        void in_string_match() {
            boolean result = ruleEngine.evaluate("status IN ('A', 'B', 'C')", Map.of("status", "A"));
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("status IN ('A','B','C') returns false when status is 'D'")
        void in_string_noMatch() {
            boolean result = ruleEngine.evaluate("status IN ('A', 'B', 'C')", Map.of("status", "D"));
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("age IN (18, 21, 25) returns true when age is 21")
        void in_numeric_match() {
            boolean result = ruleEngine.evaluate("age IN (18, 21, 25)", Map.of("age", 21));
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("age IN (18, 21, 25) returns false when age is 30")
        void in_numeric_noMatch() {
            boolean result = ruleEngine.evaluate("age IN (18, 21, 25)", Map.of("age", 30));
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("status NOT IN ('A','B') returns true when status is 'C'")
        void notIn_noMatch() {
            boolean result = ruleEngine.evaluate("status NOT IN ('A', 'B')", Map.of("status", "C"));
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("status NOT IN ('A','B') returns false when status is 'A'")
        void notIn_match() {
            boolean result = ruleEngine.evaluate("status NOT IN ('A', 'B')", Map.of("status", "A"));
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("IN with single value list")
        void in_singleValue() {
            boolean result = ruleEngine.evaluate("role IN ('admin')", Map.of("role", "admin"));
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("IN combined with AND")
        void in_combinedWithAnd() {
            boolean result = ruleEngine.evaluate("status IN ('A', 'B') AND active = true",
                    Map.of("status", "B", "active", true));
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("NOT IN combined with OR")
        void notIn_combinedWithOr() {
            boolean result = ruleEngine.evaluate("status NOT IN ('X', 'Y') OR vip = true",
                    Map.of("status", "A", "vip", false));
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Case-insensitive 'in' keyword")
        void in_caseInsensitive() {
            boolean result = ruleEngine.evaluate("status in ('A', 'B')", Map.of("status", "A"));
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Case-insensitive 'not in' keyword")
        void notIn_caseInsensitive() {
            boolean result = ruleEngine.evaluate("status not in ('A', 'B')", Map.of("status", "C"));
            assertThat(result).isTrue();
        }
    }

    // ── Arithmetic expressions ────────────────────────────────────────────────

    @Nested
    @DisplayName("Arithmetic expressions")
    class ArithmeticExpressions {

        @Test
        @DisplayName("abs(Col_1 - Col_2) > 0.2 — original use case")
        void absOfDifference() {
            boolean result = ruleEngine.evaluate("abs(Col_1 - Col_2) > 0.2",
                    Map.of("Col_1", 1.5, "Col_2", 1.0));
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("abs(Col_1 - Col_2) > 0.2 returns false when difference is small")
        void absOfDifference_false() {
            boolean result = ruleEngine.evaluate("abs(Col_1 - Col_2) > 0.2",
                    Map.of("Col_1", 1.1, "Col_2", 1.0));
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("a + b > 10")
        void addition() {
            boolean result = ruleEngine.evaluate("a + b > 10",
                    Map.of("a", 6, "b", 7));
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("a - b > 3")
        void subtraction() {
            boolean result = ruleEngine.evaluate("a - b > 3",
                    Map.of("a", 10, "b", 5));
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("a * b <= 100")
        void multiplication() {
            boolean result = ruleEngine.evaluate("a * b <= 100",
                    Map.of("a", 5, "b", 20));
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("a / b > 2.0")
        void division() {
            boolean result = ruleEngine.evaluate("a / b > 2.0",
                    Map.of("a", 10.0, "b", 4.0));
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("a % 2 = 0 (even number check)")
        void modulo() {
            boolean result = ruleEngine.evaluate("a % 2 = 0",
                    Map.of("a", 8));
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Operator precedence: a + b * c = 14 (not 21)")
        void precedence_mulBeforeAdd() {
            boolean result = ruleEngine.evaluate("a + b * c = 14",
                    Map.of("a", 2, "b", 3, "c", 4));
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Grouping overrides precedence: (a + b) * c = 20")
        void arithmeticGrouping() {
            boolean result = ruleEngine.evaluate("(a + b) * c = 20",
                    Map.of("a", 2, "b", 3, "c", 4));
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Unary minus: -a > -10 is true when a=5 (-5 > -10)")
        void unaryMinus() {
            boolean result = ruleEngine.evaluate("-a > -10",
                    Map.of("a", 5));
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Nested arithmetic in function: abs(a - b) > 5")
        void nestedArithInFunction() {
            boolean result = ruleEngine.evaluate("abs(a - b) > 5",
                    Map.of("a", 3.0, "b", 10.0));
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Arithmetic with literals: 10 + 5 > 12")
        void arithmeticWithLiterals() {
            boolean result = ruleEngine.evaluate("10 + 5 > 12", Map.of());
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Combined arithmetic and boolean: a + b > 10 AND c * d < 50")
        void arithmeticWithBoolean() {
            boolean result = ruleEngine.evaluate("a + b > 10 AND c * d < 50",
                    Map.of("a", 6, "b", 7, "c", 3, "d", 10));
            assertThat(result).isTrue();
        }
    }

    // ── Rule Explanation / Trace ──────────────────────────────────────────────

    @Nested
    @DisplayName("Rule trace (evaluateWithTrace)")
    class TraceTests {

        @Test
        @DisplayName("Simple comparison produces COMPARISON trace node")
        void simpleComparison_producesLeafNode() {
            RuleEngine.TraceResult result = ruleEngine.evaluateWithTrace("age > 18", Map.of("age", 25));
            assertThat(result.result()).isTrue();
            assertThat(result.trace().type()).isEqualTo("COMPARISON");
            assertThat(result.trace().result()).isTrue();
            assertThat(result.trace().evaluated()).contains(">");
            assertThat(result.trace().children()).isNull();
        }

        @Test
        @DisplayName("AND rule produces compound AND node with children")
        void andRule_producesCompoundNode() {
            RuleEngine.TraceResult result = ruleEngine.evaluateWithTrace(
                    "age > 18 AND active = true", Map.of("age", 25, "active", true));
            assertThat(result.result()).isTrue();
            assertThat(result.trace().type()).isEqualTo("AND");
            assertThat(result.trace().children()).hasSize(2);
            assertThat(result.trace().children().get(0).type()).isEqualTo("COMPARISON");
            assertThat(result.trace().children().get(1).type()).isEqualTo("COMPARISON");
        }

        @Test
        @DisplayName("OR rule short-circuits: both children still evaluated in trace")
        void orRule_traceIncludesBothBranches() {
            RuleEngine.TraceResult result = ruleEngine.evaluateWithTrace(
                    "age > 18 OR score > 100", Map.of("age", 25, "score", 50));
            assertThat(result.result()).isTrue();
            assertThat(result.trace().type()).isEqualTo("OR");
            assertThat(result.trace().children()).hasSize(2);
        }

        @Test
        @DisplayName("NOT rule produces NOT node wrapping child")
        void notRule_producesNotNode() {
            RuleEngine.TraceResult result = ruleEngine.evaluateWithTrace(
                    "NOT active = true", Map.of("active", false));
            assertThat(result.result()).isTrue();
            assertThat(result.trace().type()).isEqualTo("NOT");
            assertThat(result.trace().children()).hasSize(1);
            assertThat(result.trace().children().get(0).result()).isFalse();
        }

        @Test
        @DisplayName("Trace result matches evaluate result")
        void traceResult_matchesEvaluateResult() {
            Map<String, Object> ctx = Map.of("age", 10, "active", true);
            boolean direct = ruleEngine.evaluate("age > 18 AND active = true", ctx);
            RuleEngine.TraceResult traced = ruleEngine.evaluateWithTrace("age > 18 AND active = true", ctx);
            assertThat(traced.result()).isEqualTo(direct);
        }
    }

    // ── Validation ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Validation")
    class ValidationTests {

        @Test
        @DisplayName("Valid rule returns success")
        void validRule_returnsSuccess() {
            RuleEngine.ValidationResult result = ruleEngine.validate("age > 18 AND active = true");
            assertThat(result.valid()).isTrue();
            assertThat(result.error()).isNull();
        }

        @Test
        @DisplayName("Invalid rule returns failure with error message")
        void invalidRule_returnsFailure() {
            RuleEngine.ValidationResult result = ruleEngine.validate("age >>> 18");
            assertThat(result.valid()).isFalse();
            assertThat(result.error()).isNotBlank();
        }
    }
}
