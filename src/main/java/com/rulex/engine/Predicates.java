package com.rulex.engine;

import com.rulex.exception.RuleEvaluationException;
import com.rulex.grammar.RuleLexer;

import java.util.Collection;
import java.util.List;

final class Predicates {

    private Predicates() {}

    static boolean compare(RuleValue left, RuleValue right, int opType) {
        if (opType == RuleLexer.EQ)  return left.equalTo(right);
        if (opType == RuleLexer.NEQ) return !left.equalTo(right);

        if (left.isNumeric() && right.isNumeric()) {
            return orderedCompare(Double.compare(left.asDouble(), right.asDouble()), opType);
        }
        return orderedCompare(left.asString().compareTo(right.asString()), opType);
    }

    private static boolean orderedCompare(int cmp, int opType) {
        return switch (opType) {
            case RuleLexer.GT  -> cmp > 0;
            case RuleLexer.GTE -> cmp >= 0;
            case RuleLexer.LT  -> cmp < 0;
            case RuleLexer.LTE -> cmp <= 0;
            default -> throw new RuleEvaluationException("Unknown comparison operator: " + opType);
        };
    }

    static boolean contains(RuleValue left, RuleValue right) {
        if (left.isNull()) return false;
        Object lRaw = left.getRaw();

        if (lRaw instanceof String ls) {
            return ls.contains(right.asString());
        }
        if (lRaw instanceof Collection<?> col) {
            Object rRaw = right.getRaw();
            if (col.contains(rRaw)) return true;
            String rStr = right.asString();
            return col.stream().anyMatch(i -> i != null && i.toString().equals(rStr));
        }
        if (lRaw instanceof Object[] arr) {
            Object rRaw = right.getRaw();
            String rStr = right.asString();
            for (Object item : arr) {
                if (item != null && (item.equals(rRaw) || item.toString().equals(rStr))) return true;
            }
            return false;
        }
        return false;
    }

    static boolean in(RuleValue target, List<RuleValue> items) {
        return items.stream().anyMatch(target::equalTo);
    }
}
