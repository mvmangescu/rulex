package com.rulex.engine;

import com.rulex.exception.RuleEvaluationException;
import lombok.Getter;

@Getter
public final class RuleValue {

    public static final RuleValue NULL = new RuleValue(null);
    public static final RuleValue TRUE = new RuleValue(Boolean.TRUE);
    public static final RuleValue FALSE = new RuleValue(Boolean.FALSE);

    private final Object raw;

    private RuleValue(Object raw) {
        this.raw = raw;
    }

    public static RuleValue of(Object value) {
        if (value == null) return NULL;
        if (Boolean.TRUE.equals(value)) return TRUE;
        if (Boolean.FALSE.equals(value)) return FALSE;
        return new RuleValue(value);
    }

    public boolean isNull() {
        return raw == null;
    }

    public boolean isNumeric() {
        if (raw instanceof Number n) {
            double d = n.doubleValue();
            return !Double.isNaN(d) && !Double.isInfinite(d);
        }
        if (raw instanceof String s) {
            try {
                double d = Double.parseDouble(s);
                return !Double.isNaN(d) && !Double.isInfinite(d);
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }

    public double asDouble() {
        if (raw instanceof Number n) {
            return validFinite(n.doubleValue());
        }
        if (raw instanceof String s) {
            try {
                return validFinite(Double.parseDouble(s));
            } catch (NumberFormatException e) {
                throw new RuleEvaluationException("Cannot convert value to numeric: '" + s + "'");
            }
        }
        throw new RuleEvaluationException(
                "Cannot convert value of type " + (raw == null ? "null" : raw.getClass().getName()) + " to numeric");
    }

    public String asString() {
        return raw == null ? "null" : raw.toString();
    }

    public boolean asBoolean() {
        if (raw instanceof Boolean b) return b;
        throw new RuleEvaluationException(
                "Cannot convert value of type " + (raw == null ? "null" : raw.getClass().getName()) + " to boolean");
    }

    public boolean equalTo(RuleValue other) {
        if (this.isNull() && other.isNull()) return true;
        if (this.isNull() || other.isNull()) return false;
        if (this.isNumeric() && other.isNumeric()) {
            return Double.compare(this.asDouble(), other.asDouble()) == 0;
        }
        return this.asString().equals(other.asString());
    }

    @Override
    public String toString() {
        return "RuleValue{raw=" + raw + "}";
    }

    private static double validFinite(double d) {
        if (Double.isNaN(d)) throw new RuleEvaluationException("Numeric value is NaN");
        if (Double.isInfinite(d)) throw new RuleEvaluationException("Numeric value is Infinite");
        return d;
    }
}
