package com.rulex.store;

import java.time.Instant;

public record NamedRule(String name, String expression, Instant createdAt, Instant updatedAt) {

    public static NamedRule create(String name, String expression) {
        Instant now = Instant.now();
        return new NamedRule(name, expression, now, now);
    }

    public NamedRule withExpression(String newExpression) {
        return new NamedRule(this.name, newExpression, this.createdAt, Instant.now());
    }
}
